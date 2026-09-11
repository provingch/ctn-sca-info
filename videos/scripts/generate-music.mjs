// Procedurally synthesizes an original, royalty-free background score for the
// SCA trailer (no samples, no external audio) and writes it as a WAV file.
import { writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";

const SAMPLE_RATE = 44100;
const TOTAL_SECONDS = 24.3;
const TOTAL_SAMPLES = Math.ceil(TOTAL_SECONDS * SAMPLE_RATE);

// I - V - vi - IV in C major, 2 seconds (one bar at 120bpm) per chord.
const CHORD_DURATION = 2;
const PROGRESSION = [
  { pad: [130.81, 164.81, 195.998, 261.63], arp: [261.63, 329.63, 391.995, 523.25] }, // C
  { pad: [98.0, 146.83, 195.998, 246.94], arp: [293.66, 391.995, 493.88, 587.33] }, // G
  { pad: [110.0, 164.81, 220.0, 261.63], arp: [220.0, 261.63, 329.63, 440.0] }, // Am
  { pad: [87.31, 130.81, 174.61, 220.0], arp: [174.61, 220.0, 261.63, 349.23] }, // F
];

const left = new Float64Array(TOTAL_SAMPLES);
const right = new Float64Array(TOTAL_SAMPLES);

const clamp01 = (x) => Math.max(0, Math.min(1, x));
const smoothstep = (a, b, x) => {
  const t = clamp01((x - a) / (b - a));
  return t * t * (3 - 2 * t);
};

// Hann-ish window so notes fade in/out without clicks.
const window_ = (tLocal, dur, attack, release) => {
  if (tLocal < attack) return smoothstep(0, attack, tLocal);
  if (tLocal > dur - release) return smoothstep(dur, dur - release, tLocal);
  return 1;
};

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
      0.5 * Math.sin(2 * Math.PI * f1 * tLocal) +
      0.5 * Math.sin(2 * Math.PI * f2 * tLocal) +
      0.18 * Math.sin(2 * Math.PI * f1 * 2 * tLocal); // soft octave harmonic for warmth
    buf[idx] += amp * env * wave;
  }
};

const addKick = (buf, startSample, amp) => {
  const dur = 0.22;
  const durSamples = Math.round(dur * SAMPLE_RATE);
  for (let i = 0; i < durSamples; i++) {
    const idx = startSample + i;
    if (idx < 0 || idx >= buf.length) continue;
    const tLocal = i / SAMPLE_RATE;
    const freq = 130 * Math.exp(-tLocal * 22) + 42;
    const env = Math.exp(-tLocal * 14);
    buf[idx] += amp * env * Math.sin(2 * Math.PI * freq * tLocal);
  }
};

const addHat = (buf, startSample, amp) => {
  const dur = 0.05;
  const durSamples = Math.round(dur * SAMPLE_RATE);
  for (let i = 0; i < durSamples; i++) {
    const idx = startSample + i;
    if (idx < 0 || idx >= buf.length) continue;
    const tLocal = i / SAMPLE_RATE;
    const env = Math.exp(-tLocal * 90);
    buf[idx] += amp * env * (Math.random() * 2 - 1);
  }
};

const padGainAt = (t) => {
  if (t < 6) return smoothstep(0.5, 6, t) * 0.55;
  if (t < 18) return 0.55;
  return 0.55 * (1 - smoothstep(18, 22.5, t)) + 0.22 * (1 - smoothstep(22.5, TOTAL_SECONDS, t));
};

const arpGainAt = (t) => {
  if (t < 4) return 0;
  if (t < 6) return smoothstep(4, 6, t) * 0.32;
  if (t < 18) return 0.32;
  return 0.32 * (1 - smoothstep(18, 20, t));
};

const percActiveAt = (t) => t >= 6 && t < 18.2;

// Pad + arpeggio, chord by chord.
for (let barStart = 0; barStart < TOTAL_SECONDS; barStart += CHORD_DURATION) {
  const chordIndex = Math.floor(barStart / CHORD_DURATION) % PROGRESSION.length;
  const chord = PROGRESSION[chordIndex];
  const startSample = Math.round(barStart * SAMPLE_RATE);
  const dur = Math.min(CHORD_DURATION, TOTAL_SECONDS - barStart);

  const midT = barStart + dur / 2;
  const gain = padGainAt(midT);
  if (gain > 0.001) {
    chord.pad.forEach((freq, voice) => {
      const pan = voice % 2 === 0 ? 0.62 : 0.38;
      addOscillator(left, startSample, freq, dur, gain * (1 - pan) * 2 * 0.5, 0.35, 0.35, 0.0025);
      addOscillator(right, startSample, freq, dur, gain * pan * 2 * 0.5, 0.35, 0.35, 0.0025);
    });
  }

  const eighth = 0.25;
  for (let t = 0; t < dur; t += eighth) {
    const tAbs = barStart + t;
    const g = arpGainAt(tAbs);
    if (g <= 0.001) continue;
    const note = chord.arp[Math.floor(t / eighth) % chord.arp.length];
    const s = Math.round(tAbs * SAMPLE_RATE);
    addOscillator(left, s, note, eighth * 1.6, g * 0.55, 0.005, 0.16);
    addOscillator(right, s, note, eighth * 1.6, g * 0.45, 0.005, 0.16);
  }
}

// Percussion: kick on beats 1 & 3, closed hat on every eighth note.
for (let t = 0; t < TOTAL_SECONDS; t += 0.5) {
  if (!percActiveAt(t)) continue;
  const beatInBar = (t / 0.5) % 4;
  if (beatInBar === 0 || beatInBar === 2) {
    const s = Math.round(t * SAMPLE_RATE);
    addKick(left, s, 0.5);
    addKick(right, s, 0.5);
  }
}
for (let t = 0.25; t < TOTAL_SECONDS; t += 0.25) {
  if (!percActiveAt(t)) continue;
  const s = Math.round(t * SAMPLE_RATE);
  addHat(left, s, 0.09);
  addHat(right, s, 0.09);
}

// Final swell + master fade out over the last 0.6s.
const fadeOutStart = TOTAL_SECONDS - 0.6;
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
const softClip = (x) => Math.tanh(x * 1.15);

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
  "trailer-theme.wav",
);
writeWav(outPath, pcm, SAMPLE_RATE);
console.log(`Wrote ${outPath} (${TOTAL_SECONDS}s)`);
