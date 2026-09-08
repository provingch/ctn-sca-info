import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import AnimatedSelect from '../../components/AnimatedSelect';
import ContentState from '../../components/ui/ContentState';
import { ApiError } from '../../api/client';
import type { AdminCatalog } from '../../api/admin';
import { createQueja, getAdminQuejas, type QuejaItem } from '../../api/quejas';
import { formatSqlDateTime } from '../../utils/date';

const normalize = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();

export default function AdminQuejasPanel({ data, status, isGlobalAdmin }: { data: AdminCatalog; status: (s: string) => void; isGlobalAdmin: boolean }) {
  const [cursoId, setCursoId] = useState<number | ''>('');
  const [profesorId, setProfesorId] = useState<number | ''>('');
  const [motivo, setMotivo] = useState('');
  const [lista, setLista] = useState<QuejaItem[] | null>(null);
  const [saving, setSaving] = useState(false);
  const [listLoading, setListLoading] = useState(true);
  const [listError, setListError] = useState('');
  const [formError, setFormError] = useState('');
  const [query, setQuery] = useState('');
  const requestId = useRef(0);
  const submitting = useRef(false);

  const usuariosPorId = useMemo(() => new Map(data.usuarios.map((u) => [u.id, (u.nombre + ' ' + u.apellido).trim()])), [data.usuarios]);
  const profesoresForCurso = useMemo(() => Array.from(new Map(data.asignaciones
    .filter((a) => a.cursoId === cursoId)
    .map((a) => [a.profesorId, a.profesor])).entries())
    .map(([id, nombre]) => ({ id, nombre }))
    .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es')), [cursoId, data.asignaciones]);
  const validSelection = data.cursos.some((c) => c.id === cursoId) && profesoresForCurso.some((p) => p.id === profesorId);

  const loadList = useCallback(async () => {
    const request = ++requestId.current;
    setListLoading(true);
    setListError('');
    try {
      const items = await getAdminQuejas();
      if (request === requestId.current) setLista(items);
    } catch (err) {
      if (request === requestId.current) setListError(err instanceof ApiError ? err.message : 'No se pudo cargar la lista de quejas.');
    } finally {
      if (request === requestId.current) setListLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadList();
    return () => { requestId.current += 1; };
  }, [loadList]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (submitting.current) return;
    setFormError('');
    if (!validSelection || !motivo.trim()) {
      setFormError('Elegí un curso, un profesor asignado y describí el motivo.');
      return;
    }
    submitting.current = true;
    setSaving(true);
    try {
      await createQueja({ cursoId: Number(cursoId), profesorId: Number(profesorId), motivo: motivo.trim() });
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudo registrar la queja. Los datos se conservaron para reintentar.');
      submitting.current = false;
      setSaving(false);
      return;
    }
    setMotivo('');
    setProfesorId('');
    setCursoId('');
    setQuery('');
    status('Queja registrada.');
    await loadList();
    submitting.current = false;
    setSaving(false);
  }

  const filtered = (lista ?? []).filter((q) => normalize([
    q.profesorNombre, q.profesorApellido, q.cursoEspecialidad, q.cursoNivel, q.cursoSeccion, q.motivo,
  ].join(' ')).includes(normalize(query.trim())));

  return <div className="complaints-workspace">
    <section className="panel complaints-register" aria-labelledby="complaints-register-title">
      <header className="complaints-heading">
        <div><span className="eyebrow">Nuevo registro</span><h2 id="complaints-register-title">Registrar queja</h2>
          <p>Seleccioná el curso y el profesor involucrado. La especialidad se determina por el curso.</p></div>
      </header>
      <form className="form-grid complaints-form" onSubmit={submit} aria-busy={saving}>
        <div className="complaints-field">
          <span className="form-label">Curso</span>
          <AnimatedSelect ariaLabel="Curso" value={cursoId} disabled={saving || data.cursos.length === 0} placeholder="Seleccioná un curso" onChange={(v) => { setCursoId(Number(v) || ''); setProfesorId(''); setFormError(''); }} options={data.cursos.map((c) => ({ value: c.id, label: c.especialidad + ' · ' + c.nivel + '° ' + c.seccion }))} />
          {data.cursos.length === 0 && <small>No hay cursos disponibles en tu alcance.</small>}
        </div>
        <div className="complaints-field">
          <span className="form-label">Profesor</span>
          <AnimatedSelect ariaLabel="Profesor" value={profesorId} onChange={(v) => setProfesorId(Number(v) || '')} disabled={saving || !cursoId || profesoresForCurso.length === 0} placeholder={cursoId ? 'Seleccioná un profesor' : 'Primero elegí un curso'} options={profesoresForCurso.map((p) => ({ value: p.id, label: p.nombre }))} />
          {!!cursoId && profesoresForCurso.length === 0 && <small>No hay profesores asignados a este curso.</small>}
        </div>
        <label className="complaints-full">Motivo
          <textarea placeholder="Describí qué ocurrió y agregá los detalles necesarios para su revisión." value={motivo} onChange={(e) => setMotivo(e.target.value)} disabled={saving} rows={4} required />
        </label>
        {formError && <p className="notice error complaints-full" role="alert">{formError}</p>}
        <div className="complaints-form-footer complaints-full">
          <small>Completá los tres campos para registrar la queja.</small>
          <button className="button" type="submit" disabled={saving || !validSelection || !motivo.trim()}>{saving ? 'Registrando…' : 'Registrar queja'}</button>
        </div>
      </form>
    </section>

    <section className="panel complaints-history" aria-labelledby="complaints-history-title" aria-busy={listLoading}>
      <header className="complaints-heading">
        <div><span className="eyebrow">Historial</span><h2 id="complaints-history-title">Quejas registradas{lista !== null && <span className="complaints-count">{lista.length}</span>}</h2>
          <p>{isGlobalAdmin ? 'Registros de todas las especialidades, del más reciente al más antiguo.' : 'Registros de tu especialidad, del más reciente al más antiguo.'}</p></div>
        <button className="button secondary" type="button" disabled={listLoading || saving} onClick={() => void loadList()}>{listLoading ? 'Actualizando…' : 'Actualizar lista'}</button>
      </header>
      {lista !== null && lista.length > 0 && <div className="complaints-search form-grid">
        <label>Buscar en las quejas<input type="search" placeholder="Profesor, curso, especialidad o motivo" value={query} onChange={(e) => setQuery(e.target.value)} /></label>
        <small>{filtered.length} de {lista.length} registros</small>
      </div>}
      {listError && <ContentState compact tone="error" title="No se pudo actualizar el historial" detail={listError + (lista !== null ? ' Se conserva la última lista cargada.' : '')} />}
      {lista === null && listLoading && <ContentState compact tone="loading" title="Cargando quejas…" />}
      {lista !== null && lista.length === 0 && <ContentState compact title="Todavía no hay quejas registradas" detail="Los nuevos registros aparecerán aquí automáticamente." />}
      {lista !== null && lista.length > 0 && filtered.length === 0 && <ContentState compact title="Sin coincidencias" detail="Probá con otro nombre, curso o motivo." actions={<button className="button secondary" type="button" onClick={() => setQuery('')}>Limpiar búsqueda</button>} />}
      {filtered.length > 0 && <ul className="complaints-list">{filtered.map((q) => <li key={q.id}>
        <article className="complaint-record">
          <div className="complaint-record-heading"><h3>{[q.profesorNombre, q.profesorApellido].filter(Boolean).join(' ') || ('Profesor #' + q.profesorId)}</h3><span className="complaint-reference">Registro #{q.id}</span></div>
          <p className="complaint-course">{[q.cursoEspecialidad, q.cursoNivel ? q.cursoNivel + '°' : null, q.cursoSeccion ? 'Sección ' + q.cursoSeccion : null].filter(Boolean).join(' · ') || ('Curso #' + q.cursoId)}</p>
          <p className="complaint-reason">{q.motivo}</p>
          <footer>{formatSqlDateTime(q.creadaEn)} · Registrada por {usuariosPorId.get(q.creadaPor) ?? ('Usuario #' + q.creadaPor)}</footer>
        </article>
      </li>)}</ul>}
    </section>
  </div>;
}
