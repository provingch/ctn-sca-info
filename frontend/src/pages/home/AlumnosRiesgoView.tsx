import { Fragment, useEffect, useState } from 'react';
import { ApiError } from '../../api/client';
import { getAlumnosRiesgo, type AlumnoRiesgoDto, type AlumnosRiesgoResponse } from '../../api/home';
import ContentState from '../../components/ui/ContentState';

const MOTIVO_LABEL: Record<string, string> = { TAREAS: 'Tareas', CONDUCTA: 'Conducta' };
const MOTIVO_TONE: Record<string, string> = { TAREAS: 'tone-danger', CONDUCTA: 'tone-warning' };

export default function AlumnosRiesgoView({ especialidadNombre, nivel, seccion }: {
  especialidadNombre: string | null;
  nivel: number | null;
  seccion: string | number | '';
}) {
  const [data, setData] = useState<AlumnosRiesgoResponse | null>(null);
  const [error, setError] = useState('');
  const [expandedId, setExpandedId] = useState<number | null>(null);

  useEffect(() => {
    let active = true;
    void getAlumnosRiesgo()
      .then((result) => { if (active) { setData(result); setError(''); } })
      .catch((err) => { if (active) setError(err instanceof ApiError ? err.message : 'No se pudieron cargar los alumnos en riesgo.'); });
    return () => { active = false; };
  }, []);

  if (error) return <ContentState tone="error" title={error} />;
  if (!data) return <ContentState tone="loading" title="Cargando alumnos en riesgo…" />;

  const matchesFilter = (item: AlumnoRiesgoDto) => {
    if (especialidadNombre && item.especialidadNombre !== especialidadNombre) return false;
    if (nivel != null && parseInt(item.cursoNombre, 10) !== nivel) return false;
    if (seccion && item.seccion !== String(seccion)) return false;
    return true;
  };

  const alumnos = data.alumnos.filter(matchesFilter);

  return <>
    <section className="summary-grid">
      <article className="metric"><span>Umbral tareas</span><strong>{'>'} {data.umbralTareas}</strong></article>
      <article className="metric"><span>Umbral conducta</span><strong>{'>'} {data.umbralConducta}</strong></article>
      <article className="metric"><span>Alumnos en riesgo</span><strong>{alumnos.length}</strong></article>
    </section>
    {alumnos.length === 0 ? (
      <ContentState
        tone="empty"
        title="Ningún alumno supera los umbrales"
        detail={`Umbral: más de ${data.umbralTareas} tareas no entregadas o más de ${data.umbralConducta} notas conductuales.`}
      />
    ) : (
      <div className="table-wrap">
        <table className="grade-table alumnos-riesgo-table">
          <caption className="visually-hidden">Alumnos en riesgo</caption>
          <thead>
            <tr>
              <th scope="col">Alumno</th>
              <th scope="col">Curso</th>
              <th scope="col">Tareas no entregadas</th>
              <th scope="col">Notas conductuales</th>
              <th scope="col">Motivo</th>
              <th scope="col" aria-hidden="true"></th>
            </tr>
          </thead>
          <tbody>
            {alumnos.map((alumno) => {
              const isExpanded = expandedId === alumno.alumnoId;
              return (
                <Fragment key={alumno.alumnoId}>
                  <tr>
                    <td>{alumno.nombreCompleto}</td>
                    <td>{alumno.cursoNombre} {alumno.seccion}</td>
                    <td>{alumno.tareasNoEntregadas}</td>
                    <td>{alumno.notasConductuales}</td>
                    <td>
                      {alumno.motivos.map((motivo) => (
                        <span key={motivo} className={`launcher-badge ${MOTIVO_TONE[motivo] ?? 'tone-warning'}`}>{MOTIVO_LABEL[motivo] ?? motivo}</span>
                      ))}
                    </td>
                    <td>
                      <button
                        type="button"
                        className="button secondary"
                        aria-expanded={isExpanded}
                        onClick={() => setExpandedId(isExpanded ? null : alumno.alumnoId)}
                      >
                        {isExpanded ? 'Ocultar' : 'Ver desglose'}
                      </button>
                    </td>
                  </tr>
                  {isExpanded && (
                    <tr>
                      <td colSpan={6}>
                        <ul>
                          {alumno.desglose.map((materia) => (
                            <li key={materia.materiaId}>
                              <strong>{materia.materiaNombre}</strong>: {materia.tareasNoEntregadas} tareas no entregadas, {materia.notasConductuales} notas conductuales
                            </li>
                          ))}
                        </ul>
                      </td>
                    </tr>
                  )}
                </Fragment>
              );
            })}
          </tbody>
        </table>
      </div>
    )}
  </>;
}
