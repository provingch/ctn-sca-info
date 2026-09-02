import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import AppShell from '../../components/AppShell';
import ClassroomBadge from '../../components/ClassroomBadge';
import GradeChip from '../../components/ui/GradeChip';
import ContentState from '../../components/ui/ContentState';
import AnimatedSelect from '../../components/AnimatedSelect';
import { getPlanilla, resolvePlanilla, syncClassroom, confirmClassroomMapping, saveGrades, type PlanillaDetail } from '../../api/academics';
import { ApiError, apiDownload } from '../../api/client';
import { useToast } from '../../context/toast';

// Etiquetas de nota en orden descendente (5 -> 1), igual que el JSP legacy
// (Planilla.jsp: chips grade-chip--five..one). "1" no tiene rango propio en
// gradeRanges (es "todo lo que quede por debajo del piso de 2"), así que se
// arma su etiqueta a partir del minInclusive de "2".
const GRADE_KEYS_DESC = ['5', '4', '3', '2'] as const;
type StudentSortKey = 'total' | 'percentage' | 'grade';
type SortDirection = 'ascending' | 'descending';

function normalizeStudentName(value: string) {
  return value.toLocaleLowerCase('es').normalize('NFD').replace(/[\u0300-\u036f]/g, '');
}

function createGradeValues(result: PlanillaDetail) {
  const initial: Record<string, string> = {};
  result.rows.forEach((row) => row.grades.forEach((grade) => {
    initial[`${row.alumnoId}:${grade.tareaId}`] = grade.puntos == null ? '' : String(grade.puntos);
  }));
  return initial;
}

function formatShortDate(value: string | null | undefined) {
  if (!value) return '—';
  return new Intl.DateTimeFormat('es-PY', { day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'UTC' }).format(new Date(`${value}T00:00:00Z`));
}

function StageCombobox({ value, disabled, onChange }: { value: number; disabled: boolean; onChange: (value: number) => void }) {
  return <AnimatedSelect
    className="stage-combobox"
    ariaLabel="Etapa"
    value={value}
    disabled={disabled}
    onChange={(nextValue) => onChange(Number(nextValue))}
    options={[{ value: 1, label: 'Primera etapa' }, { value: 2, label: 'Segunda etapa' }]}
  />;
}

