import { useEffect, useState, type FormEvent } from 'react';
import { ApiError } from '../../api/client';
import { useToast } from '../../context/ToastContext';
import * as planCurricularApi from '../../api/planCurricular';

interface PlanPendiente {
  id: number;
  estado: string;
  archivoNombre: string;
  fechaSubida: string;
  materiaNombre: string;
  profesorNombre: string;
  cursoDescripcion: string;
  especialidad?: string;
}

// StatusTone removed; toasts replace local status state

export default function ReviewPlanesView() {
  const [planes, setPlanes] = useState<PlanPendiente[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedPlanId, setSelectedPlanId] = useState<number | null>(null);
  const [selectedPlan, setSelectedPlan] = useState<any | null>(null);
  const [loadingDetalle, setLoadingDetalle] = useState(false);
  const [observaciones, setObservaciones] = useState('');
  const { showToast } = useToast();
  const [procesing, setProcessing] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        setLoading(true);
        const result = await planCurricularApi.getPendientes();
        setPlanes(result);
      } catch (err) {
        const msg = err instanceof ApiError ? err.message : 'No se pudieron cargar los planes.';
        showToast(msg, { tone: 'error' });
        setPlanes([]);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  useEffect(() => {
    if (!selectedPlanId) {
      setSelectedPlan(null);
      setObservaciones('');
      return;
    }

    setLoadingDetalle(true);
    (async () => {
      try {
        const plan = await planCurricularApi.getPlanDetalle(selectedPlanId);
        setSelectedPlan(plan);
      } catch (err) {
        const msg = err instanceof ApiError ? err.message : 'No se pudo cargar el detalle del plan.';
        showToast(msg, { tone: 'error' });
        setSelectedPlan(null);
      } finally {
        setLoadingDetalle(false);
      }
    })();
  }, [selectedPlanId]);

  async function handleDescargarDocumento(e: FormEvent) {
    e.preventDefault();
    if (!selectedPlanId) return;
    try {
      await planCurricularApi.descargarDocumentoOriginal(selectedPlanId);
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : 'No se pudo descargar el documento.';
      showToast(msg, { tone: 'error' });
    }
  }

  async function handleAprobar(e: FormEvent) {
    e.preventDefault();
    if (!selectedPlanId) return;
    const confirmed = window.confirm('¿Aprobar este plan curricular? La decisión se notificará al profesor.');
    if (!confirmed) return;
    setProcessing(true);
    try {
      await planCurricularApi.aprobarPlan(selectedPlanId);
      showToast('Plan aprobado correctamente.', { tone: 'success' });
      setPlanes((current) => current.filter((p) => p.id !== selectedPlanId));
      setSelectedPlanId(null);
      setSelectedPlan(null);
      setObservaciones('');
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : 'No se pudo aprobar el plan.';
      showToast(msg, { tone: 'error' });
    } finally {
      setProcessing(false);
    }
  }

  async function handleRechazar(e: FormEvent) {
    e.preventDefault();
    if (!selectedPlanId || !observaciones.trim()) {
      showToast('Las observaciones son requeridas para rechazar.', { tone: 'error' });
      return;
    }
    setProcessing(true);
    try {
      await planCurricularApi.rechazarPlan(selectedPlanId, observaciones);
      showToast('Plan rechazado correctamente.', { tone: 'success' });
      setPlanes((current) => current.filter((p) => p.id !== selectedPlanId));
      setSelectedPlanId(null);
      setSelectedPlan(null);
      setObservaciones('');
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : 'No se pudo rechazar el plan.';
      showToast(msg, { tone: 'error' });
    } finally {
      setProcessing(false);
    }
  }

  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: 20 }}>
      <div className="panel">
        <h3>Planes pendientes de revisión ({planes.length})</h3>
        {loading ? (
          <p>Cargando planes...</p>
        ) : planes.length === 0 ? (
          <p style={{ color: 'var(--muted)' }}>No hay planes pendientes de revisión.</p>
        ) : (
          <div style={{ display: 'grid', gap: 8 }}>
            {planes.map((plan: any) => (
              <button
                key={plan.id}
                type="button"
                onClick={() => setSelectedPlanId(plan.id)}
                style={{
                  padding: 12,
                  background: selectedPlanId === plan.id ? 'var(--accent-strong)' : 'var(--paper-raised)',
                  border: selectedPlanId === plan.id ? '2px solid var(--accent)' : '1px solid var(--line)',
                  borderRadius: 4,
                  cursor: 'pointer',
                  textAlign: 'left',
                  color: 'var(--ink)',
                  transition: 'all 0.2s',
                }}
              >
                <strong>{plan.materiaNombre}</strong>
                <div style={{ fontSize: '0.9rem', color: 'var(--muted)', marginTop: 4 }}>{plan.profesorNombre}</div>
                <div style={{ fontSize: '0.85rem', color: 'var(--muted)', marginTop: 2 }}>{plan.cursoDescripcion}<br />{plan.especialidad && (<><br /><span style={{ color: 'var(--accent-deep)', fontWeight: 750 }}>{plan.especialidad}</span></>) }<br />{new Date(plan.fechaSubida).toLocaleDateString('es-AR')}</div>
              </button>
            ))}
          </div>
        )}
      </div>

      <div className="panel">
        {!selectedPlanId ? (
          <p style={{ textAlign: 'center', color: 'var(--muted)' }}>Seleccioná un plan para verlo en detalle.</p>
        ) : loadingDetalle ? (
          <p>Cargando detalle...</p>
        ) : !selectedPlan ? (
          <p style={{ color: 'var(--danger)' }}>No se pudo cargar el plan.</p>
        ) : (
          <>
            <h3>Detalle del plan curricular</h3>
            <button type="button" className="button secondary" onClick={handleDescargarDocumento} style={{ marginBottom: 12 }}>Descargar documento original</button>
            {selectedPlan.temas && selectedPlan.temas.length > 0 && (
              <div style={{ marginBottom: 16, maxHeight: 300, overflow: 'auto' }}>
                <h4 style={{ marginTop: 0 }}>Temas por mes</h4>
                <div className="table-responsive">
                  <table className="table table-striped" style={{ fontSize: '0.85rem' }}>
                    <caption className="visually-hidden">Temas del plan curricular por mes</caption>
                    <thead><tr><th>Mes</th><th>Tema/Contenido</th><th>Capacidades</th><th>Actividades</th></tr></thead>
                    <tbody>{selectedPlan.temas.map((tema: any, idx: number) => <tr key={idx}><td>{tema.mes}</td><td>{tema.temasContenidos}</td><td>{tema.capacidades || '—'}</td><td>{tema.actividades || '—'}</td></tr>)}</tbody>
                  </table>
                </div>
              </div>
            )}

            <form onSubmit={(e) => e.preventDefault()} style={{ display: 'grid', gap: 12 }}>
              <div>
                <label htmlFor="observaciones" style={{ display: 'block', marginBottom: 4 }}>Observaciones</label>
                <textarea id="observaciones" value={observaciones} onChange={(e) => setObservaciones(e.target.value)} placeholder="Ingresá observaciones (requerido para rechazar)" rows={4} style={{ width: '100%', resize: 'none' }} />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                <button type="button" className="button" disabled={procesing} onClick={handleAprobar}>{procesing ? 'Procesando...' : 'Aprobar'}</button>
                <button type="button" className="button secondary" disabled={procesing || !observaciones.trim()} onClick={handleRechazar}>{procesing ? 'Procesando...' : 'Rechazar'}</button>
              </div>
            </form>
          </>
        )}
      </div>
    </div>
  );
}
