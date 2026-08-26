export const SCHEDULE_DAYS = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'] as const;

export interface HorarioGridCell {
  content: import('react').ReactNode;
  className?: string;
  title?: string;
}

export interface HorarioTablaGridProps {
  hours: import('../../api/admin').HoraCatedraItem[];
  renderCell: (day: number, hour: import('../../api/admin').HoraCatedraItem) => HorarioGridCell;
  groupKey?: (day: number, hour: import('../../api/admin').HoraCatedraItem) => string | null;
  className?: string;
  wrapClassName?: string;
}

export type HorarioGridColumnState = { kind: 'cell'; rowSpan: number } | { kind: 'skip' };
