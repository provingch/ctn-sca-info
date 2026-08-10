import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import AppShell from '../../components/AppShell';
import { getPlanilla, saveGrades, syncClassroom, type PlanillaDetail } from '../../api/academics';
import { ApiError } from '../../api/client';

export default function PlanillaPage() {
  const id = Number(useParams().planillaId);
  const [data, setData] = useState<PlanillaDetail | null>(null);
  const [values, setValues] = useState<Record<string, string>>({});
  const [status, setStatus] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!Number.isInteger(id)) return;
    getPlanilla(id).then((result) => {
      setData(result);
      const initial: Record<string, string> = {};
      result.rows.forEach((row) => row.grades.forEach((g) => { initial[`${row.alumnoId}:${g.tareaId}`] = g.puntos == null ? '' : String(g.puntos); }));
      setValues(initial);
    }).catch((e) => setStatus(e instanceof ApiError ? e.message : 'No se pudo cargar la planilla.'));
  }, [id]);

  const totals = useMemo(() => data?.rows.map((row) => data.tareas.reduce((sum, t) => sum + Number(values[`${row.alumnoId}:${t.id}`] || 0), 0)) ?? [], [data, values]);

  async function save() {
    if (!data) return;
    setBusy(true); setStatus('');
    try {
      const grades = data.rows.map((row) => ({ alumnoId: row.alumnoId, items: data.tareas.map((t) => ({ tareaId: t.id, puntos: values[`${row.alumnoId}:${t.id}`] === '' ? null : Number(values[`${row.alumnoId}:${t.id}`]) })) }));
      const result = await saveGrades(id, grades);
      setStatus([result.message, ...result.warnings].join(' '));
    } catch (e) { setStatus(e instanceof ApiError ? e.message : 'No se pudieron guardar las notas.'); }
    finally { setBusy(false); }
  }

  async function sync() {
    setBusy(true); setStatus('');
    try { setStatus((await syncClassroom(id)).message); setData(await getPlanilla(id)); }
    catch (e) { setStatus(e instanceof ApiError ? e.message : 'No se pudo sincronizar Classroom.'); }
    finally { setBusy(false); }
  }

  if (!data) return <AppShell title="Planilla"><div className="panel">{status || 'Cargando…'}</div></AppShell>;
  return (
    <AppShell title={data.planilla.materiaNombre} subtitle={data.curso ? `${data.curso.nivel}° ${data.curso.seccion} · ${data.planilla.etapa}` : data.planilla.etapa} specialty={data.curso?.especialidad}>
      <div className="toolbar">
        <Link className="button secondary" to="/home">← Volver</Link>
        <Link className="button" to={`/planilla/${id}/tarea`}>Agregar tarea</Link>
        <button className="button secondary" onClick={sync} disabled={busy}>Sincronizar Classroom</button>
        <button className="button" onClick={save} disabled={busy}>{busy ? 'Procesando…' : 'Guardar notas'}</button>
      </div>
      <section className="summary-grid">
        <article className="metric"><span>Curso</span><strong>{data.curso ? `${data.curso.nivel}° ${data.curso.seccion}` : '—'}</strong></article>
        <article className="metric"><span>Etapa</span><strong>{data.planilla.etapa}</strong></article>
        <article className="metric"><span>Total</span><strong>{data.planilla.totalPossiblePoints} pts</strong></article>
        <article className="metric"><span>Exigencia</span><strong>{data.planilla.exigenciaPorcentaje}%</strong></article>
      </section>
      {status && <div className="notice">{status}</div>}
      <div className="table-wrap">
        <table className="grade-table">
          <thead><tr><th>Alumno</th>{data.tareas.map((t) => <th key={t.id}><span>{t.titulo}</span><small>{t.total} pts · <Link to={`/planilla/${id}/tarea/${t.id}`}>editar</Link></small></th>)}<th>Total</th><th>%</th></tr></thead>
          <tbody>{data.rows.map((row, rowIndex) => {
            const percent = data.planilla.totalPossiblePoints ? Math.round(totals[rowIndex] * 100 / data.planilla.totalPossiblePoints) : 0;
            return <tr key={row.alumnoId}><th>{row.alumnoNombre}</th>{data.tareas.map((t) => <td key={t.id}><input aria-label={`${row.alumnoNombre}, ${t.titulo}`} type="number" min="0" max={t.total} value={values[`${row.alumnoId}:${t.id}`] ?? ''} onChange={(e) => setValues((v) => ({ ...v, [`${row.alumnoId}:${t.id}`]: e.target.value }))} /></td>)}<td>{totals[rowIndex]}</td><td>{percent}%</td></tr>;
          })}</tbody>
        </table>
      </div>
    </AppShell>
  );
}
