import AnimatedSelect from '../../components/AnimatedSelect';
import type { EvaluacionFiltros } from './useEvaluacionFiltros';

/** Campos de los filtros de {@link useEvaluacionFiltros}: los comparten "Descargar planillas" y "Ver planillas". */
export default function EvaluacionFiltrosCampos({ filtros }: { filtros: EvaluacionFiltros }) {
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
