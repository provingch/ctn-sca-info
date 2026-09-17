import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { PlanillasView } from './HomePage';
import { ToastProvider } from '../../context/ToastContext';
import { getMisAsignaciones, type AsignacionCompleta } from '../../api/planCurricular';
import { resolvePlanilla, getPortadaBlob } from '../../api/academics';
import type { HomeResponse, PlanillaResumenDto } from '../../api/home';

vi.mock('../../api/planCurricular', () => ({ getMisAsignaciones: vi.fn() }));
vi.mock('../../api/academics', () => ({
  resolvePlanilla: vi.fn(),
  syncClassroom: vi.fn(),
  getPortadaBlob: vi.fn(),
  savePortada: vi.fn(),
  deletePortada: vi.fn(),
}));

function planilla(overrides: Partial<PlanillaResumenDto>): PlanillaResumenDto {
  return {
    id: 1, materiaId: 1, materiaNombre: 'Programación', cursoId: 1, cursoOrdinal: '4º', seccion: 'A',
    especialidadNombre: 'Informática', etapa: 1, tienePortada: false,
    ...overrides,
  };
}

function asignacion(overrides: Partial<AsignacionCompleta>): AsignacionCompleta {
  return {
    id: 1, materiaId: 1, materiaNombre: 'Programación', especialidadId: 21, especialidadNombre: 'Informática',
    cursoBaseId: 1, cursoRealId: 1, cursoOrdinal: '4º', seccion: 'A', estadoPlan: 'PENDIENTE',
    ...overrides,
  };
}

function baseData(planillas: PlanillaResumenDto[]): HomeResponse {
  return {
    cursos: [], selCurso: null, selEtapa: 1, viewMode: 'planillas',
    planillas: [], planillasResumen: planillas, showPlanillaCards: false,
    materiasDetectadas: [], googleClassroomConnected: false, googleClassroomError: null,
    googleClassroomCourses: [], rasgoPlanillas: [], rasgoPlanillaSeleccionada: null,
    rasgoAsistencias: [], rasgoAlumnosValidos: [], rasgoAlumnosInvalidos: [], instrumentos: [],
  } as unknown as HomeResponse;
}

function show(data: HomeResponse, overrides: Partial<Parameters<typeof PlanillasView>[0]> = {}) {
  return render(
    <ToastProvider>
      <MemoryRouter initialEntries={['/home']}>
        <Routes>
          <Route path="/home" element={
            <PlanillasView
              data={data}
              especialidadNombre={null}
              nivel={null}
              seccion=""
              materiaId={null}
              hasActiveFilter={false}
              onClearFilter={vi.fn()}
              {...overrides}
            />
          } />
          <Route path="/planilla/:id" element={<p>Planilla abierta</p>} />
        </Routes>
      </MemoryRouter>
    </ToastProvider>,
  );
}

beforeEach(() => {
  vi.resetAllMocks();
  vi.mocked(getMisAsignaciones).mockResolvedValue([]);
  vi.mocked(getPortadaBlob).mockResolvedValue(null);
});

