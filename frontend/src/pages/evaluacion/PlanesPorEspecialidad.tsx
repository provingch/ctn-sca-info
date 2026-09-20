import { useId, useState, type ReactNode } from 'react';
import type { PlanPendienteResumen } from '../../api/planCurricular';
import SpecialtyIcon from '../../components/SpecialtyIcon';
import { normalizeSpecialty } from '../../theme/theme';

/** Groups the existing list elements without changing their layout or actions. */
export default function PlanesPorEspecialidad({ planes, children }: {
  planes: PlanPendienteResumen[];
  children: (planes: PlanPendienteResumen[]) => ReactNode;
}) {
  const id = useId();
  const [expandedGroups, setExpandedGroups] = useState<Record<string, boolean>>({});
  const groups = new Map<string, { name: string; items: PlanPendienteResumen[] }>();
  for (const plan of planes) {
    const name = plan.especialidad?.trim() || 'Especialidad no disponible';
    const key = normalizeSpecialty(name);
    const group = groups.get(key) ?? { name, items: [] };
    group.items.push(plan);
    groups.set(key, group);
  }
  return <div className="complaints-groups">{Array.from(groups).sort((a, b) => a[1].name.localeCompare(b[1].name, 'es')).map(([key, group]) => {
    const expanded = expandedGroups[key] ?? groups.size === 1;
    const panelId = `${id}-planes-${key}`;
    return <section className="complaints-specialty" data-specialty={key} key={key}>
      <h4 className="complaints-specialty-heading"><button type="button" id={`${panelId}-heading`} aria-expanded={expanded} aria-controls={panelId}
        onClick={() => setExpandedGroups((current) => ({ ...current, [key]: !expanded }))}>
        <span className="complaints-specialty-logo" aria-hidden="true"><SpecialtyIcon name={group.name} /></span>
        <span className="complaints-specialty-copy"><span>{group.name}</span><small>{expanded ? 'Planes de esta especialidad' : 'Abrir para ver los planes'}</small></span>
        <span className="complaints-specialty-count">{group.items.length} {group.items.length === 1 ? 'plan' : 'planes'}</span>
        <svg className="complaints-specialty-chevron" aria-hidden="true" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="m6 9 6 6 6-6" /></svg>
      </button></h4>
      <div id={panelId} role="region" aria-labelledby={`${panelId}-heading`} hidden={!expanded}>
        <div style={{ padding: 12 }}>{children(group.items)}</div>
      </div>
    </section>;
  })}</div>;
}
