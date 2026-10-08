import "./index.css";
import { Composition, Folder } from "remotion";
import { Scene1Intro } from "./scenes/Scene1Intro";
import { SceneSolutionReveal } from "./scenes/SceneSolutionReveal";
import { Scene2Problem } from "./scenes/Scene2Problem";
import { Scene3Features, SCENE3_FEATURES_DURATION } from "./scenes/Scene3Features";
import { Scene5Outro } from "./scenes/Scene5Outro";
import { TrailerVideo, TRAILER_TOTAL_DURATION, trailerDuration } from "./TrailerVideo";
import { Tutorial, calcularTutorial } from "./Tutorial";

const TUTORIALES = [
  ["profesor", "Profesor"],
  ["evaluador", "Evaluador"],
  ["coordinacion", "Coordinación Pedagógica"],
  ["administrador", "Administrador"],
  ["padres", "Padres y encargados"],
] as const;

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
          durationInFrames={240}
          fps={FPS}
          width={WIDTH}
          height={HEIGHT}
        />
        <Composition
          id="SceneSolutionReveal"
          component={SceneSolutionReveal}
          durationInFrames={200}
          fps={FPS}
          width={WIDTH}
          height={HEIGHT}
        />
        <Composition
          id="Scene2Problem"
          component={Scene2Problem}
          durationInFrames={180}
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
          durationInFrames={150}
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
      {/* Copia del trailer con voz también en cada perfil de usuario. */}
      <Composition
        id="SCA-Trailer-Voces"
        component={TrailerVideo}
        defaultProps={{ voces: true }}
        durationInFrames={trailerDuration(true)}
        fps={FPS}
        width={WIDTH}
        height={HEIGHT}
      />
      {/* Un tutorial por perfil; la duración sale de public/tutoriales/<rol>/tramos.json. */}
      <Folder name="Tutoriales">
        {TUTORIALES.map(([rol, titulo]) => (
          <Composition
            key={rol}
            id={`Tutorial-${rol}`}
            component={Tutorial}
            calculateMetadata={calcularTutorial}
            defaultProps={{ rol, titulo, tramos: [] }}
            durationInFrames={1}
            fps={FPS}
            width={WIDTH}
            height={HEIGHT}
          />
        ))}
      </Folder>
    </>
  );
};
