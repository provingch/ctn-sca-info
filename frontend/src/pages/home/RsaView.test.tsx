import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, expect, it, vi } from 'vitest';
import RsaView from './RsaView';
import { ToastProvider } from '../../context/ToastContext';
import { getMisAsignaciones, type AsignacionCompleta } from '../../api/planCurricular';
import { getPlanilla, resolvePlanilla, saveRsa } from '../../api/academics';

vi.mock('../../api/planCurricular', () => ({ getMisAsignaciones: vi.fn() }));
vi.mock('../../api/academics', () => ({ getPlanilla: vi.fn(), resolvePlanilla: vi.fn(), saveRsa: vi.fn() }));
vi.mock('../../components/AnimatedSelect', () => ({ default: ({ ariaLabel, value, disabled, options, onChange }: {
  ariaLabel: string; value: string; disabled?: boolean; options: { value: string; label: string }[]; onChange: (value: string) => void;
}) => <select aria-label={ariaLabel} value={value} disabled={disabled} onChange={event => onChange(event.target.value)}>{options.map(item => <option key={item.value} value={item.value}>{item.label}</option>)}</select> }));

beforeEach(() => {
  vi.resetAllMocks();
  vi.mocked(getMisAsignaciones).mockResolvedValue([{ id: 1, materiaId: 7, materiaNombre: 'Redes', especialidadId: 2, especialidadNombre: 'Informática', cursoBaseId: 3, cursoRealId: 9, cursoOrdinal: '2°', seccion: 'A', estadoPlan: 'APROBADO' } satisfies AsignacionCompleta]);
  vi.mocked(resolvePlanilla).mockResolvedValue({ planillaId: 42 } as Awaited<ReturnType<typeof resolvePlanilla>>);
  vi.mocked(getPlanilla).mockResolvedValue({ planilla: { rsaPuntos: null, rsaToleranciaValor: null, rsaToleranciaUnidad: null } } as Awaited<ReturnType<typeof getPlanilla>>);
  vi.mocked(saveRsa).mockResolvedValue(undefined);
});
function show() { render(<ToastProvider><RsaView /></ToastProvider>); }
async function selectPlan() {
  await screen.findByLabelText('Curso');
  fireEvent.change(screen.getByLabelText('Curso'), { target: { value: '2°' } });
  fireEvent.change(screen.getByLabelText('Sección'), { target: { value: 'A' } });
  fireEvent.change(screen.getByLabelText('Materia'), { target: { value: '7' } });
}
it('comienza sin etapa, guía dependencias y no crea planillas antes de completar la selección', async () => {
  show();
  await screen.findByLabelText('Curso');
  for (const label of ['Sección', 'Materia', 'Etapa']) expect(screen.getByLabelText(label)).toBeDisabled();
  expect(screen.getByLabelText('Etapa')).toHaveValue('');
  expect(screen.queryByText('2. Configurá el RSA')).not.toBeInTheDocument();
  await selectPlan();
  expect(resolvePlanilla).not.toHaveBeenCalled();
  fireEvent.change(screen.getByLabelText('Etapa'), { target: { value: '2' } });
  await screen.findByText('RSA desactivado');
  expect(resolvePlanilla).toHaveBeenCalledWith(9, 7, 2);
  fireEvent.change(screen.getByLabelText('Curso'), { target: { value: '' } });
  expect(screen.getByLabelText('Etapa')).toHaveValue('');
  expect(screen.queryByText('2. Configurá el RSA')).not.toBeInTheDocument();
});
it('mantiene las reglas de validación y guarda la configuración elegida', async () => {
  show(); await selectPlan();
  fireEvent.change(screen.getByLabelText('Etapa'), { target: { value: '1' } });
  await screen.findByText('RSA desactivado');
  fireEvent.click(screen.getByLabelText('Activar RSA en esta planilla'));
  expect(screen.getByRole('button', { name: 'Guardar configuración' })).toBeDisabled();
  fireEvent.change(screen.getByLabelText('Puntos de RSA'), { target: { value: '5' } });
  fireEvent.change(screen.getByLabelText('Tolerancia sin descuento'), { target: { value: '2' } });
  fireEvent.click(screen.getByRole('button', { name: 'Guardar configuración' }));
  await waitFor(() => expect(saveRsa).toHaveBeenCalledWith(42, { puntos: 5, toleranciaValor: 2, toleranciaUnidad: 'CANTIDAD' }));
});
it('no resuelve ni ofrece configuración cuando el curso todavía no existe', async () => {
  const items = await getMisAsignaciones();
  vi.mocked(getMisAsignaciones).mockResolvedValue(items.map(item => ({ ...item, cursoRealId: null })));
  show(); await selectPlan();
  fireEvent.change(screen.getByLabelText('Etapa'), { target: { value: '1' } });
  expect(screen.getByText(/Ese curso todavía no existe/)).toBeInTheDocument();
  expect(resolvePlanilla).not.toHaveBeenCalled();
});
