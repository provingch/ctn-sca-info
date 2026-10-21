import { useCallback, useEffect, useMemo, useState } from 'react';
import { ApiError } from '../../api/client';
import { buscarPadres, createAdminRecord, deleteAdminRecord, getPadresDeAlumno, getResumenAlumno, linkPadreAlumno, unlinkPadreAlumno, updateAdminRecord, type AdminCatalog, type PadreSummary } from '../../api/admin';
import { normalizeSpecialty } from '../../theme/theme';
import SpecialtyIcon from '../../components/SpecialtyIcon';
import AnimatedSelect from '../../components/AnimatedSelect';
import useAccessibleDialog from '../../hooks/useAccessibleDialog';
import FiltersToolbar, { FilterField } from '../../components/ui/FiltersToolbar';
import { filtrarAlumnos, filtrarEgresados, hayFiltroEgresados, SIN_FILTRO_EGRESADOS, type FiltroEgresados } from './filtrosListados';
import { confirmar } from '../../utils/confirmar';
import { etiquetaCurso, mensajeBorrado, mensajeBorradoLote, mensajeTraslado, mensajeTrasladoLote } from './alumnoConfirmaciones';
import useSeleccion from '../../hooks/useSeleccion';
import { BarraSeleccion, CasillaFila, CasillaTodos } from '../../components/SeleccionTabla';
import AlumnoFicha from './AlumnoFicha';

interface AlumnosPanelProps {
  data: AdminCatalog;
  reload: () => Promise<void>;
  status: (message: string) => void;
}

type ViewStep = 'especialidades' | 'cursos' | 'secciones' | 'tabla' | 'egresados';

