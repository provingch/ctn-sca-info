import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, expect, it } from 'vitest';
import { SpecialtyProvider } from '../../context/SpecialtyContext';
import SystemPalette from './SystemPalette';

beforeEach(() => sessionStorage.clear());
it('descarta una paleta antigua con texto mal codificado', async () => {
  sessionStorage.setItem('sca-session-specialty', JSON.stringify({ id: 1, name: 'Electr\u00c3\u00b3nica' }));
  render(<SpecialtyProvider><SystemPalette especialidades={[{ id: 1, nombre: 'Electrónica' }]} /></SpecialtyProvider>);
  expect(screen.getByRole('button', { name: 'Paleta del sistema' })).toHaveTextContent('Institucional');
  await waitFor(() => expect(sessionStorage.getItem('sca-session-specialty')).toBeNull());
});
it('aplica la paleta a la sesión y permite restaurar la institucional', async () => {
  render(<SpecialtyProvider><SystemPalette especialidades={[{ id: 1, nombre: 'Informática' }]} /></SpecialtyProvider>);
  fireEvent.click(screen.getByRole('button', { name: 'Paleta del sistema' }));
  fireEvent.click(screen.getByRole('option', { name: 'Informática' }));
  await waitFor(() => expect(document.documentElement.dataset.specialty).toBe('informatica'));
  expect(JSON.parse(sessionStorage.getItem('sca-session-specialty')!)).toEqual({ id: 1, name: 'Informática' });
  fireEvent.click(screen.getByRole('button', { name: 'Paleta del sistema' }));
  fireEvent.click(screen.getByRole('option', { name: 'Institucional (predeterminada)' }));
  await waitFor(() => expect(document.documentElement.dataset.specialty).toBe('general'));
  expect(sessionStorage.getItem('sca-session-specialty')).toBeNull();
});
