import { useEffect, type ReactNode } from 'react';
import { NavLink } from 'react-router-dom';
import { normalizeSpecialty } from '../theme/theme';
import { useSpecialty } from '../context/SpecialtyContext';
import AppNavbar from './AppNavbar';
import PageBanner from './PageBanner';

export default function AppShell({ children, title, subtitle, specialty, hero = true, onBack, backLabel }: {
  children: ReactNode; title?: string; subtitle?: string; specialty?: string | null; hero?: boolean;
  /** Vuelta integrada al banner (reemplaza un botón "Volver" suelto arriba del contenido). */
  onBack?: () => void; backLabel?: string;
}) {
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
        {hero && title && <PageBanner title={title} context={subtitle} specialty={effectiveSpecialty} onBack={onBack} backLabel={backLabel} />}
        {children}
      </main>
      <footer className="app-footer">
        <div className="app-footer-inner">
          <div className="app-footer-brand" aria-hidden="true">
            <span><strong>Colegio Técnico Nacional</strong></span>
          </div>
          <span><NavLink to="/privacidad">Privacidad</NavLink> · <NavLink to="/terminos">Términos</NavLink></span>
        </div>
      </footer>
    </div>
  );
}
