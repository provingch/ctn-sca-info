import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { createClass, getHome, updateAttendance, type HomeResponse } from '../../api/home';
import { ApiError } from '../../api/client';
import AppShell from '../../components/AppShell';
import { resolvePlanilla } from '../../api/academics';
import { useNavigate } from 'react-router-dom';

export default function HomePage() {
  const [search, setSearch] = useSearchParams(); const [data, setData] = useState<HomeResponse | null>(null); const [error, setError] = useState('');
  const view = search.get('view') || ''; const cursoId = Number(search.get('cursoId') || 0); const etapa = Number(search.get('etapa') || 1);
  const load = useCallback(async () => { try { setData(await getHome({ cursoId: cursoId || undefined, etapa, view: view === 'clase' ? 'clase' : 'planillas' })); } catch (e) { setError(e instanceof ApiError ? e.message : 'Error al cargar el inicio.'); } }, [cursoId, etapa, view]);
  useEffect(() => { void load(); }, [load]);
  if (!view) return <AppShell title="Elegí cómo querés empezar"><div className="choice-grid"><button onClick={() => setSearch({ view: 'clase' })}><span>01</span><h2>Iniciar una clase</h2><p>Asistencia, rasgos e historial del curso.</p></button><button onClick={() => setSearch({ view: 'planillas' })}><span>02</span><h2>Gestionar planillas</h2><p>Tareas, puntajes y sincronización con Classroom.</p></button></div></AppShell>;
  if (!data) return <AppShell title="Panel SCA"><div className="panel">{error || 'Cargando…'}</div></AppShell>;
  const params = (next: Record<string, string>) => setSearch({ view, cursoId: String(data.selCurso?.id || cursoId), etapa: String(data.selEtapa), ...next });
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
    <AppShell title="Panel SCA del curso" specialty={data.selCurso?.especialidad}><div className="toolbar filters"><button className="button secondary" onClick={() => setSearch({})}>← Inicio</button><select value={data.selCurso?.id || ''} onChange={(e) => params({ cursoId: e.target.value })}><option value="">Seleccioná un curso</option>{data.cursos.map((c) => <option key={c.id} value={c.id}>{c.curso}° {c.seccion} · {c.especialidad}</option>)}</select><select value={data.selEtapa} onChange={(e) => params({ etapa: e.target.value })}><option value="1">Primera etapa</option><option value="2">Segunda etapa</option></select><button className={`tab ${view === 'clase' ? 'active' : ''}`} onClick={() => params({ view: 'clase' })}>Clase</button><button className={`tab ${view === 'planillas' ? 'active' : ''}`} onClick={() => params({ view: 'planillas' })}>Planillas</button></div>
      {!data.selCurso ? <section className="panel idle-state"><div className="idle-dots" aria-hidden="true"><span className="idle-dot" /><span className="idle-dot" /><span className="idle-dot" /></div><h2>Esperando selección</h2><p>Elegí un curso y una etapa para continuar con la clase.</p></section> : view === 'clase' ? <ClassView data={data} reload={load} /> : <PlanillasView data={data} />}
    </AppShell>
  </>;
}

function PlanillasView({ data }: { data: HomeResponse }) {
  const navigate = useNavigate();
  const existingMateriaIds = new Set(data.planillas.map((p) => p.materiaId));
  async function openMateria(materiaId: number) { if (!data.selCurso) return; const result = await resolvePlanilla(data.selCurso.id, materiaId, data.selEtapa); navigate(`/planilla/${result.planillaId}`); }
  return <><section className="summary-grid"><article className="metric"><span>Curso</span><strong>{data.selCurso?.curso}° {data.selCurso?.seccion}</strong></article><article className="metric"><span>Etapa</span><strong>{data.selEtapa}ª</strong></article><article className="metric"><span>Planillas</span><strong>{data.planillas.length}</strong></article><article className="metric"><span>Classroom</span><strong>{data.googleClassroomConnected ? 'Conectado' : 'Sin conexión'}</strong></article></section><div className="card-grid">{data.planillas.map((p) => <Link className="nav-card" key={p.id} to={`/planilla/${p.id}`}><span>{p.periodo}</span><h2>{p.nombre}</h2><p>{p.tareasCount} tareas registradas</p><strong>Abrir planilla →</strong></Link>)}{data.materiasDetectadas.filter((m) => !existingMateriaIds.has(m.id)).map((m) => <button className="nav-card add-card" key={m.id} onClick={() => openMateria(m.id)}><span>{m.categoria}</span><h2>{m.nombre}</h2><p>Crear la planilla para esta etapa.</p><strong>Crear y abrir →</strong></button>)}{data.planillas.length === 0 && data.materiasDetectadas.length === 0 && <section className="panel empty-state"><h2>Sin materias asignadas</h2><p>Consultá con administración para asociar materias al curso.</p></section>}</div></>;
}

function ClassView({ data, reload }: { data: HomeResponse; reload: () => Promise<void> }) {
  const [tema, setTema] = useState(''); const [instrumentoId, setInstrumentoId] = useState(0); const [ausentes, setAusentes] = useState<number[]>([]); const [status, setStatus] = useState('');
  async function create(e: FormEvent) { e.preventDefault(); if (!data.selCurso) return; try { await createClass({ cursoId: data.selCurso.id, etapa: data.selEtapa, instrumentoId, turno: 'turno', tema, alumnosAusentes: ausentes }); setStatus('Clase registrada.'); setTema(''); await reload(); } catch (err) { setStatus(err instanceof ApiError ? err.message : 'No se pudo registrar la clase.'); } }
  async function mark(id: number, estado: string) { await updateAttendance(id, estado); await reload(); }
  return <div className="two-column"><form className="panel form-grid" onSubmit={create}><h2>Registrar clase</h2><label>Tema<input value={tema} required onChange={(e) => setTema(e.target.value)} /></label><label>Instrumento<select value={instrumentoId} onChange={(e) => setInstrumentoId(Number(e.target.value))}><option value="0">Sin instrumento</option>{data.instrumentos.map((i) => <option key={i.id} value={i.id}>{i.nombre}</option>)}</select></label><fieldset><legend>Alumnos ausentes</legend><div className="check-list">{data.rasgoAlumnosValidos.map((a) => <label key={a.id}><input type="checkbox" checked={ausentes.includes(a.id)} onChange={(e) => setAusentes((v) => e.target.checked ? [...v, a.id] : v.filter((id) => id !== a.id))} />{a.apellido}, {a.nombre}</label>)}</div></fieldset>{status && <div className="notice">{status}</div>}<button className="button">Guardar clase</button></form><section className="panel"><h2>Asistencia e historial</h2>{data.rasgoAsistencias.map((a) => <div className="attendance-row" key={a.id}><span>{a.alumnoNombreCompleto}</span><button className={a.estado === 'presente' ? 'active' : ''} onClick={() => mark(a.id, 'presente')}>Presente</button><button className={a.estado === 'ausente' ? 'active danger' : ''} onClick={() => mark(a.id, 'ausente')}>Ausente</button></div>)}{data.rasgoPlanillas.map((p) => <div className="history-row" key={p.id}><strong>{p.tema}</strong><span>{p.fechaClase}</span></div>)}</section></div>;
}
