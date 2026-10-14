"""Alinea las frases del guion con la narración: transcribe con Whisper (local, con tiempo por palabra) y empareja
la secuencia de palabras del guion con la transcripción. Devuelve inicio y fin de cada frase en el audio.

Uso: .venv-tts/bin/python scripts/alinear.py <audio.wav> <frases.json>   (frases.json = lista de textos)
Imprime JSON: [{"inicio": s, "fin": s}, ...] una entrada por frase.
"""
import difflib
import json
import re
import subprocess
import sys
import unicodedata

import numpy as np
from faster_whisper import WhisperModel


def normalizar(palabra: str) -> str:
    sin_tildes = unicodedata.normalize("NFD", palabra.lower())
    sin_tildes = "".join(c for c in sin_tildes if unicodedata.category(c) != "Mn")
    return re.sub(r"[^a-z0-9ñ]", "", sin_tildes)


audio, frases_json = sys.argv[1], sys.argv[2]
frases = json.load(open(frases_json, encoding="utf-8"))

modelo = WhisperModel("base", device="cpu", compute_type="int8")
# se decodifica con ffmpeg (16 kHz mono) y se pasan las muestras: evita depender de la versión de PyAV
crudo = subprocess.run(["ffmpeg", "-v", "error", "-i", audio, "-f", "f32le", "-ac", "1", "-ar", "16000", "-"], check=True, capture_output=True).stdout
muestras = np.frombuffer(crudo, dtype=np.float32)
segmentos, _ = modelo.transcribe(muestras, language="es", word_timestamps=True, vad_filter=False)
oidas = [(normalizar(w.word), w.start, w.end) for s in segmentos for w in s.words if normalizar(w.word)]

guion = []  # (palabra, índice de frase)
for i, f in enumerate(frases):
    guion += [(normalizar(w), i) for w in f.split() if normalizar(w)]

# emparejar palabra a palabra: cada palabra del guion que coincide con una oída hereda su tiempo
pares = difflib.SequenceMatcher(a=[g[0] for g in guion], b=[o[0] for o in oidas], autojunk=False).get_matching_blocks()
tiempos = {}
for bloque in pares:
    for k in range(bloque.size):
        tiempos[bloque.a + k] = oidas[bloque.b + k]

resultado = []
for i in range(len(frases)):
    idx = [k for k, g in enumerate(guion) if g[1] == i and k in tiempos]
    if not idx:
        resultado.append(None)
        continue
    resultado.append({"inicio": tiempos[idx[0]][1], "fin": tiempos[idx[-1]][2], "cobertura": len(idx) / sum(1 for g in guion if g[1] == i)})

# frases sin ninguna palabra reconocida: se ubican entre sus vecinas
for i, r in enumerate(resultado):
    if r is None:
        prev = next((x["fin"] for x in reversed(resultado[:i]) if x), 0.0)
        nxt = next((x["inicio"] for x in resultado[i + 1:] if x), prev)
        resultado[i] = {"inicio": prev, "fin": nxt, "cobertura": 0.0}
print(json.dumps(resultado))
