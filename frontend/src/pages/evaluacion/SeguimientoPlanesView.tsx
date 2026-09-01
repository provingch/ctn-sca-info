import { useEffect, useState } from 'react';
import { ApiError } from '../../api/client';
import { useToast } from '../../context/ToastContext';
import * as evaluacionApi from '../../api/evaluacion';
import * as planCurricularApi from '../../api/planCurricular';

type Tab = 'planes' | 'incumplimientos';
// Local status/state removed; toasts are used instead

function formatDate(value?: string): string {
  if (!value) return 'Pendiente';
  const date = new Date(value.replace(' ', 'T'));
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('es-PY', { dateStyle: 'short', timeStyle: 'short' });
}

function professorName(item: evaluacionApi.IncumplimientoPendiente): string {
  return [item.usuarioApellido, item.usuarioNombre].filter(Boolean).join(' ') || `Profesor #${item.usuarioId}`;
}

function messageFor(error: unknown, fallback: string): string {
  if (!(error instanceof ApiError)) return fallback;
  if (error.body && typeof error.body === 'object') {
    const detail = (error.body as { detail?: unknown; message?: unknown }).detail
      ?? (error.body as { message?: unknown }).message;
    if (typeof detail === 'string' && detail.trim()) return detail;
  }
  return error.message;
}

