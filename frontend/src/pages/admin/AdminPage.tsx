import { useCallback, useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import AppShell from '../../components/AppShell';
import ContentState from '../../components/ui/ContentState';
import { getAdminCatalog, type AdminCatalog } from '../../api/admin';
import { ApiError } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import MateriasPanel from './MateriasPanel';
import UsuariosPanel from './UsuariosPanel';
import AsignacionesPanel from './AsignacionesPanel';
import AlumnosPanel from './AlumnosPanel';
import HorariosPanel from './HorariosPanel';
import CargaHorariaPanel from './CargaHorariaPanel';
import SistemaEstadoPanel from './SistemaEstadoPanel';
import SalasPanel from './SalasPanel';
import AdminQuejasPanel from './AdminQuejasPanel';
import ClasesEspecialidadPanel from './ClasesEspecialidadPanel';
import { toneDeMensaje } from './adminFormatters';
import { useToast } from '../../context/toast';
import LauncherCards from '../../components/LauncherCards';
import { launcherIcons } from '../../components/launcherIcons';
import SectionNavigation from '../../components/SectionNavigation';
import DashboardActivity from '../../components/DashboardActivity';
import { DashboardLayout, DashboardStats, DashboardTasks, type DashboardTask } from '../../components/Dashboard';
import { shortWhen } from '../../utils/date';
import { getAdminQuejas, quejaEstado, type QuejaItem } from '../../api/quejas';

const modules = [
  { path: '/admin/materias', key: 'materias', title: 'Materias', detail: 'Catálogo, categorías y especialidades', globalOnly: true },
  { path: '/admin/usuarios', key: 'usuarios', title: 'Usuarios', detail: 'Altas, roles y datos de acceso', globalOnly: true },
  { path: '/admin/asignaciones', key: 'asignaciones', title: 'Asignaciones', detail: 'Profesor, materia y curso', globalOnly: true },
  { path: '/admin/alumnos', key: 'alumnos', title: 'Alumnos', detail: 'Carga de estudiantes y gestión por sección' },
  { path: '/admin/horarios', key: 'horarios', title: 'Horarios', detail: 'Vista y descarga de horarios por especialidad' },
  { path: '/admin/carga-horaria', key: 'carga-horaria', title: 'Carga horaria', detail: 'Horas y horario semanal de cada profesor' },
  { path: '/admin/sistema', key: 'sistema', title: 'Estado del sistema', detail: 'Salud de base de datos, migraciones y sincronización', globalOnly: true },
  { path: '/admin/salas', key: 'salas', title: 'Salas', detail: 'Catálogo de salas comunes y de pabellón', globalOnly: true },
  { path: '/admin/quejas', key: 'quejas', title: 'Quejas', detail: 'Registrar y gestionar quejas por especialidad' },
  { path: '/admin/clases', key: 'clases', title: 'Clases dadas', detail: 'Historial de clases registradas por los profesores' },
];

export default function AdminPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, identityStatus, refreshUserIdentity } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const fichaParam = Number(searchParams.get('ficha')) || null;
  const selected = modules.find((module) => location.pathname === module.path || location.pathname.startsWith(`${module.path}/`)) ?? null;
  const [data, setData] = useState<AdminCatalog | null>(null);
  const { showToast } = useToast();
  const setStatus = useCallback((text: string) => { if (text) showToast(text, { tone: toneDeMensaje(text) }); }, [showToast]);

  const load = useCallback(async () => {
    try {
      setData(await getAdminCatalog());
    } catch (error) {
      setStatus(error instanceof ApiError ? error.message : 'No se pudo cargar el catálogo.');
    }
  }, [setStatus]);

  useEffect(() => { void load(); }, [load]);

  if (!data) {
    return <AppShell title={selected?.title || 'Panel general'}><ContentState tone={'loading'} title={'Cargando administración…'} detail={'Estamos preparando el catálogo del sistema.'} /></AppShell>;
  }

  if (identityStatus === 'idle' || identityStatus === 'loading') {
    return <AppShell title={selected?.title || 'Panel general'}><ContentState tone="loading" title="Verificando alcance administrativo…" detail="Estamos consultando la especialidad asociada a tu cuenta." /></AppShell>;
  }

  if (identityStatus === 'error' || user?.especialidadId === undefined) {
    return <AppShell title={selected?.title || 'Panel general'}><ContentState tone="error" title="No se pudo determinar tu alcance administrativo" detail="El perfil no informó especialidadId. No se habilitó acceso global por defecto para proteger los datos del sistema." actions={<button className="button" type="button" onClick={() => void refreshUserIdentity()}>Reintentar</button>} /></AppShell>;
  }

  const isScopedAdmin = user.especialidadId !== null;
  const visibleModules = modules.filter((module) => !isScopedAdmin || !('globalOnly' in module && module.globalOnly));
  const selectedIsRestricted = Boolean(selected && isScopedAdmin && 'globalOnly' in selected && selected.globalOnly);
  const scopeName = user.especialidadNombre ?? data.especialidades.find((item) => item.id === user.especialidadId)?.nombre ?? (user.especialidadId === null ? null : `Especialidad #${user.especialidadId}`);

  const cerrarFicha = () => { if (searchParams.has('ficha')) { searchParams.delete('ficha'); setSearchParams(searchParams, { replace: true }); } };

  return <AppShell title={selected?.title || 'Panel general'} subtitle={scopeName ? `Administración de ${scopeName}` : 'Administración global del sistema'} specialty={scopeName} onBack={selected ? () => navigate('/admin') : undefined} backLabel="Panel general" welcome={!selected}
    navigation={selected && <SectionNavigation label="Apartados de administración" active={selected.key} sections={visibleModules.map(module => ({ key: module.key, label: module.title, href: module.path }))} />}>
    {selectedIsRestricted ? (
      <ContentState tone="error" title="Módulo reservado al administrador global" detail="Tu cuenta administra una especialidad y no tiene acceso a esta herramienta del sistema." actions={<Link className="button" to="/admin">Volver al panel</Link>} />
    ) : !selected ? (
      <AdminInicio data={data} modules={visibleModules} isScopedAdmin={isScopedAdmin} />
    ) : (
      <div className="u-relative">
        <AdminModule module={selected} data={data} reload={load} status={setStatus} isGlobalAdmin={!isScopedAdmin} fichaInicial={fichaParam} onFichaCerrada={cerrarFicha} />
      </div>
    )}

  </AppShell>;
}

