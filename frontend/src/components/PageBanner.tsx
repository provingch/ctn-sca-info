import type { ReactNode } from 'react';
import SpecialtyIcon from './SpecialtyIcon';

export default function PageBanner({ title, context, specialty, selector, onBack }: {
  title: ReactNode;
  context?: ReactNode;
  specialty?: string | null;
  selector?: ReactNode;
  onBack?: () => void;
}) {
  return (
    <div className="page-banner">
      <div className="page-banner-text">
        <div className="page-banner-title-row">
          {onBack && <button type="button" className="page-banner-back" onClick={onBack}>← Inicio</button>}
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
