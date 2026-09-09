export function localDateValue(date = new Date()): string {
  return [String(date.getFullYear()).padStart(4, '0'), String(date.getMonth() + 1).padStart(2, '0'), String(date.getDate()).padStart(2, '0')].join('-');
}

export function isValidDateValue(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const [year, month, day] = value.split('-').map(Number);
  if (year < 1) return false;
  const date = new Date(0);
  date.setFullYear(year, month - 1, day);
  return localDateValue(date) === value;
}

export function isValidDateTimeValue(value: string): boolean {
  const [date, time] = value.split('T');
  return isValidDateValue(date ?? '') && /^([01]\d|2[0-3]):[0-5]\d$/.test(time ?? '');
}
