import { useState } from 'react';
import { clearUserGoogleTokens, createAdminRecord, deleteAdminRecord, updateAdminRecord, type AdminCatalog } from '../../api/admin';
import { ApiError } from '../../api/client';

interface UsuariosPanelProps {
  data: AdminCatalog;
  reload: () => Promise<void>;
  status: (message: string) => void;
  isGlobalAdmin: boolean;
}

const USER_LEVELS = [
  { value: 1, label: 'Profesor' },
  { value: 2, label: 'Evaluador' },
  { value: 3, label: 'Administrador' },
  { value: 4, label: 'Padre' },
];

const EMPTY_FORM = { nombre: '', apellido: '', ci: '', usuario: '', nivel: '1', correo: '', especialidadId: '' };

export default function UsuariosPanel({ data, reload, status, isGlobalAdmin }: UsuariosPanelProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState(EMPTY_FORM);

  const openCreate = () => {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setIsOpen(true);
  };

  const openEdit = (user: AdminCatalog['usuarios'][number]) => {
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

  const sections = [
    { key: 1, title: 'Profesores', users: data.usuarios.filter((user) => user.nivel === 1) },
    { key: 2, title: 'Evaluadores', users: data.usuarios.filter((user) => user.nivel === 2) },
    { key: 3, title: 'Administradores', users: data.usuarios.filter((user) => user.nivel === 3) },
    { key: 4, title: 'Padres', users: data.usuarios.filter((user) => user.nivel === 4) },
  ];

  return (
    <>
      <div className="toolbar">
        <button type="button" className="button" onClick={openCreate}>Crear registro</button>
      </div>

      {sections.map((section) => (
        <section key={section.key} className="panel" style={{ marginBottom: 18 }}>
          <h2>{section.title}</h2>
          {section.users.length === 0 ? (
            <p className="muted-copy">No hay usuarios en este nivel.</p>
          ) : (
            <div className="table-wrap">
              <table className="grade-table" style={{ minWidth: 760 }}>
                <thead>
                  <tr>
                    <th>Nombre</th>
                    <th>Apellido</th>
                    <th>Cédula</th>
                    <th>Usuario</th>
                    {section.key === 3 && <th>Especialidad</th>}
                    {(section.key !== 3 || isGlobalAdmin) && <th>Acciones</th>}
                  </tr>
                </thead>
                <tbody>
                  {section.users.map((user) => (
                    <tr key={user.id}>
                      <td>{user.nombre}</td>
                      <td>{user.apellido}</td>
                      <td>{user.ci ?? '—'}</td>
                      <td>{user.usuario}</td>
                      {section.key === 3 && <td>{user.especialidadNombre ?? data.especialidades.find((specialty) => specialty.id === user.especialidadId)?.nombre ?? 'Global'}</td>}
                      {(section.key !== 3 || isGlobalAdmin) && (
                        <td>
                          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                            <button type="button" className="button secondary" onClick={() => openEdit(user)}>Editar</button>
                            {user.nivel !== 4 && (
                              <button type="button" className="button secondary" onClick={async () => {
                                try {
                                  await clearUserGoogleTokens(user.id);
                                  status('Tokens de Google limpiados para el usuario.');
                                  await reload();
                                } catch (error) {
                                  status(error instanceof ApiError ? error.message : 'No se pudo limpiar tokens de Google.');
                                }
                              }}>Limpiar tokens Google</button>
                            )}
                            <button type="button" className="button danger" onClick={async () => {
                                if (!window.confirm('¿Eliminar este usuario?')) return;
                                try {
                                  await deleteAdminRecord('usuarios', user.id);
                                  status('Usuario eliminado.');
                                  await reload();
                                } catch (error) {
                                  status(error instanceof ApiError ? error.message : 'No se pudo eliminar el usuario.');
                                }
                              }}>Eliminar</button>
                          </div>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      ))}

      {isOpen && (
        <div className="signature-modal" role="dialog" aria-modal="true" aria-labelledby="usuario-form-title" style={{ display: 'grid' }}>
          <div className="signature-modal-header">
            <div>
              <span>Usuarios</span>
              <h2 id="usuario-form-title">{editingId ? 'Editar usuario' : 'Crear usuario'}</h2>
            </div>
            <button type="button" className="signature-modal-close" aria-label="Cerrar formulario" onClick={() => setIsOpen(false)}>×</button>
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
              <select value={form.nivel} onChange={(event) => setForm({ ...form, nivel: event.target.value, especialidadId: event.target.value === '3' ? form.especialidadId : '' })}>
                {USER_LEVELS.filter((level) => isGlobalAdmin || level.value !== 3).map((level) => (
                  <option key={level.value} value={level.value}>{level.label}</option>
                ))}
              </select>
            </label>
            {isGlobalAdmin && form.nivel === '3' && <label>
              Especialidad
              <select value={form.especialidadId} onChange={(event) => setForm({ ...form, especialidadId: event.target.value })}>
                <option value="">Administrador global</option>
                {data.especialidades.map((specialty) => <option key={specialty.id} value={specialty.id}>{specialty.nombre}</option>)}
              </select>
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
