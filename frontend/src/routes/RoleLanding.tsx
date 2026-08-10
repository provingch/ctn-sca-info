import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function RoleLanding() {
  const { user, isBootstrapping } = useAuth();
  if (isBootstrapping) return <div className="page-loading">Cargando sesión…</div>;
  if (!user) return <Navigate to="/login" replace />;
  const target: Record<number, string> = { 1: '/home', 2: '/evaluacion', 3: '/admin', 4: '/padre' };
  return <Navigate to={target[user.level] ?? '/profile'} replace />;
}
