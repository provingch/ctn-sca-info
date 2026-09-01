import type { NotificacionItem } from '../api/notificaciones';
import { formatSqlDateTime } from '../utils/date';

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
  return formatSqlDateTime(value);
}
