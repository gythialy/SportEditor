#!/usr/bin/env bash
# The launcher icon itself is a vector adaptive icon. This script only renders a
# 512x512 preview (useful for stores / design review) from design/icon.svg.
set -eo pipefail
ROOT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
cd "$ROOT"
command -v rsvg-convert >/dev/null 2>&1 || { echo "rsvg-convert is required" >&2; exit 1; }
mkdir -p design/preview
rsvg-convert -w 512 -h 512 design/icon.svg -o design/preview/icon-512.png
echo "icon preview rendered to design/preview/icon-512.png"
