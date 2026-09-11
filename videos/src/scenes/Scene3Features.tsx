import {
  AbsoluteFill,
  Easing,
  Interactive,
  interpolate,
  useCurrentFrame,
} from "remotion";
import { Background } from "../components/Background";
import { fontFamily, theme } from "../theme";

const features = [
  {
    title: "Libro de Cátedra",
    body: "Horario, plan curricular y verificación de tema por clase",
  },
  {
    title: "Control académico",
    body: "Incumplimientos, quejas y seguimiento de Coordinación Pedagógica",
  },
  {
    title: "Portal para padres",
    body: "Resumen académico y notas del alumno en tiempo real",
  },
  {
    title: "Google Classroom",
    body: "Sincronización automática de tareas, notas y cursos",
  },
];

const SLOT = 55;
const FADE = 14;

export const Scene3Features: React.FC = () => {
  const frame = useCurrentFrame();

  return (
    <AbsoluteFill style={{ fontFamily, justifyContent: "center", alignItems: "center" }}>
      <Background />
      <Interactive.Div
        name="Section label"
        style={{
          position: "absolute",
          top: 110,
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

      {features.map((feature, i) => {
        const localStart = i * SLOT;
        const localFrame = frame - localStart;
        const isLast = i === features.length - 1;

        const opacity = interpolate(
          localFrame,
          isLast
            ? [0, FADE]
            : [0, FADE, SLOT - FADE, SLOT],
          isLast ? [0, 1] : [0, 1, 1, 0],
          {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          },
        );
        const translateY = interpolate(localFrame, [0, FADE], [40, 0], {
          extrapolateLeft: "clamp",
          extrapolateRight: "clamp",
          easing: Easing.bezier(0.16, 1, 0.3, 1),
        });
        const underlineWidth = interpolate(
          localFrame,
          [FADE, FADE + 20],
          [0, 160],
          { extrapolateLeft: "clamp", extrapolateRight: "clamp" },
        );

        return (
          <Interactive.Div
            key={feature.title}
            name={`Feature card ${i + 1}`}
            style={{
              position: "absolute",
              opacity,
              translate: `0px ${translateY}px`,
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              gap: 26,
              maxWidth: 1180,
              textAlign: "center",
            }}
          >
            <div
              style={{
                fontSize: 30,
                fontWeight: 700,
                color: theme.accentLight,
                letterSpacing: "0.05em",
              }}
            >
              {String(i + 1).padStart(2, "0")}
            </div>
            <div
              style={{
                fontSize: 82,
                fontWeight: 800,
                color: theme.text,
                letterSpacing: "-0.02em",
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
                fontSize: 38,
                fontWeight: 500,
                color: theme.textMuted,
                lineHeight: 1.35,
              }}
            >
              {feature.body}
            </div>
          </Interactive.Div>
        );
      })}

      <div
        style={{
          position: "absolute",
          bottom: 110,
          display: "flex",
          gap: 14,
        }}
      >
        {features.map((feature, i) => {
          const localStart = i * SLOT;
          const active = frame >= localStart && frame < localStart + SLOT;
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