function AdminModule({ module, data, reload, status, isGlobalAdmin, fichaInicial, onFichaCerrada }: {
  module: (typeof modules)[number]; data: AdminCatalog; reload: () => Promise<void>; status: (message: string) => void; isGlobalAdmin: boolean;
  fichaInicial: number | null; onFichaCerrada: () => void;
}) {
  if (module.key === 'materias') return <MateriasPanel data={data} reload={reload} status={status} />;
  if (module.key === 'usuarios') return <UsuariosPanel data={data} reload={reload} status={status} isGlobalAdmin={isGlobalAdmin} />;
  if (module.key === 'asignaciones') return <AsignacionesPanel data={data} reload={reload} status={status} />;
  if (module.key === 'alumnos') return <AlumnosPanel data={data} reload={reload} status={status} fichaInicial={fichaInicial} onFichaCerrada={onFichaCerrada} />;
  if (module.key === 'horarios') return <HorariosPanel status={status} />;
  if (module.key === 'carga-horaria') return <CargaHorariaPanel status={status} />;
  if (module.key === 'salas') return <SalasPanel data={data} status={status} />;
  if (module.key === 'quejas') return <AdminQuejasPanel data={data} status={status} isGlobalAdmin={isGlobalAdmin} />;
  if (module.key === 'clases') return <ClasesEspecialidadPanel status={status} />;
  return <SistemaEstadoPanel />;
}

const PROFESOR = 1;

