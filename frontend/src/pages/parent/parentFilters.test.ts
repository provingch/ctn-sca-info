import { describe, expect, it } from 'vitest';
import type { ParentSubject, RasgoConducta } from '../../api/parent';
import { filtrarConducta, filtrarMaterias, hayFiltroConducta, materiasDeConducta, rangoInvalido, SIN_FILTRO_CONDUCTA } from './parentFilters';

const nota = (materia: string | null, fechaClase: string | null, codigo = 'N1'): RasgoConducta =>
  ({ materia, fechaClase, profesorNombre: 'Prof', codigo, descripcion: null, observacion: null });
const notas = [
  nota('Matemática', '2026-03-10'),
  nota('Física', '2026-04-15'),
  nota('Matemática', '2026-05-20'),
  nota(null, '2026-05-21'),
  nota('Física', null),
];

describe('filtrarMaterias', () => {
  const materias = [{ materia: 'Matemática' }, { materia: 'Educación Física' }, { materia: 'Química' }] as ParentSubject[];

  it('busca por nombre sin distinguir tildes ni mayúsculas', () => {
    expect(filtrarMaterias(materias, 'matematica').map((m) => m.materia)).toEqual(['Matemática']);
    expect(filtrarMaterias(materias, 'FISICA').map((m) => m.materia)).toEqual(['Educación Física']);
  });

  it('sin búsqueda devuelve todas', () => {
    expect(filtrarMaterias(materias, '')).toHaveLength(3);
  });
});

describe('filtrarConducta', () => {
  it('sin filtros devuelve todas, incluidas las sin fecha o sin materia', () => {
    expect(filtrarConducta(notas, SIN_FILTRO_CONDUCTA)).toHaveLength(5);
  });

  it('por materia', () => {
    expect(filtrarConducta(notas, { ...SIN_FILTRO_CONDUCTA, materia: 'Matemática' })).toHaveLength(2);
  });

  it('por rango de fechas, con los extremos incluidos', () => {
    const resultado = filtrarConducta(notas, { ...SIN_FILTRO_CONDUCTA, desde: '2026-04-15', hasta: '2026-05-20' });
    expect(resultado.map((n) => n.fechaClase)).toEqual(['2026-04-15', '2026-05-20']);
  });

  it('con sólo un extremo el otro queda abierto', () => {
    expect(filtrarConducta(notas, { ...SIN_FILTRO_CONDUCTA, desde: '2026-05-01' })).toHaveLength(2);
    expect(filtrarConducta(notas, { ...SIN_FILTRO_CONDUCTA, hasta: '2026-03-31' })).toHaveLength(1);
  });

  it('una nota sin fecha queda afuera apenas hay un rango', () => {
    const resultado = filtrarConducta(notas, { ...SIN_FILTRO_CONDUCTA, desde: '2026-01-01' });
    expect(resultado.some((n) => n.fechaClase === null)).toBe(false);
  });

  it('combina materia y fechas', () => {
    expect(filtrarConducta(notas, { materia: 'Física', desde: '2026-04-01', hasta: '2026-04-30' })).toHaveLength(1);
  });

  it('acepta una fecha con hora sin sacar el último día del rango', () => {
    const conHora = [nota('Física', '2026-05-20 10:30:00')];
    expect(filtrarConducta(conHora, { ...SIN_FILTRO_CONDUCTA, hasta: '2026-05-20' })).toHaveLength(1);
  });

  it('un rango con "desde" posterior a "hasta" es inválido y no devuelve nada', () => {
    const invertido = { ...SIN_FILTRO_CONDUCTA, desde: '2026-06-01', hasta: '2026-05-01' };
    expect(rangoInvalido(invertido)).toBe(true);
    expect(filtrarConducta(notas, invertido)).toEqual([]);
    expect(rangoInvalido({ ...SIN_FILTRO_CONDUCTA, desde: '2026-05-01', hasta: '2026-05-01' })).toBe(false);
  });
});

describe('materiasDeConducta / hayFiltroConducta', () => {
  it('lista las materias que realmente aparecen, sin repetir ni vacías, ordenadas', () => {
    expect(materiasDeConducta(notas)).toEqual(['Física', 'Matemática']);
  });

  it('detecta si hay algún filtro activo', () => {
    expect(hayFiltroConducta(SIN_FILTRO_CONDUCTA)).toBe(false);
    expect(hayFiltroConducta({ ...SIN_FILTRO_CONDUCTA, hasta: '2026-01-01' })).toBe(true);
  });
});
