// Música de fondo para los tutoriales, sintetizada por código (sin muestras): acordes suaves + arpegio liviano,
// sin percusión, para que no compita con la voz. Uso: node scripts/generar-musica-tutorial.mjs <segundos> <salida.wav> [semilla]
import { writeFileSync } from 'node:fs';

const [, , segArg, salida, semillaArg] = process.argv;
const SR = 44100;
const SEG = Number(segArg);
if (!SEG || !salida) throw new Error('Uso: node scripts/generar-musica-tutorial.mjs <segundos> <salida.wav> [semilla]');
const N = Math.ceil(SEG * SR);

// Progresión I–vi–IV–V en Do, voicings abiertos; la semilla rota el orden para que cada perfil suene distinto.
const ACORDES = [
  [130.81, 196.0, 261.63, 329.63],  // C
  [110.0, 164.81, 220.0, 261.63],   // Am
  [87.31, 174.61, 220.0, 261.63],   // F
  [98.0, 146.83, 196.0, 246.94],    // G
  [87.31, 130.81, 220.0, 329.63],   // Fmaj7
  [98.0, 146.83, 246.94, 293.66],   // G6
];
const rot = Number(semillaArg ?? 0) % ACORDES.length;
const prog = [...ACORDES.slice(rot), ...ACORDES.slice(0, rot)];
const COMPAS = 4.0; // segundos por acorde (tempo tranquilo, 60 bpm)

const izq = new Float32Array(N), der = new Float32Array(N);
const env = (t, a, r, dur) => Math.min(1, t / a) * Math.min(1, Math.max(0, (dur - t) / r));
for (let i = 0; i < N; i++) {
  const t = i / SR;
  const k = Math.floor(t / COMPAS), tl = t - k * COMPAS;
  const [bajo, ...pad] = prog[k % prog.length];
  // pad: senos levemente desafinados (coro) con envolvente lenta por acorde
  const ePad = env(tl, 1.2, 1.2, COMPAS);
  let s = 0;
  for (const f of pad) s += Math.sin(2 * Math.PI * f * t) + 0.6 * Math.sin(2 * Math.PI * f * 1.003 * t);
  let l = 0.045 * ePad * s, r = 0.045 * ePad * s;
  // bajo suave
  const b = 0.09 * env(tl, 0.3, 1.5, COMPAS) * Math.sin(2 * Math.PI * bajo * t);
  l += b; r += b;
  // arpegio: una nota por medio tiempo, alternando canales
  const paso = Math.floor(tl / 0.5), tn = tl - paso * 0.5;
  const nota = pad[paso % pad.length] * 2;
  const a = 0.05 * Math.exp(-tn * 5) * Math.sin(2 * Math.PI * nota * t);
  if (paso % 2) l += a; else r += a;
  izq[i] = l; der[i] = r;
}
// entrada y salida graduales del tema completo
const FADE = 2.5 * SR;
for (let i = 0; i < N; i++) { const g = Math.min(1, i / FADE, (N - i) / FADE); izq[i] *= g; der[i] *= g; }
let pico = 1e-9;
for (let i = 0; i < N; i++) pico = Math.max(pico, Math.abs(izq[i]), Math.abs(der[i]));

const buf = Buffer.alloc(44 + N * 4);
buf.write('RIFF', 0); buf.writeUInt32LE(36 + N * 4, 4); buf.write('WAVEfmt ', 8);
buf.writeUInt32LE(16, 16); buf.writeUInt16LE(1, 20); buf.writeUInt16LE(2, 22); buf.writeUInt32LE(SR, 24);
buf.writeUInt32LE(SR * 4, 28); buf.writeUInt16LE(4, 32); buf.writeUInt16LE(16, 34); buf.write('data', 36); buf.writeUInt32LE(N * 4, 40);
for (let i = 0; i < N; i++) {
  buf.writeInt16LE(Math.round((izq[i] / pico) * 0.8 * 32767), 44 + i * 4);
  buf.writeInt16LE(Math.round((der[i] / pico) * 0.8 * 32767), 46 + i * 4);
}
writeFileSync(salida, buf);
console.log(`✓ ${salida} (${SEG.toFixed(1)} s)`);
