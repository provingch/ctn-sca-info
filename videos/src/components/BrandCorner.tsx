import { Easing, interpolate, useCurrentFrame } from "remotion";
import { theme } from "../theme";
import { Logo } from "./Logo";

export const BrandCorner: React.FC = () => {
  const frame = useCurrentFrame();
  const opacity = interpolate(frame, [0, 14], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  const translateY = interpolate(frame, [0, 14], [-10, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.16, 1, 0.3, 1),
  });

  return (
    <div
      style={{
        position: "absolute",
        top: 70,
        left: 90,
        display: "flex",
        flexDirection: "column",
        gap: 10,
        opacity,
        translate: `0px ${translateY}px`,
        zIndex: 10,
      }}
    >
      <div style={{ display: "flex", alignItems: "center", gap: 18 }}>
        <Logo size={58} color={theme.accentLight} />
        <div
          style={{
            fontSize: 50,
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
          fontSize: 22,
          fontWeight: 600,
          letterSpacing: "0.1em",
          textTransform: "uppercase",
          color: theme.textMuted,
        }}
      >
        Colegio Técnico Nacional
      </div>
    </div>
  );
};
