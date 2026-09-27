import { useEffect, useRef, useState, type ReactNode } from 'react';
import DashboardActivity from './DashboardActivity';

/** Envuelve la columna de tarjetas de funciones + el panel de actividad, midiendo
 *  el alto real de las tarjetas para que el panel nunca la supere (con scroll interno). */
export default function CardsWithActivity({ children }: { children: ReactNode }) {
  const cardsRef = useRef<HTMLDivElement>(null);
  const [maxHeight, setMaxHeight] = useState<number | undefined>(undefined);

  useEffect(() => {
    const el = cardsRef.current;
    if (!el) return;
    const update = () => setMaxHeight(el.getBoundingClientRect().height);
    update();
    const observer = new ResizeObserver(update);
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  return <div className="launcher-body">
    <div ref={cardsRef}>{children}</div>
    <DashboardActivity maxHeight={maxHeight} />
  </div>;
}
