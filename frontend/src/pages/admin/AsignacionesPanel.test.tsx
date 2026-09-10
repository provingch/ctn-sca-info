import { fireEvent, render, screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import AsignacionesPanel from './AsignacionesPanel';
import type { AdminCatalog } from '../../api/admin';

const data: AdminCatalog = {
  usuarios: Array.from({ length: 23 }, (_, index) => ({ id: index + 1, nombre: 'Ana', apellido: index === 0 ? 'Ácosta' : `Profesor ${String(index + 1).padStart(2, '0')}`, usuario: `docente${index + 1}`, nivel: 1, correo: null })),
  asignaciones: [1, 2, 3, 4].map((id) => ({ id, profesorId: id < 4 ? 23 : 1, materiaId: 1, cursoId: 1, profesor: 'Ana', materia: 'Matemática', curso: '1 A' })),
  especialidades: [], materias: [], alumnos: [], cursos: [], cursosAlumnos: [], egresados: [],
};
function panel(catalog = data) { return <AsignacionesPanel data={catalog} reload={vi.fn()} status={vi.fn()} />; }
function choose(label: string, option: string) {
  fireEvent.click(screen.getByRole('button', { name: label }));
  fireEvent.click(screen.getByRole('option', { name: option }));
}

describe('Directorio de profesores', () => {
  it('distingue las especialidades de un profesor con sus colores y cantidades', () => {
    const catalog = { ...data, cursos: [{ id: 1, especialidad: 'Informática', nivel: 1, seccion: 'A' }, { id: 2, especialidad: 'Electrónica', nivel: 2, seccion: 'B' }], asignaciones: [data.asignaciones[3], { ...data.asignaciones[3], id: 10, cursoId: 2 }] };
    render(panel(catalog));
    const row = screen.getAllByRole('rowheader')[0];
    expect(within(row).getByText('Informática')).toHaveAttribute('data-specialty', 'informatica');
    expect(within(row).getByText('Electrónica')).toHaveAttribute('data-specialty', 'electronica');
    fireEvent.click(screen.getByRole('button', { name: 'Ver detalle de Ácosta, Ana' }));
    expect(screen.getByText('Electrónica')).toHaveAttribute('data-specialty', 'electronica');
  });
  it('pagina, busca sin tildes y combina filtros', () => {
    render(panel());
    expect(screen.getAllByRole('rowheader')).toHaveLength(10);
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }));
    expect(screen.getByRole('status')).toHaveTextContent('11–20 de 23 profesores');
    fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'acosta' } });
    expect(screen.getByRole('status')).toHaveTextContent('1–1 de 1 profesores (23 en total)');
    expect(screen.getByText('Página 1 de 1')).toBeInTheDocument();
    choose('Filtrar por asignaciones', 'Sin asignaciones');
    expect(screen.getByText('No hay coincidencias')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeDisabled();
    fireEvent.click(screen.getByRole('button', { name: 'Limpiar filtros' }));
    expect(screen.getAllByRole('rowheader')).toHaveLength(10);
  });
  it('ordena toda la lista por cantidad en ambos sentidos antes de paginar', () => {
    render(panel());
    fireEvent.click(screen.getByRole('button', { name: 'Ordenar por asignaciones' }));
    expect(screen.getAllByRole('rowheader')[0]).toHaveTextContent('Profesor 23');
    expect(screen.getByRole('button', { name: 'Ordenar por asignaciones' }).closest('th')).toHaveAttribute('aria-sort', 'descending');
    fireEvent.click(screen.getByRole('button', { name: 'Ordenar por asignaciones' }));
    expect(screen.getAllByRole('rowheader')[0]).toHaveTextContent('Profesor 02');
    choose('Filtrar por asignaciones', 'Sobre el promedio');
    expect(screen.getAllByRole('rowheader')).toHaveLength(2);
  });
  it('conserva filtros y página al volver del detalle y permite editar asignaciones', () => {
    render(panel());
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }));
    fireEvent.click(screen.getByRole('button', { name: 'Ver detalle de Profesor 11, Ana' }));
    expect(screen.getByRole('heading', { name: 'Asignaciones de Profesor 11, Ana' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '← Volver a profesores' }));
    expect(screen.getByText('Página 2 de 3')).toBeInTheDocument();
    fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'docente1' } });
    fireEvent.click(screen.getByRole('button', { name: 'Ver detalle de Ácosta, Ana' }));
    expect(screen.getByRole('button', { name: 'Eliminar' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Editar' }));
    expect(within(screen.getByRole('dialog')).getByRole('heading', { name: 'Editar asignación' })).toBeInTheDocument();
  });
  it('ajusta la página tras recargar datos y maneja un catálogo vacío', () => {
    const view = render(panel());
    choose('Profesores por página', '25');
    expect(screen.getAllByRole('rowheader')).toHaveLength(23);
    choose('Profesores por página', '10');
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }));
    view.rerender(panel({ ...data, usuarios: data.usuarios.slice(0, 1) }));
    expect(screen.getByText('Página 1 de 1')).toBeInTheDocument();
    view.rerender(panel({ ...data, usuarios: [] }));
    expect(screen.getByRole('status')).toHaveTextContent('0–0 de 0 profesores');
    expect(screen.getByText('No hay profesores registrados')).toBeInTheDocument();
  });
});