export default function PlanillaPage() {
  const id = Number(useParams().planillaId);
  const navigate = useNavigate();
  const [data, setData] = useState<PlanillaDetail | null>(null);
  const [values, setValues] = useState<Record<string, string>>({});
  const { showToast } = useToast();
  const setStatus = useCallback((text: string) => { if (text) showToast(text); }, [showToast]);
  const [resolvedCourse, setResolvedCourse] = useState<{ googleCourseId?: string | null; classroomCourseMapped?: boolean; courseName?: string | null; courseSection?: string | null; courseAlternateLink?: string | null; message?: string } | null>(null);
  const [switchingEtapa, setSwitchingEtapa] = useState(false);
  const [syncingClassroom, setSyncingClassroom] = useState(false);
  const [lastClassroomSync, setLastClassroomSync] = useState<Date | null>(null);
  const [syncSummary, setSyncSummary] = useState<{ created: number; updated: number } | null>(null);
  const [studentSearch, setStudentSearch] = useState('');
  const [studentSort, setStudentSort] = useState<{ key: StudentSortKey; direction: SortDirection } | null>(null);
  const [freezeStudents, setFreezeStudents] = useState(() => localStorage.getItem('planilla-freeze-students') !== 'false');
  const activePlanillaIdRef = useRef(id);
  activePlanillaIdRef.current = id;

  const applyPlanillaData = useCallback((result: PlanillaDetail) => {
    setData(result);
    setValues(createGradeValues(result));
  }, []);

  useEffect(() => {
    if (!Number.isInteger(id)) return;
    let active = true;
    setData(null);
    setResolvedCourse(null);
    setStudentSearch('');
    setStudentSort(null);
    getPlanilla(id).then((result) => {
      if (active) applyPlanillaData(result);
    }).catch((e) => {
      if (active) setStatus(e instanceof ApiError ? e.message : 'No se pudo cargar la planilla.');
    });
    return () => { active = false; };
  }, [applyPlanillaData, id, setStatus]);

  const performClassroomSync = useCallback(async (planillaId: number, automatic = false) => {
    setSyncingClassroom(true);
    try {
      setStatus('Sincronizando tareas y calificaciones de Classroom…');
      const result = await syncClassroom(planillaId);
      const refreshed = await getPlanilla(planillaId);
      if (activePlanillaIdRef.current !== planillaId) return;
      applyPlanillaData(refreshed);
      setResolvedCourse({ googleCourseId: result.googleCourseId, classroomCourseMapped: result.classroomCourseMapped, courseName: result.courseName, courseSection: result.courseSection, courseAlternateLink: result.courseAlternateLink, message: result.message });
      setLastClassroomSync(new Date());
      setSyncSummary({ created: result.importedCourseworks, updated: result.importedGrades });
      setStatus(`${result.message || 'Sincronización completada.'} ${refreshed.tareas.length} ${refreshed.tareas.length === 1 ? 'tarea disponible' : 'tareas disponibles'}.`);
      if (automatic) setTimeout(() => {
        if (activePlanillaIdRef.current === planillaId) setStatus('');
      }, 4000);
    } catch (e) {
      if (activePlanillaIdRef.current === planillaId) {
        if (e instanceof ApiError && e.status === 428) {
          // redirigir a pantalla de autorización si faltan scopes
          navigate('/google/authorize');
          return;
        }
        setStatus(e instanceof ApiError ? e.message : 'No se pudo sincronizar Classroom.');
      }
    } finally {
      if (activePlanillaIdRef.current === planillaId) setSyncingClassroom(false);
    }
  }, [applyPlanillaData, navigate, setStatus]);

  // Se sincroniza una vez por cada planilla/etapa visitada. Un ref booleano
  // impedía sincronizar la segunda etapa al navegar sin desmontar la página.
  const syncedPlanillasRef = useRef(new Set<number>());
  useEffect(() => {
    if (!data || data.planilla.id !== id || syncedPlanillasRef.current.has(id)) return;
    syncedPlanillasRef.current.add(id);
    void performClassroomSync(id, true);
  }, [data, id, performClassroomSync]);

  const computedRows = useMemo(() => {
    if (!data) return [];
    return data.rows.map((row, originalIndex) => {
      const total = data.tareas.reduce((sum, task) => sum + Number(values[`${row.alumnoId}:${task.id}`] || 0), 0);
      const percentage = data.planilla.totalPossiblePoints ? Math.round(total * 100 / data.planilla.totalPossiblePoints) : 0;
      return { row, originalIndex, total, percentage };
    });
  }, [data, values]);

  const handleGradeChange = useCallback((key: string, next: string) => {
    setValues((prev) => ({ ...prev, [key]: next }));
  }, []);

  const handleGradeBlur = useCallback(async (alumnoId: number, tareaId: number, taskTotal: number) => {
    const key = `${alumnoId}:${tareaId}`;
    let raw = values[key];
    let puntos: number | null = null;
    if (raw == null || raw === '') {
      puntos = null;
    } else {
      const n = Number(raw);
      if (Number.isNaN(n)) {
        showToast('Valor inválido');
        return;
      }
      puntos = Math.max(0, Math.min(taskTotal, Math.round(n)));
    }

    // if nothing to save (null treated as 0 in backend), still send to persist
    try {
      setStatus('Guardando calificación…');
      const payload = [{ alumnoId, items: [{ tareaId, puntos: puntos == null ? 0 : puntos }] }];
      const body = await saveGrades(id, payload);
      if (body) {
        if ((body as any).message) showToast((body as any).message);
        if (Array.isArray((body as any).warnings) && (body as any).warnings.length > 0) {
          (body as any).warnings.forEach((w: string) => showToast(w));
        }
      }
      // refrescar la planilla para recalcular totales/notas
      const refreshed = await getPlanilla(id);
      applyPlanillaData(refreshed);
    } catch (e) {
      setStatus(e instanceof ApiError ? e.message : 'No se pudo guardar la nota.');
    }
  }, [applyPlanillaData, id, setStatus, showToast, values]);

  const visibleRows = useMemo(() => {
    const query = normalizeStudentName(studentSearch.trim());
    const filtered = query
      ? computedRows.filter(({ row }) => normalizeStudentName(row.alumnoNombre).includes(query))
      : computedRows.slice();

    return filtered.sort((first, second) => {
      if (!studentSort) return first.originalIndex - second.originalIndex;
      const firstValue = studentSort.key === 'grade' ? first.row.nota : first[studentSort.key];
      const secondValue = studentSort.key === 'grade' ? second.row.nota : second[studentSort.key];
      const numericResult = firstValue - secondValue;
      const result = numericResult || first.row.alumnoNombre.localeCompare(second.row.alumnoNombre, 'es', { sensitivity: 'base' });
      return studentSort.direction === 'ascending' ? result : -result;
    });
  }, [computedRows, studentSearch, studentSort]);

  // Igual que el <select id="etapaSelect"> del JSP legacy: cambiar de etapa
  // resuelve (o crea) la planilla de esa etapa para el mismo curso/materia
  // y navega a su id. resolvePlanilla ya existe en la API (usado también
  // desde HomePage al abrir una materia por primera vez).
  async function changeEtapa(nuevaEtapa: number) {
    if (!data || nuevaEtapa === data.planilla.etapaIndex) return;
    setSwitchingEtapa(true); setStatus('');
    try {
      const result = await resolvePlanilla(data.planilla.cursoId, data.planilla.materiaId, nuevaEtapa);
      // Si la resolución devuelve una planilla distinta, navegamos a ella; si devuelve
      // la misma id, recargamos los datos para evitar quedarnos con el selector bloqueado.
      if (result.planillaId && result.planillaId !== id) {
        navigate(`/planilla/${result.planillaId}`, { replace: true });
      } else {
        // refrescar datos en sitio
        applyPlanillaData(await getPlanilla(result.planillaId));
      }
    } catch (e) {
      setStatus(e instanceof ApiError ? e.message : 'No se pudo cambiar de etapa.');
    } finally {
      // asegurar que el selector quede usable en todos los caminos
      setSwitchingEtapa(false);
    }
  }

  function toggleFreezeStudents(checked: boolean) {
    setFreezeStudents(checked);
    localStorage.setItem('planilla-freeze-students', String(checked));
  }

  function changeStudentSort(key: StudentSortKey) {
    setStudentSort((current) => {
      if (!current || current.key !== key) return { key, direction: 'ascending' };
      if (current.direction === 'ascending') return { key, direction: 'descending' };
      return null;
    });
  }

  if (!data) return <AppShell title="Planilla"><ContentState tone={'loading'} title={'Cargando planilla…'} detail={'Estamos preparando alumnos, tareas y calificaciones.'} /></AppShell>;

  const gr = data.gradeRanges;
  const classroomTaskCount = data.tareas.filter((task) => Boolean(task.googleCourseworkId?.trim())).length;
  const localTaskCount = data.tareas.length - classroomTaskCount;
  // Piso de "1": todo lo que caiga por debajo del mínimo de "2" (igual que
  // en Planilla.jsp: "${gradeRanges['2'][0] - 1} o menos").
  const onePointCeiling = gr['2'] ? gr['2'].minInclusive - 1 : null;

  return (
    <AppShell specialty={data.curso?.especialidad}>
      <div className="planilla-page">
      <section className="planilla-legacy-hero">
        <header className="planilla-hero-header">
          <div>
            <span className="badge">Profesor · {data.curso?.especialidad || 'Planilla'}</span>
            <h1>{data.planilla.materiaNombre}</h1>
            <p>{data.curso ? `${data.curso.nivel}° ${data.curso.seccion}` : 'Curso'} · {data.planilla.etapa}</p>
          </div>
          <div className="planilla-hero-actions">
            <Link className="button secondary" to="/home">← Volver</Link>
            <button className="button" type="button" onClick={async () => {
              try {
                await apiDownload(`/api/planillas/${id}/export`, `planilla-${id}.xlsx`);
              } catch (e) {
                setStatus(e instanceof ApiError ? e.message : 'Error en la descarga');
              }
            }}>Descargar</button>
          </div>
        </header>
        <div className="planilla-legacy-toolbar">
          <div className="inline-filter">
            <span>Etapa</span>
            <StageCombobox value={data.planilla.etapaIndex} disabled={switchingEtapa} onChange={changeEtapa} />
          </div>
          <p className="planilla-date-range"><strong>Desde:</strong> {formatShortDate(data.planilla.planillaDesde)} <strong>Hasta:</strong> {formatShortDate(data.planilla.planillaHasta)}</p>
          <div className="planilla-toolbar-actions">
            <button className="button secondary" type="button" disabled={syncingClassroom} onClick={() => void performClassroomSync(id)}>{syncingClassroom ? 'Sincronizando…' : 'Sincronizar Classroom'}</button>
            <Link className="button" to={`/planilla/${id}/tarea`}>Agregar tarea</Link>
          </div>
        </div>
      </section>
      {lastClassroomSync && <p className="sync-meta" role="status">Última sincronización: {lastClassroomSync.toLocaleString('es-AR', { dateStyle: 'medium', timeStyle: 'short' })}</p>}
      {syncSummary && <div className="sync-summary" aria-label="Resultado de la sincronización">
        <span><strong>{syncSummary.created}</strong> tareas creadas</span>
        <span><strong>{syncSummary.updated}</strong> calificaciones actualizadas</span>
      </div>}
      {resolvedCourse && resolvedCourse.classroomCourseMapped && resolvedCourse.googleCourseId && data.planilla.googleCourseId !== resolvedCourse.googleCourseId && (
        <div className="notice">
          <div>
            Se encontró un curso de Classroom posiblemente correspondiente:
            <div style={{marginTop:6}}><strong>{resolvedCourse.courseName ?? resolvedCourse.googleCourseId}</strong> {resolvedCourse.courseSection && <span>· {resolvedCourse.courseSection}</span>}</div>
            {resolvedCourse.courseAlternateLink && <div style={{marginTop:6}}><a href={resolvedCourse.courseAlternateLink} target="_blank" rel="noopener noreferrer" aria-label="Abrir curso en Classroom en una pestaña nueva">Abrir en Classroom</a></div>}
          </div>
          <div style={{marginTop:8}}>
            <button type="button" className="button" onClick={async () => {
              try {
                setStatus('Guardando asociación…');
                await confirmClassroomMapping(id, resolvedCourse.googleCourseId!);
                setStatus('Asociación guardada.');
                setResolvedCourse(null);
                applyPlanillaData(await getPlanilla(id));
              } catch (e) {
                setStatus(e instanceof ApiError ? e.message : 'No se pudo guardar la asociación.');
              }
            }}>Volver a vincular este curso</button>
            <button type="button" className="button secondary" onClick={() => setResolvedCourse(null)} style={{marginLeft:8}}>Ignorar</button>
          </div>
        </div>
      )}
      <section className="planilla-info-bar" aria-label="Escala y opciones de la planilla">
        <div className="planilla-scale-summary"><span>TP <strong>{data.planilla.totalPossiblePoints} pts</strong></span><span>Exigencia <strong>{data.planilla.exigenciaPorcentaje}%</strong></span></div>
        {Object.keys(gr).length > 0 && <div className="grade-ranges-bar" aria-label="Escala de notas">
          {GRADE_KEYS_DESC.map((key) => gr[key] && (
            <GradeChip key={key} grade={Number(key)} title={`Desde ${gr[key].minInclusive} hasta ${gr[key].maxInclusive}`} label={<><strong>{key}</strong>{gr[key].minInclusive}-{gr[key].maxInclusive}</>} />
          ))}
          {onePointCeiling !== null && (
            <GradeChip grade={1} title={`${onePointCeiling} puntos o menos`} label={<><strong>1</strong>{onePointCeiling} o menos</>} />
          )}
        </div>}
        <label className="planilla-freeze-toggle">
          <input type="checkbox" checked={freezeStudents} onChange={(event) => toggleFreezeStudents(event.target.checked)} />
          Inmovilizar alumnos
        </label>
      </section>
      {/* toasts shown globally via ToastProvider */}
      <section className="planilla-student-tools" role="search" aria-label="Buscar alumnos en la planilla">
        <label className="planilla-student-search">
          Buscar alumno
          <input type="search" value={studentSearch} onChange={(event) => setStudentSearch(event.target.value)} placeholder="Nombre del alumno…" autoComplete="off" spellCheck={false} />
        </label>
        <div className="planilla-student-tool-actions">
          <span className="planilla-student-count" aria-live="polite">
            {visibleRows.length === computedRows.length
              ? `${computedRows.length} ${computedRows.length === 1 ? 'alumno' : 'alumnos'}`
              : `${visibleRows.length} de ${computedRows.length} alumnos`}
          </span>
        </div>
      </section>
      <section className="planilla-table-panel" aria-labelledby="planilla-table-title">
        <header className="planilla-table-heading">
          <div>
            <span>Tareas</span>
            <h2 id="planilla-table-title">{data.planilla.materiaNombre}</h2>
            {data.tareas.length > 0 && (
              <div className="planilla-origin-summary" aria-label="Origen de las tareas">
                {classroomTaskCount > 0 && <ClassroomBadge label={`${classroomTaskCount} de Classroom`} />}
                {localTaskCount > 0 && <span className="origin-badge">{localTaskCount} {localTaskCount === 1 ? 'local' : 'locales'}</span>}
              </div>
            )}
          </div>
          <small>{data.tareas.length} {data.tareas.length === 1 ? 'tarea' : 'tareas'} en esta etapa</small>
        </header>
      <div className={`table-wrap planilla-grade-table-wrap${freezeStudents ? ' freeze-students' : ''}`}>
        <table className="grade-table planilla-grade-table">
          <caption className="visually-hidden">Calificaciones de {data.planilla.materiaNombre}</caption>
          <thead>
            <tr>
              <th className="planilla-number-heading">#</th>
              <th className="planilla-student-heading">Alumno</th>
              {data.tareas.map((task) => {
                const isClassroomTask = Boolean(task.googleCourseworkId?.trim());
                return (
                  <th key={task.id} className={`planilla-task-heading${isClassroomTask ? ' planilla-task-heading--classroom' : ''}`}>
                    {isClassroomTask ? (
                      task.googleCourseworkUrl ? (
                        <a className="planilla-task-link" href={task.googleCourseworkUrl} target="_blank" rel="noopener noreferrer" aria-label={`${task.titulo}, abrir en Classroom en una pestaña nueva`}>{task.titulo}</a>
                      ) : (
                        <span className="planilla-task-link readonly">{task.titulo}</span>
                      )
                    ) : (
                      <Link className="planilla-task-link" to={`/planilla/${id}/tarea/${task.id}`}>{task.titulo}</Link>
                    )}
                    <small className="planilla-task-tp">
                      TP: {task.total}{task.fechaInicio ? ` · ${formatShortDate(task.fechaInicio)}` : ''}
                      {isClassroomTask && <ClassroomBadge className="planilla-task-classroom-icon" label="Importada de Google Classroom" iconOnly />}
                    </small>
                    {!isClassroomTask && <Link className="planilla-task-edit" to={`/planilla/${id}/tarea/${task.id}`}>Editar</Link>}
                  </th>
                );
              })}
              {(['total', 'percentage', 'grade'] as const).map((key) => {
                const label = key === 'total' ? 'Total' : key === 'percentage' ? '%' : 'Nota';
                const activeDirection = studentSort?.key === key ? studentSort.direction : undefined;
                return <th key={key} className="sortable-grade-heading" aria-sort={activeDirection ?? 'none'}>
                  <button type="button" onClick={() => changeStudentSort(key)} aria-label={`Ordenar por ${label}${activeDirection ? `, actualmente ${activeDirection === 'ascending' ? 'ascendente' : 'descendente'}` : ''}`}>
                    <span>{label}</span><i aria-hidden="true" />
                  </button>
                </th>;
              })}
            </tr>
          </thead>
          <tbody>
            {visibleRows.map(({ row, originalIndex, total, percentage }) => <tr key={row.alumnoId}>
              <td className="planilla-row-number">{originalIndex + 1}</td>
              <th className="planilla-student-name" scope="row">{row.alumnoNombre}</th>
              {data.tareas.map((task) => {
                const grade = values[`${row.alumnoId}:${task.id}`];
                const isClassroomTask = Boolean(task.googleCourseworkId?.trim());
                return <td key={task.id} className={`planilla-task-grade${isClassroomTask ? ' planilla-task-grade--classroom' : ''}`} aria-label={`${row.alumnoNombre}, ${task.titulo}: ${grade === '' || grade == null ? 'sin calificación' : `${grade} puntos`}`}>
                  {isClassroomTask ? (
                    <span>{grade === '' || grade == null ? '—' : grade}</span>
                  ) : (
                    <input
                      type="number"
                      min={0}
                      max={task.total}
                      value={grade ?? ''}
                      onChange={(e) => handleGradeChange(`${row.alumnoId}:${task.id}`, e.target.value)}
                      onBlur={() => void handleGradeBlur(row.alumnoId, task.id, task.total)}
                      aria-label={`Nota para ${row.alumnoNombre} en ${task.titulo}`}
                    />
                  )}
                </td>;
              })}
              <td className="student-total-cell">{total}<small>de {data.planilla.totalPossiblePoints}</small></td>
              <td className="student-percentage-cell">{percentage}%</td>
              <td><GradeChip grade={row.nota} className="student-grade-pill" /></td>
            </tr>)}
            {visibleRows.length === 0 && <tr><td className="planilla-student-empty" colSpan={data.tareas.length + 5}>No se encontraron alumnos con ese nombre.</td></tr>}
          </tbody>
        </table>
      </div>
      </section>
      </div>
    </AppShell>
  );
}
