import { useState } from 'react';
import type { TemaPlanDto } from '../api/planCurricular';
import CoberturaTemaCelda from './CoberturaTemaCelda';
import './plan-views.css';

interface Props {
  temas: TemaPlanDto[];
  initiallyOpen?: 'first' | 'all' | 'none';
  compact?: boolean;
  /** Agrega la columna de cumplimiento; sólo tiene sentido en un plan aprobado (uno en revisión no tiene cobertura). */
  mostrarCobertura?: boolean;
  etapaCerrada?: boolean;
}

function groupByMes(temas: TemaPlanDto[]): { mes: string; ordenMes: number; items: TemaPlanDto[] }[] {
  const map = new Map<string, { mes: string; ordenMes: number; items: TemaPlanDto[] }>();
  temas.forEach((t) => {
    const key = t.mes;
    const bucket = map.get(key) ?? { mes: t.mes, ordenMes: t.ordenMes, items: [] };
    bucket.items.push(t);
    map.set(key, bucket);
  });
  const groups = Array.from(map.values());
  groups.forEach((g) => g.items.sort((a, b) => a.bloque - b.bloque));
  groups.sort((a, b) => a.ordenMes - b.ordenMes);
  return groups;
}

export default function TemasPorMesAccordion({ temas, initiallyOpen = 'first', compact = false, mostrarCobertura = false, etapaCerrada = false }: Props) {
  const grupos = groupByMes(temas);
  const [abiertos, setAbiertos] = useState<Set<string>>(() => {
    if (initiallyOpen === 'all') return new Set(grupos.map((g) => g.mes));
    if (initiallyOpen === 'first' && grupos.length > 0) return new Set([grupos[0].mes]);
    return new Set();
  });

  function toggle(mes: string) {
    setAbiertos((prev) => {
      const next = new Set(prev);
      if (next.has(mes)) next.delete(mes); else next.add(mes);
      return next;
    });
  }

  if (grupos.length === 0) return <p>No hay temas cargados.</p>;

  return <div className="temas-mes-list">
    {grupos.map((g) => {
      const open = abiertos.has(g.mes);
      const panelId = `temas-mes-${g.mes}-panel`;
      return <section key={g.mes} className={`temas-mes${compact ? ' compact' : ''}`}>
        <button type="button" className="temas-mes-toggle" aria-expanded={open} aria-controls={panelId} onClick={() => toggle(g.mes)}>
          <span>{g.mes} <small>· {g.items.length} bloque{g.items.length === 1 ? '' : 's'}</small></span>
          <span aria-hidden className="temas-mes-chevron">›</span>
        </button>
        {open && <div id={panelId} className="temas-mes-panel">
          <div className="table-responsive">
            <table className="table table-striped">
              <caption className="visually-hidden">Bloques de {g.mes}</caption>
              <thead>
                <tr>
                  <th className="bloque-col">Bloque</th>
                  <th>Tema / Contenido</th>
                  <th>Capacidades</th>
                  <th>Actividades</th>
                  {mostrarCobertura && <th>Cumplimiento</th>}
                </tr>
              </thead>
              <tbody>
                {g.items.map((t, idx) => <tr key={`${t.mes}-${t.bloque}-${idx}`}>
                  <td>B{t.bloque}</td>
                  <td>{t.temasContenidos || '—'}</td>
                  <td>{t.capacidades || '—'}</td>
                  <td>{t.actividades || '—'}</td>
                  {mostrarCobertura && <td><CoberturaTemaCelda tema={t} etapaCerrada={etapaCerrada} /></td>}
                </tr>)}
              </tbody>
            </table>
          </div>
        </div>}
      </section>;
    })}
  </div>;
}
