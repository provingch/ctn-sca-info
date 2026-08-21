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
export const buscarPadres = (q: string) => api.get<PadreSummary[]>(`/api/admin/padres/buscar?q=${encodeURIComponent(q)}`);
export const getPadresDeAlumno = (alumnoId: number) => api.get<PadreSummary[]>(`/api/admin/alumnos/${alumnoId}/padres`);
export const linkPadreAlumno = (alumnoId: number, padreId: number) => api.post<void>(`/api/admin/alumnos/${alumnoId}/padres/${padreId}`, {});
export const unlinkPadreAlumno = (alumnoId: number, padreId: number) => api.delete<void>(`/api/admin/alumnos/${alumnoId}/padres/${padreId}`);
export const wipePlanillaSyncImports = (planillaId: number) => api.post<{ message: string; deletedGrades: number; deletedTasks: number; clearedGoogleCourseIds: number }>(`/api/admin/planillas/${planillaId}/sync/wipe`);
export const wipeAllClassroomSync = () => api.post<{ message: string; deletedGrades: number; deletedTasks: number; clearedGoogleCourseIds: number }>(`/api/admin/sync/wipe-all`);
export const clearUserGoogleTokens = (userId: number) => api.post<{ message: string }>(`/api/admin/usuarios/${userId}/google/clear`);
