import type { NotificacionItem } from '../api/notificaciones';
import { formatSqlDateTime } from '../utils/date';

/** Disparado cuando algo fuera de la campana cambia el estado de notificaciones (p. ej. el padre vio a un hijo). */
export const NOTIFICATIONS_CHANGED_EVENT = 'sca-notifications-changed';

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
  if (type === 'PLANILLA_REABIERTA') {
    // Evaluación o admin reabrió una etapa: lleva al profesor a esa planilla, que vuelve a ser editable.
    return notification.entidadId ? `/planilla/${notification.entidadId}` : '/home';
  }
  if (type === 'INCUMPLIMIENTO_RESUELTO' || PROFESOR_PLAN_TYPES.has(type)) {
    return '/home?view=catedra&subview=plan-curricular';
  }
  if (type === 'NOVEDAD_ALUMNO' && entityType === 'ALUMNO' && notification.entidadId) {
    return `/padre?alumnoId=${notification.entidadId}`;
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

/** Tipos que el backend resuelve solo (no se pueden marcar leídos a mano desde la campana). */
const TIPOS_NO_DESCARTABLES = new Set(['PLAN_PENDIENTE', 'NOVEDAD_ALUMNO']);

/** El recordatorio de plan pendiente y la novedad de un hijo no se marcan leídos a mano. */
export function isResolvedAutomatically(notification: NotificacionItem): boolean {
  return TIPOS_NO_DESCARTABLES.has(normalized(notification.tipo));
}

export function formatNotificationDate(value?: string | null): string {
  return formatSqlDateTime(value);
}
