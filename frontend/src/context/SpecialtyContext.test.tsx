import { act, render, screen } from '@testing-library/react';
import { beforeEach, expect, it } from 'vitest';
import { SpecialtyProvider, useSpecialty } from './SpecialtyContext';

let api: ReturnType<typeof useSpecialty>;
function Probe() {
  api = useSpecialty();
  return <output>{api.name ?? 'institucional'}</output>;
}

beforeEach(() => {
  sessionStorage.clear();
  delete document.documentElement.dataset.specialty;
});

it('arranca en el color institucional y no persiste nada en la sesión', () => {
  render(<SpecialtyProvider><Probe /></SpecialtyProvider>);
  expect(document.documentElement.dataset.specialty).toBe('general');
  act(() => api.selectSpecialty('Informática', 1));
  expect(document.documentElement.dataset.specialty).toBe('informatica');
  expect(sessionStorage.length).toBe(0);
});

it('con una única especialidad, las pantallas generales usan su color', () => {
  render(<SpecialtyProvider><Probe /></SpecialtyProvider>);
  act(() => api.setBaseSpecialty('Electrónica', 3));
  expect(screen.getByRole('status')).toHaveTextContent('Electrónica');
  expect(document.documentElement.dataset.specialty).toBe('electronica');
  act(() => api.resetSpecialty());
  expect(document.documentElement.dataset.specialty).toBe('electronica');
});

it('con varias especialidades, el apartado cambia el color y al salir vuelve al institucional', () => {
  render(<SpecialtyProvider><Probe /></SpecialtyProvider>);
  act(() => api.setBaseSpecialty(null));
  act(() => api.selectSpecialty('Informática', 1));
  expect(document.documentElement.dataset.specialty).toBe('informatica');
  act(() => api.selectSpecialty('Construcciones Civiles', 2));
  expect(document.documentElement.dataset.specialty).toBe('construcciones');
  act(() => api.resetSpecialty());
  expect(document.documentElement.dataset.specialty).toBe('general');
});

it('un apartado de otra especialidad se impone al color base y luego lo restaura', () => {
  render(<SpecialtyProvider><Probe /></SpecialtyProvider>);
  act(() => api.setBaseSpecialty('Electrónica', 3));
  act(() => api.selectSpecialty('Informática', 1));
  expect(document.documentElement.dataset.specialty).toBe('informatica');
  act(() => api.resetSpecialty());
  expect(document.documentElement.dataset.specialty).toBe('electronica');
});
