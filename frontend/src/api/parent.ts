import { api } from './client';
export type ParentTaskStatus = 'CALIFICADA' | 'ENTREGADA_PENDIENTE' | 'NO_ENTREGADA' | 'PENDIENTE';
export interface ParentResponse { hijos: Array<{ id: number; nombre: string; apellido: string; especialidad: string; promedio: number }>; selectedAlumnoId: number | null; materias: Array<{ planillaId: number; materiaId: number; materia: string; puntos: number; total: number; porcentaje: number; nota: number; tareas: Array<{ id: number; titulo: string; fecha: string; puntos: number | null; total: number; estado: ParentTaskStatus }> }> }
export const getParentSummary = (alumnoId?: number) => api.get<ParentResponse>(`/api/padre${alumnoId ? `?alumnoId=${alumnoId}` : ''}`);