/** Inicio de Administración: cifras, lo que hay que resolver y los accesos a cada módulo. */
function AdminInicio({ data, modules: visibles, isScopedAdmin }: { data: AdminCatalog; modules: (typeof modules)[number][]; isScopedAdmin: boolean }) {
  const [quejas, setQuejas] = useState<QuejaItem[] | null | undefined>(null);

  useEffect(() => {
    let active = true;
    void getAdminQuejas().then((list) => { if (active) setQuejas(list); }).catch(() => { if (active) setQuejas(undefined); });
    return () => { active = false; };
  }, []);

  const pendientes = quejas?.filter((q) => quejaEstado(q) === 'pendiente') ?? [];
  const profesores = data.usuarios.filter((u) => u.nivel === PROFESOR);
  const conAsignacion = new Set(data.asignaciones.map((a) => a.profesorId));
  const sinAsignacion = isScopedAdmin ? [] : profesores.filter((p) => !conAsignacion.has(p.id));
  const alumnosPorCurso = new Map<number, number>();
  data.alumnos.forEach((a) => alumnosPorCurso.set(a.cursoId, (alumnosPorCurso.get(a.cursoId) ?? 0) + 1));

  const tasks: DashboardTask[] | null = quejas === null ? null : [
    ...pendientes.map((q): DashboardTask => ({
      key: `queja-${q.id}`,
      kind: 'Queja',
      tone: 'warning',
      title: [q.cursoEspecialidad, q.cursoNivel ? `${q.cursoNivel}°` : null, q.cursoSeccion].filter(Boolean).join(' ') || `Curso #${q.cursoId}`,
      detail: q.motivo,
      when: shortWhen(q.creadaEn),
      action: { label: 'Ver', href: '/admin/quejas' },
    })),
    ...sinAsignacion.map((p): DashboardTask => ({
      key: `profesor-${p.id}`,
      kind: 'Asignación',
      tone: 'warning',
      title: `${p.apellido}, ${p.nombre}`,
      detail: 'Profesor sin materias asignadas',
      action: { label: 'Asignar', href: '/admin/asignaciones' },
    })),
  ];

  // Resumen: por sección para el admin de una especialidad, por especialidad para el global.
  const filas = isScopedAdmin
    ? data.cursosAlumnos.map((c) => ({
      key: String(c.id), nombre: `${c.nivel}° ${c.seccion}`, alumnos: alumnosPorCurso.get(c.id) ?? 0,
      quejas: quejas ? pendientes.filter((q) => q.cursoNivel === c.nivel && q.cursoSeccion === c.seccion && q.cursoEspecialidad === c.especialidad).length : null,
    })).sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'))
    : [...data.cursosAlumnos.reduce((map, c) => {
      const fila = map.get(c.especialidad) ?? { key: c.especialidad, nombre: c.especialidad, alumnos: 0, quejas: quejas ? 0 : null as number | null };
      fila.alumnos += alumnosPorCurso.get(c.id) ?? 0;
      map.set(c.especialidad, fila);
      return map;
    }, new Map<string, { key: string; nombre: string; alumnos: number; quejas: number | null }>()).values()]
      .map((fila) => ({ ...fila, quejas: quejas ? pendientes.filter((q) => q.cursoEspecialidad === fila.nombre).length : null }))
      .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));

  return <DashboardLayout
    stats={<DashboardStats stats={[
      { key: 'alumnos', label: 'Alumnos activos', value: data.alumnos.length },
      isScopedAdmin
        ? { key: 'secciones', label: 'Secciones', value: data.cursosAlumnos.length }
        : { key: 'profesores', label: sinAsignacion.length ? `Profesores · ${sinAsignacion.length} sin asignar` : 'Profesores', value: profesores.length, tone: sinAsignacion.length ? 'warning' : undefined },
      { key: 'quejas', label: 'Quejas pendientes', value: quejas === null ? null : quejas === undefined ? undefined : pendientes.length, tone: pendientes.length ? 'warning' : undefined },
      { key: 'egresados', label: 'Egresados', value: data.egresados.length },
    ]} />}
    tasks={<DashboardTasks title="Para resolver" tasks={tasks} emptyText="No hay quejas pendientes ni profesores sin asignar." />}
    side={<>
      <LauncherCards options={[
        ...visibles.map((module) => ({ key: module.key, href: module.path, title: module.title, description: module.detail, icon: module.key === 'clases' ? launcherIcons.clasesDadas : launcherIcons[module.key as keyof typeof launcherIcons] })),
        ...(isScopedAdmin ? [] : [{ key: 'styleguide', href: '/styleguide', title: 'Sistema de diseño', description: 'Componentes y reglas visuales.', icon: launcherIcons.verPlanillas }]),
      ]} />
      <DashboardActivity />
    </>}
  >
    {filas.length > 0 && <section className="panel dash-summary" aria-label={isScopedAdmin ? 'Secciones' : 'Alumnos por especialidad'}>
      <header className="dash-tasks-head"><h2>{isScopedAdmin ? 'Secciones' : 'Alumnos por especialidad'}</h2><Link to="/admin/alumnos">Ver alumnos</Link></header>
      <table className="dash-table">
        <thead><tr><th scope="col">{isScopedAdmin ? 'Curso' : 'Especialidad'}</th><th scope="col" className="num">Alumnos</th><th scope="col" className="num">Quejas pendientes</th></tr></thead>
        <tbody>{filas.map((fila) => <tr key={fila.key}><td><strong>{fila.nombre}</strong></td><td className="num">{fila.alumnos}</td><td className="num">{fila.quejas ?? '—'}</td></tr>)}</tbody>
      </table>
    </section>}
  </DashboardLayout>;
}
