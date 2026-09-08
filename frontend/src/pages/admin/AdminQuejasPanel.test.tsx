import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { AdminCatalog } from '../../api/admin';
import { createQueja, getAdminQuejas } from '../../api/quejas';
import AdminQuejasPanel from './AdminQuejasPanel';

vi.mock('../../api/quejas', () => ({ createQueja: vi.fn(), getAdminQuejas: vi.fn() }));
const data: AdminCatalog = {
  materias: [], alumnos: [], cursosAlumnos: [],
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
  it('carga automáticamente, muestra el motivo y busca sin depender de tildes', async () => {
    vi.mocked(getAdminQuejas).mockResolvedValue([item]);
    render(<AdminQuejasPanel data={data} status={vi.fn()} isGlobalAdmin />);
    expect(await screen.findByText(item.motivo)).toBeInTheDocument();
    expect(getAdminQuejas).toHaveBeenCalledTimes(1);
    expect(screen.queryByText('Pendiente')).not.toBeInTheDocument();
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

