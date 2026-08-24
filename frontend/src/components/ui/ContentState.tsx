import type { ReactNode } from 'react';

export type ContentStateTone = 'loading' | 'empty' | 'error';

interface ContentStateProps {
  title: string;
  detail?: string;
  tone?: ContentStateTone;
  compact?: boolean;
  className?: string;
  actions?: ReactNode;
}

export default function ContentState({
  title,
  detail,
  tone = 'empty',
  compact = false,
  className = '',
  actions,
}: ContentStateProps) {
  const role = tone === 'error' ? 'alert' : 'status';

  return <section
    className={`content-state content-state--${tone}${compact ? ' content-state--compact' : ''}${className ? ` ${className}` : ''}`}
    role={role}
    aria-live={tone === 'error' ? 'assertive' : 'polite'}
    aria-busy={tone === 'loading' || undefined}
  >
    <span className="content-state-icon" aria-hidden="true">
      {tone === 'loading' ? <i /> : tone === 'error' ? '!' : '—'}
    </span>
    <div className="content-state-copy">
      <h2>{title}</h2>
      {detail && <p>{detail}</p>}
    </div>
    {actions && <div className="content-state-actions">{actions}</div>}
  </section>;
}
