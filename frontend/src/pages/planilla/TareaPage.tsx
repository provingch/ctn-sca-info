import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import AppShell from '../../components/AppShell';
import { createTarea, deleteTarea, getInstrumentos, getTarea, updateTarea, type Instrumento } from '../../api/academics';
import { ApiError } from '../../api/client';
import AnimatedSelect from '../../components/AnimatedSelect';
import ClassroomBadge from '../../components/ClassroomBadge';
import DatePicker from '../../components/DatePicker';
import { isValidDateValue, localDateValue } from '../../utils/dateInput';

export default function TareaPage() {
  const planillaId = Number(useParams().planillaId);
  const tareaId = Number(useParams().tareaId || 0);
  const navigate = useNavigate();
  const [instrumentos, setInstrumentos] = useState<Instrumento[]>([]);
  const [form, setForm] = useState({ instrumentoId: 0, fecha: localDateValue(), total: '10', titulo: '' });
  const [status, setStatus] = useState('');
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const submitting = useRef(false);
  const [classroomTask, setClassroomTask] = useState<{ id: string | null; url: string | null }>({ id: null, url: null });
  const isReadOnly = Boolean(classroomTask.id);
  const disabled = isReadOnly || busy || loading || loadFailed;

  useEffect(() => {
    let active = true;
    setLoading(true);
    setLoadFailed(false);
    Promise.all([getInstrumentos(), tareaId ? getTarea(tareaId) : Promise.resolve(null)])
      .then(([items, task]) => {
        if (!active) return;
        setInstrumentos(items);
        setForm(task ? { instrumentoId: task.instrumentoId, fecha: task.fecha, total: String(task.total), titulo: task.titulo } : { instrumentoId: 0, fecha: localDateValue(), total: '10', titulo: '' });
        setClassroomTask({ id: task?.googleCourseworkId ?? null, url: task?.googleCourseworkUrl ?? null });
      })
      .catch(() => { if (active) { setLoadFailed(true); setStatus('No se pudieron cargar los datos del formulario. Recargá la página para reintentar.'); } })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [tareaId]);

  const errors = {
    instrumento: instrumentos.some((item) => item.id === form.instrumentoId) ? '' : 'Seleccioná un instrumento.',
    titulo: form.titulo.trim() ? '' : 'Escribí un título para la tarea.',
    fecha: isValidDateValue(form.fecha) ? '' : 'Seleccioná una fecha válida.',
    total: !form.total.trim() ? 'Ingresá el puntaje total.' : !Number.isInteger(Number(form.total)) || Number(form.total) < 1 || Number(form.total) > 2147483647 ? 'Ingresá un número entero mayor que 0 (máximo 2147483647).' : '',
  };
  const valid = Object.values(errors).every((error) => !error);
  const showHints = !isReadOnly && !loading && !loadFailed;

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (disabled || submitting.current || !valid) return;
    submitting.current = true;
    setBusy(true);
    setStatus('');
    const payload = { ...form, total: Number(form.total), titulo: form.titulo.trim() };
    try {
      if (tareaId) await updateTarea(tareaId, payload);
      else await createTarea(planillaId, payload);
      navigate('/planilla/' + planillaId);
    } catch (err) {
      setStatus(err instanceof ApiError ? err.message : 'No se pudo guardar. Tus datos se conservaron para reintentar.');
    } finally { setBusy(false); submitting.current = false; }
  }

  async function remove() {
    if (disabled || !tareaId || !window.confirm('¿Eliminar esta tarea y sus datos asociados?')) return;
    setBusy(true);
    try { await deleteTarea(tareaId); navigate('/planilla/' + planillaId); }
    catch (err) { setStatus(err instanceof ApiError ? err.message : 'No se pudo eliminar.'); setBusy(false); }
  }

  return <AppShell title={tareaId ? 'Modificar tarea' : 'Agregar tarea'}><form className="panel form-grid task-form" onSubmit={submit} noValidate aria-busy={busy || loading}>
    {isReadOnly && <div className="notice info"><ClassroomBadge /> Esta tarea se gestiona en Google Classroom y no puede modificarse desde la planilla.</div>}
    {loading && <p role="status" className="muted-copy">Cargando datos de la tarea…</p>}
    <div className="form-field">
      <span className="field-label">Instrumento</span>
      <AnimatedSelect ariaLabel="Instrumento" value={form.instrumentoId || ''} required placeholder="Seleccioná un instrumento" disabled={disabled} describedBy={showHints && errors.instrumento ? 'task-instrument-hint' : undefined} onChange={(value) => setForm({ ...form, instrumentoId: Number(value) })} options={instrumentos.map((item) => ({ value: item.id, label: item.nombre }))} />
      {showHints && errors.instrumento && <small id="task-instrument-hint" className="field-hint">{errors.instrumento}</small>}
    </div>
    <label>Título<input value={form.titulo} placeholder="Trabajo práctico N°3" required disabled={disabled} aria-describedby={showHints && errors.titulo ? 'task-title-hint' : undefined} onChange={(e) => setForm({ ...form, titulo: e.target.value })} />
      {showHints && errors.titulo && <small id="task-title-hint" className="field-hint">{errors.titulo}</small>}
    </label>
    <div className="form-field"><span className="field-label">Fecha</span>
      <DatePicker ariaLabel="Fecha" value={form.fecha} disabled={disabled} describedBy={showHints && errors.fecha ? 'task-date-hint' : undefined} invalid={showHints && !!errors.fecha} onChange={(fecha) => setForm({ ...form, fecha })} />
      {showHints && errors.fecha && <small id="task-date-hint" className="field-hint">{errors.fecha}</small>}
    </div>
    <label>Puntaje total<input type="number" min="1" max="2147483647" step="1" value={form.total} required disabled={disabled} placeholder="Ej.: 10" aria-invalid={showHints && !!errors.total || undefined} aria-describedby={showHints && errors.total ? 'task-total-hint' : undefined} onChange={(e) => setForm({ ...form, total: e.target.value })} />
      {showHints && errors.total && <small id="task-total-hint" className="field-hint">{errors.total}</small>}
    </label>
    {classroomTask.url && <div className="classroom-link-row"><a href={classroomTask.url} target="_blank" rel="noopener noreferrer" aria-label="Abrir la tarea en Classroom en una pestaña nueva">Abrir la tarea en Classroom</a></div>}
    {status && <div className="notice error" role="alert">{status}</div>}
    <div className="task-form-footer">
      {showHints && <p className="task-form-guidance" role="status">{valid ? 'Todo listo para guardar.' : 'Completá los campos indicados para habilitar Guardar.'}</p>}
      <div className="toolbar"><Link className="button secondary" to={'/planilla/' + planillaId}>Cancelar</Link>{tareaId > 0 && !isReadOnly && <button className="button danger" type="button" disabled={disabled} onClick={remove}>Eliminar</button>}<button className="button" type="submit" disabled={disabled || !valid}>{busy ? 'Procesando…' : isReadOnly ? 'Solo lectura' : 'Guardar'}</button></div>
    </div>
  </form></AppShell>;
}
