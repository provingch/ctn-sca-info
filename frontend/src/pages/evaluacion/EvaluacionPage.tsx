import SectionNavigation from '../../components/SectionNavigation';
import { useEffect, useState } from 'react';
import AppShell from '../../components/AppShell';
import ReviewPlanesView from './ReviewPlanesView';
import SeguimientoPlanesView from './SeguimientoPlanesView';
import PlanillaDetalleView from './PlanillaDetalleView';
import { useEvaluacionFiltros } from './useEvaluacionFiltros';
import { useSearchParams } from 'react-router-dom';
import LauncherCards from '../../components/LauncherCards';
import { launcherIcons } from '../../components/launcherIcons';
import CardsWithActivity from '../../components/CardsWithActivity';

type EvaluationView = 'menu' | 'ver-planillas' | 'planes' | 'seguimiento';

function requestedView(value: string | null): EvaluationView {
  if (value === 'planillas') return 'ver-planillas'; // compat: "Descargar planillas" se unificó con "Ver planillas"
  return value === 'ver-planillas' || value === 'planes' || value === 'seguimiento' ? value : 'menu';
}

export default function EvaluacionPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [view, setView] = useState<EvaluationView>(() => requestedView(searchParams.get('view')));
  const filtros = useEvaluacionFiltros();

  useEffect(() => {
    setView(requestedView(searchParams.get('view')));
  }, [searchParams]);

  function changeView(nextView: EvaluationView) {
    setView(nextView);
    if (nextView === 'menu') setSearchParams({});
    else setSearchParams({ view: nextView, ...(nextView === 'seguimiento' && searchParams.get('tab') ? { tab: searchParams.get('tab')! } : {}) });
  }

  const navigation = <SectionNavigation label="Apartados de evaluación" active={view} sections={[
    { key: 'ver-planillas', label: 'Ver planillas', onSelect: () => changeView('ver-planillas') },
    { key: 'planes', label: 'Planes curriculares', onSelect: () => changeView('planes') },
    { key: 'seguimiento', label: 'Seguimiento', onSelect: () => changeView('seguimiento') },
  ]} />;

  if (view === 'menu') {
    return <AppShell title="Panel de Evaluación" subtitle="Revisión y seguimiento académico" welcome>
      <CardsWithActivity>
        <LauncherCards options={[
          { key: 'ver-planillas', icon: launcherIcons.verPlanillas, title: 'Ver planillas', description: 'Consultá las notas de una planilla en pantalla, descargalas (una o en lote) o reabrí una etapa cerrada.', onSelect: () => changeView('ver-planillas') },
          { key: 'planes', icon: launcherIcons.revisarPlanes, title: 'Revisar plan curricular', description: 'Aprobá o rechazá planes de profesores.', onSelect: () => changeView('planes') },
          { key: 'seguimiento', icon: launcherIcons.seguimiento, title: 'Seguimiento de profesores', description: 'Consultá cumplimiento de planes y resolvé incumplimientos.', onSelect: () => changeView('seguimiento') },
        ]} />
      </CardsWithActivity>
    </AppShell>;
  }

  if (view === 'ver-planillas') {
    return <AppShell title="Ver planillas" onBack={() => changeView('menu')} backLabel="Panel de Evaluación" navigation={navigation}>
      <PlanillaDetalleView filtros={filtros} />
    </AppShell>;
  }

  if (view === 'planes') {
    return <AppShell title="Revisar Planes Curriculares" onBack={() => changeView('menu')} backLabel="Panel de Evaluación" navigation={navigation}>
      <ReviewPlanesView />
    </AppShell>;
  }

  return <AppShell title="Seguimiento de Profesores" onBack={() => changeView('menu')} backLabel="Panel de Evaluación" navigation={navigation}>
    <SeguimientoPlanesView initialTab={searchParams.get('tab') === 'incumplimientos' ? 'incumplimientos' : 'planes'} />
  </AppShell>;
}
