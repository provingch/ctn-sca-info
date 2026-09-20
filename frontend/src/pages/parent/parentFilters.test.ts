import { describe, expect, it } from 'vitest';
import type { ParentSubject, ParentTask, RasgoConducta } from '../../api/parent';
import { agruparTareasPorMes, etiquetaMes, filtrarConducta, filtrarMaterias, hayFiltroConducta, materiasDeConducta, mesDeTarea, mesesConTareas, ordenarTareas, rangoInvalido, SIN_FECHA, SIN_FILTRO_CONDUCTA } from './parentFilters';

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

const tarea = (id: number, fecha: string | null, titulo = `TP ${id}`): ParentTask =>
  ({ id, titulo, fecha: fecha as string, puntos: 10, total: 20, estado: 'CALIFICADA' });
const tareas = [
  tarea(4, '2026-05-02'),
  tarea(1, '2026-03-10'),
  tarea(3, '2026-03-25'),
  tarea(2, '2026-08-01'),
  tarea(5, null),
];

describe('mesDeTarea / etiquetaMes', () => {
  it('saca el mes de la fecha y lo nombra en español', () => {
    expect(mesDeTarea('2026-03-10')).toBe('2026-03');
    expect(mesDeTarea('2026-03-10 09:30:00')).toBe('2026-03');
    expect(etiquetaMes('2026-03')).toBe('Marzo 2026');
    expect(etiquetaMes('2025-12')).toBe('Diciembre 2025');
  });

  it('una fecha ausente o inválida queda como sin fecha', () => {
    expect(mesDeTarea(null)).toBe(SIN_FECHA);
    expect(mesDeTarea('')).toBe(SIN_FECHA);
    expect(mesDeTarea('2026-13-01')).toBe(SIN_FECHA);
    expect(etiquetaMes(SIN_FECHA)).toBe('Sin fecha');
  });
});

describe('ordenarTareas', () => {
  it('ordena cronológicamente, deja las sin fecha al final y numera por esa posición', () => {
    const ordenadas = ordenarTareas(tareas);
    expect(ordenadas.map((t) => t.tarea.id)).toEqual([1, 3, 4, 2, 5]);
    expect(ordenadas.map((t) => t.numero)).toEqual([1, 2, 3, 4, 5]);
  });

  it('no modifica la lista original', () => {
    const original = tareas.map((t) => t.id);
    ordenarTareas(tareas);
    expect(tareas.map((t) => t.id)).toEqual(original);
  });
});

describe('mesesConTareas', () => {
  it('lista sólo los meses que tienen tareas, cronológicos, con las sin fecha al final', () => {
    expect(mesesConTareas(tareas)).toEqual([
      { value: '2026-03', label: 'Marzo 2026' },
      { value: '2026-05', label: 'Mayo 2026' },
      { value: '2026-08', label: 'Agosto 2026' },
      { value: SIN_FECHA, label: 'Sin fecha' },
    ]);
  });

  it('separa el mismo mes de años distintos', () => {
    expect(mesesConTareas([tarea(1, '2025-03-10'), tarea(2, '2026-03-10')]).map((m) => m.label)).toEqual(['Marzo 2025', 'Marzo 2026']);
  });
});

describe('agruparTareasPorMes', () => {
  it('con "Todos" agrupa por mes en orden cronológico', () => {
    const grupos = agruparTareasPorMes(tareas);
    expect(grupos.map((g) => g.label)).toEqual(['Marzo 2026', 'Mayo 2026', 'Agosto 2026', 'Sin fecha']);
    expect(grupos[0].tareas.map((t) => t.tarea.id)).toEqual([1, 3]);
  });

  it('con un mes puntual queda sólo ese mes y las tareas conservan su número', () => {
    const grupos = agruparTareasPorMes(tareas, '2026-05');
    expect(grupos).toHaveLength(1);
    expect(grupos[0].tareas.map((t) => [t.tarea.id, t.numero])).toEqual([[4, 3]]);
  });

  it('un mes sin tareas devuelve vacío y una lista vacía no rompe', () => {
    expect(agruparTareasPorMes(tareas, '2026-11')).toEqual([]);
    expect(agruparTareasPorMes([])).toEqual([]);
  });
});
