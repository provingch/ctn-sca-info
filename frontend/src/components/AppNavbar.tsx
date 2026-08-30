import { useEffect, useRef, useState } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import { getNotificaciones, getNotificacionesContador, marcarNotificacionLeida, type NotificacionItem } from '../api/notificaciones';
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
  const [notifOpen, setNotifOpen] = useState(false);
  const [notifCount, setNotifCount] = useState(0);
  const [notifs, setNotifs] = useState<NotificacionItem[]>([]);
  const config = getRoleNavigation(user?.level);
  const displayName = user?.displayName || user?.username || 'Usuario SCA';
  const initials = user?.initials || displayName.split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'S';

  useEffect(() => {
    setUserMenuOpen(false);
    setMobileMenuOpen(false);
    setNotifOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    function closeOnOutsideClick(event: PointerEvent) {
      if (headerRef.current && !headerRef.current.contains(event.target as Node)) {
        setUserMenuOpen(false);
        setMobileMenuOpen(false);
        setNotifOpen(false);
      }
    }
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setUserMenuOpen(false);
        setMobileMenuOpen(false);
        setNotifOpen(false);
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

  useEffect(() => {
    let mounted = true;
    async function loadCount() {
      try {
        const res = await getNotificacionesContador();
        const count = (res && typeof res === 'object' && 'count' in res) ? (res as any).count : 0;
        if (mounted) setNotifCount(Number(count || 0));
      } catch {
        // ignore
      }
    }
    loadCount();
    const iv = setInterval(loadCount, 45000);
    return () => { mounted = false; clearInterval(iv); };
  }, []);

  async function openNotifications() {
    if (notifOpen) {
      setNotifOpen(false);
      return;
    }
    try {
      const list = await getNotificaciones(false);
      setNotifs(list);
      setNotifOpen(true);
    } catch {
      setNotifs([]);
      setNotifOpen(true);
    }
  }

  async function handleClickNotif(n: NotificacionItem) {
    try {
      await marcarNotificacionLeida(n.id);
      setNotifCount((c) => Math.max(0, c - 1));
    } catch {
      // ignore
    }
    // Deep-link basic mapping
    if (n.tipo === 'QUEJA_ACUMULADA' || n.entidadTipo === 'queja') {
      navigate('/coordinacion');
    } else if (n.tipo === 'INCUMPLIMIENTO' || n.entidadTipo === 'incumplimiento') {
      navigate('/evaluacion');
    }
    setNotifOpen(false);
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
        <div className="navbar-notifications">
          <button className="navbar-notif-trigger" type="button" aria-haspopup="menu" aria-expanded={notifOpen} onClick={() => void openNotifications()}>
            <svg viewBox="0 0 24 24" aria-hidden="true" width="18" height="18"><path d="M12 2a4 4 0 0 0-4 4v1.1A6.002 6.002 0 0 0 6 14v3l-1 1v1h14v-1l-1-1v-3a6.002 6.002 0 0 0-2-6.9V6a4 4 0 0 0-4-4zM8 20a2 2 0 0 0 4 0H8z" /></svg>
            {notifCount > 0 && <span className="notif-badge" aria-hidden="true">{notifCount}</span>}
          </button>
          <div className={`navbar-notif-dropdown${notifOpen ? ' open' : ''}`} role="menu" aria-hidden={!notifOpen}>
            {notifs.length === 0 ? <div className="panel">No hay notificaciones</div> : (
              <ul className="notif-list">
                {notifs.map((n) => (
                  <li key={n.id} className={`notif-item${n.leida ? '' : ' unread'}`}>
                    <button type="button" className="notif-link" onClick={() => void handleClickNotif(n)}>
                      <div className="notif-message"><strong>{n.titulo}</strong><div>{n.cuerpo}</div></div>
                      <div className="notif-meta"><small>{new Date(n.createdAt).toLocaleString('es-PY')}</small></div>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
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
