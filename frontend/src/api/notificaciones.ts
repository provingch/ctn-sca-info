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

export const getNotificaciones = (soloNoLeidas = false) => api.get<NotificacionItem[]>(`/api/notificaciones${soloNoLeidas ? '?soloNoLeidas=true' : ''}`);
export const getNotificacionesContador = () => api.get<{ count: number }>('/api/notificaciones/contador');
export const marcarNotificacionLeida = (id: number) => api.post<void>(`/api/notificaciones/${id}/leer`);
