import { useEffect, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useSpecialty } from '../context/SpecialtyContext';
import { normalizeSpecialty } from '../theme/theme';
import AppShell from './AppShell';
import AppFooter from './AppFooter';
import ScaLogo from './ScaLogo';
import ThemeToggle from './ThemeToggle';
import './information-page.css';

/** Shared public/account presentation; page content and its order belong to the caller. */
export default function InformationLayout({ title, lead, introduction, children, reading = false, showBackLink = false }: {
  title: string; lead: string; introduction?: ReactNode; children: ReactNode; reading?: boolean; showBackLink?: boolean;
}) {
  const { user } = useAuth();
  const { name } = useSpecialty();
  useEffect(() => { document.title = `${title} | CTN`; }, [title]);
  const content = <article className={`about-card${reading ? ' information-reading' : ''}`}>
    <div className="about-intro">
      <p className="about-eyebrow">Colegio Técnico Nacional de Asunción</p>
      <h1>{title}</h1>
      <p className="about-lead">{lead}</p>
      {introduction}
    </div>
    {children}
    <footer className="about-footer"><p>© {new Date().getFullYear()} Colegio Técnico Nacional de Asunción.</p></footer>
  </article>;
  if (user) return <AppShell title={title} hero={false}><div className="about-page">{content}</div></AppShell>;
  return <div className="about-public account-ui" data-specialty={normalizeSpecialty(name || 'Informática')}>
    <header className="about-public-header">
      <Link to="/" className="about-brand"><ScaLogo /><span>Sistema de Carpeta Académica<small>Colegio Técnico Nacional de Asunción</small></span></Link>
      <ThemeToggle compact />
    </header>
    {showBackLink && <Link to="/" className="about-public-back">← Volver al inicio</Link>}
    <main className="about-page">{content}</main>
    <AppFooter />
  </div>;
}
