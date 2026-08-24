import { describe, expect, it } from 'vitest';
import { formatBytes, formatRelativeDate, groupSchedulesBySpecialty } from './adminFormatters';

describe('admin formatters', () => {
  it('agrupa horarios por especialidad y ordena los grupos', () => {
    const groups = groupSchedulesBySpecialty([
      { cursoId: 1, especialidad: 'Química', cursoDescripcion: '1° A', cantidadSlotsCargados: 4 },
      { cursoId: 2, especialidad: 'Informática', cursoDescripcion: '2° B', cantidadSlotsCargados: 5 },
      { cursoId: 3, especialidad: 'Química', cursoDescripcion: '3° C', cantidadSlotsCargados: 6 },
    ]);

    expect(groups.map((group) => group.specialty)).toEqual(['Informática', 'Química']);
    expect(groups[1].courses).toHaveLength(2);
  });

  it('presenta el espacio de logs en una unidad legible', () => {
    expect(formatBytes(0)).toBe('0 KB');
    expect(formatBytes(1_048_576)).toBe('1 MB');
  });

  it('presenta sincronizaciones nulas y pasadas en forma relativa', () => {
    const now = new Date('2026-08-24T12:00:00');
    expect(formatRelativeDate(null, now)).toBe('Nunca');
    expect(formatRelativeDate('2026-08-24 10:00:00', now)).toBe('hace 2 horas');
  });
});
