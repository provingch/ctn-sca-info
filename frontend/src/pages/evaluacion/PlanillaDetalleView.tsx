import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '../../api/client';
import * as evaluacionApi from '../../api/evaluacion';
import type { PlanillaDetail } from '../../api/academics';
import ContentState from '../../components/ui/ContentState';
import GradeChip from '../../components/ui/GradeChip';
import ReabrirEtapaDialog from '../../components/ReabrirEtapaDialog';
import { useToast } from '../../context/toast';
import EvaluacionFiltrosCampos from './EvaluacionFiltrosCampos';
import type { EvaluacionFiltros } from './useEvaluacionFiltros';

function formatFecha(value: string | null | undefined): string {
  if (!value) return '—';
  return new Intl.DateTimeFormat('es-PY', { day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'UTC' }).format(new Date(`${value}T00:00:00Z`));
}

/** Estado de la etapa a la que pertenece la planilla (cada planilla es de una sola etapa). */
function estadoEtapa(planilla: evaluacionApi.PlanillaResumen): { cerrada: boolean; fecha: string | null } {
  return planilla.etapaIndex === 2
    ? { cerrada: planilla.etapa2Confirmada, fecha: planilla.fechaCierreEtapa2 }
    : { cerrada: planilla.etapa1Confirmada, fecha: planilla.fechaCierreEtapa1 };
}

