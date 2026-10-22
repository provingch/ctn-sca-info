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
import DashboardActivity from '../../components/DashboardActivity';
import { DashboardLayout, DashboardStats, DashboardTasks, type DashboardTask } from '../../components/Dashboard';
import { shortWhen } from '../../utils/date';
import { getPendientes, type PlanPendienteResumen } from '../../api/planCurricular';
import { getIncumplimientos, TIPO_ATRASO, type IncumplimientoPendiente } from '../../api/evaluacion';

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
      <EvaluacionInicio onOpen={changeView} />
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

const esBloqueo = (caso: IncumplimientoPendiente) => caso.tipo.startsWith('BLOQUEO');

/** Inicio de Evaluación: cifras, lo que hay que revisar hoy y los accesos a cada apartado. */
function EvaluacionInicio({ onOpen }: { onOpen: (view: EvaluationView) => void }) {
  const [planes, setPlanes] = useState<PlanPendienteResumen[] | null>(null);
  const [casos, setCasos] = useState<IncumplimientoPendiente[] | null>(null);
  const [fallas, setFallas] = useState({ planes: false, casos: false });

  useEffect(() => {
    let active = true;
    void getPendientes().then((list) => { if (active) setPlanes(list); })
      .catch(() => { if (active) { setPlanes([]); setFallas((prev) => ({ ...prev, planes: true })); } });
    void getIncumplimientos().then((list) => { if (active) setCasos(list); })
      .catch(() => { if (active) { setCasos([]); setFallas((prev) => ({ ...prev, casos: true })); } });
    return () => { active = false; };
  }, []);

  const bloqueos = casos?.filter(esBloqueo) ?? null;
  const atrasos = casos?.filter((caso) => caso.tipo === TIPO_ATRASO) ?? null;
  const otros = casos?.filter((caso) => !esBloqueo(caso) && caso.tipo !== TIPO_ATRASO) ?? null;
  const count = (list: unknown[] | null, failed: boolean) => (failed ? undefined : list === null ? null : list.length);

  const tasks: DashboardTask[] | null = planes === null || casos === null ? null : [
    ...casos.map((caso): DashboardTask => ({
      key: `caso-${caso.id}`,
      kind: esBloqueo(caso) ? 'Bloqueo' : caso.tipo === TIPO_ATRASO ? 'Atraso' : 'Incongruencia',
      tone: esBloqueo(caso) ? 'danger' : 'warning',
      title: [caso.usuarioApellido, caso.usuarioNombre].filter(Boolean).join(', ') || 'Profesor',
      detail: [caso.materiaNombre, caso.descripcion].filter(Boolean).join(' · '),
      when: shortWhen(caso.fechaCreacion),
      action: { label: esBloqueo(caso) ? 'Resolver' : 'Revisar', primary: esBloqueo(caso), onSelect: () => onOpen('seguimiento') },
    })),
    ...planes.map((plan): DashboardTask => ({
      key: `plan-${plan.id}`,
      kind: 'Plan',
      tone: 'accent',
      title: plan.profesorNombreCorto || plan.profesorNombre,
      detail: `${plan.materiaNombre} · ${plan.cursoDescripcion}`,
      when: shortWhen(plan.fechaSubida),
      action: { label: 'Revisar', onSelect: () => onOpen('planes') },
    })),
  ];

  return <DashboardLayout
    stats={<DashboardStats stats={[
      { key: 'planes', label: 'Planes por revisar', value: count(planes, fallas.planes) },
      { key: 'bloqueos', label: 'Bloqueos de Iniciar clase', value: count(bloqueos, fallas.casos), tone: bloqueos?.length ? 'danger' : undefined },
      { key: 'atrasos', label: 'Atrasos por revisar', value: count(atrasos, fallas.casos), tone: atrasos?.length ? 'warning' : undefined },
      { key: 'otros', label: 'Incongruencias por revisar', value: count(otros, fallas.casos) },
    ]} />}
    tasks={<DashboardTasks title="Para revisar hoy" tasks={tasks} emptyText="No hay planes ni casos pendientes. Todo al día." columns={['Qué', 'Profesor', 'Desde']} link={{ label: 'Ver seguimiento', onSelect: () => onOpen('seguimiento') }} />}
    side={<>
      <LauncherCards options={[
        { key: 'ver-planillas', icon: launcherIcons.verPlanillas, title: 'Ver planillas', description: 'Notas, descargas y etapas.', onSelect: () => onOpen('ver-planillas') },
        { key: 'planes', icon: launcherIcons.revisarPlanes, title: 'Planes curriculares', description: 'Aprobar o rechazar.', onSelect: () => onOpen('planes') },
        { key: 'seguimiento', icon: launcherIcons.seguimiento, title: 'Seguimiento', description: 'Cumplimiento e incumplimientos.', onSelect: () => onOpen('seguimiento') },
      ]} />
      <DashboardActivity />
    </>}
  />;
}
