import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { ApiError } from '../../api/client';
import { createSala, deleteSala, getSalas, updateSala, type AdminCatalog, type SalaItem } from '../../api/admin';
import AnimatedSelect from '../../components/AnimatedSelect';
import SpecialtyIcon from '../../components/SpecialtyIcon';
import SpecialtyBadge from '../../components/SpecialtyBadge';
import ContentState from '../../components/ui/ContentState';
import useAccessibleDialog from '../../hooks/useAccessibleDialog';
import { normalizeSpecialty } from '../../theme/theme';
import './SalasPanel.css';

const normalize = (text: string) => text.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
const groupKey = (room: SalaItem) => room.especialidadId === null ? 'common' : String(room.especialidadId);
const message = (reason: unknown, fallback: string) => reason instanceof ApiError ? reason.message : fallback;

export default function SalasPanel({ data, status }: { data: AdminCatalog; status: (message: string) => void }) {
  const [items, setItems] = useState<SalaItem[] | null>(null);
  const [loadError, setLoadError] = useState('');
  const [query, setQuery] = useState('');
  const [selectedGroup, setSelectedGroup] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [formOpen, setFormOpen] = useState(false);
  const [name, setName] = useState('');
  const [specialty, setSpecialty] = useState('');
  const [editing, setEditing] = useState<number | null>(null);
  const [target, setTarget] = useState<SalaItem | null>(null);
  const [formError, setFormError] = useState('');
  const [deleteError, setDeleteError] = useState('');
  const [deleteBlocked, setDeleteBlocked] = useState(false);
  const [busy, setBusy] = useState(false);
  const lock = useRef(false);
  const formDialog = useAccessibleDialog(formOpen, () => { if (!lock.current) setFormOpen(false); });
  const deleteDialog = useAccessibleDialog(!!target, () => { if (!lock.current) setTarget(null); });
  const load = useCallback(async () => {
    try { setItems(await getSalas()); setLoadError(''); return true; }
    catch (reason) { setLoadError(message(reason, 'No se pudieron cargar las salas.')); return false; }
  }, []);
  useEffect(() => { void load(); }, [load]);

  const names = new Map<string, string>([['common', 'Comunes'], ...data.especialidades.map((item): [string, string] => [String(item.id), item.nombre])]);
  for (const room of items ?? []) if (!names.has(groupKey(room))) names.set(groupKey(room), room.especialidadNombre || 'Especialidad no disponible');
  const groups = [...names].map(([key, title]) => ({ key, title, rooms: (items ?? []).filter((room) => groupKey(room) === key) }));
  const groupName = (room: SalaItem) => names.get(groupKey(room)) || 'Sin especialidad';
  const globalSearch = query.trim().length > 0;
  const showList = selectedGroup !== null || globalSearch;
  const filtered = (items ?? []).filter((room) => globalSearch ? normalize(room.nombre).includes(normalize(query)) : groupKey(room) === selectedGroup)
    .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es', { numeric: true, sensitivity: 'base' }) || groupName(a).localeCompare(groupName(b), 'es'));
  const pages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const currentPage = Math.min(page, pages);
  const offset = (currentPage - 1) * pageSize;
  const visible = filtered.slice(offset, offset + pageSize);
  const showGroups = () => { setSelectedGroup(null); setQuery(''); setPage(1); };
  const openGroup = (key: string) => { setSelectedGroup(key); setQuery(''); setPage(1); };
  const openForm = (room?: SalaItem) => {
    setEditing(room?.id ?? null); setName(room?.nombre ?? '');
    setSpecialty(room ? (room.especialidadId === null ? '' : String(room.especialidadId)) : selectedGroup && selectedGroup !== 'common' ? selectedGroup : '');
    setFormError(''); setFormOpen(true);
  };
  async function save(event: FormEvent) {
    event.preventDefault();
    if (lock.current) return;
    const payload = { nombre: name.trim().replace(/\s+/g, ' '), especialidadId: specialty ? Number(specialty) : null };
    if (!payload.nombre) { setFormError('El nombre de la sala es requerido.'); return; }
    lock.current = true; setBusy(true); setFormError('');
    try {
      if (editing !== null) await updateSala(editing, payload); else await createSala(payload);
      setFormOpen(false);
      status(await load() ? 'Sala guardada.' : 'Sala guardada. No se pudo actualizar el catálogo; usá Reintentar.');
    } catch (reason) { setFormError(message(reason, 'No se pudo guardar la sala.')); }
    finally { lock.current = false; setBusy(false); }
  }
  async function remove() {
    if (!target || target.bloquesAsignados !== 0 || deleteBlocked || lock.current) return;
    lock.current = true; setBusy(true); setDeleteError('');
    try {
      await deleteSala(target.id); setTarget(null);
      status(await load() ? 'Sala eliminada.' : 'Sala eliminada. No se pudo actualizar el catálogo; usá Reintentar.');
    } catch (reason) {
      setDeleteError(message(reason, 'No se pudo eliminar la sala.'));
      if (reason instanceof ApiError && reason.status === 409) { setDeleteBlocked(true); await load(); }
    } finally { lock.current = false; setBusy(false); }
  }

  return <div className="rooms-page">
    <header className="rooms-heading"><div><h2>Catálogo de salas</h2><p>Elegí una especialidad o buscá una sala en todo el catálogo.</p></div><button className="button" type="button" onClick={() => openForm()}>＋ Agregar sala</button></header>
    <div className="rooms-search"><label>Buscar sala en todo el catálogo<input type="search" value={query} placeholder="Ej.: S4, PC 01" onChange={(event) => { setQuery(event.target.value); setSelectedGroup(null); setPage(1); }} /></label>{query && <button type="button" className="button secondary" onClick={showGroups}>Limpiar búsqueda</button>}</div>
    {loadError && <ContentState tone="error" title={loadError} actions={<button type="button" className="button secondary" onClick={() => void load()}>Reintentar</button>} />}
    {!items && !loadError && <ContentState tone="loading" title="Cargando salas…" />}
    {items && <>
      {showList ? <>
        <nav className="rooms-breadcrumb" aria-label="Ubicación en salas"><button type="button" onClick={showGroups}>Salas</button><span aria-hidden="true">›</span><span aria-current="page">{globalSearch ? 'Resultados de búsqueda' : names.get(selectedGroup!)}</span></nav>
        <div><button className="button secondary" type="button" onClick={showGroups}>← Volver a especialidades</button></div>
        <section className="panel rooms-list">
          <h3>{globalSearch ? `Resultados para “${query.trim()}”` : names.get(selectedGroup!)}</h3>
          <p className="muted-copy">El uso cuenta bloques de horario asignados; no es un porcentaje de ocupación.</p>
          <p role="status" className="muted-copy">Mostrando {visible.length ? offset + 1 : 0}–{offset + visible.length} de {filtered.length} salas</p>
          <div className="table-wrap" role="region" aria-label="Salas y uso en horarios" tabIndex={0}>
            <table className="rooms-table"><caption className="visually-hidden">Salas, especialidad, uso y acciones</caption><thead><tr><th scope="col">Nombre</th><th scope="col">Especialidad</th><th scope="col">Uso en horarios</th><th scope="col">Acciones</th></tr></thead><tbody>
              {visible.map((room) => <tr key={room.id}><th scope="row">{room.nombre}</th><td><SpecialtyBadge name={groupName(room)} /></td><td><span className="room-usage" data-unused={room.bloquesAsignados === 0}>{room.bloquesAsignados == null ? 'Uso no disponible' : room.bloquesAsignados === 0 ? 'Sin bloques asignados' : `${room.bloquesAsignados} ${room.bloquesAsignados === 1 ? 'bloque asignado' : 'bloques asignados'}`}</span></td><td><div className="room-actions">
                {globalSearch && <button className="button secondary" type="button" aria-label={`Ver grupo de ${room.nombre}`} onClick={() => openGroup(groupKey(room))}>Ver grupo</button>}
                <button className="button secondary" type="button" aria-label={`Editar ${room.nombre}`} onClick={() => openForm(room)}>Editar</button>
                <button className="button danger" type="button" aria-label={`Eliminar ${room.nombre}`} onClick={() => { setTarget(room); setDeleteError(''); setDeleteBlocked(false); }}>Eliminar</button>
              </div></td></tr>)}
              {!visible.length && <tr><td colSpan={4} className="rooms-empty">{globalSearch ? 'No hay salas que coincidan con la búsqueda.' : 'Todavía no hay salas en esta especialidad.'}</td></tr>}
            </tbody></table>
          </div>
          <footer className="rooms-pagination"><label>Por página<AnimatedSelect ariaLabel="Salas por página" value={pageSize} onChange={(value) => { setPageSize(Number(value)); setPage(1); }} options={[10, 25, 50].map((value) => ({ value, label: String(value) }))} /></label><nav aria-label="Páginas de salas"><button className="button secondary" type="button" disabled={currentPage === 1} onClick={() => setPage(currentPage - 1)}>Anterior</button><span>Página {currentPage} de {pages}</span><button className="button secondary" type="button" disabled={currentPage === pages} onClick={() => setPage(currentPage + 1)}>Siguiente</button></nav></footer>
        </section>
      </> : <div className="card-grid">{groups.map((group) => <button key={group.key} className="nav-card" type="button" data-specialty={normalizeSpecialty(group.title)} onClick={() => openGroup(group.key)}><span>{group.key === 'common' ? 'Uso compartido' : 'Especialidad'}</span><h2 className="specialty-card-title"><SpecialtyIcon name={group.title} />{group.title}</h2><p>{group.rooms.length} {group.rooms.length === 1 ? 'sala' : 'salas'}</p><strong>Ver salas →</strong></button>)}</div>}
    </>}

    {formOpen && <div ref={formDialog} className="data-modal rooms-modal" role="dialog" aria-modal="true" aria-labelledby="room-form-title" tabIndex={-1}>
      <div className="signature-modal-header"><h2 id="room-form-title">{editing === null ? 'Agregar sala' : 'Editar sala'}</h2><button className="signature-modal-close" type="button" aria-label="Cerrar formulario" disabled={busy} onClick={() => setFormOpen(false)}>×</button></div>
      <form className="form-grid rooms-modal-content" onSubmit={save}>
        <label>Nombre<input data-dialog-initial-focus required maxLength={45} value={name} placeholder={specialty ? 'Ej.: S01' : 'Ej.: PC01'} aria-describedby="room-name-help" disabled={busy} onChange={(event) => setName(event.target.value)} /></label>
        <p className="field-help" id="room-name-help">Formato sugerido: S01, S02… para salas de especialidad; PC01, PC02… para comunes. Usá mayúsculas y dos dígitos. Los nombres existentes pueden conservarse.</p>
        <label>Especialidad / pabellón<AnimatedSelect ariaLabel="Especialidad o pabellón de la sala" value={specialty} disabled={busy} onChange={setSpecialty} options={[{ value: '', label: 'Comunes' }, ...data.especialidades.map((item) => ({ value: item.id, label: item.nombre }))]} /></label>
        {formError && <p className="notice error" role="alert">{formError}</p>}
        <div className="signature-modal-actions"><button className="button secondary" type="button" disabled={busy} onClick={() => setFormOpen(false)}>Cancelar</button><button className="button" type="submit" disabled={busy}>{busy ? 'Guardando…' : 'Guardar sala'}</button></div>
      </form>
    </div>}
    {target && <div ref={deleteDialog} className="data-modal rooms-modal" role="dialog" aria-modal="true" aria-labelledby="room-delete-title" aria-describedby="room-delete-help" tabIndex={-1}>
      <div className="signature-modal-header"><h2 id="room-delete-title">¿Eliminar {target.nombre}?</h2></div>
      <div className="rooms-modal-content"><p id="room-delete-help">Esta acción no se puede deshacer.</p>
        {target.bloquesAsignados == null ? <p className="notice error" role="alert">No se pudo verificar el uso de esta sala. Actualizá el catálogo antes de eliminarla.</p> : target.bloquesAsignados > 0 ? <p className="notice error" role="alert">Tiene {target.bloquesAsignados} bloques de horario asignados. No se puede eliminar hasta reasignar o quitar esos bloques.</p> : <p>La sala no tiene bloques asignados. Se volverá a comprobar su uso al confirmar.</p>}
        {deleteError && <p className="notice error" role="alert">{deleteError}</p>}
        <div className="signature-modal-actions"><button className="button secondary" type="button" data-dialog-initial-focus disabled={busy} onClick={() => setTarget(null)}>Cancelar</button><button className="button danger" type="button" disabled={busy || target.bloquesAsignados !== 0 || deleteBlocked} onClick={() => void remove()}>{busy ? 'Eliminando…' : 'Eliminar sala'}</button></div>
      </div>
    </div>}
  </div>;
}
