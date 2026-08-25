import { useEffect, useMemo, useState } from 'react';
import { ApiError } from '../../api/client';
import { downloadHorarioCurso, getHorarioResumen, type HorarioResumenCursoItem } from '../../api/admin';
import ContentState from '../../components/ui/ContentState';
import { groupSchedulesBySpecialty } from './adminFormatters';
import SpecialtyIcon from '../../components/SpecialtyIcon';

export default function HorariosPanel({ status }: { status: (message: string) => void }) {
  const [items, setItems] = useState<HorarioResumenCursoItem[] | null>(null);
  const [error, setError] = useState('');
  const [downloadingId, setDownloadingId] = useState<number | null>(null);

  useEffect(() => {
    getHorarioResumen().then(setItems).catch((reason) => setError(reason instanceof ApiError ? reason.message : 'No se pudo cargar el resumen de horarios.'));
  }, []);

  const groups = useMemo(() => groupSchedulesBySpecialty(items ?? []), [items]);
  if (!items) return <ContentState tone={error ? 'error' : 'loading'} title={error || 'Cargando horarios…'} detail={error ? 'Volvé a intentarlo recargando esta página.' : 'Estamos agrupando los cursos por especialidad.'} />;
  if (items.length === 0) return <ContentState title="No hay cursos disponibles" detail="Los cursos con horarios aparecerán en este panel." />;

  async function download(course: HorarioResumenCursoItem) {
    setDownloadingId(course.cursoId);
    try {
      await downloadHorarioCurso(course.cursoId);
      status(`Horario de ${course.cursoDescripcion} descargado.`);
    } catch (reason) {
      status(reason instanceof ApiError ? reason.message : 'No se pudo descargar el horario.');
    } finally {
      setDownloadingId(null);
    }
  }

  return <div className="admin-summary-groups">
    {groups.map((group) => <section className="panel admin-summary-section" key={group.specialty}>
      <header className="admin-summary-heading"><div><span>Especialidad</span><h2 className="specialty-card-title"><SpecialtyIcon name={group.specialty} />{group.specialty}</h2></div><strong>{group.courses.length} {group.courses.length === 1 ? 'curso' : 'cursos'}</strong></header>
      <div className="admin-list">
        {group.courses.map((course) => <div key={course.cursoId}>
          <span><strong>{course.cursoDescripcion}</strong><small>{course.cantidadSlotsCargados} {course.cantidadSlotsCargados === 1 ? 'bloque cargado' : 'bloques cargados'}</small></span>
          <button className="button secondary" type="button" disabled={downloadingId === course.cursoId} onClick={() => void download(course)}>{downloadingId === course.cursoId ? 'Descargando…' : 'Descargar'}</button>
        </div>)}
      </div>
    </section>)}
  </div>;
}
