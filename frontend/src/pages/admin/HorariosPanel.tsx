import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { ApiError } from '../../api/client';
import { downloadHorarioEspecialidadPdf, getHoraCatedraCatalog, getHorarioResumen, type HorarioResumenCursoItem, type HoraCatedraItem } from '../../api/admin';
import ContentState from '../../components/ui/ContentState';
import { groupSchedulesBySpecialty } from './adminFormatters';
import HorarioCursoPage from './HorarioCursoPage';
import SpecialtyIcon from '../../components/SpecialtyIcon';
import { normalizeSpecialty } from '../../theme/theme';

export default function HorariosPanel({ status }: { status: (message: string) => void }) {
  const { cursoId } = useParams();
  const [search, setSearch] = useSearchParams();
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

  async function downloadSpecialty(specialtyId: number | null, specialty: string) {
    if (specialtyId == null) return;
    setDownloadingKey(`specialty-${specialtyId}-pdf`);
    try {
      await downloadHorarioEspecialidadPdf(specialtyId);
      status(`Horario de ${specialty} descargado en PDF.`);
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
    const course = items.find((item) => String(item.cursoId) === cursoId);
    const backSearch = course ? new URLSearchParams({ especialidad: String(course.especialidadId ?? course.especialidad), nivel: String(course.nivel ?? parseInt(course.cursoDescripcion, 10)) }) : null;
    return <HorarioCursoPage key={cursoId} cursoId={cursoId} summary={items} hours={hours} status={status} refreshSummary={refreshSummary} backTo={backSearch ? `/admin/horarios?${backSearch}` : '/admin/horarios'} />;
  }

  const selectedGroup = groups.find((group) => String(group.specialtyId ?? group.specialty) === search.get('especialidad'));
  const selectedLevel = Number(search.get('nivel')) || null;
  const courseLevel = (item: HorarioResumenCursoItem) => item.nivel ?? parseInt(item.cursoDescripcion, 10);
  const levels = selectedGroup ? Array.from(new Set(selectedGroup.courses.map(courseLevel))).sort((a, b) => b - a) : [];
  const chooseSpecialty = (value: string) => setSearch({ especialidad: value });

  if (!selectedGroup) return <div className="card-grid">
    {groups.map((group) => <button type="button" className="nav-card" data-specialty={normalizeSpecialty(group.specialty)} key={group.specialtyId ?? group.specialty} onClick={() => chooseSpecialty(String(group.specialtyId ?? group.specialty))}>
      <span>Especialidad</span><h2 className="specialty-card-title"><SpecialtyIcon name={group.specialty} />{group.specialty}</h2>
      <p>{new Set(group.courses.map(courseLevel)).size} cursos · {group.courses.length} secciones</p><strong>Ver cursos →</strong>
    </button>)}
  </div>;

  return (
    <>
      <nav className="toolbar" aria-label="Navegación de horarios">
        <button className="button secondary" type="button" onClick={() => setSearch({})}>← Especialidades</button>
        {selectedLevel && <button className="button secondary" type="button" onClick={() => chooseSpecialty(String(selectedGroup.specialtyId ?? selectedGroup.specialty))}>← Cursos de {selectedGroup.specialty}</button>}
      </nav>
      <header className="admin-summary-heading" style={{ marginBottom: 20 }}>
        <div><h2 className="specialty-card-title"><SpecialtyIcon name={selectedGroup.specialty} />{selectedGroup.specialty}{selectedLevel ? ` · ${selectedLevel}° curso` : ''}</h2><p className="muted-copy">{selectedLevel ? 'Elegí la sección para abrir su horario.' : 'Elegí el curso para ver sus secciones.'}</p></div>
        <button className="button secondary" type="button" disabled={selectedGroup.specialtyId == null || downloadingKey !== null} onClick={() => void downloadSpecialty(selectedGroup.specialtyId, selectedGroup.specialty)}>{downloadingKey ? 'Descargando…' : 'PDF de la especialidad'}</button>
      </header>
      <div className="card-grid">
        {!selectedLevel ? levels.map((level) => <button type="button" className="nav-card" key={level} onClick={() => setSearch({ especialidad: String(selectedGroup.specialtyId ?? selectedGroup.specialty), nivel: String(level) })}>
          <span>Curso</span><h2>{level}°</h2><p>{selectedGroup.courses.filter((item) => courseLevel(item) === level).length} secciones</p><strong>Ver secciones →</strong>
        </button>) : selectedGroup.courses.filter((item) => courseLevel(item) === selectedLevel).sort((a, b) => a.cursoDescripcion.localeCompare(b.cursoDescripcion, 'es')).map((item) => <Link className="nav-card" key={item.cursoId} to={`/admin/horarios/${item.cursoId}`}>
          <span>Sección</span><h2>{item.cursoDescripcion.replace(/^\d+\s*[°º]?\s*/, '') || 'Sin sección'}</h2><p>{item.cantidadSlotsCargados} {item.cantidadSlotsCargados === 1 ? 'bloque cargado' : 'bloques cargados'}</p><strong>Ver horario →</strong>
        </Link>)}
      </div>
      {selectedLevel && !levels.includes(selectedLevel) && <ContentState title="No hay secciones para este curso" detail="Volvé a cursos para elegir uno disponible." />}
    </>
  );
}
