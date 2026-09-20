import { describe, expect, it } from 'vitest';
import type { IncumplimientoPendiente } from '../../api/evaluacion';
import { categoriaDe, filtrarIncumplimientos, hayFiltroIncumplimientos, SIN_FILTRO_INCUMPLIMIENTOS } from './filtrosIncumplimientos';

const caso = (id: number, tipo: string, apellido: string, materiaNombre: string, descripcion = ''): IncumplimientoPendiente =>
  ({ id, asignacionId: 1, usuarioId: id, tipo, descripcion, estado: 'PENDIENTE', usuarioNombre: 'X', usuarioApellido: apellido, materiaNombre });
const nombreDe = (item: IncumplimientoPendiente) => `${item.usuarioApellido} ${item.usuarioNombre}`;
const casos = [
  caso(1, 'ATRASO', 'Benítez', 'Programación', 'Atraso justificado: enfermedad'),
  caso(2, 'INCONGRUENCIA_RETROACTIVA', 'Benítez', 'Programación'),
  caso(3, 'BLOQUEO_ATRASO_TIEMPO_REAL', 'Ferreira', 'Circuitos'),
  caso(4, 'BLOQUEO_INCONGRUENCIA_RETROACTIVA', 'Gómez', 'Física'),
];

describe('categoriaDe', () => {
  it('agrupa los tipos como la lista', () => {
    expect(categoriaDe('ATRASO')).toBe('atrasos');
    expect(categoriaDe('INCONGRUENCIA_RETROACTIVA')).toBe('retroactivas');
    expect(categoriaDe('BLOQUEO_ATRASO_TIEMPO_REAL')).toBe('bloqueos');
    expect(categoriaDe('BLOQUEO_INCONGRUENCIA_RETROACTIVA')).toBe('bloqueos');
    expect(categoriaDe('OTRO')).toBeNull();
  });
});

describe('filtrarIncumplimientos', () => {
  it('sin filtros devuelve todos', () => {
    expect(filtrarIncumplimientos(casos, SIN_FILTRO_INCUMPLIMIENTOS, nombreDe)).toHaveLength(4);
    expect(hayFiltroIncumplimientos(SIN_FILTRO_INCUMPLIMIENTOS)).toBe(false);
  });

  it('por categoría, incluyendo los dos tipos de bloqueo', () => {
    expect(filtrarIncumplimientos(casos, { busqueda: '', categoria: 'bloqueos' }, nombreDe).map((c) => c.id)).toEqual([3, 4]);
    expect(filtrarIncumplimientos(casos, { busqueda: '', categoria: 'atrasos' }, nombreDe).map((c) => c.id)).toEqual([1]);
  });

  it('busca por profesor, materia o descripción sin distinguir tildes', () => {
    expect(filtrarIncumplimientos(casos, { busqueda: 'benitez', categoria: '' }, nombreDe).map((c) => c.id)).toEqual([1, 2]);
    expect(filtrarIncumplimientos(casos, { busqueda: 'FISICA', categoria: '' }, nombreDe).map((c) => c.id)).toEqual([4]);
    expect(filtrarIncumplimientos(casos, { busqueda: 'enfermedad', categoria: '' }, nombreDe).map((c) => c.id)).toEqual([1]);
  });

  it('combina categoría y búsqueda', () => {
    expect(filtrarIncumplimientos(casos, { busqueda: 'benitez', categoria: 'retroactivas' }, nombreDe).map((c) => c.id)).toEqual([2]);
    expect(filtrarIncumplimientos(casos, { busqueda: 'ferreira', categoria: 'atrasos' }, nombreDe)).toEqual([]);
  });
});
