import { useEffect, useMemo, useState } from 'react';
import AppShell from '../../components/AppShell';
import GradeChip from '../../components/ui/GradeChip';
import ContentState from '../../components/ui/ContentState';
import AnimatedSelect from '../../components/AnimatedSelect';
import DatePicker from '../../components/DatePicker';
import FiltersToolbar, { FilterField } from '../../components/ui/FiltersToolbar';
import { agruparTareasPorMes, filtrarConducta, filtrarMaterias, hayFiltroConducta, MATERIAS_PARA_BUSCADOR, materiasDeConducta, mesesConTareas, NOMBRES_MESES, rangoInvalido, SIN_FILTRO_CONDUCTA, type FiltroConducta } from './parentFilters';
import { getParentSummary, downloadReporteMensual, downloadLibreta, getRasgosConducta, type ParentResponse, type ParentStage, type ParentSubject, type ParentTaskStatus, type RasgoConducta } from '../../api/parent';
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
  const [reportMes, setReportMes] = useState(() => new Date().getMonth() + 1);
  const [downloading, setDownloading] = useState<'mensual' | 'libreta' | null>(null);
  const [downloadMsg, setDownloadMsg] = useState('');
  const [conducta, setConducta] = useState<RasgoConducta[] | null>(null);
  const [conductaError, setConductaError] = useState('');
  const [busquedaMateria, setBusquedaMateria] = useState('');
  const [filtroConducta, setFiltroConducta] = useState<FiltroConducta>(SIN_FILTRO_CONDUCTA);

  async function handleDownload(kind: 'mensual' | 'libreta', alumnoId: number) {
    setDownloading(kind);
    setDownloadMsg('');
    try {
      if (kind === 'mensual') {
        await downloadReporteMensual(alumnoId, reportMes, new Date().getFullYear());
      } else {
        await downloadLibreta(alumnoId);
      }
    } catch (e) {
      setDownloadMsg(e instanceof ApiError ? e.message : 'No se pudo generar el PDF.');
    } finally {
      setDownloading(null);
    }
  }

  function load(alumnoId?: number) {
    setError('');
    getParentSummary(alumnoId)
      .then((result) => {
        setData(result);
        const availableStages = new Set(result.materias.map((subject) => subject.etapa));
        const preferredStage = availableStages.has(currentStage()) ? currentStage() : result.materias[0]?.etapa ?? currentStage();
        setStage(preferredStage);
        setSelectedPlanillaId(null);
        setBusquedaMateria('');
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : 'No se pudo cargar el resumen.'));
  }

  useEffect(() => load(), []);

  useEffect(() => {
    if (!data || !data.selectedAlumnoId) { setConducta(null); return; }
    const alumnoId = data.selectedAlumnoId;
    setConductaError('');
    setConducta(null);
    setFiltroConducta(SIN_FILTRO_CONDUCTA);
    getRasgosConducta(alumnoId)
      .then((rows) => setConducta(rows))
      .catch((e) => setConductaError(e instanceof ApiError ? e.message : 'No se pudieron cargar las notas de conducta.'));
  }, [data]);

  useEffect(() => {
    if (!data) return;
    const subjects = data.materias.filter((subject) => subject.etapa === stage);
    if (!subjects.some((subject) => subject.planillaId === selectedPlanillaId)) {
      setSelectedPlanillaId(subjects[0]?.planillaId ?? null);
    }
  }, [data, selectedPlanillaId, stage]);

  if (!data) {
    return <AppShell title="Notas de mis hijos"><ContentState tone={error ? 'error' : 'loading'} title={error || 'Cargando calificaciones…'} detail={error ? 'Recargá la página para volver a intentarlo.' : 'Estamos reuniendo las materias y tareas publicadas.'} /></AppShell>;
  }

  const selectedChild = data.hijos.find((child) => child.id === data.selectedAlumnoId);
  const conductaVisible = conducta ? filtrarConducta(conducta, filtroConducta) : [];
  const subjects = data.materias.filter((subject) => subject.etapa === stage);
  const materiasVisibles = filtrarMaterias(subjects, busquedaMateria);
  const mostrarBuscador = subjects.length >= MATERIAS_PARA_BUSCADOR;
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
            <div className="parent-report-actions">
              <div className="parent-report-month">
                <AnimatedSelect ariaLabel="Mes del reporte" value={reportMes} onChange={(value) => setReportMes(Number(value))} options={NOMBRES_MESES.map((label, index) => ({ value: index + 1, label }))} />
              </div>
              <button className="button secondary" type="button" disabled={downloading !== null} onClick={() => void handleDownload('mensual', selectedChild.id)}>
                {downloading === 'mensual' ? 'Generando…' : 'Descargar reporte mensual'}
              </button>
              <button className="button secondary" type="button" disabled={downloading !== null || !data.libretaDisponible} title={data.libretaDisponible ? undefined : 'Disponible cuando el colegio cierre la Segunda Etapa'} onClick={() => void handleDownload('libreta', selectedChild.id)}>
                {downloading === 'libreta' ? 'Generando…' : 'Descargar libreta'}
              </button>
              <button className="button secondary parent-print-button" type="button" onClick={() => window.print()}>Imprimir resumen</button>
            </div>
          </header>
          {downloadMsg && <p className="notice error" role="alert">{downloadMsg}</p>}
          <div className="parent-overview-metrics">
            <article><span>Promedio general</span><strong>{selectedChild.promedio}%</strong><small>Todas las etapas publicadas</small></article>
            <article><span>{stageLabel(stage)}</span><strong>{stageAverage}%</strong><small>{subjects.length} {subjects.length === 1 ? 'materia' : 'materias'}</small></article>
            <article><span>Por revisar</span><strong>{pendingTasks}</strong><small>{missingTasks > 0 ? `${missingTasks} sin entregar` : 'Sin tareas vencidas'}</small></article>
          </div>
          <div className="parent-stage-comparison" aria-label="Promedio por etapa">
            {stageAverages.map((item) => <div key={item.value}><span>{item.label}</span><strong>{item.average === null ? 'Sin datos' : `${item.average}%`}</strong></div>)}
          </div>
        </section>}

        <FiltersToolbar ariaLabel="Filtros de materias" mostrando={materiasVisibles.length} total={subjects.length} unidad={subjects.length === 1 ? 'materia publicada' : 'materias publicadas'} activo={Boolean(busquedaMateria)} onLimpiar={() => setBusquedaMateria('')}>
          <div className="filter-field filter-field--auto">
            <span>Etapa</span>
            <div className="parent-stage-tabs" role="group" aria-label="Filtrar materias por etapa">
              {STAGES.map((item) => <button type="button" key={item.value} className={stage === item.value ? 'active' : ''} aria-pressed={stage === item.value} onClick={() => setStage(item.value)}>{item.label}</button>)}
            </div>
          </div>
          {mostrarBuscador && <FilterField label="Buscar materia"><input type="search" placeholder="Nombre de la materia" value={busquedaMateria} onChange={(event) => setBusquedaMateria(event.target.value)} /></FilterField>}
        </FiltersToolbar>

        {subjects.length > 0 ? <>
          <section className="parent-subject-section" aria-labelledby="parent-subjects-title">
            <div className="parent-section-heading"><div><span>Materias</span><h2 id="parent-subjects-title">Promedios de {stageLabel(stage).toLowerCase()}</h2></div><p>Seleccioná una materia para ver sus tareas y calificaciones.</p></div>
            {materiasVisibles.length === 0 ? <ContentState compact title="Ninguna materia coincide" detail={`No hay materias que coincidan con “${busquedaMateria.trim()}”.`} actions={<button type="button" className="button secondary" onClick={() => setBusquedaMateria('')}>Limpiar búsqueda</button>} /> : <div className="parent-subject-grid">
              {materiasVisibles.map((subject) => <SubjectCard key={subject.planillaId} subject={subject} selected={subject.planillaId === selectedSubject?.planillaId} onSelect={() => setSelectedPlanillaId(subject.planillaId)} />)}
            </div>}
          </section>
          {selectedSubject && <SubjectDetail key={selectedSubject.planillaId} subject={selectedSubject} />}
          <details className="panel parent-calculation-note"><summary>¿Cómo se calcula el promedio?</summary><p>El porcentaje de cada materia se obtiene dividiendo los puntos logrados entre los puntos posibles de las tareas publicadas. El promedio general combina los puntos de todas las materias disponibles.</p></details>
        </> : <ContentState title={`Sin calificaciones en ${stageLabel(stage).toLowerCase()}`} detail="Todavía no hay materias ni tareas publicadas para este alumno en la etapa seleccionada." />}

        {selectedChild && (
          <section className="panel" aria-labelledby="parent-conducta-title" style={{ marginTop: 16 }}>
            <header className="panel-header">
              <div>
                <span>Conducta</span>
                <h2 id="parent-conducta-title">Notas de conducta</h2>
                <p>Registros conductuales asignados por profesores durante las clases.</p>
              </div>
            </header>
            {conductaError ? (
              <ContentState tone="error" title="No se pudieron cargar las notas de conducta" detail={conductaError} />
            ) : conducta === null ? (
              <ContentState tone="loading" title="Cargando notas de conducta…" />
            ) : conducta.length === 0 ? (
              <ContentState tone="empty" title="Sin notas de conducta" detail={`${selectedChild.nombre} no tiene notas conductuales registradas.`} />
            ) : <>
              <FiltersToolbar ariaLabel="Filtros de notas de conducta" mostrando={conductaVisible.length} total={conducta.length} unidad="notas" activo={hayFiltroConducta(filtroConducta)} onLimpiar={() => setFiltroConducta(SIN_FILTRO_CONDUCTA)}>
                <FilterField label="Materia"><AnimatedSelect ariaLabel="Materia de la nota de conducta" value={filtroConducta.materia} onChange={(materia) => setFiltroConducta((actual) => ({ ...actual, materia }))} options={[{ value: '', label: 'Todas las materias' }, ...materiasDeConducta(conducta).map((materia) => ({ value: materia, label: materia }))]} /></FilterField>
                <FilterField label="Desde" narrow><DatePicker ariaLabel="Conducta desde" value={filtroConducta.desde} invalid={rangoInvalido(filtroConducta)} onChange={(desde) => setFiltroConducta((actual) => ({ ...actual, desde }))} /></FilterField>
                <FilterField label="Hasta" narrow><DatePicker ariaLabel="Conducta hasta" value={filtroConducta.hasta} invalid={rangoInvalido(filtroConducta)} onChange={(hasta) => setFiltroConducta((actual) => ({ ...actual, hasta }))} /></FilterField>
                {rangoInvalido(filtroConducta) && <p className="filters-error" role="alert">La fecha “Desde” es posterior a “Hasta”: no hay notas en ese rango.</p>}
              </FiltersToolbar>
              {conductaVisible.length === 0 ? <ContentState compact title="Sin resultados" detail="Ninguna nota de conducta coincide con los filtros elegidos." actions={<button type="button" className="button secondary" onClick={() => setFiltroConducta(SIN_FILTRO_CONDUCTA)}>Limpiar filtros</button>} /> : <div className="table-scroll">
                <table className="grade-table" style={{ minWidth: 720 }}>
                  <thead>
                    <tr>
                      <th>Fecha</th>
                      <th>Materia</th>
                      <th>Profesor</th>
                      <th>Código</th>
                      <th>Descripción</th>
                      <th>Observación</th>
                    </tr>
                  </thead>
                  <tbody>
                    {conductaVisible.map((row, index) => (
                      <tr key={`${row.fechaClase ?? 'sf'}-${row.codigo}-${index}`}>
                        <td>{formatDate(row.fechaClase)}</td>
                        <td>{row.materia ?? '—'}</td>
                        <td>{row.profesorNombre ?? '—'}</td>
                        <td><strong>{row.codigo}</strong></td>
                        <td>{row.descripcion ?? '—'}</td>
                        <td>{row.observacion ?? '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>}
            </>}
          </section>
        )}
      </div>
    </AppShell>
  );
}

function SubjectCard({ subject, selected, onSelect }: { subject: ParentSubject; selected: boolean; onSelect: () => void }) {
  const pending = subject.tareas.filter((task) => task.estado !== 'CALIFICADA').length;
  return <button type="button" className={`parent-subject-card${selected ? ' selected' : ''}`} aria-pressed={selected} onClick={onSelect}>
    <header><div><span>Materia</span><h3>{subject.materia}</h3></div><GradeChip grade={subject.nota} label={`Nota ${subject.nota}`} className="parent-subject-grade" /></header>
    <div className="parent-subject-average"><strong>{subject.porcentaje}%</strong><span>Promedio de la materia</span></div>
    <div className="parent-subject-progress" aria-label={`${subject.porcentaje}%`}><i style={{ width: `${Math.min(100, Math.max(0, subject.porcentaje))}%` }} /></div>
    <footer><span>{subject.puntos} de {subject.total} puntos</span><span>{subject.tareas.length} tareas{pending > 0 ? ` · ${pending} por revisar` : ''}</span></footer>
    <i className="parent-subject-arrow" aria-hidden="true">→</i>
  </button>;
}

function SubjectDetail({ subject }: { subject: ParentSubject }) {
  const [mes, setMes] = useState('');
  const meses = useMemo(() => mesesConTareas(subject.tareas), [subject.tareas]);
  const grupos = useMemo(() => agruparTareasPorMes(subject.tareas, mes), [subject.tareas, mes]);
  const visibles = grupos.reduce((suma, grupo) => suma + grupo.tareas.length, 0);
  return <section className="panel parent-subject-detail" aria-labelledby="parent-subject-detail-title">
    <header className="parent-subject-detail-header">
      <div><span>Detalle de tareas</span><h2 id="parent-subject-detail-title">{subject.materia}</h2><p>{stageLabel(subject.etapa)} · {subject.porcentaje}% de promedio</p></div>
      <GradeChip grade={subject.nota} className="parent-detail-grade" />
    </header>
    {subject.tareas.length > 0 ? <>
      {meses.length > 1 && <div className="parent-task-filters">
        <FiltersToolbar ariaLabel="Filtros de tareas" mostrando={visibles} total={subject.tareas.length} unidad="tareas" activo={Boolean(mes)} onLimpiar={() => setMes('')}>
          <FilterField label="Mes" narrow><AnimatedSelect ariaLabel="Mes de las tareas" value={mes} onChange={setMes} options={[{ value: '', label: 'Todos los meses' }, ...meses]} /></FilterField>
        </FiltersToolbar>
      </div>}
      {grupos.map((grupo) => <section className="parent-task-month-group" key={grupo.mes} aria-labelledby={`parent-task-month-${grupo.mes}`}>
        <h3 className="parent-task-month" id={`parent-task-month-${grupo.mes}`}>{grupo.label}<small>{grupo.tareas.length} {grupo.tareas.length === 1 ? 'tarea' : 'tareas'}</small></h3>
        <div className="parent-task-list">
          {grupo.tareas.map(({ tarea: task, numero }) => <article className={`parent-task-row ${task.estado.toLowerCase().replaceAll('_', '-')}`} key={task.id}>
            <span className="parent-task-number">{String(numero).padStart(2, '0')}</span>
            <div className="parent-task-copy"><strong>{task.titulo}</strong><small>{formatDate(task.fecha)}</small></div>
            <TaskResult estado={task.estado} puntos={task.puntos} total={task.total} />
          </article>)}
        </div>
      </section>)}
    </> : <ContentState compact className="parent-task-empty" title="Sin tareas publicadas" detail="Esta materia todavía no tiene actividades disponibles." />}
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
