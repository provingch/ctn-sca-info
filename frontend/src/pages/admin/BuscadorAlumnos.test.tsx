import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import BuscadorAlumnos from './BuscadorAlumnos';

const cursos = [{ id: 1, especialidad: 'Informática', nivel: 2, seccion: 'A' }];
const alumnos = [
  { id: 1, nombre: 'Ana', apellido: 'Gómez', cursoId: 1, ci: '4500123' },
  { id: 2, nombre: 'Luis', apellido: 'Benítez', cursoId: 1, ci: null },
];

describe('BuscadorAlumnos', () => {
  it('busca por apellido o cédula y abre al elegido con el teclado', () => {
    const onElegir = vi.fn();
    render(<BuscadorAlumnos alumnos={alumnos} cursos={cursos} onElegir={onElegir} />);
    const input = screen.getByRole('combobox', { name: 'Buscar alumno' });

    fireEvent.change(input, { target: { value: '4500' } });
    expect(screen.getAllByRole('option')).toHaveLength(1);
    expect(screen.getByRole('option')).toHaveTextContent('Gómez, Ana');
    expect(screen.getByRole('option')).toHaveTextContent('Informática 2ºA · CI 4500123');

    fireEvent.change(input, { target: { value: 'benitez' } });
    fireEvent.keyDown(input, { key: 'Enter' });
    expect(onElegir).toHaveBeenCalledWith(alumnos[1]);
  });

  it('Ctrl+K lleva el foco al buscador', () => {
    render(<BuscadorAlumnos alumnos={alumnos} cursos={cursos} onElegir={() => {}} />);
    fireEvent.keyDown(window, { key: 'k', ctrlKey: true });
    expect(screen.getByRole('combobox', { name: 'Buscar alumno' })).toHaveFocus();
  });
});
