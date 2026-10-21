import { useCallback, useEffect, useMemo, useState } from 'react';
import { ApiError } from '../../api/client';
import { buscarPadres, createAdminRecord, deleteAdminRecord, getPadresDeAlumno, getResumenAlumno, linkPadreAlumno, unlinkPadreAlumno, updateAdminRecord, type AdminCatalog, type PadreSummary } from '../../api/admin';
import AnimatedSelect from '../../components/AnimatedSelect';
import SpecialtyBadge from '../../components/SpecialtyBadge';
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
  /** Alumno a abrir en su ficha al entrar (buscador global del admin). */
  fichaInicial?: number | null;
  onFichaCerrada?: () => void;
}

type Vista = 'activos' | 'egresados';
const POR_PAGINA = 50;

/**
 * Alumnos activos en una sola tabla con filtros (especialidad, curso, sección y búsqueda), en lugar de
 * recorrer tarjetas de especialidad, curso y sección. Los egresados quedan en su propio listado de solo lectura.
 */
export default function AlumnosPanel({ data, reload, status, fichaInicial = null, onFichaCerrada }: AlumnosPanelProps) {
  const [vista, setVista] = useState<Vista>('activos');
  const [busquedaAlumnos, setBusquedaAlumnos] = useState('');
  const [filtroEgresados, setFiltroEgresados] = useState<FiltroEgresados>(SIN_FILTRO_EGRESADOS);
  const [filtroEspecialidad, setFiltroEspecialidad] = useState('');
  const [filtroNivel, setFiltroNivel] = useState('');
  const [filtroSeccion, setFiltroSeccion] = useState('');
  const [pagina, setPagina] = useState(1);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const formDialogRef = useAccessibleDialog(isFormOpen, () => setIsFormOpen(false));
  const [editingId, setEditingId] = useState<number | null>(null);
  const [padres, setPadres] = useState<PadreSummary[]>([]);
  const [padresSearch, setPadresSearch] = useState('');
  const [padresResults, setPadresResults] = useState<PadreSummary[]>([]);
  const [form, setForm] = useState({ nombre: '', apellido: '', ci: '', cursoId: '' });
  const [fichaId, setFichaId] = useState<number | null>(fichaInicial);
  const [cursoDestino, setCursoDestino] = useState('');
  const [enLote, setEnLote] = useState(false);

  useEffect(() => { if (fichaInicial !== null) setFichaId(fichaInicial); }, [fichaInicial]);

  const cursoPorId = useMemo(() => new Map(data.cursosAlumnos.map((course) => [course.id, course])), [data.cursosAlumnos]);
  const especialidades = useMemo(() => [...new Set(data.cursosAlumnos.map((course) => course.especialidad))].sort((a, b) => a.localeCompare(b, 'es')), [data.cursosAlumnos]);
  const niveles = useMemo(() => [...new Set(data.cursosAlumnos.filter((course) => !filtroEspecialidad || course.especialidad === filtroEspecialidad).map((course) => course.nivel))].sort((a, b) => a - b), [data.cursosAlumnos, filtroEspecialidad]);
  const secciones = useMemo(() => [...new Set(data.cursosAlumnos
    .filter((course) => (!filtroEspecialidad || course.especialidad === filtroEspecialidad) && (!filtroNivel || String(course.nivel) === filtroNivel))
    .map((course) => course.seccion))].sort((a, b) => a.localeCompare(b, 'es')), [data.cursosAlumnos, filtroEspecialidad, filtroNivel]);

  const filtrados = useMemo(() => filtrarAlumnos(data.alumnos.filter((student) => {
    const course = cursoPorId.get(student.cursoId);
    if (!course) return !filtroEspecialidad && !filtroNivel && !filtroSeccion;
    return (!filtroEspecialidad || course.especialidad === filtroEspecialidad)
      && (!filtroNivel || String(course.nivel) === filtroNivel)
      && (!filtroSeccion || course.seccion === filtroSeccion);
  }), busquedaAlumnos).sort((a, b) => {
    const ca = cursoPorId.get(a.cursoId);
    const cb = cursoPorId.get(b.cursoId);
    return (ca?.especialidad ?? '').localeCompare(cb?.especialidad ?? '', 'es') || (ca?.nivel ?? 0) - (cb?.nivel ?? 0)
      || (ca?.seccion ?? '').localeCompare(cb?.seccion ?? '', 'es') || a.apellido.localeCompare(b.apellido, 'es') || a.nombre.localeCompare(b.nombre, 'es');
  }), [data.alumnos, cursoPorId, filtroEspecialidad, filtroNivel, filtroSeccion, busquedaAlumnos]);

  const paginas = Math.max(1, Math.ceil(filtrados.length / POR_PAGINA));
  const paginaActual = Math.min(pagina, paginas);
  const rowsVisibles = filtrados.slice((paginaActual - 1) * POR_PAGINA, paginaActual * POR_PAGINA);
  const hayFiltroAlumnos = Boolean(busquedaAlumnos.trim() || filtroEspecialidad || filtroNivel || filtroSeccion);
  const limpiarFiltros = () => { setBusquedaAlumnos(''); setFiltroEspecialidad(''); setFiltroNivel(''); setFiltroSeccion(''); setPagina(1); };

  // Con especialidad, curso y sección elegidos, "Agregar alumno" ya sabe a qué curso va.
  const currentCurso = filtroEspecialidad && filtroNivel && filtroSeccion
    ? data.cursosAlumnos.find((course) => course.especialidad === filtroEspecialidad && String(course.nivel) === filtroNivel && course.seccion === filtroSeccion) ?? null
    : null;

  const seleccion = useSeleccion(useMemo(() => rowsVisibles.map((student) => student.id), [rowsVisibles]));
  const fichaAlumno = fichaId === null ? undefined : data.alumnos.find((student) => student.id === fichaId);

  const egresados = useMemo(
    () => [...data.egresados].sort((a, b) => (b.promocion ?? 0) - (a.promocion ?? 0) || a.apellido.localeCompare(b.apellido) || a.nombre.localeCompare(b.nombre)),
    [data.egresados],
  );
  const egresadosVisibles = useMemo(() => filtrarEgresados(egresados, filtroEgresados), [egresados, filtroEgresados]);
  const especialidadesDeEgresados = useMemo(
    () => [...new Set(egresados.map((item) => item.especialidad).filter((nombre): nombre is string => Boolean(nombre)))].sort((a, b) => a.localeCompare(b, 'es')),
    [egresados]);
  const promocionesDeEgresados = useMemo(
    () => [...new Set(egresados.map((item) => item.promocion).filter((anio): anio is number => anio != null))].sort((a, b) => b - a),
    [egresados]);

  const openCreate = () => {
    setEditingId(null);
    setForm({ nombre: '', apellido: '', ci: '', cursoId: currentCurso ? String(currentCurso.id) : '' });
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
    setEditingId(student.id);
    setForm({
      nombre: student.nombre,
      apellido: student.apellido,
      ci: String(student.ci ?? ''),
      cursoId: String(student.cursoId),
    });
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
        const mensaje = mensajeTraslado(`${payload.apellido}, ${payload.nombre}`, cursoPorId.get(original.cursoId), cursoPorId.get(payload.cursoId), resumen);
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

  const eliminarAlumno = async (student: AdminCatalog['alumnos'][number]) => {
    try {
      const resumen = await getResumenAlumno(student.id);
      if (!(await confirmar(mensajeBorrado(`${student.apellido}, ${student.nombre}`, resumen), 'Eliminar'))) return;
      await deleteAdminRecord('alumnos', student.id);
      status('Alumno eliminado.');
      await reload();
    } catch (error) {
      status(error instanceof ApiError ? error.message : 'No se pudo eliminar el alumno.');
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
    const destino = cursoPorId.get(Number(cursoDestino));
    if (!destino) {
      status('Elegí el curso al que vas a mover a los alumnos.');
      return;
    }
    const elegidos = data.alumnos.filter((student) => seleccion.estaSeleccionado(student.id));
    // Si todos vienen del mismo curso se nombra; si no, el mensaje habla de "sus cursos actuales".
    const origenes = new Set(elegidos.map((student) => student.cursoId));
    const desde = origenes.size === 1 ? cursoPorId.get([...origenes][0]) : undefined;
    setEnLote(true);
    try {
      if (!(await confirmar(mensajeTrasladoLote(elegidos.length, desde, destino, await resumenesSeleccion()), 'Mover'))) return;
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
      {vista === 'egresados' && (
        <>
          <div className="toolbar">
            <button type="button" className="button secondary" onClick={() => setVista('activos')}>← Alumnos activos</button>
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
                        <th scope="row" data-label="Egresado">{student.apellido}, {student.nombre}</th>
                        <td className="cell-muted" data-label="Cédula">{student.ci ?? '—'}</td>
                        <td data-label="Especialidad">{student.especialidad ?? '—'}</td>
                        <td data-label="Año de egreso">{student.promocion ?? '—'}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {vista === 'activos' && (
        <div className="panel">
          <header className="panel-heading-row">
            <div>
              <h2>Alumnos activos</h2>
              <p className="muted-copy">{data.alumnos.length} alumnos en {data.cursosAlumnos.length} cursos. Tocá un nombre para ver su ficha.</p>
            </div>
            <div className="u-row">
              {egresados.length > 0 && <button type="button" className="button secondary" onClick={() => setVista('egresados')}>Egresados ({egresados.length})</button>}
              <button type="button" className="button" onClick={openCreate}>Agregar alumno</button>
            </div>
          </header>
          <FiltersToolbar ariaLabel="Filtros de alumnos" mostrando={filtrados.length} total={data.alumnos.length} unidad="alumnos" activo={hayFiltroAlumnos} onLimpiar={limpiarFiltros}>
            <FilterField label="Buscar alumno"><input type="search" placeholder="Apellido, nombre o cédula" value={busquedaAlumnos} onChange={(event) => { setBusquedaAlumnos(event.target.value); setPagina(1); }} /></FilterField>
            {especialidades.length > 1 && <FilterField label="Especialidad" narrow><AnimatedSelect ariaLabel="Filtrar por especialidad" value={filtroEspecialidad} onChange={(value) => { setFiltroEspecialidad(value); setFiltroNivel(''); setFiltroSeccion(''); setPagina(1); }} options={[{ value: '', label: 'Todas' }, ...especialidades.map((nombre) => ({ value: nombre, label: nombre }))]} /></FilterField>}
            <FilterField label="Curso" narrow><AnimatedSelect ariaLabel="Filtrar por curso" value={filtroNivel} onChange={(value) => { setFiltroNivel(value); setFiltroSeccion(''); setPagina(1); }} options={[{ value: '', label: 'Todos' }, ...niveles.map((nivel) => ({ value: String(nivel), label: `${nivel}°` }))]} /></FilterField>
            <FilterField label="Sección" narrow><AnimatedSelect ariaLabel="Filtrar por sección" value={filtroSeccion} onChange={(value) => { setFiltroSeccion(value); setPagina(1); }} options={[{ value: '', label: 'Todas' }, ...secciones.map((seccion) => ({ value: seccion, label: seccion }))]} /></FilterField>
          </FiltersToolbar>
          <BarraSeleccion cantidad={seleccion.cantidad} unidad={['alumno', 'alumnos']} onLimpiar={seleccion.limpiar} ocupado={enLote}>
            <div className="barra-seleccion-select">
              <AnimatedSelect portal ariaLabel="Curso destino" value={cursoDestino} placeholder="Mover a…" onChange={setCursoDestino}
                options={data.cursosAlumnos.map((course) => ({ value: course.id, label: `${course.nivel}° ${course.seccion} · ${course.especialidad}` }))} />
            </div>
            <button type="button" className="button secondary" onClick={moverSeleccion} disabled={enLote || !cursoDestino}>Mover</button>
            <button type="button" className="button danger" onClick={eliminarSeleccion} disabled={enLote}>Eliminar</button>
          </BarraSeleccion>
          <div className="table-wrap">
            <table className="grade-table data-table u-table-md">
              <caption className="visually-hidden">Alumnos activos{currentCurso ? ` de ${etiquetaCurso(currentCurso)}` : ''}</caption>
              <thead>
                <tr>
                  <CasillaTodos todos={seleccion.todos} algunos={seleccion.algunos} onChange={seleccion.toggleTodos} etiqueta="Seleccionar todos los alumnos de esta página" />
                  <th scope="col">Alumno</th>
                  <th scope="col">Curso</th>
                  <th scope="col">Cédula</th>
                  <th scope="col" className="col-actions"><span className="visually-hidden">Acciones</span></th>
                </tr>
              </thead>
              <tbody>
                {data.alumnos.length === 0 ? (
                  <tr><td colSpan={5}><strong>Todavía no hay alumnos</strong>Agregá el primero con "Agregar alumno".</td></tr>
                ) : rowsVisibles.length === 0 ? (
                  <tr><td colSpan={5}>Ningún alumno coincide con los filtros.</td></tr>
                ) : (
                  rowsVisibles.map((student) => {
                    const course = cursoPorId.get(student.cursoId);
                    return (
                      <tr key={student.id} className={seleccion.estaSeleccionado(student.id) ? 'is-selected' : undefined}>
                        <CasillaFila checked={seleccion.estaSeleccionado(student.id)} onChange={() => seleccion.toggle(student.id)} etiqueta={`Seleccionar a ${student.apellido}, ${student.nombre}`} />
                        <th scope="row"><button type="button" className="link-button" onClick={() => setFichaId(student.id)}>{student.apellido}, {student.nombre}</button></th>
                        <td data-label="Curso">{course ? <span className="cell-curso"><SpecialtyBadge name={course.especialidad} /><span>{course.nivel}° {course.seccion}</span></span> : '—'}</td>
                        <td className="cell-muted" data-label="Cédula">{student.ci ?? '—'}</td>
                        <td className="col-actions">
                          <div className="row-actions">
                            <button type="button" className="button secondary" aria-label={`Ver ficha de ${student.apellido}, ${student.nombre}`} onClick={() => setFichaId(student.id)}>Ficha</button>
                            <button type="button" className="button secondary" aria-label={`Editar a ${student.apellido}, ${student.nombre}`} onClick={() => void openEdit(student)}>Editar</button>
                            <button type="button" className="button danger" aria-label={`Eliminar a ${student.apellido}, ${student.nombre}`} onClick={() => void eliminarAlumno(student)}>Eliminar</button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
          {paginas > 1 && <nav className="table-pagination" aria-label="Páginas de alumnos">
            <span>Mostrando {(paginaActual - 1) * POR_PAGINA + 1}–{Math.min(paginaActual * POR_PAGINA, filtrados.length)} de {filtrados.length}</span>
            <div className="u-row">
              <button type="button" className="button secondary" disabled={paginaActual === 1} onClick={() => setPagina(paginaActual - 1)}>Anterior</button>
              <button type="button" className="button secondary" disabled={paginaActual === paginas} onClick={() => setPagina(paginaActual + 1)}>Siguiente</button>
            </div>
          </nav>}
        </div>
      )}

      {fichaAlumno && (
        <AlumnoFicha alumno={fichaAlumno} curso={cursoPorId.get(fichaAlumno.cursoId)}
          onClose={() => { setFichaId(null); onFichaCerrada?.(); }}
          onEditar={() => { setFichaId(null); onFichaCerrada?.(); void openEdit(fichaAlumno); }} />
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
