import { apiRequest } from './client';

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
}

export interface ResolucionIncumplimiento {
  estado: 'PERMITIDO' | 'RECHAZADO';
  suspensionDesde?: string;
  suspensionHasta?: string;
}

export function getIncumplimientos(): Promise<IncumplimientoPendiente[]> {
  return apiRequest<IncumplimientoPendiente[]>('/api/evaluacion/incumplimientos');
}

export function resolverIncumplimiento(id: number, resolucion: ResolucionIncumplimiento): Promise<{ ok: boolean }> {
  return apiRequest<{ ok: boolean }>(`/api/evaluacion/incumplimientos/${id}/resolver`, {
    method: 'POST',
    body: resolucion,
  });
}
