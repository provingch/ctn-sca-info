import { useId, type ReactNode } from 'react';
export interface CardProps {
  title?: string;
  description?: string;
  variant?: 'standard' | 'featured';
  level?: 1 | 2 | 3;
  headingLevel?: 2 | 3 | 4;
  action?: ReactNode;
  children: ReactNode;
  className?: string;
}
export function Card({ title, description, variant = 'standard', level = 1, headingLevel = 2, action, children, className = '' }: CardProps) {
  const id = useId();
  const Heading = `h${headingLevel}` as 'h2' | 'h3' | 'h4';
  return <section className={`ds-card ds-card--${variant} ds-surface-${level} ${className}`} aria-labelledby={title ? id : undefined}>
    {(title || description || action) && <header className="ds-card-header"><div>{title && <Heading id={id} className="ds-h3">{title}</Heading>}{description && <p className="ds-secondary ds-small">{description}</p>}</div>{action}</header>}
    {children}
  </section>;
}
