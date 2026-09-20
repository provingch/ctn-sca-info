import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getParentSummary, getRasgosConducta, type ParentResponse, type ParentSubject, type ParentTask, type RasgoConducta } from '../../api/parent';
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

// ---- Detalle de tareas: filtro y agrupación por mes ----------------------------------------------------

const tarea = (id: number, titulo: string, fecha: string): ParentTask => ({ id, titulo, fecha, puntos: 10, total: 20, estado: 'CALIFICADA' });
const tareasVariosMeses = [tarea(1, 'TP de marzo', '2026-03-10'), tarea(3, 'Examen de mayo', '2026-05-02'), tarea(2, 'TP de marzo 2', '2026-03-25')];

async function mostrarConTareas(tareasPrimera: ParentTask[], tareasSegunda: ParentTask[] = tareasVariosMeses) {
  vi.mocked(getParentSummary).mockResolvedValue({
    hijos: [{ id: 7, nombre: 'Camila', apellido: 'Rojas', especialidad: 'Informática', promedio: 80 }],
    selectedAlumnoId: 7, libretaDisponible: false,
    materias: [{ ...materia(1, 'Matemática'), tareas: tareasPrimera }, { ...materia(2, 'Física'), tareas: tareasSegunda }],
  });
  vi.mocked(getRasgosConducta).mockResolvedValue([]);
  render(<ParentPage />);
  await screen.findByRole('heading', { name: 'Matemática', level: 2 });
}

const mesesMostrados = () => screen.queryAllByRole('heading', { level: 3 }).map((h) => h.childNodes[0]?.textContent).filter((texto) => /\d{4}|Sin fecha/.test(texto ?? ''));

describe('ParentPage — detalle de tareas por mes', () => {
  it('con "Todos" las tareas quedan agrupadas por mes, en orden cronológico', async () => {
    await mostrarConTareas(tareasVariosMeses);

    expect(mesesMostrados()).toEqual(['Marzo 2026', 'Mayo 2026']);
    const marzo = screen.getByRole('heading', { name: /Marzo 2026/ }).closest('section') as HTMLElement;
    expect(within(marzo).getByText('TP de marzo')).toBeInTheDocument();
    expect(within(marzo).getByText('TP de marzo 2')).toBeInTheDocument();
    expect(within(marzo).queryByText('Examen de mayo')).not.toBeInTheDocument();
  });

  it('el selector de mes ofrece "Todos" y sólo los meses que tienen tareas', async () => {
    await mostrarConTareas(tareasVariosMeses);

    fireEvent.click(screen.getByRole('button', { name: 'Mes de las tareas' }));
    expect(screen.getAllByRole('option').map((o) => o.textContent?.replace('✓', '').trim())).toEqual(['Todos los meses', 'Marzo 2026', 'Mayo 2026']);
  });

  it('un mes puntual filtra la lista, conserva el número de cada tarea y se puede limpiar', async () => {
    await mostrarConTareas(tareasVariosMeses);
    const barra = screen.getByRole('search', { name: 'Filtros de tareas' });
    expect(within(barra).getByRole('status')).toHaveTextContent('3 de 3 tareas');

    fireEvent.click(within(barra).getByRole('button', { name: 'Mes de las tareas' }));
    fireEvent.click(screen.getByRole('option', { name: /Mayo 2026/ }));

    expect(within(barra).getByRole('status')).toHaveTextContent('1 de 3 tareas');
    expect(mesesMostrados()).toEqual(['Mayo 2026']);
    expect(screen.queryByText('TP de marzo')).not.toBeInTheDocument();
    const fila = screen.getByText('Examen de mayo').closest('article') as HTMLElement;
    expect(fila).toHaveTextContent('03'); // era la tercera tarea cronológica

    fireEvent.click(within(barra).getByRole('button', { name: 'Limpiar filtros' }));
    expect(mesesMostrados()).toEqual(['Marzo 2026', 'Mayo 2026']);
  });

  it('con tareas de un solo mes no ofrece el filtro, pero igual muestra el mes', async () => {
    await mostrarConTareas([tarea(1, 'TP único', '2026-03-10'), tarea(2, 'Otro TP', '2026-03-20')]);

    expect(screen.queryByRole('search', { name: 'Filtros de tareas' })).not.toBeInTheDocument();
    expect(mesesMostrados()).toEqual(['Marzo 2026']);
  });

  it('las tareas sin fecha van al final bajo "Sin fecha"', async () => {
    await mostrarConTareas([tarea(1, 'Con fecha', '2026-03-10'), { ...tarea(2, 'Sin fecha cargada', ''), fecha: '' }]);

    expect(mesesMostrados()).toEqual(['Marzo 2026', 'Sin fecha']);
  });

  it('al cambiar de materia el filtro de mes vuelve a "Todos"', async () => {
    await mostrarConTareas(tareasVariosMeses);
    fireEvent.click(screen.getByRole('button', { name: 'Mes de las tareas' }));
    fireEvent.click(screen.getByRole('option', { name: /Mayo 2026/ }));
    expect(mesesMostrados()).toEqual(['Mayo 2026']);

    fireEvent.click(screen.getByRole('button', { name: /Física/ }));

    expect(await screen.findByRole('heading', { name: 'Física', level: 2 })).toBeInTheDocument();
    expect(mesesMostrados()).toEqual(['Marzo 2026', 'Mayo 2026']);
  });
});
