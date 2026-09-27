import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AlumnosRiesgoView from './AlumnosRiesgoView';
import { getAlumnosRiesgo, type AlumnoRiesgoDto, type AlumnosRiesgoResponse } from '../../api/home';

vi.mock('../../api/home', () => ({ getAlumnosRiesgo: vi.fn() }));

function alumno(overrides: Partial<AlumnoRiesgoDto>): AlumnoRiesgoDto {
  return {
    alumnoId: 1,
    nombreCompleto: 'García, Juan',
    cursoId: 1,
    cursoNombre: '4º',
    especialidadId: 21,
    especialidadNombre: 'Informática',
    seccion: 'A',
    tareasNoEntregadas: 6,
    notasConductuales: 1,
    motivos: ['TAREAS'],
    desglose: [{ materiaId: 7, materiaNombre: 'Programación', tareasNoEntregadas: 6, notasConductuales: 1 }],
    ...overrides,
  };
}

function response(alumnos: AlumnoRiesgoDto[]): AlumnosRiesgoResponse {
  return { umbralTareas: 5, umbralConducta: 5, alumnos };
}

function show(overrides: Partial<Parameters<typeof AlumnosRiesgoView>[0]> = {}) {
  return render(<AlumnosRiesgoView especialidadNombre={null} nivel={null} seccion="" {...overrides} />);
}

beforeEach(() => {
  vi.resetAllMocks();
});

describe('AlumnosRiesgoView', () => {
  it('sin alumnos en riesgo muestra el estado vacío con los umbrales', async () => {
    vi.mocked(getAlumnosRiesgo).mockResolvedValue(response([]));
    show();
    expect(await screen.findByText('Ningún alumno supera los umbrales')).toBeInTheDocument();
    expect(screen.getByText(/más de 5 tareas no entregadas/)).toBeInTheDocument();
  });

  it('lista los alumnos en riesgo con su motivo', async () => {
    vi.mocked(getAlumnosRiesgo).mockResolvedValue(response([alumno({})]));
    show();
    expect(await screen.findByText('García, Juan')).toBeInTheDocument();
    expect(screen.getByText('Tareas')).toBeInTheDocument();
    expect(screen.queryByText('Conducta')).not.toBeInTheDocument();
  });

  it('expande el desglose por materia al hacer click y lo vuelve a ocultar', async () => {
    vi.mocked(getAlumnosRiesgo).mockResolvedValue(response([alumno({})]));
    show();
    await screen.findByText('García, Juan');
    expect(screen.queryByText(/Programación/)).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Ver desglose' }));
    expect(await screen.findByText(/Programación/)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Ocultar' }));
    expect(screen.queryByText(/Programación/)).not.toBeInTheDocument();
  });

  it('filtra localmente por especialidad, curso y sección', async () => {
    const informatica = alumno({ alumnoId: 1, nombreCompleto: 'Uno, Ana', especialidadNombre: 'Informática', cursoNombre: '4º', seccion: 'A' });
    const electricidad = alumno({ alumnoId: 2, nombreCompleto: 'Dos, Beto', especialidadNombre: 'Electricidad', cursoNombre: '3º', seccion: 'B' });
    vi.mocked(getAlumnosRiesgo).mockResolvedValue(response([informatica, electricidad]));
    show({ especialidadNombre: 'Informática' });
    expect(await screen.findByText('Uno, Ana')).toBeInTheDocument();
    expect(screen.queryByText('Dos, Beto')).not.toBeInTheDocument();
  });

  it('un error de red muestra el mensaje', async () => {
    vi.mocked(getAlumnosRiesgo).mockRejectedValue(new Error('boom'));
    show();
    expect(await screen.findByText('No se pudieron cargar los alumnos en riesgo.')).toBeInTheDocument();
  });
});
