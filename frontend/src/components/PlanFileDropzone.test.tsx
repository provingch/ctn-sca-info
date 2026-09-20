import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import PlanFileDropzone from './PlanFileDropzone';

describe('Zona de carga de planes', () => {
  it('selecciona desde el explorador o al arrastrar y permite quitar el archivo', () => {
    const onChange = vi.fn(), onError = vi.fn();
    const file = new File(['excel'], 'plan.xlsx');
    const { rerender } = render(<PlanFileDropzone file={null} disabled={false} onChange={onChange} onError={onError} />);
    fireEvent.change(screen.getByLabelText('Seleccionar archivo del plan curricular'), { target: { files: [file] } });
    expect(onChange).toHaveBeenLastCalledWith(file);
    fireEvent.drop(screen.getByRole('button'), { dataTransfer: { files: [file] } });
    expect(onChange).toHaveBeenCalledTimes(2);
    rerender(<PlanFileDropzone file={file} disabled={false} onChange={onChange} onError={onError} />);
    expect(screen.getByText('plan.xlsx')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Quitar archivo' }));
    expect(onChange).toHaveBeenLastCalledWith(null);
  });
  it('rechaza formatos incompatibles, vacíos y múltiples; bloquea durante la subida', () => {
    const onChange = vi.fn(), onError = vi.fn();
    const { rerender } = render(<PlanFileDropzone file={null} disabled={false} onChange={onChange} onError={onError} />);
    for (const files of [[new File(['pdf'], 'plan.pdf')], [new File([], 'plan.xlsx')], [new File(['a'], 'a.xlsx'), new File(['b'], 'b.xlsx')]]) {
      fireEvent.drop(screen.getByRole('button'), { dataTransfer: { files } });
    }
    expect(onError).toHaveBeenCalledTimes(3);
    expect(onChange).not.toHaveBeenCalled();
    rerender(<PlanFileDropzone file={null} disabled onChange={onChange} onError={onError} />);
    fireEvent.drop(screen.getByRole('button'), { dataTransfer: { files: [new File(['a'], 'plan.xlsx')] } });
    expect(onChange).not.toHaveBeenCalled();
    expect(screen.getByRole('button')).toBeDisabled();
  });
});
