import { useState } from 'react';
import type { TemaPlanDto } from '../api/planCurricular';

interface Props {
  temas: TemaPlanDto[];
  initiallyOpen?: 'first' | 'all' | 'none';
  compact?: boolean;
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

export default function TemasPorMesAccordion({ temas, initiallyOpen = 'first', compact = false }: Props) {
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

  return <div style={{ display: 'grid', gap: 8 }}>
    {grupos.map((g) => {
      const open = abiertos.has(g.mes);
      const panelId = `temas-mes-${g.mes}-panel`;
      return <section key={g.mes} style={{ border: '1px solid var(--line)', borderRadius: 8, overflow: 'hidden', background: 'var(--paper)' }}>
        <button
          type="button"
          aria-expanded={open}
          aria-controls={panelId}
          onClick={() => toggle(g.mes)}
          style={{
            width: '100%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '10px 14px',
            background: 'var(--paper-raised)',
            border: 'none',
            cursor: 'pointer',
            fontWeight: 700,
            fontSize: '0.95rem',
            color: 'inherit',
          }}
        >
          <span>{g.mes} <span style={{ color: 'var(--muted)', fontWeight: 400, fontSize: '0.85rem' }}>· {g.items.length} bloque{g.items.length === 1 ? '' : 's'}</span></span>
          <span aria-hidden style={{ transform: `rotate(${open ? 90 : 0}deg)`, transition: 'transform .15s ease' }}>›</span>
        </button>
        {open && <div id={panelId} style={{ padding: compact ? '8px 12px' : '12px 16px' }}>
          <div className="table-responsive">
            <table className="table table-striped" style={{ fontSize: compact ? '0.85rem' : undefined }}>
              <caption className="visually-hidden">Bloques de {g.mes}</caption>
              <thead>
                <tr>
                  <th style={{ width: 60 }}>Bloque</th>
                  <th>Tema / Contenido</th>
                  <th>Capacidades</th>
                  <th>Actividades</th>
                </tr>
              </thead>
              <tbody>
                {g.items.map((t, idx) => <tr key={`${t.mes}-${t.bloque}-${idx}`}>
                  <td>B{t.bloque}</td>
                  <td>{t.temasContenidos || '—'}</td>
                  <td>{t.capacidades || '—'}</td>
                  <td>{t.actividades || '—'}</td>
                </tr>)}
              </tbody>
            </table>
          </div>
        </div>}
      </section>;
    })}
  </div>;
}
