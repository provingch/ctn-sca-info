import { useEffect, useMemo, useState } from 'react';
import AppShell from '../../components/AppShell';
import ContentState from '../../components/ui/ContentState';
import { getAdminQuejas, type QuejaItem } from '../../api/quejas';

export default function CoordinacionPage() {
  const [view, setView] = useState<'menu' | 'quejas' | 'detalle'>('menu');
  const [status, setStatus] = useState('');
  const [quejas, setQuejas] = useState<QuejaItem[]>([]);
  const [selectedProfesorId, setSelectedProfesorId] = useState<number | null>(null);
  const UMBRAL = 5; // valor visual por defecto si backend no expone el umbral

  useEffect(() => {
    if (view !== 'quejas') return;
    getAdminQuejas().then(setQuejas).catch((err) => setStatus((err as any)?.message || 'No se pudieron cargar las quejas.'));
  }, [view]);

  const agrupadas = useMemo(() => {
    const map = new Map<number, { profesorNombre: string; count: number; quejas: QuejaItem[] }>();
    quejas.forEach((q) => {
      const pid = q.profesorId;
      const cur = map.get(pid) ?? { profesorNombre: q.profesorNombre ?? `Profesor #${pid}`, count: 0, quejas: [] };
      cur.count += 1;
      cur.quejas.push(q);
      map.set(pid, cur);
    });
    return Array.from(map.entries()).map(([profesorId, data]) => ({ profesorId, ...data })).sort((a, b) => b.count - a.count);
  }, [quejas]);

  function openDetalle(profesorId: number) {
    setSelectedProfesorId(profesorId);
    setView('detalle');
  }

  const detalleQuejas = selectedProfesorId ? agrupadas.find((g) => g.profesorId === selectedProfesorId)?.quejas ?? [] : [];

  if (view === 'menu') {
    return <AppShell title="Coordinación Pedagógica">
      <div className="choice-grid">
        <button type="button" onClick={() => setView('quejas')}><span>01</span><h2>Quejas por profesor</h2><p>Ver y revisar quejas cargadas por la administración.</p></button>
      </div>
    </AppShell>;
  }

  if (view === 'quejas') {
    return <AppShell title="Quejas por Profesor">
      <button type="button" className="button secondary" onClick={() => setView('menu')} style={{ marginBottom: 16 }}>← Volver</button>
      {status && <div className="notice error" role="alert">{status}</div>}
      {quejas.length === 0 ? (
        <ContentState title="No hay quejas" detail="No se registraron quejas en este alcance." tone="empty" />
      ) : (
        <section>
          <div className="card-grid">
            {agrupadas.map((g) => (
              <article key={g.profesorId} className={`profesor-queue-card${g.count > UMBRAL ? ' flagged' : ''}`}>
                <header>
                  <h3>{g.profesorNombre}</h3>
                  <div className="badge">{g.count}</div>
                </header>
                <p>Última: {g.quejas[0]?.motivo ?? '-'}</p>
                <button className="button" type="button" onClick={() => openDetalle(g.profesorId)}>Ver detalle</button>
              </article>
            ))}
          </div>
        </section>
      )}
    </AppShell>;
  }

  return <AppShell title="Detalle de Quejas">
    <button type="button" className="button secondary" onClick={() => setView('quejas')} style={{ marginBottom: 16 }}>← Volver</button>
    <h2>Quejas del profesor</h2>
    {detalleQuejas.length === 0 ? <ContentState title="Sin quejas" detail="No se encontraron quejas para este profesor." tone="empty" /> : (
      <div className="panel">
        <ul className="list">
          {detalleQuejas.map((q) => (
            <li key={q.id} className={`list-item${q.motivo ? '' : ' muted'}`}>
              <p><strong>Motivo:</strong> {q.motivo}</p>
              <p><strong>Curso:</strong> {q.cursoDescripcion ?? `#${q.cursoId}`} — <strong>Especialidad:</strong> {q.especialidadNombre ?? `#${q.especialidadId}`}</p>
              <p><small>Cargada por: {q.creadoPor ?? 'Sistema'} — {q.fecha}</small></p>
            </li>
          ))}
        </ul>
      </div>
    )}
  </AppShell>;
}
