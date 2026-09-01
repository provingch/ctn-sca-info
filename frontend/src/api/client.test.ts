import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { apiRequest, extractErrorMessage, getAccessToken, setAccessToken, setOnAuthExpired } from './client';

describe('client refresh deduplication', () => {
  it('prefers the backend JSON message over the generic fallback', () => {
    expect(extractErrorMessage({ message: 'La contraseña actual es incorrecta.' }, '400', '/api/auth/change-password')).toBe('La contraseña actual es incorrecta.');
    expect(extractErrorMessage({ error: 'No autorizado' }, '401', '/api/secure')).toBe('No autorizado');
  });

  beforeEach(() => {
    setAccessToken('expired-token');
    setOnAuthExpired(null);
  });

  afterEach(() => {
    vi.restoreAllMocks();
    setAccessToken(null);
    setOnAuthExpired(null);
  });

  it('reuses a single refresh in-flight across concurrent 401s', async () => {
    const refreshCalls: string[] = [];

    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = init?.method ?? 'GET';

      if (url === '/api/auth/refresh' && method === 'POST') {
        refreshCalls.push(url);
        return new Response(JSON.stringify({ accessToken: 'fresh-token' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        });
      }

      if (url === '/api/protected') {
        const protectedCalls = fetchMock.mock.calls.filter(([candidate]) => String(candidate) === '/api/protected').length;

        if (protectedCalls <= 2) {
          return new Response(JSON.stringify({ error: 'expired' }), {
            status: 401,
            headers: { 'Content-Type': 'application/json' },
          });
        }

        return new Response(JSON.stringify({ ok: true }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        });
      }

      throw new Error(`Unexpected fetch: ${url} ${method}`);
    });

    vi.stubGlobal('fetch', fetchMock);

    const [first, second] = await Promise.all([
      apiRequest<{ ok: boolean }>('/api/protected'),
      apiRequest<{ ok: boolean }>('/api/protected'),
    ]);

    expect(first).toEqual({ ok: true });
    expect(second).toEqual({ ok: true });
    expect(refreshCalls).toHaveLength(1);
    expect(getAccessToken()).toBe('fresh-token');
  });
});
