#!/usr/bin/env bash
# Busca datos personales visibles (CI, IP, teléfono, correo) en grabaciones: OCR de un cuadro cada medio segundo.
# Uso: scripts/revisar-pii.sh public/tutoriales/<rol>/<tramo>.webm ...   → imprime los cuadros sospechosos (y sale con 1 si hay).
set -uo pipefail
PATRON='[[:alnum:]._+-]+@[[:alnum:]-]+\.[[:alnum:].]+|\b[0-9]{1,3}(\.[0-9]{1,3}){3}\b|\b[0-9]{1,2}\.?[0-9]{3}\.?[0-9]{3}\b|\b[0-9]{6,8}\b|\b09[0-9]{2}[ -]?[0-9]{3}[ -]?[0-9]{3}\b'
hallazgos=0
for video in "$@"; do
  tmp=$(mktemp -d)
  ffmpeg -v error -i "$video" -vf fps=2 "$tmp/%05d.png"
  for f in "$tmp"/*.png; do
    texto=$(tesseract "$f" - -l afr --psm 11 2>/dev/null)
    # las fechas (07/10/2026) y horas no son datos personales: se descartan antes de buscar
    m=$(printf '%s' "$texto" | sed -E 's#[0-9]{2}/[0-9]{2}/[0-9]{4}##g' | grep -oE "$PATRON" | head -3 | tr '\n' ' ')
    if [ -n "$m" ]; then
      seg=$(( ($(basename "$f" .png | sed 's/^0*//') - 1) / 2 ))
      echo "$video  ~${seg}s  →  $m"
      hallazgos=1
    fi
  done
  rm -rf "$tmp"
done
exit $hallazgos
