import PlanOriginalDownload from '../../components/PlanOriginalDownload';
import { useEffect, useState } from 'react';
import { ApiError } from '../../api/client';
import { useToast } from '../../context/toast';
import * as evaluacionApi from '../../api/evaluacion';
import * as planCurricularApi from '../../api/planCurricular';
import { formatSqlDateTime } from '../../utils/date';
import PlanesPorEspecialidad from './PlanesPorEspecialidad';
import { formatFechaClase } from '../../utils/fechaClase';
import ContentState from '../../components/ui/ContentState';
import CoberturaTemaCelda from '../../components/CoberturaTemaCelda';
import '../../components/plan-views.css';
import AnimatedSelect from '../../components/AnimatedSelect';
import FiltersToolbar, { FilterField } from '../../components/ui/FiltersToolbar';
import { filtrarIncumplimientos, hayFiltroIncumplimientos, SIN_FILTRO_INCUMPLIMIENTOS, type FiltroIncumplimientos } from './filtrosIncumplimientos';

type Tab = 'planes' | 'incumplimientos';
// Local status/state removed; toasts are used instead

function formatDate(value?: string): string {
  if (!value) return 'Pendiente';
  return formatSqlDateTime(value, { dateStyle: 'short', timeStyle: 'short' }, 'Pendiente');
}

