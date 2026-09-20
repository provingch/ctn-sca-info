import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { TemaPlanDto } from '../api/planCurricular';
import TemasPorMesAccordion from './TemasPorMesAccordion';

const temas: TemaPlanDto[] = [
  { mes: 'Marzo', ordenMes: 1, bloque: 1, temasContenidos: 'Unidad 1 Sistemas', estadoCobertura: 'CUBIERTO', fechaCobertura: '2026-03-20 10:00:00' },
  { mes: 'Marzo', ordenMes: 1, bloque: 2, temasContenidos: 'Unidad 2 Redes', estadoCobertura: 'PENDIENTE' },
];

describe('TemasPorMesAccordion', () => {
  it('sin mostrarCobertura no agrega la columna (un plan en revisión no tiene cobertura)', () => {
    render(<TemasPorMesAccordion temas={temas} />);
    expect(screen.queryByRole('columnheader', { name: 'Cumplimiento' })).not.toBeInTheDocument();
    expect(screen.queryByText('Pendiente')).not.toBeInTheDocument();
  });

  it('con la etapa corriendo: cubierto muestra su fecha y el resto sigue Pendiente', () => {
    render(<TemasPorMesAccordion temas={temas} mostrarCobertura etapaCerrada={false} />);
    expect(screen.getByRole('columnheader', { name: 'Cumplimiento' })).toBeInTheDocument();
    const cubierto = screen.getByText('Unidad 1 Sistemas').closest('tr') as HTMLElement;
    expect(cubierto).toHaveTextContent(/20\/3\/26|20\/03\/26|20\/3\/2026/);
    const pendiente = screen.getByText('Unidad 2 Redes').closest('tr') as HTMLElement;
    expect(pendiente).toHaveTextContent('Pendiente');
    expect(screen.queryByText('No cumplido')).not.toBeInTheDocument();
  });

  it('con la etapa cerrada: lo que quedó sin cubrir pasa a No cumplido y lo cubierto no cambia', () => {
    render(<TemasPorMesAccordion temas={temas} mostrarCobertura etapaCerrada />);
    const pendiente = screen.getByText('Unidad 2 Redes').closest('tr') as HTMLElement;
    expect(pendiente).toHaveTextContent('No cumplido');
    expect(pendiente).not.toHaveTextContent('Pendiente');
    const cubierto = screen.getByText('Unidad 1 Sistemas').closest('tr') as HTMLElement;
    expect(cubierto).not.toHaveTextContent('No cumplido');
  });
});
