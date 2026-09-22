import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, expect, it, vi } from 'vitest';
import QuejaCursoView from './QuejaCursoView';
import { ToastProvider } from '../../context/ToastContext';
import { getMisAsignaciones, type AsignacionCompleta } from '../../api/planCurricular';
import { createQuejaSobreCurso, getMisQuejas } from '../../api/quejas';

vi.mock('../../api/planCurricular', () => ({ getMisAsignaciones: vi.fn() }));
vi.mock('../../api/quejas', async (importOriginal) => ({
  ...await importOriginal<typeof import('../../api/quejas')>(),
  createQuejaSobreCurso: vi.fn(),
  getMisQuejas: vi.fn(),
}));
vi.mock('../../components/AnimatedSelect', () => ({ default: ({ ariaLabel, value, disabled, options, onChange }: {
  ariaLabel: string; value: string; disabled?: boolean; options: { value: string; label: string }[]; onChange: (value: string) => void;
}) => <select aria-label={ariaLabel} value={value} disabled={disabled} onChange={event => onChange(event.target.value)}>{options.map(item => <option key={item.value} value={item.value}>{item.label}</option>)}</select> }));

beforeEach(() => {
  vi.resetAllMocks();
  vi.mocked(getMisAsignaciones).mockResolvedValue([{ id: 1, materiaId: 7, materiaNombre: 'Redes', especialidadId: 2, especialidadNombre: 'Informática', cursoBaseId: 3, cursoRealId: 9, cursoOrdinal: '2°', seccion: 'A', estadoPlan: 'APROBADO' } satisfies AsignacionCompleta]);
  vi.mocked(getMisQuejas).mockResolvedValue([]);
  vi.mocked(createQuejaSobreCurso).mockResolvedValue(undefined);
});

function show() { render(<ToastProvider><QuejaCursoView /></ToastProvider>); }

it('carga las asignaciones, registra una queja sobre el curso elegido y la muestra en el historial', async () => {
  show();
  await screen.findByLabelText('Curso');
  fireEvent.change(screen.getByLabelText('Curso'), { target: { value: '2°' } });
  fireEvent.change(screen.getByLabelText('Sección'), { target: { value: 'A' } });
  await screen.findByText('2. Describí la situación');

  fireEvent.change(screen.getByRole('textbox'), { target: { value: 'El curso no trae los materiales.' } });
  vi.mocked(getMisQuejas).mockResolvedValue([{
    id: 1, profesorId: 5, cursoId: 3, especialidadId: 2, tipo: 'CONTRA_CURSO',
    cursoEspecialidad: 'Informática', cursoNivel: 2, cursoSeccion: 'A',
    motivo: 'El curso no trae los materiales.', creadaPor: 5, creadaEn: '2026-09-20T10:00:00',
  }]);
  fireEvent.click(screen.getByRole('button', { name: 'Registrar queja' }));

  await waitFor(() => expect(createQuejaSobreCurso).toHaveBeenCalledWith({ cursoId: 3, motivo: 'El curso no trae los materiales.' }));
  expect(await screen.findByText('El curso no trae los materiales.')).toBeInTheDocument();
});

it('no muestra el formulario hasta elegir curso y sección', async () => {
  show();
  await screen.findByLabelText('Curso');
  expect(screen.queryByText('2. Describí la situación')).not.toBeInTheDocument();
});
