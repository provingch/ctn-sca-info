import { api, apiRequest } from './client';

export const TIPO_INCONGRUENCIA_RETROACTIVA = 'INCONGRUENCIA_RETROACTIVA';
export const TIPO_BLOQUEO_INCONGRUENCIA_RETROACTIVA = 'BLOQUEO_INCONGRUENCIA_RETROACTIVA';

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
  /** Sólo en incongruencias retroactivas: la clase que originó el caso. */
  planillaRasgoId?: number | null;
  fechaClase?: string | null;
  temaIngresado?: string | null;
  temaEsperado?: string | null;
  justificacionProfesor?: string | null;
}

export interface ResolucionIncumplimiento {
  estado: 'PERMITIDO' | 'RECHAZADO';
  suspensionDesde?: string;
  suspensionHasta?: string;
  /** Nota libre al reactivar un bloqueo retroactivo. */
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