const ATRASO = evaluacionApi.TIPO_ATRASO;
const RETROACTIVA = evaluacionApi.TIPO_INCONGRUENCIA_RETROACTIVA;
const BLOQUEOS: string[] = [evaluacionApi.TIPO_BLOQUEO_INCONGRUENCIA_RETROACTIVA, evaluacionApi.TIPO_BLOQUEO_ATRASO_TIEMPO_REAL];

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
  const [notaReactivacion, setNotaReactivacion] = useState('');
  const [filtroIncumplimientos, setFiltroIncumplimientos] = useState<FiltroIncumplimientos>(SIN_FILTRO_INCUMPLIMIENTOS);
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
    setNotaReactivacion('');
  }

  /** Resuelve un atraso, una incongruencia retroactiva o un bloqueo y recarga la lista. */
  async function resolver(resolucion: evaluacionApi.ResolucionIncumplimiento, exito: string) {
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

  const selectedIncumplimiento = incumplimientos.find((item) => item.id === selectedIncumplimientoId);
  const incumplimientosVisibles = filtrarIncumplimientos(incumplimientos, filtroIncumplimientos, professorName);
  const bloqueos = incumplimientosVisibles.filter((item) => BLOQUEOS.includes(item.tipo));
  const atrasos = incumplimientosVisibles.filter((item) => item.tipo === ATRASO);
  const retroactivas = incumplimientosVisibles.filter((item) => item.tipo === RETROACTIVA);

  return <>
    <div className="tabs planes-tabs">
      <button type="button" className={tab === 'planes' ? 'active' : ''} onClick={() => setTab('planes')}>Seguimiento de planes</button>
      <button type="button" className={tab === 'incumplimientos' ? 'active' : ''} onClick={() => setTab('incumplimientos')}>Incumplimientos{!loading && ` (${incumplimientos.length})`}</button>
    </div>

    {tab === 'planes' && <div className="evaluation-split-layout">
      <div className="panel">
        <h3>Planes aprobados{!loading && ` (${planes.length})`}</h3>
        {loading ? <ContentState tone="loading" compact title="Cargando planes…" /> : planes.length === 0 ? <ContentState compact title="No hay planes aprobados" detail="Los planes aprobados aparecen acá para seguir su cumplimiento." /> : <PlanesPorEspecialidad planes={planes}>{(groupPlanes) => <div className="selectable-list">
          {groupPlanes.map((plan) => <button key={plan.id} type="button" onClick={() => { setSelectedPlanId(plan.id); }} className={`selectable-item${selectedPlanId === plan.id ? ' is-selected' : ''}`} aria-pressed={selectedPlanId === plan.id}>
            <strong>{plan.materiaNombre}</strong>
            <div className="selectable-item-meta">{plan.profesorNombreCorto ?? plan.profesorNombre}</div>
            <div className="selectable-item-note">{plan.cursoDescripcion}<br />Aprobado: {formatDate(plan.fechaRevision)}</div>
          </button>)}
        </div>}</PlanesPorEspecialidad>}
      </div>
      <div className="panel">
        {!selectedPlanId ? <ContentState compact title="Seleccioná un plan" detail="Elegí uno de la lista para consultar su cumplimiento." /> : loadingDetail ? <ContentState tone="loading" compact title="Cargando detalle…" /> : !selectedPlan ? <ContentState tone="error" compact title="No se pudo cargar el plan" detail="Volvé a seleccionarlo o recargá la página." /> : <>
          <h3>Temas y cumplimiento</h3>
          <PlanOriginalDownload key={selectedPlanId} id={selectedPlanId} />
          <p className="lead">{selectedPlan.profesorNombreCorto ?? selectedPlan.profesorNombre} · {selectedPlan.materiaNombre}</p>
          <div className="table-responsive">
            <table className="table table-striped compact-table">
              <caption className="visually-hidden">Cumplimiento de temas del plan curricular</caption>
              <thead><tr><th>Mes</th><th>Tema/Contenido</th><th>Cumplido</th></tr></thead>
              <tbody>{selectedPlan.temas?.map((tema, index) => <tr key={`${tema.ordenMes}-${index}`}><td>{tema.mes}</td><td>{tema.temasContenidos}</td><td><CoberturaTemaCelda tema={tema} etapaCerrada={selectedPlan.etapaCerrada} /></td></tr>)}</tbody>
            </table>
          </div>
        </>}
      </div>
    </div>}

    {tab === 'incumplimientos' && <div className="evaluation-split-layout equal-columns">
      <div className="panel">
        <h3>Casos pendientes{!loading && ` (${incumplimientos.length})`}</h3>
        {!loading && incumplimientos.length > 0 && <FiltersToolbar ariaLabel="Filtros de incumplimientos" mostrando={incumplimientosVisibles.length} total={incumplimientos.length} unidad="casos" activo={hayFiltroIncumplimientos(filtroIncumplimientos)} onLimpiar={() => setFiltroIncumplimientos(SIN_FILTRO_INCUMPLIMIENTOS)}>
          <FilterField label="Buscar"><input type="search" placeholder="Profesor o materia" value={filtroIncumplimientos.busqueda} onChange={(event) => setFiltroIncumplimientos((actual) => ({ ...actual, busqueda: event.target.value }))} /></FilterField>
          <FilterField label="Tipo de caso" narrow><AnimatedSelect ariaLabel="Tipo de caso" value={filtroIncumplimientos.categoria} onChange={(categoria) => setFiltroIncumplimientos((actual) => ({ ...actual, categoria: categoria as FiltroIncumplimientos['categoria'] }))} options={[{ value: '', label: 'Todos los tipos' }, { value: 'bloqueos', label: 'Bloqueos de Iniciar clase' }, { value: 'atrasos', label: 'Atrasos pendientes' }, { value: 'retroactivas', label: 'Incongruencias retroactivas' }]} /></FilterField>
        </FiltersToolbar>}
        {loading ? <ContentState tone="loading" compact title="Cargando incumplimientos…" /> : incumplimientos.length === 0 ? <ContentState compact title="No hay incumplimientos pendientes" detail="Los atrasos, incongruencias y bloqueos por resolver aparecen acá." /> : incumplimientosVisibles.length === 0 ? <ContentState compact title="Sin coincidencias" detail="Ningún caso coincide con los filtros elegidos." actions={<button type="button" className="button secondary" onClick={() => setFiltroIncumplimientos(SIN_FILTRO_INCUMPLIMIENTOS)}>Limpiar filtros</button>} /> : <div className="selectable-list">
          {[
            { titulo: 'Bloqueos de Iniciar clase', items: bloqueos, destacado: true },
            { titulo: 'Atrasos pendientes de revisión', items: atrasos, destacado: false },
            { titulo: 'Incongruencias retroactivas', items: retroactivas, destacado: false },
          ].filter((grupo) => grupo.items.length > 0).map((grupo) => <section key={grupo.titulo} aria-label={grupo.titulo} className="selectable-group">
            <h4 className={`selectable-group-title${grupo.destacado ? ' is-alert' : ''}`}>{grupo.titulo} ({grupo.items.length})</h4>
            {grupo.items.map((item) => <button key={item.id} type="button" onClick={() => selectIncumplimiento(item.id)} className={`selectable-item${selectedIncumplimientoId === item.id ? ' is-selected' : grupo.destacado ? ' is-alert' : ''}`} aria-pressed={selectedIncumplimientoId === item.id}>
              <strong>{professorName(item)}</strong>
              <div className="selectable-item-meta">{item.descripcion}</div>
              <div className="selectable-item-note">{item.fechaClase ? `Clase del ${formatFechaClase(item.fechaClase)}` : item.tipo} · {formatDate(item.fechaCreacion)}</div>
            </button>)}
          </section>)}
        </div>}
      </div>
      <div className="panel">
        {!selectedIncumplimiento ? <ContentState compact title="Seleccioná un caso" detail="Elegí un incumplimiento de la lista para resolverlo." /> : BLOQUEOS.includes(selectedIncumplimiento.tipo) ? <>
          <h3 className="danger-heading">Reactivar Iniciar clase</h3>
          <p><strong>{professorName(selectedIncumplimiento)}</strong>{selectedIncumplimiento.materiaNombre ? ` · ${selectedIncumplimiento.materiaNombre}` : ''}</p>
          <p className="lead">{selectedIncumplimiento.descripcion}</p>
          <p className="lead">El bloqueo no vence solo: sigue hasta que lo levantes acá. La nota queda registrada y se le envía al profesor.</p>
          <div className="form-grid">
            <div className="form-field"><label htmlFor="nota-reactivacion" className="field-label">Nota de reactivación</label>
              <textarea id="nota-reactivacion" rows={4} value={notaReactivacion} disabled={resolving} onChange={(event) => setNotaReactivacion(event.target.value)} placeholder="Por qué se reactiva y a qué se llegó" className="textarea-fixed" />
            </div>
            <button type="button" className="button" disabled={resolving || !notaReactivacion.trim()} onClick={() => void resolver({ estado: 'PERMITIDO', nota: notaReactivacion.trim() }, 'Iniciar clase reactivado y profesor notificado.')}>{resolving ? 'Reactivando...' : 'Reactivar Iniciar clase'}</button>
          </div>
        </> : <>
          <h3>{selectedIncumplimiento.tipo === ATRASO ? 'Atraso justificado' : 'Incongruencia retroactiva'}</h3>
          <p><strong>{professorName(selectedIncumplimiento)}</strong>{selectedIncumplimiento.materiaNombre ? ` · ${selectedIncumplimiento.materiaNombre}` : ''}</p>
          <dl className="detail-list">
            <div><dt>Fecha de la clase</dt><dd>{formatFechaClase(selectedIncumplimiento.fechaClase)}</dd></div>
            <div><dt>Tema ingresado</dt><dd>{selectedIncumplimiento.temaIngresado ?? '—'}</dd></div>
            <div><dt>Tema esperado según el plan</dt><dd>{selectedIncumplimiento.temaEsperado ?? '—'}</dd></div>
            <div><dt>Justificación del profesor</dt><dd>{selectedIncumplimiento.justificacionProfesor?.trim() || 'Sin justificación cargada.'}</dd></div>
          </dl>
          <p className="lead">Aceptar no cuenta como falta. Rechazar suma una falta: al llegar al umbral se bloquea "Iniciar clase" hasta que evaluación lo reactive.</p>
          <div className="inline-actions">
            <button type="button" className="button" disabled={resolving} onClick={() => void resolver({ estado: 'PERMITIDO' }, selectedIncumplimiento.tipo === ATRASO ? 'Atraso aceptado y profesor notificado.' : 'Incongruencia aceptada y profesor notificado.')}>Aceptar</button>
            <button type="button" className="button secondary" disabled={resolving} onClick={() => void resolver({ estado: 'RECHAZADO' }, selectedIncumplimiento.tipo === ATRASO ? 'Atraso rechazado y profesor notificado.' : 'Incongruencia rechazada y profesor notificado.')}>Rechazar</button>
          </div>
        </>}
      </div>
    </div>}

  </>;
}
