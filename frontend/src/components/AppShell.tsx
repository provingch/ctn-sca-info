import { useEffect, type ReactNode } from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import CtnLogo from './CtnLogo';
import { normalizeSpecialty } from '../theme/theme';
import { useSpecialty } from '../context/SpecialtyContext';
import AppNavbar from './AppNavbar';
import { getRoleNavigation } from '../config/navigation';

export default function AppShell({ children, title, subtitle, specialty }: { children: ReactNode; title?: string; subtitle?: string; specialty?: string | null }) {
  const { user } = useAuth();
  const { name: selectedSpecialty, selectSpecialty } = useSpecialty();
  const effectiveSpecialty = selectedSpecialty || specialty || null;

  useEffect(() => {
    if (specialty && specialty !== selectedSpecialty) {
      selectSpecialty(specialty);
    }
  }, [specialty, selectedSpecialty, selectSpecialty]);

  useEffect(() => {
    document.title = title ? `${title} | SCA CTN` : 'SCA CTN';
  }, [title]);

  return (
    <div className="app-frame" data-specialty={normalizeSpecialty(effectiveSpecialty)}>
      <a className="skip-link" href="#main-content">Saltar al contenido principal</a>
      <AppNavbar />
      <main id="main-content" className="app-main" tabIndex={-1}>
        {title && <section className="page-hero"><div className="hero-content"><span className="hero-kicker"><i />{user ? getRoleNavigation(user.level).roleLabel : 'SCA'}</span><h1>{title}</h1><p>{subtitle || 'Sistema de Carpetas Académicas del Colegio Técnico Nacional'}</p></div><div className="hero-emblem"><CtnLogo variant="full" /></div></section>}
        {children}
      </main>
      <footer className="app-footer">
        <div className="app-footer-inner">
          <NavLink className="app-footer-brand" to="/" aria-label="Colegio Técnico Nacional — ir al inicio">
            <CtnLogo variant="full" className="app-footer-logo" />
            <span><strong>Colegio Técnico Nacional</strong><small>Sistema de Carpetas Académicas</small></span>
          </NavLink>
          <span><NavLink to="/privacidad">Privacidad</NavLink> · <NavLink to="/terminos">Términos</NavLink></span>
        </div>
      </footer>
    </div>
  );
}
