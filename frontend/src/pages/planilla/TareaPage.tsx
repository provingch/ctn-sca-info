import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import AppShell from '../../components/AppShell';
import { createTarea, deleteTarea, getInstrumentos, getTarea, updateTarea, type Instrumento } from '../../api/academics';
import { ApiError } from '../../api/client';
import AnimatedSelect from '../../components/AnimatedSelect';
import ClassroomBadge from '../../components/ClassroomBadge';

export default function TareaPage() {
  const planillaId = Number(useParams().planillaId); const tareaId = Number(useParams().tareaId || 0);
  const navigate = useNavigate();
  const [instrumentos, setInstrumentos] = useState<Instrumento[]>([]);
  const [form, setForm] = useState({ instrumentoId: 0, fecha: new Date().toISOString().slice(0, 10), total: 10, titulo: '' });
  const [status, setStatus] = useState(''); const [busy, setBusy] = useState(false);
  const [classroomTask, setClassroomTask] = useState<{ id: string | null; url: string | null }>({ id: null, url: null });
  const isReadOnly = Boolean(classroomTask.id);

  useEffect(() => {
    getInstrumentos().then(setInstrumentos).catch(() => setStatus('No se cargaron los instrumentos.'));
    if (tareaId) {
      getTarea(tareaId).then((t) => {
        setForm({ instrumentoId: t.instrumentoId, fecha: t.fecha, total: t.total, titulo: t.titulo });
        setClassroomTask({ id: t.googleCourseworkId, url: t.googleCourseworkUrl });
      }).catch(() => setStatus('No se pudo cargar la tarea.'));
    }
  }, [tareaId]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (isReadOnly) {
      setStatus('Esta tarea viene de Google Classroom y no puede editarse desde esta planilla.');
      return;
    }
    setBusy(true); setStatus('');
    try { if (tareaId) await updateTarea(tareaId, form); else await createTarea(planillaId, form); navigate(`/planilla/${planillaId}`); } catch (err) { setStatus(err instanceof ApiError ? err.message : 'No se pudo guardar.'); } finally { setBusy(false); }
  }

  async function remove() {
    if (isReadOnly) {
      setStatus('Esta tarea viene de Google Classroom y no puede eliminarse desde esta planilla.');
      return;
    }
    if (!tareaId || !window.confirm('¿Eliminar esta tarea y sus datos asociados?')) return;
    setBusy(true); try { await deleteTarea(tareaId); navigate(`/planilla/${planillaId}`); } catch (err) { setStatus(err instanceof ApiError ? err.message : 'No se pudo eliminar.'); setBusy(false); }
  }

  return <AppShell title={tareaId ? 'Modificar tarea' : 'Agregar tarea'}><form className="panel form-grid" onSubmit={submit}>
    {isReadOnly && <div className="notice info"><ClassroomBadge /> Esta tarea se gestiona en Google Classroom y no puede modificarse desde la planilla.</div>}
    {classroomTask.url && !isReadOnly && null}
    <label>Instrumento<AnimatedSelect ariaLabel="Instrumento" value={form.instrumentoId || ''} required placeholder="Seleccione…" disabled={isReadOnly} onChange={(value) => setForm({ ...form, instrumentoId: Number(value) })} options={instrumentos.map((item) => ({ value: item.id, label: item.nombre }))} /></label>
    <label>Título<input value={form.titulo} required disabled={isReadOnly} onChange={(e) => setForm({ ...form, titulo: e.target.value })} /></label>
    <label>Fecha<input type="date" value={form.fecha} required disabled={isReadOnly} onChange={(e) => setForm({ ...form, fecha: e.target.value })} /></label>
    <label>Puntaje total<input type="number" min="1" value={form.total} required disabled={isReadOnly} onChange={(e) => setForm({ ...form, total: Number(e.target.value) })} /></label>
    {classroomTask.url && (
      <div className="classroom-link-row">
        <a href={classroomTask.url} target="_blank" rel="noopener noreferrer">Abrir la tarea en Classroom</a>
      </div>
    )}
    {status && <div className="notice error">{status}</div>}<div className="toolbar"><Link className="button secondary" to={`/planilla/${planillaId}`}>Cancelar</Link>{tareaId > 0 && !isReadOnly && <button className="button danger" type="button" onClick={remove}>Eliminar</button>}<button className="button" disabled={busy || isReadOnly}>{busy ? 'Guardando…' : isReadOnly ? 'Solo lectura' : 'Guardar'}</button></div>
  </form></AppShell>;
}
