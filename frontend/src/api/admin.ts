import { api } from './client';

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

import { apiDownload } from './client';

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

export const getAdminCatalog = () => api.get<AdminCatalog>('/api/admin');
export const createAdminRecord = (section: string, payload: unknown) => api.post<void>(`/api/admin/${section}`, payload);
export const updateAdminRecord = (section: string, id: number, payload: unknown) => api.put<void>(`/api/admin/${section}/${id}`, payload);
export const deleteAssignment = (id: number) => api.delete<void>(`/api/admin/asignaciones/${id}`);
export const deleteAdminRecord = (section: string, id: number) => api.delete<void>(`/api/admin/${section}/${id}`);
export const getMateriaEspecialidades = (materiaId: number) => api.get<number[]>(`/api/admin/materias/${materiaId}/especialidades`);
export const getHoraCatedraCatalog = () => api.get<HoraCatedraItem[]>('/api/admin/horario/catalogo');
export const getHorarioByAsignacion = (asignacionId: number) => api.get<HorarioSlotItem[]>(`/api/admin/horario/asignaciones/${asignacionId}`);
export const createHorarioSlot = (asignacionId: number, payload: { diaSemana: number; horaCatedraId: number; sala?: string }) => api.post<HorarioSlotItem>(`/api/admin/horario/asignaciones/${asignacionId}/slots`, payload);
export const deleteHorarioSlot = (id: number) => api.delete<void>(`/api/admin/horario/slots/${id}`);
export const downloadHorarioCurso = (cursoId: number) => apiDownload(`/api/admin/horario/export?cursoId=${cursoId}`, `horario-curso-${cursoId}.xlsx`);
export const getHorarioResumen = () => api.get<HorarioResumenCursoItem[]>('/api/admin/horario/resumen');
export const getSistemaEstado = () => api.get<SistemaEstadoResponse>('/api/admin/sistema-estado');
export const buscarPadres = (q: string) => api.get<PadreSummary[]>(`/api/admin/padres/buscar?q=${encodeURIComponent(q)}`);
export const getPadresDeAlumno = (alumnoId: number) => api.get<PadreSummary[]>(`/api/admin/alumnos/${alumnoId}/padres`);
export const linkPadreAlumno = (alumnoId: number, padreId: number) => api.post<void>(`/api/admin/alumnos/${alumnoId}/padres/${padreId}`, {});
export const unlinkPadreAlumno = (alumnoId: number, padreId: number) => api.delete<void>(`/api/admin/alumnos/${alumnoId}/padres/${padreId}`);
export const wipePlanillaSyncImports = (planillaId: number) => api.post<{ message: string; deletedGrades: number; deletedTasks: number; clearedGoogleCourseIds: number }>(`/api/admin/planillas/${planillaId}/sync/wipe`);
export const wipeAllClassroomSync = () => api.post<{ message: string; deletedGrades: number; deletedTasks: number; clearedGoogleCourseIds: number }>(`/api/admin/sync/wipe-all`);
export const clearUserGoogleTokens = (userId: number) => api.post<{ message: string }>(`/api/admin/usuarios/${userId}/google/clear`);
