import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import CambiarContraseniaTemporal from '../pages/auth/CambiarContraseniaTemporal';

export function ProtectedRoute({ children, allowedLevels }: { children: ReactNode; allowedLevels?: number[] }) {
  const { user, isBootstrapping, identityStatus } = useAuth();

  if (!user) {
    if (isBootstrapping) {
      return <div className="page-loading">Cargando sesión…</div>;
    }
    return <Navigate to="/login" replace />;
  }

  // Hasta saber si la contraseña es temporal no se monta la pantalla: con contraseña temporal el backend
  // rechaza casi todo con 403 y la pantalla mostraría errores.
  if (user.debeCambiarContrasenia === undefined && identityStatus !== 'error') {
    return <div className="page-loading">Cargando sesión…</div>;
  }
  if (user.debeCambiarContrasenia) return <CambiarContraseniaTemporal />;

  if (allowedLevels && !allowedLevels.includes(user.level)) {
    const fallback: Record<number, string> = {
      1: '/home',
      2: '/evaluacion',
      3: '/admin',
      4: '/padre',
      5: '/coordinacion',
    };
    return <Navigate to={fallback[user.level] ?? '/profile'} replace />;
  }

  return <>{children}</>;
}
