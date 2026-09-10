import { useEffect, useRef, useState } from 'react';
import { clearUserGoogleTokens, createAdminRecord, deleteAdminRecord, updateAdminRecord, type AdminCatalog } from '../../api/admin';
import { ApiError } from '../../api/client';
import AnimatedSelect from '../../components/AnimatedSelect';
import useAccessibleDialog from '../../hooks/useAccessibleDialog';
import './UsuariosPanel.css';

interface UsuariosPanelProps {
  data: AdminCatalog;
  reload: () => Promise<void>;
  status: (message: string) => void;
  isGlobalAdmin: boolean;
}

type UserRecord = AdminCatalog['usuarios'][number];

const USER_LEVELS = [
  { value: 1, label: 'Profesor' },
  { value: 2, label: 'Evaluador' },
  { value: 3, label: 'Administrador' },
  { value: 4, label: 'Padre' },
  { value: 5, label: 'Coordinación Pedagógica' },
];

const LEVEL_LABELS: Record<number, string> = {
  1: 'Profesores',
  2: 'Evaluadores',
  3: 'Administradores',
  4: 'Padres',
  5: 'Coordinación Pedagógica',
};

const PAGE_SIZES = [10, 25, 50];

const EMPTY_FORM = { nombre: '', apellido: '', ci: '', usuario: '', nivel: '1', correo: '', especialidadId: '' };

const normalize = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();

function RowActionsMenu({ label, onSelect }: { label: string; onSelect: () => void }) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const handlePointer = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) setOpen(false);
    };
    const handleKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false);
    };
    document.addEventListener('mousedown', handlePointer);
    document.addEventListener('keydown', handleKey);
    return () => {
      document.removeEventListener('mousedown', handlePointer);
      document.removeEventListener('keydown', handleKey);
    };
  }, [open]);

  return (
    <div className="usuarios-row-menu" ref={containerRef}>
      <button
        type="button"
        className="button secondary usuarios-row-menu-trigger"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label="Más acciones"
        onClick={() => setOpen((value) => !value)}
      >
        <span aria-hidden="true">⋮</span>
      </button>
      {open && (
        <div className="usuarios-row-menu-list" role="menu">
          <button
            type="button"
            role="menuitem"
            className="usuarios-row-menu-item"
            onClick={() => { setOpen(false); onSelect(); }}
          >
            {label}
          </button>
        </div>
      )}
    </div>
  );
}

