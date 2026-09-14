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

const features = [
  {
    title: "Libro de Cátedra",
    body: "Horario, plan curricular y verificación de tema por clase",
    screen: { title: "SCA · Panel de Profesor", videoSrc: "videos/safe/profesor_montage.mp4" },
    duration: 130,
  },
  {
    title: "Planillas y notas",
    body: "Carga de tareas y calificaciones por curso, período y sección",
    screen: { title: "SCA · Panel de Evaluación", videoSrc: "videos/safe/eval_montage.mp4" },
    duration: 130,
  },
  {
    title: "Portal para padres",
    body: "Resumen académico y notas del alumno en tiempo real",
    screen: { title: "SCA · Panel de Familias", videoSrc: "videos/safe/padres_montage.mp4" },
    duration: 130,
  },
  {
    title: "Control académico",
    body: "Incumplimientos, quejas y seguimiento de Coordinación Pedagógica",
    screen: { title: "SCA · Panel de Administración", videoSrc: "videos/safe/admin_montage.mp4" },
    duration: 130,
  },
  {
    title: "Google Classroom",
    body: "Sincronización automática de tareas, notas y cursos",
    screen: null,
    duration: 90,
  },
] as const;

export const SCENE3_FEATURES_DURATION = features.reduce((a, f) => a + f.duration, 0);

const FADE = 16;

const FeatureSlide: React.FC<{
  feature: (typeof features)[number];
  index: number;
  isLast: boolean;
}> = ({ feature, index, isLast }) => {
  const localFrame = useCurrentFrame();
  const slotDuration = feature.duration;
  const hasScreen = feature.screen !== null;

  const opacity = interpolate(
    localFrame,
    isLast
      ? [0, FADE]
      : [0, FADE, slotDuration - FADE, slotDuration],
    isLast ? [0, 1] : [0, 1, 1, 0],
    {
      extrapolateLeft: "clamp",
      extrapolateRight: "clamp",
      easing: Easing.bezier(0.16, 1, 0.3, 1),
    },
  );

  const underlineWidth = interpolate(localFrame, [FADE, FADE + 20], [0, 140], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  const screenScale = interpolate(localFrame, [0, FADE + 10], [0.9, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.16, 1, 0.3, 1),
    output: "perceptual-scale",
  });
  const screenOpacity = interpolate(localFrame, [4, FADE + 6], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  return (
    <Interactive.Div
      name={`Feature card ${index + 1}`}
      style={{
        position: "absolute",
        opacity,
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        gap: hasScreen ? 28 : 26,
        maxWidth: 1780,
        padding: "0 60px",
      }}
    >
      {feature.screen && (
        <div
          style={{
            opacity: screenOpacity,
            scale: screenScale,
            transform: "perspective(1600px) rotateX(2deg)",
            filter: `drop-shadow(0 40px 80px ${theme.accent}28)`,
          }}
        >
          <ScreenMockup title={feature.screen.title} width={1180} contentPadding={0}>
            <Video
              src={staticFile(feature.screen.videoSrc)}
              muted
              style={{ width: "100%", display: "block" }}
            />
          </ScreenMockup>
        </div>
      )}
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          textAlign: "center",
          gap: hasScreen ? 8 : 22,
        }}
      >
        <div
          style={{
            fontSize: hasScreen ? 20 : 26,
            fontWeight: 700,
            color: theme.accentLight,
            letterSpacing: "0.05em",
          }}
        >
          {String(index + 1).padStart(2, "0")}
        </div>
        <div
          style={{
            fontSize: hasScreen ? 40 : 82,
            fontWeight: 800,
            color: theme.text,
            letterSpacing: "-0.02em",
            lineHeight: 1.1,
          }}
        >
          {feature.title}
        </div>
        <div
          style={{
            width: underlineWidth,
            height: 5,
            borderRadius: 3,
            background: theme.accent,
          }}
        />
        <div
          style={{
            fontSize: hasScreen ? 22 : 38,
            fontWeight: 500,
            color: theme.textMuted,
            lineHeight: 1.3,
            maxWidth: hasScreen ? 900 : 1180,
          }}
        >
          {feature.body}
        </div>
      </div>
    </Interactive.Div>
  );
};

export const Scene3Features: React.FC = () => {
  const frame = useCurrentFrame();

  let cursor = 0;
  const starts = features.map((f) => {
    const start = cursor;
    cursor += f.duration;
    return start;
  });

  return (
    <AbsoluteFill style={{ fontFamily, justifyContent: "center", alignItems: "center" }}>
      <Background />
      <Interactive.Div
        name="Section label"
        style={{
          position: "absolute",
          top: 90,
          fontSize: 28,
          fontWeight: 700,
          letterSpacing: "0.16em",
          textTransform: "uppercase",
          color: theme.accentLight,
          opacity: interpolate(frame, [0, 15], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        Funcionalidades clave
      </Interactive.Div>

      {features.map((feature, i) => (
        <Sequence
          key={feature.title}
          from={starts[i]}
          durationInFrames={feature.duration}
          layout="none"
        >
          <FeatureSlide feature={feature} index={i} isLast={i === features.length - 1} />
        </Sequence>
      ))}

      <div
        style={{
          position: "absolute",
          bottom: 90,
          display: "flex",
          gap: 14,
        }}
      >
        {features.map((feature, i) => {
          const active = frame >= starts[i] && frame < starts[i] + feature.duration;
          return (
            <div
              key={feature.title}
              style={{
                width: active ? 34 : 14,
                height: 8,
                borderRadius: 4,
                background: active ? theme.accentLight : theme.border,
              }}
            />
          );
        })}
      </div>
    </AbsoluteFill>
  );
};
