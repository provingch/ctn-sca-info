import { useEffect, useState, type FormEvent } from 'react';
import { ApiError } from '../../api/client';
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

type StatusTone = 'info' | 'success' | 'error';

export default function ReviewPlanesView() {
  const [planes, setPlanes] = useState<PlanPendiente[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedPlanId, setSelectedPlanId] = useState<number | null>(null);
  const [selectedPlan, setSelectedPlan] = useState<planCurricularApi.PlanCurricularEstado | null>(null);
  const [loadingDetalle, setLoadingDetalle] = useState(false);
  const [observaciones, setObservaciones] = useState('');
  const [status, setStatus] = useState('');
  const [statusTone, setStatusTone] = useState<StatusTone>('info');
  const [procesing, setProcessing] = useState(false);

  // Cargar lista de planes pendientes
  useEffect(() => {
    (async () => {
      try {
        setLoading(true);
        const result = await planCurricularApi.getPendientes();
        setPlanes(result);
        setStatus('');
        setStatusTone('info');
      } catch (err) {
        setStatus(err instanceof ApiError ? err.message : 'No se pudieron cargar los planes.');
        setStatusTone('error');
        setPlanes([]);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  // Cargar detalle del plan seleccionado
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
        setStatus(err instanceof ApiError ? err.message : 'No se pudo cargar el detalle del plan.');
        setStatusTone('error');
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
      setStatus(err instanceof ApiError ? err.message : 'No se pudo descargar el documento.');
      setStatusTone('error');
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
      setStatus('Plan aprobado correctamente.');
      setStatusTone('success');
      setPlanes((current) => current.filter((p) => p.id !== selectedPlanId));
      setSelectedPlanId(null);
      setSelectedPlan(null);
      setObservaciones('');
    } catch (err) {
      setStatus(err instanceof ApiError ? err.message : 'No se pudo aprobar el plan.');
      setStatusTone('error');
    } finally {
      setProcessing(false);
    }
  }

  async function handleRechazar(e: FormEvent) {
    e.preventDefault();
    if (!selectedPlanId || !observaciones.trim()) {
      setStatus('Las observaciones son requeridas para rechazar.');
      setStatusTone('error');
      return;
    }
    setProcessing(true);
    try {
      await planCurricularApi.rechazarPlan(selectedPlanId, observaciones);
      setStatus('Plan rechazado correctamente.');
      setStatusTone('success');
      setPlanes((current) => current.filter((p) => p.id !== selectedPlanId));
      setSelectedPlanId(null);
      setSelectedPlan(null);
      setObservaciones('');
    } catch (err) {
      setStatus(err instanceof ApiError ? err.message : 'No se pudo rechazar el plan.');
      setStatusTone('error');
    } finally {
      setProcessing(false);
    }
  }

  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: 20 }}>
      {/* Panel izquierdo: lista de planes */}
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
                onClick={() => {
                  setSelectedPlanId(plan.id);
                  setStatus('');
                  setStatusTone('info');
                }}
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
                <div style={{ fontSize: '0.9rem', color: 'var(--muted)', marginTop: 4 }}>
                  {plan.profesorNombre}
                </div>
                <div style={{ fontSize: '0.85rem', color: 'var(--muted)', marginTop: 2 }}>
                  {plan.cursoDescripcion}
                  {plan.especialidad && (
                    <>
                      <br />
                      <span style={{ color: 'var(--accent-deep)', fontWeight: 750 }}>{plan.especialidad}</span>
                    </>
                  )}
                  <br />
                  {new Date(plan.fechaSubida).toLocaleDateString('es-AR')}
                </div>
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Panel derecho: detalle del plan */}
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

            {/* Botón de descarga */}
            <button
              type="button"
              className="button secondary"
              onClick={handleDescargarDocumento}
              style={{ marginBottom: 12 }}
            >
              Descargar documento original
            </button>

            {/* Tabla de temas */}
            {selectedPlan.temas && selectedPlan.temas.length > 0 && (
              <div style={{ marginBottom: 16, maxHeight: 300, overflow: 'auto' }}>
                <h4 style={{ marginTop: 0 }}>Temas por mes</h4>
                <div className="table-responsive">
                  <table className="table table-striped" style={{ fontSize: '0.85rem' }}>
                    <caption className="visually-hidden">Temas del plan curricular por mes</caption>
                    <thead>
                      <tr>
                        <th style={{ width: '15%' }}>Mes</th>
                        <th style={{ width: '35%' }}>Tema/Contenido</th>
                        <th style={{ width: '25%' }}>Capacidades</th>
                        <th style={{ width: '25%' }}>Actividades</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selectedPlan.temas.map((tema, idx) => (
                        <tr key={idx}>
                          <td>{tema.mes}</td>
                          <td>{tema.temasContenidos}</td>
                          <td>{tema.capacidades || '—'}</td>
                          <td>{tema.actividades || '—'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {/* Formulario de decisión */}
            <form onSubmit={(e) => e.preventDefault()} style={{ display: 'grid', gap: 12 }}>
              <div>
                <label htmlFor="observaciones" style={{ display: 'block', marginBottom: 4 }}>
                  Observaciones
                </label>
                <textarea
                  id="observaciones"
                  value={observaciones}
                  onChange={(e) => setObservaciones(e.target.value)}
                  placeholder="Ingresá observaciones (requerido para rechazar)"
                  rows={4}
                  style={{ width: '100%', resize: 'none' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                <button
                  type="button"
                  className="button"
                  disabled={procesing}
                  onClick={handleAprobar}
                >
                  {procesing ? 'Procesando...' : 'Aprobar'}
                </button>
                <button
                  type="button"
                  className="button secondary"
                  disabled={procesing || !observaciones.trim()}
                  onClick={handleRechazar}
                >
                  {procesing ? 'Procesando...' : 'Rechazar'}
                </button>
              </div>
            </form>
          </>
        )}

        {status && (
          <div
            className={`notice ${statusTone}`}
            style={{ marginTop: 12 }}
            role={statusTone === 'error' ? 'alert' : 'status'}
          >
            {status}
          </div>
        )}
      </div>
    </div>
  );
}
