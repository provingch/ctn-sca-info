import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { ApiError } from '../../api/client';
import AnimatedSelect from '../../components/AnimatedSelect';
import * as planCurricularApi from '../../api/planCurricular';
import type { HomeResponse } from '../../api/home';

interface AsignacionOption {
  id: number;
  materiaId: number;
  materiaNombre?: string;
  estadoPlan?: 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'NO_CARGADO';
}

function RefreshIcon() {
  return (
    <svg aria-hidden="true" viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20 6v5h-5" />
      <path d="M4 18v-5h5" />
      <path d="M6.1 9a7 7 0 0 1 11.6-2.6L20 9" />
      <path d="m4 15 2.3 2.6A7 7 0 0 0 17.9 15" />
    </svg>
  );
}

function UploadedFile({ name }: { name?: string }) {
  return (
    <p style={{ margin: '8px 0 0', color: 'var(--muted)', fontSize: '0.9rem' }}>
      Archivo subido: <strong style={{ color: 'var(--ink)' }}>{name || 'Nombre no disponible'}</strong>
    </p>
  );
}

export default function PlanCurricularView({ data, reload }: { data: HomeResponse; reload: () => Promise<void> }) {
  const [asignacionesDisponibles, setAsignacionesDisponibles] = useState<AsignacionOption[]>([]);
  const [selectedAsignacionId, setSelectedAsignacionId] = useState<number | null>(null);
  const [plan, setPlan] = useState<planCurricularApi.PlanCurricularEstado | null>(null);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [status, setStatus] = useState('');
  const [planError, setPlanError] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isReplacingPending, setIsReplacingPending] = useState(false);

  // Cargar asignaciones disponibles cuando cambia el curso
  useEffect(() => {
    if (!data.selCurso) {
      setAsignacionesDisponibles([]);
      setSelectedAsignacionId(null);
      setPlan(null);
      setPlanError('');
      setIsReplacingPending(false);
      return;
    }

    (async () => {
      try {
        const list = await planCurricularApi.getAsignacionesDisponibles(data.selCurso!.id);
        setAsignacionesDisponibles(list);
        if (list.length === 1) {
          setSelectedAsignacionId(list[0].id);
        } else {
          setSelectedAsignacionId(null);
          setPlan(null);
          setPlanError('');
          setIsReplacingPending(false);
        }
      } catch {
        setAsignacionesDisponibles([]);
        setSelectedAsignacionId(null);
        setPlan(null);
        setPlanError('');
        setIsReplacingPending(false);
      }
    })();
  }, [data.selCurso]);

  const loadCurrentPlan = useCallback(async () => {
    if (!selectedAsignacionId || !data.selCurso) {
      setPlan(null);
      setPlanError('');
      return;
    }

    setLoading(true);
    setPlanError('');
    try {
      const currentYear = new Date().getFullYear();
      const transition = new Date(currentYear, 6, 15); // 15 de julio
      const currentEtapa = new Date() < transition ? '1' : '2';

      const result = await planCurricularApi.getMiPlan(selectedAsignacionId, currentEtapa, currentYear);
      setPlan(result ?? null);
      setIsReplacingPending(false);
    } catch (err) {
      if (err instanceof ApiError && err.status === 204) {
        setPlan(null);
      } else {
        setPlanError(err instanceof ApiError ? err.message : 'No se pudo cargar el estado del plan.');
      }
    } finally {
      setLoading(false);
    }
  }, [selectedAsignacionId, data.selCurso]);

  // Cargar plan cuando cambia la asignación seleccionada
  useEffect(() => {
    void loadCurrentPlan();
  }, [loadCurrentPlan]);

  async function handleDownloadPlantilla(e: FormEvent) {
    e.preventDefault();
    if (!selectedAsignacionId) return;
    try {
      setStatus('Descargando plantilla...');
      await planCurricularApi.downloadPlantilla(selectedAsignacionId);
      setStatus('');
    } catch (err) {
      setStatus(err instanceof ApiError ? err.message : 'No se pudo descargar la plantilla.');
    }
  }

  async function handleUploadPlan(e: FormEvent) {
    e.preventDefault();
    if (!selectedFile || !selectedAsignacionId) return;

    setUploading(true);
    try {
      setStatus('Subiendo plan curricular...');
      const result = await planCurricularApi.uploadPlanCurricular(selectedAsignacionId, selectedFile);
      setStatus('Plan curricular subido correctamente.');
      const uploadedFilename = selectedFile.name;
      setSelectedFile(null);
      setPlan({ id: result.id, estado: 'PENDIENTE', archivoNombre: uploadedFilename, fechaSubida: new Date().toISOString() });
      setPlanError('');
      setIsReplacingPending(false);
      await reload();
    } catch (err) {
      setStatus(err instanceof ApiError ? err.message : 'No se pudo subir el plan curricular.');
    } finally {
      setUploading(false);
    }
  }

  return (
    <div className="two-column">
      <div className="panel" style={{ display: 'grid', gap: 12, gridColumn: '1 / -1' }}>
        <div className="class-card">
          <div className="class-card-head">
            <h3>Plan Curricular</h3>
          </div>

          {/* Selector de asignación */}
          {asignacionesDisponibles.length === 0 ? (
            <p>No hay asignaciones disponibles para este curso.</p>
          ) : (
            <>
              {asignacionesDisponibles.length > 1 && (
                <div style={{ marginBottom: 16 }}>
                  <label htmlFor="asignacionSelect">Seleccione la asignación:</label>
                  <AnimatedSelect
                    ariaLabel="Asignación"
                    value={selectedAsignacionId || ''}
                    onChange={(value) => setSelectedAsignacionId(Number(value))}
                    options={[
                      { value: '', label: 'Seleccione una asignación' },
                      ...asignacionesDisponibles.map((a) => ({
                        value: a.id,
                        label: a.materiaNombre ?? `Asignación ${a.id}`,
                      })),
                    ]}
                  />
                </div>
              )}

              {/* Mostrar estado del plan */}
              {selectedAsignacionId && (
                <div style={{ marginBottom: 16 }}>
                  <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 12 }}>
                    <button
                      type="button"
                      className="button secondary"
                      onClick={() => void loadCurrentPlan()}
                      disabled={loading}
                      aria-label="Actualizar estado del plan curricular"
                      style={{ gap: 8 }}
                    >
                      <RefreshIcon />
                      {loading ? 'Actualizando...' : 'Actualizar estado'}
                    </button>
                  </div>

                  {loading ? (
                    <div className="content-state content-state--compact" role="status" aria-live="polite">
                      <div className="content-state-icon"><i /></div>
                      <div className="content-state-copy">
                        <h2>Consultando el estado del plan</h2>
                        <p>Estamos buscando la revisión más reciente de esta asignación.</p>
                      </div>
                    </div>
                  ) : planError ? (
                    <div className="content-state content-state--compact content-state--error" role="alert">
                      <div className="content-state-icon">!</div>
                      <div className="content-state-copy">
                        <h2>No pudimos consultar el plan</h2>
                        <p>{planError}</p>
                      </div>
                      <div className="content-state-actions">
                        <button type="button" className="button secondary" onClick={() => void loadCurrentPlan()}>
                          Reintentar
                        </button>
                      </div>
                    </div>
                  ) : !plan || plan.estado === 'NO_CARGADO' ? (
                    <>
                      <p style={{ marginBottom: 12, color: 'var(--muted)' }}>
                        Aún no has subido un plan curricular para esta asignación.
                      </p>
                      <div style={{ marginBottom: 12 }}>
                        <button
                          type="button"
                          className="button secondary"
                          onClick={handleDownloadPlantilla}
                        >
                          Descargar plantilla
                        </button>
                      </div>

                      {/* Formulario de upload */}
                      <form onSubmit={handleUploadPlan} style={{ display: 'grid', gap: 12 }}>
                        <div>
                          <label htmlFor="fileInput">Seleccionar archivo completado:</label>
                          <input
                            id="fileInput"
                            type="file"
                            accept=".xlsx"
                            onChange={(e) => setSelectedFile(e.target.files?.[0] ?? null)}
                            required
                            style={{ marginTop: 8, width: '100%' }}
                          />
                        </div>
                        <button type="submit" className="button" disabled={!selectedFile || uploading}>
                          {uploading ? 'Subiendo...' : 'Subir plan curricular'}
                        </button>
                      </form>
                    </>
                  ) : plan.estado === 'PENDIENTE' ? (
                    <>
                      <div
                        style={{
                          padding: 16,
                          background: 'color-mix(in srgb, var(--warning) 10%, var(--paper))',
                          border: '1px solid color-mix(in srgb, var(--warning) 45%, var(--line))',
                          borderRadius: 'var(--radius-sm)',
                          marginBottom: 12,
                        }}
                      >
                        <p style={{ margin: 0 }}>
                          <strong>Tu plan está en revisión</strong>
                        </p>
                        <p style={{ margin: '8px 0 0', color: 'var(--ink)', lineHeight: 1.55 }}>
                          La carga se completó correctamente. Un evaluador revisará tu plan. Te notificaremos cuando haya una respuesta.
                        </p>
                        <UploadedFile name={plan.archivoNombre} />
                        <p style={{ margin: '5px 0 0', fontSize: '0.85rem', color: 'var(--muted)' }}>
                          Subido: {plan.fechaSubida ? new Date(plan.fechaSubida).toLocaleDateString('es-AR') : 'N/A'}
                        </p>
                      </div>
                      {!isReplacingPending ? (
                        <button
                          type="button"
                          className="button secondary"
                          onClick={() => {
                            const confirmed = window.confirm('¿Querés preparar un plan de reemplazo? El plan que está en revisión no se perderá: seguirá vigente hasta que selecciones y subas un archivo nuevo.');
                            if (!confirmed) return;
                            setSelectedFile(null);
                            setIsReplacingPending(true);
                          }}
                        >
                          Subir nuevo plan
                        </button>
                      ) : (
                        <div style={{ display: 'grid', gap: 12, paddingTop: 4 }}>
                          <div className="notice" style={{ margin: 0 }}>
                            El plan actual sigue en revisión. Solo se reemplazará cuando confirmes la subida del nuevo archivo.
                          </div>
                          <form onSubmit={handleUploadPlan} style={{ display: 'grid', gap: 12 }}>
                            <label htmlFor="fileInputReemplazo">Seleccionar nuevo archivo completado:</label>
                            <input
                              id="fileInputReemplazo"
                              type="file"
                              accept=".xlsx"
                              onChange={(e) => setSelectedFile(e.target.files?.[0] ?? null)}
                              required
                            />
                            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                              <button type="button" className="button secondary" onClick={() => { setSelectedFile(null); setIsReplacingPending(false); }}>
                                Cancelar
                              </button>
                              <button type="submit" className="button" disabled={!selectedFile || uploading}>
                                {uploading ? 'Subiendo...' : 'Confirmar reemplazo'}
                              </button>
                            </div>
                          </form>
                        </div>
                      )}
                    </>
                  ) : plan.estado === 'APROBADO' ? (
                    <>
                      <div
                        style={{
                          padding: 16,
                          background: 'color-mix(in srgb, var(--success) 10%, var(--paper))',
                          border: '1px solid color-mix(in srgb, var(--success) 45%, var(--line))',
                          borderRadius: 'var(--radius-sm)',
                          marginBottom: 12,
                        }}
                      >
                        <p style={{ margin: 0 }}>
                          <strong>✓ Tu plan ha sido aprobado</strong>
                        </p>
                        <UploadedFile name={plan.archivoNombre} />
                        <p style={{ margin: '5px 0 0', fontSize: '0.85rem', color: 'var(--muted)' }}>
                          Aprobado: {plan.fechaRevision ? new Date(plan.fechaRevision).toLocaleDateString('es-AR') : 'N/A'}
                        </p>
                      </div>

                      {/* Tabla de temas */}
                      {plan.temas && plan.temas.length > 0 && (
                        <div style={{ marginBottom: 16 }}>
                          <h4 style={{ marginTop: 0 }}>Temas del plan curricular</h4>
                          <div className="table-responsive">
                            <table className="table table-striped" style={{ fontSize: '0.9rem' }}>
                              <thead>
                                <tr>
                                  <th>Mes</th>
                                  <th>Tema/Contenido</th>
                                  <th>Capacidades</th>
                                  <th>Actividades</th>
                                </tr>
                              </thead>
                              <tbody>
                                {plan.temas.map((tema, idx) => (
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
                    </>
                  ) : plan.estado === 'RECHAZADO' ? (
                    <>
                      <div
                        style={{
                          padding: 16,
                          background: 'color-mix(in srgb, var(--danger) 9%, var(--paper))',
                          border: '1px solid color-mix(in srgb, var(--danger) 45%, var(--line))',
                          borderRadius: 'var(--radius-sm)',
                          marginBottom: 12,
                        }}
                      >
                        <p style={{ margin: 0 }}>
                          <strong>Tu plan ha sido rechazado</strong>
                        </p>
                        <UploadedFile name={plan.archivoNombre} />
                        {plan.observacionesEvaluador && (
                          <div style={{ marginTop: 14, padding: 14, borderLeft: '4px solid var(--danger)', borderRadius: '0 var(--radius-sm) var(--radius-sm) 0', background: 'color-mix(in srgb, var(--danger) 8%, var(--paper-raised))' }}>
                            <strong style={{ display: 'block', marginBottom: 6, color: 'var(--danger)', fontSize: '0.8rem', letterSpacing: '.04em', textTransform: 'uppercase' }}>
                              Observaciones del evaluador
                            </strong>
                            <p style={{ margin: 0, color: 'var(--ink)', fontSize: '1.05rem', fontWeight: 650, lineHeight: 1.55 }}>
                              {plan.observacionesEvaluador}
                            </p>
                          </div>
                        )}
                      </div>

                      <div style={{ marginBottom: 12 }}>
                        <button
                          type="button"
                          className="button secondary"
                          onClick={handleDownloadPlantilla}
                        >
                          Descargar plantilla
                        </button>
                      </div>

                      {/* Formulario de re-upload */}
                      <form onSubmit={handleUploadPlan} style={{ display: 'grid', gap: 12 }}>
                        <div>
                          <label htmlFor="fileInputRechazado">Subir plan corregido:</label>
                          <input
                            id="fileInputRechazado"
                            type="file"
                            accept=".xlsx"
                            onChange={(e) => setSelectedFile(e.target.files?.[0] ?? null)}
                            required
                            style={{ marginTop: 8, width: '100%' }}
                          />
                        </div>
                        <button type="submit" className="button" disabled={!selectedFile || uploading}>
                          {uploading ? 'Subiendo...' : 'Subir plan corregido'}
                        </button>
                      </form>
                    </>
                  ) : null}
                </div>
              )}
            </>
          )}
        </div>

        {status && (
          <div
            className={`notice ${status.includes('error') || status.includes('No se pudo') ? 'error' : ''}`}
            style={{ marginBottom: 12 }}
          >
            {status}
          </div>
        )}
      </div>
    </div>
  );
}
