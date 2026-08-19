import { useEffect, useState } from 'react';
import AppShell from '../../components/AppShell';
import { getParentSummary, type ParentResponse, type ParentStage, type ParentSubject, type ParentTaskStatus } from '../../api/parent';
import { ApiError } from '../../api/client';
import { normalizeSpecialty } from '../../theme/theme';

const STAGES: Array<{ value: ParentStage; label: string }> = [
  { value: 'primera', label: 'Primera etapa' },
  { value: 'segunda', label: 'Segunda etapa' },
];

function currentStage(): ParentStage {
  const today = new Date();
  return today.getMonth() > 6 || (today.getMonth() === 6 && today.getDate() >= 15) ? 'segunda' : 'primera';
}

function formatDate(value?: string | null) {
  if (!value) return 'Sin fecha';
  return new Intl.DateTimeFormat('es-PY', { day: '2-digit', month: 'short', year: 'numeric', timeZone: 'UTC' }).format(new Date(`${value}T00:00:00Z`));
}

function stageLabel(stage: ParentStage) {
  return STAGES.find((item) => item.value === stage)?.label ?? stage;
}

export default function ParentPage() {
  const [data, setData] = useState<ParentResponse | null>(null);
  const [error, setError] = useState('');
  const [stage, setStage] = useState<ParentStage>(currentStage);
  const [selectedPlanillaId, setSelectedPlanillaId] = useState<number | null>(null);

  function load(alumnoId?: number) {
    setError('');
    getParentSummary(alumnoId)
      .then((result) => {
        setData(result);
        const availableStages = new Set(result.materias.map((subject) => subject.etapa));
        const preferredStage = availableStages.has(currentStage()) ? currentStage() : result.materias[0]?.etapa ?? currentStage();
        setStage(preferredStage);
        setSelectedPlanillaId(null);
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : 'No se pudo cargar el resumen.'));
  }

  useEffect(() => load(), []);

  useEffect(() => {
    if (!data) return;
    const subjects = data.materias.filter((subject) => subject.etapa === stage);
    if (!subjects.some((subject) => subject.planillaId === selectedPlanillaId)) {
      setSelectedPlanillaId(subjects[0]?.planillaId ?? null);
    }
  }, [data, selectedPlanillaId, stage]);

  if (!data) {
    return <AppShell title="Notas de mis hijos"><section className="panel">{error || 'Cargando…'}</section></AppShell>;
  }

  const selectedChild = data.hijos.find((child) => child.id === data.selectedAlumnoId);
  const subjects = data.materias.filter((subject) => subject.etapa === stage);
  const selectedSubject = subjects.find((subject) => subject.planillaId === selectedPlanillaId) ?? null;
  const stagePoints = subjects.reduce((sum, subject) => sum + subject.puntos, 0);
  const stageTotal = subjects.reduce((sum, subject) => sum + subject.total, 0);
  const stageAverage = stageTotal > 0 ? Math.round(stagePoints * 100 / stageTotal) : 0;
  const pendingTasks = subjects.flatMap((subject) => subject.tareas).filter((task) => task.estado !== 'CALIFICADA').length;
  const missingTasks = subjects.flatMap((subject) => subject.tareas).filter((task) => task.estado === 'NO_ENTREGADA').length;
  const latestTaskDate = data.materias.flatMap((subject) => subject.tareas).map((task) => task.fecha).filter(Boolean).sort().at(-1);
  const stageAverages = STAGES.map((item) => {
    const items = data.materias.filter((subject) => subject.etapa === item.value);
    const points = items.reduce((sum, subject) => sum + subject.puntos, 0);
    const total = items.reduce((sum, subject) => sum + subject.total, 0);
    return { ...item, average: total > 0 ? Math.round(points * 100 / total) : null };
  });

  return (
    <AppShell title="Notas de mis hijos" specialty={selectedChild?.especialidad}>
      <div className="parent-page">
        <div className="card-grid child-grid" aria-label="Hijos vinculados">
          {data.hijos.map((child) => (
            <button type="button" key={child.id} data-specialty={normalizeSpecialty(child.especialidad)} className={`nav-card child-card ${data.selectedAlumnoId === child.id ? 'selected' : ''}`} aria-pressed={data.selectedAlumnoId === child.id} onClick={() => load(child.id)}>
              <span>{child.especialidad}</span>
              <h2>{child.apellido}, {child.nombre}</h2>
              <strong>Promedio general: {child.promedio}%</strong>
            </button>
          ))}
        </div>

        {selectedChild && <section className="panel parent-overview" aria-labelledby="parent-overview-title">
          <header className="parent-overview-header">
            <div><span>Resumen académico</span><h2 id="parent-overview-title">{selectedChild.nombre} {selectedChild.apellido}</h2><p>{selectedChild.especialidad} · Actividad hasta {formatDate(latestTaskDate)}</p></div>
            <button className="button secondary parent-print-button" type="button" onClick={() => window.print()}>Imprimir resumen</button>
          </header>
          <div className="parent-overview-metrics">
            <article><span>Promedio general</span><strong>{selectedChild.promedio}%</strong><small>Todas las etapas publicadas</small></article>
            <article><span>{stageLabel(stage)}</span><strong>{stageAverage}%</strong><small>{subjects.length} {subjects.length === 1 ? 'materia' : 'materias'}</small></article>
            <article><span>Por revisar</span><strong>{pendingTasks}</strong><small>{missingTasks > 0 ? `${missingTasks} sin entregar` : 'Sin tareas vencidas'}</small></article>
          </div>
          <div className="parent-stage-comparison" aria-label="Promedio por etapa">
            {stageAverages.map((item) => <div key={item.value}><span>{item.label}</span><strong>{item.average === null ? 'Sin datos' : `${item.average}%`}</strong></div>)}
          </div>
        </section>}

        <div className="parent-subject-toolbar">
          <div className="parent-stage-tabs" role="group" aria-label="Filtrar materias por etapa">
            {STAGES.map((item) => <button type="button" key={item.value} className={stage === item.value ? 'active' : ''} aria-pressed={stage === item.value} onClick={() => setStage(item.value)}>{item.label}</button>)}
          </div>
          <span>{subjects.length} {subjects.length === 1 ? 'materia publicada' : 'materias publicadas'}</span>
        </div>

        {subjects.length > 0 ? <>
          <section className="parent-subject-section" aria-labelledby="parent-subjects-title">
            <div className="parent-section-heading"><div><span>Materias</span><h2 id="parent-subjects-title">Promedios de {stageLabel(stage).toLowerCase()}</h2></div><p>Seleccioná una materia para ver sus tareas y calificaciones.</p></div>
            <div className="parent-subject-grid">
              {subjects.map((subject) => <SubjectCard key={subject.planillaId} subject={subject} selected={subject.planillaId === selectedSubject?.planillaId} onSelect={() => setSelectedPlanillaId(subject.planillaId)} />)}
            </div>
          </section>
          {selectedSubject && <SubjectDetail subject={selectedSubject} />}
          <details className="panel parent-calculation-note"><summary>¿Cómo se calcula el promedio?</summary><p>El porcentaje de cada materia se obtiene dividiendo los puntos logrados entre los puntos posibles de las tareas publicadas. El promedio general combina los puntos de todas las materias disponibles.</p></details>
        </> : <section className="panel empty-state"><h2>Sin calificaciones en {stageLabel(stage).toLowerCase()}</h2><p>Todavía no hay materias ni tareas publicadas para este alumno en la etapa seleccionada.</p></section>}
      </div>
    </AppShell>
  );
}

