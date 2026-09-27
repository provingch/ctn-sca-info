import { useEffect, useState } from 'react';
import { getProfile } from '../api/profile';
import { splitActivityLine } from '../pages/home/activityLine';

/** Siempre visible, con un estado vacío en vez de desaparecer (ver HomeLauncher/AppShell). */
export default function DashboardActivity() {
  const [activity, setActivity] = useState<string[]>([]);
  useEffect(() => {
    let active = true;
    void getProfile().then((profile) => {
      if (active && profile.showActivityPanel) setActivity(profile.activityLog ?? []);
    }).catch(() => { /* Optional dashboard content: keep hidden if unavailable. */ });
    return () => { active = false; };
  }, []);
  const entries = activity.map(splitActivityLine).filter((entry) => entry).reverse();
  return <section className="panel dashboard-activity" aria-label="Actividad reciente">
    <h2>Actividad reciente</h2>
    {entries.length > 0 ? (
      <div className="launcher-activity-list">{entries.map((entry, index) => entry && (
        <div className="launcher-activity-item" key={`${entry.date}-${index}`}>
          <span className="launcher-activity-message">{entry.message}</span>
          <span className="launcher-activity-date">{entry.date.replace(/^(\d{4})-(\d{2})-(\d{2}) (\d{2}:\d{2}):\d{2}$/, '$3/$2/$1 · $4')}</span>
        </div>
      ))}</div>
    ) : (
      <p className="launcher-activity-empty">Sin actividad reciente todavía.</p>
    )}
  </section>;
}
