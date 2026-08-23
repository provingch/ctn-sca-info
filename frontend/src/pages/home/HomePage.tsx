import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { createClass, getHome, updateRasgoCodigos, type HomeResponse } from '../../api/home';
import { ApiError } from '../../api/client';
import AppShell from '../../components/AppShell';
import { getEspecialidades, resolvePlanilla, syncClassroom, type Especialidad } from '../../api/academics';
import { useNavigate } from 'react-router-dom';
import AnimatedSelect from '../../components/AnimatedSelect';
import { useSpecialty } from '../../context/SpecialtyContext';
import PlanCurricularView from './PlanCurricularView';

const HORARIOS_CATEDRA = ['7:00', '7:35', '8:10', '8:45', '9:40', '10:15', '10:50', '11:25', '13:00', '13:35', '14:10', '14:45', '15:20', '16:15', '16:50', '17:25'];
const RASGO_CODIGOS = [
  ['N1', 'Llegada tardia a clase'],
  ['N2', 'Sale de clase sin autorización'],
  ['N3', 'No realiza la tarea asignada en clase'],
  ['N4', 'No dispone de los materiales necesarios'],
  ['N5', 'No presenta la tarea las tareas asignadas para la casa'],
  ['N6', 'Utiliza vocabulario indebido en clase'],
  ['N7', 'Charla mucho en clase'],
  ['N8', 'No utiliza el uniforme establecido'],
  ['N9', 'Ausente en clase, presente en la institución'],
] as const;

export default function HomePage() {
  const [search, setSearch] = useSearchParams();
  const [data, setData] = useState<HomeResponse | null>(null);
  const [error, setError] = useState('');
  const [especialidades, setEspecialidades] = useState<Especialidad[]>([]);
  const view = search.get('view') || '';
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
      const homeView = view === 'catedra' && search.get('subview') === 'clase' ? 'clase' : 'planillas';
      setData(await getHome({ cursoId: cursoId || undefined, etapa, view: homeView }));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Error al cargar el inicio.');
    } finally {
      setSelectionLoading(false);
    }
  }, [cursoId, etapa, view]);

  useEffect(() => {
    void getEspecialidades().then(setEspecialidades).catch(() => setEspecialidades([]));
  }, []);

  // If admin catalog doesn't provide especialidades (e.g. network/auth),
  // derive a fallback list from the cursos returned by getHome so the
  // select always shows options for the user to pick.
  useEffect(() => {
    if (especialidades.length === 0 && data) {
      const names = Array.from(new Set(data.cursos.map((c) => c.especialidad).filter(Boolean)));
      if (names.length > 0) {
        const fallback = names.map((nombre, i) => ({ id: 100000 + i, nombre } as Especialidad));
        setEspecialidades(fallback);
      }
    }
  }, [data, especialidades.length]);

  // Sync application palette with selected especialidad in query
  useEffect(() => {
    const id = Number(search.get('especialidadId') || 0);
    if (id) {
      const s = especialidades.find((item) => item.id === id);
      if (s) selectSpecialty(s.nombre, s.id);
    } else {
      resetSpecialty();
    }
  }, [search, especialidades, selectSpecialty, resetSpecialty]);

  useEffect(() => { void load(); }, [load]);

  useEffect(() => {
    if (!especialidadId) {
      setSelectedNivel(null);
      setSelectedSeccion('');
      return;
    }
  }, [especialidadId]);

  if (!view) return <AppShell title="Elegí cómo querés empezar"><div className="choice-grid"><button onClick={() => setSearch({ view: 'catedra' })}><span>01</span><h2>Libro de Cátedra</h2><p>Plan curricular e inicio de clases.</p></button><button onClick={() => setSearch({ view: 'planillas' })}><span>02</span><h2>Gestionar planillas</h2><p>Tareas, puntajes y sincronización con Classroom.</p></button></div></AppShell>;
  if (!data) return <AppShell title="Panel SCA"><div className="panel">{error || 'Cargando…'}</div></AppShell>;

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
    etapa: String(data.selEtapa),
    especialidadId: String(especialidadId || ''),
    cursoId: String(cursoId || ''),
    ...next,
  });

  const courseOptions = niveles.map((n) => ({ value: n, label: `${n}°` }));
  const sectionOptions = (selectedCourseNivel != null ? seccionesForNivel(selectedCourseNivel) : []).map((s) => ({ value: s, label: String(s) }));

  const showSelectionWait = !hasEspecialidad || !hasCursoSeleccionado || !hasSeccionSeleccionada;

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
    <AppShell title="Panel SCA del curso" specialty={selectedEspecialidad?.nombre ?? null}><div className="toolbar filters"><button className="button secondary" onClick={() => setSearch({})}>← Inicio</button>
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
    </div>
      {selectionLoading ? <section className="panel idle-state"><div className="idle-dots" aria-hidden="true"><span className="idle-dot" /><span className="idle-dot" /><span className="idle-dot" /></div><h2>Cargando planilla…</h2><p>Esperá un momento mientras cargamos la planilla seleccionada.</p></section> : showSelectionWait ? <section className="panel idle-state"><div className="idle-dots" aria-hidden="true"><span className="idle-dot" /><span className="idle-dot" /><span className="idle-dot" /></div><h2>Esperando selección</h2><p>Elegí una especialidad, un curso y una sección para continuar.</p></section> : (
        view === 'catedra' ? (
          !search.get('subview') ? (
            <div className="choice-grid">
              <button onClick={() => params({ subview: 'clase' })}><span>01</span><h2>Iniciar clase</h2><p>Asistencia, rasgos e historial del curso.</p></button>
              <button onClick={() => params({ subview: 'plan-curricular' })}><span>02</span><h2>Plan curricular</h2><p>Cargá y revisá tu plan curricular anual.</p></button>
            </div>
          ) : search.get('subview') === 'plan-curricular' ? (
            <PlanCurricularView data={data} reload={load} />
          ) : (
            <ClassView data={data} reload={load} />
          )
        ) : (
          <PlanillasView data={data} syncingProp={syncingAll} setSyncingProp={setSyncingAll} />
        )
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
  }, [data.googleClassroomConnected, data.planillas, setSyncingProp]);

  const syncing = syncingProp ?? false;

  return <><section className="summary-grid"><article className="metric"><span>Curso</span><strong>{data.selCurso?.curso}° {data.selCurso?.seccion}</strong></article><article className="metric"><span>Planillas</span><strong>{data.planillas.length}</strong></article><article className="metric"><span>Classroom</span><strong>{syncing ? 'Sincronizando…' : (data.googleClassroomConnected ? 'Conectado' : 'Sin conexión')}</strong></article></section><div className="card-grid">{data.planillas.map((p) => <Link className="nav-card" key={p.id} to={`/planilla/${p.id}`}><span>{p.periodo}</span><h2>{p.nombre}</h2><p>{p.tareasCount} tareas registradas</p><strong>Abrir planilla →</strong></Link>)}{data.materiasDetectadas.filter((m) => !existingMateriaIds.has(m.id)).map((m) => <button className="nav-card add-card" key={m.id} onClick={() => openMateria(m.id)}><span>{m.categoria}</span><h2>{m.nombre}</h2><p>Crear la planilla para esta etapa.</p><strong>Crear y abrir →</strong></button>)}{data.planillas.length === 0 && data.materiasDetectadas.length === 0 && <section className="panel empty-state"><h2>Sin materias asignadas</h2><p>Consultá con administración para asociar materias al curso.</p></section>}</div></>;
}

