import construcciones from '../assets/logos-especialidad/construcciones-civiles.svg';
import electricidad from '../assets/logos-especialidad/electricidad.svg';
import electronica from '../assets/logos-especialidad/electronica.svg';
import electromecanica from '../assets/logos-especialidad/electromecanica.svg';
import informatica from '../assets/logos-especialidad/informatica.svg';
import mecanicaIndustrial from '../assets/logos-especialidad/mecanica-industrial.svg';
import mecanicaAutomotriz from '../assets/logos-especialidad/mecanica-automotriz.svg';
import quimicaIndustrial from '../assets/logos-especialidad/quimica-industrial.svg';
import CtnLogo from '../components/CtnLogo';
import { normalizeSpecialty } from '../theme/theme';

const icons: Array<[string, string]> = [
  ['construcciones civiles', construcciones],
  ['electricidad', electricidad],
  ['electronica', electronica],
  ['electromecanica', electromecanica],
  ['informatica', informatica],
  ['mecanica industrial', mecanicaIndustrial],
  ['mecanica general', mecanicaIndustrial],
  ['mecanica automotriz', mecanicaAutomotriz],
  ['quimica industrial', quimicaIndustrial],
  ['quimica', quimicaIndustrial],
];

export function iconUrlForSpecialtyName(name?: string | null) {
  if (!name) return null;
  const normalized = normalizeSpecialty(name).replaceAll('-', ' ');
  return icons.find(([candidate]) => normalized.includes(candidate) || candidate.includes(normalized))?.[1] ?? null;
}

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

export default iconUrlForSpecialtyName;
