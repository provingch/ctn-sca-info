import { api } from './client';
export type ParentTaskStatus = 'CALIFICADA' | 'ENTREGADA_PENDIENTE' | 'NO_ENTREGADA' | 'PENDIENTE';
export type ParentStage = 'primera' | 'segunda';
export interface ParentTask { id: number; titulo: string; fecha: string; puntos: number | null; total: number; estado: ParentTaskStatus }
export interface ParentSubject { planillaId: number; materiaId: number; materia: string; etapa: ParentStage; puntos: number; total: number; porcentaje: number; nota: number; tareas: ParentTask[] }
export interface ParentResponse {
  hijos: Array<{ id: number; nombre: string; apellido: string; especialidad: string; promedio: number }>;
  selectedAlumnoId: number | null;
  materias: ParentSubject[];
}
export const getParentSummary = (alumnoId?: number) => api.get<ParentResponse>(`/api/padre${alumnoId ? `?alumnoId=${alumnoId}` : ''}`);
