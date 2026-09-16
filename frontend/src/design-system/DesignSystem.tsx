import type { ReactNode } from 'react';
import { designVariables } from './tokens';
import './styles.css';

export interface DesignSystemProps {
  children: ReactNode;
  specialty?: string;
  /** Optional custom brand color, #RRGGBB. Accessible variants are derived. */
  accent?: string;
  className?: string;
}
/** Scoped theme: never changes :root, localStorage, or the existing application. */
export function DesignSystem({ children, specialty, accent, className = '' }: DesignSystemProps) {
  return <div className={`sca-ds ${className}`} style={designVariables(specialty, accent)}>{children}</div>;
}
