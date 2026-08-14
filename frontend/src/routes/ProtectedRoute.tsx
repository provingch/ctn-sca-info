import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function ProtectedRoute({ children, allowedLevels }: { children: ReactNode; allowedLevels?: number[] }) {
  const { user, isBootstrapping } = useAuth();

  if (isBootstrapping) {
    return <div className="page-loading">Cargando sesión…</div>;
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (allowedLevels && !allowedLevels.includes(user.level)) {
    const fallback: Record<number, string> = {
      1: '/home',
      2: '/evaluacion',
      3: '/admin',
      4: '/padre',
    };
    return <Navigate to={fallback[user.level] ?? '/profile'} replace />;
  }

  return <>{children}</>;
}
