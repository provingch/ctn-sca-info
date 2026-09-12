import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { createClass, crearCodigoConducta, desactivarCodigoConducta, getClaseActual, getHome, listarCodigosConducta, updateRasgoCodigos, type ClaseActualDto, type CodigoConducta, type HomeResponse } from '../../api/home';
import { ApiError } from '../../api/client';
import AppShell from '../../components/AppShell';
import ContentState from '../../components/ui/ContentState';
import { getEspecialidades, resolvePlanilla, syncClassroom, type Especialidad } from '../../api/academics';
import { useNavigate } from 'react-router-dom';
import AnimatedSelect from '../../components/AnimatedSelect';
import { useSpecialty } from '../../context/SpecialtyContext';
import PlanCurricularView from './PlanCurricularView';
import useAccessibleDialog from '../../hooks/useAccessibleDialog';
import { useAuth } from '../../context/AuthContext';

const normalizeSpecialtyName = (value: string) => value
  .trim()
  .toLowerCase()
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .replace(/[-_]+/g, ' ')
  .replace(/\s+/g, ' ')
  .trim();

const HORARIOS_CATEDRA = ['7:00', '7:35', '8:10', '8:45', '9:40', '10:15', '10:50', '11:25', '13:00', '13:35', '14:10', '14:45', '15:40', '16:15', '16:50', '17:25'];
export default function HomePage() {
  const [search, setSearch] = useSearchParams();
  const [data, setData] = useState<HomeResponse | null>(null);
  const [error, setError] = useState('');
  const [especialidades, setEspecialidades] = useState<Especialidad[]>([]);
  const view = search.get('view') || '';
  const subview = search.get('subview') || '';
  const cursoId = Number(search.get('cursoId') || 0);
  const etapa = Number(search.get('etapa') || 1);
  const especialidadId = Number(search.get('especialidadId') || 0);
  const selectedEspecialidad = especialidades.find((item) => item.id === especialidadId);
  const [selectedNivel, setSelectedNivel] = useState<number | null>(null);
  const [selectedSeccion, setSelectedSeccion] = useState<string | number | ''>('');
  const [selectionLoading, setSelectionLoading] = useState(false);
  const [syncingAll, setSyncingAll] = useState(false);
  const { selectSpecialty, resetSpecialty } = useSpecialty();

  const hasEspecialidad = !!especialidadId;
  const hasCursoSeleccionado = !!cursoId;
  const selectedCourseNivel = selectedNivel;
  const hasSeccionSeleccionada = !!selectedSeccion;

  const setCourseSelection = (value: string | number) => {
    const nextNivel = Number(value) || null;
    setSelectionLoading(true);
    setSelectedNivel(nextNivel);
    setSelectedSeccion('');
    params({ cursoId: '' });
  };

  const load = useCallback(async () => {
    try {
      const homeView = view === 'catedra' && subview === 'clase' ? 'clase' : 'planillas';
      setData(await getHome({ cursoId: cursoId || undefined, etapa, view: homeView }));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Error al cargar el inicio.');
    } finally {
      setSelectionLoading(false);
    }
  }, [cursoId, etapa, subview, view]);

  useEffect(() => {
    let active = true;

    void getEspecialidades()
      .then((catalog) => {
        if (!active) return;

        const scopedNames = Array.from(new Set((data?.cursos ?? []).map((curso) => curso.especialidad).filter((nombre): nombre is string => !!nombre && nombre.trim().length > 0)));

        // Always derive the combo from the specialties present in `data.cursos` when available.
        // Cross-match by normalized name against the official catalog to preserve real ids
        // and only create synthetic entries when no match exists for a given name.
        if (scopedNames.length > 0) {
          const catalogByName = new Map(catalog.map((item) => [normalizeSpecialtyName(item.nombre), item]));
          const derived = scopedNames
            .map((nombre, idx) => {
              const normalized = normalizeSpecialtyName(nombre);
              const match = catalogByName.get(normalized);
              return { id: match?.id ?? (Number.MIN_SAFE_INTEGER + idx), nombre } as Especialidad;
            })
            .filter((item, index, items) => items.findIndex((candidate) => normalizeSpecialtyName(candidate.nombre) === normalizeSpecialtyName(item.nombre)) === index);

          setEspecialidades(derived);
          return;
        }

        // If there are no specialties derived from courses, fall back to full catalog.
        setEspecialidades(catalog);
      })
      .catch(() => {
        if (!active) return;
        const scopedNames = Array.from(new Set((data?.cursos ?? []).map((curso) => curso.especialidad).filter((nombre): nombre is string => !!nombre && nombre.trim().length > 0)));
        setEspecialidades(scopedNames.length > 0
          ? scopedNames.map((nombre, index) => ({ id: 100000 + index, nombre } as Especialidad))
          : []);
      });

    return () => {
      active = false;
    };
  }, [data]);

  // Sync application palette with selected especialidad in query
  useEffect(() => {
    const id = Number(search.get('especialidadId') || 0);

    if (!id && especialidades.length === 1) {
      setSearch((prev) => {
        const next = new URLSearchParams(prev);
        next.set('especialidadId', String(especialidades[0].id));
        return next;
      });
      return;
    }

    if (id) {
      const s = especialidades.find((item) => item.id === id);
      if (s) selectSpecialty(s.nombre, s.id);
    } else {
      resetSpecialty();
    }
  }, [search, especialidades, selectSpecialty, resetSpecialty, setSearch]);

  useEffect(() => { void load(); }, [load]);

  useEffect(() => {
    if (!especialidadId) {
      setSelectedNivel(null);
      setSelectedSeccion('');
      return;
    }
  }, [especialidadId]);

  if (!view) return <AppShell title="Elegí cómo querés empezar"><div className="choice-grid"><button type="button" onClick={() => setSearch({ view: 'catedra' })}><span>01</span><h2>Libro de Cátedra</h2><p>Plan curricular e inicio de clases.</p></button><button type="button" onClick={() => setSearch({ view: 'planillas' })}><span>02</span><h2>Gestionar planillas</h2><p>Tareas, puntajes y sincronización con Classroom.</p></button></div></AppShell>;
  if (!data) return <AppShell title="Panel SCA"><ContentState tone={error ? 'error' : 'loading'} title={error || 'Cargando inicio…'} detail={error ? 'Recargá la página para volver a intentarlo.' : 'Estamos preparando tus cursos y planillas.'} /></AppShell>;

  const visibleCursos = selectedEspecialidad
    ? data.cursos.filter((curso) => curso.especialidad === selectedEspecialidad.nombre)
    : data.cursos;

  // const selectedCourseId = cursoId || data.selCurso?.id;

  // derive unique niveles and secciones for selectors
  const nivelesBase = Array.from(new Set(visibleCursos.map((c) => Number(c.curso)).filter((n) => !isNaN(n) && n > 0)));
  const niveles = nivelesBase.sort((a, b) => a - b);
  const seccionesForNivel = (nivel: number) => Array.from(new Set(visibleCursos.filter((c) => Number(c.curso) === nivel).map((c) => c.seccion))).sort();

  

  const params = (next: Record<string, string>) => setSearch({
    view,
    subview,
    etapa: String(data.selEtapa),
    especialidadId: String(especialidadId || ''),
    cursoId: String(cursoId || ''),
    ...next,
  });

  const courseOptions = niveles.map((n) => ({ value: n, label: `${n}°` }));
  const sectionOptions = (selectedCourseNivel != null ? seccionesForNivel(selectedCourseNivel) : []).map((s) => ({ value: s, label: String(s) }));

  const showSelectionWait = !hasEspecialidad || !hasCursoSeleccionado || !hasSeccionSeleccionada;
  const showSelector = view === 'planillas' || (view === 'catedra' && subview === 'clase');

  return <>
    <style>{`
      @keyframes idlePulse {
        0%, 100% { opacity: 0.35; transform: scale(0.96); }
        50% { opacity: 1; transform: scale(1); }
      }
      .idle-state {
        display: grid;
        gap: 1rem;
        text-align: center;
        padding: 2rem 1.5rem;
        border: 1px dashed #d0d7e2;
        background: linear-gradient(135deg, rgba(148, 163, 184, 0.08), rgba(59, 130, 246, 0.04));
      }
      .idle-dots {
        display: flex;
        justify-content: center;
        gap: 0.5rem;
      }
      .idle-dot {
        width: 0.7rem;
        height: 0.7rem;
        border-radius: 999px;
        background: #3b82f6;
        animation: idlePulse 1.2s ease-in-out infinite;
      }
      .idle-dot:nth-child(2) { animation-delay: 0.15s; }
      .idle-dot:nth-child(3) { animation-delay: 0.3s; }
    `}</style>
    <AppShell title="Panel SCA del curso" specialty={selectedEspecialidad?.nombre ?? null}><div className="toolbar filters"><button type="button" className="button secondary" onClick={() => setSearch({})}>← Inicio</button>
      {showSelector && <>
        <label className="inline-filter">Especialidad
          <AnimatedSelect ariaLabel="Especialidad" value={especialidadId || ''} onChange={(value) => {
            setSelectedNivel(null);
            setSelectedSeccion('');
            params({ especialidadId: value, cursoId: '' });
            const id = Number(value || 0);
            if (id) {
              const s = especialidades.find((item) => item.id === id);
              if (s) selectSpecialty(s.nombre, s.id);
            } else {
              resetSpecialty();
            }
          }} placeholder="Seleccione la especialidad" options={[{ value: '', label: 'Seleccione la especialidad' }, ...especialidades.map((item) => ({ value: item.id, label: item.nombre }))]} />
        </label>
        <label className="inline-filter">Curso
          <AnimatedSelect ariaLabel="Curso" value={selectedCourseNivel ?? ''} onChange={(value) => {
            setCourseSelection(value);
          }} disabled={!hasEspecialidad || visibleCursos.length === 0} placeholder="Seleccione el curso" options={[{ value: '', label: 'Seleccione el curso' }, ...courseOptions]} />
        </label>
        <label className="inline-filter">Sección
            <AnimatedSelect ariaLabel="Sección" value={selectedSeccion ?? ''} onChange={(value) => {
            setSelectionLoading(true);
            setSelectedSeccion(value);
            const nivel = selectedCourseNivel ?? undefined;
            const seccion = String(value);
            const match = visibleCursos.find((c) => (nivel == null || Number(c.curso) === nivel) && c.seccion === seccion && (!selectedEspecialidad || c.especialidad === selectedEspecialidad.nombre));
            params({ cursoId: match ? String(match.id) : '' });
          }} disabled={!hasEspecialidad || selectedCourseNivel == null || visibleCursos.length === 0} placeholder="Seleccione la sección" options={[{ value: '', label: 'Seleccione la sección' }, ...sectionOptions]} />
        </label>
      </>}
    </div>
      {view === 'catedra' ? (
        !subview ? (
          <div className="choice-grid">
            <button type="button" onClick={() => params({ subview: 'clase' })}><span>01</span><h2>Iniciar clase</h2><p>Asistencia, rasgos e historial del curso.</p></button>
            <button type="button" onClick={() => params({ subview: 'plan-curricular' })}><span>02</span><h2>Plan curricular</h2><p>Cargá y revisá tu plan curricular anual.</p></button>
          </div>
        ) : subview === 'plan-curricular' ? (
          <PlanCurricularView />
        ) : selectionLoading ? (
          <section className="panel idle-state"><div className="idle-dots" aria-hidden="true"><span className="idle-dot" /><span className="idle-dot" /><span className="idle-dot" /></div><h2>Cargando…</h2><p>Esperá un momento mientras preparamos la clase.</p></section>
        ) : showSelectionWait ? (
          <section className="panel idle-state"><div className="idle-dots" aria-hidden="true"><span className="idle-dot" /><span className="idle-dot" /><span className="idle-dot" /></div><h2>Esperando selección</h2><p>Elegí una especialidad, un curso y una sección para continuar.</p></section>
        ) : (
          <ClassView data={data} reload={load} />
        )
      ) : selectionLoading ? (
        <section className="panel idle-state"><div className="idle-dots" aria-hidden="true"><span className="idle-dot" /><span className="idle-dot" /><span className="idle-dot" /></div><h2>Cargando planilla…</h2><p>Esperá un momento mientras cargamos la planilla seleccionada.</p></section>
      ) : showSelectionWait ? (
        <section className="panel idle-state"><div className="idle-dots" aria-hidden="true"><span className="idle-dot" /><span className="idle-dot" /><span className="idle-dot" /></div><h2>Esperando selección</h2><p>Elegí una especialidad, un curso y una sección para continuar.</p></section>
      ) : (
        <PlanillasView data={data} syncingProp={syncingAll} setSyncingProp={setSyncingAll} />
      )}
    </AppShell>
  </>;
}

