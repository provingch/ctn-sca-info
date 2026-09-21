import { render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import AboutPage from './AboutPage';
vi.mock('../../context/AuthContext', () => ({ useAuth: () => ({ user: null }) }));
vi.mock('../../context/SpecialtyContext', () => ({ useSpecialty: () => ({ name: null }) }));

function renderPage() {
  return render(<MemoryRouter><AboutPage /></MemoryRouter>);
}

describe('AboutPage', () => {
  afterEach(() => vi.useRealTimers());

  it('lista a cada integrante con su nombre y un link a su GitHub que abre en pestaña nueva', () => {
    renderPage();
    const equipo = screen.getByRole('heading', { name: 'Equipo' }).closest('section')!;
    const links = within(equipo).getAllByRole('link');

    expect(links.map((link) => link.getAttribute('href'))).toEqual([
      'https://github.com/provingch',
      'https://github.com/Sh1b0',
      'https://github.com/schmidtsamuel626',
      'https://github.com/tukp5678',
    ]);
    links.forEach((link) => {
      expect(link).toHaveAttribute('target', '_blank');
      expect(link.getAttribute('rel')).toContain('noopener');
    });
    ['Thiago Estigarribia', 'Joshua Mongelos', 'Samuel Schmidt', 'Marcos Molinas'].forEach((name) => {
      expect(equipo).toHaveTextContent(name);
    });
  });

  it('muestra la cronología del CHANGELOG', () => {
    renderPage();
    expect(screen.getByText('Inicio del desarrollo')).toBeInTheDocument();
    expect(screen.getByText('18 de junio de 2026')).toHaveAttribute('datetime', '2026-06-18');
    expect(screen.getByText('Propuesta aceptada')).toBeInTheDocument();
    expect(screen.getByText('Fusión de dos proyectos en uno')).toBeInTheDocument();
  });

  it('calcula el año del copyright con la fecha actual en vez de dejarlo fijo', () => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date('2031-03-01T12:00:00Z'));
    renderPage();
    expect(screen.getByText(/© 2031 Colegio Técnico Nacional de Asunción/)).toBeInTheDocument();
  });

  it('vuelve al inicio', () => {
    renderPage();
    expect(screen.getByRole('link', { name: '← Volver al inicio' })).toHaveAttribute('href', '/');
  });
});