export default function UsuariosPanel({ data, reload, status, isGlobalAdmin }: UsuariosPanelProps) {
  const [isOpen, setIsOpen] = useState(false);
  const formDialogRef = useAccessibleDialog(isOpen, () => setIsOpen(false));
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState(EMPTY_FORM);

  const visibleLevels = USER_LEVELS.map((level) => level.value).filter((value) => isGlobalAdmin || value !== 5);

  const [query, setQuery] = useState('');
  const [nivelFiltro, setNivelFiltro] = useState(visibleLevels[0]);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [sort, setSort] = useState<{ key: 'apellido' | 'nombre'; descending: boolean }>({ key: 'apellido', descending: false });

  const specialtyLabel = (user: UserRecord) => {
    if (!Object.hasOwn(user, 'especialidadId')) return 'No informado por el backend';
    if (user.especialidadId == null) return 'Global';
    return user.especialidadNombre ?? data.especialidades.find((specialty) => specialty.id === user.especialidadId)?.nombre ?? `Especialidad #${user.especialidadId}`;
  };

  const openCreate = () => {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setIsOpen(true);
  };

  const openEdit = (user: UserRecord) => {
    setEditingId(user.id);
    setForm({
      nombre: user.nombre,
      apellido: user.apellido,
      ci: String(user.ci ?? ''),
      usuario: user.usuario,
      nivel: String(user.nivel),
      correo: user.correo ?? '',
      especialidadId: user.especialidadId == null ? '' : String(user.especialidadId),
    });
    setIsOpen(true);
  };

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    const payload = {
      nombre: form.nombre.trim(),
      apellido: form.apellido.trim(),
      usuario: form.usuario.trim(),
      ci: form.ci ? Number(form.ci) : null,
      nivel: Number(form.nivel),
      correo: form.correo.trim() || null,
      especialidadId: Number(form.nivel) === 3 && form.especialidadId ? Number(form.especialidadId) : null,
    };

    try {
      if (editingId) {
        await updateAdminRecord('usuarios', editingId, payload);
        status('Usuario actualizado.');
      } else {
        await createAdminRecord('usuarios', payload);
        status('Usuario creado.');
      }
      setIsOpen(false);
      await reload();
    } catch (error) {
      status(error instanceof ApiError ? error.message : 'No se pudo guardar el usuario.');
    }
  };

  const clearTokens = async (user: UserRecord) => {
    try {
      await clearUserGoogleTokens(user.id);
      status('Tokens de Google limpiados para el usuario.');
      await reload();
    } catch (error) {
      status(error instanceof ApiError ? error.message : 'No se pudo limpiar tokens de Google.');
    }
  };

  const removeUser = async (user: UserRecord) => {
    if (!window.confirm('¿Eliminar este usuario?')) return;
    try {
      await deleteAdminRecord('usuarios', user.id);
      status('Usuario eliminado.');
      await reload();
    } catch (error) {
      status(error instanceof ApiError ? error.message : 'No se pudo eliminar el usuario.');
    }
  };

  const countByLevel = data.usuarios.reduce((acc, user) => {
    acc.set(user.nivel, (acc.get(user.nivel) ?? 0) + 1);
    return acc;
  }, new Map<number, number>());

  const roleOptions = visibleLevels.map((value) => ({
    value,
    label: `${LEVEL_LABELS[value]} (${countByLevel.get(value) ?? 0})`,
  }));

  const levelUsers = data.usuarios.filter((user) => user.nivel === nivelFiltro);
  const normalizedQuery = normalize(query);
  const filtered = levelUsers
    .filter((user) => {
      if (!normalizedQuery) return true;
      const haystack = normalize([
        user.nombre,
        user.apellido,
        user.ci ?? '',
        user.usuario,
        nivelFiltro === 3 ? specialtyLabel(user) : '',
      ].join(' '));
      return haystack.includes(normalizedQuery);
    })
    .sort((a, b) => {
      const left = sort.key === 'apellido' ? `${a.apellido} ${a.nombre}` : `${a.nombre} ${a.apellido}`;
      const right = sort.key === 'apellido' ? `${b.apellido} ${b.nombre}` : `${b.nombre} ${b.apellido}`;
      const comparison = left.localeCompare(right, 'es', { sensitivity: 'base' }) || a.id - b.id;
      return sort.descending ? -comparison : comparison;
    });

  const pageCount = Math.max(1, Math.ceil(filtered.length / pageSize));
  const currentPage = Math.min(page, pageCount);
  const offset = (currentPage - 1) * pageSize;
  const visibleUsers = filtered.slice(offset, offset + pageSize);

  const showEspecialidad = nivelFiltro === 3;
  const showActions = (nivelFiltro !== 3 && nivelFiltro !== 5) || isGlobalAdmin;
  const columnCount = 4 + (showEspecialidad ? 1 : 0) + (showActions ? 1 : 0);

  const changeSort = (key: 'apellido' | 'nombre') => {
    setSort((current) => ({ key, descending: current.key === key ? !current.descending : false }));
    setPage(1);
  };

  const sortIndicator = (key: 'apellido' | 'nombre') => (sort.key === key ? (sort.descending ? '↓' : '↑') : '↕');

  return (
    <>
      <section className="panel usuarios-panel">
        <div className="usuarios-toolbar">
          <label>
            Rol
            <AnimatedSelect
              ariaLabel="Filtrar por rol"
              value={nivelFiltro}
              onChange={(value) => { setNivelFiltro(Number(value)); setPage(1); }}
              options={roleOptions}
            />
          </label>
          <label>
            Buscar
            <input
              type="search"
              placeholder="Buscar por nombre, cédula o usuario"
              value={query}
              onChange={(event) => { setQuery(event.target.value); setPage(1); }}
            />
          </label>
          <label>
            Por página
            <AnimatedSelect
              ariaLabel="Usuarios por página"
              value={pageSize}
              onChange={(value) => { setPageSize(Number(value)); setPage(1); }}
              options={PAGE_SIZES.map((value) => ({ value, label: String(value) }))}
            />
          </label>
          <button type="button" className="button usuarios-create" onClick={openCreate}>Crear registro</button>
        </div>

        <div className="table-wrap">
          <table className="grade-table usuarios-table" style={{ minWidth: 760 }}>
            <caption className="visually-hidden">Usuarios registrados{query ? `, filtrados por “${query}”` : ''}</caption>
            <thead>
              <tr>
                <th scope="col" aria-sort={sort.key === 'nombre' ? (sort.descending ? 'descending' : 'ascending') : 'none'}>
                  <button type="button" className="usuarios-sort" onClick={() => changeSort('nombre')} aria-label="Ordenar por nombre">
                    Nombre <span aria-hidden="true">{sortIndicator('nombre')}</span>
                  </button>
                </th>
                <th scope="col" aria-sort={sort.key === 'apellido' ? (sort.descending ? 'descending' : 'ascending') : 'none'}>
                  <button type="button" className="usuarios-sort" onClick={() => changeSort('apellido')} aria-label="Ordenar por apellido">
                    Apellido <span aria-hidden="true">{sortIndicator('apellido')}</span>
                  </button>
                </th>
                <th scope="col">Cédula</th>
                <th scope="col">Usuario</th>
                {showEspecialidad && <th scope="col">Especialidad</th>}
                {showActions && <th scope="col">Acciones</th>}
              </tr>
            </thead>
            <tbody>
              {visibleUsers.map((user) => (
                <tr key={user.id}>
                  <td>{user.nombre}</td>
                  <td>{user.apellido}</td>
                  <td>{user.ci ?? '—'}</td>
                  <td>{user.usuario}</td>
                  {showEspecialidad && <td>{specialtyLabel(user)}</td>}
                  {showActions && (
                    <td>
                      <div className="usuarios-row-actions">
                        <button type="button" className="button secondary" onClick={() => openEdit(user)}>Editar</button>
                        <button type="button" className="button danger" onClick={() => void removeUser(user)}>Eliminar</button>
                        {user.nivel !== 4 && (
                          <RowActionsMenu label="Limpiar tokens Google" onSelect={() => void clearTokens(user)} />
                        )}
                      </div>
                    </td>
                  )}
                </tr>
              ))}
              {visibleUsers.length === 0 && (
                <tr>
                  <td colSpan={columnCount} className="usuarios-empty">
                    {levelUsers.length === 0 ? (
                      <>
                        <strong>No hay {LEVEL_LABELS[nivelFiltro].toLowerCase()} registrados</strong>
                        <p>Cuando se registren usuarios con este rol van a aparecer acá.</p>
                      </>
                    ) : (
                      <>
                        <strong>No hay coincidencias</strong>
                        <p>Probá con otro nombre, cédula o usuario dentro de {LEVEL_LABELS[nivelFiltro]}.</p>
                      </>
                    )}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="usuarios-pagination">
          <p role="status">
            Mostrando {visibleUsers.length ? offset + 1 : 0}–{offset + visibleUsers.length} de {filtered.length} usuarios
            {query && filtered.length !== levelUsers.length ? ` (${levelUsers.length} en ${LEVEL_LABELS[nivelFiltro]})` : ''}
          </p>
          <nav aria-label="Paginación de usuarios">
            <button type="button" className="button secondary" disabled={currentPage === 1} onClick={() => setPage(currentPage - 1)}>Anterior</button>
            <span>Página {currentPage} de {pageCount}</span>
            <button type="button" className="button secondary" disabled={currentPage === pageCount} onClick={() => setPage(currentPage + 1)}>Siguiente</button>
          </nav>
        </div>
      </section>

      {isOpen && (
        <div ref={formDialogRef} className="data-modal" role="dialog" aria-modal="true" aria-labelledby="usuario-form-title" tabIndex={-1}>
          <div className="signature-modal-header">
            <div>
              <span>Usuarios</span>
              <h2 id="usuario-form-title">{editingId ? 'Editar usuario' : 'Crear usuario'}</h2>
            </div>
            <button type="button" className="signature-modal-close" aria-label="Cerrar formulario" data-dialog-initial-focus onClick={() => setIsOpen(false)}>×</button>
          </div>
          <form className="form-grid" onSubmit={submit} style={{ alignContent: 'start' }}>
            <label>
              Nombres
              <input value={form.nombre} onChange={(event) => setForm({ ...form, nombre: event.target.value })} required />
            </label>
            <label>
              Apellidos
              <input value={form.apellido} onChange={(event) => setForm({ ...form, apellido: event.target.value })} required />
            </label>
            <label>
              Cédula
              <input value={form.ci} onChange={(event) => setForm({ ...form, ci: event.target.value })} />
            </label>
            <label>
              Nombre de usuario
              <input value={form.usuario} onChange={(event) => setForm({ ...form, usuario: event.target.value })} required />
            </label>
            <label>
              Rol
              <AnimatedSelect ariaLabel="Rol del usuario" value={form.nivel} onChange={(value) => setForm({ ...form, nivel: value, especialidadId: value === '3' ? form.especialidadId : '' })} options={USER_LEVELS.filter((level) => isGlobalAdmin || (level.value !== 3 && level.value !== 5))} />
            </label>
            {isGlobalAdmin && form.nivel === '3' && <label>
              Especialidad
              <AnimatedSelect ariaLabel="Especialidad administrada" value={form.especialidadId} onChange={(value) => setForm({ ...form, especialidadId: value })} options={[{ value: '', label: 'Administrador global' }, ...data.especialidades.map((specialty) => ({ value: specialty.id, label: specialty.nombre }))]} />
              <small className="field-help">Dejá vacío para crear un administrador global con acceso a todo.</small>
            </label>}
            <label>
              Correo
              <input type="email" value={form.correo} onChange={(event) => setForm({ ...form, correo: event.target.value })} />
            </label>
            <div className="signature-modal-actions">
              <button type="button" className="button secondary" onClick={() => setIsOpen(false)}>Cancelar</button>
              <button type="submit" className="button" style={{ gridColumn: 'span 2' }}>{editingId ? 'Guardar cambios' : 'Crear usuario'}</button>
            </div>
          </form>
        </div>
      )}
    </>
  );
}
