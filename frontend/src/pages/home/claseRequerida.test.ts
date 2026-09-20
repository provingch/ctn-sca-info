import { describe, expect, it } from 'vitest';
import { datosFaltantesDeLaClase, mensajeDatosFaltantes } from './claseRequerida';

const completa = { horario: '8:45', cantidadHoras: '2', modalidad: 'Presencial', instrumentoId: 5 };

describe('datosFaltantesDeLaClase', () => {
  it('una clase completa no tiene faltantes', () => {
    expect(datosFaltantesDeLaClase(completa)).toEqual([]);
  });

  it('detecta cada dato faltante por separado', () => {
    expect(datosFaltantesDeLaClase({ ...completa, horario: '' })).toEqual(['el horario de inicio']);
    expect(datosFaltantesDeLaClase({ ...completa, cantidadHoras: '' })).toEqual(['las horas cátedra']);
    expect(datosFaltantesDeLaClase({ ...completa, cantidadHoras: '0' })).toEqual(['las horas cátedra']);
    expect(datosFaltantesDeLaClase({ ...completa, modalidad: ' ' })).toEqual(['la modalidad']);
    expect(datosFaltantesDeLaClase({ ...completa, instrumentoId: 0 })).toEqual(['el tipo de clase']);
  });

  it('con el formulario recién abierto faltan horario, horas y tipo de clase (la modalidad ya viene en Presencial)', () => {
    expect(datosFaltantesDeLaClase({ horario: '', cantidadHoras: '', modalidad: 'Presencial', instrumentoId: 0 }))
      .toEqual(['el horario de inicio', 'las horas cátedra', 'el tipo de clase']);
  });
});

describe('mensajeDatosFaltantes', () => {
  it('arma un mensaje legible según cuántos falten', () => {
    expect(mensajeDatosFaltantes([])).toBe('');
    expect(mensajeDatosFaltantes(['el tipo de clase'])).toBe('Completá el tipo de clase para registrar la clase.');
    expect(mensajeDatosFaltantes(['el horario de inicio', 'las horas cátedra'])).toBe('Completá el horario de inicio y las horas cátedra para registrar la clase.');
    expect(mensajeDatosFaltantes(['el horario de inicio', 'las horas cátedra', 'el tipo de clase']))
      .toBe('Completá el horario de inicio, las horas cátedra y el tipo de clase para registrar la clase.');
  });
});
