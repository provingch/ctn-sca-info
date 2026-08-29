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
  estadoCobertura?: 'PENDIENTE' | 'CUBIERTO';
  fechaCobertura?: string;
}

export interface PlanCurricularEstado {
  id: number | null;
  estado: 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'NO_CARGADO';
  archivoNombre?: string;
  fechaSubida?: string;
  fechaRevision?: string;
  observacionesEvaluador?: string;
  materiaNombre?: string;
  profesorNombre?: string;
  cursoDescripcion?: string;
  especialidad?: string;
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
  fechaRevision?: string;
  etapa?: string;
  anio?: number;
}

export interface AsignacionCompleta {
  id: number;
  materiaId: number;
  materiaNombre: string;
  especialidadId: number;
  especialidadNombre: string;
  cursoId: number;
  cursoOrdinal: string;
  seccion: string;
  estadoPlan: 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'NO_CARGADO';
}

export interface PlanHistorialItem {
  id: number;
  estado: 'PENDIENTE' | 'APROBADO' | 'RECHAZADO';
  archivoNombre: string;
  fechaSubida: string;
  fechaRevision?: string;
  observacionesEvaluador?: string;
  etapa: string;
  anio: number;
  materiaNombre: string;
  especialidadId: number;
  especialidadNombre: string;
  cursoOrdinal: string;
  seccion: string;
  asignacionId: number;
}

export interface AsignacionCandidata {
  id: number;
  descripcion: string;
}

export interface MultiplesCoincidenciasError {
  mensaje: string;
  candidatas: AsignacionCandidata[];
}

export function getAsignacionesDisponibles(cursoId: number) {
  return apiRequest<AsignacionOption[]>(`/api/plan-curricular/asignaciones-disponibles?cursoId=${cursoId}`);
}

export function getMisAsignaciones(): Promise<AsignacionCompleta[]> {
  return apiRequest<AsignacionCompleta[]>('/api/plan-curricular/mis-asignaciones');
}

export function getMisPlanes(): Promise<PlanHistorialItem[]> {
  return apiRequest<PlanHistorialItem[]>('/api/plan-curricular/mios');
}

export function subirPlanAutoDetectado(file: File, asignacionId?: number): Promise<{
  id: number;
  especialidadNombre: string;
  cursoOrdinal: string;
  seccion: string;
  materiaNombre: string;
}> {
  const formData = new FormData();
  formData.append('file', file);
  if (asignacionId !== undefined) formData.append('asignacionId', asignacionId.toString());
  return apiRequest<{ id: number; especialidadNombre: string; cursoOrdinal: string; seccion: string; materiaNombre: string } | string>('/api/plan-curricular', { method: 'POST', body: formData }).then((result) => {
    if (typeof result !== 'string') return result;
    const id = Number(/\d+/.exec(result)?.[0] ?? 0);
    return { id, especialidadNombre: '', cursoOrdinal: '', seccion: '', materiaNombre: '' };
  });
}

export function downloadPlantilla(asignacionId: number, etapa?: string) {
  const etapaQuery = etapa ? `&etapa=${encodeURIComponent(etapa)}` : '';
  return apiDownload(`/api/plan-curricular/plantilla?asignacionId=${asignacionId}${etapaQuery}`, 'plan-curricular-plantilla.xlsx');
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

export function getAprobados(): Promise<PlanPendienteResumen[]> {
  return apiRequest<PlanPendienteResumen[]>('/api/plan-curricular/aprobados');
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
