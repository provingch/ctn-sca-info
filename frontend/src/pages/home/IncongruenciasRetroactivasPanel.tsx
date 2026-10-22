import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '../../api/client';
import * as evaluacionApi from '../../api/evaluacion';
import { useToast } from '../../context/toast';
import { formatFechaClase } from '../../utils/fechaClase';
import '../../components/plan-views.css';

const MAX_JUSTIFICACION = 2000;

function messageFor(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

/**
 * Clases que el profesor dio antes de tener el plan aprobado y que, al aprobarse, no coincidieron con lo
 * planificado. Mientras evaluación no las revise puede agregar una justificación a cada una.
 */
export default function IncongruenciasRetroactivasPanel() {
  const [items, setItems] = useState<evaluacionApi.IncumplimientoPendiente[] | null>(null);
  const [error, setError] = useState('');
  const [textos, setTextos] = useState<Record<number, string>>({});
  const [guardando, setGuardando] = useState<number | null>(null);
  const { showToast } = useToast();

  const load = useCallback(async () => {
    setError('');
    try {
      const list = await evaluacionApi.getMisIncongruencias();
      setItems(list);
      setTextos(Object.fromEntries(list.map((item) => [item.id, item.justificacionProfesor ?? ''])));
    } catch (err) {
      setError(messageFor(err, 'No se pudieron cargar tus incongruencias.'));
      setItems([]);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  async function guardar(item: evaluacionApi.IncumplimientoPendiente) {
    const texto = (textos[item.id] ?? '').trim();
    if (!texto) {
      showToast('Escribí la justificación antes de guardar.', { tone: 'error' });
      return;
    }
    setGuardando(item.id);
    try {
      await evaluacionApi.justificarIncongruencia(item.id, texto);
      setItems((current) => current?.map((other) => other.id === item.id ? { ...other, justificacionProfesor: texto } : other) ?? null);
      showToast('Justificación guardada.', { tone: 'success', autoDismiss: true });
    } catch (err) {
      showToast(messageFor(err, 'No se pudo guardar la justificación.'), { tone: 'error' });
    } finally {
      setGuardando(null);
    }
  }

  if (error) return <section className="class-card span-full"><div className="notice error">{error} <button type="button" className="button secondary" onClick={() => void load()}>Reintentar</button></div></section>;
  if (!items || items.length === 0) return null;

  return <section className="class-card span-full" aria-labelledby="incongruencias-title">
    <div className="class-card-head"><h3 id="incongruencias-title">Clases previas al plan con incongruencias ({items.length})</h3></div>
    <p className="plan-hint">Estas clases se dieron antes de que tu plan estuviera aprobado y no coinciden con lo planificado. Agregá una justificación si corresponde: evaluación la ve al revisar cada caso.</p>
    <div className="incongruencia-list">
      {items.map((item) => {
        const cambiada = (textos[item.id] ?? '').trim() !== (item.justificacionProfesor ?? '').trim();
        return <article key={item.id} className="incongruencia-item">
          <strong>{item.materiaNombre ?? 'Clase'} · {formatFechaClase(item.fechaClase)}</strong>
          <p><span>Tema dado:</span> {item.temaIngresado ?? '—'}</p>
          <p><span>Tema esperado según el plan:</span> {item.temaEsperado ?? '—'}</p>
          <div className="class-field">
            <label htmlFor={`justificacion-${item.id}`}>Justificación</label>
            <textarea id={`justificacion-${item.id}`} rows={3} maxLength={MAX_JUSTIFICACION} value={textos[item.id] ?? ''} disabled={guardando === item.id}
              onChange={(event) => setTextos((current) => ({ ...current, [item.id]: event.target.value }))}
              placeholder="Explicá por qué la clase no siguió el plan" className="textarea-fixed" />
          </div>
          <div><button type="button" className="button" disabled={guardando === item.id || !cambiada || !(textos[item.id] ?? '').trim()} onClick={() => void guardar(item)}>{guardando === item.id ? 'Guardando…' : item.justificacionProfesor ? 'Actualizar justificación' : 'Guardar justificación'}</button></div>
        </article>;
      })}
    </div>
  </section>;
}
