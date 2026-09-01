import { useEffect, useState } from 'react';
import { ApiError } from '../../api/client';
import { useToast } from '../../context/toast';
import * as planCurricularApi from '../../api/planCurricular';
import { formatSqlDateTime } from '../../utils/date';

// StatusTone removed; toasts replace local status state

export default function ReviewPlanesView() {
  const [planes, setPlanes] = useState<planCurricularApi.PlanPendienteResumen[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedPlanId, setSelectedPlanId] = useState<number | null>(null);
  const [selectedPlan, setSelectedPlan] = useState<planCurricularApi.PlanCurricularEstado | null>(null);
  const [loadingDetalle, setLoadingDetalle] = useState(false);
  const [observaciones, setObservaciones] = useState('');
  const { showToast } = useToast();
  const [procesing, setProcessing] = useState(false);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        setLoading(true);
        const result = await planCurricularApi.getPendientes();
        if (active) setPlanes(result);
      } catch (err) {
        const msg = err instanceof ApiError ? err.message : 'No se pudieron cargar los planes.';
        if (active) {
          showToast(msg, { tone: 'error' });
          setPlanes([]);
        }
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, [showToast]);

  useEffect(() => {
    if (!selectedPlanId) {
      setSelectedPlan(null);
      setObservaciones('');
      return;
    }

    let active = true;
    setLoadingDetalle(true);
    (async () => {
      try {
        const plan = await planCurricularApi.getPlanDetalle(selectedPlanId);
        if (active) setSelectedPlan(plan);
      } catch (err) {
        const msg = err instanceof ApiError ? err.message : 'No se pudo cargar el detalle del plan.';
        if (active) {
          showToast(msg, { tone: 'error' });
          setSelectedPlan(null);
        }
      } finally {
        if (active) setLoadingDetalle(false);
      }
    })();
    return () => { active = false; };
  }, [selectedPlanId, showToast]);

  async function handleDescargarDocumento() {
    if (!selectedPlanId) return;
    try {
      await planCurricularApi.descargarDocumentoOriginal(selectedPlanId);
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : 'No se pudo descargar el documento.';
      showToast(msg, { tone: 'error' });
    }
  }

  async function handleAprobar() {
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

  async function handleRechazar() {
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
    <div className="evaluation-split-layout">
      <div className="panel">
        <h3>Planes pendientes de revisión ({planes.length})</h3>
        {loading ? (
          <p>Cargando planes...</p>
        ) : planes.length === 0 ? (
          <p style={{ color: 'var(--muted)' }}>No hay planes pendientes de revisión.</p>
        ) : (
          <div style={{ display: 'grid', gap: 8 }}>
            {planes.map((plan) => (
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
                <div style={{ fontSize: '0.85rem', color: 'var(--muted)', marginTop: 2 }}>{plan.cursoDescripcion}<br />{plan.especialidad && (<><span style={{ color: 'var(--accent-deep)', fontWeight: 750 }}>{plan.especialidad}</span><br /></>)}{formatSqlDateTime(plan.fechaSubida, { dateStyle: 'short' })}</div>
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
                    <tbody>{selectedPlan.temas.map((tema, index) => <tr key={`${tema.ordenMes}-${index}`}><td>{tema.mes}</td><td>{tema.temasContenidos}</td><td>{tema.capacidades || '—'}</td><td>{tema.actividades || '—'}</td></tr>)}</tbody>
                  </table>
                </div>
              </div>
            )}

            <form onSubmit={(e) => e.preventDefault()} style={{ display: 'grid', gap: 12 }}>
              <div>
                <label htmlFor="observaciones" style={{ display: 'block', marginBottom: 4 }}>Observaciones</label>
                <textarea id="observaciones" value={observaciones} onChange={(e) => setObservaciones(e.target.value)} placeholder="Ingresá observaciones (requerido para rechazar)" rows={4} style={{ width: '100%', resize: 'none' }} />
              </div>

              <div className="decision-actions">
                <button type="button" className="button" disabled={procesing} onClick={() => void handleAprobar()}>{procesing ? 'Procesando...' : 'Aprobar'}</button>
                <button type="button" className="button secondary" disabled={procesing || !observaciones.trim()} onClick={() => void handleRechazar()}>{procesing ? 'Procesando...' : 'Rechazar'}</button>
              </div>
            </form>
          </>
        )}
      </div>
    </div>
  );
}
