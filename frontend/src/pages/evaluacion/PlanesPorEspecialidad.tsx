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
  const [groupBy, setGroupBy] = useState<'especialidad' | 'profesor'>('especialidad');
  const [expandedGroups, setExpandedGroups] = useState<Record<string, boolean>>({});
  const groups = new Map<string, { name: string; items: PlanPendienteResumen[] }>();
  for (const plan of planes) {
    const name = groupBy === 'especialidad' ? plan.especialidad?.trim() || 'Especialidad no disponible' : plan.profesorNombre?.trim() || 'Profesor no disponible';
    // Without an identity from an older API, keep entries separate rather than merge namesakes.
    const key = groupBy === 'especialidad' ? normalizeSpecialty(name) : `profesor-${plan.profesorId ?? `plan-${plan.id}`}`;
    const group = groups.get(key) ?? { name, items: [] };
    group.items.push(plan);
    groups.set(key, group);
  }
  return <><div className="tabs" role="group" aria-label="Agrupar planes por" style={{ marginBottom: 16 }}>
    <button type="button" className={groupBy === 'especialidad' ? 'active' : ''} aria-pressed={groupBy === 'especialidad'} onClick={() => setGroupBy('especialidad')}>Especialidad</button>
    <button type="button" className={groupBy === 'profesor' ? 'active' : ''} aria-pressed={groupBy === 'profesor'} onClick={() => setGroupBy('profesor')}>Profesor</button>
  </div><div className="complaints-groups">{Array.from(groups).sort((a, b) => a[1].name.localeCompare(b[1].name, 'es')).map(([key, group]) => {
    const expanded = expandedGroups[key] ?? groups.size === 1;
    const panelId = `${id}-planes-${key}`;
    return <section className="complaints-specialty" data-specialty={groupBy === 'especialidad' ? key : undefined} key={key}>
      <h4 className="complaints-specialty-heading"><button type="button" id={`${panelId}-heading`} aria-expanded={expanded} aria-controls={panelId}
        onClick={() => setExpandedGroups((current) => ({ ...current, [key]: !expanded }))}>
        <span className="complaints-specialty-logo" aria-hidden="true">{groupBy === 'especialidad' ? <SpecialtyIcon name={group.name} /> : <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><circle cx="12" cy="8" r="4" /><path d="M4 22v-3a8 8 0 0 1 16 0v3" /></svg>}</span>
        <span className="complaints-specialty-copy"><span>{group.name}</span><small>{expanded ? groupBy === 'especialidad' ? 'Planes de esta especialidad' : 'Planes de este profesor' : 'Abrir para ver los planes'}</small></span>
        <span className="complaints-specialty-count">{group.items.length} {group.items.length === 1 ? 'plan' : 'planes'}</span>
        <svg className="complaints-specialty-chevron" aria-hidden="true" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="m6 9 6 6 6-6" /></svg>
      </button></h4>
      <div id={panelId} role="region" aria-labelledby={`${panelId}-heading`} hidden={!expanded}>
        <div style={{ padding: 12 }}>{children(group.items)}</div>
      </div>
    </section>;
  })}</div></>;
}
