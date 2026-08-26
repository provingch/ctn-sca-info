import { useEffect, useMemo, useState } from 'react';
import AppShell from '../../components/AppShell';
import { getCursosEvaluacion, getEspecialidades, type CursoEvaluacion, type Especialidad } from '../../api/academics';
import { ApiError } from '../../api/client';
import { useSpecialty } from '../../context/SpecialtyContext';
import { normalizeSpecialty } from '../../theme/theme';
import AnimatedSelect from '../../components/AnimatedSelect';
import ReviewPlanesView from './ReviewPlanesView';

export default function EvaluacionPage() {
  const specialty = useSpecialty();
  const [view, setView] = useState<'menu' | 'planillas' | 'planes'>('menu');
  const [especialidades, setEspecialidades] = useState<Especialidad[]>([]);
  const [cursos, setCursos] = useState<CursoEvaluacion[]>([]);
  const [especialidadId, setEspecialidadId] = useState(specialty.id ?? 0);
  const [cursoNivel, setCursoNivel] = useState(0);
  const [seccion, setSeccion] = useState('');
  const [etapa, setEtapa] = useState('primera');
  const [periodo, setPeriodo] = useState(new Date().getFullYear());
  const [status, setStatus] = useState('');

  useEffect(() => {
    getEspecialidades().then((items) => {
      setEspecialidades(items);
      if (!especialidadId && specialty.name) {
        const saved = items.find((item) => normalizeSpecialty(item.nombre) === normalizeSpecialty(specialty.name));
        if (saved) setEspecialidadId(saved.id);
      }
    }).catch((error) => setStatus(error instanceof ApiError ? error.message : 'No se pudieron cargar las especialidades.'));
  }, [especialidadId, specialty.name]);

  useEffect(() => {
    setCursoNivel(0);
    setSeccion('');
    setCursos([]);
    if (!especialidadId) return;
    getCursosEvaluacion(especialidadId)
      .then(setCursos)
      .catch((error) => setStatus(error instanceof ApiError ? error.message : 'No se pudieron cargar los cursos.'));
  }, [especialidadId]);

  const niveles = useMemo(() => [...new Set(cursos.map((course) => course.nivel))].sort((a, b) => a - b), [cursos]);
  const secciones = useMemo(() => [...new Set(cursos.filter((course) => course.nivel === cursoNivel).map((course) => course.seccion))].sort(), [cursos, cursoNivel]);
  const selected = cursos.find((course) => course.nivel === cursoNivel && course.seccion === seccion);
  const exportUrl = selected ? `/api/evaluacion/export?cursoId=${selected.id}&etapa=${etapa}&periodo=${periodo}` : '#';

  function changeSpecialty(id: number) {
    setEspecialidadId(id);
    setStatus('');
    const selectedSpecialty = especialidades.find((item) => item.id === id);
    if (selectedSpecialty) specialty.selectSpecialty(selectedSpecialty.nombre, selectedSpecialty.id);
    else specialty.resetSpecialty();
  }

  if (view === 'menu') {
    return <AppShell title="Panel de Evaluación">
      <div className="choice-grid">
        <button type="button" onClick={() => setView('planillas')}><span>01</span><h2>Descargar planillas</h2><p>Exportá planillas completadas de los cursos.</p></button>
        <button type="button" onClick={() => setView('planes')}><span>02</span><h2>Revisar plan curricular</h2><p>Aprobá o rechazá planes de profesores.</p></button>
      </div>
    </AppShell>;
  }

  if (view === 'planes') {
    return <AppShell title="Revisar Planes Curriculares">
      <button type="button" className="button secondary" onClick={() => setView('menu')} style={{ marginBottom: 16 }}>← Volver</button>
      <ReviewPlanesView />
    </AppShell>;
  }

  return <AppShell title="Descargar planillas">
    <button type="button" className="button secondary" onClick={() => setView('menu')} style={{ marginBottom: 16 }}>← Volver</button>
    <section className="panel form-grid evaluation-filters">
      <p className="lead">Elegí la especialidad, el curso, la sección y el período académico para generar sus planillas.</p>
      {status && <div className="notice error" role="alert">{status}</div>}
      <label>Especialidad
        <AnimatedSelect ariaLabel="Especialidad" value={especialidadId || ''} required placeholder="Seleccione una especialidad…" onChange={(value) => changeSpecialty(Number(value))} options={especialidades.map((item) => ({ value: item.id, label: item.nombre }))} />
      </label>
      <label>Curso
        <AnimatedSelect ariaLabel="Curso" value={cursoNivel || ''} required disabled={!especialidadId || niveles.length === 0} placeholder={especialidadId ? 'Seleccione un curso…' : 'Primero seleccione una especialidad'} onChange={(value) => { setCursoNivel(Number(value)); setSeccion(''); }} options={niveles.map((nivel) => ({ value: nivel, label: `${nivel}°` }))} />
      </label>
      <label>Sección
        <AnimatedSelect ariaLabel="Sección" value={seccion} required disabled={!cursoNivel || secciones.length === 0} placeholder={cursoNivel ? 'Seleccione una sección…' : 'Primero seleccione un curso'} onChange={setSeccion} options={secciones.map((item) => ({ value: item, label: `Sección ${item}` }))} />
      </label>
      <label>Etapa<AnimatedSelect ariaLabel="Etapa" value={etapa} onChange={setEtapa} options={[{ value: 'primera', label: 'Primera etapa' }, { value: 'segunda', label: 'Segunda etapa' }]} /></label>
      <label>Período<input type="number" min="2000" value={periodo} onChange={(event) => setPeriodo(Number(event.target.value))} /></label>
      <a className={`button ${!selected ? 'disabled' : ''}`} href={selected ? exportUrl : undefined} aria-disabled={!selected} tabIndex={selected ? undefined : -1}>Descargar planillas</a>
    </section>
  </AppShell>;
}