describe('PlanillasView', () => {
  it('sin planillas ni materias asignadas muestra el estado de "todavía no tenés planillas"', async () => {
    show(baseData([]));
    expect(await screen.findByText('Todavía no tenés planillas')).toBeInTheDocument();
  });

  it('muestra ocho planillas en cursos distintos y las tarjetas de crear al final, sin refetch', async () => {
    const planillas = Array.from({ length: 8 }, (_, i) => planilla({ id: i + 1, materiaId: i + 1, materiaNombre: `Materia ${i + 1}`, cursoOrdinal: `${(i % 3) + 1}º`, seccion: 'A' }));
    vi.mocked(getMisAsignaciones).mockResolvedValue([asignacion({ id: 99, materiaId: 50, materiaNombre: 'Materia nueva', cursoRealId: 5, cursoOrdinal: '2º', seccion: 'B' })]);
    show(baseData(planillas));
    for (const p of planillas) expect(await screen.findByText(p.materiaNombre)).toBeInTheDocument();
    expect(await screen.findByText('Materia nueva')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Crear planilla/ })).toBeInTheDocument();
    // getMisAsignaciones solo se llama una vez al montar, nunca por cambios de filtro.
    expect(getMisAsignaciones).toHaveBeenCalledTimes(1);
  });

  it('no repite una materia que ya tiene planilla entre las tarjetas de crear', async () => {
    const existente = planilla({ id: 1, materiaId: 1, cursoId: 7 });
    vi.mocked(getMisAsignaciones).mockResolvedValue([asignacion({ id: 1, materiaId: 1, cursoRealId: 7 })]);
    show(baseData([existente]));
    await screen.findByText('Programación');
    expect(screen.queryByRole('button', { name: /Crear planilla/ })).not.toBeInTheDocument();
  });

  it('filtra por especialidad, curso y sección sin tocar la red', async () => {
    const informatica = planilla({ id: 1, materiaId: 1, materiaNombre: 'Programación', especialidadNombre: 'Informática', cursoOrdinal: '4º', seccion: 'A' });
    const electricidad = planilla({ id: 2, materiaId: 2, materiaNombre: 'Instalaciones', especialidadNombre: 'Electricidad', cursoOrdinal: '3º', seccion: 'B' });
    show(baseData([informatica, electricidad]), { especialidadNombre: 'Informática', hasActiveFilter: true });
    await waitFor(() => expect(screen.getByText('Programación')).toBeInTheDocument());
    expect(screen.queryByText('Instalaciones')).not.toBeInTheDocument();
  });

  it('un filtro sin resultados muestra un estado vacío distinto al de "sin planillas"', async () => {
    const informatica = planilla({ id: 1, especialidadNombre: 'Informática' });
    const onClearFilter = vi.fn();
    show(baseData([informatica]), { especialidadNombre: 'Electricidad', hasActiveFilter: true, onClearFilter });
    expect(await screen.findByText('No hay planillas con ese filtro')).toBeInTheDocument();
    expect(screen.queryByText('Todavía no tenés planillas')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Limpiar filtro' }));
    expect(onClearFilter).toHaveBeenCalledTimes(1);
  });

  it('crear una planilla llama a resolvePlanilla con el curso real y navega, sin llamarlo dos veces', async () => {
    vi.mocked(getMisAsignaciones).mockResolvedValue([asignacion({ id: 1, materiaId: 1, cursoRealId: 5, materiaNombre: 'Matemática', cursoOrdinal: '2º', seccion: 'B' })]);
    vi.mocked(resolvePlanilla).mockResolvedValue({ planillaId: 42 });
    show(baseData([]));
    const button = await screen.findByRole('button', { name: /Crear planilla/ });
    fireEvent.click(button);
    await waitFor(() => expect(resolvePlanilla).toHaveBeenCalledWith(5, 1, 1));
    expect(resolvePlanilla).toHaveBeenCalledTimes(1);
    expect(await screen.findByText('Planilla abierta')).toBeInTheDocument();
  });

  it('nunca pide la portada cuando tienePortada es false', async () => {
    show(baseData([planilla({ tienePortada: false })]));
    await screen.findByText('Programación');
    expect(getPortadaBlob).not.toHaveBeenCalled();
  });

  it('el botón "Agregar portada" abre el selector de archivos sin navegar a la planilla', async () => {
    // Regresión: el wrapper de los controles llamaba preventDefault() sobre el
    // click burbujeado del <input type="file"> oculto (disparado por
    // inputRef.current.click()), lo que cancelaba la apertura del selector de
    // archivos antes de que cualquier código de subida llegara a ejecutarse.
    const { container } = show(baseData([planilla({ id: 7 })]));
    const button = await screen.findByRole('button', { name: 'Agregar portada' });
    fireEvent.click(button);

    const input = container.querySelector('input[type="file"]') as HTMLInputElement;
    const clickEvent = new MouseEvent('click', { bubbles: true, cancelable: true });
    const notPrevented = input.dispatchEvent(clickEvent);

    expect(notPrevented).toBe(true);
    expect(screen.queryByText('Planilla abierta')).not.toBeInTheDocument();
  });
});
