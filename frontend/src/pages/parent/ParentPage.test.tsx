import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getParentSummary, getRasgosConducta, type ParentResponse, type ParentSubject, type RasgoConducta } from '../../api/parent';
import ParentPage from './ParentPage';

vi.mock('../../components/AppShell', () => ({ default: ({ children }: { children: React.ReactNode }) => <div>{children}</div> }));
vi.mock('../../api/parent', () => ({ getParentSummary: vi.fn(), getRasgosConducta: vi.fn(), downloadReporteMensual: vi.fn(), downloadLibreta: vi.fn() }));

const materia = (planillaId: number, nombre: string): ParentSubject =>
  ({ planillaId, materiaId: planillaId, materia: nombre, etapa: 'primera', puntos: 80, total: 100, porcentaje: 80, nota: 4, tareas: [] });
const nombres = ['Matemática', 'Física', 'Química', 'Programación', 'Historia', 'Inglés', 'Guaraní'];
const resumen = (cantidad: number): ParentResponse => ({
  hijos: [{ id: 7, nombre: 'Camila', apellido: 'Rojas', especialidad: 'Informática', promedio: 80 }],
  selectedAlumnoId: 7, libretaDisponible: false,
  materias: nombres.slice(0, cantidad).map((nombre, index) => materia(index + 1, nombre)),
});
const nota = (materiaNombre: string | null, fechaClase: string | null, codigo: string): RasgoConducta =>
  ({ materia: materiaNombre, fechaClase, profesorNombre: 'Prof. Ruiz', codigo, descripcion: null, observacion: null });
const notas = [nota('Matemática', '2026-03-10', 'N1'), nota('Física', '2026-04-15', 'N2'), nota('Matemática', '2026-05-20', 'N3')];

async function mostrar(cantidadMaterias: number) {
  vi.mocked(getParentSummary).mockResolvedValue(resumen(cantidadMaterias));
  vi.mocked(getRasgosConducta).mockResolvedValue(notas);
  render(<ParentPage />);
  await screen.findByRole('heading', { name: 'Notas de conducta' });
  await screen.findByRole('search', { name: 'Filtros de notas de conducta' });
}

beforeEach(() => { vi.resetAllMocks(); });

describe('ParentPage — filtros', () => {
  it('con pocas materias no muestra el buscador, pero el toggle de etapa vive en la barra de filtros', async () => {
    await mostrar(3);
    const barra = screen.getByRole('search', { name: 'Filtros de materias' });
    expect(within(barra).getByRole('button', { name: 'Primera etapa' })).toBeInTheDocument();
    expect(within(barra).queryByRole('searchbox')).not.toBeInTheDocument();
    expect(within(barra).getByRole('status')).toHaveTextContent('3 de 3 materias publicadas');
  });

  it('con una lista larga el buscador filtra el grid por nombre, sin tildes, y se puede limpiar', async () => {
    await mostrar(7);
    const barra = screen.getByRole('search', { name: 'Filtros de materias' });

    fireEvent.change(within(barra).getByRole('searchbox'), { target: { value: 'FISICA' } });

    expect(within(barra).getByRole('status')).toHaveTextContent('1 de 7 materias publicadas');
    expect(screen.getByRole('button', { name: /Física/ })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Matemática/ })).not.toBeInTheDocument();

    fireEvent.change(within(barra).getByRole('searchbox'), { target: { value: 'zzz' } });
    expect(screen.getByText('Ninguna materia coincide')).toBeInTheDocument();

    fireEvent.click(within(barra).getByRole('button', { name: 'Limpiar filtros' }));
    expect(screen.getByRole('button', { name: /Matemática/ })).toBeInTheDocument();
    expect(within(barra).getByRole('status')).toHaveTextContent('7 de 7 materias publicadas');
  });

  it('las notas de conducta se filtran por materia y ofrecen limpiar', async () => {
    await mostrar(3);
    const barra = screen.getByRole('search', { name: 'Filtros de notas de conducta' });
    expect(within(barra).getByRole('status')).toHaveTextContent('3 de 3 notas');

    fireEvent.click(within(barra).getByRole('button', { name: 'Materia de la nota de conducta' }));
    fireEvent.click(screen.getByRole('option', { name: 'Física' }));

    expect(within(barra).getByRole('status')).toHaveTextContent('1 de 3 notas');
    expect(screen.getByText('N2')).toBeInTheDocument();
    expect(screen.queryByText('N1')).not.toBeInTheDocument();

    fireEvent.click(within(barra).getByRole('button', { name: 'Limpiar filtros' }));
    expect(screen.getByText('N1')).toBeInTheDocument();
    expect(within(barra).getByRole('status')).toHaveTextContent('3 de 3 notas');
  });

  it('las opciones de materia de conducta son las que aparecen en las notas', async () => {
    await mostrar(3);
    fireEvent.click(screen.getByRole('button', { name: 'Materia de la nota de conducta' }));

    const opciones = screen.getAllByRole('option').map((opcion) => opcion.textContent?.replace('✓', '').trim());
    expect(opciones).toEqual(['Todas las materias', 'Física', 'Matemática']);
  });

  it('sin notas de conducta no muestra filtros', async () => {
    vi.mocked(getParentSummary).mockResolvedValue(resumen(3));
    vi.mocked(getRasgosConducta).mockResolvedValue([]);
    render(<ParentPage />);

    await screen.findByText('Sin notas de conducta');
    await waitFor(() => expect(screen.queryByRole('search', { name: 'Filtros de notas de conducta' })).not.toBeInTheDocument());
  });
});
