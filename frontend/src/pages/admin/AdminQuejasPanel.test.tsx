import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { AdminCatalog } from '../../api/admin';
import { createQueja, getAdminQuejas, reviewQueja } from '../../api/quejas';
import AdminQuejasPanel from './AdminQuejasPanel';

vi.mock('../../api/quejas', async (importOriginal) => ({ ...await importOriginal<typeof import('../../api/quejas')>(), createQueja: vi.fn(), getAdminQuejas: vi.fn(), reviewQueja: vi.fn() }));
const data: AdminCatalog = {
  materias: [], alumnos: [], cursosAlumnos: [], egresados: [],
  usuarios: [{ id: 9, nombre: 'Admin', apellido: 'Global', usuario: 'admin', nivel: 3, correo: null }],
  especialidades: [{ id: 99, nombre: 'Electricidad' }, { id: 21, nombre: 'Informática' }],
  cursos: [{ id: 13, especialidad: 'Informática', nivel: 2, seccion: 'A' }, { id: 14, especialidad: 'Electricidad', nivel: 1, seccion: 'B' }],
  asignaciones: [
    { id: 1, profesorId: 7, cursoId: 13, profesor: 'Ana Pérez', materiaId: 1, materia: 'Redes', curso: '2 A' },
    { id: 2, profesorId: 7, cursoId: 13, profesor: 'Ana Pérez', materiaId: 2, materia: 'Taller', curso: '2 A' },
  ],
};
const item = { id: 42, profesorId: 7, profesorNombre: 'Ana', profesorApellido: 'Pérez', cursoId: 13, especialidadId: 21, cursoEspecialidad: 'Informática', cursoNivel: 2, cursoSeccion: 'A', motivo: 'No responde a las consultas sobre las actividades.', creadaPor: 9, creadaEn: '2026-09-08T10:00:00' };
function choose(name: string, option: string) {
  fireEvent.click(screen.getByRole('button', { name }));
  fireEvent.click(screen.getByRole('option', { name: option }));
}
function fill() {
  choose('Curso', 'Informática · 2° A');
  choose('Profesor', 'Ana Pérez');
  fireEvent.change(screen.getByRole('textbox', { name: 'Motivo' }), { target: { value: '  Motivo válido  ' } });
}
beforeEach(() => {
  vi.resetAllMocks();
  vi.mocked(getAdminQuejas).mockResolvedValue([]);
  vi.mocked(createQueja).mockResolvedValue(undefined);
});

