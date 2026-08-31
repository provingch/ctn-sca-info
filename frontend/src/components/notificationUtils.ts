import type { NotificacionItem } from '../api/notificaciones';

function normalized(value?: string | null): string {
  return value?.trim().toLocaleUpperCase('es') ?? '';
}

export function notificationDestination(notification: NotificacionItem, userLevel?: number | null): string | null {
  const type = normalized(notification.tipo);
  const entityType = normalized(notification.entidadTipo);

  if (type === 'COORDINACION' || type === 'QUEJA_ACUMULADA' || entityType === 'QUEJA') {
    return userLevel === 5 ? '/coordinacion?view=quejas' : '/admin/quejas';
  }
  if (type === 'INCUMPLIMIENTO' || entityType === 'INCUMPLIMIENTO' || entityType === 'INCUMPLIMIENTO_REVISION') {
    return userLevel === 2 ? '/evaluacion?view=seguimiento&tab=incumplimientos' : '/home?view=catedra';
  }
  if (type === 'INCUMPLIMIENTO_RESUELTO') {
    return '/home?view=catedra';
  }
  return null;
}

export function formatNotificationDate(value?: string | null): string {
  if (!value) return 'Fecha no disponible';
  const normalizedValue = value.includes(' ') && !value.includes('T') ? value.replace(' ', 'T') : value;
  const date = new Date(normalizedValue);
  if (Number.isNaN(date.getTime())) return value || 'Fecha no disponible';
  return date.toLocaleString('es-PY', { dateStyle: 'short', timeStyle: 'short' });
}
