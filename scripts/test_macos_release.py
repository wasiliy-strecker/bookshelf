#!/usr/bin/env python3
"""Host-independent checks for native input validation and safe packaging."""
import hashlib
import importlib.util
from pathlib import Path
import struct
import tempfile
import unittest
import zipfile

SPEC = importlib.util.spec_from_file_location("macos_release", Path(__file__).with_name("macos_release.py"))
release = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(release)


def macho(cpu):
    return b"\xcf\xfa\xed\xfe" + struct.pack("<7I", cpu, 0, 6, 0, 0, 0, 0)


class MacPreparationTest(unittest.TestCase):
    def test_recognizes_both_real_header_layouts_and_universal_launcher(self):
        self.assertEqual({"arm64"}, release.macho_architectures(macho(0x100000c)))
        self.assertEqual({"x86_64"}, release.macho_architectures(macho(0x1000007)))
        universal = b"\xca\xfe\xba\xbe" + struct.pack(">I", 2)
        universal += struct.pack(">5I", 0x1000007, 0, 4096, 32, 12)
        universal += struct.pack(">5I", 0x100000c, 0, 8192, 32, 12)
        self.assertEqual({"arm64", "x86_64"}, release.macho_architectures(universal))

    def test_rejects_foreign_truncated_and_duplicate_architectures(self):
        for data in (b"MZ", macho(7), b"\xca\xfe\xba\xbe" + struct.pack(">I", 2),
                     b"\xca\xfe\xba\xbe" + struct.pack(">I", 2)
                     + struct.pack(">5I", 0x100000c, 0, 4096, 32, 12) * 2):
            with self.assertRaises(ValueError):
                release.macho_architectures(data)

    def test_refuses_corrupt_cached_dependency_without_replacing_it(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "dependency.jar"
            path.write_bytes(b"corrupt")
            entry = {"file": path.name, "sha256": hashlib.sha256(b"expected").hexdigest()}
            with self.assertRaises(ValueError):
                release.checked_download(entry, Path(directory))
            self.assertEqual(b"corrupt", path.read_bytes())

    def test_sqlite_is_split_by_architecture_without_losing_java_or_licenses(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "upstream.jar"
            with zipfile.ZipFile(source, "w") as jar:
                jar.writestr("org/sqlite/JDBC.class", b"java-bytecode")
                jar.writestr("META-INF/LICENSE", b"original license")
                jar.writestr("org/sqlite/native/Mac/aarch64/libsqlitejdbc.dylib", macho(0x100000c))
                jar.writestr("org/sqlite/native/Mac/x86_64/libsqlitejdbc.dylib", macho(0x1000007))
                jar.writestr("org/sqlite/native/Linux/x86_64/libsqlitejdbc.so", b"ELF")
            checksums = []
            for arch in release.ARCHITECTURES:
                native = root / arch
                native.mkdir()
                destination = root / (arch + ".jar")
                release.strip_native_jar(source, destination, native, arch, sqlite=True)
                self.assertEqual({arch}, release.macho_architectures((native / "libsqlitejdbc.dylib").read_bytes()))
                with zipfile.ZipFile(destination) as jar:
                    self.assertEqual({"org/sqlite/JDBC.class", "META-INF/LICENSE"}, set(jar.namelist()))
                    self.assertEqual(b"original license", jar.read("META-INF/LICENSE"))
                checksums.append(release.sha256(destination))
            self.assertEqual(checksums[0], checksums[1])

    def test_bundle_assembly_refuses_incompatible_library(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "javafx.jar"
            with zipfile.ZipFile(source, "w") as jar:
                jar.writestr("libglass.dylib", macho(0x1000007))
            native = root / "native"
            native.mkdir()
            with self.assertRaises(ValueError):
                release.strip_native_jar(source, root / "stripped.jar", native, "arm64")


if __name__ == "__main__":
    unittest.main()
