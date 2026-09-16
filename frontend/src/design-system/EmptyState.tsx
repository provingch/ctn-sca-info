import type { ReactNode } from 'react';
import { Icon } from './Icon';
export interface EmptyStateProps { title: string; description: string; icon?: ReactNode; action?: ReactNode }
export function EmptyState({ title, description, icon = <Icon name="empty" />, action }: EmptyStateProps) {
  return <div className="ds-empty"><span className="ds-empty-icon" aria-hidden="true">{icon}</span><h3 className="ds-h3">{title}</h3><p className="ds-secondary">{description}</p>{action}</div>;
}
