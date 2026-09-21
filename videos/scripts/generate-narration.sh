#!/usr/bin/env bash
# Regenera las 4 narraciones del trailer con Gemini TTS (misma voz en las cuatro, para que no salte el timbre).
# Requiere: pip install google-genai, y GOOGLE_API_KEY (AI Studio) o GOOGLE_CLOUD_PROJECT + gcloud auth (Vertex).
# Uso: ./scripts/generate-narration.sh   (VOICE=Achird para probar otra voz)
# Después, pegar las ventanas que imprime al final en NARRATION_WINDOWS de src/TrailerVideo.tsx.
set -euo pipefail
cd "$(dirname "$0")/.."

OUT=public/audio/narration
VOICE="${VOICE:-Kore}"
STYLE="Di con tono cálido, seguro y publicitario, a ritmo ágil y natural, sin pausas largas:"

tts() { python3 scripts/gemini_tts.py --voice "$VOICE" --style "$STYLE" --text "$2" --output "$OUT/$1"; }

tts 01-problema.wav "Horarios en una planilla, notas en otra, el plan curricular perdido entre carpetas. Coordinar un colegio técnico no tendría que ser así."
tts 02-solucion.wav "Te presentamos SCA: el Sistema de Carpeta Académica del Colegio Técnico Nacional de Asunción."
tts 03-gestion.wav "Horarios, planillas, plan curricular y notas: toda la gestión académica del colegio, en un solo lugar, al instante."
tts 04-outro.wav "Instalalo como app, activá las notificaciones, y llevá el CTN siempre con vos."

# Inicio absoluto de cada escena en SCA-Trailer y NARRATION_START de cada una (ver SCENE_DURATIONS/TRANSITION en
# src/TrailerVideo.tsx y NARRATION_START en cada escena). Actualizar si cambia la duración de alguna escena anterior.
# Límite = frames que le quedan a la voz dentro de su escena (descontando la transición de salida, salvo el outro).
echo
echo "// NARRATION_WINDOWS (pegar en src/TrailerVideo.tsx):"
for spec in "01-problema 0 12 293" "02-solucion 305 10 225" "03-gestion 540 10 245" "04-outro 1830 9 211"; do
  read -r name start offset limit <<<"$spec"
  secs=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$OUT/$name.wav")
  frames=$(python3 -c "print(round($secs * 30))")
  from=$((start + offset))
  note=""
  if [ "$frames" -gt "$limit" ]; then note="  // OJO: dura $frames f y la escena deja $limit f: alargar la escena"; fi
  echo "  [$from, $((from + frames))], // $name.wav ($frames f)$note"
done
