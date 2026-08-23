import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { createClass, getHome, type HomeResponse } from '../../api/home';
import { ApiError } from '../../api/client';
import AppShell from '../../components/AppShell';
import { getEspecialidades, resolvePlanilla, syncClassroom, type Especialidad } from '../../api/academics';
import { useNavigate } from 'react-router-dom';
import AnimatedSelect from '../../components/AnimatedSelect';
import { useSpecialty } from '../../context/SpecialtyContext';

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
      setData(await getHome({ cursoId: cursoId || undefined, etapa, view: view === 'clase' ? 'clase' : 'planillas' }));
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

  if (!view) return <AppShell title="Elegí cómo querés empezar"><div className="choice-grid"><button onClick={() => setSearch({ view: 'clase' })}><span>01</span><h2>Iniciar una clase</h2><p>Asistencia, rasgos e historial del curso.</p></button><button onClick={() => setSearch({ view: 'planillas' })}><span>02</span><h2>Gestionar planillas</h2><p>Tareas, puntajes y sincronización con Classroom.</p></button></div></AppShell>;
  if (!data) return <AppShell title="Panel SCA"><div className="panel">{error || 'Cargando…'}</div></AppShell>;

  const visibleCursos = selectedEspecialidad
    ? data.cursos.filter((curso) => curso.especialidad === selectedEspecialidad.nombre)
    : data.cursos;

  // const selectedCourseId = cursoId || data.selCurso?.id;

  // derive unique niveles and secciones for selectors
  const nivelesBase = Array.from(new Set(visibleCursos.map((c) => Number(c.curso)).filter((n) => !isNaN(n) && n > 0)));
  if (nivelesBase.length > 0 && !nivelesBase.includes(1)) {
    nivelesBase.push(1);
  }
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
      {selectionLoading ? <section className="panel idle-state"><div className="idle-dots" aria-hidden="true"><span className="idle-dot" /><span className="idle-dot" /><span className="idle-dot" /></div><h2>Cargando planilla…</h2><p>Esperá un momento mientras cargamos la planilla seleccionada.</p></section> : showSelectionWait ? <section className="panel idle-state"><div className="idle-dots" aria-hidden="true"><span className="idle-dot" /><span className="idle-dot" /><span className="idle-dot" /></div><h2>Esperando selección</h2><p>Elegí una especialidad, un curso y una sección para continuar.</p></section> : (view === 'clase' ? <ClassView data={data} reload={load} /> : <PlanillasView data={data} syncingProp={syncingAll} setSyncingProp={setSyncingAll} />)}
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
  const [disciplina, setDisciplina] = useState('');
  const [instrumentoId, setInstrumentoId] = useState(0);
  const [ausentes, setAusentes] = useState<number[]>([]);
  const [status, setStatus] = useState('');
  const [horario, setHorario] = useState('');
  const [cantidadHoras, setCantidadHoras] = useState('');
  const [modalidad, setModalidad] = useState('Presencial');
  const [observaciones, setObservaciones] = useState('');

  const handleHorarioInput = (value: string) => {
    const sanitized = value.replace(/[^\d:-]/g, '').slice(0, 11);
    setHorario(sanitized);
  };

  const handleCantidadHorasInput = (value: string) => {
    const sanitized = value.replace(/\D/g, '').slice(0, 2);
    setCantidadHoras(sanitized);
  };

  async function create(e: FormEvent) {
    e.preventDefault();
    if (!data.selCurso) return;
    try {
      await createClass({ cursoId: data.selCurso.id, etapa: data.selEtapa, instrumentoId, turno: 'turno', tema, alumnosAusentes: ausentes });
      setStatus('Clase registrada.');
      setTema('');
      setDisciplina('');
      setHorario('');
      setCantidadHoras('');
      setModalidad('Presencial');
      setInstrumentoId(0);
      setAusentes([]);
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
              <label htmlFor="horarioClase">Horario</label>
              <input id="horarioClase" placeholder="Ej: 07:00-09:20" value={horario} onChange={(e) => handleHorarioInput(e.target.value)} inputMode="numeric" maxLength={11} pattern="^\\d{2}:\\d{2}-\\d{2}:\\d{2}$" />
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
                    <td>{alumno.apellido}, {alumno.nombre}</td>
                    <td style={{ textAlign: 'right' }}>
                      <label style={{ display: 'inline-flex', gap: 8, alignItems: 'center' }}>
                        <input className="ausente-checkbox" type="checkbox" value={String(alumno.id)} checked={ausentes.includes(alumno.id)} onChange={(e) => setAusentes((v) => e.target.checked ? [...v, alumno.id] : v.filter((id) => id !== alumno.id))} />
                        Ausente
                      </label>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

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
          <button type="submit" className="button">Guardar inicio de clase</button>
          <button type="button" className="button secondary" onClick={() => {
            const payload = {
              cursoId: data.selCurso ? Number(data.selCurso.id) : 0,
              instrumentoId: instrumentoId,
              tema,
              horarioClase: horario,
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
