import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { Anotacion } from '../api/anotaciones';
import AnotacionesList from './AnotacionesList';

const base: Anotacion = {
  id: 1, alumnoId: 5, alumnoNombre: 'Ana', alumnoApellido: 'Gómez', curso: 'Informática · 2° B',
  fechaHora: '2026-10-21 07:30:00', tipo: 'No trae corbata', observacion: null, materia: 'Programación', profesor: 'Luis Pérez', tieneFoto: true,
};

describe('AnotacionesList', () => {
  it('pide la foto solo de las anotaciones que la tienen y la muestra', async () => {
    globalThis.URL.createObjectURL = vi.fn(() => 'blob:foto-1');
    globalThis.URL.revokeObjectURL = vi.fn();
    const loadFoto = vi.fn(() => Promise.resolve(new Blob(['x'], { type: 'image/jpeg' })));
    render(<AnotacionesList anotaciones={[base, { ...base, id: 2, tipo: 'Llegó tarde', tieneFoto: false }]} loadFoto={loadFoto} mostrarAlumno />);

    expect(await screen.findByRole('img', { name: 'Evidencia de la anotación: No trae corbata' })).toHaveAttribute('src', 'blob:foto-1');
    expect(loadFoto).toHaveBeenCalledTimes(1);
    expect(screen.getAllByText('Gómez, Ana')).toHaveLength(2);
    expect(screen.getByText('Llegó tarde')).toBeInTheDocument();
  });

  it('si la foto falla muestra un aviso en su lugar', async () => {
    render(<AnotacionesList anotaciones={[base]} loadFoto={() => Promise.reject(new Error('404'))} />);
    expect(await screen.findByText('Foto no disponible')).toBeInTheDocument();
    expect(screen.queryByText('Gómez, Ana')).not.toBeInTheDocument();
  });

  it('con fotoAlAmpliar no descarga la evidencia hasta que se pide', async () => {
    globalThis.URL.createObjectURL = vi.fn(() => 'blob:foto-1');
    globalThis.URL.revokeObjectURL = vi.fn();
    const loadFoto = vi.fn(() => Promise.resolve(new Blob(['x'], { type: 'image/jpeg' })));
    render(<AnotacionesList anotaciones={[base, { ...base, id: 2, tipo: 'Llegó tarde', tieneFoto: false }]} loadFoto={loadFoto} fotoAlAmpliar />);

    expect(loadFoto).not.toHaveBeenCalled();
    expect(screen.getAllByRole('button', { name: 'Ver evidencia' })).toHaveLength(1);
    fireEvent.click(screen.getByRole('button', { name: 'Ver evidencia' }));
    expect(await screen.findByRole('img', { name: 'Evidencia de la anotación: No trae corbata' })).toBeInTheDocument();
    expect(loadFoto).toHaveBeenCalledTimes(1);
    fireEvent.click(screen.getByRole('button', { name: 'Ocultar evidencia' }));
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });
});
