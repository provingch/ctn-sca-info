/**
 * Datos que "Iniciar clase" exige además del tema. El backend es la fuente de verdad (responde 400 si faltan); esto
 * sólo adelanta el error para que aparezca antes de mandar el request. Devuelve los nombres de lo que falta.
 */
export function datosFaltantesDeLaClase(clase: { horario: string; cantidadHoras: string; modalidad: string; instrumentoId: number }): string[] {
  const faltantes: string[] = [];
  if (!clase.horario.trim()) faltantes.push('el horario de inicio');
  if (!(Number(clase.cantidadHoras) > 0)) faltantes.push('las horas cátedra');
  if (!clase.modalidad.trim()) faltantes.push('la modalidad');
  if (!(clase.instrumentoId > 0)) faltantes.push('el tipo de clase');
  return faltantes;
}

export function mensajeDatosFaltantes(faltantes: string[]): string {
  if (faltantes.length === 0) return '';
  const lista = faltantes.length === 1 ? faltantes[0] : `${faltantes.slice(0, -1).join(', ')} y ${faltantes[faltantes.length - 1]}`;
  return `Completá ${lista} para registrar la clase.`;
}
