import { describe, expect, it } from 'vitest';
import type { EgresadoItem, StudentItem } from '../../api/admin';
import type { QuejaItem } from '../../api/quejas';
import { filtrarAlumnos, filtrarEgresados, filtrarQuejas, hayFiltroEgresados, hayFiltroQuejas, SIN_FILTRO_EGRESADOS, SIN_FILTRO_QUEJAS } from './filtrosListados';

const queja = (id: number, motivo: string, creadaEn: string, extra: Partial<QuejaItem> = {}): QuejaItem =>
  ({ id, tipo: 'CONTRA_PROFESOR', profesorId: 1, cursoId: 1, especialidadId: 1, motivo, creadaPor: 9, creadaEn, ...extra });
const quejas = [
  queja(1, 'No entregó las notas', '2026-03-10 10:00:00'),
  queja(2, 'Faltas reiteradas', '2026-04-15 09:00:00', { aceptadaEn: '2026-04-16 08:00:00' }),
  queja(3, 'Trato inadecuado', '2026-05-20 09:00:00', { rechazadaEn: '2026-05-21 08:00:00' }),
];
const texto = (q: QuejaItem) => q.motivo;

describe('filtrarQuejas', () => {
  it('sin filtros devuelve todas', () => {
    expect(filtrarQuejas(quejas, SIN_FILTRO_QUEJAS, texto)).toHaveLength(3);
    expect(hayFiltroQuejas(SIN_FILTRO_QUEJAS)).toBe(false);
  });

  it('busca en el texto armado sin distinguir tildes', () => {
    expect(filtrarQuejas(quejas, { ...SIN_FILTRO_QUEJAS, busqueda: 'ENTREGO' }, texto).map((q) => q.id)).toEqual([1]);
  });

  it('por estado', () => {
    expect(filtrarQuejas(quejas, { ...SIN_FILTRO_QUEJAS, estado: 'pendiente' }, texto).map((q) => q.id)).toEqual([1]);
    expect(filtrarQuejas(quejas, { ...SIN_FILTRO_QUEJAS, estado: 'aceptada' }, texto).map((q) => q.id)).toEqual([2]);
    expect(filtrarQuejas(quejas, { ...SIN_FILTRO_QUEJAS, estado: 'rechazada' }, texto).map((q) => q.id)).toEqual([3]);
  });

  it('por rango de la fecha de registro, extremos incluidos y con la hora ignorada', () => {
    const rango = { ...SIN_FILTRO_QUEJAS, desde: '2026-04-15', hasta: '2026-05-20' };
    expect(filtrarQuejas(quejas, rango, texto).map((q) => q.id)).toEqual([2, 3]);
  });

  it('combina todos los filtros', () => {
    expect(filtrarQuejas(quejas, { ...SIN_FILTRO_QUEJAS, busqueda: 'trato', estado: 'rechazada', desde: '2026-05-01', hasta: '' }, texto).map((q) => q.id)).toEqual([3]);
    expect(filtrarQuejas(quejas, { ...SIN_FILTRO_QUEJAS, busqueda: 'trato', estado: 'pendiente', desde: '', hasta: '' }, texto)).toEqual([]);
  });

  it('un rango invertido no devuelve nada', () => {
    expect(filtrarQuejas(quejas, { ...SIN_FILTRO_QUEJAS, desde: '2026-06-01', hasta: '2026-01-01' }, texto)).toEqual([]);
  });
});

const alumno = (id: number, apellido: string, nombre: string, ci: string | null) => ({ id, apellido, nombre, ci, cursoId: 1 }) as StudentItem;

describe('filtrarAlumnos', () => {
  const alumnos = [alumno(1, 'Pérez', 'Ana', '1234567'), alumno(2, 'Gómez', 'Luis', null)];

  it('busca por apellido, nombre o cédula', () => {
    expect(filtrarAlumnos(alumnos, 'perez').map((a) => a.id)).toEqual([1]);
    expect(filtrarAlumnos(alumnos, 'luis').map((a) => a.id)).toEqual([2]);
    expect(filtrarAlumnos(alumnos, '1234').map((a) => a.id)).toEqual([1]);
    expect(filtrarAlumnos(alumnos, '')).toHaveLength(2);
  });
});

const egresado = (id: number, apellido: string, especialidad: string | null, promocion: number | null) =>
  ({ id, apellido, nombre: 'X', ci: null, especialidad, promocion }) as EgresadoItem;

describe('filtrarEgresados', () => {
  const egresados = [egresado(1, 'Pérez', 'Informática', 2024), egresado(2, 'Gómez', 'Electrónica', 2024), egresado(3, 'Pérez', 'Electrónica', 2023), egresado(4, 'Sosa', null, null)];

  it('por especialidad, por año de egreso y por texto', () => {
    expect(filtrarEgresados(egresados, { ...SIN_FILTRO_EGRESADOS, especialidad: 'Electrónica' }).map((e) => e.id)).toEqual([2, 3]);
    expect(filtrarEgresados(egresados, { ...SIN_FILTRO_EGRESADOS, promocion: '2024' }).map((e) => e.id)).toEqual([1, 2]);
    expect(filtrarEgresados(egresados, { ...SIN_FILTRO_EGRESADOS, busqueda: 'perez' }).map((e) => e.id)).toEqual([1, 3]);
  });

  it('combina los filtros y detecta si hay alguno activo', () => {
    expect(filtrarEgresados(egresados, { busqueda: 'perez', especialidad: 'Electrónica', promocion: '2023' }).map((e) => e.id)).toEqual([3]);
    expect(hayFiltroEgresados(SIN_FILTRO_EGRESADOS)).toBe(false);
    expect(hayFiltroEgresados({ ...SIN_FILTRO_EGRESADOS, promocion: '2024' })).toBe(true);
  });
});
