import { useState } from 'react';
import MateriasCatalog from './MateriasCatalog';
import { createAdminRecord, getMateriaEspecialidades, updateAdminRecord, type AdminCatalog } from '../../api/admin';
import { ApiError } from '../../api/client';
import AnimatedSelect from '../../components/AnimatedSelect';
import useAccessibleDialog from '../../hooks/useAccessibleDialog';

interface MateriasPanelProps {
  data: AdminCatalog;
  reload: () => Promise<void>;
  status: (message: string) => void;
}

export default function MateriasPanel({ data, reload, status }: MateriasPanelProps) {
  const [isOpen, setIsOpen] = useState(false);
  const formDialogRef = useAccessibleDialog(isOpen, () => setIsOpen(false));
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState({ nombre: '', categoria: 'comun', especialidadIds: [] as number[] });
  const onlySpecialty = data.especialidades.length === 1 ? data.especialidades[0] : null;

  const openCreate = () => {
    setEditingId(null);
    setForm({ nombre: '', categoria: 'comun', especialidadIds: onlySpecialty ? [onlySpecialty.id] : [] });
    setIsOpen(true);
  };

  const openEdit = async (item: AdminCatalog['materias'][number]) => {
    let especialidadIds = item.especialidadIds ?? [];
    if ((!item.especialidadIds || item.especialidadIds.length === 0) && item.id) {
      try {
        especialidadIds = await getMateriaEspecialidades(item.id);
      } catch {
        status('No se pudieron cargar las especialidades de la materia. Reintentá antes de editar.');
        return;
      }
    }
    setEditingId(item.id);
    setForm({ nombre: item.nombre, categoria: item.categoria ?? 'comun', especialidadIds: onlySpecialty ? [onlySpecialty.id] : especialidadIds });
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
      <MateriasCatalog data={data} reload={reload} status={status} onCreate={openCreate} onEdit={(item) => void openEdit(item)} />

      {isOpen && (
        <div ref={formDialogRef} className="data-modal" role="dialog" aria-modal="true" aria-labelledby="materia-form-title" tabIndex={-1}>
          <div className="signature-modal-header">
            <div>
              <span>Materias</span>
              <h2 id="materia-form-title">{editingId ? 'Editar materia' : 'Crear materia'}</h2>
            </div>
            <button type="button" className="signature-modal-close" aria-label="Cerrar formulario" data-dialog-initial-focus onClick={() => setIsOpen(false)}>×</button>
          </div>
          <form className="form-grid" onSubmit={submit} style={{ alignContent: 'start' }}>
            <label>
              Nombre
              <input value={form.nombre} onChange={(event) => setForm({ ...form, nombre: event.target.value })} required />
            </label>
            <label>
              Tipo
              <AnimatedSelect ariaLabel="Tipo de materia" value={form.categoria} onChange={handleCategoriaChange} options={[{ value: 'comun', label: 'Común' }, { value: 'especifico', label: 'Específica' }]} />
            </label>

            {onlySpecialty ? (
              <div className="admin-single-specialty"><small>Especialidad</small><strong>{onlySpecialty.nombre}</strong></div>
            ) : form.categoria === 'especifico' ? (
              <label>
                Especialidad
                <AnimatedSelect ariaLabel="Especialidad de la materia" value={form.especialidadIds[0] ?? ''} placeholder="Seleccione…" onChange={(value) => setForm({ ...form, especialidadIds: value ? [Number(value)] : [] })} options={data.especialidades.map((specialty) => ({ value: specialty.id, label: specialty.nombre }))} />
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
