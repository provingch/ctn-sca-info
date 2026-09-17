import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ClassView } from './HomePage';
import { ToastProvider } from '../../context/ToastContext';
import { ApiError } from '../../api/client';
import { getAsignacionesDisponibles } from '../../api/planCurricular';
import { createClass, getClaseActual, listarCodigosConducta, type HomeResponse } from '../../api/home';
import { classEndTime } from './classFormUtils';

vi.mock('../../context/AuthContext', () => ({ useAuth: () => ({ user: { level: 1 } }) }));
vi.mock('../../api/planCurricular', () => ({ getAsignacionesDisponibles: vi.fn() }));
vi.mock('../../api/home', () => ({ getClaseActual: vi.fn(), listarCodigosConducta: vi.fn(), createClass: vi.fn() }));
const data = { selCurso: { id: 7, curso: '2', seccion: 'A', especialidad: 'Informática' }, selEtapa: 1,
  instrumentos: [], rasgoAsistencias: [{ id: 9, alumnoId: 1, codigos: ['N1'] }],
  rasgoAlumnosValidos: [{ id: 1, nombre: 'Ana', apellido: 'Pérez' }], rasgoAlumnosInvalidos: [],
} as unknown as HomeResponse;
const show = () => render(<ToastProvider><ClassView data={data} reload={vi.fn().mockResolvedValue(undefined)} /></ToastProvider>);
beforeEach(() => {
  vi.resetAllMocks();
  vi.mocked(getAsignacionesDisponibles).mockResolvedValue([{ id: 21, materiaId: 3, materiaNombre: 'Redes II' }]);
  vi.mocked(getClaseActual).mockResolvedValue({ hasClaseAhora: false });
  vi.mocked(listarCodigosConducta).mockResolvedValue([{ id: 1, codigo: 'N1', descripcion: 'Conducta', activo: true }]);
});

