import { api } from './client';

export interface Instrumento { id: number; nombre: string }
export interface Tarea {
  id: number; planillaId: number; instrumentoId: number; fecha: string; total: number; titulo: string;
  fechaInicio: string | null; fechaLimite: string | null; googleCourseworkId: string | null;
  googleCourseworkUrl: string | null; gradesCleared?: boolean; warning?: string | null;
}
export interface PlanillaDetail {
  planilla: { id: number; cursoId: number; materiaId: number; materiaNombre: string; categoria: string; etapa: string; etapaIndex: number; periodo: number; exigenciaPorcentaje: number; totalPossiblePoints: number; planillaDesde: string | null; planillaHasta: string | null; googleCourseId?: string | null };
  curso: { id: number; especialidad: string; seccion: string; nivel: number } | null;
  tareas: Tarea[];
  rows: Array<{ registroId: number; alumnoId: number; alumnoNombre: string; grades: Array<{ tareaId: number; puntos: number | null }>; total: number; porcentaje: number; nota: number }>;
  gradeRanges: Record<string, { minInclusive: number; maxInclusive: number }>;
  warnings: string[];
}
export interface CursoEvaluacion { id: number; especialidad: string; nivel: number; seccion: string }
export interface Especialidad { id: number; nombre: string }

export const getPlanilla = (id: number) => api.get<PlanillaDetail>(`/api/planillas/${id}`);
export const resolvePlanilla = (cursoId: number, materiaId: number, etapa: number) => api.post<{ planillaId: number }>('/api/planillas/resolve', { cursoId, materiaId, etapa });
export const saveGrades = (id: number, grades: unknown[]) => api.post<{ message: string; warnings: string[] }>(`/api/planillas/${id}/notas`, { grades });
export interface ClassroomSyncResponse { planillaId: number; googleCourseId?: string | null; classroomCourseMapped: boolean; importedCourseworks: number; linkedStudents: number; importedGrades: number; courseName?: string | null; courseSection?: string | null; courseAlternateLink?: string | null; message: string }
export const syncClassroom = (id: number) => api.post<ClassroomSyncResponse>(`/api/planillas/${id}/sync/classroom`);
export const confirmClassroomMapping = (planillaId: number, googleCourseId: string) => api.post<{ message: string }>(`/api/planillas/${planillaId}/classroom`, { googleCourseId });

export const getInstrumentos = () => api.get<Instrumento[]>('/api/instrumentos');
export const getTarea = (id: number) => api.get<Tarea>(`/api/tareas/${id}`);
export const createTarea = (planillaId: number, body: Omit<Tarea, 'id' | 'planillaId' | 'fechaInicio' | 'fechaLimite' | 'googleCourseworkId' | 'googleCourseworkUrl'>) => api.post<Tarea>(`/api/planillas/${planillaId}/tareas`, body);
export const updateTarea = (id: number, body: { instrumentoId: number; fecha: string; total: number; titulo: string }) => api.put<Tarea>(`/api/tareas/${id}`, body);
export const deleteTarea = (id: number) => api.delete<void>(`/api/tareas/${id}`);
export const getEspecialidades = () => api.get<Especialidad[]>('/api/evaluacion/especialidades');
export const getCursosEvaluacion = (especialidadId?: number) => api.get<CursoEvaluacion[]>(`/api/evaluacion/cursos${especialidadId ? `?especialidadId=${especialidadId}` : ''}`);
