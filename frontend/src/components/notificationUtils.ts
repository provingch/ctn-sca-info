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
    return userLevel === 2 ? '/evaluacion?view=seguimiento&tab=incumplimientos' : '/home?view=catedra&subview=plan-curricular';
  }
  if (type === 'INCUMPLIMIENTO_RESUELTO' || PROFESOR_PLAN_TYPES.has(type)) {
    return '/home?view=catedra&subview=plan-curricular';
  }
  return null;
}

/** Avisos del profesor sobre su plan curricular y las clases dadas antes de que estuviera aprobado. */
const PROFESOR_PLAN_TYPES = new Set([
  'PLAN_PENDIENTE',
  'PLAN_ACEPTADO',
  'PLAN_RECHAZADO',
  'PLAN_RETROACTIVO_INCONGRUENCIAS',
  'INICIAR_CLASE_BLOQUEADO',
  'BLOQUEO_RETROACTIVO_LEVANTADO',
]);

/** El recordatorio de plan pendiente no se marca leído a mano: se cierra solo al subir el plan. */
export function isResolvedAutomatically(notification: NotificacionItem): boolean {
  return normalized(notification.tipo) === 'PLAN_PENDIENTE';
}

export function formatNotificationDate(value?: string | null): string {
  return formatSqlDateTime(value);
}
