import type { ReactNode } from 'react';
import SpecialtyIcon from './SpecialtyIcon';

export default function PageBanner({ title, context, specialty, selector }: {
  title: ReactNode;
  context?: ReactNode;
  specialty?: string | null;
  selector?: ReactNode;
}) {
  return (
    <div className="page-banner">
      <div className="page-banner-text">
        <h1>{title}</h1>
        {context && <span>{context}</span>}
      </div>
      {selector}
      <div className="page-banner-emblem">
        <SpecialtyIcon name={specialty ?? ''} />
      </div>
    </div>
  );
}
