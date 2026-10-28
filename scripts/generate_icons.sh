#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
for size in 1024 256 300; do
  name="icon-$size.png"
  if [[ "$size" == 300 ]]; then name=store-icon-300.png; fi
  convert -background none assets/branding/icon.svg -resize "${size}x${size}" "assets/branding/$name"
done
convert assets/branding/icon-256.png -define icon:auto-resize=256,128,64,48,32,16 assets/branding/icon.ico
cp assets/branding/icon-256.png src/main/resources/de/appfabrik/buecherregal/icon-256.png
