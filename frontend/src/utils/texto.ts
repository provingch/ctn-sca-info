/** Minúsculas y sin tildes, para buscar "matematica" y encontrar "Matemática". */
export function normalizarTexto(valor: string | number | null | undefined): string {
  return String(valor ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();
}

/** true si `busqueda` (vacía = todo) aparece en alguno de los campos, sin distinguir tildes ni mayúsculas. */
export function coincideBusqueda(busqueda: string, ...campos: Array<string | number | null | undefined>): boolean {
  const termino = normalizarTexto(busqueda);
  if (!termino) return true;
  return normalizarTexto(campos.map((campo) => campo ?? '').join(' ')).includes(termino);
}
