import { api, apiDownload, ApiError } from './client';

export interface MateriaItem {
  id: number;
  nombre: string;
  categoria: string;
  especialidadIds?: number[];
}

export interface PadreSummary {
  id: number;
  nombre: string;
  apellido: string;
  ci: number | null;
  usuario: string;
}

export interface UserItem {
  id: number;
  nombre: string;
  apellido: string;
  usuario: string;
  nivel: number;
  correo: string | null;
  ci?: number | null;
  especialidadId?: number | null;
  especialidadNombre?: string | null;
}

export interface AssignmentItem {
  id: number;
  profesorId: number;
  materiaId: number;
  cursoId: number;
  profesor: string;
  materia: string;
  curso: string;
}

export interface StudentItem {
  id: number;
  nombre: string;
  apellido: string;
  cursoId: number;
  ci: number | null;
  correoEncargado: string | null;
  correoEncargado2: string | null;
}

export interface CourseItem {
  id: number;
  especialidad: string;
  nivel: number;
  seccion: string;
}

export interface SpecialtyItem {
  id: number;
  nombre: string;
}

export interface HoraCatedraItem {
  id: number;
  numero: number;
  etiqueta: string | null;
  horaInicio: string;
  horaFin: string;
}

export interface HorarioSlotItem {
  id: number;
  asignacionId: number;
  diaSemana: number;
  horaCatedraId: number;
  horaCatedraNumero: number;
  horaCatedraEtiqueta: string | null;
  horaInicio: string;
  horaFin: string;
  sala: string | null;
  materiaNombre: string | null;
  cursoDescripcion: string | null;
  profesorNombre: string | null;
}

export interface HorarioResumenCursoItem {
  cursoId: number;
  especialidad: string;
  cursoDescripcion: string;
  cantidadSlotsCargados: number;
}

export interface HorarioImportRowItem {
  diaSemana: number;
  horaCatedraId: number;
  horaCatedraEtiqueta: string | null;
  materiaTexto: string;
  profesorTexto: string;
  asignacionId: number | null;
  estado: 'ok' | 'sin_asignacion' | 'conflicto_profesor' | 'conflicto_curso';
  detalle: string | null;
}
export interface HorarioImportResponse { creados: number; omitidos: number; filas: HorarioImportRowItem[]; }
export interface AsignacionResumenItem { asignacionId: number; materiaNombre: string; profesorNombre: string; }

export interface MigracionEstadoItem {
  version: string;
  appliedAt: string | null;
}

export interface SistemaEstadoResponse {
  dbConectada: boolean;
  migraciones: MigracionEstadoItem[];
  ultimaSyncClassroom: string | null;
  espacioLogsBytes: number;
}

export interface AdminCatalog {
  materias: MateriaItem[];
  usuarios: UserItem[];
  asignaciones: AssignmentItem[];
  alumnos: StudentItem[];
  cursos: CourseItem[];
  especialidades: SpecialtyItem[];
}

export const ADMIN_FORBIDDEN_MESSAGE = 'No tenés permiso para gestionar este recurso.';

export function normalizeAdminError(error: unknown) {
  return error instanceof ApiError && error.status === 403
    ? new ApiError(403, ADMIN_FORBIDDEN_MESSAGE, error.body)
    : error;
}

async function adminRequest<T>(request: () => Promise<T>): Promise<T> {
  try {
    return await request();
  } catch (error) {
    throw normalizeAdminError(error);
  }
}

