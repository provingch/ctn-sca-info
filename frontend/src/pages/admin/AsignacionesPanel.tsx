import { useEffect, useMemo, useState } from 'react';
import { createAdminRecord, createHorarioSlot, deleteAssignment, deleteHorarioSlot, getHoraCatedraCatalog, getHorarioByAsignacion, updateAdminRecord, type AdminCatalog, type HoraCatedraItem, type HorarioSlotItem } from '../../api/admin';
import { ApiError } from '../../api/client';
import AnimatedSelect from '../../components/AnimatedSelect';
import useAccessibleDialog from '../../hooks/useAccessibleDialog';

interface AsignacionesPanelProps {
  data: AdminCatalog;
  reload: () => Promise<void>;
  status: (message: string) => void;
}

interface HorarioStepperProps {
  label: string;
  item?: HoraCatedraItem;
  canDecrease: boolean;
  canIncrease: boolean;
  onDecrease: () => void;
  onIncrease: () => void;
}

function HorarioStepper({ label, item, canDecrease, canIncrease, onDecrease, onIncrease }: HorarioStepperProps) {
  return (
    <div className="horario-stepper-field">
      <span className="horario-stepper-label">{label}</span>
      <div className="horario-stepper">
        <div className="horario-stepper-value" aria-live="polite">
          <strong>{item?.horaInicio || '—'}</strong>
          <small>Fin {item?.horaFin || '—'}</small>
        </div>
        <div className="horario-stepper-actions">
          <button type="button" className="button secondary" aria-label={`Avanzar hora de ${label.toLowerCase()} 35 minutos`} disabled={!canIncrease} onClick={onIncrease}>
            <svg viewBox="0 0 20 20" aria-hidden="true"><path d="m5 12.5 5-5 5 5" /></svg>
          </button>
          <button type="button" className="button secondary" aria-label={`Retroceder hora de ${label.toLowerCase()} 35 minutos`} disabled={!canDecrease} onClick={onDecrease}>
            <svg viewBox="0 0 20 20" aria-hidden="true"><path d="m5 7.5 5 5 5-5" /></svg>
          </button>
        </div>
      </div>
    </div>
  );
}

