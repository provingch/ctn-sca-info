import { theme } from "../theme";

export const ScreenMockup: React.FC<{
  title: string;
  width: number;
  children: React.ReactNode;
}> = ({ title, width, children }) => {
  return (
    <div
      style={{
        width,
        borderRadius: 18,
        overflow: "hidden",
        background: theme.panel,
        border: `1px solid ${theme.border}`,
        boxShadow: "0 50px 90px -25px rgba(0,0,0,0.65)",
      }}
    >
      <div
        style={{
          height: 42,
          display: "flex",
          alignItems: "center",
          gap: 8,
          padding: "0 16px",
          background: "rgba(255,255,255,0.03)",
          borderBottom: `1px solid ${theme.border}`,
        }}
      >
        <div style={{ width: 10, height: 10, borderRadius: "50%", background: "#e6685c" }} />
        <div style={{ width: 10, height: 10, borderRadius: "50%", background: "#e6c05c" }} />
        <div style={{ width: 10, height: 10, borderRadius: "50%", background: "#5cbf6a" }} />
        <div
          style={{
            marginLeft: 12,
            fontSize: 13,
            fontWeight: 600,
            color: theme.textMuted,
          }}
        >
          {title}
        </div>
      </div>
      <div style={{ padding: 22, background: theme.bgDeep }}>{children}</div>
    </div>
  );
};
