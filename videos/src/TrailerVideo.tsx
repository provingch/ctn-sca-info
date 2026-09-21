import { Audio } from "@remotion/media";
import { AbsoluteFill, interpolate, staticFile } from "remotion";
import { TransitionSeries, linearTiming } from "@remotion/transitions";
import { fade } from "@remotion/transitions/fade";
import { Scene1Intro } from "./scenes/Scene1Intro";
import { SceneSolutionReveal } from "./scenes/SceneSolutionReveal";
import { Scene2Problem } from "./scenes/Scene2Problem";
import { Scene3Features, SCENE3_FEATURES_DURATION } from "./scenes/Scene3Features";
import { Scene5Outro } from "./scenes/Scene5Outro";

const TRANSITION = 15;
const SCENE_DURATIONS = [240, 200, 180, SCENE3_FEATURES_DURATION, 150];

// Absolute frame windows (in the top-level composition timeline) where a
// narration Audio is playing in one of the scenes above, so the background
// music can duck under it. Kept in sync with each scene's own
// NARRATION_START constant + its narration file's real duration.
const NARRATION_WINDOWS: [number, number][] = [
  [12, 197], // Scene1Intro: 01-problema.wav
  [235, 384], // SceneSolutionReveal: 02-solucion.wav
  [420, 536], // Scene2Problem: 03-gestion.wav
  [1619, 1720], // Scene5Outro: 04-outro.wav
];

const MUSIC_VOLUME = 0.75;
const DUCKED_VOLUME = 0.22;
const DUCK_FADE = 20;

const duckedVolume = (frame: number) => {
  for (const [start, end] of NARRATION_WINDOWS) {
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
const musicVolume = (frame: number) =>
  duckedVolume(frame) *
  interpolate(frame, [TRAILER_TOTAL_DURATION - END_FADE, TRAILER_TOTAL_DURATION], [1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

export const TrailerVideo: React.FC = () => {
  return (
    <AbsoluteFill>
      <Audio src={staticFile("audio/trailer-theme-cheto.mp3")} volume={musicVolume} />
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
          <Scene3Features />
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

export const TRAILER_TOTAL_DURATION =
  SCENE_DURATIONS.reduce((a, b) => a + b, 0) - TRANSITION * (SCENE_DURATIONS.length - 1);
