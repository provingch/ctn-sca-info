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

const pills = ["Horarios", "Planillas", "Plan curricular", "Notas"];

export const Scene2Problem: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  return (
    <AbsoluteFill
      style={{
        fontFamily,
        justifyContent: "center",
        alignItems: "center",
      }}
    >
      <Background />
      <AbsoluteFill
        style={{
          justifyContent: "center",
          alignItems: "center",
          padding: "0 160px",
          gap: 56,
        }}
      >
        <Interactive.Div
          name="Headline"
          style={{
            fontSize: 92,
            fontWeight: 800,
            letterSpacing: "-0.02em",
            color: theme.text,
            textAlign: "center",
            lineHeight: 1.15,
            opacity: interpolate(frame, [0, 0.55 * fps], [0, 1], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            }),
            translate: interpolate(
              frame,
              [0, 0.55 * fps],
              ["0px 30px", "0px 0px"],
              {
                extrapolateLeft: "clamp",
                extrapolateRight: "clamp",
                easing: Easing.bezier(0.16, 1, 0.3, 1),
              },
            ),
          }}
        >
          Toda la gestión académica del CTN,{" "}
          <span style={{ color: theme.accentLight }}>en un solo lugar</span>
        </Interactive.Div>
        <div
          style={{
            display: "flex",
            flexDirection: "row",
            justifyContent: "center",
            alignItems: "center",
            gap: 22,
          }}
        >
          {pills.map((pill, i) => {
            const start = 0.75 * fps + i * 0.16 * fps;
            const opacity = interpolate(
              frame,
              [start, start + 0.35 * fps],
              [0, 1],
              {
                extrapolateLeft: "clamp",
                extrapolateRight: "clamp",
              },
            );
            const scale = interpolate(
              frame,
              [start, start + 0.35 * fps],
              [0.85, 1],
              {
                extrapolateLeft: "clamp",
                extrapolateRight: "clamp",
                easing: Easing.bezier(0.16, 1, 0.3, 1),
                output: "perceptual-scale",
              },
            );
            return (
              <Interactive.Div
                key={pill}
                name={`Pill ${pill}`}
                style={{
                  opacity,
                  scale,
                  padding: "18px 34px",
                  borderRadius: 999,
                  border: `1px solid ${theme.border}`,
                  background: "rgba(145, 160, 255, 0.08)",
                  color: theme.text,
                  fontSize: 32,
                  fontWeight: 600,
                }}
              >
                {pill}
              </Interactive.Div>
            );
          })}
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
