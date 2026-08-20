import { useMemo, useState } from 'react';
import { ApiError } from '../../api/client';
import { createAdminRecord, deleteAdminRecord, updateAdminRecord, type AdminCatalog } from '../../api/admin';

interface AlumnosPanelProps {
  data: AdminCatalog;
  reload: () => Promise<void>;
  status: (message: string) => void;
}

type ViewStep = 'especialidades' | 'cursos' | 'secciones' | 'tabla';

export default function AlumnosPanel({ data, reload, status }: AlumnosPanelProps) {
  const [step, setStep] = useState<ViewStep>('especialidades');
  const [selectedEspecialidadId, setSelectedEspecialidadId] = useState<number | null>(null);
  const [selectedCursoId, setSelectedCursoId] = useState<number | null>(null);
  const [selectedSeccion, setSelectedSeccion] = useState<string | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState({ nombre: '', apellido: '', ci: '', cursoId: '', correoEncargado: '', correoEncargado2: '' });

  const especialidades = data.especialidades;

  const cursosByEspecialidad = useMemo(() => {
    return new Map(
      especialidades.map((specialty) => {
        const cursos = data.cursos.filter((course) => course.especialidad === specialty.nombre);
        return [specialty.id, cursos];
      }),
    );
  }, [data.cursos, especialidades]);

  const rows = useMemo(() => {
    if (selectedEspecialidadId === null || selectedCursoId === null || selectedSeccion === null) return [];
    return data.alumnos.filter((student) => {
      const course = data.cursos.find((item) => item.id === student.cursoId);
      return course && course.especialidad === especialidades.find((e) => e.id === selectedEspecialidadId)?.nombre && course.id === selectedCursoId && course.seccion === selectedSeccion;
    });
  }, [data.alumnos, data.cursos, especialidades, selectedCursoId, selectedEspecialidadId, selectedSeccion]);

  const openCreate = () => {
    setEditingId(null);
    const preselectedCourse = selectedCursoId ? String(selectedCursoId) : '';
    setForm({ nombre: '', apellido: '', ci: '', cursoId: preselectedCourse, correoEncargado: '', correoEncargado2: '' });
    setIsFormOpen(true);
  };

  const openEdit = (student: AdminCatalog['alumnos'][number]) => {
    const course = data.cursos.find((item) => item.id === student.cursoId);
    setEditingId(student.id);
    setForm({
      nombre: student.nombre,
      apellido: student.apellido,
      ci: String(student.ci ?? ''),
      cursoId: String(student.cursoId),
      correoEncargado: student.correoEncargado ?? '',
      correoEncargado2: student.correoEncargado2 ?? '',
    });
    setSelectedEspecialidadId(especialidades.find((specialty) => specialty.nombre === course?.especialidad)?.id ?? null);
    setSelectedCursoId(student.cursoId);
    setSelectedSeccion(course?.seccion ?? null);
    setStep('tabla');
    setIsFormOpen(true);
  };

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    const payload = {
      nombre: form.nombre.trim(),
      apellido: form.apellido.trim(),
      ci: form.ci ? Number(form.ci) : null,
      cursoId: Number(form.cursoId || selectedCursoId || 0),
      correoEncargado: form.correoEncargado.trim() || null,
      correoEncargado2: form.correoEncargado2.trim() || null,
    };

    if (!payload.nombre || !payload.apellido || !payload.cursoId) {
      status('Nombre, apellido y curso son requeridos.');
      return;
    }

    try {
      if (editingId) {
        await updateAdminRecord('alumnos', editingId, payload);
        status('Alumno actualizado.');
      } else {
        await createAdminRecord('alumnos', payload);
        status('Alumno creado.');
      }
      setIsFormOpen(false);
      await reload();
    } catch (error) {
      status(error instanceof ApiError ? error.message : 'No se pudo guardar el alumno.');
    }
  };

  const goToEspecialidades = () => {
    setStep('especialidades');
    setSelectedEspecialidadId(null);
    setSelectedCursoId(null);
    setSelectedSeccion(null);
  };

  const goToCursos = (specialtyId: number) => {
    setSelectedEspecialidadId(specialtyId);
    setSelectedCursoId(null);
    setSelectedSeccion(null);
    setStep('cursos');
  };

  const goToSecciones = (cursoId: number) => {
    setSelectedCursoId(cursoId);
    setSelectedSeccion(null);
    setStep('secciones');
  };

  const goToTabla = (section: string) => {
    setSelectedSeccion(section);
    setStep('tabla');
  };

  const currentEspecialidad = especialidades.find((item) => item.id === selectedEspecialidadId)?.nombre ?? null;
  const currentCurso = data.cursos.find((item) => item.id === selectedCursoId) ?? null;

  return (
    <>
      {step === 'especialidades' && (
        <div className="card-grid">
          {especialidades.map((specialty) => {
            const totals = data.alumnos.filter((student) => data.cursos.find((course) => course.id === student.cursoId)?.especialidad === specialty.nombre).length;
            return (
              <button type="button" key={specialty.id} className="nav-card" onClick={() => goToCursos(specialty.id)}>
                <span>Especialidad</span>
                <h2>{specialty.nombre}</h2>
                <p>{totals} alumnos</p>
                <strong>Ver cursos →</strong>
              </button>
            );
          })}
        </div>
      )}

      {step === 'cursos' && (
        <>
          <div className="toolbar">
            <button type="button" className="button secondary" onClick={goToEspecialidades}>← Volver a especialidades</button>
          </div>
          <div className="card-grid">
            {(cursosByEspecialidad.get(selectedEspecialidadId ?? -1) ?? []).map((course) => {
              const totals = data.alumnos.filter((student) => student.cursoId === course.id).length;
              return (
                <button type="button" key={course.id} className="nav-card" onClick={() => goToSecciones(course.id)}>
                  <span>{course.nivel}°</span>
                  <h2>{course.seccion}</h2>
                  <p>{totals} alumnos</p>
                  <strong>Ver secciones →</strong>
                </button>
              );
            })}
          </div>
        </>
      )}

      {step === 'secciones' && (
        <>
          <div className="toolbar">
            <button type="button" className="button secondary" onClick={() => setStep('cursos')}>← Volver a cursos</button>
            <button type="button" className="button secondary" onClick={goToEspecialidades}>Inicio</button>
          </div>
          <div className="card-grid">
            {Array.from(new Set((data.cursos.filter((course) => course.id === selectedCursoId).map((course) => course.seccion)))).map((section) => {
              const totals = data.alumnos.filter((student) => {
                const course = data.cursos.find((item) => item.id === student.cursoId);
                return course && course.id === selectedCursoId && course.seccion === section;
              }).length;
              return (
                <button type="button" key={section} className="nav-card" onClick={() => goToTabla(section)}>
                  <span>Sección</span>
                  <h2>{section}</h2>
                  <p>{totals} alumnos</p>
                  <strong>Ver alumnos →</strong>
                </button>
              );
            })}
          </div>
        </>
      )}

      {step === 'tabla' && (
        <>
          <div className="toolbar">
            <button type="button" className="button secondary" onClick={() => setStep('secciones')}>← Volver a secciones</button>
            <button type="button" className="button secondary" onClick={goToEspecialidades}>Inicio</button>
            <button type="button" className="button" onClick={openCreate}>Agregar alumno</button>
          </div>

          <div className="panel">
            <h2>{currentEspecialidad} · {currentCurso?.nivel}° · {selectedSeccion}</h2>
            <div className="table-wrap">
              <table className="grade-table" style={{ minWidth: 760 }}>
                <thead>
                  <tr>
                    <th>Apellido</th>
                    <th>Nombre</th>
                    <th>Cédula</th>
                    <th>Padre/s</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.length === 0 ? (
                    <tr><td colSpan={5}>No hay alumnos en esta sección.</td></tr>
                  ) : (
                    rows.map((student) => (
                      <tr key={student.id}>
                        <td>{student.apellido}</td>
                        <td>{student.nombre}</td>
                        <td>{student.ci ?? '—'}</td>
                        <td>—</td>
                        <td>
                          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                            <button type="button" className="button secondary" onClick={() => openEdit(student)}>Editar</button>
                            <button type="button" className="button danger" onClick={async () => {
                              if (!window.confirm('¿Eliminar este alumno?')) return;
                              try {
                                await deleteAdminRecord('alumnos', student.id);
                                status('Alumno eliminado.');
                                await reload();
                              } catch (error) {
                                status(error instanceof ApiError ? error.message : 'No se pudo eliminar el alumno.');
                              }
                            }}>Eliminar</button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {isFormOpen && (
        <div className="signature-modal" role="dialog" aria-modal="true" aria-labelledby="alumno-form-title" style={{ display: 'grid' }}>
          <div className="signature-modal-header">
            <div>
              <span>Alumnos</span>
              <h2 id="alumno-form-title">{editingId ? 'Editar alumno' : 'Crear alumno'}</h2>
            </div>
            <button type="button" className="signature-modal-close" aria-label="Cerrar formulario" onClick={() => setIsFormOpen(false)}>×</button>
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
            {selectedCursoId ? (
              <label>
                Curso
                <input value={data.cursos.find((course) => course.id === selectedCursoId)?.nivel + '° ' + data.cursos.find((course) => course.id === selectedCursoId)?.seccion + ' · ' + data.cursos.find((course) => course.id === selectedCursoId)?.especialidad || ''} readOnly />
              </label>
            ) : (
              <label>
                Curso
                <select value={form.cursoId} onChange={(event) => setForm({ ...form, cursoId: event.target.value })}>
                  <option value="">Seleccione…</option>
                  {data.cursos.map((course) => (
                    <option key={course.id} value={course.id}>{course.nivel}° {course.seccion} · {course.especialidad}</option>
                  ))}
                </select>
              </label>
            )}
            <label>
              Correo del encargado
              <input type="email" value={form.correoEncargado} onChange={(event) => setForm({ ...form, correoEncargado: event.target.value })} />
            </label>
            <label>
              Segundo correo
              <input type="email" value={form.correoEncargado2} onChange={(event) => setForm({ ...form, correoEncargado2: event.target.value })} />
            </label>

            <div className="signature-modal-actions">
              <button type="button" className="button secondary" onClick={() => setIsFormOpen(false)}>Cancelar</button>
              <button type="submit" className="button" style={{ gridColumn: 'span 2' }}>{editingId ? 'Guardar cambios' : 'Crear alumno'}</button>
            </div>
          </form>
        </div>
      )}
    </>
  );
}
