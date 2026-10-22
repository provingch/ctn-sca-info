import PlanFileDropzone from '../../components/PlanFileDropzone';
import PlanOriginalDownload from '../../components/PlanOriginalDownload';
import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '../../api/client';
import AnimatedSelect from '../../components/AnimatedSelect';
import * as planCurricularApi from '../../api/planCurricular';
import useAccessibleDialog from '../../hooks/useAccessibleDialog';
import { useToast } from '../../context/toast';
import { formatSqlDateTime } from '../../utils/date';
import TemasPorMesAccordion from '../../components/TemasPorMesAccordion';
import IncongruenciasRetroactivasPanel from './IncongruenciasRetroactivasPanel';
import '../../components/plan-views.css';

type AssignmentGroup = { id: number; nombre: string; asignaciones: planCurricularApi.AsignacionCompleta[] };

function unique<T>(items: T[], key: (item: T) => string | number): T[] {
  return Array.from(new Map(items.map((item) => [key(item), item])).values());
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback;
}

function EstadoBadge({ estado }: { estado: string }) {
  const color = estado === 'APROBADO' ? 'success' : estado === 'RECHAZADO' ? 'danger' : 'warning';
  return <span className={`estado-badge estado-badge--${color}`}>{estado}</span>;
}

function PlanDetalleModal({ id, onClose }: { id: number; onClose: () => void }) {
  const [plan, setPlan] = useState<planCurricularApi.PlanCurricularEstado | null>(null);
  const [error, setError] = useState('');
  const dialogRef = useAccessibleDialog(true, onClose);

  useEffect(() => {
    let active = true;
    void planCurricularApi.getPlanDetalle(id)
      .then((result) => { if (active) setPlan(result); })
      .catch((reason) => { if (active) setError(errorMessage(reason, 'No se pudo cargar el detalle del plan.')); });
    return () => { active = false; };
  }, [id]);

  return <div ref={dialogRef} className="plan-modal-overlay" role="dialog" aria-modal="true" aria-labelledby="plan-detail-title" tabIndex={-1} onClick={onClose}>
    <section className="data-modal plan-modal" onClick={(event) => event.stopPropagation()}>
      <div className="signature-modal-header">
        <div><span>Plan curricular</span><h2 id="plan-detail-title">Detalle del plan curricular</h2></div>
        <button type="button" className="signature-modal-close" aria-label="Cerrar detalle" data-dialog-initial-focus onClick={onClose}>×</button>
      </div>
      <div className="data-modal-section">
        {error ? <div className="notice error">{error}</div> : !plan ? <p>Cargando detalle…</p> : <>
          <div className="inline-actions"><EstadoBadge estado={plan.estado} /><span>{plan.archivoNombre}</span><PlanOriginalDownload id={id} /></div>
          {plan.observacionesEvaluador && <div className="plan-observaciones"><strong>Observaciones del evaluador</strong><p>{plan.observacionesEvaluador}</p></div>}
          {plan.temas?.length ? <TemasPorMesAccordion temas={plan.temas} mostrarCobertura={plan.estado === 'APROBADO'} etapaCerrada={plan.etapaCerrada} /> : <p>No hay temas parseados para este plan.</p>}
        </>}
      </div>
    </section>
  </div>;
}

const MAX_BLOQUES_POR_MES = 20;
const DEFAULT_BLOQUES = 4;

