import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { ApiError } from '../../api/client';
import ThemeToggle from '../../components/ThemeToggle';
import CtnLogo from '../../components/CtnLogo';
import PasswordInput from '../../components/PasswordInput';
import { applyTheme, getInitialTheme } from '../../theme/theme';

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

  useEffect(() => {
    applyTheme(getInitialTheme());
  }, []);

  async function handleCredentialsSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const res = await login(username.trim(), password, rememberMe);
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
      <div className="auth-page" data-specialty="general">
        <div className="public-theme-toggle"><ThemeToggle /></div>
        <div className="auth-brand-panel"><span className="auth-brand-mark"><CtnLogo variant="full" /></span><p>Sistema de Carpetas Académicas</p><h2>La gestión académica, clara y conectada.</h2><small>Colegio Técnico Nacional · Asunción</small></div>
        <form onSubmit={handle2faSubmit} className="auth-form">
          <h1>Verificación en dos pasos</h1>
          <p>Ingresá el código de tu app de autenticación.</p>
          <label>
            Código
            <input
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 8))}
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
    <div className="auth-page" data-specialty="general">
      <div className="public-theme-toggle"><ThemeToggle /></div>
      <div className="auth-brand-panel"><span className="auth-brand-mark"><CtnLogo variant="full" /></span><p>Sistema de Carpetas Académicas</p><h2>La gestión académica, clara y conectada.</h2><small>Colegio Técnico Nacional · Asunción</small></div>
      <form onSubmit={handleCredentialsSubmit} className="auth-form">
        <h1>Iniciar sesión</h1>
        <label>
          Usuario o Cédula
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus required />
        </label>
        <label>
          Contraseña
          <PasswordInput
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
