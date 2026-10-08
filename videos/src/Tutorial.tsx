import { Audio, Video } from "@remotion/media";
import {
  AbsoluteFill,
  CalculateMetadataFunction,
  Easing,
  Img,
  interpolate,
  Sequence,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { fontFamily } from "./theme";

// Tutoriales por perfil: tramos grabados con scripts/grabar.mjs (+ narración de scripts/narrar-tutoriales.mjs).
export type Tramo = {
  id: string;
  archivo: string;
  inicio: number; // segundos a recortar del principio (la navegación previa)
  texto: string;
  durVideo: number;
  audio?: string;
  durAudio?: number;
};

export type TutorialProps = { rol: string; titulo: string; tramos: Tramo[] };

const FPS = 30;
const INTRO = 3.5;
const OUTRO = 4;
const MARGEN = 0.8; // aire después de que termina la voz
const VINO = "#7a1f2b";
const NARANJA = "#e8590c";

const TITULOS: Record<string, string> = {
  "01-inicio": "Tu panel", "01-resumen": "Resumen de tu hijo", "02-iniciar-clase": "Iniciar una clase",
  "03-plan-curricular": "Plan curricular", "04-clases-dadas": "Clases dadas", "05-planilla": "La planilla",
  "06-firma": "Tu firma", "02-planillas": "Ver planillas", "03-planes": "Revisar planes",
  "04-seguimiento": "Seguimiento e incumplimientos", "02-quejas": "Quejas", "03-resolver": "Resolver una queja",
  "04-conducta": "Códigos de conducta", "02-materias": "Materias y tareas", "03-conducta": "Notas de conducta",
  "02-usuarios": "Usuarios", "03-asignaciones": "Asignaciones", "04-alumnos-horarios": "Alumnos y horarios",
  "05-quejas-clases": "Quejas y clases dadas",
};

// sin audio todavía: duración estimada por la longitud del texto (~15 caracteres por segundo)
const voz = (t: Tramo) => t.durAudio ?? t.texto.length / 15;
const util = (t: Tramo) => t.durVideo - t.inicio;
export const largoTramo = (t: Tramo) => Math.max(voz(t) + MARGEN, Math.min(util(t), voz(t) + 6));

export const calcularTutorial: CalculateMetadataFunction<TutorialProps> = async ({ props }) => {
  const res = await fetch(staticFile(`tutoriales/${props.rol}/tramos.json`));
  const json: Record<string, Omit<Tramo, "id">> = await res.json();
  const tramos = Object.entries(json).sort(([a], [b]) => a.localeCompare(b)).map(([id, t]) => ({ id, ...t }));
  const total = INTRO + tramos.reduce((s, t) => s + largoTramo(t), 0) + OUTRO;
  return { durationInFrames: Math.ceil(total * FPS), props: { ...props, tramos } };
};

const fade = (frame: number, dur: number) =>
  interpolate(frame, [0, 12, dur - 12, dur], [0, 1, 1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });

const Portada: React.FC<{ titulo: string; sub: string }> = ({ titulo, sub }) => {
  const frame = useCurrentFrame();
  const { durationInFrames } = useVideoConfig();
  return (
    <AbsoluteFill style={{ background: `linear-gradient(150deg, #f7e9eb, #ffffff 65%)`, justifyContent: "center", alignItems: "center", fontFamily, opacity: fade(frame, durationInFrames) }}>
      <Img src={staticFile("tutoriales/logo.svg")} style={{ width: 150, marginBottom: 36 }} />
      <div style={{ color: VINO, fontSize: 30, fontWeight: 700, letterSpacing: "0.08em", textTransform: "uppercase" }}>Sistema de Carpeta Académica</div>
      <div
        style={{
          fontSize: 92, fontWeight: 800, color: "#1d2433", marginTop: 18,
          translate: interpolate(frame, [0, 25], ["0px 30px", "0px 0px"], { extrapolateRight: "clamp", easing: Easing.bezier(0.16, 1, 0.3, 1) }),
        }}
      >
        {titulo}
      </div>
      <div style={{ fontSize: 36, color: "#5b6474", marginTop: 16 }}>{sub}</div>
    </AbsoluteFill>
  );
};

// Subtítulo: la frase que corresponde a este momento (reparto proporcional a la longitud de cada frase).
const Subtitulo: React.FC<{ texto: string; dur: number }> = ({ texto, dur }) => {
  const frame = useCurrentFrame();
  const frases = texto.match(/[^.!?]+[.!?]*/g)?.map((f) => f.trim()).filter(Boolean) ?? [texto];
  const total = frases.reduce((s, f) => s + f.length, 0);
  let acum = 0;
  const actual = frases.find((f) => (acum += f.length) / total >= frame / (dur * FPS)) ?? frases[frases.length - 1];
  return (
    <div style={{ position: "absolute", left: 0, right: 0, bottom: 54, display: "flex", justifyContent: "center", fontFamily }}>
      <div style={{ maxWidth: 1500, background: "rgba(17, 22, 36, .86)", color: "#fff", fontSize: 38, lineHeight: 1.35, padding: "16px 30px", borderRadius: 14, textAlign: "center" }}>
        {actual}
      </div>
    </div>
  );
};

const Paso: React.FC<{ t: Tramo; n: number; de: number }> = ({ t, n, de }) => {
  const frame = useCurrentFrame();
  const largo = largoTramo(t);
  // si la voz dura más que lo grabado, el video va un poco más lento (nunca menos de 0,6×)
  const velocidad = Math.max(0.6, Math.min(1, util(t) / (largo - MARGEN)));
  return (
    <AbsoluteFill style={{ backgroundColor: "#eef1f6", opacity: fade(frame, largo * FPS) }}>
      <Video src={staticFile(t.archivo)} trimBefore={Math.round(t.inicio * FPS)} playbackRate={velocidad} muted style={{ width: "100%", height: "100%" }} />
      {t.audio ? <Audio src={staticFile(t.audio)} /> : null}
      <div style={{ position: "absolute", bottom: 190, left: 40, fontFamily, display: "flex", gap: 14, alignItems: "center", background: "rgba(255,255,255,.95)", borderRadius: 999, padding: "10px 22px 10px 10px", boxShadow: "0 6px 20px rgba(20,30,50,.18)" }}>
        <span style={{ background: NARANJA, color: "#fff", borderRadius: 999, width: 46, height: 46, display: "grid", placeItems: "center", fontSize: 24, fontWeight: 800 }}>{n}</span>
        <span style={{ fontSize: 28, fontWeight: 700, color: "#1d2433" }}>{TITULOS[t.id] ?? t.id}</span>
        <span style={{ fontSize: 22, color: "#8a93a3" }}>{n} / {de}</span>
      </div>
      <Subtitulo texto={t.texto} dur={voz(t)} />
    </AbsoluteFill>
  );
};

export const Tutorial: React.FC<TutorialProps> = ({ titulo, tramos }) => {
  let desde = INTRO * FPS;
  return (
    <AbsoluteFill style={{ backgroundColor: "#fff" }}>
      <Sequence durationInFrames={INTRO * FPS}>
        <Portada titulo={titulo} sub="Tutorial paso a paso" />
      </Sequence>
      {tramos.map((t, i) => {
        const largo = Math.round(largoTramo(t) * FPS);
        const s = (
          <Sequence key={t.id} from={desde} durationInFrames={largo} name={t.id}>
            <Paso t={t} n={i + 1} de={tramos.length} />
          </Sequence>
        );
        desde += largo;
        return s;
      })}
      <Sequence from={desde} durationInFrames={OUTRO * FPS}>
        <Portada titulo="¿Te quedó alguna duda?" sub="Abrí el manual desde tu nombre → Manual, arriba a la derecha" />
      </Sequence>
    </AbsoluteFill>
  );
};
