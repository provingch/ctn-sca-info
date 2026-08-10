/**
 * Espejo parcial de HomeController.java / HomeResponse.
 *
 * NOTA: HomeResponse tiene ~18 campos (cursos, planillas, Google Classroom,
 * rasgos, asistencias, alumnos, instrumentos...). Acá tipamos primero lo que
 * la pantalla Home (Bloque 2) necesita para el layout inicial; el resto se
 * agrega a medida que las pantallas de Bloque 3+ (planilla/rasgos) lo
 * requieran, para no mantener un tipo gigante desactualizado a mano.
 */
import { apiRequest } from './client';

export interface CursoDto {
  id: number;
  especialidad: string;
  curso: string;
  seccion: string;
}

export interface PlanillaDto {
  id: number;
  nombre: string;
  periodo: string;
  tareasCount: number;
  materiaId: number;
}

export interface HomeResponse {
  cursos: CursoDto[];
  selCurso: CursoDto | null;
  selEtapa: number;
  viewMode: string;
  planillas: PlanillaDto[];
  showPlanillaCards: boolean;
  // classroomPlanillaMap, classroomPlanillaMateriaMap, materiasDetectadas,
  // googleClassroom*, rasgoPlanillas, rasgoPlanillaSeleccionada,
  // rasgoAsistencias, rasgoAlumnos*, instrumentos:
  // pendientes de tipar cuando se construya la pantalla que los consume
  // (ver backend HomeResponse.java para los nombres exactos).
  [key: string]: unknown;
}

export interface GetHomeParams {
  cursoId?: number;
  etapa?: number;
  view?: 'clase' | 'planillas';
}

export function getHome(params: GetHomeParams = {}): Promise<HomeResponse> {
  const query = new URLSearchParams();
  if (params.cursoId !== undefined) query.set('cursoId', String(params.cursoId));
  if (params.etapa !== undefined) query.set('etapa', String(params.etapa));
  if (params.view !== undefined) query.set('view', params.view);
  const qs = query.toString();
  return apiRequest<HomeResponse>(`/api/home${qs ? `?${qs}` : ''}`, { method: 'GET' });
}
