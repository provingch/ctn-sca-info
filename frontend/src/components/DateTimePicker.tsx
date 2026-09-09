import DatePicker from './DatePicker';

export default function DateTimePicker({ value, onChange, ariaLabel, disabled = false }: {
  value: string; onChange: (value: string) => void; ariaLabel: string; disabled?: boolean;
}) {
  const [date = '', time = ''] = value.split('T');
  return <div className="date-time-picker">
    <DatePicker ariaLabel={ariaLabel + ': fecha'} value={date} disabled={disabled} onChange={(next) => onChange(next || time ? next + 'T' + time : '')} />
    <input type="time" aria-label={ariaLabel + ': hora'} value={time} disabled={disabled} onChange={(event) => onChange(date || event.target.value ? date + 'T' + event.target.value : '')} />
  </div>;
}
