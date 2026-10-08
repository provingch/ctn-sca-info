// Genera la narración de cada tramo (Gemini TTS, misma voz que el trailer) y completa tramos.json con las duraciones.
// Uso: node scripts/narrar-tutoriales.mjs [rol ...]   ·   SIN_VOZ=1 node ... solo mide los videos   (requiere GOOGLE_API_KEY o Vertex; ver gemini_tts.py)
import fs from 'node:fs';
import { execFileSync } from 'node:child_process';

const RAIZ = new URL('../', import.meta.url).pathname;
const VOZ = process.env.VOICE ?? 'Kore';
const ESTILO = 'Explicá con tono claro, amable y tranquilo, como en un video tutorial, a ritmo natural:';
const PY = process.env.PY ?? RAIZ + '.venv-tts/bin/python';
const duracion = f => +execFileSync('ffprobe', ['-v', 'error', '-show_entries', 'format=duration', '-of', 'csv=p=0', f], { encoding: 'utf8' }).trim();

const roles = process.argv.slice(2).length ? process.argv.slice(2) : fs.readdirSync(RAIZ + 'public/tutoriales', { withFileTypes: true }).filter(d => d.isDirectory()).map(d => d.name);
for (const rol of roles) {
  const dir = `${RAIZ}public/tutoriales/${rol}/`;
  const indice = dir + 'tramos.json';
  const tramos = JSON.parse(fs.readFileSync(indice, 'utf8'));
  for (const [id, t] of Object.entries(tramos)) {
    const wav = dir + id + '.wav';
    // se regenera solo si cambió el texto
    // SIN_VOZ=1: solo medir los videos (la voz se agrega después)
    if (!process.env.SIN_VOZ && (!fs.existsSync(wav) || t.textoNarrado !== t.texto)) {
      for (let intento = 1; ; intento++) {
        try {
          execFileSync(PY, [RAIZ + 'scripts/gemini_tts.py', '--voice', VOZ, '--style', ESTILO, '--text', t.texto, '--output', wav], { stdio: 'inherit' });
          break;
        } catch (e) { if (intento === 3) throw e; console.error(`reintento ${id} (${intento}/3)`); }
      }
      t.textoNarrado = t.texto;
    }
    if (fs.existsSync(wav) && t.textoNarrado === t.texto) {
      t.audio = `tutoriales/${rol}/${id}.wav`;
      t.durAudio = +duracion(wav).toFixed(2);
    } else { delete t.audio; delete t.durAudio; }
    t.durVideo = +duracion(dir + id + '.webm').toFixed(2);
    console.log(`${rol}/${id}: video ${(t.durVideo - t.inicio).toFixed(1)} s útil, voz ${t.durAudio ?? '—'} s`);
  }
  fs.writeFileSync(indice, JSON.stringify(tramos, null, 1));
}
