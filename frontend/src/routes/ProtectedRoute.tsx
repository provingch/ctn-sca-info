import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user, isBootstrapping } = useAuth();

  if (isBootstrapping) {
    // Todavía intentando hidratar la sesión normal o persistente vía /auth/refresh.
    // Evita un parpadeo hacia /login si el navegador todavía conserva la cookie.
    return <div className="page-loading">Cargando sesión…</div>;
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}
