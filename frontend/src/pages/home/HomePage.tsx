import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { createClass, getHome, updateAttendance, type HomeResponse } from '../../api/home';
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
  const [syncingAll, setSyncingAll] = useState(false);
  const { selectSpecialty, resetSpecialty } = useSpecialty();

  const load = useCallback(async () => {
    try {
      setData(await getHome({ cursoId: cursoId || undefined, etapa, view: view === 'clase' ? 'clase' : 'planillas' }));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Error al cargar el inicio.');
    }
  }, [cursoId, etapa, view]);

  useEffect(() => {
    void getEspecialidades().then(setEspecialidades).catch(() => setEspecialidades([]));
  }, []);

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

  if (!view) return <AppShell title="Elegí cómo querés empezar"><div className="choice-grid app-frame--fit"><button onClick={() => setSearch({ view: 'clase' })}><span>01</span><h2>Iniciar una clase</h2><p>Asistencia, rasgos e historial del curso.</p></button><button onClick={() => setSearch({ view: 'planillas' })}><span>02</span><h2>Gestionar planillas</h2><p>Tareas, puntajes y sincronización con Classroom.</p></button></div></AppShell>;
  if (!data) return <AppShell title="Panel SCA"><div className="panel">{error || 'Cargando…'}</div></AppShell>;

  const visibleCursos = selectedEspecialidad
    ? data.cursos.filter((curso) => curso.especialidad === selectedEspecialidad.nombre)
    : data.cursos;

  // const selectedCourseId = cursoId || data.selCurso?.id;

  // derive unique niveles and secciones for selectors
  const niveles = Array.from(new Set(visibleCursos.map((c) => Number(c.curso)))).map((n) => Number(n)).filter((n) => !isNaN(n)).sort((a, b) => a - b);
  const seccionesForNivel = (nivel: number) => Array.from(new Set(visibleCursos.filter((c) => Number(c.curso) === nivel).map((c) => c.seccion))).sort();

  const params = (next: Record<string, string>) => setSearch({
    view,
    etapa: String(data.selEtapa),
    especialidadId: String(especialidadId || ''),
    cursoId: String(cursoId || data.selCurso?.id || ''),
    ...next,
  });

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
    <AppShell title="Panel SCA del curso" specialty={data.selCurso?.especialidad}><div className="toolbar filters"><button className="button secondary" onClick={() => setSearch({})}>← Inicio</button>
      <label className="inline-filter">Especialidad
        <AnimatedSelect ariaLabel="Especialidad" value={especialidadId} onChange={(value) => {
          params({ especialidadId: value, cursoId: '' });
          const id = Number(value || 0);
          if (id) {
            const s = especialidades.find((item) => item.id === id);
            if (s) selectSpecialty(s.nombre, s.id);
          } else {
            resetSpecialty();
          }
        }} options={[{ value: 0, label: 'Todas' }, ...especialidades.map((item) => ({ value: item.id, label: item.nombre }))]} />
      </label>
      <label className="inline-filter">Nivel
        <AnimatedSelect ariaLabel="Nivel" value={selectedNivel ?? ''} onChange={(value) => { const v = Number(value) || null; setSelectedNivel(v); setSelectedSeccion(''); params({ cursoId: '' }); }} disabled={visibleCursos.length === 0} placeholder="Nivel" options={[{ value: '', label: 'Todos' }, ...niveles.map((n) => ({ value: n, label: `${n}°` }))]} />
      </label>
      <label className="inline-filter">Sección
          <AnimatedSelect ariaLabel="Sección" value={selectedSeccion ?? ''} onChange={(value) => {
          setSelectedSeccion(value);
          // find matching curso id for current specialty+nivel+seccion
          const nivel = selectedNivel ?? (data.selCurso ? Number(data.selCurso.curso) : undefined);
          const seccion = String(value);
          const match = visibleCursos.find((c) => (nivel == null || Number(c.curso) === nivel) && c.seccion === seccion && (!selectedEspecialidad || c.especialidad === selectedEspecialidad.nombre));
          params({ cursoId: match ? String(match.id) : '' });
        }} disabled={visibleCursos.length === 0 || (selectedNivel == null && !data.selCurso)} placeholder="Sección" options={[{ value: '', label: 'Todas' }, ...(selectedNivel != null ? seccionesForNivel(selectedNivel).map((s) => ({ value: s, label: String(s) })) : [])]} />
      </label>
      <button className={`tab ${view === 'clase' ? 'active' : ''}`} onClick={() => params({ view: 'clase' })}>Clase</button>
      <button className={`tab ${view === 'planillas' ? 'active' : ''}`} onClick={() => params({ view: 'planillas' })}>Planillas</button>
    </div>
      {!data.selCurso ? <section className="panel idle-state"><div className="idle-dots" aria-hidden="true"><span className="idle-dot" /><span className="idle-dot" /><span className="idle-dot" /></div><h2>Esperando selección</h2><p>Elegí una especialidad y un curso para continuar con la clase.</p></section> : view === 'clase' ? <ClassView data={data} reload={load} /> : <PlanillasView data={data} syncingProp={syncingAll} setSyncingProp={setSyncingAll} />}
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
      try {
        for (const p of data.planillas) {
          if (cancelled) break;
          try {
            await syncClassroom(p.id);
          } catch (_) {
            // ignore per-planilla errors
          }
        }
      } finally {
        if (!cancelled) setSyncingProp?.(false);
      }
    })();
    return () => { cancelled = true; setSyncingProp?.(false); };
  }, [data.googleClassroomConnected, data.planillas, setSyncingProp]);

  const syncing = syncingProp ?? false;

  return <><section className="summary-grid"><article className="metric"><span>Curso</span><strong>{data.selCurso?.curso}° {data.selCurso?.seccion}</strong></article><article className="metric"><span>Etapa</span><strong>{data.selEtapa}ª</strong></article><article className="metric"><span>Planillas</span><strong>{data.planillas.length}</strong></article><article className="metric"><span>Classroom</span><strong>{syncing ? 'Sincronizando…' : (data.googleClassroomConnected ? 'Conectado' : 'Sin conexión')}</strong></article></section><div className="card-grid">{data.planillas.map((p) => <Link className="nav-card" key={p.id} to={`/planilla/${p.id}`}><span>{p.periodo}</span><h2>{p.nombre}</h2><p>{p.tareasCount} tareas registradas</p><strong>Abrir planilla →</strong></Link>)}{data.materiasDetectadas.filter((m) => !existingMateriaIds.has(m.id)).map((m) => <button className="nav-card add-card" key={m.id} onClick={() => openMateria(m.id)}><span>{m.categoria}</span><h2>{m.nombre}</h2><p>Crear la planilla para esta etapa.</p><strong>Crear y abrir →</strong></button>)}{data.planillas.length === 0 && data.materiasDetectadas.length === 0 && <section className="panel empty-state"><h2>Sin materias asignadas</h2><p>Consultá con administración para asociar materias al curso.</p></section>}</div></>;
}

