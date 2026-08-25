import { useCallback, useEffect, useRef, useState } from 'react';
import { ApiError } from '../../api/client';
import AnimatedSelect from '../../components/AnimatedSelect';
import * as planCurricularApi from '../../api/planCurricular';

type AssignmentGroup = { id: number; nombre: string; asignaciones: planCurricularApi.AsignacionCompleta[] };

function unique<T>(items: T[], key: (item: T) => string | number): T[] {
  return Array.from(new Map(items.map((item) => [key(item), item])).values());
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback;
}

function EstadoBadge({ estado }: { estado: string }) {
  const color = estado === 'APROBADO' ? 'success' : estado === 'RECHAZADO' ? 'danger' : 'warning';
  return <span style={{ padding: '2px 10px', borderRadius: 999, fontSize: '0.8rem', fontWeight: 700, background: `color-mix(in srgb, var(--${color}) 15%, var(--paper))`, color: `var(--${color})`, border: `1px solid color-mix(in srgb, var(--${color}) 45%, var(--line))` }}>{estado}</span>;
}

function PlanDetalleModal({ id, onClose }: { id: number; onClose: () => void }) {
  const [plan, setPlan] = useState<planCurricularApi.PlanCurricularEstado | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    void planCurricularApi.getPlanDetalle(id).then(setPlan).catch((err) => setError(errorMessage(err, 'No se pudo cargar el detalle del plan.')));
  }, [id]);

  return <div role="dialog" aria-modal="true" aria-label="Detalle del plan curricular" style={{ position: 'fixed', inset: 0, zIndex: 100, display: 'grid', placeItems: 'center', padding: 20, background: 'rgba(0, 0, 0, .55)' }} onClick={onClose}>
    <section className="panel" style={{ width: 'min(1000px, 100%)', maxHeight: '85vh', overflow: 'auto', margin: 0 }} onClick={(event) => event.stopPropagation()}>
      <div className="class-card-head"><h3>Detalle del plan curricular</h3><button type="button" className="button secondary" onClick={onClose}>Cerrar</button></div>
      {error ? <div className="notice error">{error}</div> : !plan ? <p>Cargando detalle…</p> : <>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap', margin: '16px 0' }}><EstadoBadge estado={plan.estado} /><span>{plan.archivoNombre}</span><button type="button" className="button secondary" onClick={() => void planCurricularApi.descargarDocumentoOriginal(id)}>Descargar archivo original</button></div>
        {plan.observacionesEvaluador && <div style={{ marginBottom: 16, padding: 14, borderLeft: '4px solid var(--danger)', background: 'color-mix(in srgb, var(--danger) 8%, var(--paper-raised))' }}><strong>Observaciones del evaluador</strong><p style={{ margin: '6px 0 0' }}>{plan.observacionesEvaluador}</p></div>}
        {plan.temas?.length ? <div className="table-responsive"><table className="table table-striped"><thead><tr><th>Mes</th><th>Tema / Contenido</th><th>Capacidades</th><th>Actividades</th></tr></thead><tbody>{plan.temas.map((tema, index) => <tr key={index}><td>{tema.mes}</td><td>{tema.temasContenidos}</td><td>{tema.capacidades || '—'}</td><td>{tema.actividades || '—'}</td></tr>)}</tbody></table></div> : <p>No hay temas parseados para este plan.</p>}
      </>}
    </section>
  </div>;
}

