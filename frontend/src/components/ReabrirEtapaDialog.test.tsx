import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api/client';
import ReabrirEtapaDialog from './ReabrirEtapaDialog';

function abrir(onConfirm = vi.fn().mockResolvedValue(undefined), onCancel = vi.fn()) {
  render(<ReabrirEtapaDialog etapa={2} onCancel={onCancel} onConfirm={onConfirm} />);
  return { onConfirm, onCancel };
}

describe('ReabrirEtapaDialog', () => {
  it('no se puede confirmar sin motivo, ni con sólo espacios', () => {
    abrir();
    const confirmar = screen.getByRole('button', { name: 'Reabrir Etapa 2' });
    expect(confirmar).toBeDisabled();
    fireEvent.change(screen.getByLabelText(/Motivo de la reapertura/), { target: { value: '   ' } });
    expect(confirmar).toBeDisabled();
  });

  it('confirma con el motivo sin espacios sobrantes', async () => {
    const { onConfirm } = abrir();
    fireEvent.change(screen.getByLabelText(/Motivo de la reapertura/), { target: { value: '  Se cerró por error  ' } });
    fireEvent.click(screen.getByRole('button', { name: 'Reabrir Etapa 2' }));
    await waitFor(() => expect(onConfirm).toHaveBeenCalledWith('Se cerró por error'));
  });

  it('si falla muestra el error, conserva el motivo y deja reintentar', async () => {
    const onConfirm = vi.fn().mockRejectedValue(new ApiError(400, 'Etapa 2 no está cerrada'));
    abrir(onConfirm);
    const campo = screen.getByLabelText(/Motivo de la reapertura/) as HTMLTextAreaElement;
    fireEvent.change(campo, { target: { value: 'Motivo válido' } });
    fireEvent.click(screen.getByRole('button', { name: 'Reabrir Etapa 2' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Etapa 2 no está cerrada');
    expect(campo.value).toBe('Motivo válido');
    expect(screen.getByRole('button', { name: 'Reabrir Etapa 2' })).toBeEnabled();
  });

  it('cancelar no confirma nada', () => {
    const { onConfirm, onCancel } = abrir();
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onConfirm).not.toHaveBeenCalled();
  });
});
