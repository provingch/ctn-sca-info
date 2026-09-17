import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ClassView } from './HomePage';
import { ToastProvider } from '../../context/ToastContext';
import { ApiError } from '../../api/client';
import { getAsignacionesDisponibles } from '../../api/planCurricular';
import { createClass, getMiHorarioHoy, listarCodigosConducta, type HomeResponse, type HorarioBloqueHoyDto } from '../../api/home';
import { classEndTime } from './classFormUtils';

vi.mock('../../context/AuthContext', () => ({ useAuth: () => ({ user: { level: 1 } }) }));
vi.mock('../../api/planCurricular', () => ({ getAsignacionesDisponibles: vi.fn() }));
vi.mock('../../api/home', () => ({ getMiHorarioHoy: vi.fn(), listarCodigosConducta: vi.fn(), createClass: vi.fn() }));
const data = { selCurso: { id: 7, curso: '2', seccion: 'A', especialidad: 'Informática' }, selEtapa: 1,
  instrumentos: [], rasgoAsistencias: [{ id: 9, alumnoId: 1, codigos: ['N1'] }],
  rasgoAlumnosValidos: [{ id: 1, nombre: 'Ana', apellido: 'Pérez' }], rasgoAlumnosInvalidos: [],
} as unknown as HomeResponse;
const bloqueHoy: HorarioBloqueHoyDto = {
  asignacionId: 21, cursoId: 8, materiaNombre: 'Redes II', cursoDescripcion: '3° A Informática',
  salaNombre: 'Aula 2', horaInicio: '07:00', horaFin: '07:35', horasCatedra: 1, registrada: false,
};
const show = () => render(<ToastProvider><ClassView data={data} reload={vi.fn().mockResolvedValue(undefined)} /></ToastProvider>);
beforeEach(() => {
  vi.resetAllMocks();
  vi.mocked(getAsignacionesDisponibles).mockResolvedValue([{ id: 21, materiaId: 3, materiaNombre: 'Redes II' }]);
  vi.mocked(getMiHorarioHoy).mockResolvedValue([]);
  vi.mocked(listarCodigosConducta).mockResolvedValue([{ id: 1, codigo: 'N1', descripcion: 'Conducta', activo: true }]);
});

