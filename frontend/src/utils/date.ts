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

/** Fecha corta para la columna "Desde": "hoy 09:12", "ayer", "14/10". */
export function shortWhen(value?: string | null): string {
  if (!value) return '';
  const date = new Date(value.includes('T') ? value : value.replace(' ', 'T'));
  if (Number.isNaN(date.getTime())) return value;
  const today = new Date();
  const startOf = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
  const days = Math.round((startOf(today) - startOf(date)) / 86_400_000);
  const time = date.toLocaleTimeString('es-PY', { hour: '2-digit', minute: '2-digit', hour12: false });
  if (days === 0) return `hoy ${time}`;
  if (days === 1) return 'ayer';
  return date.toLocaleDateString('es-PY', { day: '2-digit', month: '2-digit' });
}
