import { apiRequest, apiDownload } from './client';

export interface AsignacionOption {
  id: number;
  materiaId: number;
  materiaNombre?: string;
  estadoPlan?: 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'NO_CARGADO';
}

export interface TemaPlanDto {
  mes: string;
  ordenMes: number;
  bloque: number;
  capacidades?: string;
  temasContenidos: string;
  actividades?: string;
  instrumentos?: string;
  indicadorConceptual?: string;
  indicadorProcedimental?: string;
  indicadorActitudinal?: string;
}

export interface PlanCurricularEstado {
  id: number | null;
  estado: 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'NO_CARGADO';
  archivoNombre?: string;
  fechaSubida?: string;
  fechaRevision?: string;
  observacionesEvaluador?: string;
  temas?: TemaPlanDto[];
  etapa?: string;
  anio?: number;
}

export interface PlanPendienteResumen {
  id: number;
  estado: string;
  archivoNombre: string;
  fechaSubida: string;
  materiaNombre: string;
  profesorNombre: string;
  cursoDescripcion: string;
  especialidad?: string;
}

export function getAsignacionesDisponibles(cursoId: number) {
  return apiRequest<AsignacionOption[]>(`/api/plan-curricular/asignaciones-disponibles?cursoId=${cursoId}`);
}

export function downloadPlantilla(asignacionId: number) {
  return apiDownload(`/api/plan-curricular/plantilla?asignacionId=${asignacionId}`, 'plan-curricular-plantilla.xlsx');
}

export function getMiPlan(asignacionId: number, etapa: string, anio: number): Promise<PlanCurricularEstado | undefined> {
  return apiRequest<PlanCurricularEstado>(`/api/plan-curricular/mi-plan?asignacionId=${asignacionId}&etapa=${etapa}&anio=${anio}`);
}

export function uploadPlanCurricular(asignacionId: number, file: File): Promise<{ id: number }> {
  const formData = new FormData();
  formData.append('asignacionId', asignacionId.toString());
  formData.append('file', file);
  return apiRequest<{ id: number }>('/api/plan-curricular', {
    method: 'POST',
    body: formData,
  });
}

export function getPendientes(): Promise<PlanPendienteResumen[]> {
  return apiRequest<PlanPendienteResumen[]>('/api/plan-curricular/pendientes');
}

export function getPlanDetalle(id: number): Promise<PlanCurricularEstado> {
  return apiRequest<PlanCurricularEstado>(`/api/plan-curricular/${id}`);
}

export function descargarDocumentoOriginal(id: number) {
  return apiDownload(`/api/plan-curricular/${id}/documento`, 'plan-curricular.xlsx');
}

export function aprobarPlan(id: number): Promise<void> {
  return apiRequest<void>(`/api/plan-curricular/${id}/aprobar`, { method: 'POST' });
}

export function rechazarPlan(id: number, observaciones: string): Promise<void> {
  return apiRequest<void>(`/api/plan-curricular/${id}/rechazar?observaciones=${encodeURIComponent(observaciones)}`, { method: 'POST' });
}
