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

export interface CodigoConducta {
  id: number;
  codigo: string;
  descripcion: string;
  activo: boolean;
}

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

/** Item liviano para el listado de planillas sin cursoId (todas las del profesor). */
export interface PlanillaResumenDto {
  id: number;
  materiaId: number;
  materiaNombre: string;
  cursoId: number;
  cursoOrdinal: string;
  seccion: string;
  especialidadNombre: string;
  etapa: number;
  tienePortada: boolean;
}

export interface HomeResponse {
  cursos: CursoDto[];
  selCurso: CursoDto | null;
  selEtapa: number;
  viewMode: string;
  planillas: PlanillaDto[];
  planillasResumen: PlanillaResumenDto[];
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

export function createClass(payload: { cursoId: number; asignacionId?: number | null; etapa: number; instrumentoId: number; horaInicio: string; horasCatedra: number | null; modalidad: string; observaciones: string; tema: string; justificacionAtraso?: string; alumnosAusentes: number[]; codigosPorAlumno: Record<number, string[]> }) {
  return apiRequest<void>('/api/home/create-rasgo-planilla', { method: 'POST', body: payload });
}

export function updateAttendance(asistenciaId: number, estado: string) {
  return apiRequest<void>('/api/home/submit-rasgo-asistencia', { method: 'POST', body: { asistenciaId, estado } });
}

export function updateRasgoCodigos(asistenciaId: number, codigos: string[]) {
  return apiRequest<void>('/api/home/update-rasgo-codigos', { method: 'POST', body: { asistenciaId, codigos } });
}

export interface UpdateClaseRequest {
  tema: string;
  asistencias: Array<{ asistenciaId: number; estado: 'presente' | 'ausente'; codigos: string[] }>;
}

export function updateClase(planillaId: number, payload: UpdateClaseRequest) {
  return apiRequest<void>(`/api/home/mis-clases/${planillaId}`, { method: 'PUT', body: payload });
}

export function listarCodigosConducta() {
  return apiRequest<CodigoConducta[]>('/api/codigos-conducta', { method: 'GET' });
}

export function crearCodigoConducta(codigo: string, descripcion: string) {
  return apiRequest<CodigoConducta>('/api/codigos-conducta', { method: 'POST', body: { codigo, descripcion } });
}

export function desactivarCodigoConducta(id: number) {
  return apiRequest<void>(`/api/codigos-conducta/${id}/desactivar`, { method: 'POST' });
}

export interface GetHomeParams {
  cursoId?: number;
  etapa?: number;
  view?: 'clase' | 'planillas';
}

export interface ClaseActualDto {
  hasClaseAhora: boolean;
  asignacionId?: number;
  cursoId?: number;
  materia?: string;
  cursoDescripcion?: string;
  etapa?: number;
  horaInicio?: string;
  horaFin?: string;
  temaSugerido?: string;
}

export function getClaseActual(): Promise<ClaseActualDto> {
  return apiRequest<ClaseActualDto>('/api/home/clase-actual', { method: 'GET' });
}

export interface ClaseDadaDto {
  id: number;
  fechaClase: string | null;
  tema: string;
  cursoId: number;
  cursoDescripcion: string;
  asignacionId: number | null;
  materiaNombre: string | null;
  profesorId: number;
  profesorNombre: string | null;
  especialidadId: number | null;
  especialidadNombre: string | null;
  totalAlumnos: number;
  totalAusentes: number;
  totalJustificados: number;
}

export interface ClaseDetalleDto {
  id: number;
  tema: string;
  fechaClase: string | null;
  cursoId: number;
  asistencias: Array<{
    id: number;
    alumnoId: number;
    alumnoNombreCompleto: string;
    estado: string;
    faltaCodigo: string | null;
    faltaObservacion: string | null;
    codigos: string[];
  }>;
}

export function getMisClases(): Promise<ClaseDadaDto[]> {
  return apiRequest<ClaseDadaDto[]>('/api/home/mis-clases', { method: 'GET' });
}

export function getMiClase(planillaId: number): Promise<ClaseDetalleDto> {
  return apiRequest<ClaseDetalleDto>(`/api/home/mis-clases/${planillaId}`, { method: 'GET' });
}

export function getHome(params: GetHomeParams = {}): Promise<HomeResponse> {
  const query = new URLSearchParams();
  if (params.cursoId !== undefined) query.set('cursoId', String(params.cursoId));
  if (params.etapa !== undefined) query.set('etapa', String(params.etapa));
  if (params.view !== undefined) query.set('view', params.view);
  const qs = query.toString();
  return apiRequest<HomeResponse>(`/api/home${qs ? `?${qs}` : ''}`, { method: 'GET' });
}
