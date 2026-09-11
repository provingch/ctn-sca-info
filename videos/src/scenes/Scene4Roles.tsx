import {
  AbsoluteFill,
  Easing,
  Interactive,
  interpolate,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { Background } from "../components/Background";
import { fontFamily, theme } from "../theme";

const roles = [
  { level: 1, name: "Profesor", detail: "Planillas y plan curricular" },
  { level: 2, name: "Evaluador", detail: "Aprobación y seguimiento" },
  { level: 3, name: "Administrador", detail: "Panel de gestión completo" },
  { level: 4, name: "Padre / Encargado", detail: "Notas y resumen" },
  { level: 5, name: "Coord. Pedagógica", detail: "Desempeño docente" },
];

export const Scene4Roles: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  return (
    <AbsoluteFill
      style={{ fontFamily, justifyContent: "center", alignItems: "center" }}
    >
      <Background />
      <AbsoluteFill
        style={{
          justifyContent: "center",
          alignItems: "center",
          gap: 70,
        }}
      >
        <Interactive.Div
          name="Headline"
          style={{
            fontSize: 80,
            fontWeight: 800,
            letterSpacing: "-0.02em",
            color: theme.text,
            textAlign: "center",
            opacity: interpolate(frame, [0, 0.5 * fps], [0, 1], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            }),
            translate: interpolate(
              frame,
              [0, 0.5 * fps],
              ["0px 28px", "0px 0px"],
              {
                extrapolateLeft: "clamp",
                extrapolateRight: "clamp",
                easing: Easing.bezier(0.16, 1, 0.3, 1),
              },
            ),
          }}
        >
          Un rol para cada usuario
        </Interactive.Div>

        <div
          style={{
            display: "flex",
            flexDirection: "row",
            gap: 26,
            padding: "0 130px",
          }}
        >
          {roles.map((role, i) => {
            const start = 0.65 * fps + i * 0.14 * fps;
            const opacity = interpolate(
              frame,
              [start, start + 0.4 * fps],
              [0, 1],
              { extrapolateLeft: "clamp", extrapolateRight: "clamp" },
            );
            const scale = interpolate(
              frame,
              [start, start + 0.4 * fps],
              [0.8, 1],
              {
                extrapolateLeft: "clamp",
                extrapolateRight: "clamp",
                easing: Easing.bezier(0.16, 1, 0.3, 1),
                output: "perceptual-scale",
              },
            );
            return (
              <Interactive.Div
                key={role.name}
                name={`Role ${role.name}`}
                style={{
                  opacity,
                  scale,
                  width: 300,
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  gap: 16,
                  padding: "34px 20px",
                  borderRadius: 20,
                  border: `1px solid ${theme.border}`,
                  background: "rgba(145, 160, 255, 0.06)",
                }}
              >
                <div
                  style={{
                    width: 52,
                    height: 52,
                    borderRadius: "50%",
                    background: theme.accent,
                    color: theme.text,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    fontSize: 26,
                    fontWeight: 800,
                  }}
                >
                  {role.level}
                </div>
                <div
                  style={{
                    fontSize: 30,
                    fontWeight: 700,
                    color: theme.text,
                    textAlign: "center",
                    lineHeight: 1.2,
                  }}
                >
                  {role.name}
                </div>
                <div
                  style={{
                    fontSize: 22,
                    fontWeight: 500,
                    color: theme.textMuted,
                    textAlign: "center",
                  }}
                >
                  {role.detail}
                </div>
              </Interactive.Div>
            );
          })}
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
