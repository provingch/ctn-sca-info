import type { ButtonHTMLAttributes, ReactNode } from 'react';
interface ButtonBase extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children' | 'aria-label'> {
  'aria-label': string;
  loading?: boolean;
  loadingLabel?: string;
}
export type ButtonProps = ButtonBase & (
  | { variant: 'icon'; icon: ReactNode; children?: never }
  | { variant?: 'primary' | 'secondary'; icon?: ReactNode; children: ReactNode }
);
export function Button({ variant = 'primary', icon, children, loading = false, loadingLabel = 'Cargando', disabled, className = '', type = 'button', 'aria-label': label, ...props }: ButtonProps) {
  return <button {...props} type={type} className={`ds-button ds-button--${variant} ${className}`} disabled={disabled || loading} aria-busy={loading || undefined} aria-label={loading ? `${label}: ${loadingLabel}` : label}>
    {loading ? <span className="ds-spinner" aria-hidden="true" /> : icon && <span className="ds-button-icon" aria-hidden="true">{icon}</span>}
    {variant !== 'icon' && <span>{children}</span>}
  </button>;
}
