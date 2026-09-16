import type { ReactNode } from 'react';
export interface TopNavProps {
  brand: string;
  subtitle?: string;
  logo?: ReactNode;
  items: { id: string; label: string; href: string }[];
  activeId: string;
  user: { name: string; role: string; initials: string; href: string };
}
/** Page navigation uses links + aria-current, not ARIA tabs without tabpanels. */
export function TopNav({ brand, subtitle, logo, items, activeId, user }: TopNavProps) {
  return <header className="ds-topnav"><div className="ds-topnav-top"><div className="ds-brand">{logo && <span className="ds-brand-mark" aria-hidden="true">{logo}</span>}<div><strong>{brand}</strong>{subtitle && <p className="ds-tertiary ds-small">{subtitle}</p>}</div></div><a className="ds-profile" href={user.href} aria-label={`Ver perfil de ${user.name}`}><span className="ds-avatar" aria-hidden="true">{user.initials}</span><span><strong>{user.name}</strong><span className="ds-secondary ds-small">{user.role}</span></span></a></div><nav className="ds-nav" aria-label="Navegación principal">{items.map((item) => <a key={item.id} href={item.href} aria-current={item.id === activeId ? 'page' : undefined}>{item.label}</a>)}</nav></header>;
}
