import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, expect, it, vi } from 'vitest';
import SalasPanel from './SalasPanel';
import { createSala, deleteSala, getSalas, updateSala, type AdminCatalog, type SalaItem } from '../../api/admin';
import { ApiError } from '../../api/client';

vi.mock('../../api/admin', () => ({ createSala: vi.fn(), deleteSala: vi.fn(), getSalas: vi.fn(), updateSala: vi.fn() }));
const data: AdminCatalog = { especialidades: [{ id: 1, nombre: 'Electrónica' }, { id: 2, nombre: 'Informática' }], usuarios: [], materias: [], asignaciones: [], alumnos: [], cursos: [], cursosAlumnos: [], egresados: [] };
const rooms: SalaItem[] = [{ id: 1, nombre: 'PC 01', especialidadId: null, especialidadNombre: null, bloquesAsignados: 0 }, ...Array.from({ length: 11 }, (_, index) => ({ id: index + 2, nombre: `S${index + 1}`, especialidadId: 1, especialidadNombre: 'Electrónica', bloquesAsignados: index === 3 ? 8 : 0 }))];
beforeEach(() => { vi.resetAllMocks(); vi.mocked(getSalas).mockResolvedValue(rooms); });
function show() { return render(<SalasPanel data={data} status={vi.fn()} />); }
async function openElectronics() { fireEvent.click(await screen.findByRole('button', { name: /Electrónica.*11 salas/ })); }

it('muestra cajas con conteos, especialidades vacías y paginación con regreso', async () => {
  show();
  expect(await screen.findByRole('button', { name: /Comunes.*1 sala/ })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /Informática.*0 salas/ })).toBeInTheDocument();
  expect(screen.queryByRole('textbox', { name: 'Nombre' })).not.toBeInTheDocument();
  await openElectronics();
  expect(screen.getByRole('navigation', { name: 'Ubicación en salas' })).toHaveTextContent('Salas›Electrónica');
  expect(screen.getByRole('status')).toHaveTextContent('1–10 de 11 salas');
  expect(screen.getByText('8 bloques asignados')).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }));
  expect(screen.getByRole('rowheader')).toHaveTextContent('S11');
  fireEvent.click(screen.getByRole('button', { name: '← Volver a especialidades' }));
  expect(screen.getByRole('button', { name: /Comunes.*Ver salas/ })).toBeInTheDocument();
});
it('busca globalmente y abre el grupo de una coincidencia', async () => {
  show(); await openElectronics();
  fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'pc 01' } });
  expect(screen.getByRole('rowheader')).toHaveTextContent('PC 01');
  fireEvent.click(screen.getByRole('button', { name: 'Ver grupo de PC 01' }));
  expect(screen.getByRole('navigation', { name: 'Ubicación en salas' })).toHaveTextContent('Comunes');
  fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'no existe' } });
  expect(screen.getByText('No hay salas que coincidan con la búsqueda.')).toBeInTheDocument();
});
it('abre el alta en un modal con especialidad preseleccionada y conserva errores', async () => {
  show(); await openElectronics();
  fireEvent.click(screen.getByRole('button', { name: '＋ Agregar sala' }));
  expect(screen.getByRole('button', { name: 'Especialidad o pabellón de la sala' })).toHaveTextContent('Electrónica');
  expect(screen.getByPlaceholderText('Ej.: S01')).toBeInTheDocument();
  fireEvent.change(screen.getByRole('textbox', { name: 'Nombre' }), { target: { value: ' S12 ' } });
  vi.mocked(createSala).mockRejectedValue(new ApiError(409, 'Ya existe esa sala'));
  fireEvent.click(screen.getByRole('button', { name: 'Guardar sala' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe esa sala');
  expect(createSala).toHaveBeenCalledWith({ nombre: 'S12', especialidadId: 1 });
  expect(screen.getByRole('dialog')).toBeInTheDocument();
});
it('edita una sala existente sin renombrar otras y cierra al guardar', async () => {
  show(); await openElectronics();
  fireEvent.click(screen.getByRole('button', { name: 'Editar S1' }));
  fireEvent.change(screen.getByRole('textbox', { name: 'Nombre' }), { target: { value: 'S01' } });
  fireEvent.click(screen.getByRole('button', { name: 'Guardar sala' }));
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  expect(updateSala).toHaveBeenCalledWith(2, { nombre: 'S01', especialidadId: 1 });
});
it('bloquea eliminar salas en uso y exige confirmar las que no tienen bloques', async () => {
  show(); await openElectronics();
  fireEvent.click(screen.getByRole('button', { name: 'Eliminar S4' }));
  expect(screen.getByRole('alert')).toHaveTextContent('8 bloques');
  expect(screen.getByRole('button', { name: 'Eliminar sala' })).toBeDisabled();
  fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
  expect(deleteSala).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole('button', { name: 'Eliminar S1' }));
  expect(screen.getByRole('dialog')).toHaveTextContent('Esta acción no se puede deshacer.');
  expect(deleteSala).not.toHaveBeenCalled();
  fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Eliminar sala' }));
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  expect(deleteSala).toHaveBeenCalledTimes(1);
  expect(deleteSala).toHaveBeenCalledWith(2);
});
it('muestra el conflicto si la sala pasó a usarse antes de confirmar', async () => {
  vi.mocked(deleteSala).mockRejectedValue(new ApiError(409, 'La sala tiene bloques de horario asignados.'));
  show(); await openElectronics();
  fireEvent.click(screen.getByRole('button', { name: 'Eliminar S1' }));
  fireEvent.click(screen.getByRole('button', { name: 'Eliminar sala' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('La sala tiene bloques');
  expect(screen.getByRole('button', { name: 'Eliminar sala' })).toBeDisabled();
});
it('no confunde un error de carga con un catálogo vacío', async () => {
  vi.mocked(getSalas).mockRejectedValueOnce(new Error('offline'));
  show();
  fireEvent.click(await screen.findByRole('button', { name: 'Reintentar' }));
  expect(await screen.findByRole('button', { name: /Electrónica.*11 salas/ })).toBeInTheDocument();
});
