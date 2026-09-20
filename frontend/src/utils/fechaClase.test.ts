import { describe, expect, it } from 'vitest';
import { formatFechaClase } from './fechaClase';

describe('formatFechaClase', () => {
  it('formatea la fecha de la clase sin correrla por zona horaria', () => {
    expect(formatFechaClase('2026-06-10')).toBe('10/06/2026');
    expect(formatFechaClase('2026-06-10T00:00:00.000+00:00')).toBe('10/06/2026');
  });

  it('conserva un valor desconocido y avisa cuando falta', () => {
    expect(formatFechaClase('otro')).toBe('otro');
    expect(formatFechaClase(null)).toBe('Fecha no disponible');
  });
});
