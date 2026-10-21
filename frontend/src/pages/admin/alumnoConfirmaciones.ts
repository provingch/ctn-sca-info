import type { CourseItem, ResumenAlumno } from '../../api/admin';

const plural = (n: number, singular: string, varios: string) => `${n} ${n === 1 ? singular : varios}`;

function enumerar(partes: string[]): string {
  if (partes.length <= 1) return partes.join('');
  return `${partes.slice(0, -1).join(', ')} y ${partes.at(-1)}`;
}

export function etiquetaCurso(curso: CourseItem | undefined): string {
  return curso ? `${curso.especialidad} ${curso.nivel}º${curso.seccion}` : 'otro curso';
}

/** Texto de la confirmación de borrado: dice todo lo que se pierde, porque no se puede deshacer. */
export function mensajeBorrado(nombre: string, resumen: ResumenAlumno): string {
  const partes: string[] = [];
  if (resumen.notas > 0) partes.push(`${plural(resumen.notas, 'nota', 'notas')} en ${plural(resumen.planillasConNotas, 'planilla', 'planillas')}`);
  if (resumen.asistencias > 0) partes.push(plural(resumen.asistencias, 'registro de asistencia', 'registros de asistencia'));
  if (resumen.anotaciones > 0) partes.push(plural(resumen.anotaciones, 'anotación', 'anotaciones'));
  if (resumen.fichaEnfermeria) partes.push('su ficha de enfermería');
  if (resumen.consultasEnfermeria > 0) partes.push(plural(resumen.consultasEnfermeria, 'consulta de enfermería', 'consultas de enfermería'));
  if (partes.length === 0) return `¿Eliminar a ${nombre}? No tiene notas ni otros datos cargados.`;
  return `¿Eliminar a ${nombre}? También se van a borrar ${enumerar(partes)}. Esto no se puede deshacer.`;
}

/** Texto de la confirmación al cambiar de curso: avisa qué pasa con las notas que quedan en el curso actual. */
export function mensajeTraslado(nombre: string, desde: CourseItem | undefined, hacia: CourseItem | undefined, resumen: ResumenAlumno): string {
  const otraEspecialidad = desde && hacia && desde.especialidad !== hacia.especialidad;
  const base = `Vas a mover a ${nombre} de ${etiquetaCurso(desde)} a ${etiquetaCurso(hacia)}${otraEspecialidad ? ' (otra especialidad)' : ''}.`;
  if (resumen.planillasConNotasCursoActual === 0) return `${base} No tiene notas cargadas en su curso actual.`;
  return `${base} Tiene notas en ${plural(resumen.planillasConNotasCursoActual, 'planilla', 'planillas')} de su curso actual: `
    + 'quedan como historial, en gris y de solo lectura, y no se pasan al curso nuevo.';
}
