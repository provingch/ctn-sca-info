import { describe, expect, it } from 'vitest';
import { APP_ANDROID_URL, getRoleNavigation } from './navigation';

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

  it.each([[1, true], [2, true], [3, false], [4, true], [5, true]])('nivel %i: app Android = %s', (level, expected) => {
    expect(Boolean(getRoleNavigation(level as number).appAndroid)).toBe(expected);
    expect(APP_ANDROID_URL).toBe('/app/sca.apk');
  });

  it('usa una navegación segura para niveles desconocidos', () => {
    expect(getRoleNavigation(99).primaryItems).toEqual([expect.objectContaining({ to: '/profile' })]);
  });
});
