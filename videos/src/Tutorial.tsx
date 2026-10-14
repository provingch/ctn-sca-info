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

// Tutorial por perfil: una grabación continua (scripts/grabar-tutorial.mjs) hecha al ritmo de la narración
// (scripts/narrar-tutorial.mjs). Cada frase suena en el momento exacto en que arrancó su paso: el video va a velocidad normal.
type Escena = { id: string; titulo: string; t: number };
type Paso = { id: string; t: number; texto: string; archivo: string; dur: number };
export type TutorialProps = { rol: string; titulo: string; escenas: Escena[]; pasos: Paso[]; duracion: number };

const FPS = 30;
const INTRO = 3.5;
const OUTRO = 4;
const VINO = "#7a1f2b";
const NARANJA = "#e8590c";
const s2f = (s: number) => Math.round(s * FPS);

export const calcularTutorial: CalculateMetadataFunction<TutorialProps> = async ({ props }) => {
  const base = `tutoriales/${props.rol}/`;
  const grab = await (await fetch(staticFile(base + "grabacion.json"))).json();
  const voz = await (await fetch(staticFile(base + "voz/linea.json"))).json();
  const pasos: Paso[] = grab.pasos.map((p: { id: string; t: number }) => {
    const v = voz.pasos.find((x: { id: string }) => x.id === p.id);
    return { id: p.id, t: p.t, texto: v.texto, archivo: v.archivo, dur: v.dur };
  });
  return {
    durationInFrames: s2f(INTRO + grab.duracion + OUTRO),
    props: { ...props, escenas: grab.escenas, pasos, duracion: grab.duracion },
  };
};

const fundido = (frame: number, dur: number, f = 12) =>
  interpolate(frame, [0, f, dur - f, dur], [0, 1, 1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });

const Portada: React.FC<{ titulo: string; sub: string }> = ({ titulo, sub }) => {
  const frame = useCurrentFrame();
  const { durationInFrames } = useVideoConfig();
  return (
    <AbsoluteFill style={{ background: "linear-gradient(150deg, #f7e9eb, #ffffff 65%)", justifyContent: "center", alignItems: "center", fontFamily, opacity: fundido(frame, durationInFrames) }}>
      <Img src={staticFile("tutoriales/logo.svg")} style={{ width: 150, marginBottom: 36 }} />
      <div style={{ color: VINO, fontSize: 30, fontWeight: 700, letterSpacing: "0.08em", textTransform: "uppercase" }}>Sistema de Carpeta Académica</div>
      <div style={{ fontSize: 92, fontWeight: 800, color: "#1d2433", marginTop: 18, translate: interpolate(frame, [0, 25], ["0px 30px", "0px 0px"], { extrapolateRight: "clamp", easing: Easing.bezier(0.16, 1, 0.3, 1) }) }}>
        {titulo}
      </div>
      <div style={{ fontSize: 36, color: "#5b6474", marginTop: 16 }}>{sub}</div>
    </AbsoluteFill>
  );
};

const Subtitulo: React.FC<{ texto: string }> = ({ texto }) => {
  const frame = useCurrentFrame();
  const { durationInFrames } = useVideoConfig();
  return (
    <div style={{ position: "absolute", left: 0, right: 0, bottom: 46, display: "flex", justifyContent: "center", fontFamily, opacity: fundido(frame, durationInFrames, 6) }}>
      <div style={{ maxWidth: 1560, background: "rgba(17, 22, 36, .86)", color: "#fff", fontSize: 34, lineHeight: 1.35, padding: "14px 28px", borderRadius: 14, textAlign: "center" }}>{texto}</div>
    </div>
  );
};

const TituloEscena: React.FC<{ n: number; de: number; titulo: string }> = ({ n, de, titulo }) => {
  const frame = useCurrentFrame();
  return (
    <div style={{ position: "absolute", top: 118, right: 40, fontFamily, display: "flex", gap: 14, alignItems: "center", background: "rgba(255,255,255,.96)", borderRadius: 999, padding: "10px 24px 10px 10px", boxShadow: "0 6px 20px rgba(20,30,50,.18)", translate: interpolate(frame, [0, 14], ["0px -12px", "0px 0px"], { extrapolateRight: "clamp" }), opacity: interpolate(frame, [0, 14], [0, 1], { extrapolateRight: "clamp" }) }}>
      <span style={{ background: NARANJA, color: "#fff", borderRadius: 999, width: 46, height: 46, display: "grid", placeItems: "center", fontSize: 24, fontWeight: 800 }}>{n}</span>
      <span style={{ fontSize: 28, fontWeight: 700, color: "#1d2433" }}>{titulo}</span>
      <span style={{ fontSize: 22, color: "#8a93a3" }}>{n} / {de}</span>
    </div>
  );
};

export const Tutorial: React.FC<TutorialProps> = ({ rol, titulo, escenas, pasos, duracion }) => {
  const total = INTRO + duracion + OUTRO;
  // música: baja mientras habla la voz (con medio segundo de rampa)
  const volumenMusica = (f: number) => {
    const t = f / FPS - INTRO;
    const enVoz = pasos.some((p) => t > p.t && t < p.t + p.dur);
    const cerca = pasos.some((p) => t > p.t - 0.5 && t < p.t + p.dur + 0.5);
    const base = enVoz ? 0.06 : cerca ? 0.1 : 0.2;
    return base * interpolate(f, [0, s2f(1.5), s2f(total - 2.5), s2f(total)], [0, 1, 1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  };
  return (
    <AbsoluteFill style={{ backgroundColor: "#fff" }}>
      <Audio src={staticFile(`tutoriales/${rol}/musica.wav`)} volume={volumenMusica} />
      <Sequence durationInFrames={s2f(INTRO)}>
        <Portada titulo={titulo} sub="Tutorial paso a paso" />
      </Sequence>
      <Sequence from={s2f(INTRO)} durationInFrames={s2f(duracion)} name="Grabación">
        <Video src={staticFile(`tutoriales/${rol}/video.webm`)} muted style={{ width: "100%", height: "100%" }} />
      </Sequence>
      {escenas.map((e, i) => (
        <Sequence key={e.id} name={`Escena ${e.id}`} from={s2f(INTRO + e.t)} durationInFrames={s2f((escenas[i + 1]?.t ?? duracion) - e.t)}>
          <TituloEscena n={i + 1} de={escenas.length} titulo={e.titulo} />
        </Sequence>
      ))}
      {pasos.map((p) => (
        <Sequence key={p.id} name={`Paso ${p.id}`} from={s2f(INTRO + p.t)} durationInFrames={Math.max(1, s2f(p.dur))}>
          <Audio src={staticFile(p.archivo)} />
          <Subtitulo texto={p.texto} />
        </Sequence>
      ))}
      <Sequence from={s2f(INTRO + duracion)} durationInFrames={s2f(OUTRO)}>
        <Portada titulo="¿Te quedó alguna duda?" sub="Abrí el manual desde tu nombre → Manual, arriba a la derecha" />
      </Sequence>
    </AbsoluteFill>
  );
};
