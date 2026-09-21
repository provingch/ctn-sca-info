import { useEffect, useState } from 'react';
import { getProfile } from '../api/profile';
import { splitActivityLine } from '../pages/home/activityLine';

/** Only real, permitted activity; an empty dashboard never reserves a blank panel. */
export default function DashboardActivity() {
  const [activity, setActivity] = useState<string[]>([]);
  useEffect(() => {
    let active = true;
    void getProfile().then((profile) => {
      if (active && profile.showActivityPanel) setActivity(profile.activityLog ?? []);
    }).catch(() => { /* Optional dashboard content: keep hidden if unavailable. */ });
    return () => { active = false; };
  }, []);
  const entries = activity.map(splitActivityLine)
    .filter((entry) => entry && entry.message !== 'Inició sesión').slice(-4).reverse();
  if (!entries.length) return null;
  return <section className="panel dashboard-activity" aria-label="Actividad reciente">
    <h2>Actividad reciente</h2>
    <div className="launcher-activity-list">{entries.map((entry, index) => entry && (
      <div className="launcher-activity-item" key={`${entry.date}-${index}`}>
        <span className="launcher-activity-message">{entry.message}</span>
        <span className="launcher-activity-date">{entry.date.replace(/^(\d{4})-(\d{2})-(\d{2}) (\d{2}:\d{2}):\d{2}$/, '$3/$2/$1 · $4')}</span>
      </div>
    ))}</div>
  </section>;
}
