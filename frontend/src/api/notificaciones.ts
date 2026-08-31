import { api } from './client';

export interface NotificacionItem {
  id: number;
  usuarioId: number;
  userType?: string | null;
  tipo?: string | null;
  titulo: string;
  cuerpo: string;
  entidadTipo?: string | null;
  entidadId?: number | null;
  leida: boolean;
  createdAt: string;
}

export interface NotificacionesContador {
  count: number;
}

export interface NotificacionesLecturaResultado {
  ok: boolean;
  actualizadas?: number;
}

export const getNotificaciones = (soloNoLeidas = false) => api.get<NotificacionItem[]>(`/api/notificaciones${soloNoLeidas ? '?soloNoLeidas=true' : ''}`);
export const getNotificacionesContador = () => api.get<NotificacionesContador>('/api/notificaciones/contador');
export const marcarNotificacionLeida = (id: number) => api.post<NotificacionesLecturaResultado>(`/api/notificaciones/${id}/leer`);
export const marcarTodasNotificacionesLeidas = () => api.post<NotificacionesLecturaResultado>('/api/notificaciones/leer-todas');
