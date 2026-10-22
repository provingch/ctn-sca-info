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

function sumar(resumenes: ResumenAlumno[]): ResumenAlumno {
  return resumenes.reduce((total, r) => ({
    notas: total.notas + r.notas,
    planillasConNotas: total.planillasConNotas + r.planillasConNotas,
    planillasConNotasCursoActual: total.planillasConNotasCursoActual + r.planillasConNotasCursoActual,
    asistencias: total.asistencias + r.asistencias,
    anotaciones: total.anotaciones + r.anotaciones,
    fichaEnfermeria: total.fichaEnfermeria || r.fichaEnfermeria,
    consultasEnfermeria: total.consultasEnfermeria + r.consultasEnfermeria,
  }), { notas: 0, planillasConNotas: 0, planillasConNotasCursoActual: 0, asistencias: 0, anotaciones: 0, fichaEnfermeria: false, consultasEnfermeria: 0 });
}

/** Borrado en lote: una sola confirmación con lo que se pierde entre todos los alumnos elegidos. */
export function mensajeBorradoLote(resumenes: ResumenAlumno[]): string {
  if (resumenes.length === 1) return mensajeBorrado('este alumno', resumenes[0]);
  const total = sumar(resumenes);
  const partes: string[] = [];
  if (total.notas > 0) partes.push(plural(total.notas, 'nota', 'notas'));
  if (total.asistencias > 0) partes.push(plural(total.asistencias, 'registro de asistencia', 'registros de asistencia'));
  if (total.anotaciones > 0) partes.push(plural(total.anotaciones, 'anotación', 'anotaciones'));
  const fichas = resumenes.filter((r) => r.fichaEnfermeria).length;
  if (fichas > 0) partes.push(plural(fichas, 'ficha de enfermería', 'fichas de enfermería'));
  if (total.consultasEnfermeria > 0) partes.push(plural(total.consultasEnfermeria, 'consulta de enfermería', 'consultas de enfermería'));
  const base = `¿Eliminar ${resumenes.length} alumnos?`;
  if (partes.length === 0) return `${base} Ninguno tiene notas ni otros datos cargados.`;
  return `${base} Entre todos, también se van a borrar ${enumerar(partes)}. Esto no se puede deshacer.`;
}

/** Traslado en lote al mismo curso destino. */
export function mensajeTrasladoLote(cantidad: number, desde: CourseItem | undefined, hacia: CourseItem | undefined, resumenes: ResumenAlumno[]): string {
  const otraEspecialidad = desde && hacia && desde.especialidad !== hacia.especialidad;
  // Sin un origen común (alumnos de cursos distintos) se habla de "sus cursos actuales".
  const origen = desde ? etiquetaCurso(desde) : 'sus cursos actuales';
  const base = `Vas a mover ${plural(cantidad, 'alumno', 'alumnos')} de ${origen} a ${etiquetaCurso(hacia)}${otraEspecialidad ? ' (otra especialidad)' : ''}.`;
  const conNotas = resumenes.filter((r) => r.planillasConNotasCursoActual > 0).length;
  if (conNotas === 0) return `${base} Ninguno tiene notas cargadas en su curso actual.`;
  return `${base} ${plural(conNotas, 'alumno tiene', 'alumnos tienen')} notas en su curso actual: `
    + 'quedan como historial, en gris y de solo lectura, y no se pasan al curso nuevo.';
}
