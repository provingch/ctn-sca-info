import { describe, expect, it } from 'vitest';
import type { NotificacionItem } from '../api/notificaciones';
import { formatNotificationDate, isResolvedAutomatically, notificationDestination } from './notificationUtils';

const base: NotificacionItem = {
  id: 1,
  usuarioId: 2,
  titulo: 'Aviso',
  cuerpo: 'Detalle',
  leida: false,
  createdAt: '2026-08-30 10:15:00',
};

describe('notificationDestination', () => {
  it('reconoce los tipos y entidades en mayúsculas que guarda el backend', () => {
    expect(notificationDestination({ ...base, tipo: 'COORDINACION', entidadTipo: 'QUEJA', entidadId: 44 }, 5))
      .toBe('/coordinacion?view=quejas');
    expect(notificationDestination({ ...base, tipo: 'INCUMPLIMIENTO', entidadTipo: 'INCUMPLIMIENTO_REVISION' }, 2))
      .toBe('/evaluacion?view=seguimiento&tab=incumplimientos');
  });

  it('manda al profesor a la pestaña de plan curricular ante incumplimientos, no al menú', () => {
    expect(notificationDestination({ ...base, tipo: 'INCUMPLIMIENTO', entidadTipo: 'INCUMPLIMIENTO_REVISION' }, 1))
      .toBe('/home?view=catedra&subview=plan-curricular');
    expect(notificationDestination({ ...base, tipo: 'INCUMPLIMIENTO_RESUELTO' }, 1))
      .toBe('/home?view=catedra&subview=plan-curricular');
  });

  it('lleva al profesor a plan curricular desde los avisos de plan y de incongruencias retroactivas', () => {
    for (const tipo of ['PLAN_PENDIENTE', 'PLAN_ACEPTADO', 'PLAN_RECHAZADO', 'PLAN_RETROACTIVO_INCONGRUENCIAS', 'INICIAR_CLASE_BLOQUEADO', 'BLOQUEO_RETROACTIVO_LEVANTADO']) {
      expect(notificationDestination({ ...base, tipo }, 1)).toBe('/home?view=catedra&subview=plan-curricular');
    }
  });

  it('devuelve null para avisos informativos sin un destino conocido', () => {
    expect(notificationDestination({ ...base, tipo: 'GENERAL' }, 1)).toBeNull();
  });
});

describe('formatNotificationDate', () => {
  it('conserva un valor inválido en vez de mostrar Invalid Date', () => {
    expect(formatNotificationDate('fecha desconocida')).toBe('fecha desconocida');
    expect(formatNotificationDate(undefined)).toBe('Fecha no disponible');
  });
});

describe('isResolvedAutomatically', () => {
  it('sólo el recordatorio de plan pendiente se resuelve solo al subir el plan', () => {
    expect(isResolvedAutomatically({ ...base, tipo: 'PLAN_PENDIENTE' })).toBe(true);
    expect(isResolvedAutomatically({ ...base, tipo: 'plan_pendiente' })).toBe(true);
    expect(isResolvedAutomatically({ ...base, tipo: 'PLAN_ACEPTADO' })).toBe(false);
    expect(isResolvedAutomatically({ ...base })).toBe(false);
  });
});
