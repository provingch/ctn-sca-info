import { useEffect, useRef, type ReactNode } from 'react';
import './seleccion-tabla.css';

/** Casilla de la cabecera: marca o desmarca todas las filas visibles; queda a medias si hay algunas. */
export function CasillaTodos({ todos, algunos, onChange, etiqueta }: { todos: boolean; algunos: boolean; onChange: () => void; etiqueta: string }) {
  const ref = useRef<HTMLInputElement>(null);
  useEffect(() => { if (ref.current) ref.current.indeterminate = algunos; }, [algunos]);
  return <th className="seleccion-col" scope="col">
    <input ref={ref} type="checkbox" className="seleccion-check" checked={todos} onChange={onChange} aria-label={etiqueta} />
  </th>;
}

export function CasillaFila({ checked, onChange, etiqueta }: { checked: boolean; onChange: () => void; etiqueta: string }) {
  return <td className="seleccion-col">
    <input type="checkbox" className="seleccion-check" checked={checked} onChange={onChange} aria-label={etiqueta} />
  </td>;
}

/** Barra que aparece con filas elegidas: cuántas son, las acciones en lote y cómo soltar la selección. */
export function BarraSeleccion({ cantidad, unidad, onLimpiar, children, ocupado = false }: {
  cantidad: number;
  unidad: [singular: string, plural: string];
  onLimpiar: () => void;
  children: ReactNode;
  ocupado?: boolean;
}) {
  if (cantidad === 0) return null;
  return <div className="barra-seleccion" role="region" aria-label="Acciones sobre la selección" aria-busy={ocupado}>
    <strong aria-live="polite">{cantidad} {cantidad === 1 ? unidad[0] : unidad[1]} {cantidad === 1 ? 'seleccionado' : 'seleccionados'}</strong>
    <div className="barra-seleccion-acciones">
      {children}
      <button type="button" className="button secondary" onClick={onLimpiar} disabled={ocupado}>Quitar selección</button>
    </div>
  </div>;
}
