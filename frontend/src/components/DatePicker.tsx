import { useEffect, useId, useRef, useState, type KeyboardEvent } from 'react';
import './DatePicker.css';

const isoDate = (date: Date) => [String(date.getFullYear()).padStart(4, '0'), String(date.getMonth() + 1).padStart(2, '0'), String(date.getDate()).padStart(2, '0')].join('-');
const parseDate = (value: string) => {
  const [year, month, day] = value.split('-').map(Number);
  return year && month && day ? new Date(year, month - 1, day, 12) : new Date();
};
const monthLabel = new Intl.DateTimeFormat('es', { month: 'long', year: 'numeric' });
const dayLabel = new Intl.DateTimeFormat('es', { dateStyle: 'full' });
const displayDate = new Intl.DateTimeFormat('es', { day: '2-digit', month: '2-digit', year: 'numeric' });
const weekdays = ['Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sá', 'Do'];

export default function DatePicker({ value, onChange, ariaLabel, disabled = false }: {
  value: string; onChange: (value: string) => void; ariaLabel: string; disabled?: boolean;
}) {
  const id = useId();
  const dialogRef = useRef<HTMLDialogElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const dayRefs = useRef(new Map<string, HTMLButtonElement>());
  const [open, setOpen] = useState(false);
  const [activeDate, setActiveDate] = useState(() => parseDate(value));
  const [position, setPosition] = useState({ top: 0, left: 0 });
  const today = isoDate(new Date());
  const activeIso = isoDate(activeDate);
  const monthStart = new Date(activeDate.getFullYear(), activeDate.getMonth(), 1, 12);
  const offset = (monthStart.getDay() + 6) % 7;
  const days = Array.from({ length: 42 }, (_, index) => new Date(activeDate.getFullYear(), activeDate.getMonth(), index - offset + 1, 12));

  function close() {
    dialogRef.current?.close();
    setOpen(false);
    triggerRef.current?.focus();
  }

  function show() {
    if (disabled) return;
    const bounds = triggerRef.current!.getBoundingClientRect();
    const width = Math.min(336, window.innerWidth - 24);
    setPosition({
      left: Math.max(12, Math.min(bounds.left, window.innerWidth - width - 12)),
      top: Math.max(12, Math.min(bounds.bottom + 8, window.innerHeight - 440)),
    });
    setActiveDate(parseDate(value));
    setOpen(true);
  }

  useEffect(() => {
    if (!open || disabled) {
      dialogRef.current?.close();
      return;
    }
    dialogRef.current?.showModal();
    const dismiss = () => { dialogRef.current?.close(); setOpen(false); };
    window.addEventListener('resize', dismiss);
    return () => window.removeEventListener('resize', dismiss);
  }, [open, disabled]);

  useEffect(() => {
    if (open && !disabled) dayRefs.current.get(activeIso)?.focus();
  }, [activeIso, open, disabled]);

  function choose(date: string) {
    if (disabled) return;
    onChange(date);
    close();
  }

  function moveMonth(amount: number) {
    const lastDay = new Date(activeDate.getFullYear(), activeDate.getMonth() + amount + 1, 0).getDate();
    setActiveDate(new Date(activeDate.getFullYear(), activeDate.getMonth() + amount, Math.min(activeDate.getDate(), lastDay), 12));
  }

  function handleDayKey(event: KeyboardEvent<HTMLButtonElement>) {
    const step = ({ ArrowLeft: -1, ArrowRight: 1, ArrowUp: -7, ArrowDown: 7 } as Record<string, number>)[event.key];
    if (step !== undefined || event.key === 'Home' || event.key === 'End') {
      event.preventDefault();
      const weekday = (activeDate.getDay() + 6) % 7;
      const delta = step ?? (event.key === 'Home' ? -weekday : 6 - weekday);
      setActiveDate(new Date(activeDate.getFullYear(), activeDate.getMonth(), activeDate.getDate() + delta, 12));
    } else if (event.key === 'PageUp' || event.key === 'PageDown') {
      event.preventDefault();
      moveMonth((event.key === 'PageUp' ? -1 : 1) * (event.shiftKey ? 12 : 1));
    }
  }

  return <div className="date-picker">
    <button ref={triggerRef} type="button" className="date-picker-trigger" aria-label={ariaLabel} aria-haspopup="dialog" aria-expanded={open && !disabled} aria-controls={id} disabled={disabled} onClick={show}>
      <span className={value ? '' : 'placeholder'}>{value ? displayDate.format(parseDate(value)) : 'Seleccioná una fecha'}</span>
      <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="3" /><path d="M7 3v4m10-4v4M3 11h18m-14 4h3m4 0h3" /></svg>
    </button>
    <dialog ref={dialogRef} id={id} className="date-picker-dialog" style={position} aria-label={ariaLabel} onCancel={() => setOpen(false)} onClose={() => setOpen(false)} onClick={(event) => {
      if (event.target !== event.currentTarget) return;
      const rect = event.currentTarget.getBoundingClientRect();
      if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) close();
    }}>
      <div className="date-picker-header">
        <button type="button" aria-label="Mes anterior" onClick={() => moveMonth(-1)}>‹</button>
        <h2 aria-live="polite">{monthLabel.format(activeDate)}</h2>
        <button type="button" aria-label="Mes siguiente" onClick={() => moveMonth(1)}>›</button>
      </div>
      <table className="date-picker-grid" role="grid" aria-label="Elegir fecha">
        <thead><tr>{weekdays.map((day) => <th key={day} scope="col">{day}</th>)}</tr></thead>
        <tbody>{Array.from({ length: 6 }, (_, week) => <tr key={week}>{days.slice(week * 7, week * 7 + 7).map((date) => {
          const iso = isoDate(date);
          return <td key={iso}><button
            ref={(element) => { if (element) dayRefs.current.set(iso, element); else dayRefs.current.delete(iso); }}
            type="button" tabIndex={iso === activeIso ? 0 : -1} aria-label={dayLabel.format(date)} aria-pressed={iso === value} aria-current={iso === today ? 'date' : undefined}
            className={date.getMonth() !== activeDate.getMonth() ? 'outside-month' : ''}
            onKeyDown={handleDayKey} onClick={() => choose(iso)}
          >{date.getDate()}</button></td>;
        })}</tr>)}</tbody>
      </table>
      <div className="date-picker-footer"><button type="button" disabled={!value} onClick={() => choose('')}>Borrar</button><button type="button" onClick={() => choose(today)}>Hoy</button><button type="button" onClick={close}>Cerrar</button></div>
    </dialog>
  </div>;
}
