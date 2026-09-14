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

  const cornerOpacity = interpolate(frame, [0, 0.4 * fps], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  const cornerTranslateY = interpolate(frame, [0, 0.4 * fps], [-12, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.16, 1, 0.3, 1),
  });

  const headlineOpacity = interpolate(frame, [0.5 * fps, 1.1 * fps], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.16, 1, 0.3, 1),
  });
  const headlineTranslateY = interpolate(
    frame,
    [0.5 * fps, 1.1 * fps],
    [28, 0],
    {
      extrapolateLeft: "clamp",
      extrapolateRight: "clamp",
      easing: Easing.bezier(0.16, 1, 0.3, 1),
    },
  );

  return (
    <AbsoluteFill style={{ fontFamily }}>
      <Background />

      <Interactive.Div
        name="Brand corner"
        style={{
          position: "absolute",
          top: 70,
          left: 90,
          display: "flex",
          flexDirection: "column",
          gap: 10,
          opacity: cornerOpacity,
          translate: `0px ${cornerTranslateY}px`,
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
          <Logo size={46} color={theme.accentLight} />
          <div
            style={{
              fontSize: 40,
              fontWeight: 800,
              letterSpacing: "-0.02em",
              color: theme.text,
            }}
          >
            SCA
          </div>
        </div>
        <div
          style={{
            fontSize: 18,
            fontWeight: 600,
            letterSpacing: "0.1em",
            textTransform: "uppercase",
            color: theme.textMuted,
          }}
        >
          Colegio Técnico Nacional
        </div>
      </Interactive.Div>

      <AbsoluteFill
        style={{
          justifyContent: "center",
          alignItems: "center",
          padding: "0 180px",
        }}
      >
        <Interactive.Div
          name="Problem headline"
          style={{
            fontSize: 72,
            fontWeight: 800,
            letterSpacing: "-0.02em",
            color: theme.text,
            textAlign: "center",
            lineHeight: 1.2,
            opacity: headlineOpacity,
            translate: `0px ${headlineTranslateY}px`,
          }}
        >
          ¿Cansado del{" "}
          <span style={{ color: theme.accentLight }}>traspapeleo</span> de
          planillas, notas, exámenes y documentos académicos?
        </Interactive.Div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
