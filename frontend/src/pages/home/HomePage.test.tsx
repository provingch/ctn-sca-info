import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import HomePage from './HomePage';
import { ToastProvider } from '../../context/ToastContext';
import { SpecialtyProvider } from '../../context/SpecialtyContext';
import { AuthProvider } from '../../context/AuthContext';
import { getHome, getAlumnosRiesgo, type HomeResponse, type AlumnosRiesgoResponse } from '../../api/home';
import { getMisAsignaciones } from '../../api/planCurricular';
import { getEspecialidades } from '../../api/academics';
import * as authApi from '../../api/auth';

vi.mock('../../api/home', () => ({
  getHome: vi.fn(),
  getMiHorarioHoy: vi.fn(),
  listarCodigosConducta: vi.fn(),
  createClass: vi.fn(),
  getAlumnosRiesgo: vi.fn(),
}));
vi.mock('../../api/planCurricular', () => ({ getMisAsignaciones: vi.fn(), getAsignacionesDisponibles: vi.fn() }));
vi.mock('../../api/profile', () => ({ getProfile: vi.fn() }));
vi.mock('../../api/auth', () => ({ refresh: vi.fn() }));
vi.mock('../../api/academics', () => ({
  getEspecialidades: vi.fn(),
  getPortadaBlob: vi.fn(),
  resolvePlanilla: vi.fn(),
  savePortada: vi.fn(),
  deletePortada: vi.fn(),
  syncClassroom: vi.fn(),
}));

function baseData(): HomeResponse {
  return {
    cursos: [], selCurso: null, selEtapa: 1, viewMode: 'planillas',
    planillas: [], planillasResumen: [], showPlanillaCards: false,
    materiasDetectadas: [], googleClassroomConnected: false, googleClassroomError: null,
    googleClassroomCourses: [], rasgoPlanillas: [], rasgoPlanillaSeleccionada: null,
    rasgoAsistencias: [], rasgoAlumnosValidos: [], rasgoAlumnosInvalidos: [], instrumentos: [],
  } as unknown as HomeResponse;
}

function riesgoData(): AlumnosRiesgoResponse {
  return { umbralTareas: 5, umbralConducta: 5, alumnos: [] };
}

function show(initialEntry: string) {
  return render(
    <ToastProvider>
      <SpecialtyProvider>
        <AuthProvider>
          <MemoryRouter initialEntries={[initialEntry]}>
            <Routes>
              <Route path="/home" element={<HomePage />} />
            </Routes>
          </MemoryRouter>
        </AuthProvider>
      </SpecialtyProvider>
    </ToastProvider>,
  );
}

beforeEach(() => {
  vi.resetAllMocks();
  vi.mocked(getHome).mockResolvedValue(baseData());
  vi.mocked(getAlumnosRiesgo).mockResolvedValue(riesgoData());
  vi.mocked(getMisAsignaciones).mockResolvedValue([]);
  vi.mocked(getEspecialidades).mockResolvedValue([]);
  vi.mocked(authApi.refresh).mockRejectedValue(new Error('no session'));
});

describe('HomePage — vista de planillas', () => {
  it('view=planillas sin subview sigue mostrando la lista de planillas (regresión)', async () => {
    show('/home?view=planillas');
    expect(await screen.findByText('Todavía no tenés planillas')).toBeInTheDocument();
    expect(screen.queryByText('Ningún alumno supera los umbrales')).not.toBeInTheDocument();
    expect(getAlumnosRiesgo).not.toHaveBeenCalled();
  });

  it('view=planillas&subview=riesgo muestra la vista de alumnos en riesgo', async () => {
    show('/home?view=planillas&subview=riesgo');
    expect(await screen.findByText('Ningún alumno supera los umbrales')).toBeInTheDocument();
    expect(screen.queryByText('Todavía no tenés planillas')).not.toBeInTheDocument();
  });

  it('la pestaña "Alumnos en riesgo" navega a esa vista sin recargar planillas de nuevo', async () => {
    show('/home?view=planillas');
    await screen.findByText('Todavía no tenés planillas');

    fireEvent.click(screen.getByRole('tab', { name: 'Alumnos en riesgo' }));

    expect(await screen.findByText('Ningún alumno supera los umbrales')).toBeInTheDocument();
  });
});
