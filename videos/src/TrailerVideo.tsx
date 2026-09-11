import { Audio } from "@remotion/media";
import { AbsoluteFill, staticFile } from "remotion";
import { TransitionSeries, linearTiming } from "@remotion/transitions";
import { fade } from "@remotion/transitions/fade";
import { Scene1Intro } from "./scenes/Scene1Intro";
import { Scene2Problem } from "./scenes/Scene2Problem";
import { Scene3Features } from "./scenes/Scene3Features";
import { Scene4Roles } from "./scenes/Scene4Roles";
import { Scene5Outro } from "./scenes/Scene5Outro";

const TRANSITION = 15;
const SCENE_DURATIONS = [90, 100, 325, 160, 110];

export const TrailerVideo: React.FC = () => {
  return (
    <AbsoluteFill>
      <Audio src={staticFile("audio/trailer-theme.mp3")} volume={0.75} />
      <TransitionSeries>
        <TransitionSeries.Sequence durationInFrames={SCENE_DURATIONS[0]} name="Intro">
          <Scene1Intro />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={fade()}
          timing={linearTiming({ durationInFrames: TRANSITION })}
        />
        <TransitionSeries.Sequence durationInFrames={SCENE_DURATIONS[1]} name="Problem">
          <Scene2Problem />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={fade()}
          timing={linearTiming({ durationInFrames: TRANSITION })}
        />
        <TransitionSeries.Sequence durationInFrames={SCENE_DURATIONS[2]} name="Features">
          <Scene3Features />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={fade()}
          timing={linearTiming({ durationInFrames: TRANSITION })}
        />
        <TransitionSeries.Sequence durationInFrames={SCENE_DURATIONS[3]} name="Roles">
          <Scene4Roles />
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
