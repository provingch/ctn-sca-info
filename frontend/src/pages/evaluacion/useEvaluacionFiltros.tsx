import { useEffect, useMemo, useState } from 'react';
import { getCursosEvaluacion, getEspecialidades, getMateriasEvaluacion, type CursoEvaluacion, type Especialidad } from '../../api/academics';
import { ApiError } from '../../api/client';
import { useSpecialty } from '../../context/SpecialtyContext';
import { normalizeSpecialty } from '../../theme/theme';

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
