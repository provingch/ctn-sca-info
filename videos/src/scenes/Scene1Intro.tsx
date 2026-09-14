import { Video } from "@remotion/media";
import {
  AbsoluteFill,
  Easing,
  Interactive,
  interpolate,
  staticFile,
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

  const videoOpacity = interpolate(frame, [0.65 * fps, 1.3 * fps], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  const videoScale = interpolate(frame, [0.65 * fps, 1.3 * fps], [0.92, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.16, 1, 0.3, 1),
    output: "perceptual-scale",
  });

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
          padding: "0 130px",
        }}
      >
        <div
          style={{
            display: "flex",
            flexDirection: "row",
            alignItems: "center",
            justifyContent: "center",
            gap: 90,
          }}
        >
          <Interactive.Div
            name="Problem headline"
            style={{
              fontSize: 58,
              fontWeight: 800,
              letterSpacing: "-0.02em",
              color: theme.text,
              textAlign: "left",
              lineHeight: 1.22,
              width: 620,
              opacity: headlineOpacity,
              translate: `0px ${headlineTranslateY}px`,
            }}
          >
            ¿Cansado del{" "}
            <span style={{ color: theme.accentLight }}>traspapeleo</span> de
            planillas, notas, exámenes y documentos académicos?
          </Interactive.Div>

          <Interactive.Div
            name="Tired worker video"
            style={{
              opacity: videoOpacity,
              scale: videoScale,
              borderRadius: 24,
              overflow: "hidden",
              border: `1px solid ${theme.border}`,
              boxShadow: `0 40px 90px -20px rgba(0,0,0,0.65), 0 0 60px ${theme.accent}18`,
              width: 620,
              lineHeight: 0,
            }}
          >
            <Video
              src={staticFile("videos/safe/tired_worker.mp4")}
              muted
              style={{ width: "100%", display: "block" }}
            />
          </Interactive.Div>
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