function DescargarPlantillaSection({ group }: { group: AssignmentGroup }) {
  const [curso, setCurso] = useState('');
  const [seccion, setSeccion] = useState('');
  const [materiaId, setMateriaId] = useState('');
  const [error, setError] = useState('');
  const cursos = unique(group.asignaciones, (item) => item.cursoOrdinal);
  const secciones = curso ? unique(group.asignaciones.filter((item) => item.cursoOrdinal === curso), (item) => item.seccion) : [];
  const materias = curso && seccion ? group.asignaciones.filter((item) => item.cursoOrdinal === curso && item.seccion === seccion) : [];
  const asignacion = materias.find((item) => item.materiaId === Number(materiaId));

  async function download() {
    if (!asignacion) return;
    setError('');
    try { await planCurricularApi.downloadPlantilla(asignacion.id); } catch (err) { setError(errorMessage(err, 'No se pudo descargar la plantilla.')); }
  }

  return <section className="class-card" style={{ marginTop: 12 }}>
    <h3 style={{ margin: 0, fontSize: '1.15rem' }}>Descargar plantilla</h3>
    <div className="class-grid">
      <div className="class-field"><label>Curso</label><AnimatedSelect ariaLabel={`Curso de ${group.nombre}`} value={curso} onChange={(value) => { setCurso(value); setSeccion(''); setMateriaId(''); }} options={[{ value: '', label: 'Seleccione curso' }, ...cursos.map((item) => ({ value: item.cursoOrdinal, label: item.cursoOrdinal }))]} /></div>
      <div className="class-field"><label>Sección</label><AnimatedSelect ariaLabel={`Sección de ${group.nombre}`} value={seccion} disabled={!curso} onChange={(value) => { setSeccion(value); setMateriaId(''); }} options={[{ value: '', label: 'Seleccione sección' }, ...secciones.map((item) => ({ value: item.seccion, label: item.seccion }))]} /></div>
      <div className="class-field"><label>Materia</label><AnimatedSelect ariaLabel={`Materia de ${group.nombre}`} value={materiaId} disabled={!seccion} onChange={setMateriaId} options={[{ value: '', label: 'Seleccione materia' }, ...materias.map((item) => ({ value: item.materiaId, label: item.materiaNombre }))]} /></div>
      <div className="class-field" style={{ justifyContent: 'end' }}><button type="button" className="button" disabled={!asignacion} onClick={() => void download()}>Descargar plantilla</button></div>
    </div>
    {asignacion?.estadoPlan === 'APROBADO' && <div className="notice" style={{ margin: 0, background: 'color-mix(in srgb, var(--success) 10%, var(--paper))', borderColor: 'color-mix(in srgb, var(--success) 45%, var(--line))' }}>Esta asignación ya cuenta con un plan aprobado. Podés descargar nuevamente su plantilla si lo necesitás.</div>}
    {error && <div className="notice error">{error}</div>}
  </section>;
}

