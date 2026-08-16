import { api } from './client';
export interface AdminCatalog {
  materias: Array<{ id: number; nombre: string; categoria: string }>;
  usuarios: Array<{ id: number; nombre: string; apellido: string; usuario: string; nivel: number; correo: string | null; especialidadId: number | null }>;
  asignaciones: Array<{ id: number; profesorId: number; materiaId: number; cursoId: number; profesor: string; materia: string; curso: string }>;
  alumnos: Array<{ id: number; nombre: string; apellido: string; cursoId: number; ci: number | null; correoEncargado: string | null; correoEncargado2: string | null }>;
  cursos: Array<{ id: number; especialidad: string; nivel: number; seccion: string }>;
  especialidades: Array<{ id: number; nombre: string }>;
}
export const getAdminCatalog = () => api.get<AdminCatalog>('/api/admin');
export const createAdminRecord = (section: string, payload: unknown) => api.post<void>(`/api/admin/${section}`, payload);
export const deleteAssignment = (id: number) => api.delete<void>(`/api/admin/asignaciones/${id}`);
export const deleteAdminRecord = (section: string, id: number) => api.delete<void>(`/api/admin/${section}/${id}`);
export const getMateriaEspecialidades = (materiaId: number) => api.get<number[]>(`/api/admin/materias/${materiaId}/especialidades`);
export const wipePlanillaSyncImports = (planillaId: number) => api.post<{ message: string; deletedGrades: number; deletedTasks: number; clearedGoogleCourseIds: number }>(`/api/admin/planillas/${planillaId}/sync/wipe`);
export const wipeAllClassroomSync = () => api.post<{ message: string; deletedGrades: number; deletedTasks: number; clearedGoogleCourseIds: number }>(`/api/admin/sync/wipe-all`);
