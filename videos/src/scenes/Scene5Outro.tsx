import {
  AbsoluteFill,
  Easing,
  Interactive,
  interpolate,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { Background } from "../components/Background";
import { Logo } from "../components/Logo";
import { fontFamily, theme } from "../theme";

const tags = ["PWA instalable", "Notificaciones push", "Multiplataforma"];

export const Scene5Outro: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  return (
    <AbsoluteFill
      style={{ fontFamily, justifyContent: "center", alignItems: "center" }}
    >
      <Background />
      <AbsoluteFill
        style={{ justifyContent: "center", alignItems: "center", gap: 40 }}
      >
        <Interactive.Div
          name="Outro logo"
          style={{
            opacity: interpolate(frame, [0, 0.4 * fps], [0, 1], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
            }),
            scale: interpolate(frame, [0, 0.4 * fps], [0.8, 1], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
              output: "perceptual-scale",
            }),
            filter: `drop-shadow(0 0 34px ${theme.accent}66)`,
          }}
        >
          <Logo size={92} color={theme.accentLight} />
        </Interactive.Div>
        <Interactive.Div
          name="Outro headline"
          style={{
            fontSize: 78,
            fontWeight: 800,
            letterSpacing: "-0.02em",
            color: theme.text,
            textAlign: "center",
            opacity: interpolate(frame, [0.3 * fps, 0.75 * fps], [0, 1], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            }),
            translate: interpolate(
              frame,
              [0.3 * fps, 0.75 * fps],
              ["0px 24px", "0px 0px"],
              {
                extrapolateLeft: "clamp",
                extrapolateRight: "clamp",
                easing: Easing.bezier(0.16, 1, 0.3, 1),
              },
            ),
          }}
        >
          Gestión académica, simplificada
        </Interactive.Div>
        <div style={{ display: "flex", gap: 18 }}>
          {tags.map((tag, i) => {
            const start = 1.0 * fps + i * 0.15 * fps;
            const opacity = interpolate(
              frame,
              [start, start + 0.35 * fps],
              [0, 1],
              { extrapolateLeft: "clamp", extrapolateRight: "clamp" },
            );
            return (
              <Interactive.Div
                key={tag}
                name={`Tag ${tag}`}
                style={{
                  opacity,
                  padding: "14px 26px",
                  borderRadius: 999,
                  border: `1px solid ${theme.border}`,
                  color: theme.accentLight,
                  fontSize: 24,
                  fontWeight: 600,
                }}
              >
                {tag}
              </Interactive.Div>
            );
          })}
        </div>
        <Interactive.Div
          name="Footer"
          style={{
            fontSize: 24,
            fontWeight: 600,
            letterSpacing: "0.1em",
            textTransform: "uppercase",
            color: theme.textMuted,
            marginTop: 30,
            opacity: interpolate(frame, [2.0 * fps, 2.5 * fps], [0, 1], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
            }),
          }}
        >
          Colegio Técnico Nacional
        </Interactive.Div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
