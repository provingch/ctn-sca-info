import { describe, expect, it } from 'vitest';
import { resolveIdentitySpecialty } from './authIdentity';

describe('admin identity scope', () => {
  it('rechaza un perfil administrador sin especialidadId', () => {
    expect(() => resolveIdentitySpecialty(3, {}, [])).toThrow('no informó el alcance');
  });

  it('distingue un administrador global mediante null', () => {
    expect(resolveIdentitySpecialty(3, { especialidadId: null }, [])).toEqual({ id: null, name: null });
  });

  it('resuelve el nombre de una especialidad asignada', () => {
    expect(resolveIdentitySpecialty(3, { especialidadId: 2 }, [{ id: 2, nombre: 'Informática' }])).toEqual({ id: 2, name: 'Informática' });
  });
});
