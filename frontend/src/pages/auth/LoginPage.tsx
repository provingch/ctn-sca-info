import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { ApiError } from '../../api/client';

type Step = 'credentials' | 'twofactor';

export default function LoginPage() {
  const { login, verify2fa } = useAuth();
  const navigate = useNavigate();

  const [step, setStep] = useState<Step>('credentials');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [tempToken, setTempToken] = useState<string | null>(null);
  const [code, setCode] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleCredentialsSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const res = await login(username, password, rememberMe);
      if (res.requiere2fa && res.tempToken) {
        setTempToken(res.tempToken);
        setStep('twofactor');
      } else {
        navigate('/', { replace: true });
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo iniciar sesión.');
    } finally {
      setSubmitting(false);
    }
  }

  async function handle2faSubmit(e: FormEvent) {
    e.preventDefault();
    if (!tempToken) return;
    setError(null);
    setSubmitting(true);
    try {
      await verify2fa(tempToken, code, rememberMe);
      navigate('/', { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Código inválido.');
    } finally {
      setSubmitting(false);
    }
  }

  if (step === 'twofactor') {
    return (
      <div className="auth-page">
        <form onSubmit={handle2faSubmit} className="auth-form">
          <h1>Verificación en dos pasos</h1>
          <p>Ingresá el código de tu app de autenticación.</p>
          <label>
            Código
            <input
              value={code}
              onChange={(e) => setCode(e.target.value)}
              inputMode="numeric"
              autoFocus
              required
            />
          </label>
          {error && <p className="form-error">{error}</p>}
          <button type="submit" disabled={submitting}>
            {submitting ? 'Verificando…' : 'Verificar'}
          </button>
        </form>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <form onSubmit={handleCredentialsSubmit} className="auth-form">
        <h1>Iniciar sesión</h1>
        <label>
          Usuario
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus required />
        </label>
        <label>
          Contraseña
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </label>
        <label className="checkbox-label">
          <input
            type="checkbox"
            checked={rememberMe}
            onChange={(e) => setRememberMe(e.target.checked)}
          />
          Recordarme en este dispositivo
        </label>
        {error && <p className="form-error">{error}</p>}
        <button type="submit" disabled={submitting}>
          {submitting ? 'Ingresando…' : 'Ingresar'}
        </button>
      </form>
    </div>
  );
}