function PlanillasView({ data, syncingProp, setSyncingProp }: { data: HomeResponse; syncingProp?: boolean; setSyncingProp?: (v: boolean) => void }) {
  const navigate = useNavigate();
  const existingMateriaIds = new Set(data.planillas.map((p) => p.materiaId));
  async function openMateria(materiaId: number) { if (!data.selCurso) return; const result = await resolvePlanilla(data.selCurso.id, materiaId, data.selEtapa); navigate(`/planilla/${result.planillaId}`); }
  // Auto-sync planillas in background when Classroom is connected
  useEffect(() => {
    if (!data.googleClassroomConnected) return;
    let cancelled = false;
    (async () => {
      setSyncingProp?.(true);
      try 
      {
        for (const p of data.planillas) {
          if (cancelled) break;
          try {
            await syncClassroom(p.id);
          } catch (err) {
            // si el backend indica que faltan scopes, redirigir a autorización
            if (err instanceof ApiError && err.status === 428) {
              navigate('/google/authorize');
              break;
            }
            // ignore otros errores por planilla
          }
        }
      } finally {
        if (!cancelled) setSyncingProp?.(false);
      }
    })();
    return () => { cancelled = true; setSyncingProp?.(false); };
  }, [data.googleClassroomConnected, data.planillas, navigate, setSyncingProp]);

  const syncing = syncingProp ?? false;

  return <><section className="summary-grid"><article className="metric"><span>Curso</span><strong>{data.selCurso?.curso}° {data.selCurso?.seccion}</strong></article><article className="metric"><span>Planillas</span><strong>{data.planillas.length}</strong></article><article className="metric"><span>Classroom</span><strong>{syncing ? 'Sincronizando…' : (data.googleClassroomConnected ? 'Conectado' : 'Sin conexión')}</strong></article></section><div className="card-grid">{data.planillas.map((p) => <Link className="nav-card" key={p.id} to={`/planilla/${p.id}`}><span>{p.periodo}</span><h2>{p.nombre}</h2><p>{p.tareasCount} tareas registradas</p><strong>Abrir planilla →</strong></Link>)}{data.materiasDetectadas.filter((m) => !existingMateriaIds.has(m.id)).map((m) => <button type="button" className="nav-card add-card" key={m.id} onClick={() => openMateria(m.id)}><span>{m.categoria}</span><h2>{m.nombre}</h2><p>Crear la planilla para esta etapa.</p><strong>Crear y abrir →</strong></button>)}{data.planillas.length === 0 && data.materiasDetectadas.length === 0 && <section className="panel empty-state"><h2>Sin materias asignadas</h2><p>Consultá con administración para asociar materias al curso.</p></section>}</div></>;
}