describe('AdminQuejasPanel', () => {
  it('completa la revisión con conclusión, fecha y responsable y actualiza los totales', async () => {
    vi.mocked(getAdminQuejas).mockResolvedValue([item]);
    const revision = { estado: 'revisada' as const, revisadaEn: '2026-09-14T12:30:00', revisadaPor: 9, conclusion: 'Se acordó un seguimiento con el profesor.' };
    let resolve!: (value: typeof revision) => void;
    vi.mocked(reviewQueja).mockReturnValue(new Promise((done) => { resolve = done; }));
    render(<AdminQuejasPanel data={data} status={vi.fn()} isGlobalAdmin />);
    fireEvent.click(await screen.findByRole('button', { name: 'Revisar queja #42' }));
    const complete = screen.getByRole('button', { name: 'Completar revisión' });
    expect(complete).toBeDisabled();
    fireEvent.change(screen.getByRole('textbox', { name: 'Conclusión de la revisión' }), { target: { value: `  ${revision.conclusion}  ` } });
    fireEvent.click(complete);
    fireEvent.click(complete);
    expect(reviewQueja).toHaveBeenCalledTimes(1);
    expect(reviewQueja).toHaveBeenCalledWith(42, revision.conclusion);
    resolve(revision);
    expect(await screen.findByText('Revisada')).toBeVisible();
    expect(screen.getByText(revision.conclusion)).toBeVisible();
    expect(screen.getByText(/Revisada el/)).toHaveTextContent('Admin Global');
    expect(screen.queryByRole('button', { name: 'Revisar queja #42' })).not.toBeInTheDocument();
    expect(screen.getByText('Quejas pendientes').parentElement).toHaveTextContent('0');
    expect(screen.getByText('Quejas revisadas').parentElement).toHaveTextContent('1');
  });

  it('conserva la conclusión y el estado pendiente si falla la revisión', async () => {
    vi.mocked(getAdminQuejas).mockResolvedValue([item]);
    vi.mocked(reviewQueja).mockRejectedValue(new Error('Sin conexión'));
    render(<AdminQuejasPanel data={data} status={vi.fn()} isGlobalAdmin />);
    fireEvent.click(await screen.findByRole('button', { name: 'Revisar queja #42' }));
    fireEvent.change(screen.getByRole('textbox', { name: 'Conclusión de la revisión' }), { target: { value: 'Conclusión a conservar' } });
    fireEvent.click(screen.getByRole('button', { name: 'Completar revisión' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo guardar la revisión');
    expect(screen.getByRole('textbox', { name: 'Conclusión de la revisión' })).toHaveValue('Conclusión a conservar');
    expect(screen.getByText('Pendiente')).toBeVisible();
    expect(screen.getByText('Quejas pendientes').parentElement).toHaveTextContent('1');
  });

  it('muestra revisiones guardadas y conserva los totales al buscar', async () => {
    vi.mocked(getAdminQuejas).mockResolvedValue([item, { ...item, id: 43, estado: 'revisada', revisadaEn: '2026-09-14T12:30:00', revisadaPor: 9, conclusion: 'Resuelto mediante reunión.' }]);
    render(<AdminQuejasPanel data={data} status={vi.fn()} isGlobalAdmin />);
    await screen.findByText('Revisada');
    fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'reunion' } });
    expect(screen.getByText('Resuelto mediante reunión.')).toBeVisible();
    expect(screen.getByText('Total de quejas').parentElement).toHaveTextContent('2');
    expect(screen.getByText('Quejas pendientes').parentElement).toHaveTextContent('1');
    expect(screen.getByText('Quejas revisadas').parentElement).toHaveTextContent('1');
  });

  it('separa las quejas por especialidad y abre los grupos al buscar', async () => {
    const electricidad = { ...item, id: 43, especialidadId: 99, cursoId: 14, cursoEspecialidad: 'Electricidad', motivo: 'Faltan materiales de taller.' };
    vi.mocked(getAdminQuejas).mockResolvedValue([electricidad, item, { ...item, id: 41 }]);
    render(<AdminQuejasPanel data={data} status={vi.fn()} isGlobalAdmin />);
    const infoButton = await screen.findByRole('button', { name: /Informática.*2 quejas/ });
    const electricityButton = screen.getByRole('button', { name: /Electricidad.*1 queja/ });
    expect(infoButton).toHaveAttribute('aria-expanded', 'false');
    expect(electricityButton).toHaveAttribute('aria-expanded', 'false');
    expect(infoButton.closest('section')).toHaveAttribute('data-specialty', 'informatica');
    expect(electricityButton.closest('section')).toHaveAttribute('data-specialty', 'electricidad');
    fireEvent.click(infoButton);
    expect(infoButton).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByText('Registro #42').compareDocumentPosition(screen.getByText('Registro #41')) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(screen.getByText(electricidad.motivo)).not.toBeVisible();
    fireEvent.click(infoButton);
    expect(infoButton).toHaveAttribute('aria-expanded', 'false');
    fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'electricidad' } });
    expect(screen.queryByRole('button', { name: /Informática.*quejas/ })).not.toBeInTheDocument();
    expect(screen.getByText(electricidad.motivo)).toBeVisible();
    expect(electricityButton).toHaveAttribute('aria-expanded', 'true');
  });

  it('recupera la especialidad desde el catálogo si el registro no trae su nombre', async () => {
    vi.mocked(getAdminQuejas).mockResolvedValue([{ ...item, cursoEspecialidad: null }]);
    render(<AdminQuejasPanel data={data} status={vi.fn()} isGlobalAdmin />);
    expect(await screen.findByRole('button', { name: /Informática.*1 queja/ })).toHaveAttribute('aria-expanded', 'true');
    fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'informatica' } });
    expect(screen.getByText(item.motivo)).toBeVisible();
  });

  it('carga automáticamente, muestra el motivo y busca sin depender de tildes', async () => {
    vi.mocked(getAdminQuejas).mockResolvedValue([item]);
    render(<AdminQuejasPanel data={data} status={vi.fn()} isGlobalAdmin />);
    expect(await screen.findByText(item.motivo)).toBeInTheDocument();
    expect(getAdminQuejas).toHaveBeenCalledTimes(1);
    expect(screen.getByText('Pendiente')).toBeInTheDocument();
    fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'informatica' } });
    expect(screen.getByText(item.motivo)).toBeInTheDocument();
    fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'inexistente' } });
    expect(screen.getByText('Sin coincidencias')).toBeInTheDocument();
  });

  it('distingue un error inicial de un historial vacío y permite reintentar', async () => {
    vi.mocked(getAdminQuejas).mockRejectedValueOnce(new Error('Sin conexión'));
    render(<AdminQuejasPanel data={data} status={vi.fn()} isGlobalAdmin />);
    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo actualizar');
    expect(screen.queryByText('Todavía no hay quejas registradas')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Actualizar lista' }));
    expect(await screen.findByText('Todavía no hay quejas registradas')).toBeInTheDocument();
  });

  it('limpia el profesor al cambiar de curso y bloquea motivos en blanco', async () => {
    render(<AdminQuejasPanel data={data} status={vi.fn()} isGlobalAdmin />);
    await screen.findByText('Todavía no hay quejas registradas');
    fill();
    expect(screen.getByRole('button', { name: 'Registrar queja' })).toBeEnabled();
    fireEvent.change(screen.getByRole('textbox', { name: 'Motivo' }), { target: { value: '   ' } });
    expect(screen.getByRole('button', { name: 'Registrar queja' })).toBeDisabled();
    choose('Curso', 'Electricidad · 1° B');
    expect(screen.getByText('No hay profesores asignados a este curso.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Profesor' })).toBeDisabled();
  });

  it('registra una sola vez, sin inventar especialidad, y separa el error de refresco', async () => {
    const status = vi.fn();
    vi.mocked(getAdminQuejas).mockResolvedValueOnce([]).mockRejectedValueOnce(new Error('Sin conexión'));
    let resolve!: () => void;
    vi.mocked(createQueja).mockReturnValue(new Promise<void>((done) => { resolve = done; }));
    render(<AdminQuejasPanel data={data} status={status} isGlobalAdmin />);
    await screen.findByText('Todavía no hay quejas registradas');
    fill();
    const button = screen.getByRole('button', { name: 'Registrar queja' });
    fireEvent.click(button);
    fireEvent.click(button);
    expect(createQueja).toHaveBeenCalledTimes(1);
    expect(createQueja).toHaveBeenCalledWith({ cursoId: 13, profesorId: 7, motivo: 'Motivo válido' });
    resolve();
    await waitFor(() => expect(status).toHaveBeenCalledWith('Queja registrada.'));
    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo actualizar el historial');
    expect(screen.getByRole('textbox', { name: 'Motivo' })).toHaveValue('');
  });

  it('conserva los datos si falla el registro', async () => {
    vi.mocked(createQueja).mockRejectedValue(new Error('Error'));
    render(<AdminQuejasPanel data={data} status={vi.fn()} isGlobalAdmin={false} />);
    await screen.findByText('Todavía no hay quejas registradas');
    fill();
    fireEvent.click(screen.getByRole('button', { name: 'Registrar queja' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo registrar');
    expect(screen.getByRole('textbox', { name: 'Motivo' })).toHaveValue('  Motivo válido  ');
    expect(screen.getByText('Registros de tu especialidad, del más reciente al más antiguo.')).toBeInTheDocument();
  });
});

