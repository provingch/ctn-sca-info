/**
 * Cliente HTTP central.
 *
 * Decisión de storage del JWT (Bloque 1/2, ver contexto de sesión):
 * - El access token vive SOLO en memoria (variable de módulo), nunca en
 *   localStorage/sessionStorage. Se pierde en cada full reload, a propósito:
 *   así se evita la superficie de ataque XSS que implicaría persistirlo.
 * - La sesión de largo plazo ("recordarme") la resuelve el backend con la
 *   cookie httpOnly `SCA_REMEMBER` (ver AuthController.java), que este
 *   cliente nunca lee ni escribe directamente — el browser la maneja sola
 *   porque el front se sirve same-origin desde el mismo jar de Spring Boot.
 * - Al recibir 401, este cliente intenta UNA vez `POST /api/auth/refresh`
 *   (que manda la cookie automáticamente) para obtener un access token
 *   nuevo sin pedir login. Si el refresh también falla, se limpia el
 *   estado y se notifica a AuthContext para forzar logout.
 */

let accessToken: string | null = null;

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function getAccessToken(): string | null {
  return accessToken;
}

/** Callback que AuthContext registra para reaccionar cuando el refresh falla. */
let onAuthExpired: (() => void) | null = null;

export function setOnAuthExpired(handler: (() => void) | null): void {
  onAuthExpired = handler;
}

const API_BASE = ''; // same-origin: '' + '/api/...' resuelve contra el propio host

interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown;
  /** Si es false, no intenta refresh automático en un 401 (usado por /auth/refresh mismo). */
  allowRefreshRetry?: boolean;
}

export class ApiError extends Error {
  status: number;
  body: unknown;

  constructor(status: number, message: string, body?: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

async function parseBody(response: Response): Promise<unknown> {
  const contentType = response.headers.get('content-type') ?? '';
  if (!contentType.includes('application/json')) {
    const text = await response.text();
    return text.length > 0 ? text : null;
  }
  try {
    return await response.json();
  } catch {
    return null;
  }
}

async function rawRequest(path: string, options: RequestOptions = {}): Promise<Response> {
  const { body, headers, ...rest } = options;

  const finalHeaders: HeadersInit = {
    ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
    ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    ...headers,
  };

  return fetch(`${API_BASE}${path}`, {
    ...rest,
    headers: finalHeaders,
    // same-origin: necesario para que la cookie SCA_REMEMBER viaje en /auth/refresh y /auth/logout
    credentials: 'same-origin',
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
}

/** Intenta renovar el access token usando la cookie httpOnly. Devuelve true si tuvo éxito. */
async function tryRefresh(): Promise<boolean> {
  try {
    const response = await rawRequest('/api/auth/refresh', {
      method: 'POST',
      allowRefreshRetry: false,
    });
    if (!response.ok) {
      return false;
    }
    const data = (await parseBody(response)) as { accessToken?: string } | null;
    if (!data?.accessToken) {
      return false;
    }
    setAccessToken(data.accessToken);
    return true;
  } catch {
    return false;
  }
}

/**
 * Request autenticado genérico. Lanza ApiError si la respuesta no es 2xx
 * (después de intentar refresh una vez, si corresponde).
 */
export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { allowRefreshRetry = true, ...rest } = options;

  let response = await rawRequest(path, rest);

  if (response.status === 401 && allowRefreshRetry) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      response = await rawRequest(path, rest);
    } else {
      setAccessToken(null);
      onAuthExpired?.();
    }
  }

  if (!response.ok) {
    const body = await parseBody(response);
    const message =
      typeof body === 'string' && body.length > 0
        ? body
        : `Error ${response.status} en ${path}`;
    throw new ApiError(response.status, message, body);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await parseBody(response)) as T;
}

export const api = {
  get: <T>(path: string) => apiRequest<T>(path, { method: 'GET' }),
  post: <T>(path: string, body?: unknown) => apiRequest<T>(path, { method: 'POST', body }),
  put: <T>(path: string, body?: unknown) => apiRequest<T>(path, { method: 'PUT', body }),
  delete: <T>(path: string) => apiRequest<T>(path, { method: 'DELETE' }),
};
