import { useMemo, useState } from 'react';
import { createAdminRecord, deleteAssignment, updateAdminRecord, type AdminCatalog } from '../../api/admin';
import { ApiError } from '../../api/client';

interface AsignacionesPanelProps {
  data: AdminCatalog;
  reload: () => Promise<void>;
  status: (message: string) => void;
}

export default function AsignacionesPanel({ data, reload, status }: AsignacionesPanelProps) {
  const [selectedProfesorId, setSelectedProfesorId] = useState<number | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState({ materiaId: '', cursoIds: [] as number[] });

  const profesores = useMemo(() => data.usuarios.filter((user) => user.nivel === 1), [data.usuarios]);
  const profesorAsignaciones = useMemo(
    () => data.asignaciones.filter((assignment) => assignment.profesorId === selectedProfesorId),
    [data.asignaciones, selectedProfesorId],
  );

  const openCreate = () => {
    setEditingId(null);
    setForm({ materiaId: '', cursoIds: [] });
    setIsFormOpen(true);
  };

  const openEdit = (assignment: AdminCatalog['asignaciones'][number]) => {
    setEditingId(assignment.id);
    setForm({ materiaId: String(assignment.materiaId), cursoIds: [assignment.cursoId] });
    setIsFormOpen(true);
  };

  const selectedProfesor = profesores.find((profesor) => profesor.id === selectedProfesorId) ?? null;
  const materiaOptions = data.materias;
  const selectedMateria = materiaOptions.find((materia) => String(materia.id) === form.materiaId) ?? null;
  const allowedEspecialidadIds = selectedMateria?.especialidadIds ?? [];

  const visibleCourseOptions = data.cursos.filter((course) => {
    if (!selectedMateria) return true;
    const allowedEspecialidades = allowedEspecialidadIds.length > 0 ? allowedEspecialidadIds : [];
    return allowedEspecialidades.length === 0 || allowedEspecialidades.includes(data.especialidades.find((specialty) => specialty.nombre === course.especialidad)?.id ?? -1);
  });

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!selectedProfesorId || !form.materiaId || form.cursoIds.length === 0) {
      status('Debes elegir materia y al menos un curso.');
      return;
    }

    try {
      if (editingId) {
        await updateAdminRecord('asignaciones', editingId, {
          profesorId: selectedProfesorId,
          materiaId: Number(form.materiaId),
          cursoId: form.cursoIds[0],
        });
        status('Asignación actualizada.');
      } else {
        await createAdminRecord('asignaciones/batch', {
          profesorId: selectedProfesorId,
          materiaId: Number(form.materiaId),
          cursoIds: form.cursoIds,
        });
        status('Asignaciones creadas.');
      }
      setIsFormOpen(false);
      await reload();
    } catch (error) {
      status(error instanceof ApiError ? error.message : 'No se pudo guardar la asignación.');
    }
  };

  if (!selectedProfesorId) {
    return (
      <>
        <div className="table-wrap">
          <table className="grade-table" style={{ minWidth: 680 }}>
            <thead>
              <tr>
                <th>Profesor</th>
                <th>Asignaciones</th>
              </tr>
            </thead>
            <tbody>
              {profesores.map((profesor) => (
                <tr key={profesor.id}>
                  <td>
                    <button type="button" className="button secondary" style={{ width: '100%', justifyContent: 'flex-start' }} onClick={() => setSelectedProfesorId(profesor.id)}>
                      {profesor.apellido}, {profesor.nombre}
                    </button>
                  </td>
                  <td>{data.asignaciones.filter((assignment) => assignment.profesorId === profesor.id).length}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </>
    );
  }

  return (
    <>
      <div className="toolbar">
        <button type="button" className="button secondary" onClick={() => setSelectedProfesorId(null)}>← Volver a profesores</button>
        <button type="button" className="button" onClick={openCreate}>Crear asignación</button>
      </div>

      <h2>Asignaciones de {selectedProfesor?.apellido}, {selectedProfesor?.nombre}</h2>

      <div className="table-wrap">
        <table className="grade-table" style={{ minWidth: 760 }}>
          <thead>
            <tr>
              <th>Materia</th>
              <th>Especialidad</th>
              <th>Curso</th>
              <th>Sección</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {profesorAsignaciones.length === 0 ? (
              <tr><td colSpan={5}>No hay asignaciones para este profesor.</td></tr>
            ) : (
              profesorAsignaciones.map((assignment) => {
                const course = data.cursos.find((item) => item.id === assignment.cursoId);
                return (
                  <tr key={assignment.id}>
                    <td>{assignment.materia}</td>
                    <td>{course?.especialidad ?? '—'}</td>
                    <td>{course?.nivel ?? '—'}°</td>
                    <td>{course?.seccion ?? '—'}</td>
                    <td>
                      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                        <button type="button" className="button secondary" onClick={() => openEdit(assignment)}>Editar</button>
                        <button type="button" className="button danger" onClick={async () => {
                          if (!window.confirm('¿Eliminar esta asignación?')) return;
                          try {
                            await deleteAssignment(assignment.id);
                            status('Asignación eliminada.');
                            await reload();
                          } catch (error) {
                            status(error instanceof ApiError ? error.message : 'No se pudo eliminar la asignación.');
                          }
                        }}>Eliminar</button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {isFormOpen && (
        <div className="signature-modal" role="dialog" aria-modal="true" aria-labelledby="asignacion-form-title" style={{ display: 'grid' }}>
          <div className="signature-modal-header">
            <div>
              <span>Asignaciones</span>
              <h2 id="asignacion-form-title">{editingId ? 'Editar asignación' : 'Crear asignación'}</h2>
            </div>
            <button type="button" className="signature-modal-close" aria-label="Cerrar formulario" onClick={() => setIsFormOpen(false)}>×</button>
          </div>
          <form className="form-grid" onSubmit={submit} style={{ alignContent: 'start' }}>
            <label>
              Materia
              <select value={form.materiaId} onChange={(event) => setForm({ materiaId: event.target.value, cursoIds: [] })}>
                <option value="">Seleccione…</option>
                {materiaOptions.map((subject) => (
                  <option key={subject.id} value={subject.id}>{subject.nombre}</option>
                ))}
              </select>
            </label>

            <fieldset>
              <legend>Cursos y secciones</legend>
              {visibleCourseOptions.length === 0 ? (
                <p>No hay cursos disponibles para la materia elegida.</p>
              ) : (
                <div className="check-list" style={{ display: 'grid' }}>
                  {visibleCourseOptions.map((course) => (
                    <label key={course.id} style={{ display: 'flex', gap: 8 }}>
                      <input
                        type="checkbox"
                        checked={form.cursoIds.includes(course.id)}
                        onChange={(event) => {
                          const next = event.target.checked
                            ? [...form.cursoIds, course.id]
                            : form.cursoIds.filter((id) => id !== course.id);
                          setForm({ ...form, cursoIds: next });
                        }}
                      />
                      {course.nivel}° {course.seccion} · {course.especialidad}
                    </label>
                  ))}
                </div>
              )}
            </fieldset>

            <div className="signature-modal-actions">
              <button type="button" className="button secondary" onClick={() => setIsFormOpen(false)}>Cancelar</button>
              <button type="submit" className="button" style={{ gridColumn: 'span 2' }}>{editingId ? 'Guardar cambios' : 'Crear asignación'}</button>
            </div>
          </form>
        </div>
      )}
    </>
  );
}
