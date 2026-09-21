import { Fragment } from "react";
import { theme } from "../theme";

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
    <div style={{ width: "100%" }}>
      <div
        style={{
          fontSize: 13,
          fontWeight: 700,
          color: theme.text,
          marginBottom: 10,
        }}
      >
        Laboratorio Java · 3° A
      </div>
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
              fontSize: 13,
              fontWeight: 700,
              padding: "9px 12px",
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
                fontSize: 13,
                fontWeight: j === 0 ? 600 : 500,
                padding: "8px 12px",
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

const EXCEL_ROWS = [
  "Alumno 01",
  "Alumno 02",
  "Alumno 03",
  "Alumno 04",
  "Alumno 05",
  "Alumno 06",
];

export const ExcelMock: React.FC = () => {
  return (
    <div
      style={{
        width: "100%",
        background: "#fff",
        color: "#1a1a1a",
        fontFamily: "Calibri, Arial, sans-serif",
        borderRadius: 6,
        overflow: "hidden",
      }}
    >
      <div
        style={{
          background: "#1d6f5e",
          color: "#fff",
          padding: "10px 14px",
          fontWeight: 700,
          fontSize: 13,
          textAlign: "center",
        }}
      >
        COLEGIO TÉCNICO NACIONAL DE ASUNCIÓN
      </div>
      <div
        style={{
          padding: "7px 14px",
          fontSize: 11,
          color: "#444",
          borderBottom: "1px solid #e2e2e2",
          background: "#f7f7f7",
        }}
      >
        Disciplina: Laboratorio Java &nbsp;·&nbsp; Profesor/a: Prof. Ejemplo
      </div>
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "24px 1.6fr 0.7fr 0.7fr 0.7fr 0.7fr",
        }}
      >
        {["#", "Apellidos y Nombres", "T1", "T2", "T3", "Prom"].map((h) => (
          <div
            key={h}
            style={{
              background: "#e8ede8",
              color: "#1a1a1a",
              fontSize: 11,
              fontWeight: 700,
              padding: "6px 8px",
              borderBottom: "1px solid #cfcfcf",
              textAlign: h === "Apellidos y Nombres" ? "left" : "center",
            }}
          >
            {h}
          </div>
        ))}
        {EXCEL_ROWS.map((name, i) => (
          <Fragment key={name}>
            <div
              style={{
                fontSize: 11,
                color: "#333",
                padding: "6px 8px",
                background: i % 2 === 0 ? "#fbfbfb" : "#fff",
                borderBottom: "1px solid #ececec",
                textAlign: "center",
              }}
            >
              {i + 1}
            </div>
            <div
              style={{
                fontSize: 11,
                color: "#1a1a1a",
                fontWeight: 600,
                padding: "6px 8px",
                background: i % 2 === 0 ? "#fbfbfb" : "#fff",
                borderBottom: "1px solid #ececec",
              }}
            >
              {name}
            </div>
            {[0, 0, 0].map((_, j) => (
              <div
                key={j}
                style={{
                  fontSize: 11,
                  color: "#333",
                  padding: "6px 8px",
                  background: i % 2 === 0 ? "#fbfbfb" : "#fff",
                  borderBottom: "1px solid #ececec",
                  textAlign: "center",
                }}
              >
                0
              </div>
            ))}
            <div
              style={{
                fontSize: 11,
                fontWeight: 700,
                color: "#1a1a1a",
                padding: "6px 8px",
                background: i % 2 === 0 ? "#fbfbfb" : "#fff",
                borderBottom: "1px solid #ececec",
                textAlign: "center",
              }}
            >
              0
            </div>
          </Fragment>
        ))}
      </div>
    </div>
  );
};

const EVAL_CARDS = [
  {
    n: "01",
    title: "Descargar planillas",
    desc: "Exportá planillas completadas de los cursos.",
  },
  {
    n: "02",
    title: "Revisar plan curricular",
    desc: "Aprobá o rechazá planes de profesores.",
  },
  {
    n: "03",
    title: "Seguimiento de profesores",
    desc: "Consultá cumplimiento de planes y resolvé incumplimientos.",
  },
];

