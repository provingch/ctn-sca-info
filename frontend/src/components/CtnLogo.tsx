import compactLogoUrl from '../assets/ctn-logo.svg';
import fullLogoUrl from '../assets/ctn-logo-2.svg';
import gearLogoUrl from '../assets/ctn-gear.svg';

export default function CtnLogo({ variant = 'compact', className = '' }: { variant?: 'compact' | 'full' | 'gear'; className?: string }) {
  const src = variant === 'full' ? fullLogoUrl : variant === 'gear' ? gearLogoUrl : compactLogoUrl;
  const alt = variant === 'gear' ? 'Engranaje CTN' : 'Logo del Colegio Técnico Nacional';
  return (
    <img
      className={`ctn-logo ${className}`.trim()}
      src={src}
      alt={alt}
    />
  );
}
