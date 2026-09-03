export function normalizeGradeInput(value: string, taskTotal: number): string {
  if (value === '') return '';

  const numericValue = Number(value);
  if (!Number.isFinite(numericValue)) return '';

  return String(Math.max(0, Math.min(taskTotal, Math.round(numericValue))));
}
