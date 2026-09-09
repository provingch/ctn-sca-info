import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MateriasCatalog from './MateriasCatalog';
import { deleteAdminRecord, type AdminCatalog } from '../../api/admin';
import { ApiError } from '../../api/client';
vi.mock('../../api/admin', () => ({ deleteAdminRecord: vi.fn() }));
const data: AdminCatalog = {
  materias: Array.from({ length: 23 }, (_, i) => ({ id: i + 1, nombre: 'Materia ' + String(i + 1).padStart(2, '0'), categoria: i % 2 ? 'especifico' : 'comun', especialidadIds: i % 2 ? [1] : [1, 2] })),
  especialidades: [{ id: 1, nombre: 'Informática' }, { id: 2, nombre: 'Electricidad' }],
  asignaciones: [], usuarios: [], alumnos: [], cursos: [], cursosAlumnos: [],
};
const reload = vi.fn();
const status = vi.fn();
function renderCatalog(catalog = data) { return render(<MateriasCatalog data={catalog} reload={reload} status={status} onCreate={vi.fn()} onEdit={vi.fn()} />); }
function choose(label: string, option: string) {
  fireEvent.click(screen.getByRole('button', { name: label }));
  fireEvent.click(screen.getByRole('option', { name: option }));
}
beforeEach(() => { vi.resetAllMocks(); reload.mockResolvedValue(undefined); vi.mocked(deleteAdminRecord).mockResolvedValue(undefined); });
describe('Catálogo de materias', () => {
  it('pagina y reinicia la página al aplicar filtros combinados', () => {
    renderCatalog();
    expect(screen.getAllByRole('row')).toHaveLength(11);
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }));
    expect(screen.getByText('Página 2 de 3')).toBeInTheDocument();
    choose('Filtrar por tipo', 'Común');
    choose('Filtrar por especialidad', 'Electricidad');
    fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'materia 0' } });
    expect(screen.getByText('Página 1 de 1')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('5 de 23 materias');
    expect(screen.queryByText('Materia 02')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Limpiar filtros' }));
    expect(screen.getByRole('status')).toHaveTextContent('23 de 23 materias');
  });
  it('ordena toda la lista antes de paginar e indica la dirección', () => {
    renderCatalog();
    fireEvent.click(screen.getByRole('button', { name: 'Ordenar por nombre' }));
    expect(screen.getAllByRole('rowheader')[0]).toHaveTextContent('Materia 23');
    expect(screen.getByRole('button', { name: 'Ordenar por nombre' }).closest('th')).toHaveAttribute('aria-sort', 'descending');
    choose('Materias por página', '25');
    expect(screen.getAllByRole('rowheader')).toHaveLength(23);
  });
  it('muestra chips usando los colores existentes y un estado sin coincidencias', () => {
    renderCatalog();
    const first = screen.getAllByRole('row')[1];
    expect(within(first).getByText('Informática')).toHaveAttribute('data-specialty', 'informatica');
    expect(within(first).getByText('Electricidad')).toHaveAttribute('data-specialty', 'electricidad');
    fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'No existe' } });
    expect(screen.getByText('No hay coincidencias')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeDisabled();
  });
  it('busca nombres ignorando tildes y ajusta una página que queda vacía', () => {
    const view = renderCatalog();
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }));
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }));
    const shorter = { ...data, materias: [{ ...data.materias[0], nombre: 'Matemática' }] };
    view.rerender(<MateriasCatalog data={shorter} reload={reload} status={status} onCreate={vi.fn()} onEdit={vi.fn()} />);
    expect(screen.getByText('Página 1 de 1')).toBeInTheDocument();
    fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'matematica' } });
    expect(screen.getByRole('rowheader')).toHaveTextContent('Matemática');
  });
  it('cancelar la confirmación no elimina, confirmar envía una sola solicitud', async () => {
    renderCatalog();
    fireEvent.click(screen.getByRole('button', { name: 'Eliminar Materia 01' }));
    expect(screen.getByRole('dialog')).toHaveTextContent('Materia 01');
    expect(deleteAdminRecord).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Eliminar Materia 01' }));
    fireEvent.click(screen.getByRole('button', { name: 'Eliminar materia' }));
    await waitFor(() => expect(reload).toHaveBeenCalledTimes(1));
    expect(deleteAdminRecord).toHaveBeenCalledExactlyOnceWith('materias', 1);
  });
  it('bloquea materias con asignaciones y conserva el diálogo si el servidor rechaza', async () => {
    const view = renderCatalog({ ...data, asignaciones: [{ id: 1, materiaId: 1, profesorId: 2, cursoId: 3, profesor: 'Profesor', materia: 'Materia 01', curso: '1 A' }] });
    fireEvent.click(screen.getByRole('button', { name: 'Eliminar Materia 01' }));
    expect(screen.getByRole('button', { name: 'Eliminar materia' })).toBeDisabled();
    expect(screen.getByRole('alert')).toHaveTextContent('asignaciones vinculadas');
    view.unmount();
    vi.mocked(deleteAdminRecord).mockRejectedValue(new ApiError(409, 'La materia tiene planillas vinculadas.'));
    renderCatalog();
    fireEvent.click(screen.getByRole('button', { name: 'Eliminar Materia 01' }));
    fireEvent.click(screen.getByRole('button', { name: 'Eliminar materia' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('planillas vinculadas');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(reload).not.toHaveBeenCalled();
  });
});
