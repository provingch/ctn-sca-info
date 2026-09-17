import { useMemo, useState } from 'react';
import AnimatedSelect from '../../components/AnimatedSelect';
import useAccessibleDialog from '../../hooks/useAccessibleDialog';
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
  descripcion = 'Tocá un alumno para marcarlo ausente. Los no marcados se guardan como presentes.',
}: RasgosAsistenciaEditorProps) {
  const [showCodeHelp, setShowCodeHelp] = useState(false);
  const codeHelpDialogRef = useAccessibleDialog(showCodeHelp, () => setShowCodeHelp(false));
  const [addingRasgo, setAddingRasgo] = useState(false);
  const [nuevoAlumnoId, setNuevoAlumnoId] = useState('');
  const [nuevoCodigo, setNuevoCodigo] = useState('');

  const totalAusentes = alumnos.filter((alumno) => ausentes.includes(alumno.id)).length;
  const totalPresentes = alumnos.length - totalAusentes;
  const porcentajeAsistencia = alumnos.length > 0 ? Math.round((totalPresentes * 100) / alumnos.length) : 0;
  const porcentajeAusencia = alumnos.length > 0 ? 100 - porcentajeAsistencia : 0;
  const sinCodigosCargados = !codigosConductaError && codigosConducta.length === 0;
  const puedeAgregarRasgo = codigosConducta.length > 0;

  const descripcionPorCodigo = useMemo(
    () => new Map(codigosConducta.map((item) => [item.codigo, item.descripcion])),
    [codigosConducta],
  );
  const alumnosConRasgos = alumnos.filter((alumno) => (codigosPorAlumno[alumno.id] ?? []).length > 0);
  const codigosYaAsignados = nuevoAlumnoId ? (codigosPorAlumno[Number(nuevoAlumnoId)] ?? []) : [];
  const codigosDisponibles = codigosConducta.filter((item) => !codigosYaAsignados.includes(item.codigo));

  function abrirAgregarRasgo() {
    setNuevoAlumnoId('');
    setNuevoCodigo('');
    setAddingRasgo(true);
  }

  function cerrarAgregarRasgo() {
    setAddingRasgo(false);
    setNuevoAlumnoId('');
    setNuevoCodigo('');
  }

  function confirmarNuevoRasgo() {
    if (!nuevoAlumnoId || !nuevoCodigo) return;
    onCodigoChange(Number(nuevoAlumnoId), nuevoCodigo);
    cerrarAgregarRasgo();
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
            return (
              <button
                type="button"
                key={alumno.id}
                className={`attendance-chip ${ausente ? 'tone-danger' : 'tone-success'}`}
                aria-pressed={ausente}
                title={nombre}
                aria-label={`${nombre}, ${estado}${codigos.length > 0 ? `, rasgos ${codigos.join(', ')}` : ''}`}
                onClick={() => onAusenteChange(alumno.id, !ausente)}
              >
                <span className="attendance-chip-order" aria-hidden="true">{index + 1}.</span>
                <span className="attendance-chip-name" aria-hidden="true">{nombre}</span>
                {badge && <span className="attendance-chip-badge" aria-hidden="true">{badge}</span>}
              </button>
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
      <div className="class-card-head">
        <h3>Rasgos conductuales</h3>
        <button type="button" className="button secondary" disabled={!puedeAgregarRasgo} onClick={abrirAgregarRasgo}>Agregar rasgo</button>
      </div>

      {codigosConductaError && (
        <div className="notice error" style={{ marginBottom: 12, display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
          <p style={{ margin: 0, flex: 1 }}>No se pudieron cargar los rasgos conductuales. {codigosConductaError}</p>
          {onRetryCodigosConducta && <button type="button" className="button secondary" onClick={onRetryCodigosConducta}>Reintentar</button>}
        </div>
      )}
      {sinCodigosCargados && <p className="rasgos-conducta-empty">{SIN_CODIGOS_TEXTO}</p>}

      {addingRasgo && (
        <div className="rasgo-add-panel">
          <div className="class-grid">
            <div className="class-field">
              <label>Alumno</label>
              <AnimatedSelect
                ariaLabel="Alumno para el nuevo rasgo"
                value={nuevoAlumnoId}
                placeholder="Elegí un alumno…"
                onChange={(value) => { setNuevoAlumnoId(value); setNuevoCodigo(''); }}
                options={alumnos.map((alumno) => ({ value: alumno.id, label: nombreCompleto(alumno) }))}
              />
            </div>
            <div className="class-field">
              <label>Código</label>
              <AnimatedSelect
                ariaLabel="Código del nuevo rasgo"
                value={nuevoCodigo}
                disabled={!nuevoAlumnoId}
                placeholder={nuevoAlumnoId ? 'Elegí un código…' : 'Elegí un alumno primero'}
                onChange={setNuevoCodigo}
                options={codigosDisponibles.map((item) => ({ value: item.codigo, label: `${item.codigo} — ${item.descripcion}` }))}
              />
            </div>
          </div>
          <div className="rasgo-add-actions">
            <button type="button" className="button secondary" onClick={cerrarAgregarRasgo}>Cancelar</button>
            <button type="button" className="button" disabled={!nuevoAlumnoId || !nuevoCodigo} onClick={confirmarNuevoRasgo}>Confirmar</button>
          </div>
        </div>
      )}

      {alumnosConRasgos.length === 0 ? (
        <p className="rasgos-empty">No hay rasgos registrados en esta clase.</p>
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

      <button type="button" className="button secondary" onClick={() => setShowCodeHelp(true)}>¿Qué significa cada código?</button>
    </div>

    {showCodeHelp && <div ref={codeHelpDialogRef} role="dialog" aria-modal="true" aria-labelledby="code-help-title" tabIndex={-1} style={{ position: 'fixed', inset: 0, zIndex: 100, display: 'grid', placeItems: 'center', padding: 20, background: 'rgba(0, 0, 0, .55)' }} onClick={() => setShowCodeHelp(false)}>
      <section className="panel" style={{ width: 'min(620px, 100%)', maxHeight: '80vh', overflow: 'auto' }} onClick={(event) => event.stopPropagation()}>
        <div className="class-card-head"><h3 id="code-help-title">Significado de códigos</h3><button type="button" className="button secondary" data-dialog-initial-focus onClick={() => setShowCodeHelp(false)}>Cerrar</button></div>
        {codigosConductaError ? (
          <div className="notice error" style={{ display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
            <p style={{ margin: 0, flex: 1 }}>No se pudieron cargar los rasgos conductuales. {codigosConductaError}</p>
            {onRetryCodigosConducta && <button type="button" className="button secondary" onClick={onRetryCodigosConducta}>Reintentar</button>}
          </div>
        ) : sinCodigosCargados ? (
          <p>{SIN_CODIGOS_TEXTO}</p>
        ) : (
          <table className="table table-striped"><caption className="visually-hidden">Códigos de rasgos conductuales</caption><thead><tr><th>Código</th><th>Significado</th></tr></thead><tbody>{codigosConducta.map((item) => <tr key={item.codigo}><td><strong>{item.codigo}</strong></td><td>{item.descripcion}</td></tr>)}</tbody></table>
        )}
      </section>
    </div>}
  </>;
}
