import {
  AbsoluteFill,
  Easing,
  Interactive,
  interpolate,
  useCurrentFrame,
} from "remotion";
import { Background } from "../components/Background";
import { ScreenMockup } from "../components/ScreenMockup";
import { HorarioMock, PlanillaMock, PadresMock } from "../components/mockups";
import { fontFamily, theme } from "../theme";

const features = [
  {
    title: "Libro de Cátedra",
    body: "Horario, plan curricular y verificación de tema por clase",
    screen: { title: "SCA · Horario", content: <HorarioMock /> },
  },
  {
    title: "Planillas y notas",
    body: "Carga de tareas y calificaciones por curso, período y sección",
    screen: { title: "SCA · Planilla", content: <PlanillaMock /> },
  },
  {
    title: "Portal para padres",
    body: "Resumen académico y notas del alumno en tiempo real",
    screen: { title: "SCA · Portal de padres", content: <PadresMock /> },
  },
  {
    title: "Control académico",
    body: "Incumplimientos, quejas y seguimiento de Coordinación Pedagógica",
    screen: null,
  },
  {
    title: "Google Classroom",
    body: "Sincronización automática de tareas, notas y cursos",
    screen: null,
  },
];

const SLOT = 65;
const FADE = 16;

export const Scene3Features: React.FC = () => {
  const frame = useCurrentFrame();

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

      {features.map((feature, i) => {
        const localStart = i * SLOT;
        const localFrame = frame - localStart;
        const isLast = i === features.length - 1;

        const opacity = interpolate(
          localFrame,
          isLast ? [0, FADE] : [0, FADE, SLOT - FADE, SLOT],
          isLast ? [0, 1] : [0, 1, 1, 0],
          {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          },
        );
        const hasScreen = feature.screen !== null;

        const textTranslateX = interpolate(
          localFrame,
          [0, FADE],
          [hasScreen ? -50 : 0, 0],
          {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          },
        );
        const underlineWidth = interpolate(
          localFrame,
          [FADE, FADE + 20],
          [0, 140],
          { extrapolateLeft: "clamp", extrapolateRight: "clamp" },
        );

        const screenRotateY = interpolate(localFrame, [0, FADE + 10], [22, -6], {
          extrapolateLeft: "clamp",
          extrapolateRight: "clamp",
          easing: Easing.bezier(0.16, 1, 0.3, 1),
        });
        const screenScale = interpolate(localFrame, [0, FADE + 10], [0.82, 1], {
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
            key={feature.title}
            name={`Feature card ${i + 1}`}
            style={{
              position: "absolute",
              opacity,
              display: "flex",
              flexDirection: hasScreen ? "row" : "column",
              alignItems: "center",
              justifyContent: "center",
              gap: hasScreen ? 90 : 26,
              maxWidth: 1700,
              padding: "0 100px",
            }}
          >
            <div
              style={{
                display: "flex",
                flexDirection: "column",
                alignItems: hasScreen ? "flex-start" : "center",
                textAlign: hasScreen ? "left" : "center",
                gap: 22,
                width: hasScreen ? 560 : "auto",
                maxWidth: hasScreen ? 560 : 1180,
                translate: `${textTranslateX}px 0px`,
              }}
            >
              <div
                style={{
                  fontSize: 26,
                  fontWeight: 700,
                  color: theme.accentLight,
                  letterSpacing: "0.05em",
                }}
              >
                {String(i + 1).padStart(2, "0")}
              </div>
              <div
                style={{
                  fontSize: hasScreen ? 60 : 82,
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
                  fontSize: hasScreen ? 30 : 38,
                  fontWeight: 500,
                  color: theme.textMuted,
                  lineHeight: 1.35,
                }}
              >
                {feature.body}
              </div>
            </div>
            {feature.screen && (
              <div
                style={{
                  opacity: screenOpacity,
                  scale: screenScale,
                  transform: `perspective(1600px) rotateX(3deg) rotateY(${screenRotateY}deg)`,
                  filter: `drop-shadow(0 30px 60px ${theme.accent}22)`,
                }}
              >
                <ScreenMockup title={feature.screen.title} width={640}>
                  {feature.screen.content}
                </ScreenMockup>
              </div>
            )}
          </Interactive.Div>
        );
      })}

      <div
        style={{
          position: "absolute",
          bottom: 90,
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
