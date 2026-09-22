import { fireEvent, render, screen, within } from '@testing-library/react';
import { expect, it } from 'vitest';
import ComplaintGroups from './ComplaintGroups';
import type { QuejaItem } from '../../api/quejas';

const item = (id: number, estado: QuejaItem['estado'], especialidadId = 1): QuejaItem => ({ id, estado, especialidadId, tipo: 'CONTRA_PROFESOR', profesorId: 8, cursoId: 4, motivo: `Queja ${id}`, creadaPor: 1, creadaEn: `2026-09-${id < 10 ? '0' : ''}${id}T10:00:00` });
const specialtyName = (q: QuejaItem) => q.especialidadId === 1 ? 'Informática' : 'Electricidad';
const list = (items: QuejaItem[]) => <ul>{items.map(q => <li key={q.id}><button>{q.motivo}</button></li>)}</ul>;

it('separa estados dentro de especialidades colapsables y no hereda su color a las acciones', () => {
  render(<ComplaintGroups quejas={[item(1, 'pendiente'), item(2, 'resuelta'), item(3, 'aceptada', 2)]} specialtyName={specialtyName}>{list}</ComplaintGroups>);
  expect(screen.queryByRole('button', { name: 'Queja 1' })).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: /Informática/ }));
  const region = screen.getByRole('region', { name: /Informática/ });
  expect(within(region).getByRole('heading', { name: 'Pendientes 1' })).toBeInTheDocument();
  expect(within(region).getByRole('heading', { name: 'Resueltas 1' })).toBeInTheDocument();
  expect(within(region).getByRole('button', { name: 'Queja 1' }).closest('[data-specialty]')).toBeNull();
  expect(region.parentElement?.querySelector('[data-specialty="informatica"]')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /Informática/ }).closest('[data-specialty]')).toBeNull();
});

it('filtra por estado, mantiene contadores y mueve quejas cuando se actualizan', () => {
  const { rerender } = render(<ComplaintGroups quejas={[item(1, 'pendiente'), item(2, 'resuelta')]} specialtyName={specialtyName}>{list}</ComplaintGroups>);
  fireEvent.click(screen.getByRole('button', { name: 'Pendientes (1)' }));
  expect(screen.getByRole('button', { name: 'Queja 1' })).toBeVisible();
  expect(screen.queryByRole('button', { name: 'Queja 2' })).not.toBeInTheDocument();
  rerender(<ComplaintGroups quejas={[item(1, 'aceptada'), item(2, 'resuelta')]} specialtyName={specialtyName}>{list}</ComplaintGroups>);
  expect(screen.getByRole('button', { name: 'Pendientes (0)' })).toHaveAttribute('aria-pressed', 'true');
  expect(screen.getByText('No hay quejas en este estado.')).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: 'Aceptadas (1)' }));
  expect(screen.getByRole('button', { name: 'Queja 1' })).toBeVisible();
});

it('respeta estados históricos derivados de fechas y ordena las quejas recientes primero', () => {
  render(<ComplaintGroups quejas={[item(1, 'pendiente'), item(2, 'pendiente'), { ...item(3, undefined), revisadaEn: '2026-09-20' }]} specialtyName={specialtyName}>{list}</ComplaintGroups>);
  expect(screen.getByRole('button', { name: 'Revisadas (1)' })).toBeInTheDocument();
  const section = screen.getByRole('heading', { name: 'Pendientes 2' }).parentElement!;
  expect(within(section).getAllByRole('button').map(button => button.textContent)).toEqual(['Queja 2', 'Queja 1']);
});
