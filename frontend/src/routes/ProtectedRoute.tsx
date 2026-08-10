import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user, isBootstrapping } = useAuth();

  if (isBootstrapping) {
    // Todavía intentando hidratar sesión vía /auth/refresh (cookie SCA_REMEMBER).
    // Evita un parpadeo hacia /login si en realidad hay sesión "recordarme" activa.
    return <div className="page-loading">Cargando sesión…</div>;
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}
