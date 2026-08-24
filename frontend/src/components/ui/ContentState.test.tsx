import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import ContentState from './ContentState';

describe('ContentState', () => {
  it('expone la carga como un estado ocupado', () => {
    render(<ContentState tone="loading" title="Cargando planilla…" detail="Esperá un momento." />);

    expect(screen.getByRole('status')).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('heading', { name: 'Cargando planilla…' })).toBeInTheDocument();
  });

  it('anuncia los errores con prioridad', () => {
    render(<ContentState tone="error" title="No se pudo cargar" detail="Intentá nuevamente." />);

    expect(screen.getByRole('alert')).toHaveAttribute('aria-live', 'assertive');
  });
});
