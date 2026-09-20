import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { PlanillaDetail } from '../../api/academics';
import * as evaluacionApi from '../../api/evaluacion';
import PlanillaDetalleView from './PlanillaDetalleView';
import type { EvaluacionFiltros } from './useEvaluacionFiltros';

const showToast = vi.fn();
vi.mock('../../context/toast', () => ({ useToast: () => ({ showToast }) }));
vi.mock('../../api/evaluacion', () => ({
  listarPlanillas: vi.fn(),
  getPlanillaEvaluacion: vi.fn(),
  reabrirEtapaEvaluacion: vi.fn(),
  descargarPlanillas: vi.fn(),
}));

const filtros = {
  especialidades: [{ id: 2, nombre: 'Informática' }], especialidadId: 2, changeSpecialty: vi.fn(),
  niveles: [1], cursoNivel: 1, setCursoNivel: vi.fn(),
  secciones: ['A'], seccion: 'A', setSeccion: vi.fn(),
  etapa: 'primera', setEtapa: vi.fn(),
  materias: [], materiaId: 0, setMateriaId: vi.fn(),
  periodo: 2026, setPeriodo: vi.fn(),
  selected: { id: 5, especialidad: 'Informática', nivel: 1, seccion: 'A' }, status: '',
} as unknown as EvaluacionFiltros;

const cerrada: evaluacionApi.PlanillaResumen = {
  id: 100, cursoId: 5, materiaId: 10, materiaNombre: 'Programación', profesorId: 7, profesorNombre: 'Marcos Benítez', etapaIndex: 1, periodo: 2026,
  fechaCierreEtapa1: '2026-06-30', etapa1Confirmada: true, fechaCierreEtapa2: null, etapa2Confirmada: false,
};
const abierta: evaluacionApi.PlanillaResumen = { ...cerrada, id: 101, materiaId: 11, materiaNombre: 'Matemática', profesorNombre: 'Lucía Ferreira', fechaCierreEtapa1: null, etapa1Confirmada: false };

const detalle = {
  planilla: { id: 100, cursoId: 5, materiaId: 10, materiaNombre: 'Programación', etapaIndex: 1, periodo: 2026, totalPossiblePoints: 50, rsaPuntos: null },
  curso: { id: 5, especialidad: 'Informática', seccion: 'A', nivel: 1 },
  tareas: [{ id: 1, titulo: 'TP 1', total: 20 }, { id: 2, titulo: 'TP 2', total: 30 }],
  rows: [
    { registroId: 1, alumnoId: 1, alumnoNombre: 'Uno, Ana', grades: [{ tareaId: 1, puntos: 18 }], total: 18, porcentaje: 36, nota: 2, rsaPuntos: 0 },
    { registroId: 2, alumnoId: 2, alumnoNombre: 'Dos, Beto', grades: [{ tareaId: 1, puntos: 20 }, { tareaId: 2, puntos: 25 }], total: 45, porcentaje: 90, nota: 5, rsaPuntos: 0 },
  ],
  gradeRanges: {}, warnings: [],
} as unknown as PlanillaDetail;

beforeEach(() => {
  vi.resetAllMocks();
  vi.mocked(evaluacionApi.listarPlanillas).mockResolvedValue([cerrada, abierta]);
  vi.mocked(evaluacionApi.getPlanillaEvaluacion).mockResolvedValue(detalle);
  vi.mocked(evaluacionApi.reabrirEtapaEvaluacion).mockResolvedValue({ planillaId: 100 });
  vi.mocked(evaluacionApi.descargarPlanillas).mockResolvedValue('planillas.xlsx');
});

async function abrirPlanilla(materia: string) {
  render(<PlanillaDetalleView filtros={filtros} />);
  fireEvent.click(await screen.findByRole('button', { name: `Ver planilla de ${materia}` }));
  await screen.findByText('Modo evaluación — solo lectura');
}

describe('PlanillaDetalleView', () => {
  it('lista las planillas del curso con profesor y estado de la etapa', async () => {
    render(<PlanillaDetalleView filtros={filtros} />);

    expect(await screen.findByRole('rowheader', { name: 'Programación' })).toBeInTheDocument();
    expect(evaluacionApi.listarPlanillas).toHaveBeenCalledWith(5, 'primera', 2026, 0);
    const fila = screen.getByRole('rowheader', { name: 'Matemática' }).closest('tr') as HTMLElement;
    expect(within(fila).getByText('Lucía Ferreira')).toBeInTheDocument();
    expect(within(fila).getByText('Abierta')).toBeInTheDocument();
    expect(within(screen.getByRole('rowheader', { name: 'Programación' }).closest('tr') as HTMLElement).getByText('Cerrada')).toBeInTheDocument();
  });

  it('sin planillas para los filtros muestra el estado vacío, no un error', async () => {
    vi.mocked(evaluacionApi.listarPlanillas).mockResolvedValue([]);
    render(<PlanillaDetalleView filtros={filtros} />);

    expect(await screen.findByText('No hay planillas')).toBeInTheDocument();
  });

  it('el detalle es de sólo lectura: notas en texto plano, sin inputs ni guardado', async () => {
    await abrirPlanilla('Programación');

    expect(await screen.findByRole('rowheader', { name: 'Uno, Ana' })).toBeInTheDocument();
    expect(screen.getByLabelText('Uno, Ana, TP 1: 18 puntos')).toHaveTextContent('18');
    expect(screen.getByLabelText('Uno, Ana, TP 2: sin calificación')).toHaveTextContent('—');
    expect(screen.getByText('36%')).toBeInTheDocument();
    expect(screen.queryAllByRole('spinbutton')).toHaveLength(0);
    expect(screen.queryAllByRole('textbox')).toHaveLength(0);
    expect(screen.queryByRole('button', { name: /guardar/i })).not.toBeInTheDocument();
  });

  it('descarga esa planilla puntual con sus filtros', async () => {
    await abrirPlanilla('Programación');

    fireEvent.click(screen.getByRole('button', { name: 'Descargar esta planilla' }));
    await waitFor(() => expect(evaluacionApi.descargarPlanillas).toHaveBeenCalledWith(5, 'primera', 2026, 10));
  });

  it('sólo ofrece reabrir una etapa que está cerrada', async () => {
    await abrirPlanilla('Matemática');

    expect(screen.queryByRole('button', { name: /Reabrir Etapa/ })).not.toBeInTheDocument();
  });

  it('reabrir exige motivo y lo manda al backend', async () => {
    await abrirPlanilla('Programación');

    fireEvent.click(screen.getByRole('button', { name: 'Reabrir Etapa 1' }));
    const dialogo = await screen.findByRole('dialog');
    const confirmar = within(dialogo).getByRole('button', { name: 'Reabrir Etapa 1' });
    expect(confirmar).toBeDisabled();

    fireEvent.change(within(dialogo).getByLabelText(/Motivo de la reapertura/), { target: { value: 'El profesor pidió corregir una nota' } });
    fireEvent.click(confirmar);

    await waitFor(() => expect(evaluacionApi.reabrirEtapaEvaluacion).toHaveBeenCalledWith(100, 1, 'El profesor pidió corregir una nota'));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(showToast).toHaveBeenCalledWith(expect.stringContaining('Etapa 1 reabierta'), expect.anything());
  });
});
