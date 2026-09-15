import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ClassView } from './HomePage';
import { ToastProvider } from '../../context/ToastContext';
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
  it('espera la consulta antes de avisar y retira la notificación automáticamente', async () => {
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
      expect(screen.queryByText('No hay asignaciones disponibles para este curso.')).not.toBeInTheDocument();
    } finally { vi.useRealTimers(); }
  });
  it('distingue un fallo de consulta y permite reintentar', async () => {
    vi.mocked(getAsignacionesDisponibles).mockRejectedValueOnce(new Error('Red'));
    show();
    fireEvent.click(await screen.findByRole('button', { name: 'Reintentar asignaciones' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Guardar inicio de clase' })).toBeEnabled());
    expect(screen.queryByText('No hay asignaciones disponibles para este curso.')).not.toBeInTheDocument();
  });
  it('no copia rasgos de clases anteriores y muestra opciones fuera de la tabla', async () => {
    show();
    await waitFor(() => expect(screen.getByLabelText('Disciplina')).toHaveValue('Redes II'));
    const trigger = screen.getByRole('button', { name: 'Rasgos conductuales de Ana Pérez' });
    expect(trigger).toHaveTextContent('Seleccione');
    fireEvent.click(trigger);
    const list = screen.getByRole('listbox');
    expect(list.closest('table')).toBeNull();
    expect(list).toHaveAttribute('aria-multiselectable', 'true');
    fireEvent.click(screen.getByRole('option', { name: 'N1' }));
    expect(screen.getByRole('option', { name: 'N1' })).toHaveAttribute('aria-selected', 'true');
    fireEvent.keyDown(trigger, { key: 'Escape' });
    fireEvent.click(screen.getByRole('button', { name: 'Limpiar formulario' }));
    expect(trigger).toHaveTextContent('Seleccione');
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
});
