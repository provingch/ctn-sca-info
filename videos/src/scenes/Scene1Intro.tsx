import { Audio, Video } from "@remotion/media";
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
import { fontFamily } from "../theme";

const NARRATION_START = 12;

export const Scene1Intro: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const videoOpacity = interpolate(frame, [0.2 * fps, 0.85 * fps], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  const videoScale = interpolate(frame, [0.2 * fps, 0.85 * fps], [0.92, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.16, 1, 0.3, 1),
    output: "perceptual-scale",
  });

  return (
    <AbsoluteFill style={{ fontFamily }}>
      <Background />
      <BrandCorner />

      <Sequence from={NARRATION_START} layout="none">
        <Audio src={staticFile("audio/narration/01-problema.wav")} />
      </Sequence>

      <AbsoluteFill style={{ justifyContent: "center", alignItems: "center" }}>
        <Interactive.Div
          name="Tired worker video"
          style={{
            opacity: videoOpacity,
            scale: videoScale,
            borderRadius: 28,
            overflow: "hidden",
            border: "1px solid rgba(145, 160, 255, 0.22)",
            boxShadow: "0 60px 120px -30px rgba(0,0,0,0.7), 0 0 90px rgba(82,103,247,0.16)",
            width: 1300,
            lineHeight: 0,
          }}
        >
          <Video
            src={staticFile("videos/safe/tired_worker.mp4")}
            muted
            style={{ width: "100%", display: "block" }}
          />
        </Interactive.Div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
