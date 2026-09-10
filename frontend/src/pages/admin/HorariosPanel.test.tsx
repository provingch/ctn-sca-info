import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, expect, it, vi } from 'vitest';
import HorariosPanel from './HorariosPanel';
import { downloadHorarioEspecialidadPdf, getHoraCatedraCatalog, getHorarioResumen } from '../../api/admin';

vi.mock('../../api/admin', () => ({ getHorarioResumen: vi.fn(), getHoraCatedraCatalog: vi.fn(), downloadHorarioEspecialidadPdf: vi.fn() }));
vi.mock('./HorarioCursoPage', () => ({ default: ({ cursoId, backTo }: { cursoId: string; backTo: string }) => <div>Horario del curso {cursoId}<a href={backTo}>Volver a secciones</a></div> }));
function Location() { const location = useLocation(); return <output aria-label="Ruta">{location.pathname}{location.search}</output>; }
function show(path = '/admin/horarios') {
  return render(<MemoryRouter initialEntries={[path]}><Location /><Routes><Route path="/admin/horarios" element={<HorariosPanel status={vi.fn()} />} /><Route path="/admin/horarios/:cursoId" element={<HorariosPanel status={vi.fn()} />} /></Routes></MemoryRouter>);
}
beforeEach(() => {
  vi.resetAllMocks();
  vi.mocked(getHoraCatedraCatalog).mockResolvedValue([]);
  vi.mocked(getHorarioResumen).mockResolvedValue([
    { cursoId: 1, especialidadId: 2, especialidad: 'Electrónica', nivel: 1, cursoDescripcion: '1° A', cantidadSlotsCargados: 4 },
    { cursoId: 2, especialidadId: 2, especialidad: 'Electrónica', nivel: 1, cursoDescripcion: '1° B', cantidadSlotsCargados: 0 },
    { cursoId: 3, especialidadId: 2, especialidad: 'Electrónica', nivel: 2, cursoDescripcion: '2° A', cantidadSlotsCargados: 5 },
    { cursoId: 4, especialidadId: 3, especialidad: 'Informática', nivel: 1, cursoDescripcion: '1° A', cantidadSlotsCargados: 5 },
  ]);
});
it('muestra tarjetas por especialidad, luego curso y sección hasta abrir el horario', async () => {
  show();
  fireEvent.click(await screen.findByRole('button', { name: /Electrónica.*Ver cursos/ }));
  expect(screen.queryByRole('link', { name: /Ver horario/ })).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: /Curso 1°/ }));
  expect(screen.getAllByRole('link', { name: /Ver horario/ })).toHaveLength(2);
  fireEvent.click(screen.getByRole('link', { name: /Sección B/ }));
  expect(await screen.findByText('Horario del curso 2')).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Volver a secciones' })).toHaveAttribute('href', '/admin/horarios?especialidad=2&nivel=1');
});
it('restaura la selección desde la URL, permite volver y conserva el PDF por especialidad', async () => {
  show('/admin/horarios?especialidad=2&nivel=1');
  await screen.findByRole('link', { name: /Sección A/ });
  fireEvent.click(screen.getByRole('button', { name: 'PDF de la especialidad' }));
  expect(downloadHorarioEspecialidadPdf).toHaveBeenCalledWith(2);
  await waitFor(() => expect(screen.getByRole('button', { name: 'PDF de la especialidad' })).toBeEnabled());
  fireEvent.click(screen.getByRole('button', { name: '← Cursos de Electrónica' }));
  expect(screen.getByRole('button', { name: /Curso 2°/ })).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: '← Especialidades' }));
  expect(screen.getByRole('button', { name: /Informática.*Ver cursos/ })).toBeInTheDocument();
});
it('presenta un estado vacío y errores de carga', async () => {
  vi.mocked(getHorarioResumen).mockResolvedValue([]);
  const first = show();
  expect(await screen.findByText('No hay cursos disponibles')).toBeInTheDocument();
  first.unmount();
  vi.mocked(getHorarioResumen).mockRejectedValue(new Error('offline'));
  show();
  expect(await screen.findByText('No se pudo cargar el resumen de horarios.')).toBeInTheDocument();
});
