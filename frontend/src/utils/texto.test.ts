import { describe, expect, it } from 'vitest';
import { coincideBusqueda, normalizarTexto } from './texto';

describe('normalizarTexto', () => {
  it('quita tildes y pasa a minúsculas', () => {
    expect(normalizarTexto('  Matemática Aplicada ')).toBe('matematica aplicada');
    expect(normalizarTexto(null)).toBe('');
    expect(normalizarTexto(42)).toBe('42');
  });
});

describe('coincideBusqueda', () => {
  it('una búsqueda vacía coincide con todo', () => {
    expect(coincideBusqueda('', 'lo que sea')).toBe(true);
    expect(coincideBusqueda('   ')).toBe(true);
  });

  it('no distingue tildes ni mayúsculas y mira todos los campos', () => {
    expect(coincideBusqueda('MATEMATICA', 'Matemática', 'Prof. Rojas')).toBe(true);
    expect(coincideBusqueda('rojas', 'Matemática', 'Prof. Rojas')).toBe(true);
    expect(coincideBusqueda('quimica', 'Matemática', 'Prof. Rojas')).toBe(false);
  });

  it('tolera campos vacíos y numéricos', () => {
    expect(coincideBusqueda('3', null, undefined, 3)).toBe(true);
  });
});
