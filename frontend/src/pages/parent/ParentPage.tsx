import { useEffect, useState } from 'react';
import AppShell from '../../components/AppShell';
import { getParentSummary, type ParentResponse, type ParentTaskStatus } from '../../api/parent';
import { ApiError } from '../../api/client';
import { normalizeSpecialty } from '../../theme/theme';

export default function ParentPage() {
  const [data, setData] = useState<ParentResponse | null>(null);
  const [error, setError] = useState('');

  function load(alumnoId?: number) {
    getParentSummary(alumnoId)
      .then(setData)
      .catch((e) => setError(e instanceof ApiError ? e.message : 'No se pudo cargar el resumen.'));
  }

  useEffect(() => load(), []);

  if (!data) {
    return <AppShell title="Notas de mis hijos"><section className="panel">{error || 'Cargando…'}</section></AppShell>;
  }

  const selectedChild = data.hijos.find((child) => child.id === data.selectedAlumnoId);

  return (
    <AppShell title="Notas de mis hijos" specialty={selectedChild?.especialidad}>
      <div className="card-grid child-grid">
        {data.hijos.map((child) => (
          <button
            key={child.id}
            data-specialty={normalizeSpecialty(child.especialidad)}
            className={`nav-card child-card ${data.selectedAlumnoId === child.id ? 'selected' : ''}`}
            onClick={() => load(child.id)}
          >
            <span>{child.especialidad}</span>
            <h2>{child.apellido}, {child.nombre}</h2>
            <strong>Promedio general: {child.promedio}%</strong>
          </button>
        ))}
      </div>
      <div className="subject-list">
        {data.materias.map((materia) => (
          <details className="panel" key={materia.planillaId}>
            <summary>
              <div><h2>{materia.materia}</h2><span>{materia.puntos} / {materia.total} puntos</span></div>
              <div className="grade-bubble">{materia.nota}<small>{materia.porcentaje}%</small></div>
            </summary>
            <div className="task-list">
              {materia.tareas.map((tarea) => (
                <div className="history-row" key={tarea.id}>
                  <span><strong>{tarea.titulo}</strong><small>{tarea.fecha}</small></span>
                  <TaskResult estado={tarea.estado} puntos={tarea.puntos} total={tarea.total} />
                </div>
              ))}
            </div>
          </details>
        ))}
        {data.materias.length === 0 && (
          <section className="panel empty-state">
            <h2>Sin calificaciones publicadas</h2>
            <p>Todavía no hay planillas disponibles para este alumno.</p>
          </section>
        )}
      </div>
    </AppShell>
  );
}

function TaskResult({ estado, puntos, total }: { estado: ParentTaskStatus; puntos: number | null; total: number }) {
  if (estado === 'CALIFICADA') {
    return <div className="parent-task-result graded"><strong>{puntos ?? 0} / {total}</strong><small>Calificada</small></div>;
  }

  const labels: Record<Exclude<ParentTaskStatus, 'CALIFICADA'>, string> = {
    ENTREGADA_PENDIENTE: 'Entregada · pendiente',
    NO_ENTREGADA: 'No entregada',
    PENDIENTE: 'Pendiente',
  };

  return <div className="parent-task-result"><span className={`parent-task-status ${estado.toLowerCase().replace('_', '-')}`}>{labels[estado]}</span><small>de {total} puntos</small></div>;
}
