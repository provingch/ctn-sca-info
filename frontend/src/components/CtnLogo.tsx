import compactLogoUrl from '../../../legacy/src/main/webapp/images/ctn-logo.svg';
import fullLogoUrl from '../../../legacy/src/main/webapp/images/ctn-logo-2.svg';

export default function CtnLogo({ variant = 'compact', className = '' }: { variant?: 'compact' | 'full'; className?: string }) {
  return (
    <img
      className={`ctn-logo ${className}`.trim()}
      src={variant === 'full' ? fullLogoUrl : compactLogoUrl}
      alt="Logo del Colegio Técnico Nacional"
    />
  );
}
