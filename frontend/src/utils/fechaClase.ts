/** "2026-06-10" (o "2026-06-10T..."/"2026-06-10 ...") → "10/06/2026" sin pasar por Date (evita el corrimiento por zona horaria). */
export function formatFechaClase(value?: string | null): string {
  const match = value?.match(/^(\d{4})-(\d{2})-(\d{2})/);
  return match ? `${match[3]}/${match[2]}/${match[1]}` : value || 'Fecha no disponible';
}
