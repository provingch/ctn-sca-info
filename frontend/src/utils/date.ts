export function parseSqlDateTime(value?: string | null): Date | null {
  if (!value) return null;
  const normalized = value.includes(' ') && !value.includes('T') ? value.replace(' ', 'T') : value;
  const date = new Date(normalized);
  return Number.isNaN(date.getTime()) ? null : date;
}

export function formatSqlDateTime(
  value: string | null | undefined,
  options: Intl.DateTimeFormatOptions = { dateStyle: 'short', timeStyle: 'short' },
  fallback = 'Fecha no disponible',
): string {
  const date = parseSqlDateTime(value);
  return date ? date.toLocaleString('es-PY', options) : value || fallback;
}
