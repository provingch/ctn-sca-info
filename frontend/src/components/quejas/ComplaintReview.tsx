import { useRef, useState, type FormEvent } from 'react';
import { ApiError } from '../../api/client';
import { isQuejaReviewed, reviewQueja, type QuejaItem, type QuejaRevision } from '../../api/quejas';
import { formatSqlDateTime } from '../../utils/date';

export default function ComplaintReview({ queja, reviewerName, onReviewed }: {
  queja: QuejaItem;
  reviewerName?: string;
  onReviewed: (revision: QuejaRevision) => void;
}) {
  const [editing, setEditing] = useState(false);
  const [conclusion, setConclusion] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const submitting = useRef(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (submitting.current || !conclusion.trim() || conclusion.trim().length > 5000) return;
    submitting.current = true;
    setSaving(true);
    setError('');
    try {
      const revision = await reviewQueja(queja.id, conclusion.trim());
      onReviewed(revision);
      setEditing(false);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo guardar la revisión. Tu conclusión se conservó para reintentar.');
    } finally {
      submitting.current = false;
      setSaving(false);
    }
  }

  if (isQuejaReviewed(queja)) return <div className="complaint-resolution">
    <strong>Conclusión de la revisión</strong>
    <p>{queja.conclusion || 'Sin conclusión registrada.'}</p>
    <small>Revisada el {formatSqlDateTime(queja.revisadaEn)}{queja.revisadaPor != null && <> · Por {reviewerName || `Usuario #${queja.revisadaPor}`}</>}</small>
  </div>;

  if (!editing) return <div className="complaint-review-action"><button className="button secondary" type="button" onClick={() => setEditing(true)}>Revisar queja #{queja.id}</button></div>;

  return <form className="complaint-review-form form-grid" onSubmit={submit} aria-busy={saving} aria-label={`Revisión de queja #${queja.id}`}>
    <label htmlFor={`conclusion-${queja.id}`}>Conclusión de la revisión</label>
    <textarea id={`conclusion-${queja.id}`} autoFocus rows={3} maxLength={5000} required disabled={saving} value={conclusion} onChange={(event) => setConclusion(event.target.value)} placeholder="Describí lo verificado y la resolución o las medidas tomadas." />
    <small>Al completar, se guardarán la fecha y el responsable de la revisión.</small>
    {error && <p className="notice error" role="alert">{error}</p>}
    <div className="complaint-review-buttons">
      <button className="button secondary" type="button" disabled={saving} onClick={() => setEditing(false)}>Cancelar</button>
      <button className="button" type="submit" disabled={saving || !conclusion.trim()}>{saving ? 'Guardando revisión…' : 'Completar revisión'}</button>
    </div>
  </form>;
}
