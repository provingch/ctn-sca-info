import { api, apiDownload, apiRequest } from './client';
import type { PlanillaDetail } from './academics';

export const TIPO_ATRASO = 'ATRASO';
export const TIPO_INCONGRUENCIA_RETROACTIVA = 'INCONGRUENCIA_RETROACTIVA';
export const TIPO_BLOQUEO_INCONGRUENCIA_RETROACTIVA = 'BLOQUEO_INCONGRUENCIA_RETROACTIVA';
export const TIPO_BLOQUEO_ATRASO_TIEMPO_REAL = 'BLOQUEO_ATRASO_TIEMPO_REAL';

export interface IncumplimientoPendiente {
  id: number;
  asignacionId: number;
  usuarioId: number;
  tipo: string;
  descripcion: string;
  estado: 'PENDIENTE';
  fechaCreacion?: string;
  usuarioNombre?: string;
  usuarioApellido?: string;
  materiaNombre?: string | null;
  /** En atrasos e incongruencias retroactivas: la clase que originó el caso. */
  planillaRasgoId?: number | null;
  fechaClase?: string | null;
  temaIngresado?: string | null;
  temaEsperado?: string | null;
  justificacionProfesor?: string | null;
}

export interface ResolucionIncumplimiento {
  estado: 'PERMITIDO' | 'RECHAZADO';
  /** Nota libre al reactivar un bloqueo de Iniciar clase. */
  nota?: string;
}

export interface ResultadoResolucion {
  ok: boolean;
  /** true si este rechazo alcanzó el umbral y bloqueó "Iniciar clase" en la asignación. */
  bloqueoGenerado?: boolean;
}

export function getIncumplimientos(): Promise<IncumplimientoPendiente[]> {
  return apiRequest<IncumplimientoPendiente[]>('/api/evaluacion/incumplimientos');
}

export function resolverIncumplimiento(id: number, resolucion: ResolucionIncumplimiento): Promise<ResultadoResolucion> {
  return apiRequest<ResultadoResolucion>(`/api/evaluacion/incumplimientos/${id}/resolver`, {
    method: 'POST',
    body: resolucion,
  });
}

/** Incongruencias retroactivas pendientes del profesor autenticado. */
export function getMisIncongruencias(): Promise<IncumplimientoPendiente[]> {
  return api.get<IncumplimientoPendiente[]>('/api/incumplimientos/mis-incongruencias');
}

export function justificarIncongruencia(id: number, justificacion: string): Promise<{ ok: boolean }> {
  return api.patch<{ ok: boolean }>(`/api/incumplimientos/${id}/justificar`, { justificacion });
}

/** Una planilla de un curso, con su profesor y el estado de cierre (vista de evaluación, sólo lectura). */
export interface PlanillaResumen {
  id: number;
  cursoId: number;
  materiaId: number;
  materiaNombre: string;
  profesorId: number;
  profesorNombre: string;
  etapaIndex: number;
  periodo: number;
  fechaCierreEtapa1: string | null;
  etapa1Confirmada: boolean;
  fechaCierreEtapa2: string | null;
  etapa2Confirmada: boolean;
}

function planillasQuery(cursoId: number, etapa: string, periodo: number, materiaId?: number): string {
  return `cursoId=${cursoId}&etapa=${encodeURIComponent(etapa)}&periodo=${periodo}${materiaId && materiaId > 0 ? `&materiaId=${materiaId}` : ''}`;
}

export const listarPlanillas = (cursoId: number, etapa: string, periodo: number, materiaId?: number) =>
  api.get<PlanillaResumen[]>(`/api/evaluacion/planillas?${planillasQuery(cursoId, etapa, periodo, materiaId)}`);

/** Mismo detalle que ve el profesor, pero sin exigir ser el dueño y sin crear filas de registro. */
export const getPlanillaEvaluacion = (id: number) => api.get<PlanillaDetail>(`/api/evaluacion/planillas/${id}`);

/** Reabre una etapa cerrada; el motivo es obligatorio y queda en el registro de actividad. */
export const reabrirEtapaEvaluacion = (planillaId: number, etapa: 1 | 2, motivo: string) =>
  api.post<{ planillaId: number }>(`/api/evaluacion/planillas/${planillaId}/etapa${etapa}/reabrir`, { motivo });

/** Descarga el Excel de las planillas que matchean, con la sesión (un <a href> no lleva el token). */
export const descargarPlanillas = (cursoId: number, etapa: string, periodo: number, materiaId?: number) =>
  apiDownload(`/api/evaluacion/export?${planillasQuery(cursoId, etapa, periodo, materiaId)}`, 'planillas.xlsx');
