import { useEffect, useState } from 'react';
import AppShell from '../../components/AppShell';
import { getParentSummary, type ParentResponse } from '../../api/parent';
import { ApiError } from '../../api/client';

export default function ParentPage() {
  const [data, setData] = useState<ParentResponse | null>(null); const [error, setError] = useState('');
  function load(alumnoId?: number) { getParentSummary(alumnoId).then(setData).catch((e) => setError(e instanceof ApiError ? e.message : 'No se pudo cargar el resumen.')); }
  useEffect(() => load(), []);
  if (!data) return <AppShell title="Notas de mis hijos"><section className="panel">{error || 'Cargando…'}</section></AppShell>;
  return <AppShell title="Notas de mis hijos"><div className="card-grid child-grid">{data.hijos.map((h) => <button key={h.id} className={`nav-card child-card ${data.selectedAlumnoId === h.id ? 'selected' : ''}`} onClick={() => load(h.id)}><span>{h.especialidad}</span><h2>{h.apellido}, {h.nombre}</h2><strong>Promedio general: {h.promedio}%</strong></button>)}</div><div className="subject-list">{data.materias.map((m) => <details className="panel" key={m.planillaId}><summary><div><h2>{m.materia}</h2><span>{m.puntos} / {m.total} puntos</span></div><div className="grade-bubble">{m.nota}<small>{m.porcentaje}%</small></div></summary><div className="task-list">{m.tareas.map((t) => <div className="history-row" key={t.id}><span><strong>{t.titulo}</strong><small>{t.fecha}</small></span><strong>{t.puntos} / {t.total}</strong></div>)}</div></details>)}{data.materias.length === 0 && <section className="panel empty-state"><h2>Sin calificaciones publicadas</h2><p>Todavía no hay planillas disponibles para este alumno.</p></section>}</div></AppShell>;
}
