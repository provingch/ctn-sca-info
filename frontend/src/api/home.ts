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
  materiasDetectadas: Array<{ id: number; nombre: string; categoria: string }>;
  googleClassroomConnected: boolean;
  googleClassroomError: string | null;
  googleClassroomCourses: Array<{ id: string; name: string; section: string; url: string }>;
  rasgoPlanillas: Array<{ id: number; tema: string; fechaClase: string }>;
  rasgoPlanillaSeleccionada: { id: number; tema: string; fechaClase: string } | null;
  rasgoAsistencias: Array<{ id: number; alumnoId: number; alumnoNombreCompleto: string; estado: string; faltaCodigo: string | null; faltaObservacion: string | null; codigos: string[] }>;
  rasgoAlumnosValidos: Array<{ id: number; nombre: string; apellido: string }>;
  rasgoAlumnosInvalidos: Array<{ id: number; nombre: string; apellido: string }>;
  instrumentos: Array<{ id: number; nombre: string }>;
  [key: string]: unknown;
}

export function createClass(payload: { cursoId: number; etapa: number; instrumentoId: number; turno: string; tema: string; alumnosAusentes: number[]; codigosPorAlumno: Record<number, string[]> }) {
  return apiRequest<void>('/api/home/create-rasgo-planilla', { method: 'POST', body: payload });
}

export function updateAttendance(asistenciaId: number, estado: string) {
  return apiRequest<void>('/api/home/submit-rasgo-asistencia', { method: 'POST', body: { asistenciaId, estado } });
}

export function updateRasgoCodigos(asistenciaId: number, codigos: string[]) {
  return apiRequest<void>('/api/home/update-rasgo-codigos', { method: 'POST', body: { asistenciaId, codigos } });
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
