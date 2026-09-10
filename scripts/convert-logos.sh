#!/usr/bin/env bash
# Convierte SVGs fuente estables a PNGs usados por la exportación XLSX.
# Requiere `rsvg-convert` (librsvg) o `inkscape` en PATH.

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SOURCE="$ROOT/backend/src/main/resources/logos-source"
OUT_SPECIALTY="$ROOT/backend/src/main/resources/static/assets/png"
OUT_INSTITUTIONAL="$ROOT/backend/src/main/resources/static/logo-institucional.png"
OUT_SCA_COLOR="$ROOT/backend/src/main/resources/static/logo-sca-color.png"
mkdir -p "$OUT_SPECIALTY"

# Convierte manteniendo el aspecto original (solo se fija el ancho).
convert_svg_keep_aspect() {
  local svg="$1"
  local out="$2"
  local width="${3:-480}"
  if command -v rsvg-convert >/dev/null 2>&1; then
    rsvg-convert -w "$width" "$svg" -o "$out"
  elif command -v inkscape >/dev/null 2>&1; then
    inkscape "$svg" --export-type=png --export-filename="$out" --export-width="$width"
  else
    echo "No se encontró rsvg-convert ni inkscape. Instala librsvg2-bin o inkscape." >&2
    exit 2
  fi
}

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

institutional_svg="$SOURCE/logos-colegio/institucional.svg"
if [ ! -f "$institutional_svg" ]; then
  echo "Falta el SVG institucional esperado: $institutional_svg" >&2
  exit 2
fi
convert_svg "$institutional_svg" "$OUT_INSTITUTIONAL"
echo "Converted $institutional_svg -> $OUT_INSTITUTIONAL"

# Logo SCA a color para el header de los reportes PDF de padres.
sca_color_svg="$SOURCE/logos-sca/logo_SCA_color.svg"
if [ ! -f "$sca_color_svg" ]; then
  echo "Falta el SVG a color esperado: $sca_color_svg" >&2
  exit 2
fi
convert_svg_keep_aspect "$sca_color_svg" "$OUT_SCA_COLOR" 480
echo "Converted $sca_color_svg -> $OUT_SCA_COLOR"

for svg in "$SOURCE"/logos-especialidad/*.svg; do
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