function SubjectCard({ subject, selected, onSelect }: { subject: ParentSubject; selected: boolean; onSelect: () => void }) {
  const pending = subject.tareas.filter((task) => task.estado !== 'CALIFICADA').length;
  return <button type="button" className={`parent-subject-card${selected ? ' selected' : ''}`} aria-pressed={selected} onClick={onSelect}>
    <header><div><span>Materia</span><h3>{subject.materia}</h3></div><span className={`grade-chip grade-chip--${Math.min(5, Math.max(1, subject.nota))} parent-subject-grade`}>Nota {subject.nota}</span></header>
    <div className="parent-subject-average"><strong>{subject.porcentaje}%</strong><span>Promedio de la materia</span></div>
    <div className="parent-subject-progress" aria-label={`${subject.porcentaje}%`}><i style={{ width: `${Math.min(100, Math.max(0, subject.porcentaje))}%` }} /></div>
    <footer><span>{subject.puntos} de {subject.total} puntos</span><span>{subject.tareas.length} tareas{pending > 0 ? ` · ${pending} por revisar` : ''}</span></footer>
    <i className="parent-subject-arrow" aria-hidden="true">→</i>
  </button>;
}

function SubjectDetail({ subject }: { subject: ParentSubject }) {
  return <section className="panel parent-subject-detail" aria-labelledby="parent-subject-detail-title">
    <header className="parent-subject-detail-header">
      <div><span>Detalle de tareas</span><h2 id="parent-subject-detail-title">{subject.materia}</h2><p>{stageLabel(subject.etapa)} · {subject.porcentaje}% de promedio</p></div>
      <span className={`grade-chip grade-chip--${Math.min(5, Math.max(1, subject.nota))} parent-detail-grade`}>{subject.nota}</span>
    </header>
    {subject.tareas.length > 0 ? <div className="parent-task-list">
      {subject.tareas.map((task, index) => <article className={`parent-task-row ${task.estado.toLowerCase().replaceAll('_', '-')}`} key={task.id}>
        <span className="parent-task-number">{String(index + 1).padStart(2, '0')}</span>
        <div className="parent-task-copy"><strong>{task.titulo}</strong><small>{formatDate(task.fecha)}</small></div>
        <TaskResult estado={task.estado} puntos={task.puntos} total={task.total} />
      </article>)}
    </div> : <div className="empty-state parent-task-empty"><h3>Sin tareas publicadas</h3><p>Esta materia todavía no tiene actividades disponibles.</p></div>}
  </section>;
}

function TaskResult({ estado, puntos, total }: { estado: ParentTaskStatus; puntos: number | null; total: number }) {
  if (estado === 'CALIFICADA') {
    const percentage = total > 0 ? Math.round((puntos ?? 0) * 100 / total) : 0;
    return <div className="parent-task-result graded"><strong>{puntos ?? 0} / {total}</strong><small>{percentage}% · Calificada</small></div>;
  }

  const labels: Record<Exclude<ParentTaskStatus, 'CALIFICADA'>, string> = {
    ENTREGADA_PENDIENTE: 'Entregada · sin calificar',
    NO_ENTREGADA: 'No entregada',
    PENDIENTE: 'Pendiente',
  };

  return <div className="parent-task-result"><span className={`parent-task-status ${estado.toLowerCase().replaceAll('_', '-')}`}>{labels[estado]}</span><small>de {total} puntos</small></div>;
}
