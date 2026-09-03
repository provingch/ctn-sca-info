import { describe, expect, it } from 'vitest';
import { normalizeGradeInput } from './gradeValidation';

describe('normalizeGradeInput', () => {
  it('mantiene vacío un puntaje todavía no cargado', () => {
    expect(normalizeGradeInput('', 10)).toBe('');
  });

  it('limita el puntaje al rango de la tarea mientras se escribe', () => {
    expect(normalizeGradeInput('-2', 10)).toBe('0');
    expect(normalizeGradeInput('7', 10)).toBe('7');
    expect(normalizeGradeInput('15', 10)).toBe('10');
  });

  it('convierte el puntaje a un número entero', () => {
    expect(normalizeGradeInput('7.6', 10)).toBe('8');
  });
});
