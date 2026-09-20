import { useId, useRef, useState } from 'react';
import './PlanFileDropzone.css';

export default function PlanFileDropzone({ file, disabled, onChange, onError }: {
  file: File | null; disabled: boolean; onChange: (file: File | null) => void; onError: (message: string) => void;
}) {
  const input = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);
  const hint = useId();
  function select(files: File[]) {
    if (disabled || !files.length) return;
    if (files.length !== 1) { onError('Seleccioná un solo archivo de plan curricular.'); return; }
    if (!/\.xlsx$/i.test(files[0].name)) { onError('El plan debe ser un archivo Excel (.xlsx).'); return; }
    if (!files[0].size) { onError('El archivo está vacío. Seleccioná la plantilla completada.'); return; }
    onChange(files[0]);
  }
  return <div className="plan-file-picker">
    <input ref={input} type="file" accept=".xlsx" hidden disabled={disabled} aria-label="Seleccionar archivo del plan curricular" onChange={(event) => { select(Array.from(event.target.files ?? [])); event.target.value = ''; }} />
    <button type="button" className={`plan-file-dropzone${dragging && !disabled ? ' dragging' : ''}`} disabled={disabled} aria-describedby={hint}
      onClick={() => input.current?.click()}
      onDragOver={(event) => { event.preventDefault(); event.dataTransfer.dropEffect = disabled ? 'none' : 'copy'; if (!disabled) setDragging(true); }}
      onDragLeave={(event) => { if (!event.currentTarget.contains(event.relatedTarget as Node | null)) setDragging(false); }}
      onDrop={(event) => { event.preventDefault(); setDragging(false); select(Array.from(event.dataTransfer.files)); }}>
      <svg aria-hidden="true" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6"><path d="M12 16V3m-5 5 5-5 5 5M4 15v5h16v-5" /></svg>
      <strong>{file ? file.name : 'Arrastrá tu plan curricular aquí'}</strong>
      <span>{file ? 'Hacé clic o arrastrá otro archivo para reemplazarlo' : 'o hacé clic para seleccionar el archivo'}</span>
      <small id={hint}>{file ? `${Math.max(1, Math.ceil(file.size / 1024))} KB · Excel (.xlsx)` : 'Plantilla completada en formato Excel (.xlsx)'}</small>
    </button>
    {file && <div className="plan-file-selection"><span role="status">Archivo listo para subir</span><button type="button" className="button secondary" disabled={disabled} onClick={() => onChange(null)}>Quitar archivo</button></div>}
  </div>;
}
