import { describe, expect, it } from 'vitest';
import { ADMIN_FORBIDDEN_MESSAGE, normalizeAdminError } from './admin';
import { ApiError } from './client';

describe('admin API errors', () => {
  it('convierte un 403 en un mensaje administrativo claro', () => {
    const result = normalizeAdminError(new ApiError(403, 'Forbidden'));
    expect(result).toBeInstanceOf(ApiError);
    expect((result as ApiError).status).toBe(403);
    expect((result as ApiError).message).toBe(ADMIN_FORBIDDEN_MESSAGE);
  });

  it('preserva errores que no sean 403', () => {
    const original = new ApiError(409, 'Conflicto');
    expect(normalizeAdminError(original)).toBe(original);
  });
});