export const EvaluacionMock: React.FC = () => {
  return (
    <div style={{ width: "100%", display: "flex", flexDirection: "column", gap: 16 }}>
      <div
        style={{
          fontSize: 12,
          fontWeight: 700,
          letterSpacing: "0.1em",
          color: theme.accentLight,
          textTransform: "uppercase",
        }}
      >
        Evaluador
      </div>
      <div style={{ fontSize: 24, fontWeight: 800, color: theme.text }}>
        Panel de Evaluación
      </div>
      <div style={{ display: "flex", gap: 12 }}>
        {EVAL_CARDS.map((c) => (
          <div
            key={c.n}
            style={{
              flex: 1,
              background: "rgba(145,160,255,0.06)",
              border: `1px solid ${theme.border}`,
              borderRadius: 14,
              padding: 16,
            }}
          >
            <div style={{ color: theme.accentLight, fontWeight: 800, fontSize: 16 }}>
              {c.n}
            </div>
            <div style={{ color: theme.text, fontWeight: 700, fontSize: 14, marginTop: 8 }}>
              {c.title}
            </div>
            <div style={{ color: theme.textMuted, fontSize: 12, marginTop: 6, lineHeight: 1.35 }}>
              {c.desc}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

const QUEJA_ESTADOS = ["Pendiente", "Aceptada", "Revisada", "Resuelta"];

const CONDUCT_CODES = [
  { code: "N1", desc: "Falta de material" },
  { code: "N2", desc: "Interrumpe la clase" },
];

export const CoordinacionMock: React.FC = () => {
  const activeStep = 2; // "Revisada" resaltado, para mostrar el flujo en progreso

  return (
    <div style={{ width: "100%", display: "flex", flexDirection: "column", gap: 18 }}>
      <div
        style={{
          fontSize: 12,
          fontWeight: 700,
          letterSpacing: "0.1em",
          color: theme.accentLight,
          textTransform: "uppercase",
        }}
      >
        Coordinación Pedagógica
      </div>
      <div style={{ fontSize: 24, fontWeight: 800, color: theme.text }}>
        Queja · Prof. Ejemplo
      </div>
      <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
        {QUEJA_ESTADOS.map((estado, i) => (
          <Fragment key={estado}>
            <div
              style={{
                padding: "8px 14px",
                borderRadius: 999,
                fontSize: 12,
                fontWeight: 700,
                background: i <= activeStep ? theme.accent : "rgba(145,160,255,0.08)",
                color: i <= activeStep ? "#fff" : theme.textMuted,
              }}
            >
              {estado}
            </div>
            {i < QUEJA_ESTADOS.length - 1 && (
              <div
                style={{
                  width: 18,
                  height: 2,
                  background: i < activeStep ? theme.accent : theme.border,
                }}
              />
            )}
          </Fragment>
        ))}
      </div>
      <div
        style={{
          background: "rgba(145,160,255,0.06)",
          border: `1px solid ${theme.border}`,
          borderRadius: 14,
          padding: 16,
          display: "flex",
          flexDirection: "column",
          gap: 10,
        }}
      >
        <div
          style={{
            fontSize: 12,
            fontWeight: 700,
            color: theme.textMuted,
            textTransform: "uppercase",
            letterSpacing: "0.08em",
          }}
        >
          Catálogo de conducta
        </div>
        {CONDUCT_CODES.map((c) => (
          <div key={c.code} style={{ display: "flex", gap: 10, alignItems: "center" }}>
            <div
              style={{
                width: 34,
                height: 24,
                borderRadius: 6,
                background: theme.accentDeep,
                color: "#fff",
                fontSize: 12,
                fontWeight: 800,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
              }}
            >
              {c.code}
            </div>
            <div style={{ fontSize: 13, color: theme.text, fontWeight: 500 }}>{c.desc}</div>
          </div>
        ))}
      </div>
    </div>
  );
};

const ADMIN_STATS = [
  { label: "Especialidades", value: "5" },
  { label: "Usuarios activos", value: "312" },
  { label: "Cursos", value: "48" },
  { label: "Salas", value: "18" },
];

export const AdministracionMock: React.FC = () => {
  return (
    <div style={{ width: "100%", display: "flex", flexDirection: "column", gap: 14 }}>
      <div style={{ fontSize: 24, fontWeight: 800, color: theme.text }}>
        Panel de Administración
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 12 }}>
        {ADMIN_STATS.map((s) => (
          <div
            key={s.label}
            style={{
              background: "rgba(145,160,255,0.06)",
              border: `1px solid ${theme.border}`,
              borderRadius: 14,
              padding: "14px 12px",
            }}
          >
            <div style={{ fontSize: 26, fontWeight: 800, color: theme.accentLight }}>
              {s.value}
            </div>
            <div style={{ fontSize: 11, color: theme.textMuted, marginTop: 4 }}>{s.label}</div>
          </div>
        ))}
      </div>
      <div
        style={{
          background: "rgba(145,160,255,0.04)",
          border: `1px solid ${theme.border}`,
          borderRadius: 14,
          padding: 14,
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
        }}
      >
        <div style={{ color: theme.text, fontWeight: 600, fontSize: 13 }}>
          Especialidad: Informática
        </div>
        <div style={{ color: theme.accentLight, fontWeight: 700, fontSize: 12 }}>
          24 secciones
        </div>
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

export const FamiliasMock: React.FC = () => {
  const avg = 8.6;
  const pct = avg / 10;
  const r = 42;
  const c = 2 * Math.PI * r;

  return (
    <div style={{ width: "100%", display: "flex", flexDirection: "column", gap: 14 }}>
      <div style={{ display: "flex", gap: 8 }}>
        {["Primera etapa", "Segunda etapa"].map((label, i) => (
          <div
            key={label}
            style={{
              padding: "6px 12px",
              borderRadius: 999,
              fontSize: 11,
              fontWeight: 700,
              background: i === 0 ? theme.accent : "transparent",
              border: i === 0 ? "none" : `1px solid ${theme.border}`,
              color: i === 0 ? "#fff" : theme.textMuted,
            }}
          >
            {label}
          </div>
        ))}
      </div>
      <div style={{ fontSize: 13, fontWeight: 700, color: theme.text }}>
        Resumen académico
      </div>
      <div style={{ display: "flex", gap: 24, alignItems: "center" }}>
        <div style={{ position: "relative", width: 106, height: 106, flexShrink: 0 }}>
          <svg width={106} height={106}>
            <circle
              cx={53}
              cy={53}
              r={r}
              fill="none"
              stroke="rgba(145,160,255,0.15)"
              strokeWidth={9}
            />
            <circle
              cx={53}
              cy={53}
              r={r}
              fill="none"
              stroke={theme.accentLight}
              strokeWidth={9}
              strokeLinecap="round"
              strokeDasharray={`${c * pct} ${c}`}
              transform="rotate(-90 53 53)"
            />
          </svg>
          <div
            style={{
              position: "absolute",
              inset: 0,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: 24,
              fontWeight: 800,
              color: theme.text,
            }}
          >
            {avg.toFixed(1)}
          </div>
        </div>
        <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: 9 }}>
          {SUBJECTS.map((s) => (
            <div key={s.name} style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <div style={{ width: 100, fontSize: 12, fontWeight: 600, color: theme.textMuted }}>
                {s.name}
              </div>
              <div
                style={{
                  flex: 1,
                  height: 7,
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
              <div style={{ width: 30, fontSize: 12, fontWeight: 700, color: theme.text }}>
                {s.grade.toFixed(1)}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
