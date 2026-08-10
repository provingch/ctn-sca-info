import { useEffect, useState } from 'react';
import AppShell from '../../components/AppShell';
import { getCursosEvaluacion, getEspecialidades, type CursoEvaluacion, type Especialidad } from '../../api/academics';

export default function EvaluacionPage() {
  const [especialidades, setEspecialidades] = useState<Especialidad[]>([]); const [cursos, setCursos] = useState<CursoEvaluacion[]>([]);
  const [especialidadId, setEspecialidadId] = useState(0); const [cursoId, setCursoId] = useState(0); const [etapa, setEtapa] = useState('primera'); const [periodo, setPeriodo] = useState(new Date().getFullYear());
  useEffect(() => { getEspecialidades().then(setEspecialidades); }, []);
  useEffect(() => { getCursosEvaluacion(especialidadId || undefined).then(setCursos); setCursoId(0); }, [especialidadId]);
  const selected = cursos.find((c) => c.id === cursoId);
  const exportUrl = selected ? `/api/evaluacion/export?cursoId=${cursoId}&etapa=${etapa}&periodo=${periodo}` : '#';
  return <AppShell title="Descargar planillas"><section className="panel form-grid">
    <p className="lead">Elegí el curso y el período académico para generar sus planillas.</p>
    <label>Especialidad<select value={especialidadId} onChange={(e) => setEspecialidadId(Number(e.target.value))}><option value="0">Todas</option>{especialidades.map((e) => <option key={e.id} value={e.id}>{e.nombre}</option>)}</select></label>
    <label>Curso<select value={cursoId} onChange={(e) => setCursoId(Number(e.target.value))}><option value="0">Seleccione…</option>{cursos.map((c) => <option key={c.id} value={c.id}>{c.nivel}° {c.seccion} · {c.especialidad}</option>)}</select></label>
    <label>Etapa<select value={etapa} onChange={(e) => setEtapa(e.target.value)}><option value="primera">Primera etapa</option><option value="segunda">Segunda etapa</option></select></label>
    <label>Período<input type="number" min="2000" value={periodo} onChange={(e) => setPeriodo(Number(e.target.value))} /></label>
    <a className={`button ${!selected ? 'disabled' : ''}`} href={exportUrl} aria-disabled={!selected}>Descargar planillas</a>
  </section></AppShell>;
}
