import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import AppShell from '../../components/AppShell';
import { getPlanilla, resolvePlanilla, syncClassroom, type PlanillaDetail } from '../../api/academics';
import { ApiError } from '../../api/client';

// Etiquetas de nota en orden descendente (5 -> 1), igual que el JSP legacy
// (Planilla.jsp: chips grade-chip--five..one). "1" no tiene rango propio en
// gradeRanges (es "todo lo que quede por debajo del piso de 2"), así que se
// arma su etiqueta a partir del minInclusive de "2".
const GRADE_KEYS_DESC = ['5', '4', '3', '2'] as const;

export default function PlanillaPage() {
  const id = Number(useParams().planillaId);
  const navigate = useNavigate();
  const [data, setData] = useState<PlanillaDetail | null>(null);
  const [values, setValues] = useState<Record<string, string>>({});
  const [status, setStatus] = useState('');
  const [switchingEtapa, setSwitchingEtapa] = useState(false);

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
        await syncClassroom(id);
        setData(await getPlanilla(id));
        setStatus('Sincronización completada.');
      } catch (e) {
        setStatus(e instanceof ApiError ? e.message : 'No se pudo sincronizar Classroom.');
      }
    })();
  }, [data, id]);

  const totals = useMemo(() => data?.rows.map((row) => data.tareas.reduce((sum, t) => sum + Number(values[`${row.alumnoId}:${t.id}`] || 0), 0)) ?? [], [data, values]);

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
            <option value={1}>primera etapa</option>
            <option value={2}>segunda etapa</option>
          </select>
        </label>
        <Link className="button" to={`/planilla/${id}/tarea`}>Agregar tarea</Link>
        {/* Habilitamos la descarga individual usando el endpoint backend recién agregado */}
        <a className="button" href={`/api/planillas/${id}/export`}>Descargar</a>
      </div>
      {/* Mensaje informativo removido por solicitud de UX */}
      <section className="summary-grid">
        <article className="metric"><span>Curso</span><strong>{data.curso ? `${data.curso.nivel}° ${data.curso.seccion}` : '—'}</strong></article>
        <article className="metric"><span>Etapa</span><strong>{data.planilla.etapa}</strong></article>
        <article className="metric"><span>Total</span><strong>{data.planilla.totalPossiblePoints} pts</strong></article>
        <article className="metric"><span>Exigencia</span><strong>{data.planilla.exigenciaPorcentaje}%</strong></article>
      </section>
      {Object.keys(gr).length > 0 && (
        <section className="grade-ranges-bar" aria-label="Escala de notas">
          {GRADE_KEYS_DESC.map((key) => gr[key] && (
            <span key={key} className={`grade-chip grade-chip--${key}`} title={`Desde ${gr[key].minInclusive} hasta ${gr[key].maxInclusive}`}>
              <strong>{key}</strong>{gr[key].minInclusive}-{gr[key].maxInclusive}
            </span>
          ))}
          {onePointCeiling !== null && (
            <span className="grade-chip grade-chip--one" title={`${onePointCeiling} puntos o menos`}>
              <strong>1</strong>{onePointCeiling} o menos
            </span>
          )}
        </section>
      )}
      {status && <div className="notice">{status}</div>}
      <div className="table-wrap">
        <table className="grade-table">
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
              <th>Total</th>
              <th>%</th>
              <th>Nota</th>
            </tr>
          </thead>
          <tbody>{data.rows.map((row, rowIndex) => {
            const percent = data.planilla.totalPossiblePoints ? Math.round(totals[rowIndex] * 100 / data.planilla.totalPossiblePoints) : 0;
            return <tr key={row.alumnoId}><th>{row.alumnoNombre}</th>{data.tareas.map((t) => <td key={t.id}><input aria-label={`${row.alumnoNombre}, ${t.titulo}`} type="number" min="0" max={t.total} value={values[`${row.alumnoId}:${t.id}`] ?? ''} onChange={(e) => setValues((v) => ({ ...v, [`${row.alumnoId}:${t.id}`]: e.target.value }))} disabled /></td>)}<td>{totals[rowIndex]}</td><td>{percent}%</td><td>{row.nota}</td></tr>;
          })}</tbody>
        </table>
      </div>
    </AppShell>
  );
}