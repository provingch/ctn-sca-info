import "./index.css";
import { Composition, Folder } from "remotion";
import { Scene1Intro } from "./scenes/Scene1Intro";
import { SceneSolutionReveal } from "./scenes/SceneSolutionReveal";
import { Scene2Problem } from "./scenes/Scene2Problem";
import { Scene3Features, SCENE3_FEATURES_DURATION } from "./scenes/Scene3Features";
import { Scene5Outro } from "./scenes/Scene5Outro";
import { TrailerVideo, TRAILER_TOTAL_DURATION } from "./TrailerVideo";

const WIDTH = 1920;
const HEIGHT = 1080;
const FPS = 30;

export const RemotionRoot: React.FC = () => {
  return (
    <>
      <Folder name="Trailer-Scenes">
        <Composition
          id="Scene1Intro"
          component={Scene1Intro}
          durationInFrames={320}
          fps={FPS}
          width={WIDTH}
          height={HEIGHT}
        />
        <Composition
          id="SceneSolutionReveal"
          component={SceneSolutionReveal}
          durationInFrames={250}
          fps={FPS}
          width={WIDTH}
          height={HEIGHT}
        />
        <Composition
          id="Scene2Problem"
          component={Scene2Problem}
          durationInFrames={270}
          fps={FPS}
          width={WIDTH}
          height={HEIGHT}
        />
        <Composition
          id="Scene3Features"
          component={Scene3Features}
          durationInFrames={SCENE3_FEATURES_DURATION}
          fps={FPS}
          width={WIDTH}
          height={HEIGHT}
        />
        <Composition
          id="Scene5Outro"
          component={Scene5Outro}
          durationInFrames={220}
          fps={FPS}
          width={WIDTH}
          height={HEIGHT}
        />
      </Folder>
      <Composition
        id="SCA-Trailer"
        component={TrailerVideo}
        durationInFrames={TRAILER_TOTAL_DURATION}
        fps={FPS}
        width={WIDTH}
        height={HEIGHT}
      />
    </>
  );
};
