import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import TareaPage from './TareaPage';
import { createTarea, getInstrumentos, getTarea, updateTarea } from '../../api/academics';
vi.mock('../../components/AppShell', () => ({ default: ({ children }: { children: React.ReactNode }) => <main>{children}</main> }));
vi.mock('../../api/academics', () => ({ createTarea: vi.fn(), getInstrumentos: vi.fn(), getTarea: vi.fn(), updateTarea: vi.fn(), deleteTarea: vi.fn() }));
beforeAll(() => {
  HTMLDialogElement.prototype.showModal = function () { this.setAttribute('open', ''); };
  HTMLDialogElement.prototype.close = function () { this.removeAttribute('open'); };
});
beforeEach(() => {
  vi.resetAllMocks();
  vi.mocked(getInstrumentos).mockResolvedValue([{ id: 1, nombre: 'Trabajo práctico' }]);
  vi.mocked(createTarea).mockResolvedValue({} as never);
});
const renderPage = (edit = false) => render(<MemoryRouter initialEntries={[edit ? '/planilla/5/tarea/7' : '/planilla/5/tarea']}><Routes><Route path="/planilla/:planillaId/tarea/:tareaId?" element={<TareaPage />} /><Route path="/planilla/5" element={<p>Planilla guardada</p>} /></Routes></MemoryRouter>);
async function fill() {
  await waitFor(() => expect(screen.getByRole('button', { name: 'Instrumento' })).toBeEnabled());
  fireEvent.click(screen.getByRole('button', { name: 'Instrumento' }));
  fireEvent.click(screen.getByRole('option', { name: 'Trabajo práctico' }));
  fireEvent.change(screen.getByPlaceholderText('Trabajo práctico N°3'), { target: { value: '  Trabajo de redes  ' } });
}
describe('Formulario de tarea', () => {
  it('permite borrar todo el puntaje y bloquea guardar hasta completar correctamente', async () => {
    renderPage();
    await fill();
    const score = screen.getByRole('spinbutton');
    fireEvent.change(score, { target: { value: '' } });
    expect(score).toHaveValue(null);
    expect(screen.getByText('Ingresá el puntaje total.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeDisabled();
    for (const value of ['0', '-1', '2.5']) {
      fireEvent.change(score, { target: { value } });
      expect(screen.getByRole('button', { name: 'Guardar' })).toBeDisabled();
    }
    fireEvent.change(score, { target: { value: '25' } });
    fireEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    await screen.findByText('Planilla guardada');
    expect(createTarea).toHaveBeenCalledWith(5, expect.objectContaining({ total: 25, titulo: 'Trabajo de redes', instrumentoId: 1 }));
  });
  it('rechaza títulos en blanco y fechas borradas desde el calendario compartido', async () => {
    renderPage();
    await fill();
    fireEvent.change(screen.getByPlaceholderText('Trabajo práctico N°3'), { target: { value: '   ' } });
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeDisabled();
    fireEvent.change(screen.getByPlaceholderText('Trabajo práctico N°3'), { target: { value: 'Título válido' } });
    fireEvent.click(screen.getByRole('button', { name: 'Fecha' }));
    fireEvent.click(screen.getByRole('button', { name: 'Borrar' }));
    expect(screen.getByText('Seleccioná una fecha válida.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeDisabled();
  });
  it('no habilita acciones cuando falla la carga de una tarea existente', async () => {
    vi.mocked(getTarea).mockRejectedValue(new Error('Sin conexión'));
    renderPage(true);
    await screen.findByRole('alert');
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Eliminar' })).toBeDisabled();
    expect(updateTarea).not.toHaveBeenCalled();
  });
  it('conserva los campos cuando falla el guardado', async () => {
    vi.mocked(createTarea).mockRejectedValue(new Error('Sin conexión'));
    renderPage();
    await fill();
    fireEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    await screen.findByRole('alert');
    expect(screen.getByPlaceholderText('Trabajo práctico N°3')).toHaveValue('  Trabajo de redes  ');
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeEnabled();
  });
});

