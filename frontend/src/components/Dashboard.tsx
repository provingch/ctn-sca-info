import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';

/*
 * Bloques de los paneles de inicio (oct-2026): una fila de cifras arriba, la tabla de lo que hay que
 * hacer hoy y, al costado, los accesos a cada apartado. Los datos los arma cada panel con sus propias
 * llamadas; acá solo se dibujan.
 */

export type DashboardTone = 'danger' | 'warning' | 'success' | 'accent' | 'neutral';

export type DashboardStat = {
  key: string;
  label: string;
  /** `null` mientras carga; `undefined` si no se pudo obtener. */
  value: number | string | null | undefined;
  tone?: DashboardTone;
};

export function DashboardStats({ stats }: { stats: DashboardStat[] }) {
  return <div className="dash-stats" role="list" aria-label="Resumen">
    {stats.map((stat) => (
      <div key={stat.key} role="listitem" className={`dash-stat${stat.tone ? ` tone-${stat.tone}` : ''}`}>
        <strong aria-live="polite">{stat.value === null ? '…' : stat.value === undefined ? '—' : stat.value}</strong>
        <span>{stat.label}</span>
      </div>
    ))}
  </div>;
}

type TaskAction = { label: string; primary?: boolean } & ({ onSelect: () => void; href?: never } | { href: string; onSelect?: never });

export type DashboardTask = {
  key: string;
  kind: string;
  tone: DashboardTone;
  title: ReactNode;
  detail?: ReactNode;
  when?: string;
  action?: TaskAction;
};

function TaskButton({ action }: { action: TaskAction }) {
  const className = `button small${action.primary ? '' : ' secondary'}`;
  return action.href ? <Link className={className} to={action.href}>{action.label}</Link>
    : <button type="button" className={className} onClick={action.onSelect}>{action.label}</button>;
}

/** Tabla "para hoy": cada fila dice qué es, de quién, desde cuándo y trae su acción. */
export function DashboardTasks({ title, tasks, emptyText, link, columns = ['Qué', 'Detalle', 'Desde'] }: {
  title: string;
  /** `null` mientras carga. */
  tasks: DashboardTask[] | null;
  emptyText: string;
  link?: { label: string; href?: string; onSelect?: () => void };
  columns?: [string, string, string];
}) {
  return <section className="panel dash-tasks" aria-label={title}>
    <header className="dash-tasks-head">
      <h2>{title}</h2>
      {link && (link.href ? <Link to={link.href}>{link.label}</Link> : <button type="button" className="link-button" onClick={link.onSelect}>{link.label}</button>)}
    </header>
    {tasks === null ? <p className="dash-tasks-empty">Cargando…</p>
      : tasks.length === 0 ? <p className="dash-tasks-empty">{emptyText}</p>
      : <div className="dash-tasks-scroll"><table className="dash-table">
        <thead><tr><th scope="col">{columns[0]}</th><th scope="col">{columns[1]}</th><th scope="col">{columns[2]}</th><th scope="col"><span className="visually-hidden">Acción</span></th></tr></thead>
        <tbody>
          {tasks.map((task) => <tr key={task.key}>
            <td><span className={`launcher-badge tone-${task.tone}`}>{task.kind}</span></td>
            <td><strong>{task.title}</strong>{task.detail && <span>{task.detail}</span>}</td>
            <td className="dash-when">{task.when ?? ''}</td>
            <td className="dash-action">{task.action && <TaskButton action={task.action} />}</td>
          </tr>)}
        </tbody>
      </table></div>}
  </section>;
}

/** Cifras arriba, tareas a la izquierda y accesos (más lo que se agregue) a la derecha. */
export function DashboardLayout({ stats, tasks, side, children }: { stats: ReactNode; tasks: ReactNode; side: ReactNode; children?: ReactNode }) {
  return <div className="dash">
    {stats}
    <div className="dash-grid">
      <div className="dash-main">{tasks}</div>
      <aside className="dash-side">{side}</aside>
    </div>
    {children}
  </div>;
}
