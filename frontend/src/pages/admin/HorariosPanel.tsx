import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError } from '../../api/client';
import { downloadHorarioCurso, getHoraCatedraCatalog, getHorarioResumen, type HorarioResumenCursoItem, type HoraCatedraItem } from '../../api/admin';
import ContentState from '../../components/ui/ContentState';
import { groupSchedulesBySpecialty } from './adminFormatters';
import HorarioCursoPage from './HorarioCursoPage';
import SpecialtyIcon from '../../components/SpecialtyIcon';

export default function HorariosPanel({ status }: { status: (message: string) => void }) {
  const { cursoId } = useParams();
  const [items, setItems] = useState<HorarioResumenCursoItem[] | null>(null);
  const [hours, setHours] = useState<HoraCatedraItem[]>([]);
  const [error, setError] = useState('');
  const [downloadingId, setDownloadingId] = useState<number | null>(null);

  const load = useCallback(async () => {
    try {
      const [summary, catalog] = await Promise.all([getHorarioResumen(), getHoraCatedraCatalog()]);
      setItems(summary);
      setHours(catalog);
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No se pudo cargar el resumen de horarios.');
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const refreshSummary = useCallback(async () => {
    try {
      setItems(await getHorarioResumen());
    } catch (reason) {
      status(reason instanceof ApiError ? reason.message : 'No se pudo actualizar el resumen de horarios.');
    }
  }, [status]);

  const groups = useMemo(() => groupSchedulesBySpecialty(items ?? []), [items]);

  async function download(courseItem: HorarioResumenCursoItem) {
    setDownloadingId(courseItem.cursoId);
    try {
      await downloadHorarioCurso(courseItem.cursoId);
      status(`Horario de ${courseItem.cursoDescripcion} descargado.`);
    } catch (reason) {
      status(reason instanceof ApiError ? reason.message : 'No se pudo descargar el horario.');
    } finally {
      setDownloadingId(null);
    }
  }

  if (!items) {
    return <ContentState tone={error ? 'error' : 'loading'} title={error || 'Cargando horarios…'} detail={error ? 'Volvé a intentarlo recargando esta página.' : 'Estamos agrupando los cursos por especialidad.'} />;
  }

  if (items.length === 0) {
    return <ContentState title="No hay cursos disponibles" detail="Los cursos con horarios aparecerán en este panel." />;
  }

  if (cursoId) {
    return <HorarioCursoPage cursoId={cursoId} summary={items} hours={hours} status={status} refreshSummary={refreshSummary} />;
  }

  return (
    <div className="admin-summary-groups">
      {groups.map((group) => (
        <section className="panel admin-summary-section" key={group.specialty}>
          <header className="admin-summary-heading">
            <div>
              <span>Especialidad</span>
              <h2 className="specialty-card-title"><SpecialtyIcon name={group.specialty} />{group.specialty}</h2>
            </div>
            <strong>{group.courses.length} {group.courses.length === 1 ? 'curso' : 'cursos'}</strong>
          </header>
          <div className="admin-list">
            {group.courses.map((courseItem) => (
              <div key={courseItem.cursoId}>
                <span>
                  <strong>{courseItem.cursoDescripcion}</strong>
                  <small>{courseItem.cantidadSlotsCargados} {courseItem.cantidadSlotsCargados === 1 ? 'bloque cargado' : 'bloques cargados'}</small>
                </span>
                <span className="admin-actions">
                  <Link className="button secondary" to={`/admin/horarios/${courseItem.cursoId}`}>Abrir horario</Link>
                  <button className="button secondary" type="button" disabled={downloadingId === courseItem.cursoId} onClick={() => void download(courseItem)}>
                    {downloadingId === courseItem.cursoId ? 'Descargando…' : 'Descargar'}
                  </button>
                </span>
              </div>
            ))}
          </div>
        </section>
      ))}
    </div>
  );
}