function ConfigPlantillaModal({ asignacionId, etapa, onClose }: { asignacionId: number; etapa: string; onClose: () => void }) {
  const [meses, setMeses] = useState<string[] | null>(null);
  const [seleccion, setSeleccion] = useState<Record<string, { activo: boolean; bloques: number }>>({});
  const [errorMeses, setErrorMeses] = useState('');
  const [descargando, setDescargando] = useState(false);
  const { showToast } = useToast();
  const dialogRef = useAccessibleDialog(true, onClose);

  useEffect(() => {
    let active = true;
    planCurricularApi.getMesesDisponibles(etapa)
      .then((result) => {
        if (!active) return;
        setMeses(result);
        const initial: Record<string, { activo: boolean; bloques: number }> = {};
        result.forEach((m) => { initial[m] = { activo: true, bloques: DEFAULT_BLOQUES }; });
        setSeleccion(initial);
      })
      .catch((reason) => { if (active) setErrorMeses(errorMessage(reason, 'No se pudieron cargar los meses disponibles.')); });
    return () => { active = false; };
  }, [etapa]);

  const seleccionados = Object.entries(seleccion).filter(([, cfg]) => cfg.activo);
  const puedeDescargar = seleccionados.length > 0 && seleccionados.every(([, cfg]) => cfg.bloques >= 1 && cfg.bloques <= MAX_BLOQUES_POR_MES);

  async function descargar() {
    if (!puedeDescargar || !meses) return;
    const config: planCurricularApi.PlanTemplateConfigDto = {
      etapa,
      meses: meses
        .filter((m) => seleccion[m]?.activo)
        .map((m) => ({ mes: m, bloques: seleccion[m].bloques })),
    };
    setDescargando(true);
    try {
      await planCurricularApi.downloadPlantillaConfigurada(asignacionId, config);
      onClose();
    } catch (err) {
      showToast(errorMessage(err, 'No se pudo descargar la plantilla.'), { tone: 'error' });
    } finally {
      setDescargando(false);
    }
  }

  return <div ref={dialogRef} className="plan-modal-overlay" role="dialog" aria-modal="true" aria-labelledby="plantilla-config-title" tabIndex={-1} onClick={onClose}>
    <section className="data-modal plan-modal narrow" onClick={(event) => event.stopPropagation()}>
      <div className="signature-modal-header">
        <div><span>Plan curricular</span><h2 id="plantilla-config-title">Configurar plantilla — Etapa {etapa}</h2></div>
        <button type="button" className="signature-modal-close" aria-label="Cerrar configuración" onClick={onClose}>×</button>
      </div>
      <div className="data-modal-section">
      <p className="plan-hint">Elegí los meses que vas a cargar y cuántos bloques (celdas) necesitás para cada uno (1 a {MAX_BLOQUES_POR_MES}).</p>
      {errorMeses ? <div className="notice error">{errorMeses}</div>
        : !meses ? <p>Cargando meses…</p>
        : <div className="month-config-list">
          {meses.map((m) => {
            const cfg = seleccion[m] ?? { activo: false, bloques: DEFAULT_BLOQUES };
            return <div key={m} className="month-config-row">
              <label>
                <input
                  type="checkbox"
                  checked={cfg.activo}
                  onChange={(event) => setSeleccion((prev) => ({ ...prev, [m]: { ...cfg, activo: event.target.checked } }))}
                />
                <strong>{m}</strong>
              </label>
              <label>
                <span>Bloques</span>
                <input
                  type="number"
                  min={1}
                  max={MAX_BLOQUES_POR_MES}
                  value={cfg.bloques}
                  disabled={!cfg.activo}
                  onChange={(event) => {
                    const raw = Number(event.target.value);
                    const clamped = Number.isFinite(raw) ? Math.max(1, Math.min(MAX_BLOQUES_POR_MES, Math.floor(raw))) : DEFAULT_BLOQUES;
                    setSeleccion((prev) => ({ ...prev, [m]: { ...cfg, bloques: clamped } }));
                  }}
                  aria-label={`Cantidad de bloques para ${m}`}
                />
              </label>
            </div>;
          })}
        </div>}
      <div className="inline-actions end">
        <button type="button" className="button secondary" onClick={onClose} disabled={descargando}>Cancelar</button>
        <button type="button" className="button" data-dialog-initial-focus disabled={!puedeDescargar || descargando} onClick={() => void descargar()}>
          {descargando ? 'Descargando…' : 'Descargar plantilla'}
        </button>
      </div>
      </div>
    </section>
  </div>;
}

function DescargarPlantillaSection({ group }: { group: AssignmentGroup }) {
  const [curso, setCurso] = useState('');
  const [seccion, setSeccion] = useState('');
  const [materiaId, setMateriaId] = useState('');
  const cursos = unique(group.asignaciones, (item) => item.cursoOrdinal);
  const secciones = curso ? unique(group.asignaciones.filter((item) => item.cursoOrdinal === curso), (item) => item.seccion) : [];
  const materias = curso && seccion ? group.asignaciones.filter((item) => item.cursoOrdinal === curso && item.seccion === seccion) : [];
  const asignacion = materias.find((item) => item.materiaId === Number(materiaId));
  const [etapa, setEtapa] = useState<string>('1');
  const [configOpen, setConfigOpen] = useState(false);

  return <section className="class-card">
    <h3 className="plan-card-title">Descargar plantilla</h3>
    <div className="class-grid">
      <div className="class-field"><label>Curso</label><AnimatedSelect ariaLabel={`Curso de ${group.nombre}`} value={curso} onChange={(value) => { setCurso(value); setSeccion(''); setMateriaId(''); }} options={[{ value: '', label: 'Seleccione curso' }, ...cursos.map((item) => ({ value: item.cursoOrdinal, label: item.cursoOrdinal }))]} /></div>
      <div className="class-field"><label>Sección</label><AnimatedSelect ariaLabel={`Sección de ${group.nombre}`} value={seccion} disabled={!curso} onChange={(value) => { setSeccion(value); setMateriaId(''); }} options={[{ value: '', label: 'Seleccione sección' }, ...secciones.map((item) => ({ value: item.seccion, label: item.seccion }))]} /></div>
      <div className="class-field"><label>Materia</label><AnimatedSelect ariaLabel={`Materia de ${group.nombre}`} value={materiaId} disabled={!seccion} onChange={setMateriaId} options={[{ value: '', label: 'Seleccione materia' }, ...materias.map((item) => ({ value: item.materiaId, label: item.materiaNombre }))]} /></div>
      <div className="class-field"><label>Etapa</label><AnimatedSelect ariaLabel={`Etapa de ${group.nombre}`} value={etapa} onChange={setEtapa} options={[{ value: '1', label: 'Etapa 1' }, { value: '2', label: 'Etapa 2' }]} /></div>
      <div className="class-field class-field-action"><button type="button" className="button" disabled={!asignacion} onClick={() => setConfigOpen(true)}>Configurar y descargar</button></div>
    </div>
    {asignacion?.estadoPlan === 'APROBADO' && <div className="notice success">Esta asignación ya cuenta con un plan aprobado. Podés descargar nuevamente su plantilla si lo necesitás.</div>}
    {configOpen && asignacion && <ConfigPlantillaModal asignacionId={asignacion.id} etapa={etapa} onClose={() => setConfigOpen(false)} />}
  </section>;
}