function ClassView({ data, reload }: { data: HomeResponse; reload: () => Promise<void> }) {
  const [, setSearch] = useSearchParams();
  const { user } = useAuth();
  const [tema, setTema] = useState('');
  const [asignacionesDisponibles, setAsignacionesDisponibles] = useState<Array<{ id: number; materiaId: number; materiaNombre?: string; estadoPlan?: string }>>([]);
  const [selectedAsignacionId, setSelectedAsignacionId] = useState<number | null>(null);
  const [disciplina, setDisciplina] = useState('');
  const [instrumentoId, setInstrumentoId] = useState(0);
  const [ausentes, setAusentes] = useState<number[]>([]);
  const [status, setStatus] = useState('');
  const [horario, setHorario] = useState('');
  const [cantidadHoras, setCantidadHoras] = useState('');
  const [modalidad, setModalidad] = useState('Presencial');
  const [observaciones, setObservaciones] = useState('');
  const [codigosPorAlumno, setCodigosPorAlumno] = useState<Record<number, string[]>>({});
  const [selectorRasgosAbierto, setSelectorRasgosAbierto] = useState<number | null>(null);
  const [showCodeHelp, setShowCodeHelp] = useState(false);
  const [codigosConducta, setCodigosConducta] = useState<CodigoConducta[]>([]);
  const [nuevoCodigo, setNuevoCodigo] = useState('');
  const [nuevaDescripcion, setNuevaDescripcion] = useState('');
  const [catalogStatus, setCatalogStatus] = useState('');
  const codeHelpDialogRef = useAccessibleDialog(showCodeHelp, () => setShowCodeHelp(false));
  const canManageCodes = user?.level === 2 || user?.level === 3;

  const loadCodigosConducta = useCallback(async () => {
    try {
      setCodigosConducta(await listarCodigosConducta());
    } catch (err) {
      setCatalogStatus(err instanceof ApiError ? err.message : 'No se pudo cargar el catálogo de conducta.');
    }
  }, []);

  useEffect(() => { void loadCodigosConducta(); }, [loadCodigosConducta]);

  async function saveCodigoConducta(event: FormEvent) {
    event.preventDefault();
    const codigo = nuevoCodigo.trim().toUpperCase();
    const descripcion = nuevaDescripcion.trim();
    if (!codigo || codigo.length > 10 || !/^N[A-Z0-9]{0,9}$/.test(codigo)) {
      setCatalogStatus('El código debe comenzar con N, tener entre 2 y 10 caracteres y usar solo letras o números.');
      return;
    }
    if (!descripcion || descripcion.length > 255) {
      setCatalogStatus('La descripción es obligatoria y no puede superar 255 caracteres.');
      return;
    }
    try {
      await crearCodigoConducta(codigo, descripcion);
      setNuevoCodigo('');
      setNuevaDescripcion('');
      setCatalogStatus('Código de conducta guardado.');
      await loadCodigosConducta();
    } catch (err) {
      setCatalogStatus(err instanceof ApiError ? err.message : 'No se pudo guardar el código de conducta.');
    }
  }

  async function disableCodigoConducta(item: CodigoConducta) {
    if (!window.confirm(`¿Desactivar ${item.codigo}?`)) return;
    try {
      await desactivarCodigoConducta(item.id);
      setCatalogStatus('Código de conducta desactivado.');
      await loadCodigosConducta();
    } catch (err) {
      setCatalogStatus(err instanceof ApiError ? err.message : 'No se pudo desactivar el código de conducta.');
    }
  }

  useEffect(() => {
    const initial: Record<number, string[]> = {};
    for (const asistencia of data.rasgoAsistencias) initial[asistencia.alumnoId] = asistencia.codigos ?? [];
    setCodigosPorAlumno(initial);
  }, [data.rasgoAsistencias]);

  const [claseActual, setClaseActual] = useState<ClaseActualDto | null>(null);
  const [autoTemaAplicado, setAutoTemaAplicado] = useState(false);

  useEffect(() => {
    // when course selection changes, fetch available assignments for this professor
    if (!data.selCurso) { setAsignacionesDisponibles([]); setSelectedAsignacionId(null); return; }
    (async () => {
      try {
        const list = await import('../../api/planCurricular').then((m) => m.getAsignacionesDisponibles(data.selCurso!.id));
        setAsignacionesDisponibles(list as any);
        if ((list as any).length === 1) setSelectedAsignacionId((list as any)[0].id);
        else setSelectedAsignacionId(null);
      } catch {
        // ignore: leave list empty
        setAsignacionesDisponibles([]);
        setSelectedAsignacionId(null);
      }
    })();
  }, [data.selCurso]);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const info = await getClaseActual();
        if (active) setClaseActual(info);
      } catch {
        if (active) setClaseActual(null);
      }
    })();
    return () => { active = false; };
  }, []);

  useEffect(() => {
    if (!claseActual?.hasClaseAhora || !data.selCurso) return;
    if (claseActual.cursoId !== data.selCurso.id) return;
    if (claseActual.asignacionId && asignacionesDisponibles.some((a) => a.id === claseActual.asignacionId)) {
      setSelectedAsignacionId((prev) => prev ?? claseActual.asignacionId!);
    }
    if (!autoTemaAplicado && claseActual.temaSugerido && !tema) {
      setTema(claseActual.temaSugerido);
      setAutoTemaAplicado(true);
    }
  }, [claseActual, data.selCurso, asignacionesDisponibles, tema, autoTemaAplicado]);

  async function changeCodigos(alumnoId: number, codigos: string[]) {
    setCodigosPorAlumno((current) => ({ ...current, [alumnoId]: codigos }));
    const asistencia = data.rasgoAsistencias.find((item) => item.alumnoId === alumnoId);
    if (asistencia) {
      try {
        await updateRasgoCodigos(asistencia.id, codigos);
      } catch (err) {
        setStatus(err instanceof ApiError ? err.message : 'No se pudieron guardar los códigos.');
      }
    }
  }

  function toggleCodigo(alumnoId: number, codigo: string) {
    const codigosActuales = codigosPorAlumno[alumnoId] ?? [];
    const codigosActualizados = codigosActuales.includes(codigo)
      ? codigosActuales.filter((item) => item !== codigo)
      : [...codigosActuales, codigo];
    void changeCodigos(alumnoId, codigosActualizados);
  }

  const handleCantidadHorasInput = (value: string) => {
    const sanitized = value.replace(/\D/g, '').slice(0, 2);
    setCantidadHoras(sanitized);
  };

  // Derivar estado del plan curricular actual
  const asignacionActual = asignacionesDisponibles.find((a) => a.id === selectedAsignacionId)
    ?? (asignacionesDisponibles.length === 1 ? asignacionesDisponibles[0] : undefined);
  const estadoPlanActual = asignacionActual?.estadoPlan;
  const puedeIniciarClase = asignacionesDisponibles.length > 0 && (!selectedAsignacionId || !!asignacionActual);

  // La aprobación del plan no bloquea el inicio de clase; sólo restringe la selección disponible.
  let mensajeBloqueo = '';
  if (asignacionesDisponibles.length === 0) {
    mensajeBloqueo = 'No hay asignaciones disponibles para este curso.';
  } else if (asignacionesDisponibles.length > 1 && !selectedAsignacionId) {
    mensajeBloqueo = 'Elegí primero tu asignación.';
  }

  const indiceInicio = HORARIOS_CATEDRA.indexOf(horario);
  const horasCatedra = Number(cantidadHoras);
  const horarioFinal = indiceInicio >= 0 && horasCatedra > 0
    ? HORARIOS_CATEDRA[indiceInicio + horasCatedra] ?? ''
    : '';

  const totalAlumnos = data.rasgoAlumnosValidos.length;
  const totalAusentes = data.rasgoAlumnosValidos.filter((alumno) => ausentes.includes(alumno.id)).length;
  const totalPresentes = totalAlumnos - totalAusentes;
  const porcentajeAsistencia = totalAlumnos > 0 ? Math.round((totalPresentes * 100) / totalAlumnos) : 0;
  const porcentajeAusencia = totalAlumnos > 0 ? 100 - porcentajeAsistencia : 0;

  async function create(e: FormEvent) {
    e.preventDefault();
    if (!data.selCurso || !puedeIniciarClase) {
      setStatus(mensajeBloqueo || 'No puedes iniciar clases en este momento.');
      return;
    }
    try {
      await createClass({ cursoId: data.selCurso.id, asignacionId: selectedAsignacionId ?? asignacionActual?.id ?? null, etapa: data.selEtapa, instrumentoId, turno: 'turno', tema, alumnosAusentes: ausentes, codigosPorAlumno });
      setStatus('Clase registrada.');
      setTema('');
      setDisciplina('');
      setHorario('');
      setCantidadHoras('');
      setModalidad('Presencial');
      setInstrumentoId(0);
      setAusentes([]);
      setCodigosPorAlumno({});
      setObservaciones('');
      await reload();
    } catch (err) {
      setStatus(err instanceof ApiError ? err.message : 'No se pudo registrar la clase.');
    }
  }

  function clearForm() {
    setTema('');
    setDisciplina('');
    setHorario('');
    setCantidadHoras('');
    setModalidad('Presencial');
    setInstrumentoId(0);
    setAusentes([]);
    setObservaciones('');
    setStatus('');
  }

  return (
    <div className="two-column">
      {canManageCodes && <section className="panel" style={{ gridColumn: '1 / -1' }}>
        <div className="class-card-head"><div><span>Administración</span><h3>Catálogo de conducta</h3></div></div>
        <form className="form-grid" onSubmit={saveCodigoConducta}>
          <label>Código<input value={nuevoCodigo} maxLength={10} placeholder="Ej.: N10" onChange={(event) => setNuevoCodigo(event.target.value.toUpperCase())} required /></label>
          <label>Descripción<input value={nuevaDescripcion} maxLength={255} placeholder="Descripción del rasgo" onChange={(event) => setNuevaDescripcion(event.target.value)} required /></label>
          <span className="admin-actions"><button className="button" type="submit">Agregar código</button></span>
        </form>
        <div className="admin-list">{codigosConducta.map((item) => <div key={item.id}><span><strong>{item.codigo}</strong> {item.descripcion}</span><button className="button danger" type="button" onClick={() => void disableCodigoConducta(item)}>Desactivar</button></div>)}</div>
        {catalogStatus && <p className="notice" role="status">{catalogStatus}</p>}
      </section>}
      {!puedeIniciarClase && mensajeBloqueo && (
        <div className="panel" style={{ gridColumn: '1 / -1', marginBottom: 0 }}>
          <div className="notice error" style={{ marginBottom: 12 }}>
            <p style={{ margin: 0 }}>{mensajeBloqueo}</p>
          </div>
          {(estadoPlanActual === 'NO_CARGADO' || estadoPlanActual === 'PENDIENTE' || estadoPlanActual === 'RECHAZADO') && (
            <button
              type="button"
              className="button secondary"
              onClick={() => setSearch({ view: 'catedra', subview: 'plan-curricular' })}
            >
              Ir a cargar/revisar Plan curricular
            </button>
          )}
        </div>
      )}
      {claseActual?.hasClaseAhora && (
        <div className="panel" style={{ gridColumn: '1 / -1', marginBottom: 0 }}>
          <div className="notice" role="status" style={{ margin: 0 }}>
            <p style={{ margin: 0 }}>
              <strong>Clase en curso según tu horario:</strong> {claseActual.materia} — {claseActual.cursoDescripcion}
              {claseActual.horaInicio ? ` (${claseActual.horaInicio}${claseActual.horaFin ? '–' + claseActual.horaFin : ''})` : ''}
              {claseActual.temaSugerido ? '. Se autocompletó el tema según tu plan curricular aprobado.' : '.'}
            </p>
          </div>
        </div>
      )}
      <form className="panel" onSubmit={create} style={{ display: 'grid', gap: 12, gridColumn: '1 / -1' }}>
        <input type="hidden" name="action" value="create-rasgo-planilla" />
        <input type="hidden" name="cursoId" value={data.selCurso ? String(data.selCurso.id) : ''} id="formCursoId" />
        <input type="hidden" name="turno" value="turno" id="formTurno" />
        <input type="hidden" name="etapa" value={String(data.selEtapa)} />

        <div className="class-card">
          <div className="class-card-head">
            <h3>Registro de clase</h3>
            <button type="button" className="button secondary" id="clearButton" onClick={clearForm}>Limpiar formulario</button>
          </div>
          <div className="class-grid">
            {asignacionesDisponibles.length > 1 && <div className="class-field class-field--full">
              <label>Asignación</label>
              <AnimatedSelect ariaLabel="Asignación de la clase" value={selectedAsignacionId ?? ''} placeholder="Seleccione asignación…" onChange={(value) => setSelectedAsignacionId(value ? Number(value) : null)} options={asignacionesDisponibles.map((a) => ({ value: a.id, label: a.materiaNombre ?? ('Asignación ' + a.id) }))} />
            </div>}
            <div className="class-field">
              <label>Horario</label>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 10 }}>
                <div className="class-field">
                  <label>Inicio de clase</label>
                  <AnimatedSelect ariaLabel="Inicio de clase" value={horario} placeholder="Seleccione el horario" onChange={setHorario} options={HORARIOS_CATEDRA.map((hora) => ({ value: hora, label: hora }))} />
                </div>
                <div className="class-field">
                  <label htmlFor="horarioFinalClase">Final de la clase</label>
                  <input id="horarioFinalClase" value={horarioFinal} placeholder="Se calcula automáticamente" readOnly />
                </div>
              </div>
            </div>
            <div className="class-field class-field--hours">
              <label htmlFor="cantidadHoras">Cant. horas cátedra</label>
              <input id="cantidadHoras" type="text" inputMode="numeric" maxLength={2} value={cantidadHoras} onChange={(e) => handleCantidadHorasInput(e.target.value)} placeholder="Ej: 18" />
            </div>
            <div className="class-field">
              <label>Modalidad</label>
              <AnimatedSelect ariaLabel="Modalidad de la clase" value={modalidad} onChange={setModalidad} options={[{ value: 'Presencial', label: 'Presencial' }, { value: 'Virtual', label: 'Virtual' }]} />
            </div>
            <div className="class-field">
              <label htmlFor="instrumentoId">Tipo de clase</label>
              <AnimatedSelect ariaLabel="Instrumento" value={instrumentoId} onChange={(value) => setInstrumentoId(Number(value))} options={[{ value: 0, label: 'Sin instrumento' }, ...data.instrumentos.map((item) => ({ value: item.id, label: item.nombre }))]} />
            </div>
            <div className="class-field">
              <label htmlFor="disciplinaClase">Disciplina</label>
              <input id="disciplinaClase" value={disciplina} onChange={(e) => setDisciplina(e.target.value.replace(/[^A-Za-zÁÉÍÓÚáéíóúÑñ\s]/g, '').slice(0, 40))} placeholder="Ej.: Matemática" />
            </div>
            <div className="class-field">
              <label htmlFor="temaRasgo">Contenido específico desarrollado</label>
              <input id="temaRasgo" name="tema" maxLength={150} value={tema} onChange={(e) => setTema(e.target.value)} placeholder="Ej.: Integrales definidas y aplicaciones" required />
            </div>
            <div className="class-field class-field--full">
              <label htmlFor="observacionesGenerales">Observaciones generales</label>
              <textarea id="observacionesGenerales" rows={3} value={observaciones} onChange={(e) => setObservaciones(e.target.value)} placeholder="Cualquier eventualidad general de la clase..." style={{ resize: 'none' }} />
            </div>
          </div>
        </div>

        <div className="class-card">
          <h3>Asistencia general y justificativos</h3>
          <div style={{ marginBottom: 10 }}>
            <span className="student-pill">Habilitados: <strong>{data.rasgoAlumnosValidos.length}</strong></span>
            <span className="student-pill">Incompletos: <strong>{data.rasgoAlumnosInvalidos.length}</strong></span>
          </div>
          <div className="empty-state empty-state-card" style={{ textAlign: 'left', marginBottom: 8 }}>
            Marca ausentes en la lista. Los no marcados se guardan como presentes.
          </div>
          <div className="table-responsive" style={{ marginBottom: 8 }}>
            <table className="table table-striped" id="tablaAsistencia">
              <caption className="visually-hidden">Asistencia y rasgos conductuales de alumnos</caption>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Apellido(s) y nombre(s)</th>
                  <th style={{ textAlign: 'right', width: 140 }}>Estado (P/A)</th>
                  <th style={{ width: 190 }}>Rasgos conductuales</th>
                </tr>
              </thead>
              <tbody>
                {data.rasgoAlumnosValidos.map((alumno, idx) => (
                  <tr key={alumno.id}>
                    <td>{idx + 1}</td>
                    <td>
                      <div>{alumno.apellido}, {alumno.nombre}</div>
                      <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap', marginTop: 4 }}>
                        {(codigosPorAlumno[alumno.id] ?? []).map((codigo) => <span className="student-pill" key={codigo}>{codigo}</span>)}
                      </div>
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <label style={{ display: 'inline-flex', gap: 8, alignItems: 'center' }}>
                        <input className="ausente-checkbox" type="checkbox" value={String(alumno.id)} checked={ausentes.includes(alumno.id)} onChange={(e) => setAusentes((v) => e.target.checked ? [...v, alumno.id] : v.filter((id) => id !== alumno.id))} />
                        Ausente
                      </label>
                    </td>
                    <td className="rasgos-conductuales-cell">
                      <button type="button" className="rasgos-conductuales-trigger" aria-label={`Editar rasgos conductuales de ${alumno.nombre} ${alumno.apellido}`} aria-expanded={selectorRasgosAbierto === alumno.id} onClick={() => setSelectorRasgosAbierto((actual) => actual === alumno.id ? null : alumno.id)}>
                        <span>{(codigosPorAlumno[alumno.id] ?? []).join(', ') || 'seleccione'}</span>
                        <span aria-hidden="true">▾</span>
                      </button>
                      {selectorRasgosAbierto === alumno.id && (
                        <div className="rasgos-conductuales-menu" role="group" aria-label={`Rasgos conductuales de ${alumno.nombre} ${alumno.apellido}`}>
                          {codigosConducta.map((item) => (
                            <label key={item.codigo} className="rasgos-conductuales-option">
                              <input type="checkbox" checked={(codigosPorAlumno[alumno.id] ?? []).includes(item.codigo)} onChange={() => toggleCodigo(alumno.id, item.codigo)} />
                              {item.codigo}
                            </label>
                          ))}
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <button type="button" className="button secondary" onClick={() => setShowCodeHelp(true)}>¿Qué significa cada código?</button>
          <div className="attendance-summary" role="status" aria-live="polite" aria-atomic="true">
            <strong>Resumen de asistencia</strong>
            {totalAlumnos > 0 ? <>
              <p>Presentes: <strong>{totalPresentes} de {totalAlumnos} ({porcentajeAsistencia}%)</strong> · Ausentes: <strong>{totalAusentes} ({porcentajeAusencia}%)</strong></p>
              <small>Se actualiza al marcar ausentes. Incluye solo alumnos habilitados.</small>
            </> : <p>No hay alumnos habilitados para calcular la asistencia.</p>}
          </div>
        </div>

        {showCodeHelp && <div ref={codeHelpDialogRef} role="dialog" aria-modal="true" aria-labelledby="code-help-title" tabIndex={-1} style={{ position: 'fixed', inset: 0, zIndex: 100, display: 'grid', placeItems: 'center', padding: 20, background: 'rgba(0, 0, 0, .55)' }} onClick={() => setShowCodeHelp(false)}>
          <section className="panel" style={{ width: 'min(620px, 100%)', maxHeight: '80vh', overflow: 'auto' }} onClick={(e) => e.stopPropagation()}>
            <div className="class-card-head"><h3 id="code-help-title">Significado de códigos</h3><button type="button" className="button secondary" data-dialog-initial-focus onClick={() => setShowCodeHelp(false)}>Cerrar</button></div>
            <table className="table table-striped"><caption className="visually-hidden">Códigos de rasgos conductuales</caption><thead><tr><th>Código</th><th>Significado</th></tr></thead><tbody>{codigosConducta.map((item) => <tr key={item.codigo}><td><strong>{item.codigo}</strong></td><td>{item.descripcion}</td></tr>)}</tbody></table>
  |          </section>
        </div>}

        {status && <p className="notice" role="status">{status}</p>}

        <div className="class-card" style={{ display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
          <button type="submit" className="button" disabled={!puedeIniciarClase}>Guardar inicio de clase</button>
        </div>

      </form>

      {/* Right-side attendance & history panel removed as requested */}
    </div>
  );
}
