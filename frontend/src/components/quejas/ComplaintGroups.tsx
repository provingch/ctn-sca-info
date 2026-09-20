import { useId, useState, type ReactNode } from 'react';
import { quejaEstado, type QuejaItem } from '../../api/quejas';
import SpecialtyIcon from '../SpecialtyIcon';
import { normalizeSpecialty } from '../../theme/theme';
import './ComplaintGroups.css';

const states = [
  { key: 'pendiente', label: 'Pendientes' },
  { key: 'aceptada', label: 'Aceptadas' },
  { key: 'revisada', label: 'Revisadas' },
  { key: 'resuelta', label: 'Resueltas' },
  { key: 'rechazada', label: 'Rechazadas' },
] as const;

/** Only brands the specialty label: action buttons inherit the account theme. */
export default function ComplaintGroups({ quejas, specialtyName, children }: {
  quejas: QuejaItem[];
  specialtyName: (queja: QuejaItem) => string;
  children: (items: QuejaItem[]) => ReactNode;
}) {
  const id = useId();
  const [filter, setFilter] = useState<'todas' | ReturnType<typeof quejaEstado>>('todas');
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});
  const counts = new Map(states.map(state => [state.key, quejas.filter(q => quejaEstado(q) === state.key).length]));
  const filtered = quejas.filter(q => filter === 'todas' || quejaEstado(q) === filter);
  const groups = new Map<string, { name: string; items: QuejaItem[] }>();
  for (const q of filtered) {
    const name = specialtyName(q);
    const key = q.especialidadId != null ? String(q.especialidadId) : normalizeSpecialty(name);
    const group = groups.get(key) ?? { name, items: [] };
    group.items.push(q);
    groups.set(key, group);
  }
  return <div className="complaint-detail-groups">
    <div className="complaint-state-filters" role="group" aria-label="Filtrar quejas por estado">
      {[{ key: 'todas', label: 'Todas' }, ...states].map(state => <button key={state.key} type="button" className={`button ${filter === state.key ? '' : 'secondary'}`} aria-pressed={filter === state.key}
        onClick={() => setFilter(state.key as typeof filter)}>{state.label} ({state.key === 'todas' ? quejas.length : counts.get(state.key as ReturnType<typeof quejaEstado>)})</button>)}
    </div>
    <p className="complaint-group-count" role="status">Mostrando {filtered.length} de {quejas.length} quejas de este profesor.</p>
    {!filtered.length && <div className="complaint-group-empty"><p>No hay quejas en este estado.</p><button className="button secondary" type="button" onClick={() => setFilter('todas')}>Ver todas las quejas</button></div>}
    <div className="complaints-groups">{[...groups].sort((a, b) => a[1].name.localeCompare(b[1].name, 'es')).map(([key, group]) => {
      const open = expanded[key] ?? groups.size === 1;
      const panelId = `${id}-specialty-${key}`;
      return <section key={key} className="complaints-specialty">
        <h3 className="complaints-specialty-heading"><button type="button" aria-expanded={open} aria-controls={panelId} id={`${panelId}-heading`} onClick={() => setExpanded(current => ({ ...current, [key]: !open }))}>
          <span className="complaint-specialty-brand" data-specialty={normalizeSpecialty(group.name)}>
            <span className="complaints-specialty-logo" aria-hidden="true"><SpecialtyIcon name={group.name} /></span><span>{group.name}</span>
          </span>
          <span className="complaints-specialty-count">{group.items.length} {group.items.length === 1 ? 'queja' : 'quejas'}</span>
          <svg className="complaints-specialty-chevron" aria-hidden="true" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="m6 9 6 6 6-6" /></svg>
        </button></h3>
        <div id={panelId} role="region" aria-labelledby={`${panelId}-heading`} hidden={!open} className="complaint-specialty-body">
          {states.map(state => {
            const items = group.items.filter(q => quejaEstado(q) === state.key).sort((a, b) => b.creadaEn.localeCompare(a.creadaEn) || b.id - a.id);
            return items.length ? <section key={state.key} aria-labelledby={`${panelId}-${state.key}`}>
              <h4 id={`${panelId}-${state.key}`} className="complaint-state-heading"><span className={`complaint-status ${state.key}`}>{state.label}</span><span>{items.length}</span></h4>
              {children(items)}
            </section> : null;
          })}
        </div>
      </section>;
    })}</div>
  </div>;
}
