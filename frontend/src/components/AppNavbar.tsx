import { useCallback, useEffect, useRef, useState } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import { getNotificaciones, getNotificacionesContador, marcarNotificacionLeida, marcarTodasNotificacionesLeidas, type NotificacionItem } from '../api/notificaciones';
import { useAuth } from '../context/AuthContext';
import { getRoleNavigation, type NavigationItem } from '../config/navigation';
import CtnLogo from './CtnLogo';
import ThemeToggle from './ThemeToggle';
import { formatNotificationDate, notificationDestination } from './notificationUtils';

type NotificationStatus = 'idle' | 'loading' | 'ready' | 'error';

function NotificationTrigger({ count, open, mobile = false, onClick }: { count: number; open: boolean; mobile?: boolean; onClick: (trigger: HTMLButtonElement) => void }) {
  const countLabel = count === 0 ? 'sin notificaciones nuevas' : `${count} ${count === 1 ? 'notificación nueva' : 'notificaciones nuevas'}`;
  return <button
    className={`navbar-notif-trigger${mobile ? ' mobile' : ''}`}
    type="button"
    aria-label={`Notificaciones, ${countLabel}`}
    aria-haspopup="dialog"
    aria-controls="notification-inbox"
    aria-expanded={open}
    onClick={(event) => onClick(event.currentTarget)}
  >
    <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 2a4 4 0 0 0-4 4v1.1A6.002 6.002 0 0 0 6 14v3l-1 1v1h14v-1l-1-1v-3a6.002 6.002 0 0 0-2-6.9V6a4 4 0 0 0-4-4zM8 20a2 2 0 0 0 4 0H8z" /></svg>
    {count > 0 && <span className="notif-badge" aria-hidden="true">{count > 99 ? '99+' : count}</span>}
  </button>;
}

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
  const [notifStatus, setNotifStatus] = useState<NotificationStatus>('idle');
  const [notifError, setNotifError] = useState('');
  const [markingAll, setMarkingAll] = useState(false);
  const notificationTriggerRef = useRef<HTMLButtonElement | null>(null);
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
        setNotifOpen((wasOpen) => {
          if (wasOpen) window.requestAnimationFrame(() => notificationTriggerRef.current?.focus());
          return false;
        });
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

  const refreshNotificationCount = useCallback(async () => {
    if (!user) return;
    try {
      const result = await getNotificacionesContador();
      setNotifCount(Math.max(0, Number(result.count) || 0));
    } catch {
      // El error de la bandeja se muestra al abrirla; el contador no bloquea la navegación.
    }
  }, [user]);

  useEffect(() => {
    if (!user) return;
    void refreshNotificationCount();
    const interval = window.setInterval(() => void refreshNotificationCount(), 45000);
    const refreshOnFocus = () => void refreshNotificationCount();
    const refreshWhenVisible = () => { if (document.visibilityState === 'visible') void refreshNotificationCount(); };
    window.addEventListener('focus', refreshOnFocus);
    document.addEventListener('visibilitychange', refreshWhenVisible);
    return () => {
      window.clearInterval(interval);
      window.removeEventListener('focus', refreshOnFocus);
      document.removeEventListener('visibilitychange', refreshWhenVisible);
    };
  }, [refreshNotificationCount, user]);

  async function loadNotifications() {
    setNotifStatus('loading');
    setNotifError('');
    try {
      const list = await getNotificaciones(false);
      setNotifs(list);
      setNotifCount(list.filter((notification) => !notification.leida).length);
      setNotifStatus('ready');
    } catch {
      setNotifStatus('error');
      setNotifError('No se pudieron cargar las notificaciones. Intentá nuevamente.');
    }
  }

  function openNotifications(trigger: HTMLButtonElement) {
    notificationTriggerRef.current = trigger;
    if (notifOpen) {
      setNotifOpen(false);
      return;
    }
    setUserMenuOpen(false);
    setMobileMenuOpen(false);
    setNotifOpen(true);
    void loadNotifications();
  }

  async function handleClickNotif(n: NotificacionItem) {
    setNotifError('');
    if (!n.leida) {
      try {
        await marcarNotificacionLeida(n.id);
        setNotifs((current) => current.map((item) => item.id === n.id ? { ...item, leida: true } : item));
        setNotifCount((count) => Math.max(0, count - 1));
      } catch {
        setNotifError('No se pudo marcar la notificación como leída.');
        return;
      }
    }
    const destination = notificationDestination(n, user?.level);
    if (destination) {
      setNotifOpen(false);
      navigate(destination);
    }
  }

  async function markAllNotificationsRead() {
    if (notifCount === 0 || markingAll) return;
    setMarkingAll(true);
    setNotifError('');
    try {
      await marcarTodasNotificacionesLeidas();
      setNotifs((current) => current.map((item) => ({ ...item, leida: true })));
      setNotifCount(0);
    } catch {
      setNotifError('No se pudieron marcar todas las notificaciones como leídas.');
    } finally {
      setMarkingAll(false);
    }
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
          <NotificationTrigger count={notifCount} open={notifOpen} onClick={openNotifications} />
        </div>
        <div className="navbar-user-menu">
          <button className="navbar-user-trigger" type="button" aria-haspopup="menu" aria-expanded={userMenuOpen} onClick={() => { setNotifOpen(false); setUserMenuOpen((open) => !open); }}>
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
        <NotificationTrigger count={notifCount} open={notifOpen} mobile onClick={openNotifications} />
        <NavLink className="navbar-mobile-avatar" to="/profile" aria-label={`Abrir perfil de ${displayName}`}>
          {user?.fotoPerfil ? <img className="navbar-avatar" src={user.fotoPerfil} alt="" /> : <span className="navbar-avatar" aria-hidden="true">{initials}</span>}
        </NavLink>
        <button className={`navbar-hamburger${mobileMenuOpen ? ' open' : ''}`} type="button" aria-label={mobileMenuOpen ? 'Cerrar navegación' : 'Abrir navegación'} aria-expanded={mobileMenuOpen} aria-controls="mobile-navigation-panel" onClick={() => { setNotifOpen(false); setMobileMenuOpen((open) => !open); }}>
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

      <section id="notification-inbox" className={`navbar-notif-dropdown${notifOpen ? ' open' : ''}`} role="dialog" aria-labelledby="notification-inbox-title" aria-hidden={!notifOpen}>
        <header className="notif-header">
          <div>
            <span>Bandeja de entrada</span>
            <h2 id="notification-inbox-title">Notificaciones</h2>
          </div>
          <button type="button" className="notif-close" aria-label="Cerrar notificaciones" onClick={() => { setNotifOpen(false); notificationTriggerRef.current?.focus(); }}>×</button>
        </header>
        <div className="notif-toolbar">
          <span aria-live="polite">{notifCount === 0 ? 'Todo al día' : `${notifCount} sin leer`}</span>
          <div>
            <button type="button" disabled={notifStatus === 'loading'} onClick={() => void loadNotifications()}>Actualizar</button>
            <button type="button" disabled={notifCount === 0 || markingAll} onClick={() => void markAllNotificationsRead()}>{markingAll ? 'Marcando…' : 'Marcar todas como leídas'}</button>
          </div>
        </div>
        {notifError && <div className="notif-error" role="alert">{notifError}</div>}
        <div className="notif-content">
          {notifStatus === 'loading' ? <div className="notif-state" role="status" aria-busy="true"><i aria-hidden="true" /><strong>Cargando notificaciones…</strong></div>
            : notifStatus === 'error' ? <div className="notif-state"><strong>No se pudo abrir la bandeja</strong><button type="button" onClick={() => void loadNotifications()}>Reintentar</button></div>
              : notifs.length === 0 ? <div className="notif-state"><span aria-hidden="true">✓</span><strong>No hay notificaciones</strong><small>Los avisos nuevos aparecerán acá.</small></div>
                : <ul className="notif-list" aria-label="Notificaciones recibidas">
                  {notifs.map((notification) => {
                    const destination = notificationDestination(notification, user?.level);
                    return <li key={notification.id} className={`notif-item${notification.leida ? '' : ' unread'}`}>
                      <button type="button" className="notif-link" onClick={() => void handleClickNotif(notification)} aria-label={`${notification.titulo}. ${notification.leida ? 'Leída' : 'No leída'}${destination ? '. Abrir destino' : ''}`}>
                        <span className="notif-unread-dot" aria-hidden="true" />
                        <span className="notif-message"><strong>{notification.titulo}</strong><span>{notification.cuerpo}</span></span>
                        <span className="notif-meta"><time dateTime={notification.createdAt}>{formatNotificationDate(notification.createdAt)}</time><small>{destination ? 'Abrir →' : notification.leida ? 'Leída' : 'Marcar como leída'}</small></span>
                      </button>
                    </li>;
                  })}
                </ul>}
        </div>
      </section>
    </div>
  </header>;
}
