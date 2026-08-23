import { apiRequest, apiDownload } from './client';

export interface AsignacionOption {
  id: number;
  materiaId: number;
  materiaNombre?: string;
}

export function getAsignacionesDisponibles(cursoId: number) {
  return apiRequest<AsignacionOption[]>(`/api/plan-curricular/asignaciones-disponibles?cursoId=${cursoId}`);
}

export function downloadPlantilla(asignacionId: number) {
  return apiDownload(`/api/plan-curricular/plantilla?asignacionId=${asignacionId}`, 'plan-curricular-plantilla.xlsx');
}
