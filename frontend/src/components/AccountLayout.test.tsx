import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { expect, it, vi } from 'vitest';
import LauncherCards from './LauncherCards';
import DashboardWelcome from './DashboardWelcome';
import SectionNavigation from './SectionNavigation';
import { useAuth } from '../context/AuthContext';

vi.mock('../context/AuthContext', () => ({ useAuth: vi.fn() }));

it('la barra de apartados identifica la sección activa y conserva las acciones', () => {
  const action = vi.fn();
  render(<MemoryRouter><SectionNavigation label="Apartados" active="users" sections={[
    { key: 'users', label: 'Usuarios', href: '/admin/usuarios' },
    { key: 'plans', label: 'Planes', onSelect: action },
  ]} /></MemoryRouter>);
  expect(screen.getByRole('navigation', { name: 'Apartados' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Usuarios' })).toHaveAttribute('aria-current', 'page');
  expect(screen.getByRole('button', { name: 'Planes' })).not.toHaveAttribute('aria-current');
  fireEvent.click(screen.getByRole('button', { name: 'Planes' }));
  expect(action).toHaveBeenCalledOnce();
});

it('conserva enlaces reales en los accesos administrativos y acciones en los demás', () => {
  const action = vi.fn();
  render(<MemoryRouter><LauncherCards options={[
    { key: 'users', title: 'Usuarios', description: 'Gestión de cuentas', href: '/admin/usuarios', icon: <span /> },
    { key: 'plans', title: 'Revisar planes', description: 'Planes pendientes', onSelect: action, icon: <span /> },
  ]} /></MemoryRouter>);
  expect(screen.getByRole('link', { name: 'Usuarios' })).toHaveAttribute('href', '/admin/usuarios');
  fireEvent.click(screen.getByRole('button', { name: 'Revisar planes' }));
  expect(action).toHaveBeenCalledTimes(1);
});

it.each([
  [2, 'Panel de Evaluación'], [3, 'Panel general'], [4, 'Notas de mis hijos'], [5, 'Coordinación Pedagógica'],
])('comparte la bienvenida manteniendo el contexto de la cuenta %s', (level, title) => {
  vi.mocked(useAuth).mockReturnValue({ user: { level, displayName: 'Ana Pérez' } } as ReturnType<typeof useAuth>);
  render(<DashboardWelcome title={title} specialty="Informática" />);
  expect(screen.getByRole('heading', { name: 'Hola, Ana' })).toBeInTheDocument();
  expect(screen.getByText(new RegExp(title))).toBeInTheDocument();
  expect(screen.getByRole('img', { name: 'Especialidad Informática' })).toBeInTheDocument();
});

it('no inventa un nombre mientras la identidad se carga', () => {
  vi.mocked(useAuth).mockReturnValue({ user: { level: 3 } } as ReturnType<typeof useAuth>);
  render(<DashboardWelcome title="Panel general" />);
  expect(screen.getByRole('heading', { name: 'Hola' })).toBeInTheDocument();
});
