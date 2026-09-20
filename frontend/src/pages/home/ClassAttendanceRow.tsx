import { useState } from 'react';
import type { ClaseDetalleDto, CodigoConducta, UpdateClaseRequest } from '../../api/home';
import { isLate, studentName } from './classHistoryUtils';

export type AttendanceState = UpdateClaseRequest['asistencias'][number]['estado'];
const labels: Record<string, string> = { presente: 'Presente', ausente: 'Ausente', ausente_justificado: 'Justificado', pendiente: 'Pendiente' };
export default function ClassAttendanceRow({ item, index, estado, codes, catalog, editing, busy, onState, onCode, onJustify }: {
  item: ClaseDetalleDto['asistencias'][number]; index: number; estado: string; codes: string[]; catalog: CodigoConducta[];
  editing: boolean; busy: boolean; onState: (state: AttendanceState) => void; onCode: (code: string) => void; onJustify: () => void;
}) {
  const [expanded, setExpanded] = useState(false);
  const name = studentName(item);
  const late = estado === 'presente' && isLate(codes, catalog);
  const tone = estado === 'ausente' ? 'absent' : estado === 'ausente_justificado' || late ? 'warning' : estado === 'presente' ? 'present' : 'neutral';
  const visibleCodes = editing || expanded ? codes : codes.slice(0, 2);
  return <tr>
    <td className="class-row-number">{index + 1}</td>
    <th scope="row" className="class-student-name">{name}</th>
    <td>
      <span className={`class-attendance-badge ${tone}`}>{late ? 'Presente · tarde' : labels[estado] ?? estado}</span>
      {editing && <select aria-label={`Estado de ${name}`} value={estado} disabled={busy} onChange={(event) => onState(event.target.value as AttendanceState)}>
        <option value="presente">Presente</option><option value="ausente">Ausente</option><option value="ausente_justificado">Justificado</option><option value="pendiente">Pendiente</option>
      </select>}
    </td>
    <td><div className="class-traits">
      {visibleCodes.map((code) => {
        const description = catalog.find((entry) => entry.codigo === code)?.descripcion;
        const text = description ? `${code} · ${description}` : code;
        return <span className={`class-trait ${isLate([code], catalog) ? 'late' : ''}`} key={code} title={text}>
          <span>{text}</span>{editing && <button type="button" aria-label={`Quitar ${code} de ${name}`} disabled={busy} onClick={() => onCode(code)}>×</button>}
        </span>;
      })}
      {!codes.length && <span className="class-traits-empty">Sin rasgos</span>}
      {!editing && codes.length > 2 && <button className="class-traits-more" type="button" aria-label={`${expanded ? 'Mostrar menos rasgos' : 'Ver todos los rasgos'} de ${name}`} aria-expanded={expanded} onClick={() => setExpanded(!expanded)}>{expanded ? 'Ver menos' : `+${codes.length - 2}`}</button>}
    </div></td>
    <td>{editing ? <select value="" aria-label={`Agregar rasgo a ${name}`} disabled={busy || !catalog.some((entry) => entry.activo && !codes.includes(entry.codigo))} onChange={(event) => { if (event.target.value) onCode(event.target.value); }}>
      <option value="">+ Rasgo</option>{catalog.filter((entry) => entry.activo && !codes.includes(entry.codigo)).map((entry) => <option key={entry.codigo} value={entry.codigo}>{entry.codigo} · {entry.descripcion}</option>)}
    </select> : estado === 'ausente' ? <button className="button secondary" type="button" aria-label={`Justificar ausencia de ${name}`} disabled={busy} onClick={onJustify}>Justificar</button> : null}</td>
  </tr>;
}
