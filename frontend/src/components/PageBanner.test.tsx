import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
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

  it('no muestra volver si no se pasa onBack', () => {
    render(<PageBanner title="Panel" />);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('por defecto vuelve al inicio, como en el resto de la app', () => {
    const onBack = vi.fn();
    render(<PageBanner title="Panel" onBack={onBack} />);
    const back = screen.getByRole('button', { name: 'Volver al inicio' });
    expect(back).toHaveTextContent('← Inicio');
    fireEvent.click(back);
    expect(onBack).toHaveBeenCalledTimes(1);
  });

  it('con backLabel dice a dónde vuelve, en el texto y en el nombre accesible', () => {
    render(<PageBanner title="Quejas por Profesor" onBack={() => {}} backLabel="Coordinación" />);
    expect(screen.getByRole('button', { name: 'Volver a Coordinación' })).toHaveTextContent('← Coordinación');
  });
});
