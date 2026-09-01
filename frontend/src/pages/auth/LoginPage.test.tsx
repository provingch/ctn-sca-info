import { act, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../../api/client';
import LoginPage from './LoginPage';
import { authFeedback, formatRetryTime } from './loginLockout';

const authMocks = vi.hoisted(() => ({
  login: vi.fn(),
  verify2fa: vi.fn(),
}));

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({ login: authMocks.login, verify2fa: authMocks.verify2fa }),
}));

describe('bloqueo temporal del login', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    authMocks.login.mockReset();
    authMocks.verify2fa.mockReset();
  });

  it('cuenta hasta cero, retira el aviso y vuelve a habilitar el ingreso', async () => {
    authMocks.login.mockRejectedValue(new ApiError(429, 'Bloqueado', {
      code: 'AUTH_LOCKED',
      message: 'Demasiados intentos.',
      retryAfterSeconds: 2,
    }));
    render(<MemoryRouter><LoginPage /></MemoryRouter>);

    fireEvent.change(screen.getByLabelText('Usuario o Cédula'), { target: { value: 'usuario' } });
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'clave' } });
    await act(async () => { fireEvent.click(screen.getByRole('button', { name: 'Ingresar' })); });

    expect(screen.getByText(/2 s/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ingresar' })).toBeDisabled();

    await act(async () => { vi.advanceTimersByTime(1000); });
    expect(screen.getByText(/1 s/)).toBeInTheDocument();

    await act(async () => { vi.advanceTimersByTime(1000); });
    expect(screen.queryByText('Acceso temporalmente pausado')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ingresar' })).toBeEnabled();
  });

  it('normaliza los segundos informados por el backend', () => {
    const feedback = authFeedback(new ApiError(429, 'Bloqueado', {
      code: 'AUTH_LOCKED', message: 'Esperá.', retryAfterSeconds: 61.2,
    }), 'Error');
    expect(feedback.retryAfterSeconds).toBe(62);
    expect(formatRetryTime(feedback.retryAfterSeconds)).toBe('1 min 02 s');
  });
});
