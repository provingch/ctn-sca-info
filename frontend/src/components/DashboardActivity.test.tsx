import { render, screen, waitFor } from '@testing-library/react';
import { expect, it, vi } from 'vitest';
import { getProfile } from '../api/profile';
import DashboardActivity from './DashboardActivity';

vi.mock('../api/profile', () => ({ getProfile: vi.fn() }));
const profile = (allowed: boolean, activityLog: string[]) => ({ showActivityPanel: allowed, activityLog } as Awaited<ReturnType<typeof getProfile>>);

it.each([{ entries: [] }, { entries: ['[2026-09-20 10:00:00] Inició sesión'] }])('no reserva un panel para actividad vacía o solo accesos', async ({ entries }) => {
  vi.mocked(getProfile).mockResolvedValue(profile(true, entries));
  const { container } = render(<DashboardActivity />);
  await waitFor(() => expect(getProfile).toHaveBeenCalled());
  expect(container).toBeEmptyDOMElement();
});

it('muestra acciones reales sin divulgar la IP del registro', async () => {
  vi.mocked(getProfile).mockResolvedValue(profile(true, ['[2026-09-20 10:00:00] (ip: 127.0.0.1) Revisó un plan curricular']));
  render(<DashboardActivity />);
  expect(await screen.findByText('Revisó un plan curricular')).toBeInTheDocument();
  expect(screen.getByText('20/09/2026 · 10:00')).toBeInTheDocument();
  expect(screen.queryByText(/127\.0\.0\.1/)).not.toBeInTheDocument();
});

it('respeta la visibilidad del perfil aunque existan registros', async () => {
  vi.mocked(getProfile).mockResolvedValue(profile(false, ['[2026-09-20 10:00:00] Revisó un plan curricular']));
  const { container } = render(<DashboardActivity />);
  await waitFor(() => expect(getProfile).toHaveBeenCalled());
  expect(container).toBeEmptyDOMElement();
});
