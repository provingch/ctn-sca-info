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

export function createClass(payload: { cursoId: number; asignacionId?: number | null; instrumentoId: number; horaInicio: string; horasCatedra: number | null; modalidad: string; observaciones: string; tema: string; justificacionAtraso?: string; alumnosAusentes: number[]; codigosPorAlumno: Record<number, string[]> }) {
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
  asistencias: Array<{ asistenciaId: number; estado: 'presente' | 'ausente' | 'ausente_justificado' | 'pendiente'; codigos: string[] }>;
}

export function updateClase(planillaId: number, payload: UpdateClaseRequest) {
  return apiRequest<void>(`/api/home/mis-clases/${planillaId}`, { method: 'PUT', body: payload });
}

/** incluirInactivos: para mostrar la descripción de N ya asignadas que después se desactivaron. */
export function listarCodigosConducta(incluirInactivos = false) {
  return apiRequest<CodigoConducta[]>(`/api/codigos-conducta${incluirInactivos ? '?incluirInactivos=true' : ''}`, { method: 'GET' });
}

export function crearCodigoConducta(codigo: string, descripcion: string) {
  return apiRequest<CodigoConducta>('/api/codigos-conducta', { method: 'POST', body: { codigo, descripcion } });
}

export function editarCodigoConducta(id: number, codigo: string, descripcion: string) {
  return apiRequest<void>(`/api/codigos-conducta/${id}`, { method: 'PUT', body: { codigo, descripcion } });
}

export function desactivarCodigoConducta(id: number) {
  return apiRequest<void>(`/api/codigos-conducta/${id}/desactivar`, { method: 'POST' });
}

export interface GetHomeParams {
  cursoId?: number;
  etapa?: number;
  view?: 'clase' | 'planillas';
}

export interface HorarioBloqueHoyDto {
  asignacionId: number;
  cursoId: number;
  materiaNombre: string;
  cursoDescripcion: string;
  salaNombre: string | null;
  horaInicio: string;
  horaFin: string;
  horasCatedra: number;
  registrada: boolean;
}

export function getMiHorarioHoy(): Promise<HorarioBloqueHoyDto[]> {
  return apiRequest<HorarioBloqueHoyDto[]>('/api/home/mi-horario/hoy', { method: 'GET' });
}

export interface ClaseDadaDto {
  totalPresentes?: number;
  totalPendientes?: number;
  id: number;
  createdAt?: string | null;
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
    alumnoNombre?: string | null;
    alumnoApellido?: string | null;
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

export interface AlumnoRiesgoMateriaDto {
  materiaId: number;
  materiaNombre: string;
  tareasNoEntregadas: number;
  notasConductuales: number;
}

export interface AlumnoRiesgoDto {
  alumnoId: number;
  nombreCompleto: string;
  cursoId: number;
  cursoNombre: string;
  especialidadId: number;
  especialidadNombre: string;
  seccion: string;
  tareasNoEntregadas: number;
  notasConductuales: number;
  motivos: string[];
  desglose: AlumnoRiesgoMateriaDto[];
}

export interface AlumnosRiesgoResponse {
  umbralTareas: number;
  umbralConducta: number;
  alumnos: AlumnoRiesgoDto[];
}

export interface GetAlumnosRiesgoParams {
  cursoId?: number;
  especialidadId?: number;
}

export function getAlumnosRiesgo(params: GetAlumnosRiesgoParams = {}): Promise<AlumnosRiesgoResponse> {
  const query = new URLSearchParams();
  if (params.cursoId !== undefined) query.set('cursoId', String(params.cursoId));
  if (params.especialidadId !== undefined) query.set('especialidadId', String(params.especialidadId));
  const qs = query.toString();
  return apiRequest<AlumnosRiesgoResponse>(`/api/home/alumnos-riesgo${qs ? `?${qs}` : ''}`, { method: 'GET' });
}
