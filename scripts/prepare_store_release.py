#!/usr/bin/env python3
"""Assemble an internal Store handoff, without tests or release approval."""

import argparse
import hashlib
import json
import shutil
import subprocess
import uuid
from datetime import datetime, timezone
from pathlib import Path
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
LOCALES = ("de-DE", "en-US", "es-ES", "fr-FR", "it-IT", "pt-BR")


def git(*arguments):
    return subprocess.check_output(["git", *arguments], cwd=ROOT)


def sha256(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def acquire_sources(cache, destination):
    lock = json.loads((ROOT / "packaging/runtime-sources.json").read_text())
    cache.mkdir(parents=True, exist_ok=True)
    destination.mkdir()
    for entry in lock["files"]:
        cached = cache / entry["file"]
        if not cached.exists():
            temporary = cache / f"{uuid.uuid4().hex}.part"
            try:
                print(f"Download: {entry['file']}", flush=True)
                request = Request(
                    entry["url"], headers={"User-Agent": "Bookshelf-release-preparation"}
                )
                with urlopen(request, timeout=60) as response, temporary.open("xb") as stream:
                    shutil.copyfileobj(response, stream)
                if sha256(temporary) != entry["sha256"]:
                    raise RuntimeError(f"Download checksum mismatch: {entry['file']}")
                temporary.rename(cached)
            finally:
                temporary.unlink(missing_ok=True)
        if cached.stat().st_size != entry["bytes"] or sha256(cached) != entry["sha256"]:
            raise RuntimeError(f"Source archive differs from pinned inventory: {cached}")
        shutil.copy2(cached, destination / entry["file"])
    shutil.copy2(ROOT / "packaging/runtime-sources.json", destination)
    shutil.copy2(ROOT / "packaging/runtime-sources-readme.txt", destination / "README.txt")
    shutil.copy2(ROOT / "THIRD_PARTY_NOTICES.md", destination)
    shutil.copytree(ROOT / "third-party-licenses", destination / "licenses")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path, help="New handoff directory, never overwritten")
    parser.add_argument(
        "--source-cache",
        type=Path,
        default=ROOT / "build/releases/runtime-sources/temurin-21.0.13_9-openjfx-21.0.11_4",
    )
    arguments = parser.parse_args()
    if git("diff", "--name-only", "HEAD").strip():
        raise SystemExit("Commit the tracked source changes before preparing the handoff.")
    revision = git("rev-parse", "HEAD").decode().strip()
    output = arguments.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    status = {
        "application": "BücherRegal",
        "sourceCommit": revision,
        "createdUtc": datetime.now(timezone.utc).isoformat(),
        "preparationComplete": False,
        "storeReady": False,
        "testsExecuted": False,
        "windowsPackageBuilt": False,
        "windowsAcceptance": "pending",
        "partnerCenterIdentity": "pending",
        "screenshots": "pending-final-windows-captures",
        "runtimeSourcePublication": "pending",
    }
    status_file = output / "preparation-info.json"
    status_file.write_text(json.dumps(status, ensure_ascii=False, indent=2) + "\n")

    # Copy tracked files only. Personal JSON exports and ignored local secrets stay outside.
    source = output / "Source"
    source.mkdir()
    for name in git("ls-files", "-z").decode().split("\0"):
        if not name:
            continue
        destination = source / name
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(ROOT / name, destination)
    (source / "source-revision.txt").write_text(revision + "\n")
    shutil.copytree(ROOT / "store-listings", output / "Store")
    shutil.copytree(ROOT / "assets/branding", output / "Branding")
    shutil.copy2(ROOT / "docs/microsoft-store-release.md", output / "WINDOWS-UND-STORE.md")
    acquire_sources(arguments.source_cache.resolve(), output / "RuntimeSources")

    # Compile the text exporter only. No JUnit, GUI, user database or test execution.
    subprocess.run(
        [str(ROOT / "scripts/export_privacy.sh"), str(output / "Privacy")],
        cwd=ROOT,
        check=True,
    )
    screenshots = output / "Screenshots"
    screenshots.mkdir()
    for locale in LOCALES:
        (screenshots / locale).mkdir()
    (screenshots / "README.txt").write_text(
        "Finale Windows-Aufnahmen fehlen noch. Keine Linux-Vorschau hier verwenden.\n"
        "Pro Sprache: Sammlung, Bearbeitung, Suche, Sprachauswahl.\n"
        "PNG in 1920 x 1080 empfohlen, mindestens 1366 x 768, maximal 50 MB.\n"
        "Nur synthetische Daten. Bildunterschriften liegen unter Store/<Sprache>/captions.txt.\n",
        encoding="utf-8",
    )
    (output / "ZUERST-LESEN.txt").write_text(
        "BücherRegal: lokale Vorbereitung für den Microsoft Store\n\n"
        "Status: noch nicht bereit. Keine Tests ausgeführt. Kein MSIX gebaut.\n"
        "Nur BücherRegal wurde bearbeitet. Keine Veröffentlichung und kein Push.\n\n"
        "Als Nächstes die App im Partner Center anlegen und die drei Identitätswerte\n"
        "aus der Produktidentität nach Source/packaging/windows/store.local.json übernehmen.\n"
        "Dafür die beiliegende store.example.json kopieren. Keine Zugangsdaten weitergeben.\n\n"
        "WINDOWS-UND-STORE.md enthält Paketierung, lokale Signierung und manuelle Prüfschritte.\n"
        "Source ist eine interne Quellübergabe, keine zur Veröffentlichung freigegebene Lizenz.\n"
        "Store enthält Feldtexte, Privacy die sechs vollständigen Erklärungen der App.\n"
        "Screenshots enthält noch keine finalen Windows-Bilder.\n"
        "RuntimeSources enthält lokale Fremdkomponenten-Quellen und Herkunft.\n"
        "Öffentliche Bereitstellung und Abgleich mit dem endgültigen Windows-Paket sind offen.\n\n"
        f"Quellcommit: {revision}\n",
        encoding="utf-8",
    )
    status["preparationComplete"] = True
    status_file.write_text(json.dumps(status, ensure_ascii=False, indent=2) + "\n")
    inventory = []
    for path in sorted(output.rglob("*")):
        if path.is_file():
            inventory.append(f"{sha256(path)}  {path.relative_to(output).as_posix()}\n")
    (output / "SHA256SUMS.txt").write_text("".join(inventory), encoding="utf-8")
    print(f"Local preparation complete, Windows acceptance pending: {output}")


if __name__ == "__main__":
    main()
