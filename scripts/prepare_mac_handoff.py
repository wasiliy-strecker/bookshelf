#!/usr/bin/env python3
"""Create a new German Mac Store handoff folder from committed sources and checked inputs."""
import argparse
from datetime import datetime, timezone
import html
import json
from pathlib import Path
import shutil
import struct
import subprocess
import tempfile

import macos_release as mac
from prepare_store_release import acquire_sources
from store_assets.run_gui import source_hashes

ROOT = Path(__file__).resolve().parents[1]


def checked_capture(folder, mode, locales, viewport):
    capture = mac.read_json(folder / "capture-manifest.json")
    if (capture["platform"] != "Linux" or not capture["preview_only"]
            or capture["viewport"] != viewport or capture["mode"] != mode
            or capture["locales"] != locales):
        raise ValueError("Unexpected GUI verification or draft manifest: " + str(folder))
    if capture["source_files_sha256"] != source_hashes():
        raise ValueError("GUI verification no longer matches current sources: " + str(folder))
    return capture


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--inputs", type=Path, required=True)
    parser.add_argument("--captures", type=Path, required=True)
    parser.add_argument("--gui-verification", type=Path, required=True)
    parser.add_argument("--transfer-verification", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if subprocess.check_output(["git", "diff", "HEAD", "--name-only"], cwd=ROOT).strip():
        raise ValueError("Commit verified tracked changes before creating the handoff")
    mac.check_inputs(args.inputs.resolve())
    for name, marker in (("mac-preparation-verify.log", "BUILD SUCCESS"),
                         ("mac-preparation-python-tests.log", "\nOK\n")):
        log = (ROOT / "target" / name).read_text()
        if marker not in log:
            raise ValueError("Required local verification has not passed: " + name)
    capture_root = args.captures.resolve()
    checked_capture(capture_root, "store", ["de-DE"], [2560, 1600])
    checked_capture(args.gui_verification.resolve(), "all",
                    ["de-DE", "en-US", "es-ES", "fr-FR", "it-IT", "pt-BR"], [1920, 1080])
    checked_capture(args.transfer_verification.resolve(), "transfer", ["de-DE"], [1920, 1080])
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    revision = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    source = output / "Source"
    source.mkdir()
    names = subprocess.check_output(["git", "ls-files", "-z"], cwd=ROOT).decode().split("\0")
    for name in filter(None, names):
        if name.endswith((".local.json", ".p12", ".pfx", ".provisionprofile")):
            raise ValueError("Private signing material must never be tracked")
        destination = source / name
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(ROOT / name, destination)
    (source / "source-revision.txt").write_text(revision + "\n")
    shutil.copytree(args.inputs.resolve(), output / "MacInputs")
    shutil.copytree(ROOT / "store-listings/mac-app-store/de-DE", output / "Store/de-DE")
    shutil.copytree(ROOT / "assets/branding", output / "Branding")
    privacy = output / "Privacy"
    privacy.mkdir()
    with tempfile.TemporaryDirectory(prefix="bookshelf-privacy-") as temporary:
        export = Path(temporary) / "export"
        mac.run(ROOT / "scripts/export_privacy.sh", export)
        text = (export / "de-DE.txt").read_text()
    (privacy / "de-DE.txt").write_text(text)
    rendered = "<!doctype html><html lang=\"de\"><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width\"><title>Datenschutz – Bookshelf</title><style>body{max-width:52rem;margin:3rem auto;padding:0 1rem;font:18px/1.6 system-ui}p{white-space:pre-line}</style><main>"
    rendered += "".join("<p>" + html.escape(part) + "</p>" for part in text.split("\n\n"))
    (privacy / "index.html").write_text(rendered + "</main></html>\n")
    (privacy / "URL-NOCH-OFFEN.txt").write_text(
        "Diese vollständige Erklärung vor der Einreichung öffentlich ohne Anmeldung bereitstellen.\n"
        "Die lokale Datei ist noch keine öffentliche URL. Kein Upload wurde ausgeführt.\n"
        "Für die erste Store-Veröffentlichung nur die deutsche Erklärung verwenden.\n")
    drafts = output / "Screenshot-Drafts-Linux/de-DE"
    drafts.mkdir(parents=True)
    for name in ("01-overview.png", "02-edit.png", "03-search.png", "04-json-export.png"):
        image = capture_root / "de-DE/store" / name
        data = image.read_bytes()
        if data[:8] != b"\x89PNG\r\n\x1a\n" or struct.unpack(">II", data[16:24]) != (2560, 1600):
            raise ValueError("Incorrect screenshot dimensions: " + name)
        shutil.copy2(image, drafts)
    shutil.copy2(capture_root / "capture-manifest.json", drafts.parent)
    (drafts.parent / "ENTWUERFE-NICHT-HOCHLADEN.txt").write_text(
        "Echte Linux-Aufnahmen mit synthetischen Daten. Keine Mac-Abnahme.\n"
        "Für den Store auf dem Mac dieselben Ansichten neu aufnehmen.\n")
    (output / "Screenshots-Final-Mac/de-DE").mkdir(parents=True)
    shutil.copy2(ROOT / "docs/mac-app-store-release.md", output / "AUF-DEM-MAC.md")
    shutil.copy2(ROOT / "docs/mac-app-store-fields.md", output / "JETZT-IN-APP-STORE-CONNECT.md")
    acquire_sources(ROOT / "build/releases/runtime-sources/temurin-21.0.13_9-openjfx-21.0.11_4", output / "RuntimeSources")
    for metadata in (ROOT / "packaging/macos").glob("*.tar.gz.json"):
        shutil.copy2(metadata, output / "RuntimeSources")
    verification = output / "Verification"
    verification.mkdir()
    for label, folder in (("gui", args.gui_verification.resolve()),
                          ("transfer", args.transfer_verification.resolve()), ("store", capture_root)):
        shutil.copy2(folder / "capture-manifest.json", verification / (label + "-manifest.json"))
        for log in folder.rglob("run.log"):
            destination = verification / label / log.relative_to(folder)
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(log, destination)
    for name in ("mac-preparation-verify.log", "mac-preparation-python-tests.log", "mac-preparation-gui.log",
                 "mac-preparation-transfer.log", "mac-preparation-store.log", "mac-preparation-inputs.log"):
        shutil.copy2(ROOT / "target" / name, verification)
    report = {"application": mac.settings(), "sourceCommit": revision,
              "createdUtc": datetime.now(timezone.utc).isoformat(),
              "localPreparation": "verified", "screenshots": "Linux drafts only",
              "macBuild": False, "macAcceptance": False, "signing": False, "upload": False,
              "publicPrivacyURL": "pending", "runtimeSourcePublication": "pending", "storeReady": False}
    (output / "preparation-info.json").write_text(json.dumps(report, indent=2) + "\n")
    (output / "ZUERST-LESEN.txt").write_text(
        "Bookshelf – Mac App Store, Version 1.0, Build 1\n\n"
        "Jetzt: JETZT-IN-APP-STORE-CONNECT.md öffnen und deutsche Texte eintragen.\n"
        "Danach: Den gesamten Ordner auf den Mac kopieren und AUF-DEM-MAC.md folgen.\n"
        "Bilder unter Screenshot-Drafts-Linux sind Entwürfe. Finale Mac-Bilder fehlen.\n"
        "MacInputs enthält geprüfte Eingaben für ARM64 und Intel, noch keine fertige App.\n"
        "Datenschutzseite und Laufzeitquellen sind vorbereitet, noch nicht veröffentlicht.\n"
        "Es wurde nichts in einen Store hochgeladen. Keine persönlichen Sammlungen enthalten.\n"
        f"Quellcommit: {revision}\n")
    sums = [f"{mac.sha256(p)}  {p.relative_to(output).as_posix()}\n"
            for p in sorted(output.rglob("*")) if p.is_file()]
    (output / "SHA256SUMS.txt").write_text("".join(sums))
    print(output)


if __name__ == "__main__":
    main()
