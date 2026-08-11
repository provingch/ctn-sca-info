import { useEffect, useMemo, useState } from 'react';
import AppShell from '../../components/AppShell';
import { getCursosEvaluacion, getEspecialidades, type CursoEvaluacion, type Especialidad } from '../../api/academics';
import { ApiError } from '../../api/client';
import { useSpecialty } from '../../context/SpecialtyContext';
import { normalizeSpecialty } from '../../theme/theme';

export default function EvaluacionPage() {
  const specialty = useSpecialty();
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

  return <AppShell title="Descargar planillas">
    <section className="panel form-grid evaluation-filters">
      <p className="lead">Elegí la especialidad, el curso, la sección y el período académico para generar sus planillas.</p>
      {status && <div className="notice error" role="alert">{status}</div>}
      <label>Especialidad
        <select value={especialidadId} required onChange={(event) => changeSpecialty(Number(event.target.value))}>
          <option value="0">Seleccione una especialidad…</option>
          {especialidades.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}
        </select>
      </label>
      <label>Curso
        <select value={cursoNivel} required disabled={!especialidadId || niveles.length === 0} onChange={(event) => { setCursoNivel(Number(event.target.value)); setSeccion(''); }}>
          <option value="0">{especialidadId ? 'Seleccione un curso…' : 'Primero seleccione una especialidad'}</option>
          {niveles.map((nivel) => <option key={nivel} value={nivel}>{nivel}°</option>)}
        </select>
      </label>
      <label>Sección
        <select value={seccion} required disabled={!cursoNivel || secciones.length === 0} onChange={(event) => setSeccion(event.target.value)}>
          <option value="">{cursoNivel ? 'Seleccione una sección…' : 'Primero seleccione un curso'}</option>
          {secciones.map((item) => <option key={item} value={item}>Sección {item}</option>)}
        </select>
      </label>
      <label>Etapa<select value={etapa} onChange={(event) => setEtapa(event.target.value)}><option value="primera">Primera etapa</option><option value="segunda">Segunda etapa</option></select></label>
      <label>Período<input type="number" min="2000" value={periodo} onChange={(event) => setPeriodo(Number(event.target.value))} /></label>
      <a className={`button ${!selected ? 'disabled' : ''}`} href={exportUrl} aria-disabled={!selected}>Descargar planillas</a>
    </section>
  </AppShell>;
}
