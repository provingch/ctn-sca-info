import { useEffect, useState } from 'react';
import AppShell from '../../components/AppShell';
import { descargarPlanillas } from '../../api/evaluacion';
import { ApiError } from '../../api/client';
import ReviewPlanesView from './ReviewPlanesView';
import SeguimientoPlanesView from './SeguimientoPlanesView';
import PlanillaDetalleView from './PlanillaDetalleView';
import EvaluacionFiltrosCampos from './EvaluacionFiltrosCampos';
import { useEvaluacionFiltros } from './useEvaluacionFiltros';
import { useSearchParams } from 'react-router-dom';
import LauncherCards from '../../components/LauncherCards';
import { launcherIcons } from '../../components/launcherIcons';

type EvaluationView = 'menu' | 'planillas' | 'ver-planillas' | 'planes' | 'seguimiento';

function requestedView(value: string | null): EvaluationView {
  return value === 'planillas' || value === 'ver-planillas' || value === 'planes' || value === 'seguimiento' ? value : 'menu';
}

export default function EvaluacionPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [view, setView] = useState<EvaluationView>(() => requestedView(searchParams.get('view')));
  const filtros = useEvaluacionFiltros();
  const [descargando, setDescargando] = useState(false);
  const [descargaError, setDescargaError] = useState('');

  useEffect(() => {
    setView(requestedView(searchParams.get('view')));
  }, [searchParams]);

  function changeView(nextView: EvaluationView) {
    setView(nextView);
    if (nextView === 'menu') setSearchParams({});
    else setSearchParams({ view: nextView, ...(nextView === 'seguimiento' && searchParams.get('tab') ? { tab: searchParams.get('tab')! } : {}) });
  }

  async function descargar() {
    if (!filtros.selected || descargando) return;
    setDescargando(true);
    setDescargaError('');
    try {
      await descargarPlanillas(filtros.selected.id, filtros.etapa, filtros.periodo, filtros.materiaId);
    } catch (error) {
      setDescargaError(error instanceof ApiError ? error.message : 'No se pudo descargar el archivo.');
    } finally {
      setDescargando(false);
    }
  }

  if (view === 'menu') {
    return <AppShell title="Panel de Evaluación">
      <LauncherCards className="launcher-cards-grid" options={[
        { key: 'ver-planillas', icon: launcherIcons.verPlanillas, title: 'Ver planillas', description: 'Consultá las notas de una planilla en pantalla, descargala o reabrí una etapa cerrada.', onSelect: () => changeView('ver-planillas') },
        { key: 'planillas', icon: launcherIcons.descargarPlanillas, title: 'Descargar planillas', description: 'Exportá planillas completadas de los cursos.', onSelect: () => changeView('planillas') },
        { key: 'planes', icon: launcherIcons.revisarPlanes, title: 'Revisar plan curricular', description: 'Aprobá o rechazá planes de profesores.', onSelect: () => changeView('planes') },
        { key: 'seguimiento', icon: launcherIcons.seguimiento, title: 'Seguimiento de profesores', description: 'Consultá cumplimiento de planes y resolvé incumplimientos.', onSelect: () => changeView('seguimiento') },
      ]} />
    </AppShell>;
  }

  if (view === 'ver-planillas') {
    return <AppShell title="Ver planillas" onBack={() => changeView('menu')} backLabel="Panel de Evaluación">
      <PlanillaDetalleView filtros={filtros} />
    </AppShell>;
  }

  if (view === 'planes') {
    return <AppShell title="Revisar Planes Curriculares" onBack={() => changeView('menu')} backLabel="Panel de Evaluación">
      <ReviewPlanesView />
    </AppShell>;
  }

  if (view === 'seguimiento') {
    return <AppShell title="Seguimiento de Profesores" onBack={() => changeView('menu')} backLabel="Panel de Evaluación">
      <SeguimientoPlanesView initialTab={searchParams.get('tab') === 'incumplimientos' ? 'incumplimientos' : 'planes'} />
    </AppShell>;
  }

  return <AppShell title="Descargar planillas" onBack={() => changeView('menu')} backLabel="Panel de Evaluación">
    <section className="panel form-grid evaluation-filters">
      <p className="lead">Elegí la especialidad, el curso, la sección y el período académico para generar sus planillas.</p>
      <EvaluacionFiltrosCampos filtros={filtros} />
      {descargaError && <div className="notice error" role="alert">{descargaError}</div>}
      <button type="button" className="button" disabled={!filtros.selected || descargando} onClick={() => void descargar()}>{descargando ? 'Generando…' : 'Descargar planillas'}</button>
    </section>
  </AppShell>;
}
