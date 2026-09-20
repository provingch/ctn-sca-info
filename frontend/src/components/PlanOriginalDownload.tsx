import { useRef, useState } from 'react';
import { descargarDocumentoOriginal } from '../api/planCurricular';
import { ApiError } from '../api/client';
import { useToast } from '../context/toast';

export default function PlanOriginalDownload({ id }: { id: number }) {
  const [busy, setBusy] = useState(false);
  const inFlight = useRef(false);
  const { showToast } = useToast();
  async function download() {
    if (inFlight.current) return;
    inFlight.current = true; setBusy(true);
    try { await descargarDocumentoOriginal(id); }
    catch (error) { showToast(error instanceof ApiError ? error.message : 'No se pudo descargar el archivo original.', { tone: 'error' }); }
    finally { inFlight.current = false; setBusy(false); }
  }
  return <button type="button" className="button secondary" disabled={busy} onClick={() => void download()}>{busy ? 'Descargando…' : 'Descargar archivo original'}</button>;
}
