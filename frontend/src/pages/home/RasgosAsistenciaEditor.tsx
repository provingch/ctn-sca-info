import { useEffect, useRef, useState } from 'react';
import type { CodigoConducta } from '../../api/home';

export interface RasgoEditorAlumno {
  id: number;
  nombre: string;
  apellido: string;
}

interface RasgosAsistenciaEditorProps {
  alumnos: RasgoEditorAlumno[];
  tema: string;
  onTemaChange: (value: string) => void;
  ausentes: number[];
  onAusenteChange: (alumnoId: number, ausente: boolean) => void;
  codigosPorAlumno: Record<number, string[]>;
  onCodigoChange: (alumnoId: number, codigo: string) => void;
  codigosConducta: CodigoConducta[];
  codigosConductaError?: string;
  onRetryCodigosConducta?: () => void;
  titulo?: string;
  descripcion?: string;
}

const SIN_CODIGOS_TEXTO = 'Sin códigos cargados. Los carga el evaluador o el administrador.';

function nombreCompleto(alumno: RasgoEditorAlumno) {
  return alumno.apellido ? `${alumno.apellido}, ${alumno.nombre}` : alumno.nombre;
}

export default function RasgosAsistenciaEditor({
  alumnos,
  tema,
  onTemaChange,
  ausentes,
  onAusenteChange,
  codigosPorAlumno,
  onCodigoChange,
  codigosConducta,
  codigosConductaError,
  onRetryCodigosConducta,
  titulo = 'Asistencia general y justificativos',
  descripcion = 'Tocá un alumno para marcarlo ausente. Tocá su zona de rasgos para asignarle un código.',
}: RasgosAsistenciaEditorProps) {
  const [openRasgosAlumnoId, setOpenRasgosAlumnoId] = useState<number | null>(null);
  const openWrapRef = useRef<HTMLDivElement | null>(null);
  const lastRasgosTriggerRef = useRef<HTMLButtonElement | null>(null);

  const totalAusentes = alumnos.filter((alumno) => ausentes.includes(alumno.id)).length;
  const totalPresentes = alumnos.length - totalAusentes;
  const porcentajeAsistencia = alumnos.length > 0 ? Math.round((totalPresentes * 100) / alumnos.length) : 0;
  const porcentajeAusencia = alumnos.length > 0 ? 100 - porcentajeAsistencia : 0;
  const sinCodigosCargados = !codigosConductaError && codigosConducta.length === 0;
  const puedeAsignarRasgos = codigosConducta.length > 0;
  const tituloRasgosDeshabilitado = codigosConductaError
    ? 'No se pudieron cargar los rasgos conductuales.'
    : SIN_CODIGOS_TEXTO;

  const descripcionPorCodigo = new Map(codigosConducta.map((item) => [item.codigo, item.descripcion]));
  const alumnosConRasgos = alumnos.filter((alumno) => (codigosPorAlumno[alumno.id] ?? []).length > 0);

  useEffect(() => {
    if (openRasgosAlumnoId === null) return;
    function closeOutside(event: PointerEvent) {
      if (!openWrapRef.current?.contains(event.target as Node)) setOpenRasgosAlumnoId(null);
    }
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setOpenRasgosAlumnoId(null);
        lastRasgosTriggerRef.current?.focus();
      }
    }
    document.addEventListener('pointerdown', closeOutside);
    document.addEventListener('keydown', closeOnEscape);
    return () => {
      document.removeEventListener('pointerdown', closeOutside);
      document.removeEventListener('keydown', closeOnEscape);
    };
  }, [openRasgosAlumnoId]);

  function toggleRasgosMenu(alumnoId: number, triggerEl: HTMLButtonElement) {
    lastRasgosTriggerRef.current = triggerEl;
    setOpenRasgosAlumnoId((current) => (current === alumnoId ? null : alumnoId));
  }

  return <>
    <div className="class-card">
      <div className="class-field class-field--full">
        <label htmlFor="temaRasgo">Contenido específico desarrollado</label>
        <input id="temaRasgo" name="tema" maxLength={150} value={tema} onChange={(event) => onTemaChange(event.target.value)} placeholder="Ej.: Integrales definidas y aplicaciones" required />
      </div>
    </div>

    <div className="class-card">
      <h3>{titulo}</h3>
      <div className="class-attendance-toolbar">
        <span className="student-pill">Habilitados: <strong>{alumnos.length}</strong></span>
        <p>{descripcion}</p>
      </div>

      {alumnos.length === 0 ? (
        <p>No hay alumnos habilitados para tomar asistencia.</p>
      ) : (
        <div className="attendance-grid" role="group" aria-label="Asistencia de alumnos">
          {alumnos.map((alumno, index) => {
            const ausente = ausentes.includes(alumno.id);
            const codigos = codigosPorAlumno[alumno.id] ?? [];
            const nombre = nombreCompleto(alumno);
            const estado = ausente ? 'ausente' : 'presente';
            const badge = codigos.length > 0 ? `${codigos[0]}${codigos.length > 1 ? ` +${codigos.length - 1}` : ''}` : null;
            const popoverAbierto = openRasgosAlumnoId === alumno.id;
            return (
              <div
                key={alumno.id}
                className={`attendance-chip-wrap ${ausente ? 'tone-danger' : 'tone-success'} ${popoverAbierto ? 'rasgos-open' : ''}`}
                ref={popoverAbierto ? openWrapRef : undefined}
              >
                <button
                  type="button"
                  className="attendance-chip"
                  aria-pressed={ausente}
                  title={nombre}
                  aria-label={`${nombre}, ${estado}`}
                  onClick={() => onAusenteChange(alumno.id, !ausente)}
                >
                  <span className="attendance-chip-order" aria-hidden="true">{index + 1}.</span>
                  <span className="attendance-chip-name" aria-hidden="true">{nombre}</span>
                </button>
                <button
                  type="button"
                  className="attendance-chip-rasgos"
                  disabled={!puedeAsignarRasgos}
                  title={puedeAsignarRasgos ? undefined : tituloRasgosDeshabilitado}
                  aria-haspopup="listbox"
                  aria-expanded={popoverAbierto}
                  aria-label={`Rasgos conductuales de ${nombre}`}
                  onClick={(event) => toggleRasgosMenu(alumno.id, event.currentTarget)}
                >
                  {badge
                    ? <span className="attendance-chip-rasgos-badge" aria-hidden="true">{badge}</span>
                    : <span className="attendance-chip-rasgos-none" aria-hidden="true">N</span>}
                </button>
                {popoverAbierto && (
                  <div className="rasgos-popover" role="listbox" aria-multiselectable="true" aria-label={`Códigos para ${nombre}`}>
                    {codigosConducta.map((item) => {
                      const selected = codigos.includes(item.codigo);
                      return (
                        <button
                          type="button"
                          role="option"
                          aria-selected={selected}
                          key={item.codigo}
                          className={`rasgos-popover-option ${selected ? 'selected' : ''}`}
                          onClick={() => onCodigoChange(alumno.id, item.codigo)}
                        >
                          <span className="rasgos-popover-option-code">{item.codigo}</span>
                          <span className="rasgos-popover-option-desc">{item.descripcion}</span>
                          {selected && <span className="rasgos-popover-option-check" aria-hidden="true">✓</span>}
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      <div className="attendance-summary" role="status" aria-live="polite" aria-atomic="true">
        <strong>Resumen de asistencia</strong>
        {alumnos.length > 0 ? <>
          <p>Presentes: <strong>{totalPresentes} de {alumnos.length} ({porcentajeAsistencia}%)</strong> · Ausentes: <strong>{totalAusentes} ({porcentajeAusencia}%)</strong></p>
          <small>Se actualiza al marcar ausentes. Incluye solo alumnos habilitados.</small>
        </> : <p>No hay alumnos habilitados para calcular la asistencia.</p>}
      </div>
    </div>

    <div className="class-card">
      <h3>Rasgos conductuales</h3>

      {codigosConductaError && (
        <div className="notice error" style={{ marginBottom: 12, display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
          <p style={{ margin: 0, flex: 1 }}>No se pudieron cargar los rasgos conductuales. {codigosConductaError}</p>
          {onRetryCodigosConducta && <button type="button" className="button secondary" onClick={onRetryCodigosConducta}>Reintentar</button>}
        </div>
      )}
      {sinCodigosCargados && <p className="rasgos-conducta-empty">{SIN_CODIGOS_TEXTO}</p>}

      {alumnosConRasgos.length === 0 ? (
        <p className="rasgos-empty">No hay rasgos registrados en esta clase. Se asignan desde la zona de rasgos del chip de cada alumno.</p>
      ) : (
        <ul className="rasgos-list">
          {alumnosConRasgos.map((alumno) => {
            const codigos = codigosPorAlumno[alumno.id] ?? [];
            const nombre = nombreCompleto(alumno);
            return (
              <li className="rasgo-row" key={alumno.id}>
                <div className="rasgo-row-head">
                  <strong>{nombre}</strong>
                  <div className="rasgo-chips">
                    {codigos.map((codigo) => (
                      <span className="rasgo-chip" key={codigo}>
                        {codigo}
                        <button type="button" onClick={() => onCodigoChange(alumno.id, codigo)} aria-label={`Quitar ${codigo} de ${nombre}`}>×</button>
                      </span>
                    ))}
                  </div>
                </div>
                <p className="rasgo-row-meanings">
                  {codigos.map((codigo) => `${codigo}: ${descripcionPorCodigo.get(codigo) ?? 'Sin descripción'}`).join(' · ')}
                </p>
              </li>
            );
          })}
        </ul>
      )}

    </div>

  </>;
}
