/**
 * Espejo de AuthController.java (backend/src/main/java/ctn/informatica/sca/controller/AuthController.java)
 */
import { apiRequest } from './client';

export interface LoginRequest {
  username: string;
  password: string;
  rememberMe?: boolean;
}

export interface LoginResponse {
  requiere2fa: boolean;
  tempToken: string | null;
  accessToken: string | null;
  level: number | null;
}

export interface Verify2faRequest {
  tempToken: string;
  code: string;
  rememberMe?: boolean;
}

export interface RefreshResponse {
  accessToken: string;
  level: number;
}

export function login(payload: LoginRequest): Promise<LoginResponse> {
  return apiRequest<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: payload,
    allowRefreshRetry: false, // login nunca debe disparar un refresh
  });
}

export function verify2fa(payload: Verify2faRequest): Promise<LoginResponse> {
  return apiRequest<LoginResponse>('/api/auth/2fa/verify', {
    method: 'POST',
    body: payload,
    allowRefreshRetry: false,
  });
}

export function refresh(): Promise<RefreshResponse> {
  return apiRequest<RefreshResponse>('/api/auth/refresh', {
    method: 'POST',
    allowRefreshRetry: false, // este ES el refresh; evita loop infinito
  });
}

export function logout(): Promise<void> {
  return apiRequest<void>('/api/auth/logout', {
    method: 'POST',
    allowRefreshRetry: false,
  });
}
