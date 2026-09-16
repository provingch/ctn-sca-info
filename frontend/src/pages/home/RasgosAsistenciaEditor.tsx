import { useState } from 'react';
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
  titulo?: string;
  descripcion?: string;
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
  titulo = 'Asistencia general y justificativos',
  descripcion = 'Marcá ausentes en la lista. Los no marcados se guardan como presentes.',
}: RasgosAsistenciaEditorProps) {
  const [showCodeHelp, setShowCodeHelp] = useState(false);
  const codeHelpDialogRef = useAccessibleDialog(showCodeHelp, () => setShowCodeHelp(false));
  const totalAusentes = alumnos.filter((alumno) => ausentes.includes(alumno.id)).length;
  const totalPresentes = alumnos.length - totalAusentes;
  const porcentajeAsistencia = alumnos.length > 0 ? Math.round((totalPresentes * 100) / alumnos.length) : 0;
  const porcentajeAusencia = alumnos.length > 0 ? 100 - porcentajeAsistencia : 0;

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
      <div className="table-responsive" style={{ marginBottom: 8 }}>
        <table className="table table-striped" id="tablaAsistencia">
          <caption className="visually-hidden">Asistencia y rasgos conductuales de alumnos</caption>
          <thead><tr><th>#</th><th>Apellido(s) y nombre(s)</th><th style={{ textAlign: 'right', width: 140 }}>Estado (P/A)</th><th style={{ width: 190 }}>Rasgos conductuales</th></tr></thead>
          <tbody>
            {alumnos.map((alumno, index) => (
              <tr key={alumno.id}>
                <td>{index + 1}</td>
                <td>{alumno.apellido ? `${alumno.apellido}, ${alumno.nombre}` : alumno.nombre}</td>
                <td style={{ textAlign: 'right' }}>
                  <label style={{ display: 'inline-flex', gap: 8, alignItems: 'center' }}>
                    <input className="ausente-checkbox" type="checkbox" checked={ausentes.includes(alumno.id)} onChange={(event) => onAusenteChange(alumno.id, event.target.checked)} />
                    Ausente
                  </label>
                </td>
                <td className="rasgos-conductuales-cell">
                  <AnimatedSelect multiple portal ariaLabel={`Rasgos conductuales de ${alumno.nombre} ${alumno.apellido}`} value={codigosPorAlumno[alumno.id] ?? []} placeholder="Seleccione…" onChange={(codigo) => onCodigoChange(alumno.id, codigo)} options={codigosConducta.map((item) => ({ value: item.codigo, label: item.codigo }))} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <button type="button" className="button secondary" onClick={() => setShowCodeHelp(true)}>¿Qué significa cada código?</button>
      <div className="attendance-summary" role="status" aria-live="polite" aria-atomic="true">
        <strong>Resumen de asistencia</strong>
        {alumnos.length > 0 ? <>
          <p>Presentes: <strong>{totalPresentes} de {alumnos.length} ({porcentajeAsistencia}%)</strong> · Ausentes: <strong>{totalAusentes} ({porcentajeAusencia}%)</strong></p>
          <small>Se actualiza al marcar ausentes. Incluye solo alumnos habilitados.</small>
        </> : <p>No hay alumnos habilitados para calcular la asistencia.</p>}
      </div>
    </div>

    {showCodeHelp && <div ref={codeHelpDialogRef} role="dialog" aria-modal="true" aria-labelledby="code-help-title" tabIndex={-1} style={{ position: 'fixed', inset: 0, zIndex: 100, display: 'grid', placeItems: 'center', padding: 20, background: 'rgba(0, 0, 0, .55)' }} onClick={() => setShowCodeHelp(false)}>
      <section className="panel" style={{ width: 'min(620px, 100%)', maxHeight: '80vh', overflow: 'auto' }} onClick={(event) => event.stopPropagation()}>
        <div className="class-card-head"><h3 id="code-help-title">Significado de códigos</h3><button type="button" className="button secondary" data-dialog-initial-focus onClick={() => setShowCodeHelp(false)}>Cerrar</button></div>
        <table className="table table-striped"><caption className="visually-hidden">Códigos de rasgos conductuales</caption><thead><tr><th>Código</th><th>Significado</th></tr></thead><tbody>{codigosConducta.map((item) => <tr key={item.codigo}><td><strong>{item.codigo}</strong></td><td>{item.descripcion}</td></tr>)}</tbody></table>
      </section>
    </div>}
  </>;
}
