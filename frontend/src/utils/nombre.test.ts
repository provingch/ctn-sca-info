import { describe, expect, it } from 'vitest';
import { nombreCorto } from './nombre';

describe('nombreCorto', () => {
  it('toma la primera palabra del nombre y la del apellido', () => {
    expect(nombreCorto('Graciela Noemí', 'López Molinas')).toBe('Graciela López');
  });

  it('tolera nulls, blancos y espacios repetidos', () => {
    expect(nombreCorto('Graciela', null)).toBe('Graciela');
    expect(nombreCorto(undefined, 'López Molinas')).toBe('López');
    expect(nombreCorto('  ', '  ')).toBe('');
    expect(nombreCorto('  Graciela   Noemí ', ' López  Molinas ')).toBe('Graciela López');
  });
});
