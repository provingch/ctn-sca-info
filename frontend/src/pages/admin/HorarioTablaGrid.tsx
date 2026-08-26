import type { ReactNode } from 'react';
import type { HoraCatedraItem } from '../../api/admin';

export const SCHEDULE_DAYS = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];

export interface HorarioGridCell {
  content: ReactNode;
  className?: string;
  title?: string;
}

interface HorarioTablaGridProps {
  hours: HoraCatedraItem[];
  renderCell: (day: number, hour: HoraCatedraItem) => HorarioGridCell;
  className?: string;
  wrapClassName?: string;
}

export default function HorarioTablaGrid({ hours, renderCell, className, wrapClassName }: HorarioTablaGridProps) {
  return (
    <div className={`table-wrap ${wrapClassName ?? ''}`.trim()}>
      <table className={`grade-table ${className ?? ''}`.trim()}>
        <thead>
          <tr>
            <th>Hora</th>
            {SCHEDULE_DAYS.slice(1).map((day) => <th key={day}>{day}</th>)}
          </tr>
        </thead>
        <tbody>
          {hours.map((hour) => (
            <tr key={hour.id}>
              <th>
                <span>{hour.numero}°</span>
                <small>{hour.horaInicio} - {hour.horaFin}</small>
              </th>
              {SCHEDULE_DAYS.slice(1).map((day, index) => {
                const cell = renderCell(index + 1, hour);
                return <td key={day} className={cell.className} title={cell.title}>{cell.content}</td>;
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
