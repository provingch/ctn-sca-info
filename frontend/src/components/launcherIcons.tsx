import type { ReactNode } from 'react';

function icon(children: ReactNode) {
  return <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{children}</svg>;
}

export const launcherIcons = {
  materias: icon(<><path d="M4 5h7v15H4zM14 5l5-1 3 15-5 1zM4 9h7M4 16h7" /></>),
  usuarios: icon(<><circle cx="9" cy="8" r="3" /><path d="M3 21v-3a6 6 0 0 1 12 0v3M16 5a3 3 0 0 1 0 6m2 4a5 5 0 0 1 3 5" /></>),
  asignaciones: icon(<><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /><path d="M10 6h7v5m-3-3 3 3 3-3M6 10v7h5m-3-3 3 3-3 3" /></>),
  alumnos: icon(<><path d="m2 8 10-5 10 5-10 5-10-5Zm4 3v6c4 3 8 3 12 0v-6M22 8v8" /></>),
  horarios: icon(<><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M7 3v4m10-4v4M3 11h18M7 15h3m4 0h3M7 18h3" /></>),
  sistema: icon(<><rect x="3" y="4" width="18" height="15" rx="2" /><path d="M7 22h10M12 19v3M5 12h4l2-5 3 9 2-4h3" /></>),
  salas: icon(<><path d="M4 21h16M6 21V3h12v18M9 7h6M9 11h6M9 21v-6h6v6" /></>),
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
