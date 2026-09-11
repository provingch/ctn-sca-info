import { AbsoluteFill, useCurrentFrame } from "remotion";
import { theme } from "../theme";

export const Background: React.FC = () => {
  const frame = useCurrentFrame();
  const driftX = Math.sin(frame / 140) * 60;
  const driftY = Math.cos(frame / 160) * 40;

  return (
    <AbsoluteFill
      style={{
        backgroundColor: theme.bg,
      }}
    >
      <AbsoluteFill
        style={{
          background: `radial-gradient(circle at ${18 + driftX * 0.05}% ${
            22 + driftY * 0.05
          }%, ${theme.accent}33 0%, transparent 38%), radial-gradient(circle at ${
            84 - driftX * 0.04
          }% ${86 - driftY * 0.04}%, ${
            theme.accentLight
          }26 0%, transparent 42%), linear-gradient(180deg, ${
            theme.bgDeep
          } 0%, ${theme.bg} 100%)`,
        }}
      />
      <AbsoluteFill
        style={{
          backgroundImage: `radial-gradient(${theme.accentLight}1a 1.4px, transparent 1.4px)`,
          backgroundSize: "34px 34px",
          translate: `${driftX}px ${driftY}px`,
          opacity: 0.55,
        }}
      />
    </AbsoluteFill>
  );
};
