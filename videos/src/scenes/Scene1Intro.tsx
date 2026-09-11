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

export const Scene1Intro: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const logoScale = interpolate(frame, [0, 0.7 * fps], [0.6, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.16, 1, 0.3, 1),
    output: "perceptual-scale",
  });
  const logoOpacity = interpolate(frame, [0, 0.5 * fps], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

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
          gap: 36,
        }}
      >
        <Interactive.Div
          name="Logo mark"
          style={{
            scale: logoScale,
            opacity: logoOpacity,
            filter: `drop-shadow(0 0 46px ${theme.accent}88)`,
          }}
        >
          <Logo size={150} color={theme.accentLight} />
        </Interactive.Div>
        <Interactive.Div
          name="SCA title"
          style={{
            fontSize: 132,
            fontWeight: 800,
            letterSpacing: "-0.03em",
            color: theme.text,
            opacity: interpolate(frame, [0.35 * fps, 0.85 * fps], [0, 1], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            }),
            translate: interpolate(
              frame,
              [0.35 * fps, 0.85 * fps],
              ["0px 26px", "0px 0px"],
              {
                extrapolateLeft: "clamp",
                extrapolateRight: "clamp",
                easing: Easing.bezier(0.16, 1, 0.3, 1),
              },
            ),
          }}
        >
          SCA
        </Interactive.Div>
        <Interactive.Div
          name="Subtitle"
          style={{
            fontSize: 42,
            fontWeight: 500,
            color: theme.textMuted,
            textAlign: "center",
            opacity: interpolate(frame, [0.8 * fps, 1.3 * fps], [0, 1], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
            }),
            translate: interpolate(
              frame,
              [0.8 * fps, 1.3 * fps],
              ["0px 16px", "0px 0px"],
              { extrapolateLeft: "clamp", extrapolateRight: "clamp" },
            ),
          }}
        >
          Sistema de Carpetas Académicas
        </Interactive.Div>
        <Interactive.Div
          name="Institution tag"
          style={{
            fontSize: 26,
            fontWeight: 600,
            letterSpacing: "0.12em",
            textTransform: "uppercase",
            color: theme.accentLight,
            opacity: interpolate(frame, [1.35 * fps, 1.8 * fps], [0, 1], {
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
