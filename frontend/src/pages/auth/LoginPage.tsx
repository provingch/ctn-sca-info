import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import ThemeToggle from '../../components/ThemeToggle';
import CtnLogo from '../../components/CtnLogo';
import PasswordInput from '../../components/PasswordInput';
import { applyTheme, getInitialTheme } from '../../theme/theme';
import { authFeedback, formatRetryTime, type AuthFeedback } from './loginLockout';

type Step = 'credentials' | 'twofactor';

function AuthFeedbackNotice({ feedback, remainingSeconds, title }: { feedback: AuthFeedback; remainingSeconds: number; title: string }) {
  const countingDown = feedback.locked && remainingSeconds > 0;
  return <div className={`auth-feedback${feedback.locked ? ' is-locked' : ''}`} role="alert">
    <strong>{feedback.locked ? 'Acceso temporalmente pausado' : title}</strong>
    {countingDown ? <>
      <span aria-hidden="true">Demasiados intentos. Volvé a intentar en <time dateTime={`PT${remainingSeconds}S`}>{formatRetryTime(remainingSeconds)}</time>.</span>
      <span className="visually-hidden">Demasiados intentos. El acceso se habilitará automáticamente cuando finalice la pausa.</span>
    </> : <span>{feedback.message}</span>}
  </div>;
}

export default function LoginPage() {
  const { login, verify2fa } = useAuth();
  const navigate = useNavigate();

  const [step, setStep] = useState<Step>('credentials');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [tempToken, setTempToken] = useState<string | null>(null);
  const [code, setCode] = useState('');
  const [error, setError] = useState<AuthFeedback | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [lockExpiresAt, setLockExpiresAt] = useState<number | null>(null);
  const [remainingLockSeconds, setRemainingLockSeconds] = useState(0);

  useEffect(() => {
    applyTheme(getInitialTheme());
  }, []);

  useEffect(() => {
    if (lockExpiresAt === null) return;
    const updateCountdown = () => {
      const remaining = Math.max(0, Math.ceil((lockExpiresAt - Date.now()) / 1000));
      setRemainingLockSeconds(remaining);
      if (remaining === 0) {
        setLockExpiresAt(null);
        setError((current) => current?.locked ? null : current);
      }
    };
    updateCountdown();
    const interval = window.setInterval(updateCountdown, 250);
    return () => window.clearInterval(interval);
  }, [lockExpiresAt]);

  function clearFeedback() {
    setError(null);
    setLockExpiresAt(null);
    setRemainingLockSeconds(0);
  }

  function showAuthError(cause: unknown, fallback: string) {
    const feedback = authFeedback(cause, fallback);
    setError(feedback);
    if (feedback.locked && feedback.retryAfterSeconds > 0) {
      setRemainingLockSeconds(feedback.retryAfterSeconds);
      setLockExpiresAt(Date.now() + feedback.retryAfterSeconds * 1000);
    } else {
      setRemainingLockSeconds(0);
      setLockExpiresAt(null);
    }
  }

  async function handleCredentialsSubmit(e: FormEvent) {
    e.preventDefault();
    clearFeedback();
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
      showAuthError(err, 'No se pudo iniciar sesión.');
    } finally {
      setSubmitting(false);
    }
  }

  async function handle2faSubmit(e: FormEvent) {
    e.preventDefault();
    if (!tempToken) return;
    clearFeedback();
    setSubmitting(true);
    try {
      await verify2fa(tempToken, code, rememberMe);
      navigate('/', { replace: true });
    } catch (err) {
      showAuthError(err, 'Código inválido.');
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
          {error && <AuthFeedbackNotice feedback={error} remainingSeconds={remainingLockSeconds} title="No pudimos verificar el código" />}
          <button type="submit" disabled={submitting || remainingLockSeconds > 0}>
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
        {error && <AuthFeedbackNotice feedback={error} remainingSeconds={remainingLockSeconds} title="No se pudo iniciar sesión" />}
        <button type="submit" disabled={submitting || remainingLockSeconds > 0}>
          {submitting ? 'Ingresando…' : 'Ingresar'}
        </button>
      </form>
    </div>
  );
}
