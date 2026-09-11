import { TransitionSeries, linearTiming } from "@remotion/transitions";
import { fade } from "@remotion/transitions/fade";
import { Scene1Intro } from "./scenes/Scene1Intro";
import { Scene2Problem } from "./scenes/Scene2Problem";
import { Scene3Features } from "./scenes/Scene3Features";
import { Scene4Roles } from "./scenes/Scene4Roles";
import { Scene5Outro } from "./scenes/Scene5Outro";

const TRANSITION = 15;

export const TrailerVideo: React.FC = () => {
  return (
    <TransitionSeries>
      <TransitionSeries.Sequence durationInFrames={90} name="Intro">
        <Scene1Intro />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition
        presentation={fade()}
        timing={linearTiming({ durationInFrames: TRANSITION })}
      />
      <TransitionSeries.Sequence durationInFrames={100} name="Problem">
        <Scene2Problem />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition
        presentation={fade()}
        timing={linearTiming({ durationInFrames: TRANSITION })}
      />
      <TransitionSeries.Sequence durationInFrames={220} name="Features">
        <Scene3Features />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition
        presentation={fade()}
        timing={linearTiming({ durationInFrames: TRANSITION })}
      />
      <TransitionSeries.Sequence durationInFrames={160} name="Roles">
        <Scene4Roles />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition
        presentation={fade()}
        timing={linearTiming({ durationInFrames: TRANSITION })}
      />
      <TransitionSeries.Sequence durationInFrames={110} name="Outro">
        <Scene5Outro />
      </TransitionSeries.Sequence>
    </TransitionSeries>
  );
};

export const TRAILER_TOTAL_DURATION = 90 + 100 + 220 + 160 + 110 - TRANSITION * 4;
