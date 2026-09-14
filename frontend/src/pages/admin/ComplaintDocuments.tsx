import { useRef, useState, type FormEvent } from 'react';
import { ApiError } from '../../api/client';
import { downloadQuejaExcel, downloadQuejaPdf, quejaEstado, resolveQueja, type QuejaItem, type QuejaResolucion } from '../../api/quejas';
import { formatSqlDateTime } from '../../utils/date';

export default function ComplaintDocuments({ queja, onResolved }: { queja: QuejaItem; onResolved: (resolution: QuejaResolucion) => void }) {
  const [editing, setEditing] = useState(false);
  const [procesoRevision, setProceso] = useState('');
  const [solucionAplicada, setSolucion] = useState('');
  const [corregidaPorNombre, setNombre] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const inFlight = useRef(false);
  const estado = quejaEstado(queja);

  async function run(action: () => Promise<unknown>) {
    if (inFlight.current) return;
    inFlight.current = true; setBusy(true); setError('');
    try { await action(); }
    catch (err) { setError(err instanceof ApiError ? err.message : 'No se pudo completar la operación. Los datos se conservaron para reintentar.'); }
    finally { inFlight.current = false; setBusy(false); }
  }
  function submit(event: FormEvent) {
    event.preventDefault();
    if (!procesoRevision.trim() || !solucionAplicada.trim() || !corregidaPorNombre.trim()) return;
    void run(async () => {
      const result = await resolveQueja(queja.id, { procesoRevision: procesoRevision.trim(), solucionAplicada: solucionAplicada.trim(), corregidaPorNombre: corregidaPorNombre.trim() });
      onResolved(result); setEditing(false);
    });
  }
  return <div className="complaint-documents" aria-busy={busy}>
    {estado === 'pendiente' && <>
      <button type="button" className="button secondary" disabled={busy} onClick={() => void run(() => downloadQuejaExcel(queja.id))}>{busy ? 'Generando solicitud…' : 'Descargar solicitud de revisión (Excel)'}</button>
      <small>Datos originales protegidos. Podés completar nombre o firma, fecha de aceptación y observaciones en el archivo.</small>
    </>}
    {estado === 'revisada' && !editing && <>
      <button type="button" className="button secondary" onClick={() => setEditing(true)}>Registrar solución</button>
      <small>Completá la resolución para habilitar el reporte en PDF.</small>
    </>}
    {estado === 'revisada' && editing && <form className="complaint-review-form form-grid" aria-label={`Resolución de queja #${queja.id}`} onSubmit={submit}>
      <label>Qué se revisó<textarea autoFocus required rows={3} maxLength={5000} disabled={busy} value={procesoRevision} onChange={(e) => setProceso(e.target.value)} placeholder="Describí los pasos y las verificaciones realizadas." /></label>
      <label>Solución aplicada<textarea required rows={3} maxLength={5000} disabled={busy} value={solucionAplicada} onChange={(e) => setSolucion(e.target.value)} placeholder="Detallá la solución que se llevó a cabo." /></label>
      <label>Nombre de quien corrigió<input required maxLength={200} disabled={busy} value={corregidaPorNombre} onChange={(e) => setNombre(e.target.value)} /></label>
      <small>Al guardar, la queja quedará resuelta y se registrará la fecha automáticamente.</small>
      <div className="complaint-review-buttons"><button className="button secondary" type="button" disabled={busy} onClick={() => setEditing(false)}>Cancelar</button>
        <button className="button" type="submit" disabled={busy || !procesoRevision.trim() || !solucionAplicada.trim() || !corregidaPorNombre.trim()}>{busy ? 'Guardando solución…' : 'Marcar como resuelta'}</button></div>
    </form>}
    {estado === 'resuelta' && <>
      <div className="complaint-resolution"><strong>Qué se revisó</strong><p>{queja.procesoRevision}</p><strong>Solución aplicada</strong><p>{queja.solucionAplicada}</p>
        <small>Corregida por {queja.corregidaPorNombre} · Resuelta el {formatSqlDateTime(queja.resueltaEn)}</small></div>
      <button className="button secondary" type="button" disabled={busy} onClick={() => void run(() => downloadQuejaPdf(queja.id))}>{busy ? 'Generando reporte…' : 'Descargar reporte de solución (PDF)'}</button>
    </>}
    {error && <p className="notice error" role="alert">{error}</p>}
  </div>;
}
