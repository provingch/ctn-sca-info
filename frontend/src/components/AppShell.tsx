import { useEffect, type ReactNode } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ThemeToggle from './ThemeToggle';
import CtnLogo from './CtnLogo';
import { normalizeSpecialty } from '../theme/theme';
import { useSpecialty } from '../context/SpecialtyContext';

const roleNames: Record<number, string> = { 1: 'Profesor', 2: 'Evaluador', 3: 'Administrador', 4: 'Padre / Encargado' };
const manualPaths: Record<number, string> = {
  1: '/pdfs/manual-profesor.pdf',
  2: '/pdfs/manual-evaluador.pdf',
  3: '/pdfs/manual-administrador.pdf',
  4: '/pdfs/manual-padres.pdf',
};

export default function AppShell({ children, title, subtitle, specialty }: { children: ReactNode; title?: string; subtitle?: string; specialty?: string | null }) {
  const { user, logout } = useAuth();
  const { name: selectedSpecialty, selectSpecialty } = useSpecialty();
  const navigate = useNavigate();
  const effectiveSpecialty = selectedSpecialty || specialty || null;
  const manualPath = user ? manualPaths[user.level] : undefined;

  useEffect(() => {
    if (specialty && specialty !== selectedSpecialty) {
      selectSpecialty(specialty);
    }
  }, [specialty, selectedSpecialty, selectSpecialty]);

  async function signOut() {
    await logout();
    navigate('/login', { replace: true });
  }


  return (
    <div className="app-frame" data-specialty={normalizeSpecialty(effectiveSpecialty)}>
      <header className="app-header">
        <div className="app-header-inner">
          <NavLink className="brand" to="/" aria-label="Ir al inicio">
            <span className="brand-mark"><CtnLogo /></span>
            <span className="brand-copy">
              <span className="brand-title">Sistema de Carpeta Académica</span>
              <small className="brand-sub">Colegio Técnico Nacional de Asunción</small>
            </span>
          </NavLink>
          <nav className="app-nav" aria-label="Navegación principal">
            {user?.level === 1 && <NavLink to="/home">Cursos</NavLink>}
            {user?.level === 2 && <NavLink to="/evaluacion">Evaluación</NavLink>}
            {user?.level === 3 && <NavLink to="/admin">Administración</NavLink>}
            {user?.level === 4 && <NavLink to="/padre">Mis hijos</NavLink>}
            <NavLink to="/profile">Mi perfil</NavLink>
            <ThemeToggle compact />
            {manualPath && <a href={manualPath} target="_blank" rel="noopener noreferrer">Manual</a>}
            {/* Botón de wipe eliminado; endpoint admin sigue disponible para uso directo */}
            <button className="nav-button" type="button" onClick={signOut}>Cerrar sesión</button>
          </nav>
        </div>
      </header>
      <main className="app-main">
        {title && <section className="page-hero"><div className="hero-content"><span className="hero-kicker"><i />{user ? roleNames[user.level] ?? 'Usuario' : 'SCA'}</span><h1>{title}</h1><p>{subtitle || 'Sistema de Carpetas Académicas del Colegio Técnico Nacional'}</p></div><div className="hero-emblem"><CtnLogo /></div></section>}
        {children}
      </main>
      <footer className="app-footer">
        <div className="app-footer-inner">
          <span>Colegio Técnico Nacional</span>
          <span><NavLink to="/privacidad">Privacidad</NavLink> · <NavLink to="/terminos">Términos</NavLink></span>
        </div>
      </footer>
    </div>
  );
}
