import type { ReactNode } from 'react';
import SpecialtyIcon from './SpecialtyIcon';

export default function PageBanner({ title, context, specialty, selector, onBack, backLabel = 'Inicio', welcome = false }: {
  title: ReactNode;
  context?: ReactNode;
  specialty?: string | null;
  selector?: ReactNode;
  onBack?: () => void;
  /** A dónde vuelve `onBack` (se muestra como "← {backLabel}"). */
  backLabel?: string;
  /** Saludo de los paneles de inicio: se destaca más que el encabezado de una sección. */
  welcome?: boolean;
}) {
  return (
    <div className={`page-banner${welcome ? ' page-banner--welcome' : ''}`}>
      <div className="page-banner-text">
        {onBack && <button type="button" className="page-banner-back" aria-label={backLabel === 'Inicio' ? 'Volver al inicio' : `Volver a ${backLabel}`} onClick={onBack}><span aria-hidden="true">←</span> {backLabel}</button>}
        <div className="page-banner-title-row">
          <h1>{title}</h1>
        </div>
        {context && <span>{context}</span>}
      </div>
      {selector}
      <div className="page-banner-identity" title={specialty ? `Especialidad: ${specialty}` : 'Sistema de Carpeta Académica'}>
        <div className="page-banner-emblem"><SpecialtyIcon name={specialty ?? ''} /></div>
        <span>{specialty || 'SCA · CTN'}</span>
      </div>
    </div>
  );
}
