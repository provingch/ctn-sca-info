#!/usr/bin/env bash
# Voces de los 5 perfiles para SCA-Trailer-Voces (misma voz y estilo que generate-narration.sh).
# Requiere GOOGLE_API_KEY (AI Studio) o GOOGLE_CLOUD_PROJECT + gcloud auth (Vertex).
# Después, pegar el array que imprime al final en PERFIL_VOICE_FRAMES de src/scenes/Scene3Features.tsx.
set -euo pipefail
cd "$(dirname "$0")/.."

OUT=public/audio/narration
VOICE="${VOICE:-Kore}"
STYLE="Di con tono cálido, seguro y publicitario, a ritmo ágil y natural, sin pausas largas:"
PY="${PY:-.venv-tts/bin/python}"

tts() {
  for intento in 1 2 3; do
    "$PY" scripts/gemini_tts.py --voice "$VOICE" --style "$STYLE" --text "$2" --output "$OUT/$1" && return 0
    echo "Falló $1 (intento $intento/3)" >&2
    sleep 3
  done
  return 1
}

tts 05-profesores.wav "Los profesores suben tareas y notas sincronizadas con Classroom, y llevan el plan curricular y el libro de cátedra al día."
tts 06-evaluacion.wav "Evaluación aprueba los planes curriculares, sigue el cumplimiento de cada profesor y descarga las planillas de cada curso."
tts 07-coordinacion.wav "Coordinación Pedagógica gestiona cada queja de principio a fin y administra los códigos de conducta."
tts 08-administracion.wav "Administración maneja usuarios, horarios, salas y cursos desde un solo panel."
tts 09-familias.wav "Y las familias siguen las notas de sus hijos desde el celular, con un aviso por cada calificación nueva."

echo
printf "export const PERFIL_VOICE_FRAMES = ["
sep=""
for f in 05-profesores 06-evaluacion 07-coordinacion 08-administracion 09-familias; do
  secs=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$OUT/$f.wav")
  printf "%s%s" "$sep" "$(python3 -c "import math; print(math.ceil($secs * 30))")"
  sep=", "
done
echo "];"
