import { ApiError } from '../../api/client';

export type AuthFeedback = { message: string; locked: boolean; retryAfterSeconds: number };

export function authFeedback(error: unknown, fallback: string): AuthFeedback {
  if (!(error instanceof ApiError)) return { message: fallback, locked: false, retryAfterSeconds: 0 };
  const body = error.body;
  if (body && typeof body === 'object') {
    const authBody = body as { code?: unknown; message?: unknown; retryAfterSeconds?: unknown };
    const locked = authBody.code === 'AUTH_LOCKED' || error.status === 429;
    const parsedRetryAfter = Number(authBody.retryAfterSeconds);
    return {
      message: typeof authBody.message === 'string' ? authBody.message : error.message,
      locked,
      retryAfterSeconds: locked && Number.isFinite(parsedRetryAfter) && parsedRetryAfter > 0 ? Math.ceil(parsedRetryAfter) : 0,
    };
  }
  return { message: error.message, locked: error.status === 429, retryAfterSeconds: 0 };
}

export function formatRetryTime(totalSeconds: number): string {
  const safeSeconds = Math.max(0, Math.ceil(totalSeconds));
  const minutes = Math.floor(safeSeconds / 60);
  const seconds = safeSeconds % 60;
  return minutes > 0 ? `${minutes} min ${String(seconds).padStart(2, '0')} s` : `${seconds} s`;
}
