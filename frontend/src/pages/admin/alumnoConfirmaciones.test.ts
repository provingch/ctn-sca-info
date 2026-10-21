import { describe, expect, it } from 'vitest';
import type { ResumenAlumno } from '../../api/admin';
import { mensajeBorrado, mensajeTraslado } from './alumnoConfirmaciones';

const vacio: ResumenAlumno = { notas: 0, planillasConNotas: 0, planillasConNotasCursoActual: 0, asistencias: 0, anotaciones: 0, fichaEnfermeria: false, consultasEnfermeria: 0 };
const informatica = { id: 1, especialidad: 'Informática', nivel: 2, seccion: 'A' };
const quimica = { id: 2, especialidad: 'Química', nivel: 2, seccion: 'A' };

describe('mensajeBorrado', () => {
  it('avisa cuando no hay nada cargado', () => {
    expect(mensajeBorrado('Pérez, Juan', vacio)).toBe('¿Eliminar a Pérez, Juan? No tiene notas ni otros datos cargados.');
  });

  it('enumera todo lo que se pierde', () => {
    const resumen = { ...vacio, notas: 34, planillasConNotas: 7, anotaciones: 2, fichaEnfermeria: true, consultasEnfermeria: 1 };
    expect(mensajeBorrado('Pérez, Juan', resumen)).toBe(
      '¿Eliminar a Pérez, Juan? También se van a borrar 34 notas en 7 planillas, 2 anotaciones, su ficha de enfermería y 1 consulta de enfermería. Esto no se puede deshacer.');
  });
});

describe('mensajeTraslado', () => {
  it('marca el cambio de especialidad y cuenta las planillas que quedan atrás', () => {
    expect(mensajeTraslado('Pérez, Juan', informatica, quimica, { ...vacio, planillasConNotasCursoActual: 1 })).toBe(
      'Vas a mover a Pérez, Juan de Informática 2ºA a Química 2ºA (otra especialidad). Tiene notas en 1 planilla de su curso actual: '
      + 'quedan como historial, en gris y de solo lectura, y no se pasan al curso nuevo.');
  });

  it('un cambio de sección sin notas no menciona otra especialidad', () => {
    expect(mensajeTraslado('Pérez, Juan', informatica, { ...informatica, id: 3, seccion: 'B' }, vacio)).toBe(
      'Vas a mover a Pérez, Juan de Informática 2ºA a Informática 2ºB. No tiene notas cargadas en su curso actual.');
  });
});
