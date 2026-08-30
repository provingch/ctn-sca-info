import { api } from './client';

export interface NotificacionItem {
  id: number;
  mensaje: string;
  fecha: string;
  leida: boolean;
  tipo?: string | null;
  entidadTipo?: string | null;
  entidadId?: number | null;
}

export const getNotificaciones = (soloNoLeidas = false) => api.get<NotificacionItem[]>(`/api/notificaciones${soloNoLeidas ? '?soloNoLeidas=true' : ''}`);
export const getNotificacionesContador = () => api.get<number>('/api/notificaciones/contador');
export const marcarNotificacionLeida = (id: number) => api.post<void>(`/api/notificaciones/${id}/leer`);
