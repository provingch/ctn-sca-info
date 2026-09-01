import CtnLogo from '../components/CtnLogo';
import { iconUrlForSpecialtyName } from './specialtyIconData';

export function SpecialtyAvatar({ name, size = 36 }: { name?: string | null; size?: number }) {
  const url = iconUrlForSpecialtyName(name);
  if (!url) return <span style={{ width: size, height: size, display: 'inline-grid', placeItems: 'center', borderRadius: '50%', background: 'var(--paper)', color: 'var(--accent)' }}><CtnLogo /></span>;
  return <span style={{ width: size, height: size, display: 'inline-grid', placeItems: 'center', borderRadius: '50%', overflow: 'hidden', background: 'var(--paper)' }} aria-hidden="true"><img src={url} alt="" style={{ width: '70%', height: '70%', objectFit: 'contain' }} /></span>;
}

export function SpecialtyDecorative({ name, className = '' }: { name?: string | null; className?: string }) {
  const url = iconUrlForSpecialtyName(name);
  if (!url) return null;
  return <img className={`specialty-decorative ${className}`} src={url} alt="" aria-hidden="true" />;
}
