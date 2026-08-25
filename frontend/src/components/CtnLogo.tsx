import compactLogoUrl from '../assets/ctn-logo.svg';
import fullLogoUrl from '../assets/ctn-logo-2.svg';

export default function CtnLogo({ variant = 'compact', className = '' }: { variant?: 'compact' | 'full'; className?: string }) {
  return (
    <img
      className={`ctn-logo ${className}`.trim()}
      src={variant === 'full' ? fullLogoUrl : compactLogoUrl}
      alt="Logo del Colegio Técnico Nacional"
    />
  );
}
