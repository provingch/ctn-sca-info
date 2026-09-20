import { fireEvent, render, screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { PlanPendienteResumen } from '../../api/planCurricular';
import PlanesPorEspecialidad from './PlanesPorEspecialidad';

const plan = (id: number, especialidad?: string): PlanPendienteResumen => ({ id, especialidad, estado: 'PENDIENTE', archivoNombre: 'plan.xlsx', fechaSubida: '2026-09-19', materiaNombre: `Materia ${id}`, profesorNombre: 'Docente', cursoDescripcion: '2° A' });
const list = (planes: PlanPendienteResumen[]) => <div>{planes.map((item) => <button key={item.id}>{item.materiaNombre}</button>)}</div>;

describe('Planes por especialidad', () => {
  it('agrupa variantes del nombre, ordena especialidades y conserva el orden de los planes', () => {
    render(<PlanesPorEspecialidad planes={[plan(3, 'Informática'), plan(1, 'Electricidad'), plan(2, ' informatica ')]}>{list}</PlanesPorEspecialidad>);
    const headers = screen.getAllByRole('button');
    expect(headers[0]).toHaveTextContent('Electricidad');
    expect(headers[1]).toHaveTextContent('2 planes');
    expect(screen.queryByRole('button', { name: 'Materia 3' })).not.toBeInTheDocument();
    fireEvent.click(headers[1]);
    const region = screen.getByRole('region', { name: /Informática/ });
    expect(within(region).getAllByRole('button').map((item) => item.textContent)).toEqual(['Materia 3', 'Materia 2']);
    expect(region.closest('section')).toHaveAttribute('data-specialty', 'informatica');
  });
  it('conserva las acciones y actualiza el contador al retirar un plan revisado', () => {
    const select = vi.fn();
    const children = (items: PlanPendienteResumen[]) => items.map((item) => <button key={item.id} onClick={() => select(item.id)}>{item.materiaNombre}</button>);
    const { rerender } = render(<PlanesPorEspecialidad planes={[plan(1, 'Informática'), plan(2, 'Informática')]}>{children}</PlanesPorEspecialidad>);
    fireEvent.click(screen.getByRole('button', { name: 'Materia 2' }));
    expect(select).toHaveBeenCalledWith(2);
    rerender(<PlanesPorEspecialidad planes={[plan(1, 'Informática')]}>{children}</PlanesPorEspecialidad>);
    expect(screen.getByRole('button', { name: /Informática/ })).toHaveTextContent('1 plan');
    expect(screen.queryByRole('button', { name: 'Materia 2' })).not.toBeInTheDocument();
  });
  it('no pierde planes sin especialidad y abre automáticamente el único grupo', () => {
    render(<PlanesPorEspecialidad planes={[plan(1), plan(2, ' ')]}>{list}</PlanesPorEspecialidad>);
    expect(screen.getByRole('button', { name: /Especialidad no disponible/ })).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('button', { name: 'Materia 1' })).toBeVisible();
  });
});
