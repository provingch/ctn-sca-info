import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiError } from '../../api/client';
import {
  confirmHorarioImport,
  createHorarioSlot,
  deleteHorarioSlot,
  downloadHorarioCurso,
  getAsignacionesPorCurso,
  getHorarioCurso,
  getSalas,
  previewHorarioImport,
  type AsignacionResumenItem,
  type HoraCatedraItem,
  type HorarioImportRowItem,
  type HorarioResumenCursoItem,
  type HorarioSlotItem,
  type SalaItem,
} from '../../api/admin';
import ContentState from '../../components/ui/ContentState';
import HorarioTablaGrid, { SCHEDULE_DAYS, type HorarioGridCell } from './HorarioTablaGrid';
import SpecialtyIcon from '../../components/SpecialtyIcon';

interface HorarioCursoPageProps {
  cursoId: string | undefined;
  summary: HorarioResumenCursoItem[];
  hours: HoraCatedraItem[];
  status: (message: string) => void;
  refreshSummary: () => Promise<void>;
}

const makeKey = (day: number, hourId: number) => `${day}:${hourId}`;

export default function HorarioCursoPage({ cursoId, summary, hours, status, refreshSummary }: HorarioCursoPageProps) {
  const courseId = Number(cursoId);
  const courseItem = useMemo(() => summary.find((item) => item.cursoId === courseId) ?? null, [courseId, summary]);
  const [slots, setSlots] = useState<HorarioSlotItem[]>([]);
  const [assignments, setAssignments] = useState<AsignacionResumenItem[]>([]);
  const [salas, setSalas] = useState<SalaItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewRows, setPreviewRows] = useState<HorarioImportRowItem[] | null>(null);
  const [confirming, setConfirming] = useState(false);
  const [manualAssignment, setManualAssignment] = useState('');
  const [manualDay, setManualDay] = useState('1');
  const [manualHour, setManualHour] = useState('');
  const [manualUntil, setManualUntil] = useState('');
  const [manualSala, setManualSala] = useState('');
  const [savingManual, setSavingManual] = useState(false);

  const loadCourseData = async () => {
    if (!courseItem) {
      setError(Number.isNaN(courseId) ? 'El identificador del curso no es válido.' : 'No se encontró el curso solicitado.');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError('');
    try {
      const [courseSlots, courseAssignments, courseSalas] = await Promise.all([
        getHorarioCurso(courseItem.cursoId),
        getAsignacionesPorCurso(courseItem.cursoId),
        getSalas(courseItem.especialidadId),
      ]);
      setSlots(courseSlots);
      setAssignments(courseAssignments);
      setSalas(courseSalas);
      setManualAssignment(String(courseAssignments[0]?.asignacionId ?? ''));
      setManualDay('1');
      setManualHour(String(hours[0]?.id ?? ''));
      setManualUntil(String(hours[0]?.id ?? ''));
      setManualSala('');
      setPreviewRows(null);
      setSelectedFile(null);
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No se pudo cargar el horario del curso.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadCourseData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [courseItem?.cursoId, courseItem?.especialidadId]);

  const sortedSlots = useMemo(() => [...slots].sort((first, second) => first.diaSemana - second.diaSemana || first.horaCatedraId - second.horaCatedraId), [slots]);
  const activeRows = previewRows ?? sortedSlots;
  const cellMap = useMemo(() => new Map(activeRows.map((item) => [makeKey(item.diaSemana, item.horaCatedraId), item])), [activeRows]);
  const hasPreviewOkRows = previewRows?.some((row) => row.estado === 'ok') ?? false;

  const renderCell = (day: number, hour: HoraCatedraItem): HorarioGridCell => {
    const item = cellMap.get(makeKey(day, hour.id));
    if (!item) {
      return { content: '—' };
    }

    if ('estado' in item) {
      return {
        className: item.estado !== 'ok' ? `schedule-cell-${item.estado}` : undefined,
        title: item.detalle ?? undefined,
        content: (
          <>
            <strong>{item.materiaTexto}</strong>
            <small>{item.profesorTexto}{item.salaNombre ? ` · ${item.salaNombre}` : ''}</small>
            {item.detalle && <em>{item.detalle}</em>}
          </>
        ),
      };
    }

    return {
      content: (
        <>
          <strong>{item.materiaNombre}</strong>
          <small>{item.profesorNombre}{item.salaNombre ? ` · ${item.salaNombre}` : ''}</small>
        </>
      ),
    };
  };

  async function download() {
    if (!courseItem) return;
    try {
      await downloadHorarioCurso(courseItem.cursoId);
      status(`Horario de ${courseItem.cursoDescripcion} descargado.`);
    } catch (reason) {
      status(reason instanceof ApiError ? reason.message : 'No se pudo descargar el horario.');
    }
  }

  async function previewFile(selected: File | null) {
    if (!courseItem || !selected) return;
    setSelectedFile(selected);
    setPreviewRows(null);
    try {
      setPreviewRows(await previewHorarioImport(courseItem.cursoId, selected));
    } catch (reason) {
      setSelectedFile(null);
      setPreviewRows(null);
      status(reason instanceof ApiError ? reason.message : 'No se pudo leer el horario.');
    }
  }

  async function confirmImport() {
    if (!courseItem || !selectedFile) return;
    setConfirming(true);
    try {
      const result = await confirmHorarioImport(courseItem.cursoId, selectedFile);
      status(`Carga completada: ${result.creados} creados, ${result.omitidos} omitidos.`);
      await loadCourseData();
      await refreshSummary();
    } catch (reason) {
      status(reason instanceof ApiError ? reason.message : 'No se pudo confirmar la carga.');
    } finally {
      setConfirming(false);
    }
  }

  async function addManualSlot() {
    if (!manualAssignment || !manualHour || !manualUntil || !courseItem) return;
    setSavingManual(true);
    try {
      const start = hours.findIndex((hour) => String(hour.id) === manualHour);
      const end = hours.findIndex((hour) => String(hour.id) === manualUntil);
      if (start === -1 || end === -1) {
        status('Catálogo de horas no válido.');
        return;
      }
      const selectedHours = hours.slice(Math.min(start, end), Math.max(start, end) + 1);
      for (const hour of selectedHours) {
        await createHorarioSlot(Number(manualAssignment), {
          diaSemana: Number(manualDay),
          horaCatedraId: hour.id,
          salaId: manualSala ? Number(manualSala) : null,
        });
      }
      status(`${selectedHours.length} hora(s) agregada(s).`);
      await loadCourseData();
      await refreshSummary();
    } catch (reason) {
      status(reason instanceof ApiError ? reason.message : 'No se pudo agregar el rango de horario.');
    } finally {
      setSavingManual(false);
    }
  }

  async function removeManualSlot(slotId: number) {
    if (!courseItem) return;
    try {
      await deleteHorarioSlot(slotId);
      status('Slot removido.');
      await loadCourseData();
      await refreshSummary();
    } catch (reason) {
      status(reason instanceof ApiError ? reason.message : 'No se pudo quitar el slot.');
    }
  }

  if (loading) {
    return <ContentState tone="loading" title="Cargando horario…" detail="Estamos preparando la tabla del curso y sus herramientas." />;
  }

  if (error || !courseItem) {
    return (
      <ContentState
        tone="error"
        title={error || 'No se encontró el curso solicitado'}
        detail="Volvé al listado para elegir otro curso o recargá la página."
        actions={<Link className="button" to="/admin/horarios">Volver al listado</Link>}
      />
    );
  }

  return (
    <div className="schedule-course-page">
      <div className="toolbar schedule-course-toolbar">
        <Link className="button secondary" to="/admin/horarios">← Volver al listado</Link>
        <div className="schedule-course-actions">
          <label className="button secondary">
            Cargar
            <input hidden type="file" accept=".xlsx" onChange={(event) => void previewFile(event.target.files?.[0] ?? null)} />
          </label>
          <button className="button secondary" type="button" onClick={() => void download()}>Descargar</button>
        </div>
      </div>

      <section className="panel schedule-course-hero">
        <header className="admin-summary-heading">
          <div>
            <span>Horario del curso</span>
            <h2 className="specialty-card-title"><SpecialtyIcon name={courseItem.especialidad} />{courseItem.cursoDescripcion}</h2>
          </div>
          <strong>{sortedSlots.length} {sortedSlots.length === 1 ? 'bloque cargado' : 'bloques cargados'}</strong>
        </header>
        <div className="schedule-course-meta">
          <span>{courseItem.especialidad}</span>
          <span>•</span>
          <span>{previewRows ? 'Vista previa de carga activa' : 'Horario actual'}</span>
          {selectedFile && <span>•</span>}
          {selectedFile && <span>{selectedFile.name}</span>}
        </div>
      </section>

      <div className="schedule-course-layout">
        <section className="panel schedule-course-panel">
          <header className="admin-summary-heading">
            <div>
              <span>{previewRows ? 'Vista previa' : 'Tabla actual'}</span>
              <h2>Horario semanal</h2>
            </div>
            {previewRows && <strong>{previewRows.filter((row) => row.estado === 'ok').length} filas listas</strong>}
          </header>
          <HorarioTablaGrid
            hours={hours}
            className="schedule-course-grid"
            wrapClassName="schedule-course-preview-wrap"
            renderCell={renderCell}
          />
          {previewRows && (
            <footer className="schedule-course-preview-footer schedule-import-actions">
              <button className="button secondary" type="button" onClick={() => { setPreviewRows(null); setSelectedFile(null); }}>Cancelar</button>
              <button className="button" type="button" disabled={confirming || !hasPreviewOkRows} onClick={() => void confirmImport()}>
                {confirming ? 'Aplicando…' : 'Confirmar carga'}
              </button>
            </footer>
          )}
        </section>

        <aside className="panel schedule-course-sidebar">
          <header className="admin-summary-heading">
            <div>
              <span>Edición manual</span>
              <h2>Agregar o quitar bloques</h2>
            </div>
            <strong>{assignments.length} {assignments.length === 1 ? 'asignación' : 'asignaciones'}</strong>
          </header>

          <div className="form-grid schedule-course-form">
            <label>Materia - Profesor
              <select value={manualAssignment} onChange={(event) => setManualAssignment(event.target.value)}>
                {assignments.map((item) => <option key={item.asignacionId} value={item.asignacionId}>{item.materiaNombre} — {item.profesorNombre}</option>)}
              </select>
            </label>
            <label>Día
              <select value={manualDay} onChange={(event) => setManualDay(event.target.value)}>
                {SCHEDULE_DAYS.slice(1).map((day, index) => <option key={day} value={index + 1}>{day}</option>)}
              </select>
            </label>
            <label>Desde
              <select value={manualHour} onChange={(event) => setManualHour(event.target.value)}>
                {hours.map((hour) => <option key={hour.id} value={hour.id}>{hour.numero}° · {hour.horaInicio} - {hour.horaFin}</option>)}
              </select>
            </label>
            <label>Hasta
              <select value={manualUntil} onChange={(event) => setManualUntil(event.target.value)}>
                {hours.map((hour) => <option key={hour.id} value={hour.id}>{hour.numero}° · {hour.horaInicio} - {hour.horaFin}</option>)}
              </select>
            </label>
            <label>Sala
              <select value={manualSala} onChange={(event) => setManualSala(event.target.value)}>
                <option value="">Sin sala</option>
                {salas.map((sala) => <option key={sala.id} value={sala.id}>{sala.nombre}</option>)}
              </select>
            </label>
            <button className="button" type="button" onClick={() => void addManualSlot()} disabled={savingManual || assignments.length === 0 || hours.length === 0}>
              {savingManual ? 'Agregando…' : 'Agregar rango'}
            </button>
          </div>

          <div className="admin-list schedule-course-slot-list">
            {sortedSlots.length === 0 && <div className="admin-empty">Todavía no hay bloques cargados para este curso.</div>}
            {sortedSlots.map((slot) => (
              <div key={slot.id}>
                <span>
                  <strong>{SCHEDULE_DAYS[slot.diaSemana]} · {slot.horaInicio} - {slot.horaFin}</strong>
                  <small>{slot.materiaNombre} — {slot.profesorNombre}{slot.salaNombre ? ` · ${slot.salaNombre}` : ''}</small>
                </span>
                <button className="button danger" type="button" onClick={() => void removeManualSlot(slot.id)}>Quitar</button>
              </div>
            ))}
          </div>
        </aside>
      </div>
    </div>
  );
}
