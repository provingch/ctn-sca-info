import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { AdminCatalog } from '../../api/admin';
import { createQuejaContraProfesor, getAdminQuejas } from '../../api/quejas';
import AdminQuejasPanel from './AdminQuejasPanel';

vi.mock('../../api/quejas', async (importOriginal) => ({ ...await importOriginal<typeof import('../../api/quejas')>(), createQuejaContraProfesor: vi.fn(), getAdminQuejas: vi.fn() }));
const data: AdminCatalog = {
  materias: [], alumnos: [], cursosAlumnos: [], egresados: [],
  usuarios: [{ id: 9, nombre: 'Admin', apellido: 'Global', usuario: 'admin', nivel: 3, correo: null }, { id: 11, nombre: 'Coordinadora', apellido: 'Pedagógica', usuario: 'coord', nivel: 5, correo: null }],
  especialidades: [{ id: 99, nombre: 'Electricidad' }, { id: 21, nombre: 'Informática' }],
  cursos: [{ id: 13, especialidad: 'Informática', nivel: 2, seccion: 'A' }, { id: 14, especialidad: 'Electricidad', nivel: 1, seccion: 'B' }],
  asignaciones: [
    { id: 1, profesorId: 7, cursoId: 13, profesor: 'Ana Pérez', materiaId: 1, materia: 'Redes', curso: '2 A' },
    { id: 2, profesorId: 7, cursoId: 13, profesor: 'Ana Pérez', materiaId: 2, materia: 'Taller', curso: '2 A' },
  ],
};
const item = { id: 42, profesorId: 7, profesorNombre: 'Ana', profesorApellido: 'Pérez', cursoId: 13, especialidadId: 21, tipo: 'CONTRA_PROFESOR' as const, cursoEspecialidad: 'Informática', cursoNivel: 2, cursoSeccion: 'A', motivo: 'No responde a las consultas sobre las actividades.', creadaPor: 9, creadaEn: '2026-09-08T10:00:00' };
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
  vi.mocked(createQuejaContraProfesor).mockResolvedValue(undefined);
});

