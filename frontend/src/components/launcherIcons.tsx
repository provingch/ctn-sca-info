import type { ReactNode } from 'react';

function icon(children: ReactNode) {
  return <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{children}</svg>;
}

export const launcherIcons = {
  clase: icon(<><circle cx="12" cy="12" r="9" /><path d="M10 8.5v7l6-3.5-6-3.5Z" /></>),
  planCurricular: icon(<><rect x="6" y="4" width="12" height="16" rx="2" /><path d="M9 4V3a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v1" /><path d="m9.5 12.5 1.8 1.8 3.2-3.6" /></>),
  clasesDadas: icon(<><path d="M3 12a9 9 0 1 0 3-6.7" /><path d="M3 4v4h4" /><path d="M12 8v4l3 2" /></>),
  quejas: icon(<><path d="M4 5h16a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H9l-4 4v-4H4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1Z" /><path d="M12 8v4" /><path d="M12 15h.01" /></>),
  conducta: icon(<><path d="M12 3l7 3v6c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6l7-3Z" /><path d="m9.5 12 2 2 3.5-3.5" /></>),
  verPlanillas: icon(<><rect x="3" y="5" width="18" height="14" rx="2" /><path d="M3 10h18M9 5v14" /><circle cx="15.5" cy="14.5" r="1.5" /></>),
  descargarPlanillas: icon(<><path d="M12 4v10" /><path d="m8 10 4 4 4-4" /><path d="M5 18h14" /></>),
  revisarPlanes: icon(<><rect x="5" y="3" width="12" height="16" rx="2" /><path d="M8 8h6M8 11.5h6" /><circle cx="17" cy="17" r="4" /><path d="m15.5 17 1 1 2-2" /></>),
  seguimiento: icon(<><path d="M4 19V5" /><path d="M4 19h16" /><path d="m7 15 4-4 3 3 5-6" /></>),
} satisfies Record<string, ReactNode>;
