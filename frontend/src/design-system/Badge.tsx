import type { ReactNode } from 'react';
export type BadgeTone = 'neutral' | 'success' | 'warning' | 'error' | 'info' | 'brand';
export interface BadgeProps { children: ReactNode; tone?: BadgeTone }
/** Status must always include text; color is supplementary. Brand is not a status. */
export function Badge({ children, tone = 'neutral' }: BadgeProps) {
  return <span className={`ds-badge ds-badge--${tone}`}>{children}</span>;
}
