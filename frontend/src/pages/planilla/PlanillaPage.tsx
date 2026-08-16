import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import AppShell from '../../components/AppShell';
import { getPlanilla, resolvePlanilla, syncClassroom, confirmClassroomMapping, type PlanillaDetail } from '../../api/academics';
import { ApiError, apiDownload } from '../../api/client';

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

export default function PlanillaPage() {
  const id = Number(useParams().planillaId);
  const navigate = useNavigate();
  const [data, setData] = useState<PlanillaDetail | null>(null);
  const [values, setValues] = useState<Record<string, string>>({});
  const [status, setStatus] = useState('');
  const [resolvedCourse, setResolvedCourse] = useState<{ googleCourseId?: string | null; classroomCourseMapped?: boolean; message?: string } | null>(null);
  const [switchingEtapa, setSwitchingEtapa] = useState(false);
  const [studentSearch, setStudentSearch] = useState('');
  const [studentSort, setStudentSort] = useState<{ key: StudentSortKey; direction: SortDirection } | null>(null);

  useEffect(() => {
    if (!Number.isInteger(id)) return;
    getPlanilla(id).then((result) => {
      setData(result);
      const initial: Record<string, string> = {};
      result.rows.forEach((row) => row.grades.forEach((g) => { initial[`${row.alumnoId}:${g.tareaId}`] = g.puntos == null ? '' : String(g.puntos); }));
      setValues(initial);
    }).catch((e) => setStatus(e instanceof ApiError ? e.message : 'No se pudo cargar la planilla.'));
  }, [id]);

  // Auto-sync once when planilla data is loaded and Classroom is available
  const syncRanRef = useRef(false);
  useEffect(() => {
    if (!data || syncRanRef.current) return;
    syncRanRef.current = true;
    if (!data.planilla) return;
    (async () => {
      try {
        setStatus('Sincronizando Classroom…');
        const res = await syncClassroom(id);
        setStatus('Sincronización completada.');
        setResolvedCourse({ googleCourseId: res.googleCourseId, classroomCourseMapped: res.classroomCourseMapped, message: res.message });
        setData(await getPlanilla(id));
      } catch (e) {
        setStatus(e instanceof ApiError ? e.message : 'No se pudo sincronizar Classroom.');
      }
    })();
  }, [data, id]);

  const computedRows = useMemo(() => {
    if (!data) return [];
    return data.rows.map((row, originalIndex) => {
      const total = data.tareas.reduce((sum, task) => sum + Number(values[`${row.alumnoId}:${task.id}`] || 0), 0);
      const percentage = data.planilla.totalPossiblePoints ? Math.round(total * 100 / data.planilla.totalPossiblePoints) : 0;
      return { row, originalIndex, total, percentage };
    });
  }, [data, values]);

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

  // manual sync removed; synchronization runs automatically on load

  // Igual que el <select id="etapaSelect"> del JSP legacy: cambiar de etapa
  // resuelve (o crea) la planilla de esa etapa para el mismo curso/materia
  // y navega a su id. resolvePlanilla ya existe en la API (usado también
  // desde HomePage al abrir una materia por primera vez).
  async function changeEtapa(nuevaEtapa: number) {
    if (!data || nuevaEtapa === data.planilla.etapaIndex) return;
    setSwitchingEtapa(true); setStatus('');
    try {
      const result = await resolvePlanilla(data.planilla.cursoId, data.planilla.materiaId, nuevaEtapa);
      navigate(`/planilla/${result.planillaId}`, { replace: true });
    } catch (e) {
      setStatus(e instanceof ApiError ? e.message : 'No se pudo cambiar de etapa.');
      setSwitchingEtapa(false);
    }
  }

  function changeStudentSort(key: StudentSortKey) {
    setStudentSort((current) => {
      if (!current || current.key !== key) return { key, direction: 'ascending' };
      if (current.direction === 'ascending') return { key, direction: 'descending' };
      return null;
    });
  }

  if (!data) return <AppShell title="Planilla"><div className="panel">{status || 'Cargando…'}</div></AppShell>;

  const gr = data.gradeRanges;
  // Piso de "1": todo lo que caiga por debajo del mínimo de "2" (igual que
  // en Planilla.jsp: "${gradeRanges['2'][0] - 1} o menos").
  const onePointCeiling = gr['2'] ? gr['2'].minInclusive - 1 : null;

  return (
    <AppShell title={data.planilla.materiaNombre} subtitle={data.curso ? `${data.curso.nivel}° ${data.curso.seccion} · ${data.planilla.etapa}` : data.planilla.etapa} specialty={data.curso?.especialidad}>
      <div className="toolbar">
        <Link className="button secondary" to="/home">← Volver</Link>
        <label className="inline-filter">
          Etapa
          <select value={data.planilla.etapaIndex} disabled={switchingEtapa} onChange={(e) => changeEtapa(Number(e.target.value))}>
            <option value={1}>Primera etapa</option>
            <option value={2}>Segunda etapa</option>
          </select>
        </label>
        <Link className="button" to={`/planilla/${id}/tarea`}>Agregar tarea</Link>
        {/* Habilitamos la descarga individual usando fetch+blob para incluir Authorization */}
        <button className="button" onClick={async () => {
          try {
            await apiDownload(`/api/planillas/${id}/export`, `planilla-${id}.xlsx`);
          } catch (e) {
            setStatus(e instanceof ApiError ? e.message : 'Error en la descarga');
          }
        }}>Descargar</button>
      </div>
      {/* Mensaje informativo removido por solicitud de UX */}
      <section className="summary-grid">
        <article className="metric"><span>Curso</span><strong>{data.curso ? `${data.curso.nivel}° ${data.curso.seccion}` : '—'}</strong></article>
        <article className="metric"><span>Etapa</span><strong>{data.planilla.etapa}</strong></article>
        <article className="metric"><span>Total</span><strong>{data.planilla.totalPossiblePoints} pts</strong></article>
        <article className="metric"><span>Exigencia</span><strong>{data.planilla.exigenciaPorcentaje}%</strong></article>
      </section>
      {resolvedCourse && resolvedCourse.classroomCourseMapped && resolvedCourse.googleCourseId && data.planilla.googleCourseId !== resolvedCourse.googleCourseId && (
        <div className="notice">
          <div>Se encontró un curso de Classroom posiblemente correspondiente: <strong>{resolvedCourse.googleCourseId}</strong></div>
          <div style={{marginTop:8}}>
            <button className="button" onClick={async () => {
              try {
                setStatus('Guardando asociación…');
                await confirmClassroomMapping(id, resolvedCourse.googleCourseId!);
                setStatus('Asociación guardada.');
                setResolvedCourse(null);
                setData(await getPlanilla(id));
              } catch (e) {
                setStatus(e instanceof ApiError ? e.message : 'No se pudo guardar la asociación.');
              }
            }}>Confirmar curso Classroom</button>
            <button className="button secondary" onClick={() => setResolvedCourse(null)} style={{marginLeft:8}}>Ignorar</button>
          </div>
        </div>
      )}
      {Object.keys(gr).length > 0 && (
        <section className="grade-ranges-bar" aria-label="Escala de notas">
          {GRADE_KEYS_DESC.map((key) => gr[key] && (
            <span key={key} className={`grade-chip grade-chip--${key}`} title={`Desde ${gr[key].minInclusive} hasta ${gr[key].maxInclusive}`}>
              <strong>{key}</strong>{gr[key].minInclusive}-{gr[key].maxInclusive}
            </span>
          ))}
          {onePointCeiling !== null && (
            <span className="grade-chip grade-chip--1" title={`${onePointCeiling} puntos o menos`}>
              <strong>1</strong>{onePointCeiling} o menos
            </span>
          )}
        </section>
      )}
      {status && <div className="notice">{status}</div>}
      <section className="planilla-student-tools" role="search" aria-label="Buscar alumnos en la planilla">
        <label className="planilla-student-search">
          Buscar alumno
          <input type="search" value={studentSearch} onChange={(event) => setStudentSearch(event.target.value)} placeholder="Nombre del alumno…" autoComplete="off" spellCheck={false} />
        </label>
        <span className="planilla-student-count" aria-live="polite">
          {visibleRows.length === computedRows.length
            ? `${computedRows.length} ${computedRows.length === 1 ? 'alumno' : 'alumnos'}`
            : `${visibleRows.length} de ${computedRows.length} alumnos`}
        </span>
      </section>
      <div className="table-wrap planilla-grade-table-wrap">
        <table className="grade-table planilla-grade-table">
          <thead>
            <tr>
              <th>Alumno</th>
              {data.tareas.map((t) => (
                <th key={t.id}>
                  {/* Igual que el JSP legacy: si la tarea viene de Google
                      Classroom, el título linkea directo al coursework en
                      Classroom (nueva pestaña); si no, a la edición interna. */}
                  {t.googleCourseworkUrl ? (
                    <a href={t.googleCourseworkUrl} target="_blank" rel="noopener noreferrer">{t.titulo}</a>
                  ) : (
                    <Link to={`/planilla/${id}/tarea/${t.id}`}>{t.titulo}</Link>
                  )}
                  <small>{t.total} pts{t.googleCourseworkUrl && ' · '}{t.googleCourseworkUrl && <Link to={`/planilla/${id}/tarea/${t.id}`}>editar</Link>}</small>
                </th>
              ))}
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
            {visibleRows.map(({ row, total, percentage }) => <tr key={row.alumnoId}>
              <th>{row.alumnoNombre}</th>
              {data.tareas.map((t) => <td key={t.id}><input aria-label={`${row.alumnoNombre}, ${t.titulo}`} type="number" min="0" max={t.total} value={values[`${row.alumnoId}:${t.id}`] ?? ''} onChange={(e) => setValues((v) => ({ ...v, [`${row.alumnoId}:${t.id}`]: e.target.value }))} disabled /></td>)}
              <td className="student-total-cell">{total}</td>
              <td className="student-percentage-cell">{percentage}%</td>
              <td><span className={`grade-chip grade-chip--${Math.min(5, Math.max(1, row.nota))} student-grade-pill`} aria-label={`Nota ${row.nota}`}>{row.nota}</span></td>
            </tr>)}
            {visibleRows.length === 0 && <tr><td className="planilla-student-empty" colSpan={data.tareas.length + 4}>No se encontraron alumnos con ese nombre.</td></tr>}
          </tbody>
        </table>
      </div>
    </AppShell>
  );
}