describe('Inicio de clase', () => {
  it('espera la consulta antes de avisar y mantiene el aviso mientras el curso siga sin asignaciones', async () => {
    vi.useFakeTimers();
    try {
      let resolve!: (items: []) => void;
      vi.mocked(getAsignacionesDisponibles).mockReturnValue(new Promise((done) => { resolve = done; }));
      show();
      await act(async () => {}); // deja resolver el horario de hoy (vacío) y activar el camino manual
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
  it('no copia rasgos de clases anteriores y permite asignar uno nuevo desde el chip del alumno', async () => {
    show();
    await waitFor(() => expect(screen.getByLabelText('Disciplina')).toHaveValue('Redes II'));
    expect(screen.getByText(/No hay rasgos registrados en esta clase/)).toBeInTheDocument();

    const rasgosTrigger = screen.getByRole('button', { name: 'Rasgos conductuales de Pérez, Ana' });
    expect(rasgosTrigger).toHaveAttribute('aria-expanded', 'false');
    fireEvent.click(rasgosTrigger);
    expect(rasgosTrigger).toHaveAttribute('aria-expanded', 'true');
    const option = screen.getByRole('option', { name: /N1/ });
    expect(option).toHaveAttribute('aria-selected', 'false');
    fireEvent.click(option);

    expect(screen.queryByText(/No hay rasgos registrados en esta clase/)).not.toBeInTheDocument();
    expect(screen.getByText('N1: Conducta')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Limpiar formulario' }));
    expect(screen.getByText(/No hay rasgos registrados en esta clase/)).toBeInTheDocument();
  });
  it('quita un rasgo asignado con la × de su pastilla en el resumen', async () => {
    show();
    await waitFor(() => expect(screen.getByLabelText('Disciplina')).toHaveValue('Redes II'));
    fireEvent.click(screen.getByRole('button', { name: 'Rasgos conductuales de Pérez, Ana' }));
    fireEvent.click(screen.getByRole('option', { name: /N1/ }));
    expect(screen.getByText('N1: Conducta')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Quitar N1 de Pérez, Ana' }));
    expect(screen.queryByText('N1: Conducta')).not.toBeInTheDocument();
    expect(screen.getByText(/No hay rasgos registrados en esta clase/)).toBeInTheDocument();
  });
  it('cierra el selector de rasgos del chip al tocar afuera', async () => {
    show();
    await waitFor(() => expect(screen.getByLabelText('Disciplina')).toHaveValue('Redes II'));
    const rasgosTrigger = screen.getByRole('button', { name: 'Rasgos conductuales de Pérez, Ana' });
    fireEvent.click(rasgosTrigger);
    expect(screen.getByRole('listbox', { name: 'Códigos para Pérez, Ana' })).toBeInTheDocument();
    fireEvent.pointerDown(document.body);
    expect(screen.queryByRole('listbox', { name: 'Códigos para Pérez, Ana' })).not.toBeInTheDocument();
  });
  it('muestra el horario de hoy antes que cualquier selector y distingue bloques registrados de los que no', async () => {
    vi.mocked(getMiHorarioHoy).mockResolvedValue([
      bloqueHoy,
      { ...bloqueHoy, asignacionId: 22, horaInicio: '07:35', horaFin: '08:10', materiaNombre: 'Física', registrada: true },
    ]);
    show();
    await screen.findByText('Tu horario de hoy');
    expect(screen.queryByLabelText('Asignación de la clase')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Disciplina')).not.toBeInTheDocument();

    const disponible = screen.getByRole('button', { name: /Redes II/ });
    expect(disponible).toHaveTextContent('Registrar');
    const registrado = screen.getByText('Física').closest('div');
    expect(registrado).toHaveTextContent('Registrada');
    expect(registrado?.tagName).toBe('DIV');
  });
  it('precarga materia, curso, horario y horas cátedra desde el bloque elegido, sin selectores para tocar', async () => {
    vi.mocked(getMiHorarioHoy).mockResolvedValue([bloqueHoy]);
    vi.mocked(createClass).mockResolvedValue(undefined);
    show();
    fireEvent.click(await screen.findByRole('button', { name: /Redes II/ }));

    expect(screen.getByText('3° A Informática')).toBeInTheDocument();
    expect(screen.getByText('07:00–07:35')).toBeInTheDocument();
    expect(screen.getByText('Aula 2')).toBeInTheDocument();
    expect(screen.queryByLabelText('Asignación de la clase')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Inicio de clase')).not.toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('Contenido específico desarrollado'), { target: { value: 'Repaso de subredes' } });
    fireEvent.click(screen.getByRole('button', { name: 'Guardar inicio de clase' }));

    await waitFor(() => expect(createClass).toHaveBeenCalledTimes(1));
    expect(vi.mocked(createClass).mock.calls[0][0]).toMatchObject({
      cursoId: 8, asignacionId: 21, horaInicio: '07:00', horasCatedra: 1, tema: 'Repaso de subredes',
    });
  });
  it('vuelve a la lista de bloques al limpiar un formulario abierto desde un bloque', async () => {
    vi.mocked(getMiHorarioHoy).mockResolvedValue([bloqueHoy]);
    show();
    fireEvent.click(await screen.findByRole('button', { name: /Redes II/ }));
    expect(screen.queryByText('Tu horario de hoy')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Limpiar formulario' }));
    expect(await screen.findByText('Tu horario de hoy')).toBeInTheDocument();
  });
  it('abre el camino manual directamente cuando no hay horario cargado para hoy', async () => {
    vi.mocked(getMiHorarioHoy).mockResolvedValue([]);
    show();
    await waitFor(() => expect(screen.getByLabelText('Disciplina')).toHaveValue('Redes II'));
    expect(screen.queryByText('Tu horario de hoy')).not.toBeInTheDocument();
  });
  it('permite registrar otra clase a mano desde la lista de bloques', async () => {
    vi.mocked(getMiHorarioHoy).mockResolvedValue([bloqueHoy]);
    show();
    await screen.findByText('Tu horario de hoy');
    fireEvent.click(screen.getByRole('button', { name: 'Registrar otra clase' }));
    await waitFor(() => expect(screen.getByLabelText('Disciplina')).toHaveValue('Redes II'));
    expect(screen.getByLabelText('Asignación de la clase')).toBeInTheDocument();
  });
  it('avisa si el horario de hoy no carga y deja registrar otra clase igual', async () => {
    vi.mocked(getMiHorarioHoy).mockRejectedValueOnce(new ApiError(500, 'Error de horario'));
    show();
    await screen.findByText('No se pudo cargar tu horario de hoy. Error de horario');
    fireEvent.click(screen.getByRole('button', { name: 'Registrar otra clase' }));
    await waitFor(() => expect(screen.getByLabelText('Disciplina')).toHaveValue('Redes II'));
  });
  it('avisa al padre del modo (bloques o manual) para que sepa cuándo mostrar su propio selector', async () => {
    vi.mocked(getMiHorarioHoy).mockResolvedValue([bloqueHoy]);
    const onModoChange = vi.fn();
    render(<ToastProvider><ClassView data={data} reload={vi.fn().mockResolvedValue(undefined)} onModoChange={onModoChange} /></ToastProvider>);
    await screen.findByText('Tu horario de hoy');
    expect(onModoChange).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: /Redes II/ }));
    await waitFor(() => expect(onModoChange).toHaveBeenLastCalledWith('bloques'));

    fireEvent.click(screen.getByRole('button', { name: 'Limpiar formulario' }));
    await screen.findByText('Tu horario de hoy');
    fireEvent.click(screen.getByRole('button', { name: 'Registrar otra clase' }));
    await waitFor(() => expect(onModoChange).toHaveBeenLastCalledWith('manual'));
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
    expect(screen.getByRole('button', { name: 'Rasgos conductuales de Pérez, Ana' })).toBeEnabled();
  });
  it('distingue el catálogo vacío del catálogo que no cargó', async () => {
    vi.mocked(listarCodigosConducta).mockReset();
    vi.mocked(listarCodigosConducta).mockResolvedValue([]);
    show();
    await waitFor(() => expect(screen.getByLabelText('Disciplina')).toHaveValue('Redes II'));
    expect(screen.getByText('Sin códigos cargados. Los carga el evaluador o el administrador.')).toBeInTheDocument();
    const rasgosTrigger = screen.getByRole('button', { name: 'Rasgos conductuales de Pérez, Ana' });
    expect(rasgosTrigger).toBeDisabled();
    expect(rasgosTrigger).toHaveAttribute('title', 'Sin códigos cargados. Los carga el evaluador o el administrador.');
    fireEvent.click(screen.getByRole('button', { name: '¿Qué significa cada código?' }));
    expect(screen.getAllByText('Sin códigos cargados. Los carga el evaluador o el administrador.').length).toBe(2);
  });
});
