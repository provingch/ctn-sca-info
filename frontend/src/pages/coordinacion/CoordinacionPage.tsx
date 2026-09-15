import { useEffect, useMemo, useState } from 'react';
import AppShell from '../../components/AppShell';
import ContentState from '../../components/ui/ContentState';
import { getAdminQuejas, type QuejaItem } from '../../api/quejas';
import { getAdminCatalog } from '../../api/admin';
import { useSearchParams } from 'react-router-dom';
import { formatSqlDateTime } from '../../utils/date';
import CatalogoConductaPanel from '../../components/CatalogoConductaPanel';

export default function CoordinacionPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [view, setView] = useState<'menu' | 'quejas' | 'conducta' | 'detalle'>(() => searchParams.get('view') === 'quejas' ? 'quejas' : searchParams.get('view') === 'conducta' ? 'conducta' : 'menu');
  const [status, setStatus] = useState('');
  const [quejas, setQuejas] = useState<QuejaItem[]>([]);
  const [usuariosPorId, setUsuariosPorId] = useState<Map<number, string>>(new Map());
  const [selectedProfesorId, setSelectedProfesorId] = useState<number | null>(null);
  const UMBRAL = 5; // valor visual por defecto si backend no expone el umbral

  useEffect(() => {
    const nextView = searchParams.get('view');
    setView(nextView === 'quejas' || nextView === 'conducta' ? nextView : 'menu');
  }, [searchParams]);

  function changeView(nextView: 'menu' | 'quejas' | 'conducta') {
    setView(nextView);
    setSearchParams(nextView === 'menu' ? {} : { view: nextView });
  }

  useEffect(() => {
    if (view !== 'quejas') return;
    getAdminQuejas().then(setQuejas).catch((error: unknown) => setStatus(error instanceof Error ? error.message : 'No se pudieron cargar las quejas.'));
    getAdminCatalog().then((catalog) => {
      const map = new Map<number, string>();
      catalog.usuarios.forEach((u) => map.set(u.id, `${u.nombre} ${u.apellido}`.trim()));
      setUsuariosPorId(map);
    }).catch(() => { /* el nombre de quien cargó la queja queda con fallback */ });
  }, [view]);

  function nombreCreador(creadaPor: number): string {
    return usuariosPorId.get(creadaPor) ?? `Usuario #${creadaPor}`;
  }

  const agrupadas = useMemo(() => {
    const map = new Map<number, { profesorNombre: string; profesorApellido?: string | null; count: number; quejas: QuejaItem[] }>();
    quejas.forEach((q) => {
      const pid = q.profesorId;
      const cur = map.get(pid) ?? { profesorNombre: q.profesorNombre ?? `Profesor #${pid}`, profesorApellido: q.profesorApellido ?? '', count: 0, quejas: [] };
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
        <button type="button" onClick={() => changeView('quejas')}><span>01</span><h2>Quejas por profesor</h2><p>Ver y revisar quejas cargadas por la administración.</p></button>
        <button type="button" onClick={() => changeView('conducta')}><span>02</span><h2>Reportes conductuales</h2><p>Crear y administrar los códigos N usados para registrar el comportamiento de los alumnos.</p></button>
      </div>
    </AppShell>;
  }

  if (view === 'quejas') {
    return <AppShell title="Quejas por Profesor">
      <button type="button" className="button secondary" onClick={() => changeView('menu')} style={{ marginBottom: 16 }}>← Volver</button>
      {status && <div className="notice error" role="alert">{status}</div>}
      {quejas.length === 0 ? (
        <ContentState title="No hay quejas" detail="No se registraron quejas en este alcance." tone="empty" />
      ) : (
        <section>
          <div className="card-grid">
            {agrupadas.map((g) => (
              <button
                key={g.profesorId}
                type="button"
                className={`nav-card${g.count > UMBRAL ? ' flagged' : ''}`}
                onClick={() => openDetalle(g.profesorId)}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div className="avatar" style={{ width: 44, height: 44, borderRadius: 999, fontSize: '0.95rem' }}>{(g.profesorNombre || 'P').slice(0, 1)}</div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <h2 style={{ margin: 0, fontSize: '1.05rem' }}>{`${g.profesorNombre} ${g.profesorApellido ?? ''}`.trim()}</h2>
                    <p style={{ margin: '4px 0 0', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>Última: {g.quejas[0]?.motivo ?? '-'}</p>
                  </div>
                  <span
                    className="badge"
                    style={g.count > UMBRAL ? { borderColor: 'var(--danger)', background: 'color-mix(in srgb, var(--danger) 16%, var(--paper))', color: 'var(--danger)' } : undefined}
                  >
                    {g.count}
                  </span>
                </div>
              </button>
            ))}
          </div>
        </section>
      )}
    </AppShell>;
  }

  if (view === 'conducta') {
    return <AppShell title="Reportes conductuales">
      <button type="button" className="button secondary" onClick={() => changeView('menu')} style={{ marginBottom: 16 }}>← Volver</button>
      <CatalogoConductaPanel />
    </AppShell>;
  }

  return <AppShell title="Detalle de Quejas">
    <button type="button" className="button secondary" onClick={() => changeView('quejas')} style={{ marginBottom: 16 }}>← Volver</button>
    <section className="panel">
      <header className="planilla-table-heading" style={{ borderLeftColor: 'var(--accent)' }}>
        <div>
          <span>Detalle</span>
          <h2>Quejas del profesor</h2>
        </div>
        <small className="muted-copy">{detalleQuejas.length} queja(s) registrada(s)</small>
      </header>
      {detalleQuejas.length === 0 ? (
        <ContentState title="Sin quejas" detail="No se encontraron quejas para este profesor." tone="empty" />
      ) : (
        <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
          {detalleQuejas.map((q) => (
            <li key={q.id} style={{ display: 'flex', gap: 12, alignItems: 'flex-start', padding: 12, borderBottom: '1px solid var(--line)' }}>
              <div className="avatar" style={{ width: 40, height: 40, borderRadius: 999, fontSize: '0.85rem', display: 'grid', placeItems: 'center', background: 'var(--bg-soft)', color: 'var(--muted)' }}>{(q.cursoEspecialidad ?? 'C').slice(0, 1)}</div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <strong>{q.motivo}</strong>
                  <span className="badge" style={{ marginLeft: 'auto' }}>{`${q.cursoEspecialidad ?? ''} ${q.cursoNivel ?? ''}° ${q.cursoSeccion ?? ''}`.trim()}</span>
                </div>
                <div style={{ marginTop: 6 }}>
                  <small style={{ color: 'var(--muted)' }}>
                    Cargada por {nombreCreador(q.creadaPor)} — {formatSqlDateTime(q.creadaEn)}
                  </small>
                </div>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  </AppShell>;
}
