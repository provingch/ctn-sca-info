import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, useLocation } from 'react-router-dom';
import ClassHistoryView from './ClassHistoryView';
import { ToastProvider } from '../../context/ToastContext';
import { getMiClase, getMisClases, listarCodigosConducta, updateClase, type ClaseDadaDto, type ClaseDetalleDto } from '../../api/home';
import { classDate, filterClassHistory, isLate } from './classHistoryUtils';

vi.mock('../../api/home', () => ({ getMiClase: vi.fn(), getMisClases: vi.fn(), listarCodigosConducta: vi.fn(), updateClase: vi.fn(), updateAttendance: vi.fn() }));
const lesson = (overrides: Partial<ClaseDadaDto> = {}): ClaseDadaDto => ({
  id: 1, fechaClase: '2026-09-17', createdAt: '2026-09-17T08:00:00', tema: 'Proyección ortogonal', cursoId: 5,
  cursoDescripcion: 'Informática 2026 A', asignacionId: 1, materiaNombre: 'Dibujo Técnico', profesorId: 1,
  profesorNombre: 'Profesor', especialidadId: 2, especialidadNombre: 'Informática',
  totalAlumnos: 2, totalAusentes: 0, totalJustificados: 1, totalPresentes: 1, totalPendientes: 0, ...overrides,
});
const catalog = [{ id: 7, codigo: 'N7', descripcion: 'No utiliza el uniforme establecido', activo: true }, { id: 8, codigo: 'N8', descripcion: 'Llegada tardía', activo: true }];
const detail = (): ClaseDetalleDto => ({ id: 1, fechaClase: new Date().toISOString().slice(0, 10), tema: 'Proyección ortogonal', cursoId: 5, asistencias: [
  { id: 11, alumnoId: 1, alumnoNombreCompleto: 'ANA MARÍA PÉREZ GÓMEZ', alumnoNombre: 'ANA MARÍA', alumnoApellido: 'PÉREZ GÓMEZ', estado: 'presente', faltaCodigo: null, faltaObservacion: null, codigos: ['N7'] },
  { id: 12, alumnoId: 2, alumnoNombreCompleto: 'ANA BENÍTEZ', alumnoNombre: 'ANA', alumnoApellido: 'BENÍTEZ', estado: 'ausente_justificado', faltaCodigo: 'SALUD', faltaObservacion: 'Certificado', codigos: [] },
] });
function Location() { return <output data-testid="url">{useLocation().search}</output>; }
function show(url = '/home?view=mis-clases') {
  return render(<ToastProvider><MemoryRouter initialEntries={[url]}><Location /><ClassHistoryView /></MemoryRouter></ToastProvider>);
}
beforeEach(() => {
  vi.resetAllMocks();
  vi.mocked(getMisClases).mockResolvedValue([lesson()]);
  vi.mocked(getMiClase).mockResolvedValue(detail());
  vi.mocked(listarCodigosConducta).mockResolvedValue(catalog);
  vi.mocked(updateClase).mockResolvedValue(undefined);
});

