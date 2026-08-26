import { useEffect, useRef, useState } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getRoleNavigation, type NavigationItem } from '../config/navigation';
import CtnLogo from './CtnLogo';
import ThemeToggle from './ThemeToggle';

function PrimaryLinks({ items, mobile = false }: { items: NavigationItem[]; mobile?: boolean }) {
  const location = useLocation();
  return <nav className={mobile ? 'navbar-mobile-links' : 'navbar-primary'} aria-label="Navegación principal">
    {items.map((item) => <NavLink key={item.to} to={item.to} end={item.end} className={({ isActive }) => isActive || item.activePrefixes?.some((prefix) => location.pathname.startsWith(prefix)) ? 'active' : undefined}>{item.label}</NavLink>)}
  </nav>;
}

export default function AppNavbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const headerRef = useRef<HTMLElement>(null);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const config = getRoleNavigation(user?.level);
  const displayName = user?.displayName || user?.username || 'Usuario SCA';
  const initials = user?.initials || displayName.split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'S';

  useEffect(() => {
    setUserMenuOpen(false);
    setMobileMenuOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    function closeOnOutsideClick(event: PointerEvent) {
      if (headerRef.current && !headerRef.current.contains(event.target as Node)) {
        setUserMenuOpen(false);
        setMobileMenuOpen(false);
      }
    }
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setUserMenuOpen(false);
        setMobileMenuOpen(false);
      }
    }
    document.addEventListener('pointerdown', closeOnOutsideClick);
    document.addEventListener('keydown', closeOnEscape);
    return () => {
      document.removeEventListener('pointerdown', closeOnOutsideClick);
      document.removeEventListener('keydown', closeOnEscape);
    };
  }, []);

  async function signOut() {
    setUserMenuOpen(false);
    setMobileMenuOpen(false);
    await logout();
    navigate('/login', { replace: true });
  }

  return <header className="app-header" ref={headerRef}>
    <div className="app-header-inner">
      <NavLink className="brand" to="/" aria-label="Ir al inicio">
        <span className="brand-mark"><CtnLogo /></span>
        <span className="brand-copy">
          <span className="brand-title">Sistema de Carpeta Académica</span>
          <small className="brand-sub">Colegio Técnico Nacional de Asunción</small>
        </span>
      </NavLink>

      <PrimaryLinks items={config.primaryItems} />

      <div className="navbar-desktop-actions">
        <ThemeToggle compact />
        <div className="navbar-user-menu">
          <button className="navbar-user-trigger" type="button" aria-haspopup="menu" aria-expanded={userMenuOpen} onClick={() => setUserMenuOpen((open) => !open)}>
            {user?.fotoPerfil ? <img className="navbar-avatar" src={user.fotoPerfil} alt="" /> : <span className="navbar-avatar" aria-hidden="true">{initials}</span>}
            <span className="navbar-user-copy"><strong>{displayName}</strong><small>{config.roleLabel}</small></span>
            <svg viewBox="0 0 20 20" aria-hidden="true"><path d="m6 8 4 4 4-4" /></svg>
          </button>
          <div className={`navbar-user-dropdown${userMenuOpen ? ' open' : ''}`} role="menu" aria-hidden={!userMenuOpen}>
            <NavLink to="/profile" role="menuitem">Mi perfil</NavLink>
            <a href={config.manualPath} target="_blank" rel="noopener noreferrer" role="menuitem" aria-label="Abrir manual en una pestaña nueva">Manual</a>
            <span className="navbar-menu-divider" />
            <button className="navbar-logout" type="button" role="menuitem" onClick={() => void signOut()}>Cerrar sesión</button>
          </div>
        </div>
      </div>

      <div className="navbar-mobile-actions">
        <NavLink className="navbar-mobile-avatar" to="/profile" aria-label={`Abrir perfil de ${displayName}`}>
          {user?.fotoPerfil ? <img className="navbar-avatar" src={user.fotoPerfil} alt="" /> : <span className="navbar-avatar" aria-hidden="true">{initials}</span>}
        </NavLink>
        <button className={`navbar-hamburger${mobileMenuOpen ? ' open' : ''}`} type="button" aria-label={mobileMenuOpen ? 'Cerrar navegación' : 'Abrir navegación'} aria-expanded={mobileMenuOpen} aria-controls="mobile-navigation-panel" onClick={() => setMobileMenuOpen((open) => !open)}>
          <span /><span /><span />
        </button>
      </div>

      <div id="mobile-navigation-panel" className={`navbar-mobile-panel${mobileMenuOpen ? ' open' : ''}`} aria-hidden={!mobileMenuOpen}>
        <PrimaryLinks items={config.primaryItems} mobile />
        <div className="navbar-mobile-account">
          <span>Cuenta</span>
          <a href={config.manualPath} target="_blank" rel="noopener noreferrer" aria-label="Abrir manual en una pestaña nueva">Manual</a>
          <ThemeToggle />
          <button className="navbar-logout" type="button" onClick={() => void signOut()}>Cerrar sesión</button>
        </div>
      </div>
    </div>
  </header>;
}
