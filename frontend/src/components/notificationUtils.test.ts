import { describe, expect, it } from 'vitest';
import type { NotificacionItem } from '../api/notificaciones';
import { formatNotificationDate, notificationDestination } from './notificationUtils';

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
