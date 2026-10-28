import { useMemo } from 'react';
import type { CargaHorariaItem, CargaHorariaSlotItem } from '../../api/admin';
import { normalizeSpecialty } from '../../theme/theme';
import HorarioTablaGrid from './HorarioTablaGrid';

const SABADO = 6;

function toMinutes(time: string) {
  const [h, m] = time.split(':').map(Number);
  return h * 60 + m;
}

function formatMinutes(total: number) {
  return `${Math.floor(total / 60)}h ${String(total % 60).padStart(2, '0')}min`;
}

function cursoLabel(slot: CargaHorariaSlotItem) {
  return [slot.cursoNivel != null ? `${slot.cursoNivel}°` : null, slot.especialidadNombre, slot.cursoSeccion].filter(Boolean).join(' ');
}

function Swatch({ especialidad }: { especialidad?: string }) {
  return <span className={`carga-swatch ${especialidad ? '' : 'carga-swatch-comun'}`.trim()} data-specialty={especialidad ? normalizeSpecialty(especialidad) : undefined} aria-hidden="true" />;
}

export default function CargaHorariaView({ data }: { data: CargaHorariaItem }) {
  const { slots } = data;
  const hours = useMemo(() => [...data.catalogo].sort((a, b) => a.numero - b.numero), [data.catalogo]);
  const comunes = slots.filter((slot) => slot.comun).length;
  const minutos = slots.reduce((total, slot) => total + Math.max(0, toMinutes(slot.horaFin) - toMinutes(slot.horaInicio)), 0);
  const porEspecialidad = useMemo(() => {
    const counts = new Map<string, number>();
    slots.filter((slot) => !slot.comun).forEach((slot) => {
      const name = slot.especialidadNombre ?? 'Sin especialidad';
      counts.set(name, (counts.get(name) ?? 0) + 1);
    });
    return Array.from(counts.entries()).sort((a, b) => a[0].localeCompare(b[0], 'es'));
  }, [slots]);
  const sabado = slots.filter((slot) => slot.diaSemana === SABADO);

  return (
    <div className="carga-horaria">
      <div className="carga-summary">
        <div><span>Horas cátedra</span><strong data-testid="carga-total">{slots.length}</strong></div>
        <div><span>Común</span><strong>{comunes}</strong></div>
        <div><span>Específicas</span><strong>{slots.length - comunes}</strong></div>
        <div><span>Horas reloj</span><strong>{formatMinutes(minutos)}</strong></div>
      </div>
      {porEspecialidad.length > 0 && <ul className="carga-chips" aria-label="Horas específicas por especialidad">
        {porEspecialidad.map(([name, count]) => <li key={name} className="carga-chip"><Swatch especialidad={name} />{name}: {count}</li>)}
      </ul>}
      <ul className="carga-legend" aria-label="Leyenda">
        <li><Swatch />Común</li>
        {porEspecialidad.map(([name]) => <li key={name}><Swatch especialidad={name} />{name}</li>)}
      </ul>
      <HorarioTablaGrid
        hours={hours}
        className="schedule-course-grid"
        wrapClassName="schedule-course-preview-wrap"
        renderCell={(day, hour) => {
          const slot = slots.find((item) => item.diaSemana === day && item.horaCatedraId === hour.id);
          if (!slot) return { content: null, className: 'carga-cell' };
          return {
            className: 'carga-cell',
            title: `${slot.materiaNombre} · ${cursoLabel(slot)}`,
            content: (
              <div className={`carga-slot ${slot.comun ? 'carga-slot-comun' : 'carga-slot-especifica'}`} data-specialty={slot.comun ? undefined : normalizeSpecialty(slot.especialidadNombre ?? '')}>
                <strong>{slot.materiaNombre}</strong>
                <small>{cursoLabel(slot)}</small>
                {slot.salaNombre && <small>{slot.salaNombre}</small>}
              </div>
            ),
          };
        }}
      />
      {sabado.length > 0 && <p className="muted-copy">También tiene clases el sábado: {sabado.map((slot) => `${slot.materiaNombre} (${cursoLabel(slot)}, ${slot.horaInicio}–${slot.horaFin})`).join('; ')}.</p>}
    </div>
  );
}

