#!/usr/bin/env bash
# Convierte SVGs fuente estables a PNGs usados por la exportación XLSX.
# Requiere `rsvg-convert` (librsvg) o `inkscape` en PATH.

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SOURCE="$ROOT/backend/src/main/resources/logos-source"
OUT_SPECIALTY="$ROOT/backend/src/main/resources/static/assets/png"
OUT_INSTITUTIONAL="$ROOT/backend/src/main/resources/static/logo-institucional.png"
mkdir -p "$OUT_SPECIALTY"

convert_svg() {
  local svg="$1"
  local out="$2"
  if command -v rsvg-convert >/dev/null 2>&1; then
    rsvg-convert -w 600 -h 200 "$svg" -o "$out"
  elif command -v inkscape >/dev/null 2>&1; then
    inkscape "$svg" --export-type=png --export-filename="$out" --export-width=600 --export-height=200
  else
    echo "No se encontró rsvg-convert ni inkscape. Instala librsvg2-bin o inkscape." >&2
    exit 2
  fi
}

if [ ! -d "$SOURCE" ]; then
  echo "No existe la carpeta fuente: $SOURCE" >&2
  exit 2
fi

institutional_svg="$SOURCE/institucional.svg"
if [ ! -f "$institutional_svg" ]; then
  echo "Falta el SVG institucional esperado: $institutional_svg" >&2
  exit 2
fi
convert_svg "$institutional_svg" "$OUT_INSTITUTIONAL"
echo "Converted $institutional_svg -> $OUT_INSTITUTIONAL"

for svg in "$SOURCE"/*.svg; do
  [ -e "$svg" ] || continue
  name=$(basename "$svg" .svg)
  case "$name" in
    institucional)
      continue
      ;;
    construcciones-civiles)
      out="$OUT_SPECIALTY/logo-especialidad-construcciones.png"
      ;;
    electricidad)
      out="$OUT_SPECIALTY/logo-especialidad-electricidad.png"
      ;;
    electromecanica)
      out="$OUT_SPECIALTY/logo-especialidad-electromecanica.png"
      ;;
    electronica)
      out="$OUT_SPECIALTY/logo-especialidad-electronica.png"
      ;;
    informatica)
      out="$OUT_SPECIALTY/logo-especialidad-informatica.png"
      ;;
    mecanica-automotriz)
      out="$OUT_SPECIALTY/logo-especialidad-mecanica-automotriz.png"
      ;;
    mecanica-industrial)
      out="$OUT_SPECIALTY/logo-especialidad-mecanica-general.png"
      ;;
    quimica-industrial)
      out="$OUT_SPECIALTY/logo-especialidad-quimica.png"
      ;;
    *)
      echo "Nombre de SVG no mapeado: $svg" >&2
      exit 2
      ;;
  esac
  convert_svg "$svg" "$out"
  echo "Converted $svg -> $out"
done

echo "PNG files written to: $OUT_SPECIALTY and $OUT_INSTITUTIONAL"
