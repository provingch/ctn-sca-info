import { useEffect, useState } from 'react';
import { ApiError } from '../../api/client';
import { useToast } from '../../context/toast';
import AnimatedSelect from '../../components/AnimatedSelect';
import DateTimePicker from '../../components/DateTimePicker';
import { isValidDateTimeValue } from '../../utils/dateInput';
import * as evaluacionApi from '../../api/evaluacion';
import * as planCurricularApi from '../../api/planCurricular';
import { formatSqlDateTime } from '../../utils/date';
import PlanesPorEspecialidad from './PlanesPorEspecialidad';
import { formatFechaClase } from '../../utils/fechaClase';

type Tab = 'planes' | 'incumplimientos';
// Local status/state removed; toasts are used instead

function formatDate(value?: string): string {
  if (!value) return 'Pendiente';
  return formatSqlDateTime(value, { dateStyle: 'short', timeStyle: 'short' }, 'Pendiente');
}

const RETROACTIVA = evaluacionApi.TIPO_INCONGRUENCIA_RETROACTIVA;
const BLOQUEO = evaluacionApi.TIPO_BLOQUEO_INCONGRUENCIA_RETROACTIVA;

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
  const [notaReactivacion, setNotaReactivacion] = useState('');
  const [loading, setLoading] = useState(true);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [resolving, setResolving] = useState(false);
  // removed unused local status state
  const { showToast } = useToast();

  useEffect(() => {
    setTab(initialTab);
  }, [initialTab]);

  useEffect(() => {
    let active = true;
    Promise.all([planCurricularApi.getAprobados(), evaluacionApi.getIncumplimientos()])
      .then(([approvedPlans, pendingIncumplimientos]) => {
        if (active) {
          setPlanes(approvedPlans);
          setIncumplimientos(pendingIncumplimientos);
        }
      })
      .catch((error) => {
        const msg = messageFor(error, 'No se pudo cargar el seguimiento de profesores.');
        if (active) showToast(msg, { tone: 'error' });
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [showToast]);

  useEffect(() => {
    if (selectedPlanId == null) {
      setSelectedPlan(null);
      return;
    }
    let active = true;
    setLoadingDetail(true);
    planCurricularApi.getPlanDetalle(selectedPlanId)
      .then((plan) => { if (active) setSelectedPlan(plan); })
      .catch((error) => {
        if (active) {
          setSelectedPlan(null);
          const msg = messageFor(error, 'No se pudo cargar el detalle del plan.');
          showToast(msg, { tone: 'error' });
        }
      })
      .finally(() => { if (active) setLoadingDetail(false); });
    return () => { active = false; };
  }, [selectedPlanId, showToast]);

  function selectIncumplimiento(id: number) {
    setSelectedIncumplimientoId(id);
    setEstadoResolucion('PERMITIDO');
    setSuspensionDesde('');
    setSuspensionHasta('');
    setNotaReactivacion('');
  }

  /** Resuelve las incongruencias retroactivas y el bloqueo (sin fechas de suspensión) y recarga la lista. */
  async function resolverSinSuspension(resolucion: evaluacionApi.ResolucionIncumplimiento, exito: string) {
    if (selectedIncumplimientoId == null) return;
    setResolving(true);
    try {
      const resultado = await evaluacionApi.resolverIncumplimiento(selectedIncumplimientoId, resolucion);
      setIncumplimientos(await evaluacionApi.getIncumplimientos());
      setSelectedIncumplimientoId(null);
      setNotaReactivacion('');
      if (resultado.bloqueoGenerado) {
        showToast('Con este rechazo se alcanzó el umbral: "Iniciar clase" quedó bloqueado en la asignación hasta que lo reactives.', { tone: 'success' });
      } else {
        showToast(exito, { tone: 'success', autoDismiss: true });
      }
    } catch (error) {
      showToast(messageFor(error, 'No se pudo resolver el caso.'), { tone: 'error' });
    } finally {
      setResolving(false);
    }
  }

  async function handleResolver() {
    if (selectedIncumplimientoId == null) return;
    if (estadoResolucion === 'RECHAZADO') {
      if (!isValidDateTimeValue(suspensionDesde) || !isValidDateTimeValue(suspensionHasta)) {
        showToast('Indicá el inicio y el fin de la suspensión.', { tone: 'error' });
        return;
      }
      if (new Date(suspensionHasta).getTime() <= new Date(suspensionDesde).getTime()) {
        showToast('El fin de la suspensión debe ser posterior al inicio.', { tone: 'error' });
        return;
      }
    }
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
  const bloqueos = incumplimientos.filter((item) => item.tipo === BLOQUEO);
  const retroactivas = incumplimientos.filter((item) => item.tipo === RETROACTIVA);
  const atrasos = incumplimientos.filter((item) => item.tipo !== BLOQUEO && item.tipo !== RETROACTIVA);

  return <>
    <div className="tabs" style={{ marginBottom: 16 }}>
      <button type="button" className={tab === 'planes' ? 'active' : ''} onClick={() => setTab('planes')}>Seguimiento de planes</button>
      <button type="button" className={tab === 'incumplimientos' ? 'active' : ''} onClick={() => setTab('incumplimientos')}>Incumplimientos ({incumplimientos.length})</button>
    </div>

    {tab === 'planes' && <div className="evaluation-split-layout">
      <div className="panel">
        <h3>Planes aprobados ({planes.length})</h3>
        {loading ? <p>Cargando planes...</p> : planes.length === 0 ? <p style={{ color: 'var(--muted)' }}>No hay planes aprobados para seguimiento.</p> : <PlanesPorEspecialidad planes={planes}>{(groupPlanes) => <div style={{ display: 'grid', gap: 8 }}>
          {groupPlanes.map((plan) => <button key={plan.id} type="button" onClick={() => { setSelectedPlanId(plan.id); }} style={{ padding: 12, background: selectedPlanId === plan.id ? 'var(--accent-strong)' : 'var(--paper-raised)', border: selectedPlanId === plan.id ? '2px solid var(--accent)' : '1px solid var(--line)', borderRadius: 4, cursor: 'pointer', textAlign: 'left', color: 'var(--ink)' }}>
            <strong>{plan.materiaNombre}</strong>
            <div style={{ fontSize: '0.9rem', color: 'var(--muted)', marginTop: 4 }}>{plan.profesorNombreCorto ?? plan.profesorNombre}</div>
            <div style={{ fontSize: '0.85rem', color: 'var(--muted)', marginTop: 2 }}>{plan.cursoDescripcion}<br />Aprobado: {formatDate(plan.fechaRevision)}</div>
          </button>)}
        </div>}</PlanesPorEspecialidad>}
      </div>
      <div className="panel">
        {!selectedPlanId ? <p style={{ textAlign: 'center', color: 'var(--muted)' }}>Seleccioná un plan para consultar su cumplimiento.</p> : loadingDetail ? <p>Cargando detalle...</p> : !selectedPlan ? <p style={{ color: 'var(--danger)' }}>No se pudo cargar el plan.</p> : <>
          <h3>Temas y cumplimiento</h3>
          <p className="lead">{selectedPlan.profesorNombreCorto ?? selectedPlan.profesorNombre} · {selectedPlan.materiaNombre}</p>
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

    {tab === 'incumplimientos' && <div className="evaluation-split-layout equal-columns">
      <div className="panel">
        <h3>Casos pendientes ({incumplimientos.length})</h3>
        {loading ? <p>Cargando incumplimientos...</p> : incumplimientos.length === 0 ? <p style={{ color: 'var(--muted)' }}>No hay incumplimientos pendientes de resolución.</p> : <div style={{ display: 'grid', gap: 16 }}>
          {[
            { titulo: 'Bloqueos de Iniciar clase', items: bloqueos, destacado: true },
            { titulo: 'Incongruencias retroactivas', items: retroactivas, destacado: false },
            { titulo: 'Atrasos en tiempo real', items: atrasos, destacado: false },
          ].filter((grupo) => grupo.items.length > 0).map((grupo) => <section key={grupo.titulo} aria-label={grupo.titulo} style={{ display: 'grid', gap: 8 }}>
            <h4 style={{ margin: 0, color: grupo.destacado ? 'var(--danger)' : undefined }}>{grupo.titulo} ({grupo.items.length})</h4>
            {grupo.items.map((item) => <button key={item.id} type="button" onClick={() => selectIncumplimiento(item.id)} style={{ padding: 12, background: selectedIncumplimientoId === item.id ? 'var(--accent-strong)' : 'var(--paper-raised)', border: selectedIncumplimientoId === item.id ? '2px solid var(--accent)' : grupo.destacado ? '2px solid var(--danger)' : '1px solid var(--line)', borderRadius: 4, cursor: 'pointer', textAlign: 'left', color: 'var(--ink)' }}>
              <strong>{professorName(item)}</strong>
              <div style={{ fontSize: '0.9rem', color: 'var(--muted)', marginTop: 4 }}>{item.descripcion}</div>
              <div style={{ fontSize: '0.8rem', color: 'var(--muted)', marginTop: 4 }}>{item.tipo === RETROACTIVA && item.fechaClase ? `Clase del ${formatFechaClase(item.fechaClase)}` : item.tipo} · {formatDate(item.fechaCreacion)}</div>
            </button>)}
          </section>)}
        </div>}
      </div>
      <div className="panel">
        {!selectedIncumplimiento ? <p style={{ textAlign: 'center', color: 'var(--muted)' }}>Seleccioná un incumplimiento para resolverlo.</p> : selectedIncumplimiento.tipo === BLOQUEO ? <>
          <h3 style={{ color: 'var(--danger)' }}>Reactivar Iniciar clase</h3>
          <p><strong>{professorName(selectedIncumplimiento)}</strong>{selectedIncumplimiento.materiaNombre ? ` · ${selectedIncumplimiento.materiaNombre}` : ''}</p>
          <p className="lead">{selectedIncumplimiento.descripcion}</p>
          <p style={{ color: 'var(--muted)' }}>El bloqueo no vence solo: sigue hasta que lo levantes acá. La nota queda registrada y se le envía al profesor.</p>
          <div className="form-grid">
            <div className="form-field"><label htmlFor="nota-reactivacion" className="field-label">Nota de reactivación</label>
              <textarea id="nota-reactivacion" rows={4} value={notaReactivacion} disabled={resolving} onChange={(event) => setNotaReactivacion(event.target.value)} placeholder="Por qué se reactiva y a qué se llegó" style={{ width: '100%', resize: 'none' }} />
            </div>
            <button type="button" className="button" disabled={resolving || !notaReactivacion.trim()} onClick={() => void resolverSinSuspension({ estado: 'PERMITIDO', nota: notaReactivacion.trim() }, 'Iniciar clase reactivado y profesor notificado.')}>{resolving ? 'Reactivando...' : 'Reactivar Iniciar clase'}</button>
          </div>
        </> : selectedIncumplimiento.tipo === RETROACTIVA ? <>
          <h3>Incongruencia retroactiva</h3>
          <p><strong>{professorName(selectedIncumplimiento)}</strong>{selectedIncumplimiento.materiaNombre ? ` · ${selectedIncumplimiento.materiaNombre}` : ''}</p>
          <dl style={{ display: 'grid', gap: 8, margin: '8px 0 16px' }}>
            <div><dt style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>Fecha de la clase</dt><dd style={{ margin: 0 }}>{formatFechaClase(selectedIncumplimiento.fechaClase)}</dd></div>
            <div><dt style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>Tema ingresado</dt><dd style={{ margin: 0 }}>{selectedIncumplimiento.temaIngresado ?? '—'}</dd></div>
            <div><dt style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>Tema esperado según el plan</dt><dd style={{ margin: 0 }}>{selectedIncumplimiento.temaEsperado ?? '—'}</dd></div>
            <div><dt style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>Justificación del profesor</dt><dd style={{ margin: 0 }}>{selectedIncumplimiento.justificacionProfesor?.trim() || 'Sin justificación cargada.'}</dd></div>
          </dl>
          <p style={{ color: 'var(--muted)' }}>Aceptar no cuenta como falta. Rechazar suma una falta: al llegar al umbral se bloquea "Iniciar clase" hasta que evaluación lo reactive.</p>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            <button type="button" className="button" disabled={resolving} onClick={() => void resolverSinSuspension({ estado: 'PERMITIDO' }, 'Incongruencia aceptada y profesor notificado.')}>Aceptar</button>
            <button type="button" className="button secondary" disabled={resolving} onClick={() => void resolverSinSuspension({ estado: 'RECHAZADO' }, 'Incongruencia rechazada y profesor notificado.')}>Rechazar</button>
          </div>
        </> : <>
          <h3>Resolver incumplimiento</h3>
          <p><strong>{professorName(selectedIncumplimiento)}</strong></p>
          <p className="lead">{selectedIncumplimiento.descripcion}</p>
          <div className="form-grid">
            <label>Resolución
              <AnimatedSelect
                ariaLabel="Resolución del incumplimiento"
                value={estadoResolucion}
                onChange={(value) => setEstadoResolucion(value as 'PERMITIDO' | 'RECHAZADO')}
                options={[{ value: 'PERMITIDO', label: 'Permitido' }, { value: 'RECHAZADO', label: 'Rechazado' }]}
              />
            </label>
            {estadoResolucion === 'RECHAZADO' && <>
              <div className="form-field"><span className="field-label">Suspensión desde</span><DateTimePicker ariaLabel="Suspensión desde" value={suspensionDesde} disabled={resolving} onChange={setSuspensionDesde} /></div>
              <div className="form-field"><span className="field-label">Suspensión hasta</span><DateTimePicker ariaLabel="Suspensión hasta" value={suspensionHasta} disabled={resolving} onChange={setSuspensionHasta} /></div>
            </>}
            <button type="button" className="button" disabled={resolving || (estadoResolucion === 'RECHAZADO' && (!isValidDateTimeValue(suspensionDesde) || !isValidDateTimeValue(suspensionHasta)))} onClick={() => void handleResolver()}>{resolving ? 'Resolviendo...' : 'Resolver incumplimiento'}</button>
          </div>
        </>}
      </div>
    </div>}

    {/* status messages moved to global toasts */}
  </>;
}
