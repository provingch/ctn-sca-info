import type { HorarioResumenCursoItem } from '../../api/admin';

export function groupSchedulesBySpecialty(items: HorarioResumenCursoItem[]) {
  const grouped = items.reduce<Record<string, HorarioResumenCursoItem[]>>((result, item) => {
    const specialty = item.especialidad || 'Sin especialidad';
    (result[specialty] ??= []).push(item);
    return result;
  }, {});
  return Object.entries(grouped)
    .map(([specialty, courses]) => ({ specialty, specialtyId: courses[0]?.especialidadId ?? null, courses }))
    .sort((first, second) => first.specialty.localeCompare(second.specialty, 'es', { sensitivity: 'base' }));
}

export function formatBytes(bytes: number) {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 KB';
  const units = ['B', 'KB', 'MB', 'GB'];
  const unitIndex = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  const value = bytes / 1024 ** unitIndex;
  return `${new Intl.NumberFormat('es-PY', { maximumFractionDigits: value >= 10 ? 0 : 1 }).format(value)} ${units[unitIndex]}`;
}

export function formatRelativeDate(value: string | null, now = new Date()) {
  if (!value) return 'Nunca';
  const date = new Date(value.includes('T') ? value : value.replace(' ', 'T'));
  if (Number.isNaN(date.getTime())) return value;
  const seconds = Math.round((date.getTime() - now.getTime()) / 1000);
  const ranges: Array<[Intl.RelativeTimeFormatUnit, number]> = [
    ['year', 31_536_000], ['month', 2_592_000], ['day', 86_400], ['hour', 3_600], ['minute', 60], ['second', 1],
  ];
  const [unit, divisor] = ranges.find(([, range]) => Math.abs(seconds) >= range) ?? ranges.at(-1)!;
  return new Intl.RelativeTimeFormat('es', { numeric: 'auto' }).format(Math.round(seconds / divisor), unit);
}
