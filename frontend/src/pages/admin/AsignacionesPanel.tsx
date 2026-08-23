import { useEffect, useMemo, useState } from 'react';
import { createAdminRecord, createHorarioSlot, deleteAssignment, deleteHorarioSlot, downloadHorarioCurso, getHoraCatedraCatalog, getHorarioByAsignacion, updateAdminRecord, type AdminCatalog, type HoraCatedraItem, type HorarioSlotItem } from '../../api/admin';
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
  const [horarioPopup, setHorarioPopup] = useState<{ assignmentId: number; slots: HorarioSlotItem[]; catalog: HoraCatedraItem[]; diaSemana: number; horaCatedraId: string; hastaHoraCatedraId?: string; sala: string } | null>(null);

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

  const openHorario = async (assignmentId: number) => {
    try {
      const [catalog, slots] = await Promise.all([
        getHoraCatedraCatalog(),
        getHorarioByAsignacion(assignmentId),
      ]);
      setHorarioPopup({
        assignmentId,
        slots,
        catalog,
        diaSemana: 1,
        horaCatedraId: String(catalog[0]?.id ?? ''),
        hastaHoraCatedraId: String(catalog[0]?.id ?? ''),
        sala: '',
      });
    } catch (error) {
      status(error instanceof ApiError ? error.message : 'No se pudo cargar el horario.');
    }
  };

  const loadHorario = async (assignmentId: number) => {
    try {
      const slots = await getHorarioByAsignacion(assignmentId);
      setHorarioPopup((current) => current && current.assignmentId === assignmentId
        ? { ...current, slots }
        : current);
    } catch (error) {
      status(error instanceof ApiError ? error.message : 'No se pudo recargar el horario.');
    }
  };

  const submitHorario = async () => {
    if (!horarioPopup) return;
    if (!horarioPopup.horaCatedraId) {
      status('Debes elegir una hora cátedra.');
      return;
    }

    const desdeId = Number(horarioPopup.horaCatedraId);
    const hastaId = Number(horarioPopup.hastaHoraCatedraId || horarioPopup.horaCatedraId);

    // Find positions in catalog
    const idxDesde = horarioPopup.catalog.findIndex((it) => it.id === desdeId);
    const idxHasta = horarioPopup.catalog.findIndex((it) => it.id === hastaId);
    if (idxDesde === -1 || idxHasta === -1) {
      status('Catálogo de horas no válido.');
      return;
    }

    const slice = horarioPopup.catalog.slice(Math.min(idxDesde, idxHasta), Math.max(idxDesde, idxHasta) + 1);

    try {
      for (const item of slice) {
        try {
          await createHorarioSlot(horarioPopup.assignmentId, {
            diaSemana: horarioPopup.diaSemana,
            horaCatedraId: Number(item.id),
            sala: horarioPopup.sala.trim() || undefined,
          });
        } catch (err) {
          if (err instanceof ApiError && err.status === 409) {
            status(`Conflicto al crear hora ${item.numero}: ${err.message}`);
            break;
          }
          throw err;
        }
      }
      status('Horario(s) agregado(s).');
      await loadHorario(horarioPopup.assignmentId);
      setHorarioPopup((current) => current ? { ...current, sala: '', horaCatedraId: String(current.catalog[0]?.id ?? ''), hastaHoraCatedraId: String(current.catalog[0]?.id ?? '') } : current);
    } catch (error) {
      status(error instanceof ApiError ? error.message : 'No se pudo guardar el horario.');
    }
  };

  const removeHorarioSlot = async (slotId: number) => {
    if (!horarioPopup) return;
    try {
      await deleteHorarioSlot(slotId);
      status('Slot removido.');
      await loadHorario(horarioPopup.assignmentId);
    } catch (error) {
      status(error instanceof ApiError ? error.message : 'No se pudo quitar el slot.');
    }
  };

  useEffect(() => {
    if (horarioPopup && !horarioPopup.catalog.length) {
      void (async () => {
        try {
          const catalog = await getHoraCatedraCatalog();
          setHorarioPopup((current) => current ? { ...current, catalog, horaCatedraId: String(current.horaCatedraId || catalog[0]?.id || ''), hastaHoraCatedraId: String(current.hastaHoraCatedraId || current.horaCatedraId || catalog[0]?.id || '') } : current);
        } catch (error) {
          status(error instanceof ApiError ? error.message : 'No se pudo obtener el catálogo de horas.');
        }
      })();
    }
  }, [horarioPopup, status]);

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
      <div className="toolbar" style={{ justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          <button type="button" className="button secondary" onClick={() => setSelectedProfesorId(null)}>← Volver a profesores</button>
          <button type="button" className="button" onClick={openCreate}>Crear asignación</button>
        </div>
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
                        <button type="button" className="button secondary" onClick={() => void openHorario(assignment.id)}>Horario</button>
                        <button type="button" className="button secondary" onClick={async () => {
                          try {
                            await downloadHorarioCurso(assignment.cursoId);
                          } catch (error) {
                            status(error instanceof ApiError ? error.message : 'No se pudo descargar el horario del curso.');
                          }
                        }}>Descargar horario del curso</button>
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

      {horarioPopup && (
        <div className="signature-modal" role="dialog" aria-modal="true" aria-labelledby="horario-panel-title" style={{ display: 'grid', maxWidth: 700 }}>
          <div className="signature-modal-header">
            <div>
              <span>Horario</span>
              <h2 id="horario-panel-title">Asignación #{horarioPopup.assignmentId}</h2>
            </div>
            <button type="button" className="signature-modal-close" aria-label="Cerrar horario" onClick={() => setHorarioPopup(null)}>×</button>
          </div>

          <div className="form-grid" style={{ alignContent: 'start' }}>
            <label>
              Día
              <select value={horarioPopup.diaSemana} onChange={(event) => setHorarioPopup({ ...horarioPopup, diaSemana: Number(event.target.value) })}>
                {['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'].map((label, index) => (
                  <option key={label} value={index + 1}>{label}</option>
                ))}
              </select>
            </label>

            <label>
              Desde
              <select value={horarioPopup.horaCatedraId} onChange={(event) => setHorarioPopup({ ...horarioPopup, horaCatedraId: event.target.value, hastaHoraCatedraId: event.target.value })}>
                <option value="">Seleccione…</option>
                {horarioPopup.catalog.map((item) => (
                  <option key={item.id} value={item.id}>{item.numero} · {item.etiqueta || '—'} · {item.horaInicio}-{item.horaFin}</option>
                ))}
              </select>
            </label>

            <label>
              Hasta
              <select value={horarioPopup.hastaHoraCatedraId || ''} onChange={(event) => setHorarioPopup({ ...horarioPopup, hastaHoraCatedraId: event.target.value })}>
                <option value="">Seleccione…</option>
                {horarioPopup.catalog.filter((it) => Number(it.id) >= Number(horarioPopup.horaCatedraId || 0)).map((item) => (
                  <option key={item.id} value={item.id}>{item.numero} · {item.etiqueta || '—'} · {item.horaInicio}-{item.horaFin}</option>
                ))}
              </select>
            </label>

            <label style={{ gridColumn: 'span 2' }}>
              Sala (opcional)
              <input type="text" value={horarioPopup.sala} onChange={(event) => setHorarioPopup({ ...horarioPopup, sala: event.target.value })} placeholder="Ej.: A-101" />
            </label>

            <div className="signature-modal-actions" style={{ gridColumn: 'span 2' }}>
              <button type="button" className="button secondary" onClick={() => setHorarioPopup(null)}>Cancelar</button>
              <button type="button" className="button" onClick={() => void submitHorario()}>Agregar</button>
            </div>
          </div>

          <div style={{ marginTop: 20 }}>
            <h3>Slots cargados</h3>
            {horarioPopup.slots.length === 0 ? (
              <p>No hay slots cargados para esta asignación.</p>
            ) : (
              <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'grid', gap: 8 }}>
                {horarioPopup.slots.map((slot) => (
                  <li key={slot.id} style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'center', border: '1px solid #ddd', padding: 8, borderRadius: 6 }}>
                    <span>
                      {['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'][slot.diaSemana - 1]} · {slot.horaCatedraNumero} · {slot.horaInicio}-{slot.horaFin}
                      {slot.sala ? ` · ${slot.sala}` : ''}
                    </span>
                    <button type="button" className="button danger" onClick={() => void removeHorarioSlot(slot.id)}>Quitar</button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}
    </>
  );
}
