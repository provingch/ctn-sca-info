import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError } from '../../api/client';
import { downloadHorarioCurso, downloadHorarioCursoPdf, downloadHorarioEspecialidad, getHoraCatedraCatalog, getHorarioResumen, type HorarioResumenCursoItem, type HoraCatedraItem } from '../../api/admin';
import ContentState from '../../components/ui/ContentState';
import { groupSchedulesBySpecialty } from './adminFormatters';
import HorarioCursoPage from './HorarioCursoPage';
import SpecialtyIcon from '../../components/SpecialtyIcon';

export default function HorariosPanel({ status }: { status: (message: string) => void }) {
  const { cursoId } = useParams();
  const [items, setItems] = useState<HorarioResumenCursoItem[] | null>(null);
  const [hours, setHours] = useState<HoraCatedraItem[]>([]);
  const [error, setError] = useState('');
  const [downloadingKey, setDownloadingKey] = useState<string | null>(null);

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

  async function downloadCourse(courseItem: HorarioResumenCursoItem, formato: 'xlsx' | 'pdf') {
    setDownloadingKey(`course-${courseItem.cursoId}-${formato}`);
    try {
      if (formato === 'pdf') {
        await downloadHorarioCursoPdf(courseItem.cursoId);
      } else {
        await downloadHorarioCurso(courseItem.cursoId);
      }
      status(`Horario de ${courseItem.cursoDescripcion} descargado en ${formato.toUpperCase()}.`);
    } catch (reason) {
      status(reason instanceof ApiError ? reason.message : 'No se pudo descargar el horario.');
    } finally {
      setDownloadingKey(null);
    }
  }

  async function downloadSpecialty(specialtyId: number | null, specialty: string, formato: 'xlsx' | 'pdf') {
    if (specialtyId == null) return;
    setDownloadingKey(`specialty-${specialtyId}-${formato}`);
    try {
      await downloadHorarioEspecialidad(specialtyId, formato);
      status(`Horario de ${specialty} descargado en ${formato.toUpperCase()}.`);
    } catch (reason) {
      status(reason instanceof ApiError ? reason.message : 'No se pudo descargar el horario.');
    } finally {
      setDownloadingKey(null);
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
            <div className="admin-actions">
              <strong>{group.courses.length} {group.courses.length === 1 ? 'curso' : 'cursos'}</strong>
              <button className="button secondary" type="button" disabled={group.specialtyId == null || downloadingKey === `specialty-${group.specialtyId}-xlsx`} onClick={() => void downloadSpecialty(group.specialtyId, group.specialty, 'xlsx')}>
                {downloadingKey === `specialty-${group.specialtyId}-xlsx` ? 'Descargando…' : 'Excel'}
              </button>
              <button className="button secondary" type="button" disabled={group.specialtyId == null || downloadingKey === `specialty-${group.specialtyId}-pdf`} onClick={() => void downloadSpecialty(group.specialtyId, group.specialty, 'pdf')}>
                {downloadingKey === `specialty-${group.specialtyId}-pdf` ? 'Descargando…' : 'PDF'}
              </button>
            </div>
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
                  <button className="button secondary" type="button" disabled={downloadingKey === `course-${courseItem.cursoId}-xlsx`} onClick={() => void downloadCourse(courseItem, 'xlsx')}>
                    {downloadingKey === `course-${courseItem.cursoId}-xlsx` ? 'Descargando…' : 'Excel'}
                  </button>
                  <button className="button secondary" type="button" disabled={downloadingKey === `course-${courseItem.cursoId}-pdf`} onClick={() => void downloadCourse(courseItem, 'pdf')}>
                    {downloadingKey === `course-${courseItem.cursoId}-pdf` ? 'Descargando…' : 'PDF'}
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