export default function AlumnosPanel({ data, reload, status }: AlumnosPanelProps) {
  const [step, setStep] = useState<ViewStep>('especialidades');
  const [busquedaAlumnos, setBusquedaAlumnos] = useState('');
  const [filtroEgresados, setFiltroEgresados] = useState<FiltroEgresados>(SIN_FILTRO_EGRESADOS);
  const [selectedEspecialidadId, setSelectedEspecialidadId] = useState<number | null>(null);
  const [selectedNivel, setSelectedNivel] = useState<number | null>(null);
  const [selectedSeccion, setSelectedSeccion] = useState<string | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const formDialogRef = useAccessibleDialog(isFormOpen, () => setIsFormOpen(false));
  const [editingId, setEditingId] = useState<number | null>(null);
  const [padres, setPadres] = useState<PadreSummary[]>([]);
  const [padresSearch, setPadresSearch] = useState('');
  const [padresResults, setPadresResults] = useState<PadreSummary[]>([]);
  const [form, setForm] = useState({ nombre: '', apellido: '', ci: '', cursoId: '' });
  const [fichaId, setFichaId] = useState<number | null>(null);
  const [cursoDestino, setCursoDestino] = useState('');
  const [enLote, setEnLote] = useState(false);

  const especialidades = data.especialidades;

  const cursosByEspecialidad = useMemo(() => {
    return new Map(
      especialidades.map((specialty) => {
        const cursos = data.cursosAlumnos.filter((course) => course.especialidad === specialty.nombre);
        return [specialty.id, cursos];
      }),
    );
  }, [data.cursosAlumnos, especialidades]);

  const rows = useMemo(() => {
    if (selectedEspecialidadId === null || selectedNivel === null || selectedSeccion === null) return [];
    return data.alumnos.filter((student) => {
      const course = data.cursosAlumnos.find((item) => item.id === student.cursoId);
      return course && course.especialidad === especialidades.find((e) => e.id === selectedEspecialidadId)?.nombre && course.nivel === selectedNivel && course.seccion === selectedSeccion;
    });
  }, [data.alumnos, data.cursosAlumnos, especialidades, selectedNivel, selectedEspecialidadId, selectedSeccion]);

  const egresados = useMemo(
    () => [...data.egresados].sort((a, b) => (b.promocion ?? 0) - (a.promocion ?? 0) || a.apellido.localeCompare(b.apellido) || a.nombre.localeCompare(b.nombre)),
    [data.egresados],
  );

  const rowsVisibles = useMemo(() => filtrarAlumnos(rows, busquedaAlumnos), [rows, busquedaAlumnos]);
  const seleccion = useSeleccion(useMemo(() => rowsVisibles.map((student) => student.id), [rowsVisibles]));
  const fichaAlumno = fichaId === null ? undefined : data.alumnos.find((student) => student.id === fichaId);
  const egresadosVisibles = useMemo(() => filtrarEgresados(egresados, filtroEgresados), [egresados, filtroEgresados]);
  const especialidadesDeEgresados = useMemo(
    () => [...new Set(egresados.map((item) => item.especialidad).filter((nombre): nombre is string => Boolean(nombre)))].sort((a, b) => a.localeCompare(b, 'es')),
    [egresados]);
  const promocionesDeEgresados = useMemo(
    () => [...new Set(egresados.map((item) => item.promocion).filter((anio): anio is number => anio != null))].sort((a, b) => b - a),
    [egresados]);
  // Al cambiar de listado (otra sección, egresados, volver) los filtros arrancan limpios.
  useEffect(() => {
    setBusquedaAlumnos('');
    setFiltroEgresados(SIN_FILTRO_EGRESADOS);
  }, [step, selectedSeccion, selectedNivel, selectedEspecialidadId]);

  const openCreate = () => {
    setEditingId(null);
    const preselectedCourse = currentCurso ? String(currentCurso.id) : '';
    setForm({ nombre: '', apellido: '', ci: '', cursoId: preselectedCourse });
    setPadres([]);
    setPadresSearch('');
    setPadresResults([]);
    setIsFormOpen(true);
  };

  const loadPadres = useCallback(async (alumnoId: number | null) => {
    if (!alumnoId) {
      setPadres([]);
      return;
    }
    try {
      const response = await getPadresDeAlumno(alumnoId);
      setPadres(response);
    } catch (error) {
      setPadres([]);
      status(error instanceof ApiError ? error.message : 'No se pudo cargar los padres del alumno.');
    }
  }, [status]);

  const openEdit = async (student: AdminCatalog['alumnos'][number]) => {
    const course = data.cursosAlumnos.find((item) => item.id === student.cursoId);
    setEditingId(student.id);
    setForm({
      nombre: student.nombre,
      apellido: student.apellido,
      ci: String(student.ci ?? ''),
      cursoId: String(student.cursoId),
    });
    setSelectedEspecialidadId(especialidades.find((specialty) => specialty.nombre === course?.especialidad)?.id ?? null);
    setSelectedNivel(course?.nivel ?? null);
    setSelectedSeccion(course?.seccion ?? null);
    setStep('tabla');
    setPadres([]);
    setPadresSearch('');
    setPadresResults([]);
    setIsFormOpen(true);
    await loadPadres(student.id);
  };

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    const payload = {
      nombre: form.nombre.trim(),
      apellido: form.apellido.trim(),
      ci: form.ci.trim() || null,
      cursoId: Number(form.cursoId || currentCurso?.id || 0),
    };

    if (!payload.nombre || !payload.apellido || !payload.cursoId) {
      status('Nombre, apellido y curso son requeridos.');
      return;
    }

    try {
      const original = editingId ? data.alumnos.find((student) => student.id === editingId) : undefined;
      if (editingId && original && original.cursoId !== payload.cursoId) {
        const resumen = await getResumenAlumno(editingId);
        const mensaje = mensajeTraslado(`${payload.apellido}, ${payload.nombre}`,
          data.cursosAlumnos.find((course) => course.id === original.cursoId),
          data.cursosAlumnos.find((course) => course.id === payload.cursoId),
          resumen);
        if (!(await confirmar(mensaje, 'Mover'))) return;
      }
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

  /** Pide el resumen de cada alumno elegido para armar una sola confirmación con todo lo que implica. */
  const resumenesSeleccion = () => Promise.all(seleccion.ids.map((id) => getResumenAlumno(id)));

  const eliminarSeleccion = async () => {
    setEnLote(true);
    try {
      if (!(await confirmar(mensajeBorradoLote(await resumenesSeleccion()), 'Eliminar'))) return;
      let fallidos = 0;
      for (const id of seleccion.ids) {
        try { await deleteAdminRecord('alumnos', id); } catch { fallidos += 1; }
      }
      const eliminados = seleccion.cantidad - fallidos;
      status(fallidos === 0 ? `${eliminados} ${eliminados === 1 ? 'alumno eliminado' : 'alumnos eliminados'}.` : `Se eliminaron ${eliminados}; ${fallidos} no se pudieron eliminar.`);
      seleccion.limpiar();
      await reload();
    } catch (error) {
      status(error instanceof ApiError ? error.message : 'No se pudieron eliminar los alumnos.');
    } finally {
      setEnLote(false);
    }
  };

  const moverSeleccion = async () => {
    const destino = data.cursosAlumnos.find((course) => String(course.id) === cursoDestino);
    if (!destino) {
      status('Elegí el curso al que vas a mover a los alumnos.');
      return;
    }
    const elegidos = data.alumnos.filter((student) => seleccion.estaSeleccionado(student.id));
    setEnLote(true);
    try {
      if (!(await confirmar(mensajeTrasladoLote(elegidos.length, currentCurso ?? undefined, destino, await resumenesSeleccion()), 'Mover'))) return;
      let fallidos = 0;
      for (const student of elegidos) {
        try {
          await updateAdminRecord('alumnos', student.id, { nombre: student.nombre, apellido: student.apellido, ci: student.ci, cursoId: destino.id });
        } catch { fallidos += 1; }
      }
      const movidos = elegidos.length - fallidos;
      status(fallidos === 0 ? `${movidos} ${movidos === 1 ? 'alumno movido' : 'alumnos movidos'} a ${etiquetaCurso(destino)}.` : `Se movieron ${movidos}; ${fallidos} no se pudieron mover.`);
      seleccion.limpiar();
      setCursoDestino('');
      await reload();
    } catch (error) {
      status(error instanceof ApiError ? error.message : 'No se pudieron mover los alumnos.');
    } finally {
      setEnLote(false);
    }
  };

  const goToEspecialidades = () => {
    setStep('especialidades');
    setSelectedEspecialidadId(null);
    setSelectedNivel(null);
    setSelectedSeccion(null);
  };

  const goToCursos = (specialtyId: number) => {
    setSelectedEspecialidadId(specialtyId);
    setSelectedNivel(null);
    setSelectedSeccion(null);
    setStep('cursos');
  };

  const goToSecciones = (nivel: number) => {
    setSelectedNivel(nivel);
    setSelectedSeccion(null);
    setStep('secciones');
  };

  const goToTabla = (section: string) => {
    setSelectedSeccion(section);
    setStep('tabla');
  };

  const currentEspecialidad = especialidades.find((item) => item.id === selectedEspecialidadId)?.nombre ?? null;
  const currentCurso = data.cursosAlumnos.find((item) => item.especialidad === currentEspecialidad && item.nivel === selectedNivel && item.seccion === selectedSeccion) ?? null;

  useEffect(() => {
    if (!isFormOpen || !editingId) {
      return;
    }
    void loadPadres(editingId);
  }, [editingId, isFormOpen, loadPadres]);

  useEffect(() => {
    const query = padresSearch.trim();
    if (!query) {
      setPadresResults([]);
      return;
    }
    const timeout = window.setTimeout(async () => {
      try {
        const result = await buscarPadres(query);
        setPadresResults(result.filter((candidate) => !padres.some((parent) => parent.id === candidate.id)));
      } catch {
        setPadresResults([]);
      }
    }, 250);
    return () => window.clearTimeout(timeout);
  }, [padresSearch, padres]);

  return (
    <>
      {step === 'especialidades' && (
        <>
          {egresados.length > 0 && (
            <div className="toolbar">
              <button type="button" className="button secondary" onClick={() => setStep('egresados')}>Ver egresados ({egresados.length})</button>
            </div>
          )}
          <div className="card-grid">
            {especialidades.map((specialty) => {
              const totals = data.alumnos.filter((student) => data.cursosAlumnos.find((course) => course.id === student.cursoId)?.especialidad === specialty.nombre).length;
              return (
                <button type="button" key={specialty.id} className="nav-card" data-specialty={normalizeSpecialty(specialty.nombre)} onClick={() => goToCursos(specialty.id)}>
                  <span>Especialidad</span>
                  <h2 className="specialty-card-title"><SpecialtyIcon name={specialty.nombre} />{specialty.nombre}</h2>
                  <p>{totals} alumnos</p>
                  <strong>Ver cursos →</strong>
                </button>
              );
            })}
          </div>
        </>
      )}

      {step === 'egresados' && (
        <>
          <div className="toolbar">
            <button type="button" className="button secondary" onClick={goToEspecialidades}>← Volver</button>
          </div>
          <div className="panel">
            <h2>Egresados</h2>
            <p className="muted-copy">Registro histórico de solo lectura: alumnos cuyo curso ya egresó. No se editan ni se eliminan desde acá.</p>
            {egresados.length > 0 && <FiltersToolbar ariaLabel="Filtros de egresados" mostrando={egresadosVisibles.length} total={egresados.length} unidad="egresados" activo={hayFiltroEgresados(filtroEgresados)} onLimpiar={() => setFiltroEgresados(SIN_FILTRO_EGRESADOS)}>
              <FilterField label="Buscar egresado"><input type="search" placeholder="Apellido, nombre o cédula" value={filtroEgresados.busqueda} onChange={(event) => setFiltroEgresados((actual) => ({ ...actual, busqueda: event.target.value }))} /></FilterField>
              <FilterField label="Especialidad" narrow><AnimatedSelect ariaLabel="Especialidad del egresado" value={filtroEgresados.especialidad} onChange={(especialidad) => setFiltroEgresados((actual) => ({ ...actual, especialidad }))} options={[{ value: '', label: 'Todas las especialidades' }, ...especialidadesDeEgresados.map((nombre) => ({ value: nombre, label: nombre }))]} /></FilterField>
              <FilterField label="Año de egreso" narrow><AnimatedSelect ariaLabel="Año de egreso" value={filtroEgresados.promocion} onChange={(promocion) => setFiltroEgresados((actual) => ({ ...actual, promocion }))} options={[{ value: '', label: 'Todos los años' }, ...promocionesDeEgresados.map((anio) => ({ value: String(anio), label: String(anio) }))]} /></FilterField>
            </FiltersToolbar>}
            <div className="table-wrap">
              <table className="grade-table data-table u-table-md">
                <caption className="visually-hidden">Alumnos egresados</caption>
                <thead>
                  <tr>
                    <th scope="col">Egresado</th>
                    <th scope="col">Cédula</th>
                    <th scope="col">Especialidad</th>
                    <th scope="col">Año de egreso</th>
                  </tr>
                </thead>
                <tbody>
                  {egresados.length === 0 ? (
                    <tr><td colSpan={4}>No hay alumnos egresados.</td></tr>
                  ) : egresadosVisibles.length === 0 ? (
                    <tr><td colSpan={4}>Ningún egresado coincide con los filtros elegidos.</td></tr>
                  ) : (
                    egresadosVisibles.map((student) => (
                      <tr key={student.id}>
                        <th scope="row">{student.apellido}, {student.nombre}</th>
                        <td className="cell-muted">{student.ci ?? '—'}</td>
                        <td>{student.especialidad ?? '—'}</td>
                        <td>{student.promocion ?? '—'}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {step === 'cursos' && (
        <>
          <div className="toolbar">
            <button type="button" className="button secondary" onClick={goToEspecialidades}>← Volver a especialidades</button>
          </div>
          <div className="card-grid">
            {Array.from(new Set((cursosByEspecialidad.get(selectedEspecialidadId ?? -1) ?? []).map((course) => course.nivel))).sort((a, b) => b - a).map((nivel) => {
              const courses = (cursosByEspecialidad.get(selectedEspecialidadId ?? -1) ?? []).filter((course) => course.nivel === nivel);
              const totals = data.alumnos.filter((student) => courses.some((course) => course.id === student.cursoId)).length;
              return (
                <button type="button" key={nivel} className="nav-card" onClick={() => goToSecciones(nivel)}>
                  <span>Curso</span>
                  <h2>{nivel}°</h2>
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
            {Array.from(new Set((data.cursosAlumnos.filter((course) => course.especialidad === currentEspecialidad && course.nivel === selectedNivel).map((course) => course.seccion)))).map((section) => {
              const totals = data.alumnos.filter((student) => {
                const course = data.cursosAlumnos.find((item) => item.id === student.cursoId);
                return course && course.especialidad === currentEspecialidad && course.nivel === selectedNivel && course.seccion === section;
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
            {rows.length > 0 && <FiltersToolbar ariaLabel="Filtros de alumnos" mostrando={rowsVisibles.length} total={rows.length} unidad="alumnos" activo={Boolean(busquedaAlumnos.trim())} onLimpiar={() => setBusquedaAlumnos('')}>
              <FilterField label="Buscar alumno"><input type="search" placeholder="Apellido, nombre o cédula" value={busquedaAlumnos} onChange={(event) => setBusquedaAlumnos(event.target.value)} /></FilterField>
            </FiltersToolbar>}
            <BarraSeleccion cantidad={seleccion.cantidad} unidad={['alumno', 'alumnos']} onLimpiar={seleccion.limpiar} ocupado={enLote}>
              <div className="barra-seleccion-select">
                <AnimatedSelect portal ariaLabel="Curso destino" value={cursoDestino} placeholder="Mover a…" onChange={setCursoDestino}
                  options={data.cursosAlumnos.filter((course) => course.id !== currentCurso?.id).map((course) => ({ value: course.id, label: `${course.nivel}° ${course.seccion} · ${course.especialidad}` }))} />
              </div>
              <button type="button" className="button secondary" onClick={moverSeleccion} disabled={enLote || !cursoDestino}>Mover</button>
              <button type="button" className="button danger" onClick={eliminarSeleccion} disabled={enLote}>Eliminar</button>
            </BarraSeleccion>
            <div className="table-wrap">
              <table className="grade-table data-table u-table-md">
                <caption className="visually-hidden">Alumnos de {currentCurso?.nivel}° {selectedSeccion}</caption>
                <thead>
                  <tr>
                    <CasillaTodos todos={seleccion.todos} algunos={seleccion.algunos} onChange={seleccion.toggleTodos} etiqueta="Seleccionar todos los alumnos visibles" />
                    <th scope="col">Alumno</th>
                    <th scope="col">Cédula</th>
                    <th scope="col" className="col-actions"><span className="visually-hidden">Acciones</span></th>
                  </tr>
                </thead>
                <tbody>
                  {rows.length === 0 ? (
                    <tr><td colSpan={4}>No hay alumnos en esta sección.</td></tr>
                  ) : rowsVisibles.length === 0 ? (
                    <tr><td colSpan={4}>Ningún alumno coincide con la búsqueda.</td></tr>
                  ) : (
                    rowsVisibles.map((student) => (
                      <tr key={student.id} className={seleccion.estaSeleccionado(student.id) ? 'is-selected' : undefined}>
                        <CasillaFila checked={seleccion.estaSeleccionado(student.id)} onChange={() => seleccion.toggle(student.id)} etiqueta={`Seleccionar a ${student.apellido}, ${student.nombre}`} />
                        <th scope="row"><button type="button" className="link-button" onClick={() => setFichaId(student.id)}>{student.apellido}, {student.nombre}</button></th>
                        <td className="cell-muted">{student.ci ?? '—'}</td>
                        <td className="col-actions">
                          <div className="row-actions">
                            <button type="button" className="button secondary" aria-label={`Ver ficha de ${student.apellido}, ${student.nombre}`} onClick={() => setFichaId(student.id)}>Ficha</button>
                            <button type="button" className="button secondary" aria-label={`Editar a ${student.apellido}, ${student.nombre}`} onClick={() => openEdit(student)}>Editar</button>
                            <button type="button" className="button danger" aria-label={`Eliminar a ${student.apellido}, ${student.nombre}`} onClick={async () => {
                              try {
                                const resumen = await getResumenAlumno(student.id);
                                if (!(await confirmar(mensajeBorrado(`${student.apellido}, ${student.nombre}`, resumen), 'Eliminar'))) return;
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

      {fichaAlumno && (
        <AlumnoFicha alumno={fichaAlumno} curso={data.cursosAlumnos.find((course) => course.id === fichaAlumno.cursoId)}
          onClose={() => setFichaId(null)}
          onEditar={() => { setFichaId(null); void openEdit(fichaAlumno); }} />
      )}

      {isFormOpen && (
        <div ref={formDialogRef} className="data-modal" role="dialog" aria-modal="true" aria-labelledby="alumno-form-title" tabIndex={-1}>
          <div className="signature-modal-header">
            <div>
              <span>Alumnos</span>
              <h2 id="alumno-form-title">{editingId ? 'Editar alumno' : 'Crear alumno'}</h2>
            </div>
            <button type="button" className="signature-modal-close" aria-label="Cerrar formulario" data-dialog-initial-focus onClick={() => setIsFormOpen(false)}>×</button>
          </div>
          <form className="form-grid u-align-start" onSubmit={submit}>
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
            {/* Al crear desde una sección el curso queda fijo; al editar se puede cambiar (traslado de sección o especialidad). */}
            {currentCurso && !editingId ? (
              <label>
                Curso
                <input value={`${currentCurso.nivel}° ${currentCurso.seccion} · ${currentCurso.especialidad}`} readOnly />
              </label>
            ) : (
              <label>
                Curso
                <AnimatedSelect
                  ariaLabel="Curso del alumno"
                  value={form.cursoId}
                  placeholder="Seleccione…"
                  onChange={(value) => setForm({ ...form, cursoId: value })}
                  options={data.cursosAlumnos.map((course) => ({ value: course.id, label: `${course.nivel}° ${course.seccion} · ${course.especialidad}` }))}
                />
              </label>
            )}

            {!editingId ? (
              <div className="muted-copy u-span-full u-mt-sm">Guardá el alumno primero para poder vincular padres.</div>
            ) : (
              <>
                <div className="u-span-full">
                  <h3 className="u-mb">Padre/s</h3>
                  <div className="u-row u-mb">
                    <input className="u-grow"
                      value={padresSearch}
                      onChange={(event) => setPadresSearch(event.target.value)}
                      placeholder="Buscar padre por nombre, apellido o usuario"
                    />
                  </div>
                  {padresResults.length > 0 && (
                    <div className="u-stack u-mb">
                      {padresResults.map((parent) => (
                        <div className="linked-item" key={parent.id}>
                          <span>{parent.nombre} {parent.apellido} · {parent.usuario} {parent.ci ? `· CI ${parent.ci}` : ''}</span>
                          <button
                            type="button"
                            className="button secondary"
                            onClick={async () => {
                              try {
                                await linkPadreAlumno(editingId, parent.id);
                                status('Padre vinculado.');
                                await loadPadres(editingId);
                                setPadresSearch('');
                              } catch (error) {
                                status(error instanceof ApiError ? error.message : 'No se pudo vincular el padre.');
                              }
                            }}
                          >Agregar</button>
                        </div>
                      ))}
                    </div>
                  )}
                  {padres.length > 0 ? (
                    <div className="u-stack">
                      {padres.map((parent) => (
                        <div className="linked-item" key={parent.id}>
                          <span>{parent.nombre} {parent.apellido} · {parent.usuario} {parent.ci ? `· CI ${parent.ci}` : ''}</span>
                          <button
                            type="button"
                            className="button danger"
                            onClick={async () => {
                              try {
                                await unlinkPadreAlumno(editingId, parent.id);
                                status('Padre desvinculado.');
                                await loadPadres(editingId);
                              } catch (error) {
                                status(error instanceof ApiError ? error.message : 'No se pudo desvincular el padre.');
                              }
                            }}
                          >Quitar</button>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="muted-copy">No hay padres vinculados.</p>
                  )}
                </div>
              </>
            )}

            <div className="signature-modal-actions u-span-full">
              <button type="button" className="button secondary" onClick={() => setIsFormOpen(false)}>Cancelar</button>
              <button type="submit" className="button u-span-2">{editingId ? 'Guardar cambios' : 'Crear alumno'}</button>
            </div>
          </form>
        </div>
      )}
    </>
  );
}
