import { useCallback, useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import AppShell from '../../components/AppShell';
import AnimatedSelect from '../../components/AnimatedSelect';
import { getAdminCatalog, wipeAllClassroomSync, wipePlanillaSyncImports, type AdminCatalog } from '../../api/admin';
import { ApiError } from '../../api/client';
import { useSpecialty } from '../../context/SpecialtyContext';
import { normalizeSpecialty } from '../../theme/theme';
import MateriasPanel from './MateriasPanel';
import UsuariosPanel from './UsuariosPanel';
import AsignacionesPanel from './AsignacionesPanel';
import AlumnosPanel from './AlumnosPanel';

const modules = [
  { path: '/admin/materias', key: 'materias', title: 'Materias', detail: 'Catálogo, categorías y especialidades' },
  { path: '/admin/usuarios', key: 'usuarios', title: 'Usuarios', detail: 'Altas, roles y datos de acceso' },
  { path: '/admin/asignaciones', key: 'asignaciones', title: 'Asignaciones', detail: 'Profesor, materia y curso' },
  { path: '/admin/alumnos', key: 'alumnos', title: 'Alumnos', detail: 'Carga de estudiantes y gestión por sección' },
  { path: '/admin/ingresantes', key: 'alumnos', title: 'Alumnos', detail: 'Carga de estudiantes y gestión por sección' },
];

export default function AdminPage() {
  const location = useLocation();
  const selected = modules.find((module) => module.path === location.pathname) ?? null;
  const [data, setData] = useState<AdminCatalog | null>(null);
  const [status, setStatus] = useState('');
  const [wipeId, setWipeId] = useState('');
  const [wiping, setWiping] = useState(false);
  const [wipeResult, setWipeResult] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setData(await getAdminCatalog());
    } catch (error) {
      setStatus(error instanceof ApiError ? error.message : 'No se pudo cargar el catálogo.');
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  if (!data) {
    return <AppShell title={selected?.title || 'Panel general'}><section className="panel">{status || 'Cargando…'}</section></AppShell>;
  }

  return <AppShell title={selected?.title || 'Panel general'}>
    <AdminToolbar data={data} showBack={Boolean(selected)} />
    {status && <div className="notice" role="status">{status}</div>}
    {!selected ? (
      <div className="card-grid">{modules.map((module) => <Link className="nav-card" to={module.path} key={module.path}><span>Gestionar</span><h2>{module.title}</h2><p>{module.detail}</p><strong>Abrir →</strong></Link>)}</div>
    ) : (
      <AdminModule module={selected} data={data} reload={load} status={setStatus} />
    )}

    <section className="panel">
      <h2>Wipe: importaciones Classroom</h2>
      <p>Ingresa el ID de la planilla para borrar tareas y notas importadas desde Classroom.</p>
      <label>
        Planilla ID
        <input type="number" value={wipeId} onChange={(event) => setWipeId(event.target.value)} />
      </label>
      <div style={{ display: 'flex', gap: 8, marginTop: 8, flexWrap: 'wrap' }}>
        <button className="button danger" disabled={wiping || !wipeId} onClick={async () => {
          if (!wipeId) return;
          if (!window.confirm('¿Confirmas borrar todas las tareas y notas importadas desde Classroom para la planilla ' + wipeId + '? Esta acción no se puede deshacer.')) return;
          try {
            setWiping(true);
            setWipeResult(null);
            const res = await wipePlanillaSyncImports(Number(wipeId));
            setWipeResult(res.message + ' (tareas borradas: ' + (res.deletedTasks ?? 0) + ', notas borradas: ' + (res.deletedGrades ?? 0) + ', curso cacheado limpiado: ' + (res.clearedGoogleCourseIds ?? 0) + ')');
            await load();
          } catch (error) {
            setWipeResult(error instanceof ApiError ? error.message : 'Error al ejecutar wipe');
          } finally {
            setWiping(false);
          }
        }}>Wipe planilla</button>
        <button className="button danger" disabled={wiping} onClick={async () => {
          if (!window.confirm('¿Confirmas borrar TODO el cache de sincronización de Classroom? Esto elimina tareas, notas y asociaciones de curso de todas las planillas.')) return;
          try {
            setWiping(true);
            setWipeResult(null);
            const res = await wipeAllClassroomSync();
            setWipeResult(res.message + ' (tareas borradas: ' + (res.deletedTasks ?? 0) + ', notas borradas: ' + (res.deletedGrades ?? 0) + ', cursos cacheados limpiados: ' + (res.clearedGoogleCourseIds ?? 0) + ')');
            await load();
          } catch (error) {
            setWipeResult(error instanceof ApiError ? error.message : 'Error al ejecutar wipe global');
          } finally {
            setWiping(false);
          }
        }}>Wipe global Classroom</button>
        <button className="button" onClick={() => { setWipeId(''); setWipeResult(null); }}>Limpiar</button>
      </div>
      {wiping && <div className="notice">Ejecutando wipe…</div>}
      {wipeResult && <div className="notice">{wipeResult}</div>}
    </section>
  </AppShell>;
}

function AdminToolbar({ data, showBack }: { data: AdminCatalog; showBack: boolean }) {
  const { id, name, selectSpecialty, resetSpecialty } = useSpecialty();
  const selectedId = id ?? data.especialidades.find((item) => normalizeSpecialty(item.nombre) === normalizeSpecialty(name))?.id ?? 0;

  function changePalette(value: number) {
    const specialty = data.especialidades.find((item) => item.id === value);
    if (specialty) selectSpecialty(specialty.nombre, specialty.id);
    else resetSpecialty();
  }

  return <div className="toolbar filters admin-toolbar">
    {showBack && <Link className="button secondary" to="/admin">← Panel general</Link>}
    <label className="inline-filter">Paleta del sistema
      <AnimatedSelect ariaLabel="Paleta del sistema" value={selectedId} onChange={(value) => changePalette(Number(value))} options={[{ value: 0, label: 'Institucional (predeterminada)' }, ...data.especialidades.map((specialty) => ({ value: specialty.id, label: specialty.nombre }))]} />
    </label>
  </div>;
}

function AdminModule({ module, data, reload, status }: {
  module: (typeof modules)[number]; data: AdminCatalog; reload: () => Promise<void>; status: (message: string) => void;
}) {
  if (module.key === 'materias') return <MateriasPanel data={data} reload={reload} status={status} />;
  if (module.key === 'usuarios') return <UsuariosPanel data={data} reload={reload} status={status} />;
  if (module.key === 'asignaciones') return <AsignacionesPanel data={data} reload={reload} status={status} />;
  return <AlumnosPanel data={data} reload={reload} status={status} />;
}