describe('Inicio de clase', () => {
  it('espera la consulta antes de avisar y mantiene el aviso mientras el curso siga sin asignaciones', async () => {
    vi.useFakeTimers();
    try {
      let resolve!: (items: []) => void;
      vi.mocked(getAsignacionesDisponibles).mockReturnValue(new Promise((done) => { resolve = done; }));
      show();
      expect(screen.queryByText('No hay asignaciones disponibles para este curso.')).not.toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Guardar inicio de clase' })).toBeDisabled();
      await act(async () => { resolve([]); });
      expect(screen.getByText('No hay asignaciones disponibles para este curso.')).toBeInTheDocument();
      await act(async () => { vi.advanceTimersByTime(4300); });
      expect(screen.getByText('No hay asignaciones disponibles para este curso.')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Guardar inicio de clase' })).toBeDisabled();
    } finally { vi.useRealTimers(); }
  });
  it('distingue un fallo de consulta y permite reintentar', async () => {
    vi.mocked(getAsignacionesDisponibles).mockRejectedValueOnce(new Error('Red'));
    show();
    fireEvent.click(await screen.findByRole('button', { name: 'Reintentar asignaciones' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Guardar inicio de clase' })).toBeEnabled());
    expect(screen.queryByText('No hay asignaciones disponibles para este curso.')).not.toBeInTheDocument();
  });
  it('no copia rasgos de clases anteriores y permite agregar uno nuevo desde el selector de dos pasos', async () => {
    show();
    await waitFor(() => expect(screen.getByLabelText('Disciplina')).toHaveValue('Redes II'));
    expect(screen.getByText('No hay rasgos registrados en esta clase.')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Agregar rasgo' }));
    fireEvent.click(screen.getByRole('button', { name: 'Alumno para el nuevo rasgo' }));
    fireEvent.click(screen.getByRole('option', { name: 'Pérez, Ana' }));
    fireEvent.click(screen.getByRole('button', { name: 'Código del nuevo rasgo' }));
    fireEvent.click(screen.getByRole('option', { name: 'N1 — Conducta' }));
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar' }));

    expect(screen.queryByText('No hay rasgos registrados en esta clase.')).not.toBeInTheDocument();
    expect(screen.getByText('N1: Conducta')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Pérez, Ana, presente, rasgos N1' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Limpiar formulario' }));
    expect(screen.getByText('No hay rasgos registrados en esta clase.')).toBeInTheDocument();
  });
  it('quita un rasgo asignado con la × de su pastilla', async () => {
    show();
    await waitFor(() => expect(screen.getByLabelText('Disciplina')).toHaveValue('Redes II'));
    fireEvent.click(screen.getByRole('button', { name: 'Agregar rasgo' }));
    fireEvent.click(screen.getByRole('button', { name: 'Alumno para el nuevo rasgo' }));
    fireEvent.click(screen.getByRole('option', { name: 'Pérez, Ana' }));
    fireEvent.click(screen.getByRole('button', { name: 'Código del nuevo rasgo' }));
    fireEvent.click(screen.getByRole('option', { name: 'N1 — Conducta' }));
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar' }));
    expect(screen.getByText('N1: Conducta')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Quitar N1 de Pérez, Ana' }));
    expect(screen.queryByText('N1: Conducta')).not.toBeInTheDocument();
    expect(screen.getByText('No hay rasgos registrados en esta clase.')).toBeInTheDocument();
  });
  it('no autocompleta datos de otro curso ni sobrescribe un tema escrito', async () => {
    let resolve!: (value: Awaited<ReturnType<typeof getClaseActual>>) => void;
    vi.mocked(getClaseActual).mockReturnValue(new Promise((done) => { resolve = done; }));
    show();
    fireEvent.change(screen.getByLabelText('Contenido específico desarrollado'), { target: { value: 'Tema escrito' } });
    await act(async () => resolve({ hasClaseAhora: true, cursoId: 99, asignacionId: 21, temaSugerido: 'Tema ajeno' }));
    expect(screen.getByLabelText('Contenido específico desarrollado')).toHaveValue('Tema escrito');
    expect(screen.queryByText('Clase en curso según tu horario:')).not.toBeInTheDocument();
  });
  it('autocompleta la asignación y el tema compatibles y no repone un formulario limpiado', async () => {
    vi.mocked(getClaseActual).mockResolvedValue({ hasClaseAhora: true, cursoId: 7, asignacionId: 21, horaInicio: '07:00', temaSugerido: 'Redes locales' });
    show();
    await waitFor(() => expect(screen.getByLabelText('Contenido específico desarrollado')).toHaveValue('Redes locales'));
    expect(screen.getByLabelText('Final de la clase')).toHaveValue('7:35');
    fireEvent.click(screen.getByRole('button', { name: 'Limpiar formulario' }));
    expect(screen.getByLabelText('Contenido específico desarrollado')).toHaveValue('');
    expect(screen.getByLabelText('Disciplina')).toHaveValue('Redes II');
    expect(createClass).not.toHaveBeenCalled();
  });
  it('calcula la última hora sin incluir recreos ni cruzar turnos', () => {
    expect(classEndTime('8:45', 1)).toBe('9:20');
    expect(classEndTime('11:25', 1)).toBe('12:00');
    expect(classEndTime('17:25', 1)).toBe('18:00');
    expect(classEndTime('11:25', 2)).toBe('');
  });
  it('pide justificar el atraso, conserva la asistencia marcada y reenvía con la justificación', async () => {
    vi.mocked(createClass)
      .mockRejectedValueOnce(new ApiError(400, 'Se requiere justificar el atraso para este tema.'))
      .mockResolvedValueOnce(undefined);
    show();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Guardar inicio de clase' })).toBeEnabled());
    fireEvent.change(screen.getByLabelText('Contenido específico desarrollado'), { target: { value: 'Tema atrasado' } });
    fireEvent.click(screen.getByRole('button', { name: 'Pérez, Ana, presente' }));
    fireEvent.click(screen.getByRole('button', { name: 'Guardar inicio de clase' }));

    const textarea = await screen.findByLabelText('Justificación del atraso');
    expect(textarea).toHaveFocus();
    expect(screen.getByText('El tema está atrasado según el plan curricular. Contá el motivo para poder registrar la clase.')).toBeInTheDocument();
    expect(screen.getByLabelText('Contenido específico desarrollado')).toHaveValue('Tema atrasado');
    expect(screen.getByRole('button', { name: 'Pérez, Ana, ausente' })).toHaveAttribute('aria-pressed', 'true');

    fireEvent.change(textarea, { target: { value: 'Paro la semana pasada' } });
    fireEvent.click(screen.getByRole('button', { name: 'Guardar inicio de clase' }));

    await waitFor(() => expect(createClass).toHaveBeenCalledTimes(2));
    expect(vi.mocked(createClass).mock.calls[1][0]).toMatchObject({ tema: 'Tema atrasado', justificacionAtraso: 'Paro la semana pasada' });
    await waitFor(() => expect(screen.queryByLabelText('Justificación del atraso')).not.toBeInTheDocument());
  });
  it('no muestra la justificación de atraso ante otro 400 del mismo endpoint', async () => {
    vi.mocked(createClass).mockRejectedValueOnce(new ApiError(400, 'No hay alumnos válidos para crear la planilla de rasgos.'));
    show();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Guardar inicio de clase' })).toBeEnabled());
    fireEvent.change(screen.getByLabelText('Contenido específico desarrollado'), { target: { value: 'Tema' } });
    fireEvent.click(screen.getByRole('button', { name: 'Guardar inicio de clase' }));
    await screen.findByText('No hay alumnos válidos para crear la planilla de rasgos.');
    expect(screen.queryByLabelText('Justificación del atraso')).not.toBeInTheDocument();
  });
  it('avisa si el catálogo de rasgos no carga y permite reintentar sin bloquear el formulario', async () => {
    vi.mocked(listarCodigosConducta).mockReset();
    vi.mocked(listarCodigosConducta)
      .mockRejectedValueOnce(new ApiError(500, 'Error de catálogo'))
      .mockResolvedValueOnce([{ id: 1, codigo: 'N1', descripcion: 'Conducta', activo: true }]);
    show();
    await screen.findByText('No se pudieron cargar los rasgos conductuales. Error de catálogo');
    expect(screen.getByRole('button', { name: 'Guardar inicio de clase' })).toBeEnabled();
    fireEvent.click(screen.getByRole('button', { name: 'Reintentar' }));
    await waitFor(() => expect(screen.queryByText('No se pudieron cargar los rasgos conductuales. Error de catálogo')).not.toBeInTheDocument());
    expect(screen.getByRole('button', { name: 'Agregar rasgo' })).toBeEnabled();
  });
  it('distingue el catálogo vacío del catálogo que no cargó', async () => {
    vi.mocked(listarCodigosConducta).mockReset();
    vi.mocked(listarCodigosConducta).mockResolvedValue([]);
    show();
    await waitFor(() => expect(screen.getByLabelText('Disciplina')).toHaveValue('Redes II'));
    expect(screen.getByText('Sin códigos cargados. Los carga el evaluador o el administrador.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Agregar rasgo' })).toBeDisabled();
    fireEvent.click(screen.getByRole('button', { name: '¿Qué significa cada código?' }));
    expect(screen.getAllByText('Sin códigos cargados. Los carga el evaluador o el administrador.').length).toBe(2);
  });
});
