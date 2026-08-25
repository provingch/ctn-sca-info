import { useCallback, useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import AppShell from '../../components/AppShell';
import AnimatedSelect from '../../components/AnimatedSelect';
import ContentState from '../../components/ui/ContentState';
import { getAdminCatalog, type AdminCatalog } from '../../api/admin';
import { ApiError } from '../../api/client';
import { useSpecialty } from '../../context/SpecialtyContext';
import { normalizeSpecialty } from '../../theme/theme';
import { useAuth } from '../../context/AuthContext';
import MateriasPanel from './MateriasPanel';
import UsuariosPanel from './UsuariosPanel';
import AsignacionesPanel from './AsignacionesPanel';
import AlumnosPanel from './AlumnosPanel';
import HorariosPanel from './HorariosPanel';
import SpecialtyIcon from '../../components/SpecialtyIcon';
import SistemaEstadoPanel from './SistemaEstadoPanel';

const modules = [
  { path: '/admin/materias', key: 'materias', title: 'Materias', detail: 'Catálogo, categorías y especialidades' },
  { path: '/admin/usuarios', key: 'usuarios', title: 'Usuarios', detail: 'Altas, roles y datos de acceso' },
  { path: '/admin/asignaciones', key: 'asignaciones', title: 'Asignaciones', detail: 'Profesor, materia y curso' },
  { path: '/admin/alumnos', key: 'alumnos', title: 'Alumnos', detail: 'Carga de estudiantes y gestión por sección' },
  { path: '/admin/horarios', key: 'horarios', title: 'Horarios', detail: 'Vista y descarga de horarios por especialidad', globalOnly: true },
  { path: '/admin/sistema', key: 'sistema', title: 'Estado del sistema', detail: 'Salud de base de datos, migraciones y sincronización', globalOnly: true },
];

export default function AdminPage() {
  const location = useLocation();
  const { user, identityStatus, refreshUserIdentity } = useAuth();
  const selected = modules.find((module) => module.path === location.pathname) ?? null;
  const [data, setData] = useState<AdminCatalog | null>(null);
  const [status, setStatus] = useState('');

  const load = useCallback(async () => {
    try {
      setData(await getAdminCatalog());
    } catch (error) {
      setStatus(error instanceof ApiError ? error.message : 'No se pudo cargar el catálogo.');
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  if (!data) {
    return <AppShell title={selected?.title || 'Panel general'}><ContentState tone={status ? 'error' : 'loading'} title={status || 'Cargando administración…'} detail={status ? 'Recargá la página para volver a intentarlo.' : 'Estamos preparando el catálogo del sistema.'} /></AppShell>;
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

  return <AppShell title={selected?.title || 'Panel general'} subtitle={scopeName ? `Administración de ${scopeName}` : 'Administración global del sistema'} specialty={scopeName}>
    <AdminToolbar data={data} showBack={Boolean(selected)} scopeName={scopeName} />
    {status && <div className="notice" role="status">{status}</div>}
    {selectedIsRestricted ? (
      <ContentState tone="error" title="Módulo reservado al administrador global" detail="Tu cuenta administra una especialidad y no tiene acceso a esta herramienta del sistema." actions={<Link className="button" to="/admin">Volver al panel</Link>} />
    ) : !selected ? (
      <div className="admin-dashboard-sections">
        <section aria-labelledby="admin-management-title">
          <header className="admin-dashboard-heading"><span>Administración</span><h2 id="admin-management-title">Gestión del sistema</h2></header>
          <div className="card-grid">{visibleModules.map((module) => <Link className="nav-card" to={module.path} key={module.path}><span>Gestionar</span><h2>{module.title}</h2><p>{module.detail}</p><strong>Abrir →</strong></Link>)}</div>
        </section>
        {!isScopedAdmin && <section className="admin-reference-section" aria-labelledby="admin-reference-title">
          <header className="admin-dashboard-heading"><span>Referencia</span><h2 id="admin-reference-title">Herramientas internas</h2></header>
          <div className="card-grid"><Link className="nav-card" to="/styleguide"><span>Referencia interna</span><h2>Sistema de diseño</h2><p>Componentes, estados y reglas visuales compartidas.</p><strong>Consultar →</strong></Link></div>
        </section>}
      </div>
    ) : (
      <AdminModule module={selected} data={data} reload={load} status={setStatus} isGlobalAdmin={!isScopedAdmin} />
    )}

  </AppShell>;
}

function AdminToolbar({ data, showBack, scopeName }: { data: AdminCatalog; showBack: boolean; scopeName: string | null }) {
  const { id, name, selectSpecialty, resetSpecialty } = useSpecialty();
  const selectedId = id ?? data.especialidades.find((item) => normalizeSpecialty(item.nombre) === normalizeSpecialty(name))?.id ?? 0;

  function changePalette(value: number) {
    const specialty = data.especialidades.find((item) => item.id === value);
    if (specialty) selectSpecialty(specialty.nombre, specialty.id);
    else resetSpecialty();
  }

  return <div className="toolbar filters admin-toolbar">
    {showBack && <Link className="button secondary" to="/admin">← Panel general</Link>}
    {scopeName && <span className="admin-scope-badge"><small>Especialidad gestionada</small><strong className="specialty-card-title"><SpecialtyIcon name={scopeName} />{scopeName}</strong></span>}
    {!scopeName && data.especialidades.length > 1 && <label className="inline-filter">Paleta del sistema
      <AnimatedSelect ariaLabel="Paleta del sistema" value={selectedId} onChange={(value) => changePalette(Number(value))} options={[{ value: 0, label: 'Institucional (predeterminada)' }, ...data.especialidades.map((specialty) => ({ value: specialty.id, label: specialty.nombre }))]} />
    </label>}
  </div>;
}

function AdminModule({ module, data, reload, status, isGlobalAdmin }: {
  module: (typeof modules)[number]; data: AdminCatalog; reload: () => Promise<void>; status: (message: string) => void; isGlobalAdmin: boolean;
}) {
  if (module.key === 'materias') return <MateriasPanel data={data} reload={reload} status={status} />;
  if (module.key === 'usuarios') return <UsuariosPanel data={data} reload={reload} status={status} isGlobalAdmin={isGlobalAdmin} />;
  if (module.key === 'asignaciones') return <AsignacionesPanel data={data} reload={reload} status={status} />;
  if (module.key === 'alumnos') return <AlumnosPanel data={data} reload={reload} status={status} />;
  if (module.key === 'horarios') return <HorariosPanel status={status} />;
  return <SistemaEstadoPanel />;
}
