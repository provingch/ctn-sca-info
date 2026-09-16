import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import PageBanner from './PageBanner';

describe('PageBanner', () => {
  it('renderiza el título como encabezado principal, sin contexto ni selector si no vienen', () => {
    render(<PageBanner title="Panel SCA del curso" />);
    expect(screen.getByRole('heading', { level: 1, name: 'Panel SCA del curso' })).toBeInTheDocument();
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
  });

  it('muestra la línea de contexto y el selector cuando se pasan', () => {
    render(<PageBanner title="Hola, Ana" context="Martes · 3 cursos" selector={<select aria-label="Especialidad"><option>Informática</option></select>} />);
    expect(screen.getByText('Martes · 3 cursos')).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: 'Especialidad' })).toBeInTheDocument();
  });
});
