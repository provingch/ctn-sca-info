import type { ClaseDadaDto, ClaseDetalleDto, CodigoConducta } from '../../api/home';

export const normalizeHistoryText = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('es').trim();
export function classDate(value: string | null): string {
  if (!value) return 'Sin fecha';
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(value);
  if (!match) return value;
  const date = new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]), 12);
  const weekday = new Intl.DateTimeFormat('es-PY', { weekday: 'short' }).format(date).replace('.', '');
  return `${weekday} ${match[3]}/${match[2]}/${match[1]}`;
}
export function filterClassHistory(clases: ClaseDadaDto[], query: string, curso: string, especialidad: string, order: string) {
  const needle = normalizeHistoryText(query);
  const matches = clases.filter((item) => (!curso || String(item.cursoId) === curso)
    && (!especialidad || String(item.especialidadId ?? 'sin-especialidad') === especialidad)
    && (!needle || [item.tema, item.materiaNombre, item.cursoDescripcion, item.especialidadNombre].some((field) => normalizeHistoryText(field ?? '').includes(needle))));
  return matches.sort((a, b) => {
    // Undated records stay last in either order. ISO values compare chronologically.
    if (!a.fechaClase !== !b.fechaClase) return a.fechaClase ? -1 : 1;
    const result = (a.fechaClase ?? '').localeCompare(b.fechaClase ?? '')
      || (a.createdAt ?? '').localeCompare(b.createdAt ?? '') || a.id - b.id;
    return order === 'asc' ? result : -result;
  });
}
export function capitalizedName(value: string) {
  return value.toLocaleLowerCase('es').replace(/(^|[\s,'’-])(\p{L})/gu, (_, separator: string, letter: string) => separator + letter.toLocaleUpperCase('es'));
}
export function studentName(item: ClaseDetalleDto['asistencias'][number]) {
  const name = item.alumnoApellido ? `${item.alumnoApellido}, ${item.alumnoNombre ?? ''}` : item.alumnoNombreCompleto;
  return capitalizedName(name.trim());
}
export function isLate(codes: string[], catalog: CodigoConducta[]) {
  // The catalogue is editable: no hard-coded code (N7 is uniform in the seed).
  return codes.some((code) => {
    const description = normalizeHistoryText(catalog.find((item) => item.codigo === code)?.descripcion ?? '');
    return /\b(llegada tardia|llega tarde|llego tarde|tardanza|impuntualidad|ingreso tardio)\b/.test(description);
  });
}
export function editingAllowed(value: string | null) {
  if (!value) return false;
  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return false;
  date.setDate(date.getDate() + 7);
  const today = new Date(); today.setHours(0, 0, 0, 0);
  return today <= date;
}