function messageFor(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

/**
 * Vista de evaluación de las planillas: lista las que matchean los filtros y muestra una en pantalla, sólo lectura.
 * Comparte tabla (.grade-table) y componentes con la del profesor, pero sin ningún control de edición.
 */
export default function PlanillaDetalleView({ filtros }: { filtros: EvaluacionFiltros }) {
  const { selected, etapa, periodo, materiaId } = filtros;
  const { showToast } = useToast();
  const [planillas, setPlanillas] = useState<evaluacionApi.PlanillaResumen[]>([]);
  const [loading, setLoading] = useState(false);
  const [listError, setListError] = useState('');
  const [abierta, setAbierta] = useState<evaluacionApi.PlanillaResumen | null>(null);
  const [detalle, setDetalle] = useState<PlanillaDetail | null>(null);
  const [loadingDetalle, setLoadingDetalle] = useState(false);
  const [detalleError, setDetalleError] = useState('');
  const [reabriendo, setReabriendo] = useState(false);
  const [descargando, setDescargando] = useState(false);
  const cursoId = selected?.id;

  const cargarLista = useCallback(async () => {
    if (!cursoId) {
      setPlanillas([]);
      return;
    }
    setLoading(true);
    setListError('');
    try {
      setPlanillas(await evaluacionApi.listarPlanillas(cursoId, etapa, periodo, materiaId));
    } catch (error) {
      setPlanillas([]);
      // 404 = no hay planillas para esos filtros: es un estado vacío, no un error
      if (error instanceof ApiError && error.status === 404) return;
      setListError(messageFor(error, 'No se pudieron cargar las planillas.'));
    } finally {
      setLoading(false);
    }
  }, [cursoId, etapa, periodo, materiaId]);

  useEffect(() => {
    setAbierta(null);
    void cargarLista();
  }, [cargarLista]);

  useEffect(() => {
    if (!abierta) {
      setDetalle(null);
      setDetalleError('');
      return;
    }
    let active = true;
    setLoadingDetalle(true);
    setDetalleError('');
    evaluacionApi.getPlanillaEvaluacion(abierta.id)
      .then((result) => { if (active) setDetalle(result); })
      .catch((error) => { if (active) { setDetalle(null); setDetalleError(messageFor(error, 'No se pudo cargar la planilla.')); } })
      .finally(() => { if (active) setLoadingDetalle(false); });
    return () => { active = false; };
  }, [abierta]);

  async function descargarEstaPlanilla(planilla: evaluacionApi.PlanillaResumen) {
    setDescargando(true);
    try {
      await evaluacionApi.descargarPlanillas(planilla.cursoId, planilla.etapaIndex === 2 ? 'segunda' : 'primera', planilla.periodo, planilla.materiaId);
    } catch (error) {
      showToast(messageFor(error, 'No se pudo descargar el archivo.'), { tone: 'error' });
    } finally {
      setDescargando(false);
    }
  }

  async function confirmarReapertura(planilla: evaluacionApi.PlanillaResumen, motivo: string) {
    const etapaReabierta = planilla.etapaIndex === 2 ? 2 : 1;
    await evaluacionApi.reabrirEtapaEvaluacion(planilla.id, etapaReabierta, motivo);
    setReabriendo(false);
    showToast(`Etapa ${etapaReabierta} reabierta. Se avisó al profesor.`, { tone: 'success', autoDismiss: true });
    const lista = await evaluacionApi.listarPlanillas(planilla.cursoId, etapa, periodo, materiaId).catch(() => null);
    if (lista) {
      setPlanillas(lista);
      setAbierta(lista.find((item) => item.id === planilla.id) ?? null);
    }
  }

  if (abierta) {
    const estado = estadoEtapa(abierta);
    const etapaNumero = abierta.etapaIndex === 2 ? 2 : 1;
    return <>
      <div className="toolbar">
        <button type="button" className="button secondary" onClick={() => setAbierta(null)}>← Volver al listado</button>
      </div>
      <section className="panel evaluacion-planilla-header">
        <div>
          <span className="badge">Modo evaluación — solo lectura</span>
          <h2>{abierta.materiaNombre}</h2>
          <p className="muted-copy">
            {detalle?.curso ? `${detalle.curso.especialidad} ${detalle.curso.nivel}° ${detalle.curso.seccion}` : 'Curso'} · Profesor: {abierta.profesorNombre || '—'} · Período {abierta.periodo}
          </p>
          <p>
            <strong>Etapa {etapaNumero}:</strong>{' '}
            <span className={`badge${estado.cerrada ? ' badge--danger' : ''}`}>{estado.cerrada ? 'Cerrada' : 'Abierta'}</span>
            {estado.fecha && <small className="muted-copy"> · {estado.cerrada ? 'Cierre' : 'Cierre programado'}: {formatFecha(estado.fecha)}</small>}
          </p>
        </div>
        <div className="evaluacion-planilla-acciones">
          <button type="button" className="button secondary" disabled={descargando} onClick={() => void descargarEstaPlanilla(abierta)}>{descargando ? 'Generando…' : 'Descargar esta planilla'}</button>
          {estado.cerrada && <button type="button" className="button danger" onClick={() => setReabriendo(true)}>Reabrir Etapa {etapaNumero}</button>}
        </div>
      </section>
      {loadingDetalle ? <ContentState tone="loading" title="Cargando planilla…" />
        : detalleError ? <ContentState tone="error" title="No se pudo cargar la planilla" detail={detalleError} actions={<button type="button" className="button secondary" onClick={() => setAbierta({ ...abierta })}>Reintentar</button>} />
        : detalle && <TablaNotas detalle={detalle} />}
      {reabriendo && <ReabrirEtapaDialog etapa={etapaNumero} onCancel={() => setReabriendo(false)} onConfirm={(motivo) => confirmarReapertura(abierta, motivo)} />}
    </>;
  }

  return <>
    <section className="panel form-grid evaluation-filters">
      <p className="lead">Elegí el curso, la sección, la etapa y el período para ver las planillas correspondientes.</p>
      <EvaluacionFiltrosCampos filtros={filtros} />
    </section>
    {!selected ? <ContentState compact title="Elegí un curso y una sección" detail="Cuando los elijas, acá aparecen sus planillas." />
      : loading ? <ContentState tone="loading" title="Cargando planillas…" />
      : listError ? <ContentState tone="error" title="No se pudieron cargar las planillas" detail={listError} actions={<button type="button" className="button secondary" onClick={() => void cargarLista()}>Reintentar</button>} />
      : planillas.length === 0 ? <ContentState title="No hay planillas" detail="No encontramos planillas para los filtros seleccionados." />
      : <section className="panel">
        <h3>Planillas ({planillas.length})</h3>
        <div className="table-wrap">
          <table className="grade-table evaluacion-planillas-table">
            <caption className="visually-hidden">Planillas del curso seleccionado</caption>
            <thead><tr><th scope="col">Materia</th><th scope="col">Profesor</th><th scope="col">Estado de la etapa</th><th scope="col">Acciones</th></tr></thead>
            <tbody>
              {planillas.map((planilla) => {
                const estado = estadoEtapa(planilla);
                return <tr key={planilla.id}>
                  <th scope="row">{planilla.materiaNombre}</th>
                  <td>{planilla.profesorNombre || '—'}</td>
                  <td><span className={`badge${estado.cerrada ? ' badge--danger' : ''}`}>{estado.cerrada ? 'Cerrada' : 'Abierta'}</span>{estado.fecha && <small className="muted-copy"> {formatFecha(estado.fecha)}</small>}</td>
                  <td><button type="button" className="button secondary" aria-label={`Ver planilla de ${planilla.materiaNombre}`} onClick={() => setAbierta(planilla)}>Ver planilla</button></td>
                </tr>;
              })}
            </tbody>
          </table>
        </div>
      </section>}
  </>;
}

/** Notas por alumno: mismas clases que la del profesor, todo texto plano (sin inputs ni guardado). */
function TablaNotas({ detalle }: { detalle: PlanillaDetail }) {
  const rsaActivo = detalle.planilla.rsaPuntos != null;
  return <section className="planilla-table-panel" aria-labelledby="planilla-eval-table-title">
    <header className="planilla-table-heading">
      <div>
        <span>Calificaciones</span>
        <h2 id="planilla-eval-table-title">{detalle.planilla.materiaNombre}</h2>
      </div>
      <small>{detalle.tareas.length} {detalle.tareas.length === 1 ? 'tarea' : 'tareas'} en esta etapa</small>
    </header>
    <div className="table-wrap planilla-grade-table-wrap">
      <table className="grade-table planilla-grade-table">
        <caption className="visually-hidden">Calificaciones de {detalle.planilla.materiaNombre}, solo lectura</caption>
        <thead>
          <tr>
            <th className="planilla-number-heading">#</th>
            <th className="planilla-student-heading">Alumno</th>
            {detalle.tareas.map((task) => <th key={task.id} className="planilla-task-heading">
              {task.titulo}
              <small className="planilla-task-tp">TP: {task.total}</small>
            </th>)}
            <th>Total</th>
            <th>%</th>
            <th>Nota</th>
            {rsaActivo && <th className="rsa-column" scope="col">RSA</th>}
          </tr>
        </thead>
        <tbody>
          {detalle.rows.map((row, index) => <tr key={row.alumnoId}>
            <td className="planilla-row-number">{index + 1}</td>
            <th className="planilla-student-name" scope="row">{row.alumnoNombre}</th>
            {detalle.tareas.map((task) => {
              const puntos = row.grades.find((grade) => grade.tareaId === task.id)?.puntos;
              return <td key={task.id} className="planilla-task-grade" aria-label={`${row.alumnoNombre}, ${task.titulo}: ${puntos == null ? 'sin calificación' : `${puntos} puntos`}`}>
                <span>{puntos == null ? '—' : puntos}</span>
              </td>;
            })}
            <td className="student-total-cell">{row.total}<small>de {detalle.planilla.totalPossiblePoints}</small></td>
            <td className="student-percentage-cell">{row.porcentaje}%</td>
            <td><GradeChip grade={row.nota} className="student-grade-pill" /></td>
            {rsaActivo && <td className="rsa-column student-rsa-cell">{row.rsaPuntos}<small>de {detalle.planilla.rsaPuntos}</small></td>}
          </tr>)}
          {detalle.rows.length === 0 && <tr><td className="planilla-student-empty" colSpan={detalle.tareas.length + 5 + (rsaActivo ? 1 : 0)}>Esta planilla todavía no tiene alumnos cargados.</td></tr>}
        </tbody>
      </table>
    </div>
  </section>;
}
