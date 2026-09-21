// Procedurally synthesizes a second, more upscale/lounge original score for the
// SCA trailer (no samples, no external audio) and writes it as a WAV file.
import { writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";

const SAMPLE_RATE = 44100;
// Tiene que cubrir todo el trailer (TRAILER_TOTAL_DURATION / 30 en src/TrailerVideo.tsx): si es más corta, la música
// se corta en seco. Uso: TOTAL_SECONDS=58.7 node scripts/generate-music-cheto.mjs
const TOTAL_SECONDS = Number(process.env.TOTAL_SECONDS ?? 58.7);
const TOTAL_SAMPLES = Math.ceil(TOTAL_SECONDS * SAMPLE_RATE);

// Extended 8-chord lounge/jazz progression in C major at ~84bpm, so a
// longer track doesn't just loop the same 4 bars over and over. Bar = 4
// beats, beat = 60/84s ≈ 0.714s -> bar ≈ 2.857s.
const CHORD_DURATION = 2.857;
const PROGRESSION = [
  // Dm9 (bass D2)
  {
    bass: 73.42,
    pad: [146.83, 174.61, 220.0, 261.63, 293.66],
    arp: [293.66, 349.23, 440.0, 523.25, 587.33],
  },
  // G13 (bass G2)
  {
    bass: 98.0,
    pad: [146.83, 174.61, 220.0, 293.66, 392.0],
    arp: [392.0, 440.0, 493.88, 587.33, 659.25],
  },
  // Cmaj9 (bass C2)
  {
    bass: 65.41,
    pad: [130.81, 164.81, 246.94, 293.66, 329.63],
    arp: [329.63, 392.0, 440.0, 493.88, 587.33],
  },
  // Am9 (bass A1)
  {
    bass: 55.0,
    pad: [110.0, 130.81, 164.81, 246.94, 293.66],
    arp: [293.66, 329.63, 392.0, 440.0, 493.88],
  },
  // Fmaj9 (bass F2) — middle-eight lift.
  {
    bass: 87.31,
    pad: [174.61, 220.0, 261.63, 329.63, 392.0],
    arp: [349.23, 392.0, 440.0, 523.25, 659.25],
  },
  // Bm7b5 (bass B1) — ii of Am, adds tension before the turnaround.
  {
    bass: 61.74,
    pad: [123.47, 146.83, 174.61, 220.0, 293.66],
    arp: [293.66, 349.23, 415.3, 440.0, 523.25],
  },
  // E7 (bass E2) — V of Am, resolves into the recap.
  {
    bass: 82.41,
    pad: [164.81, 207.65, 246.94, 293.66, 329.63],
    arp: [329.63, 415.3, 493.88, 587.33, 659.25],
  },
  // Am9 (bass A1) — recap resolve before the loop returns to Dm9.
  {
    bass: 55.0,
    pad: [110.0, 130.81, 164.81, 246.94, 293.66],
    arp: [293.66, 329.63, 392.0, 440.0, 493.88],
  },
];

const left = new Float64Array(TOTAL_SAMPLES);
const right = new Float64Array(TOTAL_SAMPLES);

const clamp01 = (x) => Math.max(0, Math.min(1, x));
const smoothstep = (a, b, x) => {
  const t = clamp01((x - a) / (b - a));
  return t * t * (3 - 2 * t);
};

const window_ = (tLocal, dur, attack, release) => {
  if (tLocal < attack) return smoothstep(0, attack, tLocal);
  if (tLocal > dur - release) return smoothstep(dur, dur - release, tLocal);
  return 1;
};

// Warm, rounded tone (sine + soft undertone) for pads and the rhodes-style arp.
const addOscillator = (buf, startSample, freq, dur, amp, attack, release, detune = 0) => {
  const durSamples = Math.min(Math.round(dur * SAMPLE_RATE), buf.length - startSample);
  if (durSamples <= 0) return;
  for (let i = 0; i < durSamples; i++) {
    const idx = startSample + i;
    if (idx < 0 || idx >= buf.length) continue;
    const tLocal = i / SAMPLE_RATE;
    const env = window_(tLocal, dur, attack, release);
    const f1 = freq * (1 + detune);
    const f2 = freq * (1 - detune);
    const wave =
      0.55 * Math.sin(2 * Math.PI * f1 * tLocal) +
      0.45 * Math.sin(2 * Math.PI * f2 * tLocal) +
      0.12 * Math.sin(2 * Math.PI * f1 * 2 * tLocal) -
      0.05 * Math.sin(2 * Math.PI * f1 * 3 * tLocal);
    buf[idx] += amp * env * wave;
  }
};

// Soft, rounded sub bass note (no click, long release).
const addBass = (buf, startSample, freq, dur, amp) => {
  const durSamples = Math.min(Math.round(dur * SAMPLE_RATE), buf.length - startSample);
  if (durSamples <= 0) return;
  for (let i = 0; i < durSamples; i++) {
    const idx = startSample + i;
    if (idx < 0 || idx >= buf.length) continue;
    const tLocal = i / SAMPLE_RATE;
    const env = window_(tLocal, dur, 0.4, 0.6);
    const wave =
      Math.sin(2 * Math.PI * freq * tLocal) + 0.15 * Math.sin(2 * Math.PI * freq * 2 * tLocal);
    buf[idx] += amp * env * wave;
  }
};

// Brushed shaker: quiet, filtered-feel noise burst (no drum-machine punch).
const addBrush = (buf, startSample, amp) => {
  const dur = 0.09;
  const durSamples = Math.round(dur * SAMPLE_RATE);
  let prev = 0;
  for (let i = 0; i < durSamples; i++) {
    const idx = startSample + i;
    if (idx < 0 || idx >= buf.length) continue;
    const tLocal = i / SAMPLE_RATE;
    const env = Math.exp(-tLocal * 32) * smoothstep(0, 0.006, tLocal);
    const noise = Math.random() * 2 - 1;
    prev = prev * 0.72 + noise * 0.28; // gentle low-pass so it stays soft
    buf[idx] += amp * env * prev;
  }
};

const ENERGETIC_END = TOTAL_SECONDS - 6.5;

const padGainAt = (t) => {
  if (t < 5) return smoothstep(0.6, 5, t) * 0.5;
  if (t < ENERGETIC_END) return 0.5;
  return (
    0.5 * (1 - smoothstep(ENERGETIC_END, ENERGETIC_END + 5, t)) +
    0.2 * (1 - smoothstep(ENERGETIC_END + 5, TOTAL_SECONDS, t))
  );
};

const arpGainAt = (t) => {
  if (t < 3) return 0;
  if (t < 5) return smoothstep(3, 5, t) * 0.26;
  if (t < ENERGETIC_END) return 0.26;
  return 0.26 * (1 - smoothstep(ENERGETIC_END, ENERGETIC_END + 2.5, t));
};

const bassGainAt = (t) => {
  if (t < 4) return 0;
  if (t < 6) return smoothstep(4, 6, t) * 0.4;
  if (t < ENERGETIC_END) return 0.4;
  return 0.4 * (1 - smoothstep(ENERGETIC_END, ENERGETIC_END + 3, t));
};

const brushActiveAt = (t) => t >= 6 && t < ENERGETIC_END + 0.2;

// Pad + rhodes-style arpeggio + bass, chord by chord.
for (let barStart = 0; barStart < TOTAL_SECONDS; barStart += CHORD_DURATION) {
  const chordIndex = Math.floor(barStart / CHORD_DURATION) % PROGRESSION.length;
  const chord = PROGRESSION[chordIndex];
  const startSample = Math.round(barStart * SAMPLE_RATE);
  const dur = Math.min(CHORD_DURATION, TOTAL_SECONDS - barStart);

  const midT = barStart + dur / 2;

  const padGain = padGainAt(midT);
  if (padGain > 0.001) {
    chord.pad.forEach((freq, voice) => {
      const pan = voice % 2 === 0 ? 0.6 : 0.4;
      addOscillator(left, startSample, freq, dur, padGain * (1 - pan) * 2 * 0.42, 0.6, 0.6, 0.003);
      addOscillator(right, startSample, freq, dur, padGain * pan * 2 * 0.42, 0.6, 0.6, 0.003);
    });
  }

  const bassGain = bassGainAt(midT);
  if (bassGain > 0.001) {
    addBass(left, startSample, chord.bass, dur, bassGain * 0.5);
    addBass(right, startSample, chord.bass, dur, bassGain * 0.5);
  }

  // Sparse, syncopated triplet-feel arpeggio (not a strict grid) for a looser lounge feel.
  const steps = [0, 0.5, 0.95, 1.6, 2.1, 2.5];
  steps.forEach((offset, i) => {
    if (offset >= dur) return;
    const tAbs = barStart + offset;
    const g = arpGainAt(tAbs);
    if (g <= 0.001) return;
    const note = chord.arp[i % chord.arp.length];
    const s = Math.round(tAbs * SAMPLE_RATE);
    const noteDur = 0.85;
    addOscillator(left, s, note, noteDur, g * 0.5, 0.02, 0.5);
    addOscillator(right, s, note, noteDur, g * 0.5, 0.02, 0.5);
  });
}

// Brushed shaker on the off-beats, very quiet — texture, not a beat.
for (let t = 0.714 / 2; t < TOTAL_SECONDS; t += 0.714) {
  if (!brushActiveAt(t)) continue;
  const s = Math.round(t * SAMPLE_RATE);
  addBrush(left, s, 0.05);
  addBrush(right, s, 0.05);
}

// Single soft echo tap for a touch of space/reverb (no feedback, so it stays clean).
const delaySamples = Math.round(0.32 * SAMPLE_RATE);
const echoDecay = 0.22;
for (let i = TOTAL_SAMPLES - 1; i >= delaySamples; i--) {
  left[i] += left[i - delaySamples] * echoDecay;
  right[i] += right[i - delaySamples] * echoDecay;
}

// Final swell + master fade out over the last 0.8s.
const fadeOutStart = TOTAL_SECONDS - 0.8;
for (let i = 0; i < TOTAL_SAMPLES; i++) {
  const t = i / SAMPLE_RATE;
  const fade = t > fadeOutStart ? 1 - smoothstep(fadeOutStart, TOTAL_SECONDS, t) : 1;
  left[i] *= fade;
  right[i] *= fade;
}

// Normalize, then soft-clip for safety headroom.
let peak = 0;
for (let i = 0; i < TOTAL_SAMPLES; i++) {
  peak = Math.max(peak, Math.abs(left[i]), Math.abs(right[i]));
}
const norm = peak > 0 ? 0.85 / peak : 1;
const softClip = (x) => Math.tanh(x * 1.1);

const pcm = new Int16Array(TOTAL_SAMPLES * 2);
for (let i = 0; i < TOTAL_SAMPLES; i++) {
  const l = softClip(left[i] * norm);
  const r = softClip(right[i] * norm);
  pcm[i * 2] = Math.round(l * 32767);
  pcm[i * 2 + 1] = Math.round(r * 32767);
}

// Minimal 44-byte WAV header for 16-bit stereo PCM.
const writeWav = (filePath, samples, sampleRate) => {
  const byteRate = sampleRate * 2 * 2;
  const dataSize = samples.length * 2;
  const buffer = Buffer.alloc(44 + dataSize);
  buffer.write("RIFF", 0);
  buffer.writeUInt32LE(36 + dataSize, 4);
  buffer.write("WAVE", 8);
  buffer.write("fmt ", 12);
  buffer.writeUInt32LE(16, 16);
  buffer.writeUInt16LE(1, 20); // PCM
  buffer.writeUInt16LE(2, 22); // channels
  buffer.writeUInt32LE(sampleRate, 24);
  buffer.writeUInt32LE(byteRate, 28);
  buffer.writeUInt16LE(4, 32); // block align
  buffer.writeUInt16LE(16, 34); // bits per sample
  buffer.write("data", 36);
  buffer.writeUInt32LE(dataSize, 40);
  for (let i = 0; i < samples.length; i++) {
    buffer.writeInt16LE(samples[i], 44 + i * 2);
  }
  writeFileSync(filePath, buffer);
};

const outPath = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
  "public",
  "audio",
  "trailer-theme-cheto.wav",
);
writeWav(outPath, pcm, SAMPLE_RATE);
console.log(`Wrote ${outPath} (${TOTAL_SECONDS}s)`);
