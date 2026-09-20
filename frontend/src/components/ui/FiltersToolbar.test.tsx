import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import FiltersToolbar, { FilterField } from './FiltersToolbar';

function barra(props: Partial<Parameters<typeof FiltersToolbar>[0]> = {}) {
  const onLimpiar = vi.fn();
  render(<FiltersToolbar ariaLabel="Filtros de notas" mostrando={2} total={5} unidad="notas" activo={false} onLimpiar={onLimpiar} {...props}>
    <FilterField label="Buscar"><input type="search" /></FilterField>
  </FiltersToolbar>);
  return { onLimpiar };
}

describe('FiltersToolbar', () => {
  it('muestra el conteo con su unidad y agrupa los controles en una región de búsqueda', () => {
    barra();
    expect(screen.getByRole('search', { name: 'Filtros de notas' })).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('2 de 5 notas');
    expect(screen.getByLabelText('Buscar')).toBeInTheDocument();
  });

  it('sin filtros activos no ofrece limpiar', () => {
    barra({ activo: false });
    expect(screen.queryByRole('button', { name: 'Limpiar filtros' })).not.toBeInTheDocument();
  });

  it('con filtros activos ofrece limpiar y avisa al usarlo', () => {
    const { onLimpiar } = barra({ activo: true });
    fireEvent.click(screen.getByRole('button', { name: 'Limpiar filtros' }));
    expect(onLimpiar).toHaveBeenCalledTimes(1);
  });
});