describe('AdminQuejasPanel', () => {
  it('es de solo lectura: no ofrece revisar, resolver, aceptar, rechazar ni descargar documentos', async () => {
    const estados = [
      { ...item, id: 42, estado: 'pendiente' as const },
      { ...item, id: 43, estado: 'aceptada' as const, aceptadaEn: '2026-09-09T10:00:00', aceptadaPor: 11 },
      { ...item, id: 44, estado: 'revisada' as const, revisadaEn: '2026-09-10T10:00:00', revisadaPor: 11, conclusion: 'Situación verificada.' },
      { ...item, id: 45, estado: 'resuelta' as const, revisadaEn: '2026-09-10T10:00:00', resueltaEn: '2026-09-11T10:00:00', procesoRevision: 'Se revisó.', solucionAplicada: 'Se corrigió.', corregidaPorNombre: 'María López' },
      { ...item, id: 46, estado: 'rechazada' as const, rechazadaEn: '2026-09-09T10:00:00', rechazadaPor: 11, motivoRechazo: 'No corresponde a una falta.' },
    ];
    vi.mocked(getAdminQuejas).mockResolvedValue(estados);
    render(<AdminQuejasPanel data={data} status={vi.fn()} isGlobalAdmin />);
    await screen.findByText('Pendiente', { selector: '.complaint-status' });

    expect(screen.getByText('Aceptada', { selector: '.complaint-status' })).toBeVisible();
    expect(screen.getByText('Revisada', { selector: '.complaint-status' })).toBeVisible();
    expect(screen.getByText('Resuelta', { selector: '.complaint-status' })).toBeVisible();
    expect(screen.getByText('Rechazada', { selector: '.complaint-status' })).toBeVisible();

    for (const label of [/Revisar queja/, 'Registrar solución', /Descargar solicitud/, /Descargar reporte/, 'Aceptar', 'Rechazar', 'Completar revisión', 'Marcar como resuelta']) {
      expect(screen.queryByRole('button', { name: label })).not.toBeInTheDocument();
    }
  });

  it('muestra la conclusión de una queja revisada sin acciones', async () => {
    vi.mocked(getAdminQuejas).mockResolvedValue([{ ...item, estado: 'revisada', revisadaEn: '2026-09-14T10:30:00', revisadaPor: 11, conclusion: 'Situación verificada.' }]);
    render(<AdminQuejasPanel data={data} status={vi.fn()} isGlobalAdmin />);
    expect(await screen.findByText('Situación verificada.')).toBeVisible();
    expect(screen.getByText(/Revisada el/)).toHaveTextContent('Coordinadora Pedagógica');
  });

  it('muestra la solución de una queja resuelta sin acciones', async () => {
    vi.mocked(getAdminQuejas).mockResolvedValue([{ ...item, estado: 'resuelta', revisadaEn: '2026-09-14T10:30:00', resueltaEn: '2026-09-15T09:00:00', procesoRevision: 'Se revisaron los registros.', solucionAplicada: 'Se corrigieron las consignas.', corregidaPorNombre: 'María López' }]);
    render(<AdminQuejasPanel data={data} status={vi.fn()} isGlobalAdmin />);
    expect(await screen.findByText('Se corrigieron las consignas.')).toBeVisible();
    expect(screen.getByText(/Corregida por María López/)).toBeVisible();
  });

  it('muestra una queja rechazada sin el motivo del rechazo', async () => {
    vi.mocked(getAdminQuejas).mockResolvedValue([{ ...item, estado: 'rechazada', rechazadaEn: '2026-09-09T10:00:00', rechazadaPor: 11, motivoRechazo: 'Motivo confidencial de coordinación.' }]);
    render(<AdminQuejasPanel data={data} status={vi.fn()} isGlobalAdmin />);
    expect(await screen.findByText('Rechazada por Coordinación Pedagógica')).toBeVisible();
    expect(screen.queryByText('Motivo confidencial de coordinación.')).not.toBeInTheDocument();
  });

  it('muestra revisiones guardadas y conserva los totales al buscar', async () => {
    vi.mocked(getAdminQuejas).mockResolvedValue([item, { ...item, id: 43, estado: 'revisada', revisadaEn: '2026-09-14T12:30:00', revisadaPor: 9, conclusion: 'Resuelto mediante reunión.' }]);
    render(<AdminQuejasPanel data={data} status={vi.fn()} isGlobalAdmin />);
    await screen.findByText('Revisada', { selector: '.complaint-status' });
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
    expect(screen.getByText('Pendiente', { selector: '.complaint-status' })).toBeInTheDocument();
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
    vi.mocked(createQuejaContraProfesor).mockReturnValue(new Promise<void>((done) => { resolve = done; }));
    render(<AdminQuejasPanel data={data} status={status} isGlobalAdmin />);
    await screen.findByText('Todavía no hay quejas registradas');
    fill();
    const button = screen.getByRole('button', { name: 'Registrar queja' });
    fireEvent.click(button);
    fireEvent.click(button);
    expect(createQuejaContraProfesor).toHaveBeenCalledTimes(1);
    expect(createQuejaContraProfesor).toHaveBeenCalledWith({ cursoId: 13, profesorId: 7, motivo: 'Motivo válido' });
    resolve();
    await waitFor(() => expect(status).toHaveBeenCalledWith('Queja registrada.'));
    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo actualizar el historial');
    expect(screen.getByRole('textbox', { name: 'Motivo' })).toHaveValue('');
  });

  it('conserva los datos si falla el registro', async () => {
    vi.mocked(createQuejaContraProfesor).mockRejectedValue(new Error('Error'));
    render(<AdminQuejasPanel data={data} status={vi.fn()} isGlobalAdmin={false} />);
    await screen.findByText('Todavía no hay quejas registradas');
    fill();
    fireEvent.click(screen.getByRole('button', { name: 'Registrar queja' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo registrar');
    expect(screen.getByRole('textbox', { name: 'Motivo' })).toHaveValue('  Motivo válido  ');
    expect(screen.getByText('Registros de tu especialidad, del más reciente al más antiguo.')).toBeInTheDocument();
  });
});
