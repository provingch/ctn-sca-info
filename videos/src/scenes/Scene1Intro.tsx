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
import { BrandCorner } from "../components/BrandCorner";
import { fontFamily, theme } from "../theme";

export const Scene1Intro: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

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
      <BrandCorner />

      <AbsoluteFill
        style={{
          justifyContent: "center",
          alignItems: "center",
          padding: "0 100px",
        }}
      >
        <div
          style={{
            display: "flex",
            flexDirection: "row",
            alignItems: "center",
            justifyContent: "center",
            gap: 80,
          }}
        >
          <Interactive.Div
            name="Problem headline"
            style={{
              fontSize: 70,
              fontWeight: 800,
              letterSpacing: "-0.02em",
              color: theme.text,
              textAlign: "left",
              lineHeight: 1.22,
              width: 720,
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
              width: 720,
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
