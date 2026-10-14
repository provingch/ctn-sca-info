// Narración de un tutorial con UNA sola llamada de voz (Gemini TTS): todas las frases del guion, una por párrafo.
// Después se ubica cada frase en el audio con Whisper (alinear.py) y se corta: un archivo por frase, con su duración.
// Uso: node scripts/narrar-tutorial.mjs <rol>     (GOOGLE_API_KEY o GOOGLE_API_KEY2 en el entorno; ver gemini_tts.py)
// Salida: public/tutoriales/<rol>/voz/NN.wav + voz/linea.json  { pasos: [{ id, texto, archivo, dur }] }
import fs from 'node:fs';
import crypto from 'node:crypto';
import { execFileSync } from 'node:child_process';

const [, , rol] = process.argv;
const { escenas } = await import(`./guiones/${rol}.mjs`);
const RAIZ = new URL('../', import.meta.url).pathname;
const DIR = `${RAIZ}public/tutoriales/${rol}/voz/`;
fs.mkdirSync(DIR, { recursive: true });
const PY = process.env.PY ?? RAIZ + '.venv-tts/bin/python';

const pasos = escenas.flatMap(e => e.pasos.map((p, k) => ({ id: `${e.id}.${k + 1}`, texto: p.voz })));
const texto = pasos.map(p => p.texto).join('\n\n');
const huella = crypto.createHash('sha1').update(texto).digest('hex');
const completo = DIR + 'completo.wav';

// la llamada de voz se repite solo si cambió algún texto (la cuota diaria es chica)
if (!fs.existsSync(completo) || fs.readFileSync(DIR + 'huella', 'utf8').trim() !== huella) {
  const estilo = 'Leé con acento rioplatense (voseo: tenés, podés, elegí), como un video tutorial: tono claro, amable y tranquilo, ritmo natural sin apurarte. '
    + 'Entre cada párrafo hacé una pausa larga de un segundo y medio; dentro de cada párrafo, pausas normales.';
  execFileSync(PY, [RAIZ + 'scripts/gemini_tts.py', '--voice', process.env.VOICE ?? 'Kore', '--style', estilo, '--text', texto, '--output', completo], { stdio: 'inherit' });
  fs.writeFileSync(DIR + 'huella', huella);
}

// Cada frase se ubica en el audio con Whisper (alinear.py): tiempos por palabra, sin depender de que la voz haga pausas.
fs.writeFileSync(DIR + 'frases.json', JSON.stringify(pasos.map(p => p.texto)));
const tramos = JSON.parse(execFileSync(PY, [RAIZ + 'scripts/alinear.py', completo, DIR + 'frases.json'], { encoding: 'utf8', maxBuffer: 1 << 24 }));
const total = +execFileSync('ffprobe', ['-v', 'error', '-show_entries', 'format=duration', '-of', 'csv=p=0', completo], { encoding: 'utf8' });

// volumen cada 10 ms (RMS) para ubicar el punto más silencioso entre el fin de una frase y el inicio de la siguiente
const pcm = new Float32Array(execFileSync('ffmpeg', ['-v', 'error', '-i', completo, '-f', 'f32le', '-ac', '1', '-ar', '16000', '-'], { maxBuffer: 1 << 30 }).buffer.slice(0));
const rms = t => { const a = Math.floor(t * 16000), b = Math.min(pcm.length, a + 160); let x = 0; for (let k = a; k < b; k++) x += pcm[k] * pcm[k]; return x; };
const cortes = tramos.slice(0, -1).map((tr, i) => {
  // se busca desde un poco antes del fin de esta frase hasta un poco antes del inicio (marcado) de la siguiente
  const a = tr.fin - 0.1, b = Math.max(a + 0.05, tramos[i + 1].inicio - 0.05);
  let mejor = a, min = Infinity;
  for (let t = a; t <= b; t += 0.01) { const v = rms(t); if (v < min) { min = v; mejor = t; } }
  return mejor;
});
const linea = { huella, pasos: [] };
for (const [i, p] of pasos.entries()) {
  const archivo = `${String(i + 1).padStart(2, '0')}.wav`;
  // el corte entre dos frases va en el punto más silencioso del hueco entre ellas (Whisper suele marcar el inicio tarde)
  const desde = i ? cortes[i - 1] : 0;
  const hasta = i < pasos.length - 1 ? cortes[i] : total;
  execFileSync('ffmpeg', ['-v', 'error', '-ss', desde.toFixed(3), '-to', hasta.toFixed(3), '-i', completo, '-y', DIR + archivo]);
  linea.pasos.push({ id: p.id, texto: p.texto, archivo: `tutoriales/${rol}/voz/${archivo}`, dur: +(hasta - desde).toFixed(2), cobertura: +tramos[i].cobertura.toFixed(2) });
}

// Control: segundos por carácter de cada frase. Un corte en el lugar equivocado deja una frase muy corta y otra muy larga.
const ritmo = linea.pasos.map(p => p.dur / p.texto.length);
const mediana = [...ritmo].sort((a, b) => a - b)[Math.floor(ritmo.length / 2)];
const raros = linea.pasos.filter((p, i) => ritmo[i] < mediana * 0.55 || ritmo[i] > mediana * 1.8 || p.cobertura < 0.6);
fs.writeFileSync(DIR + 'linea.json', JSON.stringify(linea, null, 1));
console.log(`✓ ${rol}: ${linea.pasos.length} frases, ${total.toFixed(1)} s de voz`);
if (raros.length) {
  console.error('⚠ frases con duración sospechosa (revisar el corte):');
  for (const p of raros) console.error(`  ${p.id} ${p.dur}s  "${p.texto.slice(0, 60)}…"`);
  process.exit(2);
}
