import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { expect, it, vi } from 'vitest';
import { descargarDocumentoOriginal } from '../api/planCurricular';
import { ToastProvider } from '../context/ToastContext';
import PlanOriginalDownload from './PlanOriginalDownload';
vi.mock('../api/planCurricular', () => ({ descargarDocumentoOriginal: vi.fn() }));
it('descarga el plan seleccionado, informa errores y permite reintentar', async () => {
  vi.mocked(descargarDocumentoOriginal).mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce('plan.xlsx');
  render(<ToastProvider><PlanOriginalDownload id={42} /></ToastProvider>);
  fireEvent.click(screen.getByRole('button', { name: 'Descargar archivo original' }));
  await screen.findByText('No se pudo descargar el archivo original.');
  await waitFor(() => expect(screen.getByRole('button', { name: 'Descargar archivo original' })).toBeEnabled());
  fireEvent.click(screen.getByRole('button', { name: 'Descargar archivo original' }));
  await waitFor(() => expect(descargarDocumentoOriginal).toHaveBeenCalledTimes(2));
  expect(descargarDocumentoOriginal).toHaveBeenLastCalledWith(42);
});