export default function SeguimientoPlanesView({ initialTab = 'planes' }: { initialTab?: Tab }) {
  const [tab, setTab] = useState<Tab>(initialTab);
  const [planes, setPlanes] = useState<planCurricularApi.PlanPendienteResumen[]>([]);
  const [selectedPlanId, setSelectedPlanId] = useState<number | null>(null);
  const [selectedPlan, setSelectedPlan] = useState<planCurricularApi.PlanCurricularEstado | null>(null);
  const [incumplimientos, setIncumplimientos] = useState<evaluacionApi.IncumplimientoPendiente[]>([]);
  const [selectedIncumplimientoId, setSelectedIncumplimientoId] = useState<number | null>(null);
  const [estadoResolucion, setEstadoResolucion] = useState<'PERMITIDO' | 'RECHAZADO'>('PERMITIDO');
  const [suspensionDesde, setSuspensionDesde] = useState('');
  const [suspensionHasta, setSuspensionHasta] = useState('');
  const [loading, setLoading] = useState(true);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [resolving, setResolving] = useState(false);
  // removed unused local status state
  const { showToast } = useToast();

  useEffect(() => {
    setTab(initialTab);
  }, [initialTab]);

  useEffect(() => {
    Promise.all([planCurricularApi.getAprobados(), evaluacionApi.getIncumplimientos()])
      .then(([approvedPlans, pendingIncumplimientos]) => {
        setPlanes(approvedPlans);
        setIncumplimientos(pendingIncumplimientos);
      })
      .catch((error) => {
        const msg = messageFor(error, 'No se pudo cargar el seguimiento de profesores.');
        showToast(msg, { tone: 'error' });
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (selectedPlanId == null) {
      setSelectedPlan(null);
      return;
    }
    setLoadingDetail(true);
    planCurricularApi.getPlanDetalle(selectedPlanId)
      .then(setSelectedPlan)
      .catch((error) => {
        setSelectedPlan(null);
        const msg = messageFor(error, 'No se pudo cargar el detalle del plan.');
        showToast(msg, { tone: 'error' });
      })
      .finally(() => setLoadingDetail(false));
  }, [selectedPlanId]);

  function selectIncumplimiento(id: number) {
    setSelectedIncumplimientoId(id);
    setEstadoResolucion('PERMITIDO');
    setSuspensionDesde('');
    setSuspensionHasta('');
  }

  async function handleResolver() {
    if (selectedIncumplimientoId == null) return;
    setResolving(true);
    try {
      await evaluacionApi.resolverIncumplimiento(selectedIncumplimientoId, {
        estado: estadoResolucion,
        ...(estadoResolucion === 'RECHAZADO' ? { suspensionDesde, suspensionHasta } : {}),
      });
      setIncumplimientos((items) => items.filter((item) => item.id !== selectedIncumplimientoId));
      setSelectedIncumplimientoId(null);
      showToast('Incumplimiento resuelto y notificado al profesor.', { tone: 'success', autoDismiss: true });
    } catch (error) {
      const msg = messageFor(error, 'No se pudo resolver el incumplimiento.');
      showToast(msg, { tone: 'error' });
    } finally {
      setResolving(false);
    }
  }

  const selectedIncumplimiento = incumplimientos.find((item) => item.id === selectedIncumplimientoId);

  return <>
    <div className="tabs" style={{ marginBottom: 16 }}>
      <button type="button" className={tab === 'planes' ? 'active' : ''} onClick={() => setTab('planes')}>Seguimiento de planes</button>
      <button type="button" className={tab === 'incumplimientos' ? 'active' : ''} onClick={() => setTab('incumplimientos')}>Incumplimientos ({incumplimientos.length})</button>
    </div>

    {tab === 'planes' && <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: 20 }}>
      <div className="panel">
        <h3>Planes aprobados ({planes.length})</h3>
        {loading ? <p>Cargando planes...</p> : planes.length === 0 ? <p style={{ color: 'var(--muted)' }}>No hay planes aprobados para seguimiento.</p> : <div style={{ display: 'grid', gap: 8 }}>
          {planes.map((plan) => <button key={plan.id} type="button" onClick={() => { setSelectedPlanId(plan.id); }} style={{ padding: 12, background: selectedPlanId === plan.id ? 'var(--accent-strong)' : 'var(--paper-raised)', border: selectedPlanId === plan.id ? '2px solid var(--accent)' : '1px solid var(--line)', borderRadius: 4, cursor: 'pointer', textAlign: 'left', color: 'var(--ink)' }}>
            <strong>{plan.materiaNombre}</strong>
            <div style={{ fontSize: '0.9rem', color: 'var(--muted)', marginTop: 4 }}>{plan.profesorNombre}</div>
            <div style={{ fontSize: '0.85rem', color: 'var(--muted)', marginTop: 2 }}>{plan.cursoDescripcion}<br />Aprobado: {formatDate(plan.fechaRevision)}</div>
          </button>)}
        </div>}
      </div>
      <div className="panel">
        {!selectedPlanId ? <p style={{ textAlign: 'center', color: 'var(--muted)' }}>Seleccioná un plan para consultar su cumplimiento.</p> : loadingDetail ? <p>Cargando detalle...</p> : !selectedPlan ? <p style={{ color: 'var(--danger)' }}>No se pudo cargar el plan.</p> : <>
          <h3>Temas y cumplimiento</h3>
          <p className="lead">{selectedPlan.profesorNombre} · {selectedPlan.materiaNombre}</p>
          <div className="table-responsive">
            <table className="table table-striped" style={{ fontSize: '0.85rem' }}>
              <caption className="visually-hidden">Cumplimiento de temas del plan curricular</caption>
              <thead><tr><th>Mes</th><th>Tema/Contenido</th><th>Cumplido</th></tr></thead>
              <tbody>{selectedPlan.temas?.map((tema, index) => <tr key={`${tema.ordenMes}-${index}`}><td>{tema.mes}</td><td>{tema.temasContenidos}</td><td>{tema.estadoCobertura === 'CUBIERTO' ? formatDate(tema.fechaCobertura) : 'Pendiente'}</td></tr>)}</tbody>
            </table>
          </div>
        </>}
      </div>
    </div>}

    {tab === 'incumplimientos' && <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
      <div className="panel">
        <h3>Casos pendientes ({incumplimientos.length})</h3>
        {loading ? <p>Cargando incumplimientos...</p> : incumplimientos.length === 0 ? <p style={{ color: 'var(--muted)' }}>No hay incumplimientos pendientes de resolución.</p> : <div style={{ display: 'grid', gap: 8 }}>
          {incumplimientos.map((item) => <button key={item.id} type="button" onClick={() => selectIncumplimiento(item.id)} style={{ padding: 12, background: selectedIncumplimientoId === item.id ? 'var(--accent-strong)' : 'var(--paper-raised)', border: selectedIncumplimientoId === item.id ? '2px solid var(--accent)' : '1px solid var(--line)', borderRadius: 4, cursor: 'pointer', textAlign: 'left', color: 'var(--ink)' }}>
            <strong>{professorName(item)}</strong>
            <div style={{ fontSize: '0.9rem', color: 'var(--muted)', marginTop: 4 }}>{item.descripcion}</div>
            <div style={{ fontSize: '0.8rem', color: 'var(--muted)', marginTop: 4 }}>{item.tipo} · {formatDate(item.fechaCreacion)}</div>
          </button>)}
        </div>}
      </div>
      <div className="panel">
        {!selectedIncumplimiento ? <p style={{ textAlign: 'center', color: 'var(--muted)' }}>Seleccioná un incumplimiento para resolverlo.</p> : <>
          <h3>Resolver incumplimiento</h3>
          <p><strong>{professorName(selectedIncumplimiento)}</strong></p>
          <p className="lead">{selectedIncumplimiento.descripcion}</p>
          <div className="form-grid">
            <label>Resolución
              <select value={estadoResolucion} onChange={(event) => setEstadoResolucion(event.target.value as 'PERMITIDO' | 'RECHAZADO')}>
                <option value="PERMITIDO">Permitido</option>
                <option value="RECHAZADO">Rechazado</option>
              </select>
            </label>
            {estadoResolucion === 'RECHAZADO' && <>
              <label>Suspensión desde<input type="datetime-local" value={suspensionDesde} onChange={(event) => setSuspensionDesde(event.target.value)} /></label>
              <label>Suspensión hasta<input type="datetime-local" value={suspensionHasta} onChange={(event) => setSuspensionHasta(event.target.value)} /></label>
            </>}
            <button type="button" className="button" disabled={resolving} onClick={() => void handleResolver()}>{resolving ? 'Resolviendo...' : 'Resolver incumplimiento'}</button>
          </div>
        </>}
      </div>
    </div>}

    {/* status messages moved to global toasts */}
  </>;
}