export default function PlanCurricularView() {
  const [asignaciones, setAsignaciones] = useState<planCurricularApi.AsignacionCompleta[] | null>(null);
  const [planes, setPlanes] = useState<planCurricularApi.PlanHistorialItem[] | null>(null);
  const [errorAsignaciones, setErrorAsignaciones] = useState('');
  const [errorPlanes, setErrorPlanes] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const { showToast } = useToast();
  const setUploadMessage = (msg: string) => { if (msg) showToast(msg, { tone: 'success' }); };
  const setUploadError = (msg: string) => { if (msg) showToast(msg, { tone: 'error', autoDismiss: false }); };
  const [candidatas, setCandidatas] = useState<planCurricularApi.AsignacionCandidata[]>([]);
  const [candidataId, setCandidataId] = useState('');
  const [pendingFile, setPendingFile] = useState<File | null>(null);
  const [detalleId, setDetalleId] = useState<number | null>(null);


  const loadPlanes = useCallback(async () => {
    setErrorPlanes('');
    try { setPlanes(await planCurricularApi.getMisPlanes()); } catch (err) { setErrorPlanes(errorMessage(err, 'No se pudo cargar el historial de entregas.')); }
  }, []);

  const loadAsignaciones = useCallback(async () => {
    setErrorAsignaciones('');
    try { setAsignaciones(await planCurricularApi.getMisAsignaciones()); } catch (err) { setErrorAsignaciones(errorMessage(err, 'No se pudieron cargar tus asignaciones.')); }
  }, []);

  useEffect(() => { void loadAsignaciones(); void loadPlanes(); }, [loadAsignaciones, loadPlanes]);

  const groups = asignaciones ? Array.from(asignaciones.reduce((map, assignment) => {
    const id = assignment.especialidadId;
    const current = map.get(id) ?? { id, nombre: assignment.especialidadNombre, asignaciones: [] };
    current.asignaciones.push(assignment);
    map.set(id, current);
    return map;
  }, new Map<number, AssignmentGroup>()).values()) : [];

  async function upload(file: File, asignacionId?: number) {
    setUploading(true);
    try {
      const result = await planCurricularApi.subirPlanAutoDetectado(file, asignacionId);
      setUploadMessage(result?.materiaNombre ? `Plan subido para ${result.especialidadNombre} · ${result.cursoOrdinal} ${result.seccion} · ${result.materiaNombre}.` : 'Plan curricular subido correctamente.');
      setSelectedFile(null); setPendingFile(null); setCandidatas([]); setCandidataId('');

      await Promise.all([loadPlanes(), loadAsignaciones()]);
    } catch (err) {
      if (err instanceof ApiError && err.status === 400 && typeof err.body === 'object' && err.body !== null && 'candidatas' in err.body && Array.isArray((err.body as planCurricularApi.MultiplesCoincidenciasError).candidatas)) {
        setCandidatas((err.body as planCurricularApi.MultiplesCoincidenciasError).candidatas);
        setPendingFile(file);
      } else setUploadError(errorMessage(err, 'No se pudo subir el plan curricular.'));
    } finally { setUploading(false); }
  }

  return <div className="two-column">
    <div className="class-card span-full">
      <div className="class-card-head"><h3>Plan curricular</h3></div>
      <p className="plan-hint">Descargá la plantilla de una de tus asignaciones, completala y subila para su revisión.</p>
    </div>

    <IncongruenciasRetroactivasPanel />

    <section className="class-card">
      {errorAsignaciones ? <div className="notice error">{errorAsignaciones} <button type="button" className="button secondary" onClick={() => void loadAsignaciones()}>Reintentar</button></div> : asignaciones === null ? (
        <div aria-hidden="true" className="loading-ghost">
          <h4 className="selectable-group-title">Mis asignaciones</h4>
          <div className="class-grid">
            <div className="class-field"><label>Curso</label><AnimatedSelect ariaLabel="Curso" value="" disabled onChange={() => {}} options={[{ value: '', label: 'Seleccione curso' }]} /></div>
            <div className="class-field"><label>Sección</label><AnimatedSelect ariaLabel="Sección" value="" disabled onChange={() => {}} options={[{ value: '', label: 'Seleccione sección' }]} /></div>
            <div className="class-field"><label>Materia</label><AnimatedSelect ariaLabel="Materia" value="" disabled onChange={() => {}} options={[{ value: '', label: 'Seleccione materia' }]} /></div>
            <div className="class-field class-field-action"><button type="button" className="button" disabled>Descargar plantilla</button></div>
          </div>
        </div>
      ) : groups.length === 0 ? <p>No tenés asignaciones disponibles.</p> : groups.map((group) => <div key={group.id}><h4 className="selectable-group-title">{groups.length > 1 ? group.nombre : 'Mis asignaciones'}</h4><DescargarPlantillaSection group={group} /></div>)}
    </section>

    <section className="class-card"><h3 className="plan-card-title">Subir plan</h3><p className="plan-hint">El sistema identifica automáticamente la asignación a partir de la plantilla.</p><PlanFileDropzone file={selectedFile} disabled={uploading} onError={setUploadError} onChange={(file) => { setSelectedFile(file); setPendingFile(null); setCandidatas([]); setCandidataId(''); }} /><button type="button" className="button" disabled={!selectedFile || uploading} onClick={() => selectedFile && void upload(selectedFile)}>{uploading ? 'Subiendo…' : 'Subir plan curricular'}</button>
      {candidatas.length > 0 && pendingFile && <div className="notice"><p>Se encontraron varias asignaciones compatibles. Elegí la correcta para continuar.</p><AnimatedSelect ariaLabel="Asignación compatible" value={candidataId} onChange={setCandidataId} options={[{ value: '', label: 'Seleccione una asignación' }, ...candidatas.map((candidate) => ({ value: candidate.id, label: candidate.descripcion }))]} /><button type="button" className="button" disabled={!candidataId || uploading} onClick={() => void upload(pendingFile, Number(candidataId))}>Confirmar asignación</button></div>}</section>

    <section className="class-card span-full"><div className="class-card-head"><h3>Entregas realizadas</h3><button type="button" className="button secondary" onClick={() => void loadPlanes()}>Actualizar</button></div>{errorPlanes ? <div className="notice error">{errorPlanes}</div> : planes === null ? (
      <div aria-hidden="true" className="loading-ghost table-responsive">
        <table className="table table-striped"><thead><tr><th>Materia</th><th>Especialidad</th><th>Curso</th><th>Archivo</th><th>Etapa / año</th><th>Estado</th><th>Subido</th></tr></thead><tbody><tr><td colSpan={7}>&nbsp;</td></tr><tr><td colSpan={7}>&nbsp;</td></tr></tbody></table>
      </div>
    ) : planes.length === 0 ? <p>Aún no registraste entregas.</p> : <div className="table-responsive"><table className="table table-striped"><caption className="visually-hidden">Planes curriculares entregados</caption><thead><tr><th>Materia</th><th>Especialidad</th><th>Curso</th><th>Archivo</th><th>Etapa / año</th><th>Estado</th><th>Subido</th></tr></thead><tbody>{planes.map((plan) => <tr key={plan.id}><td>{plan.materiaNombre}</td><td>{plan.especialidadNombre}</td><td>{plan.cursoOrdinal} {plan.seccion}</td><td><button type="button" className="table-row-action" onClick={() => setDetalleId(plan.id)} aria-label={`Ver detalle de ${plan.archivoNombre}`}>{plan.archivoNombre}</button></td><td>{plan.etapa} / {plan.anio}</td><td><EstadoBadge estado={plan.estado} /></td><td>{formatSqlDateTime(plan.fechaSubida, { dateStyle: 'short' })}</td></tr>)}</tbody></table></div>}</section>

    {detalleId !== null && <PlanDetalleModal id={detalleId} onClose={() => setDetalleId(null)} />}
  </div>;
}
