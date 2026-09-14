import { Audio } from "@remotion/media";
import {
  AbsoluteFill,
  Easing,
  Interactive,
  Sequence,
  interpolate,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { Background } from "../components/Background";
import { BrandCorner } from "../components/BrandCorner";
import { Logo } from "../components/Logo";
import { fontFamily, theme } from "../theme";

const NARRATION_START = 10;

export const SceneSolutionReveal: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const logoScale = interpolate(frame, [0, 0.6 * fps], [0.6, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.16, 1, 0.3, 1),
    output: "perceptual-scale",
  });
  const logoOpacity = interpolate(frame, [0, 0.4 * fps], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  const eyebrowOpacity = interpolate(frame, [0.25 * fps, 0.6 * fps], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  const titleOpacity = interpolate(frame, [0.4 * fps, 0.9 * fps], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.16, 1, 0.3, 1),
  });
  const titleTranslateY = interpolate(frame, [0.4 * fps, 0.9 * fps], [24, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.16, 1, 0.3, 1),
  });

  const subtitleOpacity = interpolate(frame, [0.85 * fps, 1.3 * fps], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  return (
    <AbsoluteFill style={{ fontFamily }}>
      <Background />
      <BrandCorner />

      <Sequence from={NARRATION_START} layout="none">
        <Audio src={staticFile("audio/narration/02-solucion.wav")} />
      </Sequence>

      <AbsoluteFill
        style={{
          justifyContent: "center",
          alignItems: "center",
          gap: 30,
        }}
      >
        <Interactive.Div
          name="Solution logo"
          style={{
            scale: logoScale,
            opacity: logoOpacity,
            filter: `drop-shadow(0 0 46px ${theme.accent}88)`,
          }}
        >
          <Logo size={130} color={theme.accentLight} />
        </Interactive.Div>
        <Interactive.Div
          name="Solution eyebrow"
          style={{
            fontSize: 26,
            fontWeight: 700,
            letterSpacing: "0.16em",
            textTransform: "uppercase",
            color: theme.accentLight,
            opacity: eyebrowOpacity,
          }}
        >
          Te presentamos la solución
        </Interactive.Div>
        <Interactive.Div
          name="Solution title"
          style={{
            fontSize: 90,
            fontWeight: 800,
            letterSpacing: "-0.02em",
            color: theme.text,
            textAlign: "center",
            opacity: titleOpacity,
            translate: `0px ${titleTranslateY}px`,
          }}
        >
          SCA
        </Interactive.Div>
        <Interactive.Div
          name="Solution subtitle"
          style={{
            fontSize: 36,
            fontWeight: 500,
            color: theme.textMuted,
            textAlign: "center",
            opacity: subtitleOpacity,
          }}
        >
          Sistema de Carpetas Académicas
        </Interactive.Div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
