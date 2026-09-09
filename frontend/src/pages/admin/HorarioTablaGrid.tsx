import type { HoraCatedraItem } from '../../api/admin';
import { WEEKLY_SCHEDULE_DAYS, type HorarioGridColumnState, type HorarioTablaGridProps } from './horarioSchedule';

function buildColumnStates(hours: HoraCatedraItem[], groupKey?: HorarioTablaGridProps['groupKey']) {
  return WEEKLY_SCHEDULE_DAYS.map((_, dayIndex) => {
    const states: HorarioGridColumnState[] = [];
    for (let index = 0; index < hours.length; ) {
      const hour = hours[index];
      const key = groupKey?.(dayIndex + 1, hour) ?? null;
      if (!key) {
        states.push({ kind: 'cell', rowSpan: 1 });
        index += 1;
        continue;
      }

      let span = 1;
      while (index + span < hours.length && (groupKey?.(dayIndex + 1, hours[index + span]) ?? null) === key) {
        span += 1;
      }
      states.push({ kind: 'cell', rowSpan: span });
      for (let skip = 1; skip < span; skip += 1) {
        states.push({ kind: 'skip' });
      }
      index += span;
    }
    return states;
  });
}

export default function HorarioTablaGrid({ hours, renderCell, groupKey, className, wrapClassName }: HorarioTablaGridProps) {
  const columnStates = buildColumnStates(hours, groupKey);
  return (
    <div className={`table-wrap ${wrapClassName ?? ''}`.trim()}>
      <table className={`grade-table ${className ?? ''}`.trim()}>
        <caption className="visually-hidden">Horario semanal por hora cátedra</caption>
        <thead>
          <tr>
            <th>Hora</th>
            {WEEKLY_SCHEDULE_DAYS.map((day) => <th key={day}>{day}</th>)}
          </tr>
        </thead>
        <tbody>
          {hours.map((hour, rowIndex) => (
            <tr key={hour.id}>
              <th>
                <span>{hour.numero}°</span>
                <small>{hour.horaInicio} - {hour.horaFin}</small>
              </th>
              {WEEKLY_SCHEDULE_DAYS.map((day, index) => {
                const columnState = columnStates[index][rowIndex];
                if (columnState.kind === 'skip') {
                  return null;
                }
                const cell = renderCell(index + 1, hour);
                return <td key={day} rowSpan={columnState.rowSpan > 1 ? columnState.rowSpan : undefined} className={cell.className} title={cell.title}>{cell.content}</td>;
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