describe('Historial y detalle de clases', () => {
  it('filtra sin tildes por campos separados y desempata por hora sin mutar la lista', () => {
    const items = [lesson(), lesson({ id: 2, createdAt: '2026-09-17T09:00:00' }), lesson({ id: 3, cursoId: 8, especialidadId: 4, especialidadNombre: 'Química' })];
    expect(filterClassHistory(items, 'PROYECCION', '5', '2', 'desc').map(x => x.id)).toEqual([2, 1]);
    expect(filterClassHistory(items, 'informatica', '5', '2', 'asc').map(x => x.id)).toEqual([1, 2]);
    expect(items.map(x => x.id)).toEqual([1, 2, 3]);
    expect(classDate('2026-09-17')).toBe('jue 17/09/2026');
    expect(isLate(['N7'], catalog)).toBe(false);
    expect(isLate(['N8'], catalog)).toBe(true);
  });
  it('recupera filtros desde la URL y limpia sin perder la vista actual', async () => {
    show('/home?view=mis-clases&q=noexiste&curso=5&orden=asc');
    await screen.findByText('No hay clases que coincidan con tu búsqueda');
    expect(screen.getByRole('searchbox')).toHaveValue('noexiste');
    fireEvent.click(screen.getByRole('button', { name: 'Limpiar filtros' }));
    await screen.findByRole('button', { name: /Ver clase:/ });
    expect(screen.getByTestId('url')).toHaveTextContent('?view=mis-clases');
    fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'otra' } });
    expect(screen.getByRole('button', { name: /Ver clase:/ })).toBeInTheDocument();
    await screen.findByText('No hay clases que coincidan con tu búsqueda');
    expect(screen.getByTestId('url')).toHaveTextContent('q=otra');
  });
  it('muestra 24 tarjetas y carga las restantes a pedido', async () => {
    vi.mocked(getMisClases).mockResolvedValue(Array.from({ length: 30 }, (_, i) => lesson({ id: i + 1 })));
    show();
    await screen.findByText('Mostrando 24 de 30 clases');
    expect(screen.getAllByRole('button', { name: /Ver clase:/ })).toHaveLength(24);
    fireEvent.click(screen.getByRole('button', { name: 'Cargar más clases' }));
    expect(screen.getAllByRole('button', { name: /Ver clase:/ })).toHaveLength(30);
    expect(screen.queryByRole('button', { name: 'Cargar más clases' })).not.toBeInTheDocument();
  });
  it('muestra nombres, rasgos reales y resumen; editar tema no reescribe justificadas', async () => {
    show(); fireEvent.click(await screen.findByRole('button', { name: /Ver clase:/ }));
    const dialog = within(screen.getByRole('dialog'));
    await dialog.findByText('Pérez Gómez, Ana María');
    expect(dialog.getByText('Informática 2026 A · Dibujo Técnico')).toBeInTheDocument();
    expect(dialog.getByText('N7 · No utiliza el uniforme establecido')).toBeInTheDocument();
    expect(dialog.queryByText('Presente · tarde')).not.toBeInTheDocument();
    expect(dialog.getByText('Justificados: 1')).toBeInTheDocument();
    expect(dialog.queryByRole('combobox')).not.toBeInTheDocument();
    fireEvent.click(dialog.getByRole('button', { name: 'Editar' }));
    expect(dialog.getByLabelText('Estado de Benítez, Ana')).toHaveValue('ausente_justificado');
    fireEvent.change(dialog.getByLabelText('Tema de la clase'), { target: { value: 'Nuevo tema' } });
    fireEvent.click(dialog.getByRole('button', { name: 'Guardar cambios' }));
    await waitFor(() => expect(updateClase).toHaveBeenCalledWith(1, { tema: 'Nuevo tema', asistencias: [] }));
  });
  it('agrega y quita rasgos sin cambiar el estado justificado, conserva borrador si falla', async () => {
    vi.mocked(updateClase).mockRejectedValue(new Error('offline'));
    show(); fireEvent.click(await screen.findByRole('button', { name: /Ver clase:/ }));
    fireEvent.click(await screen.findByRole('button', { name: 'Editar' }));
    fireEvent.change(screen.getByLabelText('Agregar rasgo a Benítez, Ana'), { target: { value: 'N8' } });
    fireEvent.click(screen.getByLabelText('Quitar N7 de Pérez Gómez, Ana María'));
    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));
    await waitFor(() => expect(updateClase).toHaveBeenCalledWith(1, { tema: 'Proyección ortogonal', asistencias: [
      { asistenciaId: 11, estado: 'presente', codigos: [] }, { asistenciaId: 12, estado: 'ausente_justificado', codigos: ['N8'] },
    ] }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Guardar cambios' })).toBeEnabled());
    expect(screen.getByLabelText('Quitar N8 de Benítez, Ana')).toBeInTheDocument();
  });
  it('permite reintentar un detalle fallido', async () => {
    vi.mocked(getMiClase).mockRejectedValueOnce(new Error('offline'));
    show(); fireEvent.click(await screen.findByRole('button', { name: /Ver clase:/ }));
    await screen.findByText('No se pudo cargar el detalle');
    fireEvent.click(screen.getByRole('button', { name: 'Reintentar' }));
    await screen.findByText('Pérez Gómez, Ana María');
  });
});
