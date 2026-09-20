import type { ReactNode } from 'react';
import SpecialtyIcon from './SpecialtyIcon';

export default function PageBanner({ title, context, specialty, selector, onBack, backLabel = 'Inicio' }: {
  title: ReactNode;
  context?: ReactNode;
  specialty?: string | null;
  selector?: ReactNode;
  onBack?: () => void;
  /** A dónde vuelve `onBack` (se muestra como "← {backLabel}"). */
  backLabel?: string;
}) {
  return (
    <div className="page-banner">
      <div className="page-banner-text">
        <div className="page-banner-title-row">
          {onBack && <button type="button" className="page-banner-back" aria-label={backLabel === 'Inicio' ? 'Volver al inicio' : `Volver a ${backLabel}`} onClick={onBack}>← {backLabel}</button>}
          <h1>{title}</h1>
        </div>
        {context && <span>{context}</span>}
      </div>
      {selector}
      <div className="page-banner-emblem">
        <SpecialtyIcon name={specialty ?? ''} />
      </div>
    </div>
  );
}
