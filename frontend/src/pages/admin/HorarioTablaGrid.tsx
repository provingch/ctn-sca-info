import { SCHEDULE_DAYS, type HorarioTablaGridProps } from './horarioSchedule';

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