export const getAdminCatalog = () => adminRequest(() => api.get<AdminCatalog>('/api/admin'));
export const createAdminRecord = (section: string, payload: unknown) => adminRequest(() => api.post<void>(`/api/admin/${section}`, payload));
export const updateAdminRecord = (section: string, id: number, payload: unknown) => adminRequest(() => api.put<void>(`/api/admin/${section}/${id}`, payload));
export const deleteAssignment = (id: number) => adminRequest(() => api.delete<void>(`/api/admin/asignaciones/${id}`));
export const deleteAdminRecord = (section: string, id: number) => adminRequest(() => api.delete<void>(`/api/admin/${section}/${id}`));
export const getMateriaEspecialidades = (materiaId: number) => adminRequest(() => api.get<number[]>(`/api/admin/materias/${materiaId}/especialidades`));
export const getHoraCatedraCatalog = () => adminRequest(() => api.get<HoraCatedraItem[]>('/api/admin/horario/catalogo'));
export const getHorarioByAsignacion = (asignacionId: number) => adminRequest(() => api.get<HorarioSlotItem[]>(`/api/admin/horario/asignaciones/${asignacionId}`));
export const createHorarioSlot = (asignacionId: number, payload: { diaSemana: number; horaCatedraId: number; sala?: string }) => adminRequest(() => api.post<HorarioSlotItem>(`/api/admin/horario/asignaciones/${asignacionId}/slots`, payload));
export const deleteHorarioSlot = (id: number) => adminRequest(() => api.delete<void>(`/api/admin/horario/slots/${id}`));
export const downloadHorarioCurso = (cursoId: number) => adminRequest(() => apiDownload(`/api/admin/horario/export?cursoId=${cursoId}`, `horario-curso-${cursoId}.xlsx`));
export const getHorarioResumen = () => adminRequest(() => api.get<HorarioResumenCursoItem[]>('/api/admin/horario/resumen'));
export const previewHorarioImport = (cursoId: number, file: File) => { const form = new FormData(); form.append('file', file); return adminRequest(() => api.post<HorarioImportRowItem[]>(`/api/admin/horario/import/preview?cursoId=${cursoId}`, form)); };
export const confirmHorarioImport = (cursoId: number, file: File) => { const form = new FormData(); form.append('file', file); return adminRequest(() => api.post<HorarioImportResponse>(`/api/admin/horario/import/confirm?cursoId=${cursoId}`, form)); };
export const getHorarioCurso = (cursoId: number) => adminRequest(() => api.get<HorarioSlotItem[]>(`/api/admin/horario/curso?cursoId=${cursoId}`));
export const getAsignacionesPorCurso = (cursoId: number) => adminRequest(() => api.get<AsignacionResumenItem[]>(`/api/admin/horario/asignaciones-por-curso?cursoId=${cursoId}`));
export const getSistemaEstado = () => adminRequest(() => api.get<SistemaEstadoResponse>('/api/admin/sistema-estado'));
export const buscarPadres = (q: string) => adminRequest(() => api.get<PadreSummary[]>(`/api/admin/padres/buscar?q=${encodeURIComponent(q)}`));
export const getPadresDeAlumno = (alumnoId: number) => adminRequest(() => api.get<PadreSummary[]>(`/api/admin/alumnos/${alumnoId}/padres`));
export const linkPadreAlumno = (alumnoId: number, padreId: number) => adminRequest(() => api.post<void>(`/api/admin/alumnos/${alumnoId}/padres/${padreId}`, {}));
export const unlinkPadreAlumno = (alumnoId: number, padreId: number) => adminRequest(() => api.delete<void>(`/api/admin/alumnos/${alumnoId}/padres/${padreId}`));
export const wipePlanillaSyncImports = (planillaId: number) => adminRequest(() => api.post<{ message: string; deletedGrades: number; deletedTasks: number; clearedGoogleCourseIds: number }>(`/api/admin/planillas/${planillaId}/sync/wipe`));
export const wipeAllClassroomSync = () => adminRequest(() => api.post<{ message: string; deletedGrades: number; deletedTasks: number; clearedGoogleCourseIds: number }>(`/api/admin/sync/wipe-all`));
export const clearUserGoogleTokens = (userId: number) => adminRequest(() => api.post<{ message: string }>(`/api/admin/usuarios/${userId}/google/clear`));
