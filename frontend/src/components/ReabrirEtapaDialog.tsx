import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import useAccessibleDialog from '../hooks/useAccessibleDialog';

export const MOTIVO_REAPERTURA_MAX = 500;

/**
 * Diálogo para reabrir una etapa ya cerrada. Es una acción con impacto real (vuelve a permitir editar notas
 * cerradas), así que no se puede confirmar sin un motivo: queda en el registro de actividad y se le avisa al profesor.
 * El padre lo monta sólo mientras está abierto; si {@code onConfirm} falla, el motivo se conserva para reintentar.
 */
export default function ReabrirEtapaDialog({ etapa, onCancel, onConfirm }: {
  etapa: 1 | 2;
  onCancel: () => void;
  onConfirm: (motivo: string) => Promise<void>;
}) {
  const [motivo, setMotivo] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const dialog = useAccessibleDialog(true, () => { if (!busy) onCancel(); });
  const motivoLimpio = motivo.trim();

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (busy || !motivoLimpio) return;
    setBusy(true);
    setError('');
    try {
      await onConfirm(motivoLimpio);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : `No se pudo reabrir la Etapa ${etapa}. Reintentá.`);
      setBusy(false);
    }
  }

  return <div ref={dialog} className="data-modal reabrir-dialog" role="dialog" aria-modal="true" aria-labelledby="reabrir-title" aria-describedby="reabrir-help" tabIndex={-1}>
    <div className="signature-modal-header">
      <h2 id="reabrir-title">Reabrir Etapa {etapa}</h2>
      <button className="signature-modal-close" type="button" aria-label="Cerrar" disabled={busy} onClick={onCancel}>×</button>
    </div>
    <form className="form-grid" onSubmit={(event) => void submit(event)}>
      <p id="reabrir-help" className="reabrir-dialog-field">El profesor va a poder volver a editar notas y tareas de esta etapa. El motivo queda en el registro de actividad y se le avisa al profesor.</p>
      <label className="reabrir-dialog-field">Motivo de la reapertura
        <textarea data-dialog-initial-focus required rows={4} maxLength={MOTIVO_REAPERTURA_MAX} value={motivo} disabled={busy} placeholder="Explicá por qué se reabre la etapa" onChange={(event) => setMotivo(event.target.value)} />
        <small className="muted-copy">{motivo.length}/{MOTIVO_REAPERTURA_MAX}</small>
      </label>
      {error && <p className="notice error reabrir-dialog-field" role="alert">{error}</p>}
      <div className="signature-modal-actions">
        <button className="button secondary" type="button" disabled={busy} onClick={onCancel}>Cancelar</button>
        <button className="button danger" type="submit" disabled={busy || !motivoLimpio}>{busy ? 'Reabriendo…' : `Reabrir Etapa ${etapa}`}</button>
      </div>
    </form>
  </div>;
}
