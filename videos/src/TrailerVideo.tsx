import { Audio } from "@remotion/media";
import { AbsoluteFill, interpolate, staticFile } from "remotion";
import { TransitionSeries, linearTiming } from "@remotion/transitions";
import { fade } from "@remotion/transitions/fade";
import { Scene1Intro } from "./scenes/Scene1Intro";
import { SceneSolutionReveal } from "./scenes/SceneSolutionReveal";
import { Scene2Problem } from "./scenes/Scene2Problem";
import { NARRATION_START as PERFIL_NARRATION_START, PERFIL_VOICE_FRAMES, Scene3Features, perfilDurations, scene3Duration } from "./scenes/Scene3Features";
import { Scene5Outro } from "./scenes/Scene5Outro";

const TRANSITION = 15;
const sceneDurations = (voces: boolean) => [240, 200, 180, scene3Duration(voces), 150];

// Absolute frame windows (in the top-level composition timeline) where a
// narration Audio is playing in one of the scenes above, so the background
// music can duck under it. Kept in sync with each scene's own
// NARRATION_START constant + its narration file's real duration.
const BASE_WINDOWS: [number, number][] = [
  [12, 197], // Scene1Intro: 01-problema.wav
  [235, 384], // SceneSolutionReveal: 02-solucion.wav
  [420, 536], // Scene2Problem: 03-gestion.wav
];
const OUTRO_NARRATION: [number, number] = [9, 110]; // Scene5Outro: 04-outro.wav, relativo al inicio de la escena

const narrationWindows = (voces: boolean): [number, number][] => {
  const d = sceneDurations(voces);
  const start = (i: number) => d.slice(0, i).reduce((a, b) => a + b, 0) - TRANSITION * i;
  const perfiles: [number, number][] = [];
  if (voces) {
    let cursor = start(3);
    perfilDurations(true).forEach((dur, i) => {
      perfiles.push([cursor + PERFIL_NARRATION_START, cursor + PERFIL_NARRATION_START + PERFIL_VOICE_FRAMES[i]]);
      cursor += dur;
    });
  }
  return [...BASE_WINDOWS, ...perfiles, [start(4) + OUTRO_NARRATION[0], start(4) + OUTRO_NARRATION[1]]];
};

const MUSIC_VOLUME = 0.75;
const DUCKED_VOLUME = 0.22;
const DUCK_FADE = 20;

const duckedVolume = (frame: number, windows: [number, number][]) => {
  for (const [start, end] of windows) {
    if (frame < start - DUCK_FADE || frame > end + DUCK_FADE) continue;
    if (frame < start) {
      return interpolate(frame, [start - DUCK_FADE, start], [MUSIC_VOLUME, DUCKED_VOLUME], {
        extrapolateLeft: "clamp",
        extrapolateRight: "clamp",
      });
    }
    if (frame > end) {
      return interpolate(frame, [end, end + DUCK_FADE], [DUCKED_VOLUME, MUSIC_VOLUME], {
        extrapolateLeft: "clamp",
        extrapolateRight: "clamp",
      });
    }
    return DUCKED_VOLUME;
  }
  return MUSIC_VOLUME;
};

// Fundido de salida: la música nunca termina en seco, aunque la pista sea justo del largo del video.
const END_FADE = 45;
const musicVolume = (frame: number, voces: boolean) =>
  duckedVolume(frame, narrationWindows(voces)) *
  interpolate(frame, [trailerDuration(voces) - END_FADE, trailerDuration(voces)], [1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

export const TrailerVideo: React.FC<{ voces?: boolean }> = ({ voces = false }) => {
  const SCENE_DURATIONS = sceneDurations(voces);
  return (
    <AbsoluteFill>
      <Audio src={staticFile("audio/trailer-theme-cheto.mp3")} volume={(f) => musicVolume(f, voces)} />
      <TransitionSeries>
        <TransitionSeries.Sequence durationInFrames={SCENE_DURATIONS[0]} name="Intro">
          <Scene1Intro />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={fade()}
          timing={linearTiming({ durationInFrames: TRANSITION })}
        />
        <TransitionSeries.Sequence durationInFrames={SCENE_DURATIONS[1]} name="SolutionReveal">
          <SceneSolutionReveal />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={fade()}
          timing={linearTiming({ durationInFrames: TRANSITION })}
        />
        <TransitionSeries.Sequence durationInFrames={SCENE_DURATIONS[2]} name="Problem">
          <Scene2Problem />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={fade()}
          timing={linearTiming({ durationInFrames: TRANSITION })}
        />
        <TransitionSeries.Sequence durationInFrames={SCENE_DURATIONS[3]} name="Features">
          <Scene3Features voces={voces} />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={fade()}
          timing={linearTiming({ durationInFrames: TRANSITION })}
        />
        <TransitionSeries.Sequence durationInFrames={SCENE_DURATIONS[4]} name="Outro">
          <Scene5Outro />
        </TransitionSeries.Sequence>
      </TransitionSeries>
    </AbsoluteFill>
  );
};

export const trailerDuration = (voces: boolean) => {
  const d = sceneDurations(voces);
  return d.reduce((a, b) => a + b, 0) - TRANSITION * (d.length - 1);
};
export const TRAILER_TOTAL_DURATION = trailerDuration(false);
