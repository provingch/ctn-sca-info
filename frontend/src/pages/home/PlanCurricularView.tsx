import { useEffect, useState, type FormEvent } from 'react';
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

export default function PlanCurricularView({ data, reload }: { data: HomeResponse; reload: () => Promise<void> }) {
  const [asignacionesDisponibles, setAsignacionesDisponibles] = useState<AsignacionOption[]>([]);
  const [selectedAsignacionId, setSelectedAsignacionId] = useState<number | null>(null);
  const [plan, setPlan] = useState<planCurricularApi.PlanCurricularEstado | null>(null);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [status, setStatus] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  // Cargar asignaciones disponibles cuando cambia el curso
  useEffect(() => {
    if (!data.selCurso) {
      setAsignacionesDisponibles([]);
      setSelectedAsignacionId(null);
      setPlan(null);
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
        }
      } catch {
        setAsignacionesDisponibles([]);
        setSelectedAsignacionId(null);
        setPlan(null);
      }
    })();
  }, [data.selCurso]);

  // Cargar plan cuando cambia la asignación seleccionada
  useEffect(() => {
    if (!selectedAsignacionId || !data.selCurso) {
      setPlan(null);
      return;
    }

    setLoading(true);
    (async () => {
      try {
        const currentYear = new Date().getFullYear();
        const transition = new Date(currentYear, 6, 15); // 15 de julio
        const currentEtapa = new Date() < transition ? '1' : '2';
        
        const result = await planCurricularApi.getMiPlan(selectedAsignacionId, currentEtapa, currentYear);
        setPlan(result ?? null);
        setStatus('');
      } catch (err) {
        if (err instanceof ApiError && err.status === 204) {
          setPlan(null);
        } else {
          setStatus(err instanceof ApiError ? err.message : 'No se pudo cargar el plan');
          setPlan(null);
        }
      } finally {
        setLoading(false);
      }
    })();
  }, [selectedAsignacionId, data.selCurso]);

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
      setSelectedFile(null);
      setPlan({ id: result.id, estado: 'PENDIENTE' });
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
                  {loading ? (
                    <p>Cargando plan...</p>
                  ) : !plan ? (
                    <>
                      <p style={{ marginBottom: 12, color: '#666' }}>
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
                          padding: 12,
                          background: '#fef3c7',
                          border: '1px solid #fcd34d',
                          borderRadius: 4,
                          marginBottom: 12,
                        }}
                      >
                        <p style={{ margin: 0 }}>
                          <strong>Tu plan está en revisión</strong>
                        </p>
                        <p style={{ margin: '8px 0 0', fontSize: '0.9rem', color: '#666' }}>
                          Archivo: {plan.archivoNombre}
                          <br />
                          Subido: {plan.fechaSubida ? new Date(plan.fechaSubida).toLocaleDateString('es-AR') : 'N/A'}
                        </p>
                      </div>
                      <button
                        type="button"
                        className="button secondary"
                        onClick={async () => {
                          setSelectedFile(null);
                          setPlan(null);
                        }}
                      >
                        Subir nuevo plan
                      </button>
                    </>
                  ) : plan.estado === 'APROBADO' ? (
                    <>
                      <div
                        style={{
                          padding: 12,
                          background: '#d1fae5',
                          border: '1px solid #6ee7b7',
                          borderRadius: 4,
                          marginBottom: 12,
                        }}
                      >
                        <p style={{ margin: 0 }}>
                          <strong>✓ Tu plan ha sido aprobado</strong>
                        </p>
                        <p style={{ margin: '8px 0 0', fontSize: '0.9rem', color: '#666' }}>
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
                          padding: 12,
                          background: '#fee2e2',
                          border: '1px solid #fca5a5',
                          borderRadius: 4,
                          marginBottom: 12,
                        }}
                      >
                        <p style={{ margin: 0 }}>
                          <strong>Tu plan ha sido rechazado</strong>
                        </p>
                        {plan.observacionesEvaluador && (
                          <p style={{ margin: '8px 0 0', fontSize: '0.9rem', color: '#666' }}>
                            Observaciones: {plan.observacionesEvaluador}
                          </p>
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
