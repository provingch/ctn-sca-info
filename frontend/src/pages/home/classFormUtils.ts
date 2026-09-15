export const HORARIOS_CATEDRA = ['7:00', '7:35', '8:10', '8:45', '9:40', '10:15', '10:50', '11:25', '13:00', '13:35', '14:10', '14:45', '15:40', '16:15', '16:50', '17:25'];

export function classEndTime(start: string, count: number): string {
  const index = HORARIOS_CATEDRA.indexOf(start);
  if (index < 0 || !Number.isInteger(count) || count < 1 || index + count > (index < 8 ? 8 : 16)) return '';
  const [hours, minutes] = HORARIOS_CATEDRA[index + count - 1].split(':').map(Number);
  const end = hours * 60 + minutes + 35;
  return `${Math.floor(end / 60)}:${String(end % 60).padStart(2, '0')}`;
}
