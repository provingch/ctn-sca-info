import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import AppShell from '../../components/AppShell';
import ContentState from '../../components/ui/ContentState';
import { ApiError } from '../../api/client';
import { acceptQueja, getAdminQuejas, quejaEstado, rejectQueja, type QuejaAceptacion, type QuejaItem, type QuejaRechazo, type QuejaResolucion, type QuejaRevision } from '../../api/quejas';
import { getAdminCatalog } from '../../api/admin';
import { useSearchParams } from 'react-router-dom';
import { formatSqlDateTime } from '../../utils/date';
import CatalogoConductaPanel from '../../components/CatalogoConductaPanel';
import ComplaintReview from '../../components/quejas/ComplaintReview';
import ComplaintDocuments from '../../components/quejas/ComplaintDocuments';
import LauncherCards, { launcherIcons } from '../../components/LauncherCards';
import { nombreCorto } from '../../utils/nombre';

const ESTADO_LABEL: Record<ReturnType<typeof quejaEstado>, string> = {
  pendiente: 'Pendiente', aceptada: 'Aceptada', revisada: 'Revisada', resuelta: 'Resuelta', rechazada: 'Rechazada',
};

function QuejaPendienteActions({ queja, onAccepted, onRejected }: {
  queja: QuejaItem;
  onAccepted: (result: QuejaAceptacion) => void;
  onRejected: (result: QuejaRechazo) => void;
}) {
  const [rejecting, setRejecting] = useState(false);
  const [motivo, setMotivo] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const inFlight = useRef(false);

  async function accept() {
    if (inFlight.current) return;
    inFlight.current = true; setBusy(true); setError('');
    try {
      onAccepted(await acceptQueja(queja.id));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo aceptar la queja. Reintentá.');
    } finally {
      inFlight.current = false; setBusy(false);
    }
  }

  async function confirmReject(event: FormEvent) {
    event.preventDefault();
    if (inFlight.current || !motivo.trim()) return;
    inFlight.current = true; setBusy(true); setError('');
    try {
      onRejected(await rejectQueja(queja.id, motivo.trim()));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo rechazar la queja. El motivo se conservó para reintentar.');
    } finally {
      inFlight.current = false; setBusy(false);
    }
  }

  if (rejecting) return <form className="complaint-review-form form-grid" aria-label={`Rechazo de queja #${queja.id}`} onSubmit={confirmReject}>
    <label htmlFor={`motivo-rechazo-${queja.id}`}>Motivo del rechazo</label>
    <textarea id={`motivo-rechazo-${queja.id}`} autoFocus rows={3} required disabled={busy} value={motivo} onChange={(e) => setMotivo(e.target.value)} placeholder="Explicá por qué se rechaza esta queja." />
    {error && <p className="notice error" role="alert">{error}</p>}
    <div className="complaint-review-buttons">
      <button className="button secondary" type="button" disabled={busy} onClick={() => { setRejecting(false); setError(''); }}>Cancelar</button>
      <button className="button" type="submit" disabled={busy || !motivo.trim()}>{busy ? 'Rechazando…' : 'Confirmar rechazo'}</button>
    </div>
  </form>;

  return <div className="complaint-review-buttons">
    <button className="button secondary" type="button" disabled={busy} onClick={() => setRejecting(true)}>Rechazar</button>
    <button className="button" type="button" disabled={busy} onClick={() => void accept()}>{busy ? 'Aceptando…' : 'Aceptar'}</button>
    {error && <p className="notice error" role="alert">{error}</p>}
  </div>;
}

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

  function updateQueja(id: number, changes: Partial<QuejaItem>) {
    setQuejas((current) => current.map((item) => item.id === id ? { ...item, ...changes } : item));
  }

  const agrupadas = useMemo(() => {
    const map = new Map<number, { profesorNombre: string; profesorApellido?: string | null; nombreVisible: string; count: number; quejas: QuejaItem[] }>();
    quejas.forEach((q) => {
      const pid = q.profesorId;
      const cur = map.get(pid) ?? { profesorNombre: q.profesorNombre ?? `Profesor #${pid}`, profesorApellido: q.profesorApellido ?? '', nombreVisible: nombreCorto(q.profesorNombre, q.profesorApellido) || `Profesor #${pid}`, count: 0, quejas: [] };
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
      <LauncherCards className="launcher-cards-grid" options={[
        { key: 'quejas', icon: launcherIcons.quejas, title: 'Quejas por profesor', description: 'Ver y revisar quejas cargadas por la administración.', onSelect: () => changeView('quejas') },
        { key: 'conducta', icon: launcherIcons.conducta, title: 'Reportes conductuales', description: 'Crear y administrar los códigos N usados para registrar el comportamiento de los alumnos.', onSelect: () => changeView('conducta') },
      ]} />
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
                    <h2 style={{ margin: 0, fontSize: '1.05rem' }}>{g.nombreVisible}</h2>
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
          {detalleQuejas.map((q) => {
            const estado = quejaEstado(q);
            return <li key={q.id} style={{ display: 'flex', gap: 12, alignItems: 'flex-start', padding: 12, borderBottom: '1px solid var(--line)' }}>
              <div className="avatar" style={{ width: 40, height: 40, borderRadius: 999, fontSize: '0.85rem', display: 'grid', placeItems: 'center', background: 'var(--bg-soft)', color: 'var(--muted)' }}>{(q.cursoEspecialidad ?? 'C').slice(0, 1)}</div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <strong>{q.motivo}</strong>
                  <span className={`complaint-status ${estado}`} style={{ marginLeft: 'auto' }}>{ESTADO_LABEL[estado]}</span>
                  <span className="badge">{`${q.cursoEspecialidad ?? ''} ${q.cursoNivel ?? ''}° ${q.cursoSeccion ?? ''}`.trim()}</span>
                </div>
                <div style={{ marginTop: 6 }}>
                  <small style={{ color: 'var(--muted)' }}>
                    Cargada por {nombreCreador(q.creadaPor)} — {formatSqlDateTime(q.creadaEn)}
                  </small>
                </div>
                <div style={{ marginTop: 10 }}>
                  {estado === 'pendiente' && (
                    <QuejaPendienteActions
                      queja={q}
                      onAccepted={(result) => updateQueja(q.id, { ...result, estado: 'aceptada' })}
                      onRejected={(result) => updateQueja(q.id, { ...result, estado: 'rechazada' })}
                    />
                  )}
                  {estado === 'rechazada' && (
                    <div className="complaint-resolution">
                      <strong>Motivo del rechazo</strong>
                      <p>{q.motivoRechazo || 'Sin motivo registrado.'}</p>
                      <small>Rechazada el {formatSqlDateTime(q.rechazadaEn)}{q.rechazadaPor != null && <> · Por {nombreCreador(q.rechazadaPor)}</>}</small>
                    </div>
                  )}
                  {estado === 'aceptada' && (
                    <ComplaintReview queja={q} onReviewed={(revision: QuejaRevision) => updateQueja(q.id, revision)} />
                  )}
                  {(estado === 'revisada' || estado === 'resuelta') && (
                    <>
                      <ComplaintReview queja={q} reviewerName={q.revisadaPor != null ? nombreCreador(q.revisadaPor) : undefined} onReviewed={(revision: QuejaRevision) => updateQueja(q.id, revision)} />
                      <ComplaintDocuments queja={q} onResolved={(resolution: QuejaResolucion) => updateQueja(q.id, resolution)} />
                    </>
                  )}
                </div>
              </div>
            </li>;
          })}
        </ul>
      )}
    </section>
  </AppShell>;
}
