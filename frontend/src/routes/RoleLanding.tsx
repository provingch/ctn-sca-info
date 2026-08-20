import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function RoleLanding() {
  const { user, isBootstrapping } = useAuth();
  if (user) {
    const target: Record<number, string> = { 1: '/home', 2: '/evaluacion', 3: '/admin', 4: '/padre' };
    return <Navigate to={target[user.level] ?? '/profile'} replace />;
  }
  if (isBootstrapping) return <div className="page-loading">Cargando sesión…</div>;
  return <Navigate to="/login" replace />;
}
