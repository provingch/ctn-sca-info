import { useEffect, useMemo, useState } from 'react';
import { ApiError } from '../../api/client';
import { confirmHorarioImport, downloadHorarioCurso, getHoraCatedraCatalog, getHorarioResumen, previewHorarioImport, type HoraCatedraItem, type HorarioImportRowItem, type HorarioResumenCursoItem } from '../../api/admin';
import ContentState from '../../components/ui/ContentState';
import { groupSchedulesBySpecialty } from './adminFormatters';
import SpecialtyIcon from '../../components/SpecialtyIcon';

const DAYS = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];

export default function HorariosPanel({ status }: { status: (message: string) => void }) {
  const [items, setItems] = useState<HorarioResumenCursoItem[] | null>(null);
  const [hours, setHours] = useState<HoraCatedraItem[]>([]);
  const [error, setError] = useState('');
  const [downloadingId, setDownloadingId] = useState<number | null>(null);
  const [course, setCourse] = useState<HorarioResumenCursoItem | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [rows, setRows] = useState<HorarioImportRowItem[] | null>(null);
  const [confirming, setConfirming] = useState(false);

  useEffect(() => { Promise.all([getHorarioResumen(), getHoraCatedraCatalog()]).then(([summary, catalog]) => { setItems(summary); setHours(catalog); }).catch((reason) => setError(reason instanceof ApiError ? reason.message : 'No se pudo cargar el resumen de horarios.')); }, []);
  const groups = useMemo(() => groupSchedulesBySpecialty(items ?? []), [items]);
  if (!items) return <ContentState tone={error ? 'error' : 'loading'} title={error || 'Cargando horarios…'} detail={error ? 'Volvé a intentarlo recargando esta página.' : 'Estamos agrupando los cursos por especialidad.'} />;
  if (items.length === 0) return <ContentState title="No hay cursos disponibles" detail="Los cursos con horarios aparecerán en este panel." />;

  async function download(courseItem: HorarioResumenCursoItem) { setDownloadingId(courseItem.cursoId); try { await downloadHorarioCurso(courseItem.cursoId); status(`Horario de ${courseItem.cursoDescripcion} descargado.`); } catch (reason) { status(reason instanceof ApiError ? reason.message : 'No se pudo descargar el horario.'); } finally { setDownloadingId(null); } }
  async function preview(courseItem: HorarioResumenCursoItem, selected: File | null) { if (!selected) return; setCourse(courseItem); setFile(selected); setRows(null); try { setRows(await previewHorarioImport(courseItem.cursoId, selected)); } catch (reason) { setCourse(null); status(reason instanceof ApiError ? reason.message : 'No se pudo leer el horario.'); } }
  async function confirm() { if (!course || !file) return; setConfirming(true); try { const result = await confirmHorarioImport(course.cursoId, file); status(`Carga completada: ${result.creados} creados, ${result.omitidos} omitidos.`); setCourse(null); setRows(null); setFile(null); setItems(await getHorarioResumen()); } catch (reason) { status(reason instanceof ApiError ? reason.message : 'No se pudo confirmar la carga.'); } finally { setConfirming(false); } }
  const rowFor = (day: number, hour: number) => rows?.find((row) => row.diaSemana === day && row.horaCatedraId === hour);

  return <div className="admin-summary-groups">{groups.map((group) => <section className="panel admin-summary-section" key={group.specialty}>
    <header className="admin-summary-heading"><div><span>Especialidad</span><h2 className="specialty-card-title"><SpecialtyIcon name={group.specialty} />{group.specialty}</h2></div><strong>{group.courses.length} {group.courses.length === 1 ? 'curso' : 'cursos'}</strong></header>
    <div className="admin-list">{group.courses.map((courseItem) => <div key={courseItem.cursoId}><span><strong>{courseItem.cursoDescripcion}</strong><small>{courseItem.cantidadSlotsCargados} {courseItem.cantidadSlotsCargados === 1 ? 'bloque cargado' : 'bloques cargados'}</small></span><span className="admin-actions"><label className="button secondary">Cargar horario<input hidden type="file" accept=".xlsx" onChange={(event) => void preview(courseItem, event.target.files?.[0] ?? null)} /></label><button className="button secondary" type="button" disabled={downloadingId === courseItem.cursoId} onClick={() => void download(courseItem)}>{downloadingId === courseItem.cursoId ? 'Descargando…' : 'Descargar'}</button></span></div>)}</div>
  </section>)}
  {course && rows && <div className="schedule-import-modal" role="dialog" aria-modal="true"><div className="schedule-import-card"><header className="admin-summary-heading"><div><span>Vista previa</span><h2>{course.cursoDescripcion}</h2></div><button className="signature-modal-close" type="button" onClick={() => setCourse(null)}>×</button></header><div className="table-wrap schedule-preview-wrap"><table className="grade-table schedule-preview"><thead><tr><th>Hora</th>{DAYS.slice(1).map((day) => <th key={day}>{day}</th>)}</tr></thead><tbody>{hours.map((hour) => <tr key={hour.id}><th><span>{hour.numero}°</span><small>{hour.horaInicio} - {hour.horaFin}</small></th>{DAYS.slice(1).map((_, index) => { const row = rowFor(index + 1, hour.id); return <td key={index} className={row && row.estado !== 'ok' ? `schedule-cell-${row.estado}` : ''} title={row?.detalle ?? ''}>{row ? <><strong>{row.materiaTexto}</strong><small>{row.profesorTexto}</small>{row.detalle && <em>{row.detalle}</em>}</> : '—'}</td>; })}</tr>)}</tbody></table></div><footer className="schedule-import-actions"><button className="button secondary" type="button" onClick={() => setCourse(null)}>Cancelar</button><button className="button" type="button" disabled={confirming || !rows.some((row) => row.estado === 'ok')} onClick={() => void confirm()}>{confirming ? 'Aplicando…' : 'Confirmar carga'}</button></footer></div></div>}
  </div>;
}
