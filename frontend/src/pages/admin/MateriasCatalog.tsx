import { useMemo, useRef, useState } from 'react';
import { deleteAdminRecord, type AdminCatalog, type MateriaItem } from '../../api/admin';
import { ApiError } from '../../api/client';
import AnimatedSelect from '../../components/AnimatedSelect';
import useAccessibleDialog from '../../hooks/useAccessibleDialog';
import { normalizeSpecialty } from '../../theme/theme';
import './MateriasCatalog.css';
import { confirmar } from '../../utils/confirmar';
import useSeleccion from '../../hooks/useSeleccion';
import { BarraSeleccion, CasillaFila, CasillaTodos } from '../../components/SeleccionTabla';

const normalize = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
const categoryName = (value: string) => value === 'especifico' ? 'Específica' : 'Común';

export default function MateriasCatalog({ data, reload, status, onCreate, onEdit }: {
  data: AdminCatalog; reload: () => Promise<void>; status: (message: string) => void;
  onCreate: () => void; onEdit: (item: MateriaItem) => void;
}) {
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('');
  const [specialty, setSpecialty] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [sort, setSort] = useState<{ key: 'nombre' | 'categoria'; direction: 'ascending' | 'descending' }>({ key: 'nombre', direction: 'ascending' });
  const [target, setTarget] = useState<MateriaItem | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const lock = useRef(false);
  const dialogRef = useAccessibleDialog(!!target, () => { if (!lock.current) setTarget(null); });
  const filtered = useMemo(() => data.materias.filter((item) =>
    normalize(item.nombre).includes(normalize(query))
    && (!category || item.categoria === category)
    && (!specialty || (specialty === 'none' ? !item.especialidadIds?.length : item.especialidadIds?.includes(Number(specialty))))
  ).sort((a, b) => {
    const comparison = (sort.key === 'nombre' ? a.nombre : categoryName(a.categoria)).localeCompare(sort.key === 'nombre' ? b.nombre : categoryName(b.categoria), 'es', { sensitivity: 'base', numeric: true });
    return (sort.direction === 'ascending' ? comparison : -comparison) || a.nombre.localeCompare(b.nombre, 'es') || a.id - b.id;
  }), [data.materias, query, category, specialty, sort]);
  const pages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const currentPage = Math.min(page, pages);
  const visible = filtered.slice((currentPage - 1) * pageSize, currentPage * pageSize);
  const seleccion = useSeleccion(useMemo(() => visible.map((item) => item.id), [visible]));
  const assignedCount = target ? data.asignaciones.filter((item) => item.materiaId === target.id).length : 0;

  function changeSort(key: 'nombre' | 'categoria') {
    setSort({ key, direction: sort.key === key && sort.direction === 'ascending' ? 'descending' : 'ascending' });
    setPage(1);
  }
  async function confirmDelete() {
    if (!target || lock.current || assignedCount > 0) return;
    lock.current = true;
    setBusy(true);
    setError('');
    try {
      await deleteAdminRecord('materias', target.id);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo eliminar la materia. Reintentá más tarde.');
      setBusy(false);
      lock.current = false;
      return;
    }
    setTarget(null);
    status('Materia eliminada.');
    try { await reload(); } catch { status('La materia se eliminó, pero no se pudo actualizar la lista. Recargá la página.'); }
    setBusy(false);
    lock.current = false;
  }

  /** Las materias con asignaciones no se pueden borrar: se avisa y se saltean. */
  async function deleteSelection() {
    if (lock.current) return;
    const conAsignaciones = seleccion.ids.filter((id) => data.asignaciones.some((item) => item.materiaId === id));
    const borrables = seleccion.ids.filter((id) => !conAsignaciones.includes(id));
    if (borrables.length === 0) {
      status('Ninguna de las materias elegidas se puede eliminar: todas tienen asignaciones vinculadas.');
      return;
    }
    const aviso = conAsignaciones.length > 0 ? ` ${conAsignaciones.length} de las elegidas tienen asignaciones y se van a dejar como están.` : '';
    if (!(await confirmar(`¿Eliminar ${borrables.length} ${borrables.length === 1 ? 'materia' : 'materias'}?${aviso} Esto no se puede deshacer.`, 'Eliminar'))) return;
    lock.current = true;
    setBusy(true);
    let fallidas = 0;
    for (const id of borrables) {
      try { await deleteAdminRecord('materias', id); } catch { fallidas += 1; }
    }
    const eliminadas = borrables.length - fallidas;
    status(fallidas === 0 ? `${eliminadas} ${eliminadas === 1 ? 'materia eliminada' : 'materias eliminadas'}.` : `Se eliminaron ${eliminadas}; ${fallidas} no se pudieron eliminar (pueden tener planillas vinculadas).`);
    seleccion.limpiar();
    try { await reload(); } catch { status('Las materias se eliminaron, pero no se pudo actualizar la lista. Recargá la página.'); }
    setBusy(false);
    lock.current = false;
  }

  return <>
    <section className="panel subjects-panel" aria-label="Catálogo de materias">
      <header className="subjects-header">
        <div><h2>Catálogo de materias</h2><p>{data.materias.length} materias registradas</p></div>
        <button type="button" className="button subjects-create" onClick={onCreate}><span aria-hidden="true">＋</span> Crear materia</button>
      </header>
      <div className="subjects-filters form-grid">
        <label>Buscar por nombre<input type="search" placeholder="Ej.: Matemática" value={query} onChange={(event) => { setQuery(event.target.value); setPage(1); }} /></label>
        <div className="form-field"><span className="field-label">Tipo</span><AnimatedSelect ariaLabel="Filtrar por tipo" value={category} onChange={(value) => { setCategory(value); setPage(1); }} options={[{ value: '', label: 'Todos los tipos' }, { value: 'comun', label: 'Común' }, { value: 'especifico', label: 'Específica' }]} /></div>
        <div className="form-field"><span className="field-label">Especialidad</span><AnimatedSelect ariaLabel="Filtrar por especialidad" value={specialty} onChange={(value) => { setSpecialty(value); setPage(1); }} options={[{ value: '', label: 'Todas las especialidades' }, ...data.especialidades.map((item) => ({ value: item.id, label: item.nombre })), { value: 'none', label: 'Sin especialidad' }]} /></div>
        <button type="button" className="button secondary" disabled={!query && !category && !specialty} onClick={() => { setQuery(''); setCategory(''); setSpecialty(''); setPage(1); }}>Limpiar filtros</button>
      </div>
      <p className="subjects-results" role="status">{filtered.length} de {data.materias.length} materias{filtered.length > 0 && ' · Mostrando ' + ((currentPage - 1) * pageSize + 1) + '–' + Math.min(currentPage * pageSize, filtered.length)}</p>
      <BarraSeleccion cantidad={seleccion.cantidad} unidad={['materia', 'materias']} onLimpiar={seleccion.limpiar} ocupado={busy}>
        <button type="button" className="button danger" onClick={() => void deleteSelection()} disabled={busy}>Eliminar</button>
      </BarraSeleccion>
      <div className="table-wrap subjects-table-wrap" tabIndex={0} role="region" aria-label="Tabla de materias, desplazable horizontalmente">
        <table className="grade-table data-table subjects-table">
          <caption className="visually-hidden">Materias registradas, filtradas y ordenadas</caption>
          <thead><tr>
            <CasillaTodos todos={seleccion.todos} algunos={seleccion.algunos} onChange={seleccion.toggleTodos} etiqueta="Seleccionar todas las materias de esta página" />
            {(['nombre', 'categoria'] as const).map((key) => <th key={key} scope="col" aria-sort={sort.key === key ? sort.direction : 'none'}><button type="button" onClick={() => changeSort(key)} aria-label={'Ordenar por ' + (key === 'nombre' ? 'nombre' : 'tipo')}><span>{key === 'nombre' ? 'Nombre' : 'Tipo'}</span><span aria-hidden="true">{sort.key === key ? sort.direction === 'ascending' ? '↑' : '↓' : '↕'}</span></button></th>)}
            <th scope="col">Especialidades</th><th scope="col" className="col-actions"><span className="visually-hidden">Acciones</span></th>
          </tr></thead>
          <tbody>
            {visible.map((subject) => <tr key={subject.id} className={seleccion.estaSeleccionado(subject.id) ? 'is-selected' : undefined}>
              <CasillaFila checked={seleccion.estaSeleccionado(subject.id)} onChange={() => seleccion.toggle(subject.id)} etiqueta={'Seleccionar ' + subject.nombre} />
              <th scope="row">{subject.nombre}</th>
              <td><span className="subject-type">{categoryName(subject.categoria)}</span></td>
              <td><div className="subject-specialties">{subject.especialidadIds?.length ? subject.especialidadIds.map((id) => {
                const name = data.especialidades.find((item) => item.id === id)?.nombre ?? ('Especialidad #' + id);
                return <span className="subject-specialty-chip" data-specialty={normalizeSpecialty(name)} key={id}><i aria-hidden="true" />{name}</span>;
              }) : <span className="muted-copy">Sin especialidad</span>}</div></td>
              <td className="col-actions"><div className="subject-actions row-actions">
                <button type="button" className="button secondary" aria-label={'Editar ' + subject.nombre} onClick={() => onEdit(subject)}>Editar</button>
                <button type="button" className="button danger subject-delete" aria-label={'Eliminar ' + subject.nombre} onClick={() => { setError(''); setTarget(subject); }}>Eliminar</button>
              </div></td>
            </tr>)}
            {visible.length === 0 && <tr><td colSpan={5} className="subjects-empty"><strong>{data.materias.length ? 'No hay coincidencias' : 'Todavía no hay materias'}</strong><p>{data.materias.length ? 'Probá otro nombre o cambiá los filtros.' : 'Creá la primera materia para comenzar.'}</p></td></tr>}
          </tbody>
        </table>
      </div>
      <footer className="subjects-pagination">
        <div className="subjects-page-size"><span>Por página</span><AnimatedSelect ariaLabel="Materias por página" value={pageSize} onChange={(value) => { setPageSize(Number(value)); setPage(1); }} options={[10, 25, 50].map((value) => ({ value, label: String(value) }))} /></div>
        <nav aria-label="Páginas de materias"><button type="button" className="button secondary" disabled={currentPage === 1} onClick={() => setPage(currentPage - 1)}>Anterior</button><span>Página {currentPage} de {pages}</span><button type="button" className="button secondary" disabled={currentPage >= pages} onClick={() => setPage(currentPage + 1)}>Siguiente</button></nav>
      </footer>
    </section>
    {target && <div ref={dialogRef} className="data-modal subject-delete-modal" role="dialog" aria-modal="true" aria-labelledby="subject-delete-title" aria-describedby="subject-delete-description" tabIndex={-1}>
      <div className="signature-modal-header"><div><span>Eliminar materia</span><h2 id="subject-delete-title">¿Eliminar “{target.nombre}”?</h2></div></div>
      <div className="subject-delete-content">
        <p id="subject-delete-description">Esta acción no se puede deshacer. No se eliminarán materias que tengan planillas o asignaciones vinculadas.</p>
        {assignedCount > 0 && <p className="notice error" role="alert">Esta materia tiene {assignedCount} asignaciones vinculadas. No se puede eliminar.</p>}
        {error && <p className="notice error" role="alert">{error}</p>}
        <div className="signature-modal-actions"><button type="button" className="button secondary" data-dialog-initial-focus disabled={busy} onClick={() => setTarget(null)}>Cancelar</button><button type="button" className="button danger" disabled={busy || assignedCount > 0} onClick={() => void confirmDelete()}>{busy ? 'Eliminando…' : 'Eliminar materia'}</button></div>
      </div>
    </div>}
  </>;
}
