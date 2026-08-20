import { useState } from 'react';
import { createAdminRecord, deleteAdminRecord, getMateriaEspecialidades, updateAdminRecord, type AdminCatalog } from '../../api/admin';
import { ApiError } from '../../api/client';

interface MateriasPanelProps {
  data: AdminCatalog;
  reload: () => Promise<void>;
  status: (message: string) => void;
}

export default function MateriasPanel({ data, reload, status }: MateriasPanelProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState({ nombre: '', categoria: 'comun', especialidadIds: [] as number[] });

  const openCreate = () => {
    setEditingId(null);
    setForm({ nombre: '', categoria: 'comun', especialidadIds: [] });
    setIsOpen(true);
  };

  const openEdit = async (item: AdminCatalog['materias'][number]) => {
    let especialidadIds = item.especialidadIds ?? [];
    if ((!item.especialidadIds || item.especialidadIds.length === 0) && item.id) {
      try {
        especialidadIds = await getMateriaEspecialidades(item.id);
      } catch {
        especialidadIds = [];
      }
    }
    setEditingId(item.id);
    setForm({ nombre: item.nombre, categoria: item.categoria ?? 'comun', especialidadIds });
    setIsOpen(true);
  };

  const handleCategoriaChange = (value: string) => {
    if (value === 'especifico' && form.especialidadIds.length > 1) {
      const proceed = window.confirm('Al cambiar a "Específica" solo se conservará la primera especialidad. ¿Desea continuar?');
      if (!proceed) return;
      setForm({ ...form, categoria: value, especialidadIds: [form.especialidadIds[0]] });
      return;
    }
    setForm({ ...form, categoria: value });
  };

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    const payload = { nombre: form.nombre.trim(), categoria: form.categoria, especialidadIds: form.especialidadIds };

    try {
      if (editingId) {
        await updateAdminRecord('materias', editingId, payload);
        status('Materia actualizada.');
      } else {
        await createAdminRecord('materias', payload);
        status('Materia creada.');
      }
      setIsOpen(false);
      await reload();
    } catch (error) {
      status(error instanceof ApiError ? error.message : 'No se pudo guardar la materia.');
    }
  };

  return (
    <>
      <div className="toolbar">
        <button type="button" className="button" onClick={openCreate}>Crear registro</button>
      </div>

      <div className="table-wrap">
        <table className="grade-table" style={{ minWidth: 720 }}>
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Tipo</th>
              <th>Especialidad(es)</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {data.materias.map((subject) => (
              <tr key={subject.id}>
                <td>{subject.nombre}</td>
                <td>{subject.categoria === 'especifico' ? 'Específica' : 'Común'}</td>
                <td>
                  {subject.especialidadIds && subject.especialidadIds.length > 0
                    ? subject.especialidadIds.map((specialtyId) => data.especialidades.find((specialty) => specialty.id === specialtyId)?.nombre).filter(Boolean).join(', ')
                    : '—'}
                </td>
                <td>
                  <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                    <button type="button" className="button secondary" onClick={() => void openEdit(subject)}>Editar</button>
                    <button type="button" className="button danger" onClick={async () => {
                      if (!window.confirm('¿Eliminar esta materia? Esta acción fallará si existen planillas que la referencian.')) return;
                      try {
                        await deleteAdminRecord('materias', subject.id);
                        status('Materia eliminada.');
                        await reload();
                      } catch (error) {
                        status(error instanceof ApiError ? error.message : 'No se pudo eliminar la materia.');
                      }
                    }}>Eliminar</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {isOpen && (
        <div className="signature-modal" role="dialog" aria-modal="true" aria-labelledby="materia-form-title" style={{ display: 'grid' }}>
          <div className="signature-modal-header">
            <div>
              <span>Materias</span>
              <h2 id="materia-form-title">{editingId ? 'Editar materia' : 'Crear materia'}</h2>
            </div>
            <button type="button" className="signature-modal-close" aria-label="Cerrar formulario" onClick={() => setIsOpen(false)}>×</button>
          </div>
          <form className="form-grid" onSubmit={submit} style={{ alignContent: 'start' }}>
            <label>
              Nombre
              <input value={form.nombre} onChange={(event) => setForm({ ...form, nombre: event.target.value })} required />
            </label>
            <label>
              Tipo
              <select value={form.categoria} onChange={(event) => handleCategoriaChange(event.target.value)}>
                <option value="comun">Común</option>
                <option value="especifico">Específica</option>
              </select>
            </label>

            {form.categoria === 'especifico' ? (
              <label>
                Especialidad
                <select value={form.especialidadIds[0] ?? ''} onChange={(event) => setForm({ ...form, especialidadIds: event.target.value ? [Number(event.target.value)] : [] })}>
                  <option value="">Seleccione…</option>
                  {data.especialidades.map((specialty) => (
                    <option key={specialty.id} value={specialty.id}>{specialty.nombre}</option>
                  ))}
                </select>
              </label>
            ) : (
              <fieldset className="check-list">
                <legend>Especialidades</legend>
                {data.especialidades.map((specialty) => (
                  <label key={specialty.id}>
                    <input
                      type="checkbox"
                      checked={form.especialidadIds.includes(specialty.id)}
                      onChange={(event) => {
                        const next = event.target.checked
                          ? [...form.especialidadIds, specialty.id]
                          : form.especialidadIds.filter((item) => item !== specialty.id);
                        setForm({ ...form, especialidadIds: next });
                      }}
                    />
                    {specialty.nombre}
                  </label>
                ))}
              </fieldset>
            )}

            <div className="signature-modal-actions">
              <button type="button" className="button secondary" onClick={() => setIsOpen(false)}>Cancelar</button>
              <button type="submit" className="button" style={{ gridColumn: 'span 2' }}>{editingId ? 'Guardar cambios' : 'Crear materia'}</button>
            </div>
          </form>
        </div>
      )}
    </>
  );
}