function ClassView({ data, reload }: { data: HomeResponse; reload: () => Promise<void> }) {
  const [tema, setTema] = useState(''); const [instrumentoId, setInstrumentoId] = useState(0); const [ausentes, setAusentes] = useState<number[]>([]); const [status, setStatus] = useState('');
  async function create(e: FormEvent) { e.preventDefault(); if (!data.selCurso) return; try { await createClass({ cursoId: data.selCurso.id, etapa: data.selEtapa, instrumentoId, turno: 'turno', tema, alumnosAusentes: ausentes }); setStatus('Clase registrada.'); setTema(''); await reload(); } catch (err) { setStatus(err instanceof ApiError ? err.message : 'No se pudo registrar la clase.'); } }
  async function mark(id: number, estado: string) { await updateAttendance(id, estado); await reload(); }
  return <div className="two-column"><form className="panel form-grid" onSubmit={create}><h2>Registrar clase</h2><label>Tema<input value={tema} required onChange={(e) => setTema(e.target.value)} /></label><label>Instrumento<AnimatedSelect ariaLabel="Instrumento" value={instrumentoId} onChange={(value) => setInstrumentoId(Number(value))} options={[{ value: 0, label: 'Sin instrumento' }, ...data.instrumentos.map((item) => ({ value: item.id, label: item.nombre }))]} /></label><fieldset><legend>Alumnos ausentes</legend><div className="check-list">{data.rasgoAlumnosValidos.map((a) => <label key={a.id}><input type="checkbox" checked={ausentes.includes(a.id)} onChange={(e) => setAusentes((v) => e.target.checked ? [...v, a.id] : v.filter((id) => id !== a.id))} />{a.apellido}, {a.nombre}</label>)}</div></fieldset>{status && <div className="notice">{status}</div>}<button className="button">Guardar clase</button></form><section className="panel"><h2>Asistencia e historial</h2>{data.rasgoAsistencias.map((a) => <div className="attendance-row" key={a.id}><span>{a.alumnoNombreCompleto}</span><button className={a.estado === 'presente' ? 'active' : ''} onClick={() => mark(a.id, 'presente')}>Presente</button><button className={a.estado === 'ausente' ? 'active danger' : ''} onClick={() => mark(a.id, 'ausente')}>Ausente</button></div>)}{data.rasgoPlanillas.map((p) => <div className="history-row" key={p.id}><strong>{p.tema}</strong><span>{p.fechaClase}</span></div>)}</section></div>;
}
