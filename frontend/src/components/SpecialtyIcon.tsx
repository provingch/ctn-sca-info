import construcciones from '../assets/logos-especialidad/construcciones-civiles.svg';
import electricidad from '../assets/logos-especialidad/electricidad.svg';
import electronica from '../assets/logos-especialidad/electronica.svg';
import electromecanica from '../assets/logos-especialidad/electromecanica.svg';
import informatica from '../assets/logos-especialidad/informatica.svg';
import mecanicaIndustrial from '../assets/logos-especialidad/mecanica-industrial.svg';
import mecanicaAutomotriz from '../assets/logos-especialidad/mecanica-automotriz.svg';
import quimicaIndustrial from '../assets/logos-especialidad/quimica-industrial.svg';
import { normalizeSpecialty } from '../theme/theme';

const icons: Array<[string, string]> = [['construcciones civiles', construcciones], ['electricidad', electricidad], ['electronica', electronica], ['electromecanica', electromecanica], ['informatica', informatica], ['mecanica industrial', mecanicaIndustrial], ['mecanica general', mecanicaIndustrial], ['mecanica automotriz', mecanicaAutomotriz], ['quimica industrial', quimicaIndustrial], ['quimica', quimicaIndustrial]];

function iconFor(name: string) {
  const normalized = normalizeSpecialty(name).replaceAll('-', ' ');
  return icons.find(([candidate]) => normalized.includes(candidate) || candidate.includes(normalized))?.[1];
}

export default function SpecialtyIcon({ name, className = '' }: { name: string; className?: string }) {
  const iconUrl = iconFor(name);
  if (!iconUrl) return <span className={`specialty-icon specialty-icon-fallback ${className}`.trim()} role="img" aria-label={`Especialidad ${name}`}>{name.trim().charAt(0).toUpperCase() || '?'}</span>;
  return <span className={`specialty-icon ${className}`.trim()} role="img" aria-label={`Especialidad ${name}`}>
    <img src={iconUrl} alt="" loading="lazy" decoding="async" />
  </span>;
}
