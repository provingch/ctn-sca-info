#!/usr/bin/env bash
# Convierte SVGs de /backend/src/main/resources/static/assets/ a PNGs usados por la exportación XLSX.
# Requiere `rsvg-convert` (librsvg) o `inkscape` en PATH.

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ASSETS="$ROOT/backend/src/main/resources/static/assets"
OUT="$ASSETS/png"
mkdir -p "$OUT"

for svg in "$ASSETS"/*.svg; do
  name=$(basename "$svg" .svg)
  # Normalizar nombre simple (minúsculas, sin acentos, espacios->-)
  norm=$(echo "$name" | iconv -f utf8 -t ascii//TRANSLIT 2>/dev/null || cat)
  norm=$(echo "$norm" | tr '[:upper:]' '[:lower:]' | sed -E 's/[^a-z0-9]+/-/g' | sed -E 's/^-|-$//g')
  out="$OUT/logo-especialidad-$norm.png"
  if command -v rsvg-convert >/dev/null 2>&1; then
    rsvg-convert -w 600 -h 200 "$svg" -o "$out"
    echo "Converted $svg -> $out"
  elif command -v inkscape >/dev/null 2>&1; then
    inkscape "$svg" --export-type=png --export-filename="$out" --export-width=600 --export-height=200
    echo "Converted $svg -> $out"
  else
    echo "No se encontró rsvg-convert ni inkscape. Instala librsvg2-bin o inkscape." >&2
    exit 2
  fi
done

echo "PNG files written to: $OUT"
