import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { ApiError } from '../../api/client';
import AnimatedSelect from '../../components/AnimatedSelect';
import ContentState from '../../components/ui/ContentState';
import { useToast } from '../../context/toast';
import { getMisAsignaciones, type AsignacionCompleta } from '../../api/planCurricular';
import { createQuejaSobreCurso, getMisQuejas, quejaEstado, type QuejaItem } from '../../api/quejas';
import { formatSqlDateTime } from '../../utils/date';

function unique<T>(items: T[], key: (item: T) => string | number): T[] {
  return Array.from(new Map(items.map((item) => [key(item), item])).values());
}
function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback;
}

const ESTADO_LABEL: Record<ReturnType<typeof quejaEstado>, string> = {
  pendiente: 'Pendiente', aceptada: 'Aceptada', revisada: 'Revisada', resuelta: 'Resuelta', rechazada: 'Rechazada',
};

export default function QuejaCursoView() {
  const { showToast } = useToast();
  const [asignaciones, setAsignaciones] = useState<AsignacionCompleta[] | null>(null);
  const [errorAsignaciones, setErrorAsignaciones] = useState('');
  const [especialidadId, setEspecialidadId] = useState('');
  const [curso, setCurso] = useState('');
  const [seccion, setSeccion] = useState('');
  const [motivo, setMotivo] = useState('');
  const [saving, setSaving] = useState(false);

  const [lista, setLista] = useState<QuejaItem[] | null>(null);
  const [listLoading, setListLoading] = useState(true);
  const [listError, setListError] = useState('');

  useEffect(() => {
    let active = true;
    getMisAsignaciones()
      .then((list) => { if (active) setAsignaciones(list); })
      .catch((err) => { if (active) setErrorAsignaciones(errorMessage(err, 'No se pudieron cargar tus asignaciones.')); });
    return () => { active = false; };
  }, []);

  const loadList = useCallback(async () => {
    setListLoading(true); setListError('');
    try {
      setLista(await getMisQuejas());
    } catch (err) {
      setListError(errorMessage(err, 'No se pudo cargar tu historial de quejas.'));
    } finally {
      setListLoading(false);
    }
  }, []);
  useEffect(() => { void loadList(); }, [loadList]);

  const listaAsignaciones = asignaciones ?? [];
  const especialidades = unique(listaAsignaciones, (item) => item.especialidadId);
  const especialidadEfectiva = especialidades.length === 1 ? String(especialidades[0].especialidadId) : especialidadId;
  const deLaEspecialidad = listaAsignaciones.filter((item) => String(item.especialidadId) === especialidadEfectiva);
  // Se dedupe por curso (no por materia): la queja es sobre el grupo, no sobre una asignación puntual.
  const cursos = useMemo(() => unique(deLaEspecialidad, (item) => item.cursoOrdinal), [deLaEspecialidad]);
  const secciones = curso ? unique(deLaEspecialidad.filter((item) => item.cursoOrdinal === curso), (item) => item.seccion) : [];
  const cursoSeleccionado = deLaEspecialidad.find((item) => item.cursoOrdinal === curso && item.seccion === seccion);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!cursoSeleccionado || cursoSeleccionado.cursoBaseId == null || !motivo.trim() || saving) return;
    setSaving(true);
    try {
      await createQuejaSobreCurso({ cursoId: cursoSeleccionado.cursoBaseId, motivo: motivo.trim() });
      setMotivo(''); setCurso(''); setSeccion('');
      showToast('Queja registrada.', { tone: 'success' });
      await loadList();
    } catch (err) {
      showToast(errorMessage(err, 'No se pudo registrar la queja.'), { tone: 'error' });
    } finally {
      setSaving(false);
    }
  }

  return <div className="rsa-view">
    <header className="rsa-intro">
      <div><h3>Registrá una queja sobre un curso</h3><p>Para situaciones del grupo que Coordinación Pedagógica deba revisar — no es para calificar ni registrar conducta individual, eso se hace desde la clase.</p></div>
    </header>
    <section className="class-card rsa-selection" aria-labelledby="queja-curso-selection-title">
      <h4 id="queja-curso-selection-title">1. Elegí el curso</h4>
      {errorAsignaciones ? <div className="notice error">{errorAsignaciones}</div>
        : asignaciones === null ? <p>Cargando asignaciones…</p>
        : listaAsignaciones.length === 0 ? <p>No tenés asignaciones disponibles.</p>
        : <div className="rsa-selector-grid">
          {especialidades.length > 1 && <div className="class-field"><label>Especialidad</label><AnimatedSelect ariaLabel="Especialidad" value={especialidadId} disabled={saving} onChange={(value) => { setEspecialidadId(value); setCurso(''); setSeccion(''); }} options={[{ value: '', label: 'Elegí una especialidad' }, ...especialidades.map((item) => ({ value: item.especialidadId, label: item.especialidadNombre }))]} /></div>}
          <div className="class-field"><label>Curso</label><AnimatedSelect ariaLabel="Curso" value={curso} disabled={!especialidadEfectiva || saving} onChange={(value) => { setCurso(value); setSeccion(''); }} options={[{ value: '', label: 'Elegí un curso' }, ...cursos.map((item) => ({ value: item.cursoOrdinal, label: item.cursoOrdinal }))]} /></div>
          <div className="class-field"><label>Sección</label><AnimatedSelect ariaLabel="Sección" value={seccion} disabled={!curso || saving} onChange={setSeccion} options={[{ value: '', label: 'Elegí una sección' }, ...secciones.map((item) => ({ value: item.seccion, label: item.seccion }))]} /></div>
        </div>}
    </section>

    {cursoSeleccionado && <section className="class-card rsa-config" aria-labelledby="queja-curso-form-title">
      <h4 id="queja-curso-form-title">2. Describí la situación</h4>
      <p className="rsa-context">{cursoSeleccionado.especialidadNombre} · {curso} {seccion}</p>
      <form className="form-grid" onSubmit={submit}>
        <label className="complaints-full">Motivo
          <textarea rows={4} required disabled={saving} value={motivo} onChange={(e) => setMotivo(e.target.value)} placeholder="Describí qué ocurrió y agregá los detalles necesarios para su revisión." />
        </label>
        <div className="rsa-actions">
          <button type="submit" className="button" disabled={saving || !motivo.trim() || cursoSeleccionado.cursoBaseId == null}>{saving ? 'Registrando…' : 'Registrar queja'}</button>
        </div>
      </form>
    </section>}

    <section className="panel complaints-history" aria-busy={listLoading}>
      <header className="complaints-heading">
        <div><span className="eyebrow">Historial</span><h2>Tus quejas registradas{lista !== null && <span className="complaints-count">{lista.length}</span>}</h2></div>
      </header>
      {listError && <ContentState compact tone="error" title="No se pudo cargar tu historial" detail={listError} />}
      {lista === null && listLoading && <ContentState compact tone="loading" title="Cargando…" />}
      {lista !== null && lista.length === 0 && <ContentState compact title="Todavía no registraste quejas" tone="empty" />}
      {lista !== null && lista.length > 0 && <ul className="complaints-list">{lista.map((q) => {
        const estado = quejaEstado(q);
        return <li key={q.id}>
          <article className="complaint-record">
            <div className="complaint-record-heading"><h4>{[q.cursoEspecialidad, q.cursoNivel ? q.cursoNivel + '°' : null, q.cursoSeccion].filter(Boolean).join(' · ') || ('Curso #' + q.cursoId)}</h4><span className="complaint-reference">Registro #{q.id}</span></div>
            <span className={`complaint-status ${estado}`}>{ESTADO_LABEL[estado]}</span>
            <p className="complaint-reason">{q.motivo}</p>
            <footer>{formatSqlDateTime(q.creadaEn)}</footer>
            {estado === 'rechazada' && <div className="complaint-resolution"><strong>Rechazada por Coordinación Pedagógica</strong><p>{q.motivoRechazo || 'Sin motivo registrado.'}</p></div>}
            {(estado === 'revisada' || estado === 'resuelta') && <div className="complaint-resolution"><strong>Conclusión de la revisión</strong><p>{q.conclusion || 'Sin conclusión registrada.'}</p></div>}
            {estado === 'resuelta' && <div className="complaint-resolution"><strong>Solución aplicada</strong><p>{q.solucionAplicada}</p></div>}
          </article>
        </li>;
      })}</ul>}
    </section>
  </div>;
}