function ClassView({ data, reload }: { data: HomeResponse; reload: () => Promise<void> }) {
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
  const [showCodeHelp, setShowCodeHelp] = useState(false);

  useEffect(() => {
    const initial: Record<number, string[]> = {};
    for (const asistencia of data.rasgoAsistencias) initial[asistencia.alumnoId] = asistencia.codigos ?? [];
    setCodigosPorAlumno(initial);
  }, [data.rasgoAsistencias]);

  useEffect(() => {
    // when course selection changes, fetch available assignments for this professor
    if (!data.selCurso) { setAsignacionesDisponibles([]); setSelectedAsignacionId(null); return; }
    (async () => {
      try {
        const list = await import('../../api/planCurricular').then((m) => m.getAsignacionesDisponibles(data.selCurso!.id));
        setAsignacionesDisponibles(list as any);
        if ((list as any).length === 1) setSelectedAsignacionId((list as any)[0].id);
        else setSelectedAsignacionId(null);
      } catch (err) {
        // ignore: leave list empty
        setAsignacionesDisponibles([]);
        setSelectedAsignacionId(null);
      }
    })();
  }, [data.selCurso]);

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

  const handleCantidadHorasInput = (value: string) => {
    const sanitized = value.replace(/\D/g, '').slice(0, 2);
    setCantidadHoras(sanitized);
  };

  // Derivar estado del plan curricular actual
  const asignacionActual = asignacionesDisponibles.find((a) => a.id === selectedAsignacionId)
    ?? (asignacionesDisponibles.length === 1 ? asignacionesDisponibles[0] : undefined);
  const estadoPlanActual = asignacionActual?.estadoPlan;
  const planAprobado = estadoPlanActual === 'APROBADO';
  const puedeIniciarClase = asignacionesDisponibles.length > 0 && planAprobado;

  // Mensaje a mostrar cuando el plan no está aprobado
  let mensajeBloqueo = '';
  if (asignacionesDisponibles.length === 0) {
    mensajeBloqueo = 'No hay asignaciones disponibles para este curso.';
  } else if (asignacionesDisponibles.length > 1 && !selectedAsignacionId) {
    mensajeBloqueo = 'Elegí primero tu asignación.';
  } else if (estadoPlanActual === 'NO_CARGADO' || !estadoPlanActual) {
    mensajeBloqueo = 'Necesitás cargar y que se apruebe tu plan curricular para poder iniciar clases de esta asignación.';
  } else if (estadoPlanActual === 'PENDIENTE') {
    mensajeBloqueo = 'Tu plan curricular está en revisión. Vas a poder iniciar clases cuando se apruebe.';
  } else if (estadoPlanActual === 'RECHAZADO') {
    mensajeBloqueo = 'Tu plan curricular fue rechazado. Corregilo y volvé a subirlo desde Plan curricular.';
  }

  const indiceInicio = HORARIOS_CATEDRA.indexOf(horario);
  const horasCatedra = Number(cantidadHoras);
  const horarioFinal = indiceInicio >= 0 && horasCatedra > 0
    ? HORARIOS_CATEDRA[indiceInicio + horasCatedra] ?? ''
    : '';

  async function create(e: FormEvent) {
    e.preventDefault();
    if (!data.selCurso || !puedeIniciarClase) {
      setStatus(mensajeBloqueo || 'No puedes iniciar clases en este momento.');
      return;
    }
    try {
      await createClass({ cursoId: data.selCurso.id, etapa: data.selEtapa, instrumentoId, turno: 'turno', tema, alumnosAusentes: ausentes, codigosPorAlumno });
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
      {!puedeIniciarClase && mensajeBloqueo && (
        <div className="panel" style={{ gridColumn: '1 / -1', marginBottom: 0 }}>
          <div className="notice error" style={{ marginBottom: 12 }}>
            <p style={{ margin: 0 }}>{mensajeBloqueo}</p>
          </div>
          {(estadoPlanActual === 'NO_CARGADO' || estadoPlanActual === 'PENDIENTE' || estadoPlanActual === 'RECHAZADO') && (
            <button
              type="button"
              className="button secondary"
              onClick={() => {
                const params = new URLSearchParams({ view: 'catedra', subview: 'plan-curricular' });
                window.location.hash = `?${params.toString()}`;
              }}
            >
              Ir a cargar/revisar Plan curricular
            </button>
          )}
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
            <div className="class-field">
              <label>Horario</label>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 10 }}>
                <div className="class-field">
                  <label htmlFor="horarioClase">Inicio de clase</label>
                  <select id="horarioClase" value={horario} onChange={(e) => setHorario(e.target.value)}>
                    <option value="">Seleccione el horario</option>
                    {HORARIOS_CATEDRA.map((hora) => (
                      <option key={hora} value={hora}>{hora}</option>
                    ))}
                  </select>
                </div>
                <div className="class-field">
                  <label htmlFor="horarioFinalClase">Final de la clase</label>
                  <input id="horarioFinalClase" value={horarioFinal} placeholder="Se calcula automáticamente" readOnly />
                </div>
              </div>
            </div>
            <div className="class-field">
              <label htmlFor="cantidadHoras">Cant. horas cátedra</label>
              <input id="cantidadHoras" type="text" inputMode="numeric" maxLength={2} value={cantidadHoras} onChange={(e) => handleCantidadHorasInput(e.target.value)} placeholder="Ej: 18" />
            </div>
            <div className="class-field">
              <label htmlFor="modalidadClase">Modalidad</label>
              <select id="modalidadClase" value={modalidad} onChange={(e) => setModalidad(e.target.value)}>
                <option>Presencial</option>
                <option>Virtual</option>
              </select>
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
          <h3>Plantilla de plan curricular</h3>
          <p>Descargá la plantilla ya completada con los datos de tu asignación para completar los temas por mes.</p>
          {asignacionesDisponibles.length === 0 ? <p>No hay asignaciones disponibles para este curso.</p> : (
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              {asignacionesDisponibles.length > 1 && (
                <select value={selectedAsignacionId ?? ''} onChange={(e) => setSelectedAsignacionId(Number(e.target.value))}>
                  <option value="">Seleccione asignación…</option>
                  {asignacionesDisponibles.map((a) => <option key={a.id} value={a.id}>{a.materiaNombre ?? `Asignación ${a.id}`}</option>)}
                </select>
              )}
              <button type="button" className="button" disabled={!selectedAsignacionId} onClick={async () => {
                if (!selectedAsignacionId) return;
                try {
                  await import('../../api/planCurricular').then((m) => m.downloadPlantilla(selectedAsignacionId));
                } catch (err) {
                  setStatus(err instanceof ApiError ? err.message : 'No se pudo descargar la plantilla.');
                }
              }}>Descargar plantilla de mi plan curricular</button>
            </div>
          )}
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
              <thead>
                <tr>
                  <th>#</th>
                  <th>Apellido(s) y nombre(s)</th>
                  <th style={{ textAlign: 'right', width: 140 }}>Estado (P/A)</th>
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
                      <select multiple size={2} aria-label={`Códigos de ${alumno.nombre} ${alumno.apellido}`} value={codigosPorAlumno[alumno.id] ?? []} onChange={(e) => void changeCodigos(alumno.id, Array.from(e.target.selectedOptions, (option) => option.value))} style={{ display: 'block', width: 88, marginTop: 6, fontSize: 12 }}>
                        {RASGO_CODIGOS.map(([codigo]) => <option key={codigo} value={codigo}>{codigo}</option>)}
                      </select>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <button type="button" className="button secondary" onClick={() => setShowCodeHelp(true)}>¿Qué significa cada código?</button>
        </div>

        {showCodeHelp && <div role="dialog" aria-modal="true" aria-label="Significado de códigos" style={{ position: 'fixed', inset: 0, zIndex: 100, display: 'grid', placeItems: 'center', padding: 20, background: 'rgba(0, 0, 0, .55)' }} onClick={() => setShowCodeHelp(false)}>
          <section className="panel" style={{ width: 'min(620px, 100%)', maxHeight: '80vh', overflow: 'auto' }} onClick={(e) => e.stopPropagation()}>
            <div className="class-card-head"><h3>Significado de códigos</h3><button type="button" className="button secondary" onClick={() => setShowCodeHelp(false)}>Cerrar</button></div>
            <table className="table table-striped"><thead><tr><th>Código</th><th>Significado</th></tr></thead><tbody>{RASGO_CODIGOS.map(([codigo, significado]) => <tr key={codigo}><td><strong>{codigo}</strong></td><td>{significado}</td></tr>)}</tbody></table>
          </section>
        </div>}

        <div className="class-card">
          <h3>Reportes de asistencia</h3>
          <div className="class-grid" style={{ gridTemplateColumns: '220px minmax(0,1fr)' }}>
            <button type="button" className="button secondary" onClick={() => {
              const aus = ausentes.length;
              const total = data.rasgoAlumnosValidos.length;
              const presentes = Math.max(total - aus, 0);
              const porcentaje = total > 0 ? Math.round((presentes * 100) / total) : 0;
              setStatus(`Total alumnos: ${total} | Presentes: ${presentes} | Ausentes: ${aus} | Asistencia: ${porcentaje}%`);
            }}>Generar reporte de asistencia</button>
            <div id="reportBox" className="empty-state empty-state-card" style={{ textAlign: 'left' }}>{status || 'Aún no hay resumen de asistencia.'}</div>
          </div>
        </div>

        <div className="class-card" style={{ display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
          <button type="submit" className="button" disabled={!puedeIniciarClase}>Guardar inicio de clase</button>
          <button type="button" className="button secondary" onClick={() => {
            const payload = {
              cursoId: data.selCurso ? Number(data.selCurso.id) : 0,
              instrumentoId: instrumentoId,
              tema,
              horarioClase: horario,
              horarioFinalClase: horarioFinal,
              cantidadHoras,
              modalidad,
              observaciones,
              alumnosAusentes: ausentes
            };
            // mostrar JSON temporal en consola (equivalente al botón export de JSP)
            // el backend actual solo usa la estructura mínima enviada por createClass
            console.log('Payload Clase:', payload);
            alert(JSON.stringify(payload, null, 2));
          }}>Ver datos JSON generados</button>
        </div>

        <pre id="resultOutput" className="result-output" style={{ display: 'none' }} />
      </form>

      {/* Right-side attendance & history panel removed as requested */}
    </div>
  );
}
