import { describe, expect, it } from 'vitest';
import { coberturaTema } from './coberturaTema';

describe('coberturaTema', () => {
  it('un tema cubierto muestra su fecha, esté la etapa cerrada o no', () => {
    const tema = { estadoCobertura: 'CUBIERTO', fechaCobertura: '2026-05-10 10:00:00' };
    expect(coberturaTema(tema, false)).toEqual({ estado: 'cumplido', fecha: '2026-05-10 10:00:00' });
    expect(coberturaTema(tema, true)).toEqual({ estado: 'cumplido', fecha: '2026-05-10 10:00:00' });
  });

  it('un tema pendiente con la etapa todavía corriendo sigue diciendo Pendiente', () => {
    expect(coberturaTema({ estadoCobertura: 'PENDIENTE' }, false)).toEqual({ estado: 'pendiente' });
    expect(coberturaTema({ estadoCobertura: 'PENDIENTE' }, undefined)).toEqual({ estado: 'pendiente' });
  });

  it('un tema pendiente de una etapa cerrada es No cumplido', () => {
    expect(coberturaTema({ estadoCobertura: 'PENDIENTE' }, true)).toEqual({ estado: 'no-cumplido' });
  });

  it('un tema sin estado se trata como pendiente', () => {
    expect(coberturaTema({}, true)).toEqual({ estado: 'no-cumplido' });
    expect(coberturaTema({}, false)).toEqual({ estado: 'pendiente' });
  });
});
