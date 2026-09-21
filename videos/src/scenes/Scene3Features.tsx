import {
  AbsoluteFill,
  Easing,
  Interactive,
  Sequence,
  interpolate,
  useCurrentFrame,
} from "remotion";
import { Background } from "../components/Background";
import { BrandCorner } from "../components/BrandCorner";
import { ScreenMockup } from "../components/ScreenMockup";
import {
  AdministracionMock,
  CoordinacionMock,
  EvaluacionMock,
  ExcelMock,
  FamiliasMock,
  PlanillaMock,
} from "../components/mockups";
import { fontFamily, theme } from "../theme";

const userViews = [
  {
    role: "Profesores",
    body: "Suben tareas y calificaciones sincronizadas con Google Classroom, cargan el plan curricular y verifican el tema de cada clase desde el Libro de Cátedra, y llevan el horario por hora cátedra siempre al día.",
    screenStack: [
      { title: "planilla-ejemplo.xlsx", content: <ExcelMock /> },
      { title: "SCA · Planilla", content: <PlanillaMock /> },
    ],
    screen: null,
    duration: 210,
  },
  {
    role: "Evaluación",
    body: "Aprueban o rechazan el plan curricular de cada profesor, hacen seguimiento de cumplimiento y atrasos, reabren una etapa cerrada cuando hace falta corregir una nota, y descargan las planillas completas de cada curso.",
    screenStack: null,
    screen: { title: "SCA · Panel de Evaluación", content: <EvaluacionMock /> },
    duration: 210,
  },
  {
    role: "Coordinación Pedagógica",
    body: "Reciben y gestionan las quejas cargadas sobre cada profesor con todo su ciclo — aceptación, revisión y solución documentada — y administran el catálogo de códigos de conducta que se registra en cada clase.",
    screenStack: null,
    screen: { title: "SCA · Coordinación Pedagógica", content: <CoordinacionMock /> },
    duration: 210,
  },
  {
    role: "Administración",
    body: "Gestionan especialidades, usuarios, asignaciones, horarios y salas desde un panel central, dan de alta cursos y secciones, y mantienen el control académico de todo el colegio en un solo lugar.",
    screenStack: null,
    screen: { title: "SCA · Panel de Administración", content: <AdministracionMock /> },
    duration: 210,
  },
  {
    role: "Familias",
    body: "Consultan notas, promedios y tareas de sus hijos por etapa, reciben notificaciones push ante cada nueva calificación, y descargan el reporte mensual o la libreta final — todo desde el celular.",
    screenStack: null,
    screen: { title: "SCA · Panel de Familias", content: <FamiliasMock /> },
    duration: 210,
  },
] as const;

export const SCENE3_FEATURES_DURATION = userViews.reduce((a, f) => a + f.duration, 0);

const FADE = 16;

const UserViewSlide: React.FC<{
  view: (typeof userViews)[number];
  index: number;
  isLast: boolean;
}> = ({ view, index, isLast }) => {
  const localFrame = useCurrentFrame();
  const slotDuration = view.duration;

  const opacity = interpolate(
    localFrame,
    isLast ? [0, FADE] : [0, FADE, slotDuration - FADE, slotDuration],
    isLast ? [0, 1] : [0, 1, 1, 0],
    {
      extrapolateLeft: "clamp",
      extrapolateRight: "clamp",
      easing: Easing.bezier(0.16, 1, 0.3, 1),
    },
  );

  const textTranslateX = interpolate(localFrame, [0, FADE], [-40, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.16, 1, 0.3, 1),
  });
  const underlineWidth = interpolate(localFrame, [FADE, FADE + 20], [0, 140], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  const screenScale = interpolate(localFrame, [0, FADE + 10], [0.9, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.16, 1, 0.3, 1),
    output: "perceptual-scale",
  });
  const screenOpacity = interpolate(localFrame, [4, FADE + 6], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  return (
    <Interactive.Div
      name={`User view ${index + 1}`}
      style={{
        position: "absolute",
        opacity,
        display: "flex",
        flexDirection: "row",
        alignItems: "center",
        justifyContent: "center",
        gap: 80,
        padding: "0 90px",
      }}
    >
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          alignItems: "flex-start",
          textAlign: "left",
          gap: 18,
          width: 700,
          translate: `${textTranslateX}px 0px`,
        }}
      >
        <div
          style={{
            fontSize: 24,
            fontWeight: 700,
            color: theme.accentLight,
            letterSpacing: "0.05em",
          }}
        >
          {String(index + 1).padStart(2, "0")} · VISTA DE USUARIO
        </div>
        <div
          style={{
            fontSize: 56,
            fontWeight: 800,
            color: theme.text,
            letterSpacing: "-0.02em",
            lineHeight: 1.1,
          }}
        >
          {view.role}
        </div>
        <div
          style={{
            width: underlineWidth,
            height: 5,
            borderRadius: 3,
            background: theme.accent,
          }}
        />
        <div
          style={{
            fontSize: 27,
            fontWeight: 500,
            color: theme.textMuted,
            lineHeight: 1.4,
          }}
        >
          {view.body}
        </div>
      </div>

      <div
        style={{
          opacity: screenOpacity,
          scale: screenScale,
          transform: "perspective(1600px) rotateX(2deg)",
          filter: `drop-shadow(0 40px 80px ${theme.accent}28)`,
        }}
      >
        {view.screenStack ? (
          <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
            {view.screenStack.map((item) => (
              <ScreenMockup key={item.title} title={item.title} width={580}>
                {item.content}
              </ScreenMockup>
            ))}
          </div>
        ) : (
          view.screen && (
            <ScreenMockup title={view.screen.title} width={820}>
              {view.screen.content}
            </ScreenMockup>
          )
        )}
      </div>
    </Interactive.Div>
  );
};

export const Scene3Features: React.FC = () => {
  const frame = useCurrentFrame();

  let cursor = 0;
  const starts = userViews.map((v) => {
    const start = cursor;
    cursor += v.duration;
    return start;
  });

  return (
    <AbsoluteFill style={{ fontFamily, justifyContent: "center", alignItems: "center" }}>
      <Background />
      <BrandCorner />
      <Interactive.Div
        name="Section label"
        style={{
          position: "absolute",
          top: 90,
          left: 0,
          right: 0,
          textAlign: "center",
          fontSize: 30,
          fontWeight: 700,
          letterSpacing: "0.16em",
          textTransform: "uppercase",
          color: theme.accentLight,
          opacity: interpolate(frame, [0, 15], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        Una vista para cada usuario
      </Interactive.Div>

      {userViews.map((view, i) => (
        <Sequence
          key={view.role}
          from={starts[i]}
          durationInFrames={view.duration}
          layout="none"
        >
          <UserViewSlide view={view} index={i} isLast={i === userViews.length - 1} />
        </Sequence>
      ))}

      <div
        style={{
          position: "absolute",
          bottom: 90,
          display: "flex",
          gap: 14,
        }}
      >
        {userViews.map((view, i) => {
          const active = frame >= starts[i] && frame < starts[i] + view.duration;
          return (
            <div
              key={view.role}
              style={{
                width: active ? 34 : 14,
                height: 8,
                borderRadius: 4,
                background: active ? theme.accentLight : theme.border,
              }}
            />
          );
        })}
      </div>
    </AbsoluteFill>
  );
};
