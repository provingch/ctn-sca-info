import { Fragment } from "react";
import { theme, specialtyColors } from "../theme";

const DAYS = ["Lun", "Mar", "Mié", "Jue", "Vie"];
const HOURS = ["1°", "2°", "3°", "4°", "5°", "6°"];

const SCHEDULE: { day: number; hour: number; label: string; color: string }[] = [
  { day: 0, hour: 0, label: "MAT", color: theme.accent },
  { day: 0, hour: 1, label: "LEN", color: specialtyColors.informatica },
  { day: 0, hour: 3, label: "PROG", color: specialtyColors.electromecanica },
  { day: 1, hour: 0, label: "PROG", color: specialtyColors.electromecanica },
  { day: 1, hour: 2, label: "ELEC", color: specialtyColors.electricidad },
  { day: 1, hour: 4, label: "ED.F", color: specialtyColors.mecanica },
  { day: 2, hour: 1, label: "FIS", color: specialtyColors.construcciones },
  { day: 2, hour: 3, label: "MAT", color: theme.accent },
  { day: 3, hour: 0, label: "ING", color: specialtyColors.mecanica },
  { day: 3, hour: 2, label: "TALL", color: specialtyColors.electromecanica },
  { day: 3, hour: 5, label: "LEN", color: specialtyColors.informatica },
  { day: 4, hour: 1, label: "PROG", color: specialtyColors.electromecanica },
  { day: 4, hour: 4, label: "ELEC", color: specialtyColors.electricidad },
];

export const HorarioMock: React.FC = () => {
  return (
    <div style={{ width: 596 }}>
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "42px repeat(5, 1fr)",
          gap: 5,
        }}
      >
        <div />
        {DAYS.map((day) => (
          <div
            key={day}
            style={{
              fontSize: 15,
              fontWeight: 700,
              color: theme.textMuted,
              textAlign: "center",
              paddingBottom: 4,
            }}
          >
            {day}
          </div>
        ))}
        {HOURS.map((hour, hi) => (
          <Fragment key={hour}>
            <div
              style={{
                fontSize: 13,
                fontWeight: 700,
                color: theme.textMuted,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
              }}
            >
              {hour}
            </div>
            {DAYS.map((_, di) => {
              const entry = SCHEDULE.find((s) => s.day === di && s.hour === hi);
              return (
                <div
                  key={`${hi}-${di}`}
                  style={{
                    height: 38,
                    borderRadius: 8,
                    background: entry ? entry.color : "rgba(145, 160, 255, 0.06)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    fontSize: 13,
                    fontWeight: 700,
                    color: entry ? "#fff" : "transparent",
                  }}
                >
                  {entry ? entry.label : "-"}
                </div>
              );
            })}
          </Fragment>
        ))}
      </div>
    </div>
  );
};

const PLANILLA_ROWS = [
  ["Alumno 01", "8.5", "9.0", "8.0", "8.5"],
  ["Alumno 02", "7.0", "7.5", "8.0", "7.5"],
  ["Alumno 03", "9.5", "9.0", "10", "9.5"],
  ["Alumno 04", "6.5", "7.0", "7.5", "7.0"],
  ["Alumno 05", "8.0", "8.5", "8.0", "8.2"],
  ["Alumno 06", "9.0", "8.5", "9.0", "8.8"],
];

export const PlanillaMock: React.FC = () => {
  return (
    <div style={{ width: 596 }}>
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1.6fr 1fr 1fr 1fr 1fr",
          borderRadius: 10,
          overflow: "hidden",
          border: `1px solid ${theme.border}`,
        }}
      >
        {["Alumno", "T1", "T2", "T3", "Prom"].map((h) => (
          <div
            key={h}
            style={{
              background: theme.accent,
              color: "#fff",
              fontSize: 15,
              fontWeight: 700,
              padding: "10px 14px",
              textAlign: h === "Alumno" ? "left" : "center",
            }}
          >
            {h}
          </div>
        ))}
        {PLANILLA_ROWS.map((row, i) =>
          row.map((cell, j) => (
            <div
              key={`${i}-${j}`}
              style={{
                background: i % 2 === 0 ? "rgba(145,160,255,0.05)" : "transparent",
                color: theme.text,
                fontSize: 15,
                fontWeight: j === 0 ? 600 : 500,
                padding: "9px 14px",
                textAlign: j === 0 ? "left" : "center",
              }}
            >
              {cell}
            </div>
          )),
        )}
      </div>
    </div>
  );
};

const SUBJECTS = [
  { name: "Matemática", grade: 9.0 },
  { name: "Lengua", grade: 8.0 },
  { name: "Programación", grade: 9.5 },
  { name: "Física", grade: 7.5 },
];

export const PadresMock: React.FC = () => {
  const avg = 8.6;
  const pct = avg / 10;
  const r = 46;
  const c = 2 * Math.PI * r;

  return (
    <div style={{ width: 596, display: "flex", gap: 30, alignItems: "center" }}>
      <div style={{ position: "relative", width: 120, height: 120, flexShrink: 0 }}>
        <svg width={120} height={120}>
          <circle
            cx={60}
            cy={60}
            r={r}
            fill="none"
            stroke="rgba(145,160,255,0.15)"
            strokeWidth={10}
          />
          <circle
            cx={60}
            cy={60}
            r={r}
            fill="none"
            stroke={theme.accentLight}
            strokeWidth={10}
            strokeLinecap="round"
            strokeDasharray={`${c * pct} ${c}`}
            transform="rotate(-90 60 60)"
          />
        </svg>
        <div
          style={{
            position: "absolute",
            inset: 0,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontSize: 28,
            fontWeight: 800,
            color: theme.text,
          }}
        >
          {avg.toFixed(1)}
        </div>
      </div>
      <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: 10 }}>
        {SUBJECTS.map((s) => (
          <div key={s.name} style={{ display: "flex", alignItems: "center", gap: 12 }}>
            <div style={{ width: 130, fontSize: 15, fontWeight: 600, color: theme.textMuted }}>
              {s.name}
            </div>
            <div
              style={{
                flex: 1,
                height: 8,
                borderRadius: 4,
                background: "rgba(145,160,255,0.12)",
                overflow: "hidden",
              }}
            >
              <div
                style={{
                  width: `${(s.grade / 10) * 100}%`,
                  height: "100%",
                  background: theme.accentLight,
                }}
              />
            </div>
            <div style={{ width: 34, fontSize: 15, fontWeight: 700, color: theme.text }}>
              {s.grade.toFixed(1)}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
