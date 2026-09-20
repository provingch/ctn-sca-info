import { useEffect, useMemo, useState } from 'react';
import { getCursosEvaluacion, getEspecialidades, getMateriasEvaluacion, type CursoEvaluacion, type Especialidad } from '../../api/academics';
import { ApiError } from '../../api/client';
import { useSpecialty } from '../../context/SpecialtyContext';
import { normalizeSpecialty } from '../../theme/theme';
import AnimatedSelect from '../../components/AnimatedSelect';

/**
 * Filtros de especialidad, curso, sección, etapa, materia y período que comparten "Descargar planillas" y
 * "Ver planillas". El estado vive en EvaluacionPage, así que no se pierde al pasar de una vista a otra.
 */
export function useEvaluacionFiltros() {
  const specialty = useSpecialty();
  const [especialidades, setEspecialidades] = useState<Especialidad[]>([]);
  const [cursos, setCursos] = useState<CursoEvaluacion[]>([]);
  const [especialidadId, setEspecialidadId] = useState(specialty.id ?? 0);
  const [cursoNivel, setCursoNivel] = useState(0);
  const [seccion, setSeccion] = useState('');
  const [etapa, setEtapa] = useState('primera');
  const [periodo, setPeriodo] = useState(new Date().getFullYear());
  const [materias, setMaterias] = useState<{ id: number; nombre: string }[]>([]);
  const [materiaId, setMateriaId] = useState(0);
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
    setMaterias([]);
    setMateriaId(0);
    if (!especialidadId) return;
    getCursosEvaluacion(especialidadId)
      .then(setCursos)
      .catch((error) => setStatus(error instanceof ApiError ? error.message : 'No se pudieron cargar los cursos.'));
  }, [especialidadId]);

  const niveles = useMemo(() => [...new Set(cursos.map((course) => course.nivel))].sort((a, b) => a - b), [cursos]);
  const secciones = useMemo(() => [...new Set(cursos.filter((course) => course.nivel === cursoNivel).map((course) => course.seccion))].sort(), [cursos, cursoNivel]);
  const selected = cursos.find((course) => course.nivel === cursoNivel && course.seccion === seccion);

  useEffect(() => {
    setMaterias([]);
    setMateriaId(0);
    if (!selected) return;
    getMateriasEvaluacion(selected.id, periodo)
      .then((items) => setMaterias(items))
      .catch((error) => setStatus(error instanceof ApiError ? error.message : 'No se pudieron cargar las materias.'));
  }, [selected, periodo]);

  function changeSpecialty(id: number) {
    setEspecialidadId(id);
    setStatus('');
    const selectedSpecialty = especialidades.find((item) => item.id === id);
    if (selectedSpecialty) specialty.selectSpecialty(selectedSpecialty.nombre, selectedSpecialty.id);
    else specialty.resetSpecialty();
  }

  return {
    especialidades, especialidadId, changeSpecialty,
    niveles, cursoNivel, setCursoNivel,
    secciones, seccion, setSeccion,
    etapa, setEtapa,
    materias, materiaId, setMateriaId,
    periodo, setPeriodo,
    selected, status,
  };
}

export type EvaluacionFiltros = ReturnType<typeof useEvaluacionFiltros>;

export function EvaluacionFiltrosCampos({ filtros }: { filtros: EvaluacionFiltros }) {
  const { especialidades, especialidadId, changeSpecialty, niveles, cursoNivel, setCursoNivel, secciones, seccion, setSeccion, etapa, setEtapa, materias, materiaId, setMateriaId, periodo, setPeriodo, selected, status } = filtros;
  return <>
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
    <label>Materia
      <AnimatedSelect ariaLabel="Materia" value={materiaId || ''} disabled={!selected || materias.length === 0} placeholder={selected ? 'Seleccione una materia…' : 'Primero seleccione curso y sección'} onChange={(value) => setMateriaId(Number(value))} options={[{ value: 0, label: 'Todas las materias' }, ...materias.map((m) => ({ value: m.id, label: m.nombre }))]} />
    </label>
    <label>Período<input type="number" min="2000" value={periodo} onChange={(event) => setPeriodo(Number(event.target.value))} /></label>
  </>;
}
