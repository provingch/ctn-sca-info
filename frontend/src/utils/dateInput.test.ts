import { describe, expect, it } from 'vitest';
import { isDateReached, isValidDateTimeValue, isValidDateValue, localDateValue } from './dateInput';
describe('Fechas de formularios', () => {
  it('valida fechas reales y años bisiestos', () => {
    expect(isValidDateValue('2024-02-29')).toBe(true);
    expect(isValidDateValue('2026-02-29')).toBe(false);
    expect(isValidDateValue('2026-04-31')).toBe(false);
    expect(isValidDateValue('')).toBe(false);
  });
  it('usa el día local y exige fecha y hora completas', () => {
    expect(localDateValue(new Date(2026, 8, 9, 23, 59))).toBe('2026-09-09');
    expect(isValidDateTimeValue('2026-09-09T23:59')).toBe(true);
    expect(isValidDateTimeValue('2026-09-09T')).toBe(false);
    expect(isValidDateTimeValue('T12:30')).toBe(false);
    expect(isValidDateTimeValue('2026-09-09T24:00')).toBe(false);
  });
  it('una fecha ya llegó si es hoy o anterior, y no si es futura o está vacía', () => {
    expect(isDateReached('2026-09-09', '2026-09-09')).toBe(true);
    expect(isDateReached('2026-09-08', '2026-09-09')).toBe(true);
    expect(isDateReached('2025-12-31', '2026-01-01')).toBe(true);
    expect(isDateReached('2026-09-10', '2026-09-09')).toBe(false);
    expect(isDateReached('2027-01-01', '2026-12-31')).toBe(false);
    expect(isDateReached('', '2026-09-09')).toBe(false);
    expect(isDateReached('2026-02-30', '2026-09-09')).toBe(false);
  });
});