export default function PlanCurricularView() {
  const [asignaciones, setAsignaciones] = useState<planCurricularApi.AsignacionCompleta[] | null>(null);
  const [planes, setPlanes] = useState<planCurricularApi.PlanHistorialItem[] | null>(null);
  const [errorAsignaciones, setErrorAsignaciones] = useState('');
  const [errorPlanes, setErrorPlanes] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadMessage, setUploadMessage] = useState('');
  const [uploadError, setUploadError] = useState('');
  const [candidatas, setCandidatas] = useState<planCurricularApi.AsignacionCandidata[]>([]);
  const [candidataId, setCandidataId] = useState('');
  const [pendingFile, setPendingFile] = useState<File | null>(null);
  const [detalleId, setDetalleId] = useState<number | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

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
    setUploading(true); setUploadError(''); setUploadMessage('');
    try {
      const result = await planCurricularApi.subirPlanAutoDetectado(file, asignacionId);
      setUploadMessage(result?.materiaNombre ? `Plan subido para ${result.especialidadNombre} · ${result.cursoOrdinal} ${result.seccion} · ${result.materiaNombre}.` : 'Plan curricular subido correctamente.');
      setSelectedFile(null); setPendingFile(null); setCandidatas([]); setCandidataId('');
      if (fileRef.current) fileRef.current.value = '';
      await Promise.all([loadPlanes(), loadAsignaciones()]);
    } catch (err) {
      if (err instanceof ApiError && err.status === 400 && typeof err.body === 'object' && err.body !== null && 'candidatas' in err.body && Array.isArray((err.body as planCurricularApi.MultiplesCoincidenciasError).candidatas)) {
        setCandidatas((err.body as planCurricularApi.MultiplesCoincidenciasError).candidatas);
        setPendingFile(file);
      } else setUploadError(errorMessage(err, 'No se pudo subir el plan curricular.'));
    } finally { setUploading(false); }
  }

  return <div className="two-column">
    <div className="panel" style={{ display: 'grid', gap: 18, gridColumn: '1 / -1' }}>
      <div className="class-card"><div className="class-card-head"><h3>Plan curricular</h3></div><p style={{ margin: 0, color: 'var(--muted)' }}>Descargá la plantilla de una de tus asignaciones, completala y subila para su revisión.</p></div>
      <section className="class-card"><h3 style={{ margin: 0 }}>Plantillas por asignación</h3>{errorAsignaciones ? <div className="notice error">{errorAsignaciones} <button type="button" className="button secondary" onClick={() => void loadAsignaciones()}>Reintentar</button></div> : asignaciones === null ? <p>Cargando asignaciones…</p> : groups.length === 0 ? <p>No tenés asignaciones disponibles.</p> : groups.map((group) => <div key={group.id}><h4 style={{ margin: '14px 0 0' }}>{groups.length > 1 ? group.nombre : 'Mis asignaciones'}</h4><DescargarPlantillaSection group={group} /></div>)}</section>
      <section className="class-card"><h3 style={{ margin: 0 }}>Subir plan</h3><p style={{ margin: 0, color: 'var(--muted)' }}>El sistema identifica automáticamente la asignación a partir de la plantilla.</p><input ref={fileRef} type="file" accept=".xlsx" disabled={uploading} onChange={(event) => { setSelectedFile(event.target.files?.[0] ?? null); setUploadError(''); setUploadMessage(''); }} /><button type="button" className="button" disabled={!selectedFile || uploading} onClick={() => selectedFile && void upload(selectedFile)}>{uploading ? 'Subiendo…' : 'Subir plan curricular'}</button>{uploadMessage && <div className="notice">{uploadMessage}</div>}{uploadError && <div className="notice error">{uploadError}</div>}
        {candidatas.length > 0 && pendingFile && <div className="notice"><p>Se encontraron varias asignaciones compatibles. Elegí la correcta para continuar.</p><AnimatedSelect ariaLabel="Asignación compatible" value={candidataId} onChange={setCandidataId} options={[{ value: '', label: 'Seleccione una asignación' }, ...candidatas.map((candidate) => ({ value: candidate.id, label: candidate.descripcion }))]} /><button type="button" className="button" disabled={!candidataId || uploading} onClick={() => void upload(pendingFile, Number(candidataId))}>Confirmar asignación</button></div>}</section>
      <section className="class-card"><div className="class-card-head"><h3>Entregas realizadas</h3><button type="button" className="button secondary" onClick={() => void loadPlanes()}>Actualizar</button></div>{errorPlanes ? <div className="notice error">{errorPlanes}</div> : planes === null ? <p>Cargando entregas…</p> : planes.length === 0 ? <p>Aún no registraste entregas.</p> : <div className="table-responsive"><table className="table table-striped"><thead><tr><th>Materia</th><th>Especialidad</th><th>Curso</th><th>Archivo</th><th>Etapa / año</th><th>Estado</th><th>Subido</th></tr></thead><tbody>{planes.map((plan) => <tr key={plan.id} onClick={() => setDetalleId(plan.id)} style={{ cursor: 'pointer' }} tabIndex={0} onKeyDown={(event) => { if (event.key === 'Enter') setDetalleId(plan.id); }}><td>{plan.materiaNombre}</td><td>{plan.especialidadNombre}</td><td>{plan.cursoOrdinal} {plan.seccion}</td><td>{plan.archivoNombre}</td><td>{plan.etapa} / {plan.anio}</td><td><EstadoBadge estado={plan.estado} /></td><td>{new Date(plan.fechaSubida).toLocaleDateString('es-AR')}</td></tr>)}</tbody></table></div>}</section>
    </div>
    {detalleId !== null && <PlanDetalleModal id={detalleId} onClose={() => setDetalleId(null)} />}
  </div>;
}