export default function AsignacionesPanel({ data, reload, status }: AsignacionesPanelProps) {
  const [selectedProfesorId, setSelectedProfesorId] = useState<number | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState({ materiaId: '', cursoIds: [] as number[] });
  const [horarioPopup, setHorarioPopup] = useState<{ assignmentId: number; slots: HorarioSlotItem[]; catalog: HoraCatedraItem[]; diaSemana: number; horaCatedraId: string; hastaHoraCatedraId?: string; sala: string } | null>(null);
  const formDialogRef = useAccessibleDialog(isFormOpen, () => setIsFormOpen(false));
  const scheduleDialogRef = useAccessibleDialog(Boolean(horarioPopup), () => setHorarioPopup(null));

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
      let hadConflict = false;
      for (const item of slice) {
        try {
          await createHorarioSlot(horarioPopup.assignmentId, {
            diaSemana: horarioPopup.diaSemana,
            horaCatedraId: Number(item.id),
            salaId: null,
          });
        } catch (err) {
          if (err instanceof ApiError && err.status === 409) {
            hadConflict = true;
            status(`Conflicto al crear hora ${item.numero}: ${err.message}`);
            break;
          }
          throw err;
        }
      }

      if (!hadConflict) {
        status('Horario(s) agregado(s).');
      }

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
          const sortedCatalog = [...catalog].sort((first, second) => first.numero - second.numero);
          setHorarioPopup((current) => current ? { ...current, catalog: sortedCatalog, horaCatedraId: String(current.horaCatedraId || sortedCatalog[0]?.id || ''), hastaHoraCatedraId: String(current.hastaHoraCatedraId || current.horaCatedraId || sortedCatalog[0]?.id || '') } : current);
        } catch (error) {
          status(error instanceof ApiError ? error.message : 'No se pudo obtener el catálogo de horas.');
        }
      })();
    }
  }, [horarioPopup, status]);

  const horarioDesdeIndex = horarioPopup
    ? horarioPopup.catalog.findIndex((item) => String(item.id) === horarioPopup.horaCatedraId)
    : -1;
  const horarioHastaIndex = horarioPopup
    ? horarioPopup.catalog.findIndex((item) => String(item.id) === (horarioPopup.hastaHoraCatedraId || horarioPopup.horaCatedraId))
    : -1;

  const moveHorarioBoundary = (boundary: 'desde' | 'hasta', delta: -1 | 1) => {
    setHorarioPopup((current) => {
      if (!current || current.catalog.length === 0) return current;
      const desdeIndex = Math.max(0, current.catalog.findIndex((item) => String(item.id) === current.horaCatedraId));
      const hastaIndex = Math.max(desdeIndex, current.catalog.findIndex((item) => String(item.id) === (current.hastaHoraCatedraId || current.horaCatedraId)));

      if (boundary === 'desde') {
        const nextDesdeIndex = Math.min(current.catalog.length - 1, Math.max(0, desdeIndex + delta));
        const nextHastaIndex = Math.max(hastaIndex, nextDesdeIndex);
        return {
          ...current,
          horaCatedraId: String(current.catalog[nextDesdeIndex].id),
          hastaHoraCatedraId: String(current.catalog[nextHastaIndex].id),
        };
      }

      const nextHastaIndex = Math.min(current.catalog.length - 1, Math.max(desdeIndex, hastaIndex + delta));
      return { ...current, hastaHoraCatedraId: String(current.catalog[nextHastaIndex].id) };
    });
  };

  if (!selectedProfesorId) {
    return (
      <>
        <div className="table-wrap">
          <table className="grade-table" style={{ minWidth: 680 }}>
            <caption className="visually-hidden">Profesores y cantidad de asignaciones</caption>
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
          <caption className="visually-hidden">Asignaciones del profesor seleccionado</caption>
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
        <div ref={formDialogRef} className="data-modal" role="dialog" aria-modal="true" aria-labelledby="asignacion-form-title" tabIndex={-1}>
          <div className="signature-modal-header">
            <div>
              <span>Asignaciones</span>
              <h2 id="asignacion-form-title">{editingId ? 'Editar asignación' : 'Crear asignación'}</h2>
            </div>
            <button type="button" className="signature-modal-close" aria-label="Cerrar formulario" data-dialog-initial-focus onClick={() => setIsFormOpen(false)}>×</button>
          </div>
          <form className="form-grid" onSubmit={submit} style={{ alignContent: 'start' }}>
            <label>
              Materia
              <AnimatedSelect ariaLabel="Materia" value={form.materiaId} placeholder="Seleccione…" onChange={(value) => setForm({ materiaId: value, cursoIds: [] })} options={materiaOptions.map((subject) => ({ value: subject.id, label: subject.nombre }))} />
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
        <div ref={scheduleDialogRef} className="data-modal" role="dialog" aria-modal="true" aria-labelledby="horario-panel-title" tabIndex={-1} style={{ maxWidth: 700 }}>
          <div className="signature-modal-header">
            <div>
              <span>Horario</span>
              <h2 id="horario-panel-title">Asignación #{horarioPopup.assignmentId}</h2>
            </div>
            <button type="button" className="signature-modal-close" aria-label="Cerrar horario" data-dialog-initial-focus onClick={() => setHorarioPopup(null)}>×</button>
          </div>

          <div className="form-grid" style={{ alignContent: 'start' }}>
            <label>
              Día
              <AnimatedSelect ariaLabel="Día de la semana" value={horarioPopup.diaSemana} onChange={(value) => setHorarioPopup({ ...horarioPopup, diaSemana: Number(value) })} options={['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'].map((label, index) => ({ value: index + 1, label }))} />
            </label>

            <HorarioStepper
              label="Desde"
              item={horarioPopup.catalog[horarioDesdeIndex]}
              canDecrease={horarioDesdeIndex > 0}
              canIncrease={horarioDesdeIndex >= 0 && horarioDesdeIndex < horarioPopup.catalog.length - 1}
              onDecrease={() => moveHorarioBoundary('desde', -1)}
              onIncrease={() => moveHorarioBoundary('desde', 1)}
            />

            <HorarioStepper
              label="Hasta"
              item={horarioPopup.catalog[horarioHastaIndex]}
              canDecrease={horarioHastaIndex > horarioDesdeIndex}
              canIncrease={horarioHastaIndex >= 0 && horarioHastaIndex < horarioPopup.catalog.length - 1}
              onDecrease={() => moveHorarioBoundary('hasta', -1)}
              onIncrease={() => moveHorarioBoundary('hasta', 1)}
            />

            <label style={{ gridColumn: 'span 2' }}>
              Sala (opcional)
              <input type="text" value={horarioPopup.sala} onChange={(event) => setHorarioPopup({ ...horarioPopup, sala: event.target.value })} placeholder="Ej.: A-101" />
            </label>

            <div className="signature-modal-actions" style={{ gridColumn: 'span 2' }}>
              <button type="button" className="button secondary" onClick={() => setHorarioPopup(null)}>Cancelar</button>
              <button type="button" className="button" onClick={() => void submitHorario()}>Agregar</button>
            </div>
          </div>

          <div className="data-modal-section">
            <h3>Slots cargados</h3>
            {horarioPopup.slots.length === 0 ? (
              <p>No hay slots cargados para esta asignación.</p>
            ) : (
              <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'grid', gap: 8 }}>
                {horarioPopup.slots.map((slot) => (
                  <li key={slot.id} style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'center', border: '1px solid #ddd', padding: 8, borderRadius: 6 }}>
                    <span>
                      {['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'][slot.diaSemana - 1]} · {slot.horaCatedraNumero} · {slot.horaInicio}-{slot.horaFin}
                      {slot.salaNombre ? ` · ${slot.salaNombre}` : ''}
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
