import { Video } from "@remotion/media";
import {
  AbsoluteFill,
  Easing,
  Interactive,
  Sequence,
  interpolate,
  staticFile,
  useCurrentFrame,
} from "remotion";
import { Background } from "../components/Background";
import { ScreenMockup } from "../components/ScreenMockup";
import { fontFamily, theme } from "../theme";

const FADE = 10;

const RealClip: React.FC<{
  src: string;
  title: string;
  durationInFrames: number;
}> = ({ src, title, durationInFrames }) => {
  const frame = useCurrentFrame();
  const opacity = interpolate(
    frame,
    [0, FADE, durationInFrames - FADE, durationInFrames],
    [0, 1, 1, 0],
    {
      extrapolateLeft: "clamp",
      extrapolateRight: "clamp",
      easing: Easing.bezier(0.16, 1, 0.3, 1),
    },
  );

  return (
    <Interactive.Div
      name={`Real clip ${title}`}
      style={{
        position: "absolute",
        opacity,
        filter: `drop-shadow(0 30px 60px ${theme.accent}22)`,
      }}
    >
      <ScreenMockup title={title} width={1180} contentPadding={0}>
        <Video
          src={staticFile(src)}
          muted
          style={{ width: "100%", display: "block" }}
        />
      </ScreenMockup>
    </Interactive.Div>
  );
};

export const SceneRealFootage: React.FC = () => {
  const frame = useCurrentFrame();

  const adminFrom = 10;
  const adminDuration = 165;
  const evalFrom = adminFrom + adminDuration - 12;
  const evalDuration = 150;
  const profesorFrom = evalFrom + evalDuration - 12;
  const profesorDuration = 240;
  const padresFrom = profesorFrom + profesorDuration - 12;
  const padresDuration = 240;

  return (
    <AbsoluteFill
      style={{ fontFamily, justifyContent: "center", alignItems: "center" }}
    >
      <Background />
      <Interactive.Div
        name="Real footage label"
        style={{
          position: "absolute",
          top: 90,
          fontSize: 28,
          fontWeight: 700,
          letterSpacing: "0.16em",
          textTransform: "uppercase",
          color: theme.accentLight,
          opacity: interpolate(frame, [0, 12], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        Así se ve por dentro
      </Interactive.Div>
      <AbsoluteFill style={{ justifyContent: "center", alignItems: "center" }}>
        <Sequence from={adminFrom} durationInFrames={adminDuration} layout="none">
          <RealClip
            src="videos/safe/admin_montage.mp4"
            title="SCA · Panel de Administración"
            durationInFrames={adminDuration}
          />
        </Sequence>
        <Sequence from={evalFrom} durationInFrames={evalDuration} layout="none">
          <RealClip
            src="videos/safe/eval_montage.mp4"
            title="SCA · Panel de Evaluación"
            durationInFrames={evalDuration}
          />
        </Sequence>
        <Sequence from={profesorFrom} durationInFrames={profesorDuration} layout="none">
          <RealClip
            src="videos/safe/profesor_montage.mp4"
            title="SCA · Panel de Profesor"
            durationInFrames={profesorDuration}
          />
        </Sequence>
        <Sequence from={padresFrom} durationInFrames={padresDuration} layout="none">
          <RealClip
            src="videos/safe/padres_montage.mp4"
            title="SCA · Panel de Familias"
            durationInFrames={padresDuration}
          />
        </Sequence>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};

export const SCENE_REAL_FOOTAGE_DURATION = 10 + 165 + 150 - 12 + 240 - 12 + 240 - 12;
