import type { ReactNode } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const roleNames: Record<number, string> = { 1: 'Profesor', 2: 'Evaluador', 3: 'Administrador', 4: 'Padre / Encargado' };

export default function AppShell({ children, title }: { children: ReactNode; title?: string }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function signOut() {
    await logout();
    navigate('/login', { replace: true });
  }

  return (
    <div className="app-frame">
      <header className="app-header">
        <NavLink className="brand" to="/">SCA <span>CTN</span></NavLink>
        <nav className="app-nav" aria-label="Navegación principal">
          {user?.level === 1 && <NavLink to="/home">Cursos</NavLink>}
          {user?.level === 2 && <NavLink to="/evaluacion">Evaluación</NavLink>}
          {user?.level === 3 && <NavLink to="/admin">Administración</NavLink>}
          {user?.level === 4 && <NavLink to="/padre">Mis hijos</NavLink>}
          <NavLink to="/profile">Mi perfil</NavLink>
          <button className="nav-button" type="button" onClick={signOut}>Cerrar sesión</button>
        </nav>
      </header>
      <main className="app-main">
        {title && <div className="page-heading"><p>{user ? roleNames[user.level] ?? 'Usuario' : 'SCA'}</p><h1>{title}</h1></div>}
        {children}
      </main>
      <footer className="app-footer">
        <span>Colegio Técnico Nacional</span>
        <span><NavLink to="/privacidad">Privacidad</NavLink> · <NavLink to="/terminos">Términos</NavLink></span>
      </footer>
    </div>
  );
}
