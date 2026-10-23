import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiError } from '../../api/client';
import { changePassword } from '../../api/profile';
import PasswordInput from '../../components/PasswordInput';
import ScaLogo from '../../components/ScaLogo';
import ThemeToggle from '../../components/ThemeToggle';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/toast';

/**
 * Un administrador restableció la contraseña: hasta elegir una nueva no se puede usar el sistema
 * (el backend rechaza todo lo demás con 403). Al cambiarla se cierran las sesiones, como en Mi perfil.
 */
export default function CambiarContraseniaTemporal() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await changePassword(form);
      showToast('Contraseña actualizada. Ingresá con la nueva.', { tone: 'success' });
      await logout();
      navigate('/login', { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo actualizar la contraseña.');
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-page" data-specialty="general">
      <div className="public-theme-toggle"><ThemeToggle /></div>
      <div className="auth-brand-panel"><span className="auth-brand-mark"><ScaLogo /></span><p>Sistema de Carpetas Académicas</p><h2>La gestión académica, clara y conectada.</h2><small>Colegio Técnico Nacional · Asunción</small></div>
      <form onSubmit={submit} className="auth-form">
        <h1>Elegí una contraseña nueva</h1>
        <p>Estás usando la contraseña temporal que te dio Administración. Para seguir, cambiala por una que solo sepas vos.</p>
        <label>Contraseña temporal<PasswordInput required autoComplete="current-password" value={form.currentPassword} onChange={(e) => setForm({ ...form, currentPassword: e.target.value })} autoFocus /></label>
        <label>Nueva contraseña<PasswordInput required minLength={6} autoComplete="new-password" value={form.newPassword} onChange={(e) => setForm({ ...form, newPassword: e.target.value })} /></label>
        <label>Confirmar nueva contraseña<PasswordInput required autoComplete="new-password" value={form.confirmPassword} onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })} /></label>
        {error && <div className="auth-feedback" role="alert"><strong>No se pudo cambiar la contraseña</strong><span>{error}</span></div>}
        <button type="submit" disabled={submitting}>{submitting ? 'Guardando…' : 'Guardar y volver a ingresar'}</button>
        <button type="button" className="button secondary" onClick={() => void logout().then(() => navigate('/login', { replace: true }))}>Cerrar sesión</button>
      </form>
    </div>
  );
}
