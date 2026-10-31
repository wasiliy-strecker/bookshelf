#!/usr/bin/env python3
"""Package the existing app artwork as window, Windows and macOS icons."""

import shutil
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]


def main():
    branding = ROOT / "assets/branding"
    resources = ROOT / "src/main/resources/de/appfabrik/buecherregal"
    msix_assets = ROOT / "packaging/windows/Assets"
    msix_assets.mkdir(parents=True, exist_ok=True)
    with Image.open(branding / "icon-1024.png") as original:
        image = original.convert("RGBA")
        for size in (16, 32, 48, 64, 128, 512):
            image.resize((size, size), Image.Resampling.LANCZOS).save(
                branding / f"icon-{size}.png"
            )
        image.save(branding / "icon.ico", sizes=[(s, s) for s in (16, 32, 48, 64, 128, 256)])
        image.save(branding / "icon.icns")
        for name, size in (
            ("Square44x44Logo", 44),
            ("Square150x150Logo", 150),
            ("StoreLogo", 50),
        ):
            image.resize((size, size), Image.Resampling.LANCZOS).save(
                msix_assets / f"{name}.png"
            )
    for size in (16, 32, 48, 64, 128, 256):
        shutil.copy2(branding / f"icon-{size}.png", resources / f"icon-{size}.png")


if __name__ == "__main__":
    main()
