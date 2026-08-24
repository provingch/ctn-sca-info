import { describe, expect, it } from 'vitest';
import { getRoleNavigation } from './navigation';

describe('getRoleNavigation', () => {
  it.each([
    [1, 'Profesor', '/home'],
    [2, 'Evaluador', '/evaluacion'],
    [3, 'Administrador', '/admin'],
    [4, 'Padre / Encargado', '/padre'],
  ])('devuelve la navegación del nivel %i', (level, role, firstPath) => {
    const navigation = getRoleNavigation(level as number);
    expect(navigation.roleLabel).toBe(role);
    expect(navigation.primaryItems[0].to).toBe(firstPath);
    expect(navigation.primaryItems).toContainEqual(expect.objectContaining({ to: '/profile' }));
  });

  it('usa una navegación segura para niveles desconocidos', () => {
    expect(getRoleNavigation(99).primaryItems).toEqual([expect.objectContaining({ to: '/profile' })]);
  });
});
