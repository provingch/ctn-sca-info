import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { ApiError } from '../../api/client';
import { getMiClase, getMisClases, listarCodigosConducta, updateAttendance, updateClase, type ClaseDadaDto, type ClaseDetalleDto, type CodigoConducta } from '../../api/home';
import ContentState from '../../components/ui/ContentState';
import AnimatedSelect from '../../components/AnimatedSelect';
import useAccessibleDialog from '../../hooks/useAccessibleDialog';
import { useToast } from '../../context/toast';
import ClassAttendanceRow, { type AttendanceState } from './ClassAttendanceRow';
import { classDate, editingAllowed, filterClassHistory, studentName } from './classHistoryUtils';
import './MisClasesView.css';

const PAGE_SIZE = 24;
const errorMessage = (error: unknown, fallback: string) => error instanceof ApiError ? error.message : fallback;

export default function ClassHistoryView() {
  const [params, setParams] = useSearchParams();
  const query = params.get('q') ?? '', curso = params.get('curso') ?? '', specialty = params.get('especialidad') ?? '';
  const order = params.get('orden') === 'asc' ? 'asc' : 'desc';
  const [debouncedQuery, setDebouncedQuery] = useState(query);
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  const [clases, setClases] = useState<ClaseDadaDto[] | null>(null);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState<ClaseDetalleDto | null>(null);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState('');
  const [busy, setBusy] = useState(false);
  const inFlight = useRef(false), detailRequest = useRef(0);
  const [editing, setEditing] = useState(false);
  const [editTema, setEditTema] = useState('');
  const [editStates, setEditStates] = useState<Record<number, AttendanceState>>({});
  const [editCodigos, setEditCodigos] = useState<Record<number, string[]>>({});
  const [catalog, setCatalog] = useState<CodigoConducta[]>([]);
  const [catalogError, setCatalogError] = useState('');
  const { showToast } = useToast();

  useEffect(() => { const timer = window.setTimeout(() => setDebouncedQuery(query), 200); return () => window.clearTimeout(timer); }, [query]);
  useEffect(() => { setVisibleCount(PAGE_SIZE); }, [debouncedQuery, curso, specialty, order]);
  useEffect(() => () => { detailRequest.current++; }, []);
  function changeFilter(key: string, value: string) {
    setParams((current) => { const next = new URLSearchParams(current); if (value) next.set(key, value); else next.delete(key); return next; }, { replace: true });
  }
  function clearFilters() {
    setDebouncedQuery(''); setVisibleCount(PAGE_SIZE);
    setParams((current) => { const next = new URLSearchParams(current); ['q', 'curso', 'especialidad', 'orden'].forEach((key) => next.delete(key)); return next; }, { replace: true });
  }
  const loadCatalog = useCallback(async () => {
    try { setCatalog(await listarCodigosConducta()); setCatalogError(''); }
    catch (error) { setCatalogError(errorMessage(error, 'No se pudo cargar el catálogo de rasgos. Los códigos registrados se conservan.')); }
  }, []);
  useEffect(() => { void loadCatalog(); }, [loadCatalog]);
  const load = useCallback(async () => {
    setError('');
    try { setClases(await getMisClases()); }
    catch (error) { setError(errorMessage(error, 'No se pudieron cargar las clases.')); }
  }, []);
  useEffect(() => { void load(); }, [load]);
  const filtered = useMemo(() => filterClassHistory(clases ?? [], debouncedQuery, curso, specialty, order), [clases, debouncedQuery, curso, specialty, order]);
  const courses = useMemo(() => Array.from(new Map((clases ?? []).map((item) => [String(item.cursoId), item.cursoDescripcion || `Curso #${item.cursoId}`])).entries()).sort((a, b) => a[1].localeCompare(b[1], 'es')), [clases]);
  const specialties = useMemo(() => Array.from(new Map((clases ?? []).map((item) => [String(item.especialidadId ?? 'sin-especialidad'), item.especialidadNombre || 'Sin especialidad'])).entries()).sort((a, b) => a[1].localeCompare(b[1], 'es')), [clases]);
  const hasFilters = !!query || !!curso || !!specialty || order !== 'desc';

  const openDetail = async (id: number) => {
    const request = ++detailRequest.current;
    setSelectedId(id); setSelected(null); setEditing(false); setDetailLoading(true); setDetailError('');
    try { const result = await getMiClase(id); if (request === detailRequest.current) setSelected(result); }
    catch (error) { if (request === detailRequest.current) setDetailError(errorMessage(error, 'No se pudo cargar el detalle de la clase.')); }
    finally { if (request === detailRequest.current) setDetailLoading(false); }
  };
  function closeDetail() {
    if (inFlight.current) return;
    if (editing && !window.confirm('¿Cerrar sin guardar los cambios de esta clase?')) return;
    detailRequest.current++; setSelectedId(null); setSelected(null); setEditing(false); setDetailError('');
  }
  const dialogRef = useAccessibleDialog(selectedId !== null, closeDetail);
  function beginEdit() {
    if (!selected || !editingAllowed(selected.fechaClase)) return;
    setEditTema(selected.tema);
    setEditStates(Object.fromEntries(selected.asistencias.map((item) => [item.id, item.estado as AttendanceState])));
    setEditCodigos(Object.fromEntries(selected.asistencias.map((item) => [item.id, [...(item.codigos ?? [])]])));
    setEditing(true);
  }
  async function saveEdit() {
    if (!selected || !editTema.trim() || inFlight.current) return;
    inFlight.current = true; setBusy(true);
    const id = selected.id;
    try {
      await updateClase(id, {
        tema: editTema.trim(),
        // Do not rewrite unchanged rows or their historical codes.
        asistencias: selected.asistencias.filter((item) => editStates[item.id] !== item.estado || JSON.stringify([...(editCodigos[item.id] ?? [])].sort()) !== JSON.stringify([...(item.codigos ?? [])].sort()))
          .map((item) => ({ asistenciaId: item.id, estado: editStates[item.id], codigos: editCodigos[item.id] ?? [] })),
      });
      showToast('Clase actualizada.', { tone: 'success' }); setEditing(false);
      await openDetail(id); await load();
    } catch (error) { showToast(errorMessage(error, 'No se pudo actualizar la clase. Tus cambios se conservaron para reintentar.'), { tone: 'error' }); }
    finally { inFlight.current = false; setBusy(false); }
  }
  async function justify(id: number) {
    if (!selectedId || inFlight.current) return;
    inFlight.current = true; setBusy(true);
    try { await updateAttendance(id, 'ausente_justificado'); showToast('Ausencia marcada como justificada.', { tone: 'success' }); await openDetail(selectedId); await load(); }
    catch (error) { showToast(errorMessage(error, 'No se pudo actualizar la asistencia.'), { tone: 'error' }); }
    finally { inFlight.current = false; setBusy(false); }
  }
  const summary = clases?.find((item) => item.id === selectedId);
  const rows = useMemo(() => [...(selected?.asistencias ?? [])].sort((a, b) => {
    if (!a.alumnoApellido || !b.alumnoApellido) return 0; // Old API is already surname-ordered.
    return studentName(a).localeCompare(studentName(b), 'es') || a.id - b.id;
  }), [selected]);
  const stateOf = (item: ClaseDetalleDto['asistencias'][number]) => editing ? editStates[item.id] : item.estado;
  const counts = { present: rows.filter((item) => stateOf(item) === 'presente').length, absent: rows.filter((item) => stateOf(item) === 'ausente').length, justified: rows.filter((item) => stateOf(item) === 'ausente_justificado').length, pending: rows.filter((item) => stateOf(item) === 'pendiente').length };

  if (!clases && error) return <ContentState tone="error" title="No se pudieron cargar las clases" detail={error} actions={<button className="button" type="button" onClick={() => void load()}>Reintentar</button>} />;
  if (!clases) return <ContentState tone="loading" title="Cargando clases…" detail="Estamos consultando el historial de clases dictadas." />;
  if (!clases.length) return <ContentState tone="empty" title="Todavía no registraste clases" detail="Cuando registres una clase desde 'Iniciar clase' aparecerá acá." />;
  return <section className="panel class-history">
    <header className="panel-header"><h2>Clases dadas</h2><p>Historial de clases que registraste. Podés marcar una ausencia como justificada.</p></header>
    {error && <p className="notice error" role="alert">{error} <button type="button" onClick={() => void load()}>Reintentar</button></p>}
    <div className="class-history-toolbar form-grid" role="search" aria-label="Filtrar historial de clases">
      <label className="class-history-search" htmlFor="history-query">Buscar clases<div className="class-search-input"><svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><circle cx="10" cy="10" r="6" /><path d="m15 15 6 6" /></svg><input id="history-query" type="search" value={query} placeholder="Buscar por tema, materia o curso" onChange={(event) => changeFilter('q', event.target.value)} /></div></label>
      <div className="class-history-filter"><span>Curso</span><AnimatedSelect ariaLabel="Filtrar por curso" value={curso} onChange={(value) => changeFilter('curso', value)} options={[{ value: '', label: 'Todos los cursos' }, ...courses.map(([value, label]) => ({ value, label })), ...(curso && !courses.some(([value]) => value === curso) ? [{ value: curso, label: 'Curso no disponible' }] : [])]} /></div>
      <div className="class-history-filter"><span>Especialidad</span><AnimatedSelect ariaLabel="Filtrar por especialidad" value={specialty} onChange={(value) => changeFilter('especialidad', value)} options={[{ value: '', label: 'Todas las especialidades' }, ...specialties.map(([value, label]) => ({ value, label })), ...(specialty && !specialties.some(([value]) => value === specialty) ? [{ value: specialty, label: 'Especialidad no disponible' }] : [])]} /></div>
      <div className="class-history-filter"><span>Orden por fecha</span><AnimatedSelect ariaLabel="Orden por fecha" value={order} onChange={(value) => changeFilter('orden', value)} options={[{ value: 'desc', label: 'Más recientes primero' }, { value: 'asc', label: 'Más antiguas primero' }]} /></div>
    </div>
    <div className="class-history-results"><p role="status">Mostrando {Math.min(visibleCount, filtered.length)} de {clases.length} clases{filtered.length !== clases.length ? ` · ${filtered.length} coincidencias` : ''}</p>{hasFilters && !!filtered.length && <button type="button" className="button secondary" onClick={clearFilters}>Limpiar filtros</button>}</div>
    {!filtered.length ? <ContentState tone="empty" title="No hay clases que coincidan con tu búsqueda" actions={<button type="button" className="button secondary" onClick={clearFilters}>Limpiar filtros</button>} /> : <div className="card-grid class-history-cards">
      {filtered.slice(0, visibleCount).map((item) => <button key={item.id} type="button" className="nav-card class-history-card" onClick={() => void openDetail(item.id)} aria-label={`Ver clase: ${item.materiaNombre ?? 'Materia'}, ${item.cursoDescripcion}, ${classDate(item.fechaClase)}, ${item.tema}`}>
        <span className="class-card-date">{classDate(item.fechaClase)}<span aria-hidden="true">›</span></span><h3>{item.materiaNombre ?? 'Materia'}</h3><span>{item.cursoDescripcion}</span><p>{item.tema}</p>
        <div className="class-attendance-summary"><span className="class-attendance-badge present">Presentes: {item.totalPresentes ?? '—'}</span><span className="class-attendance-badge absent">Ausentes: {item.totalAusentes}</span><span className="class-attendance-badge warning">Justificados: {item.totalJustificados}</span>{!!item.totalPendientes && <span className="class-attendance-badge neutral">Pendientes: {item.totalPendientes}</span>}</div>
      </button>)}
    </div>}
    {visibleCount < filtered.length && <div className="class-history-more"><button type="button" className="button secondary" onClick={() => setVisibleCount((count) => count + PAGE_SIZE)}>Cargar más clases</button></div>}
    {selectedId !== null && <div ref={dialogRef} className="data-modal class-detail-modal" role="dialog" aria-modal="true" aria-labelledby="clase-detalle-title" aria-describedby="clase-detalle-course" tabIndex={-1} aria-busy={busy || detailLoading}>
      <header className="signature-modal-header"><div><span>Clase dictada</span><h2 id="clase-detalle-title">Detalle de clase</h2><p id="clase-detalle-course">{summary?.cursoDescripcion || (selected ? `Curso #${selected.cursoId}` : 'Cargando curso…')}{summary?.materiaNombre ? ` · ${summary.materiaNombre}` : ''}</p></div>
        <div className="class-detail-header-actions">{selected && !editing && editingAllowed(selected.fechaClase) && <button type="button" className="button secondary" onClick={beginEdit} disabled={busy}>Editar</button>}<button type="button" data-dialog-initial-focus className="signature-modal-close" aria-label="Cerrar detalle" disabled={busy} onClick={closeDetail}>×</button></div>
      </header>
      <div className="class-detail-body">
        {detailLoading ? <ContentState tone="loading" title="Cargando detalle…" /> : detailError ? <ContentState tone="error" title="No se pudo cargar el detalle" detail={detailError} actions={<button className="button secondary" type="button" onClick={() => void openDetail(selectedId)}>Reintentar</button>} /> : selected && <>
          <dl className="class-detail-meta"><div><dt>Tema</dt><dd>{editing ? <label className="form-grid"><span className="visually-hidden">Tema de la clase</span><input aria-label="Tema de la clase" maxLength={150} value={editTema} disabled={busy} onChange={(event) => setEditTema(event.target.value)} /></label> : selected.tema}</dd></div><div><dt>Fecha</dt><dd>{classDate(selected.fechaClase)}</dd></div></dl>
          <div className="class-attendance-summary" aria-label="Resumen de asistencia"><span className="class-attendance-badge present">Presentes: {counts.present}</span><span className="class-attendance-badge absent">Ausentes: {counts.absent}</span><span className="class-attendance-badge warning">Justificados: {counts.justified}</span>{counts.pending > 0 && <span className="class-attendance-badge neutral">Pendientes: {counts.pending}</span>}</div>
          {!editingAllowed(selected.fechaClase) && <p className="class-detail-note">El plazo de edición ya venció.</p>}
          {catalogError && <p className="notice error" role="alert">{catalogError} <button type="button" onClick={() => void loadCatalog()}>Reintentar catálogo</button></p>}
          {editing && <p className="class-detail-note">Los cambios de asistencia y rasgos se aplican al guardar.</p>}
          {!rows.length ? <ContentState tone="empty" title="Esta clase no tiene asistencias registradas" /> : <table className="grade-table class-detail-table"><caption className="visually-hidden">Asistencia y rasgos conductuales de la clase</caption><thead><tr><th scope="col">N°</th><th scope="col">Alumno</th><th scope="col">Estado</th><th scope="col">Rasgos</th><th scope="col">Acción</th></tr></thead><tbody>{rows.map((item, index) => <ClassAttendanceRow key={item.id} item={item} index={index} estado={stateOf(item)} codes={editing ? editCodigos[item.id] ?? [] : item.codigos ?? []} catalog={catalog} editing={editing} busy={busy}
            onState={(state) => setEditStates((current) => ({ ...current, [item.id]: state }))}
            onCode={(code) => setEditCodigos((current) => { const codes = current[item.id] ?? []; return { ...current, [item.id]: codes.includes(code) ? codes.filter((value) => value !== code) : [...codes, code] }; })}
            onJustify={() => void justify(item.id)} />)}</tbody></table>}
        </>}
      </div>
      {editing && <footer className="class-detail-footer"><button className="button secondary" type="button" disabled={busy} onClick={() => setEditing(false)}>Cancelar</button><button className="button" type="button" disabled={busy || !editTema.trim()} onClick={() => void saveEdit()}>{busy ? 'Guardando…' : 'Guardar cambios'}</button></footer>}
    </div>}
  </section>;
}
