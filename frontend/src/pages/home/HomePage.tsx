import { useCallback, useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react';
import { useToast } from '../../context/toast';
import { getAsignacionesDisponibles, getMisAsignaciones, type AsignacionOption, type AsignacionCompleta } from '../../api/planCurricular';
import { getProfile } from '../../api/profile';
import { Link, useSearchParams } from 'react-router-dom';
import { createClass, getClaseActual, getHome, listarCodigosConducta, type ClaseActualDto, type CodigoConducta, type HomeResponse, type PlanillaResumenDto } from '../../api/home';
import { ApiError } from '../../api/client';
import AppShell from '../../components/AppShell';
import PageBanner from '../../components/PageBanner';
import LauncherCards from '../../components/LauncherCards';
import SpecialtyIcon from '../../components/SpecialtyIcon';
import ContentState from '../../components/ui/ContentState';
import { deletePortada, getEspecialidades, getPortadaBlob, resolvePlanilla, savePortada, syncClassroom, type Especialidad } from '../../api/academics';
import { useNavigate } from 'react-router-dom';
import AnimatedSelect from '../../components/AnimatedSelect';
import { useSpecialty } from '../../context/SpecialtyContext';
import PlanCurricularView from './PlanCurricularView';
import MisClasesView from './MisClasesView';
import CatalogoConductaPanel from '../../components/CatalogoConductaPanel';
import { useAuth } from '../../context/AuthContext';
import { classEndTime, HORARIOS_CATEDRA } from './classFormUtils';
import { resizeImageToDataUri } from '../../utils/imageResize';
import RasgosAsistenciaEditor from './RasgosAsistenciaEditor';
import { ActivityItem, Card, EmptyState, Skeleton } from '../../design-system';

const normalizeSpecialtyName = (value: string) => value
  .trim()
  .toLowerCase()
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .replace(/[-_]+/g, ' ')
  .replace(/\s+/g, ' ')
  .trim();

export default function HomePage() {
  const [search, setSearch] = useSearchParams();
  const [data, setData] = useState<HomeResponse | null>(null);
  const loadId = useRef(0);
  const [error, setError] = useState('');
  const [especialidades, setEspecialidades] = useState<Especialidad[]>([]);
  const view = search.get('view') || '';
  const subview = search.get('subview') || 'clase';
  const cursoId = Number(search.get('cursoId') || 0);
  const materiaIdFiltro = Number(search.get('materiaId') || 0);
  const etapa = Number(search.get('etapa') || 1);
  const especialidadId = Number(search.get('especialidadId') || 0);
  const selectedEspecialidad = especialidades.find((item) => item.id === especialidadId);
  const [selectedNivel, setSelectedNivel] = useState<number | null>(null);
  const [selectedSeccion, setSelectedSeccion] = useState<string | number | ''>('');
  const [selectionLoading, setSelectionLoading] = useState(false);
  const [syncingAll, setSyncingAll] = useState(false);
  const { selectSpecialty, resetSpecialty } = useSpecialty();

  const hasEspecialidad = !!especialidadId;
  const hasCursoSeleccionado = !!cursoId;
  const selectedCourseNivel = selectedNivel;
  const hasSeccionSeleccionada = !!selectedSeccion;
  const isClaseView = view === 'catedra' && subview === 'clase';
  const isPlanillasView = view === 'planillas';

  const setCourseSelection = (value: string | number) => {
    const nextNivel = Number(value) || null;
    if (!isPlanillasView) setSelectionLoading(true);
    setSelectedNivel(nextNivel);
    setSelectedSeccion('');
    params({ cursoId: '' });
  };

  // En planillas, cursoId es un filtro local sobre la lista ya cargada, no un
  // parámetro de la request: effectiveCursoId se mantiene en `undefined` pase
  // lo que pase con los selectores, así `load` no cambia de identidad y el
  // useEffect de abajo no dispara una request nueva por cada cambio de filtro.
  const effectiveCursoId = isClaseView ? (cursoId || undefined) : undefined;

  const load = useCallback(async () => {
    const request = ++loadId.current;
    try {
      const homeView = isClaseView ? 'clase' : 'planillas';
      const result = await getHome({ cursoId: effectiveCursoId, etapa, view: homeView });
      if (request === loadId.current) { setData(result); setError(''); }
    } catch (e) {
      if (request === loadId.current) setError(e instanceof ApiError ? e.message : 'Error al cargar el inicio.');
    } finally {
      if (request === loadId.current) setSelectionLoading(false);
    }
  }, [effectiveCursoId, etapa, isClaseView]);

  useEffect(() => {
    let active = true;

    void getEspecialidades()
      .then((catalog) => {
        if (!active) return;

        const scopedNames = Array.from(new Set((data?.cursos ?? []).map((curso) => curso.especialidad).filter((nombre): nombre is string => !!nombre && nombre.trim().length > 0)));

        // Always derive the combo from the specialties present in `data.cursos` when available.
        // Cross-match by normalized name against the official catalog to preserve real ids
        // and only create synthetic entries when no match exists for a given name.
        if (scopedNames.length > 0) {
          const catalogByName = new Map(catalog.map((item) => [normalizeSpecialtyName(item.nombre), item]));
          const derived = scopedNames
            .map((nombre, idx) => {
              const normalized = normalizeSpecialtyName(nombre);
              const match = catalogByName.get(normalized);
              return { id: match?.id ?? (Number.MIN_SAFE_INTEGER + idx), nombre } as Especialidad;
            })
            .filter((item, index, items) => items.findIndex((candidate) => normalizeSpecialtyName(candidate.nombre) === normalizeSpecialtyName(item.nombre)) === index);

          setEspecialidades(derived);
          return;
        }

        // If there are no specialties derived from courses, fall back to full catalog.
        setEspecialidades(catalog);
      })
      .catch(() => {
        if (!active) return;
        const scopedNames = Array.from(new Set((data?.cursos ?? []).map((curso) => curso.especialidad).filter((nombre): nombre is string => !!nombre && nombre.trim().length > 0)));
        setEspecialidades(scopedNames.length > 0
          ? scopedNames.map((nombre, index) => ({ id: 100000 + index, nombre } as Especialidad))
          : []);
      });

    return () => {
      active = false;
    };
  }, [data]);

  // Sync application palette with selected especialidad in query
  useEffect(() => {
    const id = Number(search.get('especialidadId') || 0);

    if (!id && especialidades.length === 1) {
      setSearch((prev) => {
        const next = new URLSearchParams(prev);
        next.set('especialidadId', String(especialidades[0].id));
        return next;
      });
      return;
    }

    if (id) {
      const s = especialidades.find((item) => item.id === id);
      if (s) selectSpecialty(s.nombre, s.id);
    } else {
      resetSpecialty();
    }
  }, [search, especialidades, selectSpecialty, resetSpecialty, setSearch]);

  useEffect(() => { void load(); }, [load]);

  useEffect(() => {
    if (!especialidadId) {
      setSelectedNivel(null);
      setSelectedSeccion('');
      return;
    }
  }, [especialidadId]);

  // Hidrata curso y sección desde `cursoId` en la URL (deep link a catedra/clase
  // o a planillas). Se ejecuta después del efecto de arriba: si ese efecto resetea
  // por venir sin especialidadId, esta hidratación sigue corriendo y prevalece.
  useEffect(() => {
    if (!data || !cursoId) return;
    if (selectedNivel !== null || selectedSeccion !== '') return;
    const curso = data.cursos.find((c) => c.id === cursoId);
    if (!curso) return;
    setSelectedNivel(Number(curso.curso) || null);
    setSelectedSeccion(curso.seccion);
  }, [cursoId, data, selectedNivel, selectedSeccion]);

  if (!view) return <AppShell title="Inicio" hero={false}>
    <HomeLauncher
      data={data}
      especialidades={especialidades}
      especialidadId={especialidadId}
      onEspecialidadChange={(value) => setSearch((prev) => {
        const next = new URLSearchParams(prev);
        if (value) next.set('especialidadId', value); else next.delete('especialidadId');
        return next;
      })}
      onSelect={(nextView, extra) => setSearch({
        view: nextView,
        ...(especialidadId ? { especialidadId: String(especialidadId) } : {}),
        ...extra,
      })}
    />
  </AppShell>;
  if (!data) return <AppShell title="Panel SCA"><ContentState tone={error ? 'error' : 'loading'} title={error || 'Cargando inicio…'} detail={error ? 'Recargá la página para volver a intentarlo.' : 'Estamos preparando tus cursos y planillas.'} /></AppShell>;

  const visibleCursos = selectedEspecialidad
    ? data.cursos.filter((curso) => curso.especialidad === selectedEspecialidad.nombre)
    : data.cursos;

  // const selectedCourseId = cursoId || data.selCurso?.id;

  // derive unique niveles and secciones for selectors
  const nivelesBase = Array.from(new Set(visibleCursos.map((c) => Number(c.curso)).filter((n) => !isNaN(n) && n > 0)));
  const niveles = nivelesBase.sort((a, b) => a - b);
  const seccionesForNivel = (nivel: number) => Array.from(new Set(visibleCursos.filter((c) => Number(c.curso) === nivel).map((c) => c.seccion))).sort();

  

  const params = (next: Record<string, string>) => setSearch({
    view,
    subview,
    etapa: String(data.selEtapa),
    especialidadId: String(especialidadId || ''),
    cursoId: String(cursoId || ''),
    ...next,
  });

  const courseOptions = niveles.map((n) => ({ value: n, label: `${n}°` }));
  const sectionOptions = (selectedCourseNivel != null ? seccionesForNivel(selectedCourseNivel) : []).map((s) => ({ value: s, label: String(s) }));

  const showSelectionWait = !hasEspecialidad || !hasCursoSeleccionado || !hasSeccionSeleccionada;
  const showSelector = view === 'planillas' || (view === 'catedra' && subview === 'clase');
  const hasActiveFilter = hasEspecialidad || selectedCourseNivel != null || hasSeccionSeleccionada;
  const clearFilter = () => {
    setSelectedNivel(null);
    setSelectedSeccion('');
    setSearch({ view, etapa: String(data.selEtapa) });
    resetSpecialty();
  };

  return <>
    <style>{`
      @keyframes idlePulse {
        0%, 100% { opacity: 0.35; transform: scale(0.96); }
        50% { opacity: 1; transform: scale(1); }
      }
      .idle-state {
        display: grid;
        gap: 1rem;
        text-align: center;
        padding: 2rem 1.5rem;
        border: 1px dashed #d0d7e2;
        background: linear-gradient(135deg, rgba(148, 163, 184, 0.08), rgba(59, 130, 246, 0.04));
      }
      .idle-dots {
        display: flex;
        justify-content: center;
        gap: 0.5rem;
      }
      .idle-dot {
        width: 0.7rem;
        height: 0.7rem;
        border-radius: 999px;
        background: #3b82f6;
        animation: idlePulse 1.2s ease-in-out infinite;
      }
      .idle-dot:nth-child(2) { animation-delay: 0.15s; }
      .idle-dot:nth-child(3) { animation-delay: 0.3s; }
    `}</style>
    <AppShell title="Panel SCA del curso" specialty={selectedEspecialidad?.nombre ?? null} hero={false}>
      <PageBanner
        title="Panel SCA del curso"
        context={view === 'catedra' ? `${data.cursos.length} curso${data.cursos.length === 1 ? '' : 's'}${selectedEspecialidad ? ` · ${selectedEspecialidad.nombre}` : ''}` : undefined}
        specialty={selectedEspecialidad?.nombre ?? null}
        onBack={() => setSearch({})}
      />
      {view === 'catedra' && <CatedraTabs subview={subview} params={params} />}
      {showSelector && <div className="toolbar filters">
        <label className="inline-filter">Especialidad
          <AnimatedSelect ariaLabel="Especialidad" value={especialidadId || ''} onChange={(value) => {
            setSelectedNivel(null);
            setSelectedSeccion('');
            params({ especialidadId: value, cursoId: '' });
            const id = Number(value || 0);
            if (id) {
              const s = especialidades.find((item) => item.id === id);
              if (s) selectSpecialty(s.nombre, s.id);
            } else {
              resetSpecialty();
            }
          }} placeholder={isPlanillasView ? 'Todas las especialidades' : 'Seleccione la especialidad'} options={[{ value: '', label: isPlanillasView ? 'Todas las especialidades' : 'Seleccione la especialidad' }, ...especialidades.map((item) => ({ value: item.id, label: item.nombre }))]} />
        </label>
        <label className="inline-filter">Curso
          <AnimatedSelect ariaLabel="Curso" value={selectedCourseNivel ?? ''} onChange={(value) => {
            setCourseSelection(value);
          }} disabled={!hasEspecialidad || visibleCursos.length === 0} placeholder={isPlanillasView ? 'Todos los cursos' : 'Seleccione el curso'} options={[{ value: '', label: isPlanillasView ? 'Todos los cursos' : 'Seleccione el curso' }, ...courseOptions]} />
        </label>
        <label className="inline-filter">Sección
            <AnimatedSelect ariaLabel="Sección" value={selectedSeccion ?? ''} onChange={(value) => {
            if (!isPlanillasView) setSelectionLoading(true);
            setSelectedSeccion(value);
            const nivel = selectedCourseNivel ?? undefined;
            const seccion = String(value);
            const match = visibleCursos.find((c) => (nivel == null || Number(c.curso) === nivel) && c.seccion === seccion && (!selectedEspecialidad || c.especialidad === selectedEspecialidad.nombre));
            params({ cursoId: match ? String(match.id) : '' });
          }} disabled={!hasEspecialidad || selectedCourseNivel == null || visibleCursos.length === 0} placeholder={isPlanillasView ? 'Todas las secciones' : 'Seleccione la sección'} options={[{ value: '', label: isPlanillasView ? 'Todas las secciones' : 'Seleccione la sección' }, ...sectionOptions]} />
        </label>
        {isPlanillasView && hasActiveFilter && <button type="button" className="button secondary planillas-toolbar-clear" onClick={clearFilter}>Limpiar filtro</button>}
      </div>}
      {view === 'catedra' ? (
        <div role="tabpanel" id={`catedra-panel-${subview}`} aria-labelledby={`catedra-tab-${subview}`} tabIndex={-1}>
          {subview === 'plan-curricular' ? (
            <PlanCurricularView />
          ) : subview === 'mis-clases' ? (
            <MisClasesView />
          ) : selectionLoading ? (
            <section className="panel idle-state"><div className="idle-dots" aria-hidden="true"><span className="idle-dot" /><span className="idle-dot" /><span className="idle-dot" /></div><h2>Cargando…</h2><p>Esperá un momento mientras preparamos la clase.</p></section>
          ) : showSelectionWait ? (
            <p className="catedra-select-hint">Elegí una especialidad, un curso y una sección para continuar.</p>
          ) : (
            <ClassView key={data.selCurso?.id} data={data} reload={load} />
          )}
        </div>
      ) : (
        <PlanillasView
          data={data}
          syncingProp={syncingAll}
          setSyncingProp={setSyncingAll}
          especialidadNombre={selectedEspecialidad?.nombre ?? null}
          nivel={selectedNivel}
          seccion={selectedSeccion}
          materiaId={materiaIdFiltro || null}
          hasActiveFilter={hasActiveFilter}
          onClearFilter={clearFilter}
        />
      )}
    </AppShell>
  </>;
}

function splitActivityLine(line: string): { date: string; message: string } | null {
  if (!line.startsWith('[')) return null;
  const closeIdx = line.indexOf('] ');
  if (closeIdx <= 0) return null;
  return { date: line.slice(1, closeIdx), message: line.slice(closeIdx + 2) };
}

// ActivityLogService escribe fechas con LINE_FORMATTER = "yyyy-MM-dd HH:mm:ss".
function humanizeActivityDate(raw: string): string {
  const match = raw.match(/^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):\d{2}$/);
  if (!match) return raw;
  const [, y, mo, d, h, mi] = match;
  const date = new Date(Number(y), Number(mo) - 1, Number(d));
  if (Number.isNaN(date.getTime())) return raw;
  const startOfDay = (dt: Date) => new Date(dt.getFullYear(), dt.getMonth(), dt.getDate()).getTime();
  const diffDays = Math.round((startOfDay(new Date()) - startOfDay(date)) / 86400000);
  const time = `${h}:${mi}`;
  if (diffDays === 0) return `hoy ${time}`;
  if (diffDays === 1) return `ayer ${time}`;
  return `${d}/${mo} ${time}`;
}

const RECENT_MATERIAS_KEY = 'sca:materias-recientes:v1';
const RECENT_MATERIAS_LIMIT = 10;

function readRecentMaterias(): number[] {
  try {
    const raw = localStorage.getItem(RECENT_MATERIAS_KEY);
    if (!raw) return [];
    const parsed: unknown = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.filter((id): id is number => typeof id === 'number') : [];
  } catch {
    return [];
  }
}

function recordMateriaReciente(asignacionId: number) {
  try {
    const current = readRecentMaterias().filter((id) => id !== asignacionId);
    current.unshift(asignacionId);
    localStorage.setItem(RECENT_MATERIAS_KEY, JSON.stringify(current.slice(0, RECENT_MATERIAS_LIMIT)));
  } catch {
    /* sin localStorage disponible: el orden simplemente no persiste */
  }
}

function sortByRecentUsage(asignaciones: AsignacionCompleta[]): AsignacionCompleta[] {
  const recent = readRecentMaterias();
  const rank = new Map(recent.map((id, idx) => [id, idx]));
  return [...asignaciones].sort((a, b) => {
    const ra = rank.get(a.id) ?? Number.MAX_SAFE_INTEGER;
    const rb = rank.get(b.id) ?? Number.MAX_SAFE_INTEGER;
    return ra - rb;
  });
}

function HomeLauncher({ data, especialidades, especialidadId, onEspecialidadChange, onSelect }: {
  data: HomeResponse | null;
  especialidades: Especialidad[];
  especialidadId: number;
  onEspecialidadChange: (value: string) => void;
  onSelect: (view: string, extra?: Record<string, string>) => void;
}) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [asignaciones, setAsignaciones] = useState<AsignacionCompleta[] | null>(null);
  const [activity, setActivity] = useState<string[] | null>(null);

  useEffect(() => {
    let active = true;
    void getMisAsignaciones()
      .then((list) => { if (active) setAsignaciones(list); })
      .catch(() => { /* sin datos: no se muestra badge de plan curricular */ });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    let active = true;
    void getProfile()
      .then((profile) => { if (active) setActivity(profile.activityLog ?? []); })
      .catch(() => { if (active) setActivity([]); });
    return () => { active = false; };
  }, []);

  const firstName = user?.displayName?.trim().split(/\s+/)[0];
  const todayLabel = (() => {
    const raw = new Date().toLocaleDateString('es-ES', { weekday: 'long', day: 'numeric', month: 'long' });
    return raw.charAt(0).toUpperCase() + raw.slice(1);
  })();

  const rechazados = asignaciones?.filter((a) => a.estadoPlan === 'RECHAZADO').length ?? 0;
  const noCargados = asignaciones?.filter((a) => a.estadoPlan === 'NO_CARGADO').length ?? 0;

  const recentActivity = (activity ?? [])
    .filter((line) => splitActivityLine(line)?.message !== 'Inició sesión')
    .slice(-5)
    .reverse();

  const selectedEspecialidad = especialidades.find((item) => item.id === especialidadId);

  return <div className="home-launcher">
    <PageBanner
      title={firstName ? `Hola, ${firstName}` : 'Hola'}
      context={data && `${todayLabel} · ${data.cursos.length} curso${data.cursos.length === 1 ? '' : 's'}${selectedEspecialidad ? ` · ${selectedEspecialidad.nombre}` : ''}`}
      specialty={selectedEspecialidad?.nombre ?? (especialidades.length === 1 ? especialidades[0].nombre : '')}
      selector={especialidades.length > 1 && (
        <label className="inline-filter">Especialidad
          <AnimatedSelect ariaLabel="Especialidad" value={especialidadId || ''} onChange={onEspecialidadChange} placeholder="Seleccione la especialidad" options={[{ value: '', label: 'Seleccione la especialidad' }, ...especialidades.map((item) => ({ value: item.id, label: item.nombre }))]} />
        </label>
      )}
    />
    <div className="launcher-body">
      <LauncherCards options={[
        {
          key: 'catedra',
          icon: <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M4 5.5c2.2-1 5-1 8 .3V19c-3-1.3-5.8-1.3-8-.3V5.5Z" /><path d="M20 5.5c-2.2-1-5-1-8 .3V19c3-1.3 5.8-1.3 8-.3V5.5Z" /></svg>,
          title: 'Libro de Cátedra',
          description: 'Plan curricular e inicio de clases.',
          onSelect: () => onSelect('catedra'),
          badges: asignaciones && (rechazados > 0
            ? <span className="launcher-badge tone-danger">{rechazados} plan{rechazados === 1 ? '' : 'es'} rechazado{rechazados === 1 ? '' : 's'}</span>
            : noCargados > 0
              ? <span className="launcher-badge tone-warning">{noCargados} plan{noCargados === 1 ? '' : 'es'} sin cargar</span>
              : <span className="launcher-badge tone-success">Planes al día</span>),
        },
        {
          key: 'planillas',
          icon: <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><rect x="6" y="4" width="12" height="17" rx="2" /><path d="M9 4V3a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v1" /><path d="M9 11h6M9 15h6" /></svg>,
          title: 'Gestionar planillas',
          description: 'Tareas, puntajes y sincronización con Classroom.',
          onSelect: () => onSelect('planillas'),
          badges: <>
            {data && data.planillas.length > 0 && <span className="launcher-badge tone-accent">{data.planillas.length} planilla{data.planillas.length === 1 ? '' : 's'} activa{data.planillas.length === 1 ? '' : 's'}</span>}
            {data && data.googleClassroomConnected === false && <span className="launcher-badge tone-warning">Classroom sin conectar</span>}
          </>,
        },
      ]} />
      <aside className="sca-ds launcher-activity-design">
        <Card title="Actividad reciente" headingLevel={2}>
        {activity === null ? (
          <Skeleton label="Cargando actividad…" />
        ) : recentActivity.length === 0 ? (
          <EmptyState title="Todavía no hay actividad registrada." description="" />
        ) : (
          <ul className="ds-activity-list">
            {recentActivity.map((line, idx) => {
              const parsed = splitActivityLine(line);
              if (!parsed) return <li className="ds-activity" key={idx}>{line}</li>;
              const type = /clase/i.test(parsed.message) ? 'class' : /planilla|tarea|puntaje/i.test(parsed.message) ? 'grades' : /revis|aprob|rechaz/i.test(parsed.message) ? 'review' : /perfil|contraseña/i.test(parsed.message) ? 'profile' : 'other';
              return <ActivityItem key={idx} type={type} text={parsed.message} dateTime={/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(parsed.date) ? parsed.date.replace(' ', 'T') : undefined} timestamp={humanizeActivityDate(parsed.date)} />;
            })}
          </ul>
        )}
        </Card>
      </aside>
    </div>
    {asignaciones && asignaciones.length > 0 && (
      <div className="launcher-materias">
        <h3>Tus materias</h3>
        <div className="launcher-materias-list">
          {sortByRecentUsage(asignaciones).map((asignacion) => {
            const estado = asignacion.estadoPlan === 'RECHAZADO'
              ? { tone: 'tone-danger', label: 'Plan rechazado' }
              : asignacion.estadoPlan === 'NO_CARGADO'
                ? { tone: 'tone-warning', label: 'Plan sin cargar' }
                : null;
            return (
              <button type="button" key={asignacion.id} className="launcher-materia-row" onClick={() => {
                recordMateriaReciente(asignacion.id);
                const planillaExistente = asignacion.cursoRealId != null
                  ? data?.planillasResumen.find((p) => p.materiaId === asignacion.materiaId && p.cursoId === asignacion.cursoRealId)
                  : undefined;
                if (planillaExistente) {
                  navigate(`/planilla/${planillaExistente.id}`);
                  return;
                }
                onSelect('planillas', {
                  materiaId: String(asignacion.materiaId),
                  especialidadId: String(asignacion.especialidadId),
                  ...(asignacion.cursoRealId != null ? { cursoId: String(asignacion.cursoRealId) } : {}),
                });
              }}>
                <span className="launcher-materia-info">
                  <strong>{asignacion.materiaNombre}</strong>
                  <span>{asignacion.cursoOrdinal}° {asignacion.seccion}</span>
                </span>
                {estado && <span className={`launcher-materia-status ${estado.tone}`} role="img" aria-label={estado.label} title={estado.label} />}
              </button>
            );
          })}
        </div>
      </div>
    )}
  </div>;
}

const CATEDRA_TABS = [
  { key: 'clase', label: 'Iniciar clase' },
  { key: 'plan-curricular', label: 'Plan curricular' },
  { key: 'mis-clases', label: 'Clases dadas' },
] as const;

function CatedraTabs({ subview, params }: { subview: string; params: (next: Record<string, string>) => void }) {
  const [asignaciones, setAsignaciones] = useState<AsignacionCompleta[] | null>(null);
  const tabRefs = useRef<Array<HTMLButtonElement | null>>([]);

  useEffect(() => {
    let active = true;
    void getMisAsignaciones().then((list) => { if (active) setAsignaciones(list); }).catch(() => { /* sin datos: sin punto de estado en la pestaña */ });
    return () => { active = false; };
  }, []);

  const hayRechazados = asignaciones?.some((a) => a.estadoPlan === 'RECHAZADO') ?? false;
  const haySinCargar = asignaciones?.some((a) => a.estadoPlan === 'NO_CARGADO') ?? false;
  const estadoPlanTono = hayRechazados ? 'danger' : haySinCargar ? 'warning' : null;
  const estadoPlanTexto = hayRechazados ? 'Hay planes rechazados' : 'Hay planes sin cargar';

  const moveFocus = (index: number, delta: number) => {
    const nextIndex = (index + delta + CATEDRA_TABS.length) % CATEDRA_TABS.length;
    params({ subview: CATEDRA_TABS[nextIndex].key });
    tabRefs.current[nextIndex]?.focus();
  };

  const onKeyDown = (event: KeyboardEvent<HTMLButtonElement>, index: number) => {
    if (event.key === 'ArrowRight') { event.preventDefault(); moveFocus(index, 1); }
    else if (event.key === 'ArrowLeft') { event.preventDefault(); moveFocus(index, -1); }
  };

  return (
    <div className="catedra-tablist" role="tablist" aria-label="Secciones del libro de cátedra">
      {CATEDRA_TABS.map((tab, index) => {
        const active = tab.key === subview;
        return (
          <button
            key={tab.key}
            ref={(el) => { tabRefs.current[index] = el; }}
            type="button"
            role="tab"
            id={`catedra-tab-${tab.key}`}
            aria-selected={active}
            aria-controls={`catedra-panel-${tab.key}`}
            tabIndex={active ? 0 : -1}
            className={`catedra-tab${active ? ' active' : ''}`}
            onClick={() => params({ subview: tab.key })}
            onKeyDown={(event) => onKeyDown(event, index)}
          >
            {tab.label}
            {tab.key === 'plan-curricular' && estadoPlanTono && <>
              <span className={`catedra-tab-dot tone-${estadoPlanTono}`} aria-hidden="true" />
              <span className="visually-hidden">{estadoPlanTexto}</span>
            </>}
          </button>
        );
      })}
    </div>
  );
}

export function PlanillasView({ data, syncingProp, setSyncingProp, especialidadNombre, nivel, seccion, materiaId, hasActiveFilter, onClearFilter }: {
  data: HomeResponse;
  syncingProp?: boolean;
  setSyncingProp?: (v: boolean) => void;
  especialidadNombre: string | null;
  nivel: number | null;
  seccion: string | number | '';
  materiaId: number | null;
  hasActiveFilter: boolean;
  onClearFilter: () => void;
}) {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [asignaciones, setAsignaciones] = useState<AsignacionCompleta[] | null>(null);
  const [portadaOverrides, setPortadaOverrides] = useState<Record<number, boolean>>({});
  const [creatingKey, setCreatingKey] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    void getMisAsignaciones()
      .then((list) => { if (active) setAsignaciones(list); })
      .catch(() => { if (active) setAsignaciones([]); });
    return () => { active = false; };
  }, []);

  // Auto-sync planillas in background when Classroom is connected. Ahora cubre
  // todos los cursos (planillasResumen), no solo el que antes quedaba fijado.
  useEffect(() => {
    if (!data.googleClassroomConnected) return;
    let cancelled = false;
    (async () => {
      setSyncingProp?.(true);
      try {
        for (const p of data.planillasResumen) {
          if (cancelled) break;
          try {
            await syncClassroom(p.id);
          } catch (err) {
            // si el backend indica que faltan scopes, redirigir a autorización
            if (err instanceof ApiError && err.status === 428) {
              navigate('/google/authorize');
              break;
            }
            // ignore otros errores por planilla
          }
        }
      } finally {
        if (!cancelled) setSyncingProp?.(false);
      }
    })();
    return () => { cancelled = true; setSyncingProp?.(false); };
  }, [data.googleClassroomConnected, data.planillasResumen, navigate, setSyncingProp]);

  const syncing = syncingProp ?? false;

  const matchesFilter = (item: { especialidadNombre: string; cursoOrdinal: string; seccion: string }) => {
    if (especialidadNombre && item.especialidadNombre !== especialidadNombre) return false;
    if (nivel != null && parseInt(item.cursoOrdinal, 10) !== nivel) return false;
    if (seccion && item.seccion !== String(seccion)) return false;
    return true;
  };

  const planillasFiltradas = data.planillasResumen
    .filter(matchesFilter)
    .filter((p) => !materiaId || p.materiaId === materiaId);

  const existentes = new Set(data.planillasResumen.map((p) => `${p.materiaId}:${p.cursoId}`));
  const candidatosTodos = (asignaciones ?? []).filter((a) => a.cursoRealId != null && !existentes.has(`${a.materiaId}:${a.cursoRealId}`));
  const candidatos = candidatosTodos
    .filter(matchesFilter)
    .filter((a) => !materiaId || a.materiaId === materiaId);

  async function crearPlanilla(candidato: AsignacionCompleta) {
    if (candidato.cursoRealId == null || creatingKey) return;
    const key = `${candidato.materiaId}:${candidato.cursoRealId}`;
    setCreatingKey(key);
    try {
      const result = await resolvePlanilla(candidato.cursoRealId, candidato.materiaId, data.selEtapa);
      navigate(`/planilla/${result.planillaId}`);
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : 'No se pudo crear la planilla.', { tone: 'error', autoDismiss: true });
      setCreatingKey(null);
    }
  }

  const cargandoCandidatos = asignaciones === null;
  const totalSinFiltro = data.planillasResumen.length + candidatosTodos.length;
  const totalConFiltro = planillasFiltradas.length + candidatos.length;

  return <>
    <section className="summary-grid">
      <article className="metric"><span>Planillas</span><strong>{data.planillasResumen.length}</strong></article>
      <article className="metric"><span>Classroom</span><strong>{syncing ? 'Sincronizando…' : (data.googleClassroomConnected ? 'Conectado' : 'Sin conexión')}</strong></article>
    </section>
    {totalSinFiltro === 0 && !cargandoCandidatos ? (
      <section className="panel empty-state"><h2>Todavía no tenés planillas</h2><p>Cuando tengas materias asignadas vas a poder crearlas acá.</p></section>
    ) : hasActiveFilter && totalConFiltro === 0 && !cargandoCandidatos ? (
      <section className="panel empty-state"><h2>No hay planillas con ese filtro</h2><p>Probá con otra especialidad, curso o sección.</p><button type="button" className="button secondary" onClick={onClearFilter}>Limpiar filtro</button></section>
    ) : (
      <div className="card-grid">
        {planillasFiltradas.map((p) => (
          <PlanillaExistingCard
            key={p.id}
            item={p}
            tienePortada={portadaOverrides[p.id] ?? p.tienePortada}
            onPortadaChanged={(tienePortada) => setPortadaOverrides((current) => ({ ...current, [p.id]: tienePortada }))}
          />
        ))}
        {candidatos.map((a) => {
          const key = `${a.materiaId}:${a.cursoRealId}`;
          return <button type="button" key={key} className="planilla-create-card" disabled={creatingKey != null} onClick={() => void crearPlanilla(a)}>
            <span className="planilla-create-card-plus" aria-hidden="true">+</span>
            <strong>{a.materiaNombre}</strong>
            <span>{a.cursoOrdinal} {a.seccion}</span>
            <span>{creatingKey === key ? 'Creando…' : 'Crear planilla'}</span>
          </button>;
        })}
      </div>
    )}
  </>;
}

function PlanillaExistingCard({ item, tienePortada, onPortadaChanged }: {
  item: PlanillaResumenDto;
  tienePortada: boolean;
  onPortadaChanged: (tienePortada: boolean) => void;
}) {
  const [coverVersion, setCoverVersion] = useState(0);
  return (
    <article className="planilla-card">
      <div className="planilla-card-cover">
        <PlanillaCover planillaId={item.id} tienePortada={tienePortada} especialidadNombre={item.especialidadNombre} version={coverVersion} />
        <PlanillaCoverControls
          planillaId={item.id}
          tienePortada={tienePortada}
          onChanged={(next) => { onPortadaChanged(next); setCoverVersion((v) => v + 1); }}
        />
      </div>
      <div className="planilla-card-body">
        <span>{item.especialidadNombre}</span>
        <h2><Link className="planilla-card-link" to={`/planilla/${item.id}`}>{item.materiaNombre}</Link></h2>
        <p>{item.cursoOrdinal} {item.seccion} · Etapa {item.etapa}</p>
      </div>
    </article>
  );
}

function PlanillaCover({ planillaId, tienePortada, especialidadNombre, version }: {
  planillaId: number;
  tienePortada: boolean;
  especialidadNombre: string;
  version: number;
}) {
  const [url, setUrl] = useState<string | null>(null);

  useEffect(() => {
    if (!tienePortada) { setUrl(null); return; }
    let active = true;
    let objectUrl: string | null = null;
    void getPortadaBlob(planillaId).then((blob) => {
      if (!active || !blob) return;
      objectUrl = URL.createObjectURL(blob);
      setUrl(objectUrl);
    });
    return () => {
      active = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [planillaId, tienePortada, version]);

  if (tienePortada && url) return <img src={url} alt="" />;
  return <div className="planilla-card-cover-fallback"><SpecialtyIcon name={especialidadNombre} /></div>;
}

function PlanillaCoverControls({ planillaId, tienePortada, onChanged }: {
  planillaId: number;
  tienePortada: boolean;
  onChanged: (tienePortada: boolean) => void;
}) {
  const { showToast } = useToast();
  const [busy, setBusy] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  async function handleFile(file: File) {
    setBusy(true);
    try {
      const dataUri = await resizeImageToDataUri(file);
      await savePortada(planillaId, dataUri);
      onChanged(true);
      showToast('Portada actualizada.', { autoDismiss: true });
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : 'No se pudo subir la portada.', { tone: 'error', autoDismiss: true });
    } finally {
      setBusy(false);
      if (inputRef.current) inputRef.current.value = '';
    }
  }

  async function handleRemove() {
    setBusy(true);
    try {
      await deletePortada(planillaId);
      onChanged(false);
      showToast('Portada eliminada.', { autoDismiss: true });
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : 'No se pudo eliminar la portada.', { tone: 'error', autoDismiss: true });
    } finally {
      setBusy(false);
    }
  }

  return <>
    {busy && <div className="planilla-card-cover-progress">Subiendo…</div>}
    <div className="planilla-card-cover-controls">
      <input
        ref={inputRef}
        type="file"
        accept="image/png,image/jpeg,image/webp"
        style={{ display: 'none' }}
        onChange={(event) => { const file = event.target.files?.[0]; if (file) void handleFile(file); }}
      />
      <button type="button" className="planilla-card-cover-btn" disabled={busy} onClick={() => inputRef.current?.click()}>{tienePortada ? 'Cambiar portada' : 'Agregar portada'}</button>
      {tienePortada && <button type="button" className="planilla-card-cover-btn" disabled={busy} onClick={() => void handleRemove()}>Quitar</button>}
    </div>
  </>;
}

export function ClassView({ data, reload }: { data: HomeResponse; reload: () => Promise<void> }) {
  const selectedCursoId = data.selCurso?.id;
  const { user } = useAuth();
  const [tema, setTema] = useState('');
  const [asignacionesDisponibles, setAsignacionesDisponibles] = useState<AsignacionOption[]>([]);
  const [assignmentsLoading, setAssignmentsLoading] = useState(true);
  const [assignmentError, setAssignmentError] = useState('');
  const [assignmentAttempt, setAssignmentAttempt] = useState(0);
  const [saving, setSaving] = useState(false);
  const submitting = useRef(false);
  const [selectedAsignacionId, setSelectedAsignacionId] = useState<number | null>(null);
  const [disciplina, setDisciplina] = useState('');
  const [instrumentoId, setInstrumentoId] = useState(0);
  const [ausentes, setAusentes] = useState<number[]>([]);
  const [status, setStatus] = useState('');
  const [horario, setHorario] = useState('');
  const [cantidadHoras, setCantidadHoras] = useState('');
  const [modalidad, setModalidad] = useState('Presencial');
  const [observaciones, setObservaciones] = useState('');
  const [codigosPorAlumno, setCodigosPorAlumno] = useState<Record<number, string[]>>({});
  const [codigosConducta, setCodigosConducta] = useState<CodigoConducta[]>([]);
  const [codigosConductaError, setCodigosConductaError] = useState('');
  const [codigosConductaAttempt, setCodigosConductaAttempt] = useState(0);
  const [requiereJustificacion, setRequiereJustificacion] = useState(false);
  const [justificacionAtraso, setJustificacionAtraso] = useState('');
  const justificacionRef = useRef<HTMLTextAreaElement>(null);
  const canManageCodes = user?.level === 2 || user?.level === 3;

  useEffect(() => {
    let active = true;
    setCodigosConductaError('');
    void listarCodigosConducta()
      .then((codes) => { if (active) setCodigosConducta(codes); })
      .catch((err) => {
        if (!active) return;
        setCodigosConductaError(err instanceof ApiError ? err.message : 'No se pudieron cargar los códigos de conducta.');
      });
    return () => { active = false; };
  }, [codigosConductaAttempt]);

  useEffect(() => {
    if (requiereJustificacion) justificacionRef.current?.focus();
  }, [requiereJustificacion]);

  const [claseActual, setClaseActual] = useState<ClaseActualDto | null>(null);
  const [autoTemaAplicado, setAutoTemaAplicado] = useState(false);
  const autoFieldsApplied = useRef<number | null>(null);
  const temaTouched = useRef(false);
  const scheduleTouched = useRef(false);

  useEffect(() => {
    let active = true;
    setAssignmentsLoading(true); setAssignmentError('');
    setAsignacionesDisponibles([]); setSelectedAsignacionId(null);
    if (!selectedCursoId) { setAssignmentsLoading(false); return; }
    void getAsignacionesDisponibles(selectedCursoId).then((list) => {
      if (!active) return;
      setAsignacionesDisponibles(list);
      if (list.length === 1) setSelectedAsignacionId(list[0].id);
    }).catch((err) => {
      if (!active) return;
      const message = err instanceof ApiError ? err.message : 'No se pudieron consultar las asignaciones. Reintentá la carga.';
      setAssignmentError(message);
    }).finally(() => { if (active) setAssignmentsLoading(false); });
    return () => { active = false; };
  }, [selectedCursoId, assignmentAttempt]);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const info = await getClaseActual();
        if (active) setClaseActual(info);
      } catch {
        if (active) setClaseActual(null);
      }
    })();
    return () => { active = false; };
  }, []);

  useEffect(() => {
    if (claseActual?.hasClaseAhora && claseActual.cursoId === selectedCursoId && asignacionesDisponibles.some((a) => a.id === claseActual.asignacionId)) {
      setSelectedAsignacionId((current) => current ?? claseActual.asignacionId!);
    }
  }, [claseActual, selectedCursoId, asignacionesDisponibles]);

  useEffect(() => {
    const assignment = asignacionesDisponibles.find((a) => a.id === selectedAsignacionId);
    setDisciplina(assignment?.materiaNombre ?? '');
    if (!assignment || !claseActual?.hasClaseAhora || claseActual.cursoId !== data.selCurso?.id || claseActual.asignacionId !== assignment.id) return;
    if (autoFieldsApplied.current === assignment.id) return;
    autoFieldsApplied.current = assignment.id;
    if (claseActual.temaSugerido && !temaTouched.current) {
      setTema(claseActual.temaSugerido.slice(0, 150)); setAutoTemaAplicado(true);
    }
    const start = claseActual.horaInicio?.replace(/^0/, '');
    if (!scheduleTouched.current && start && HORARIOS_CATEDRA.includes(start)) { setHorario(start); setCantidadHoras('1'); }
  }, [claseActual, data.selCurso?.id, asignacionesDisponibles, selectedAsignacionId]);

  function toggleCodigo(alumnoId: number, codigo: string) {
    const codigosActuales = codigosPorAlumno[alumnoId] ?? [];
    const codigosActualizados = codigosActuales.includes(codigo)
      ? codigosActuales.filter((item) => item !== codigo)
      : [...codigosActuales, codigo];
    setCodigosPorAlumno((current) => ({ ...current, [alumnoId]: codigosActualizados }));
  }

  const handleCantidadHorasInput = (value: string) => {
    scheduleTouched.current = true;
    const sanitized = value.replace(/\D/g, '').slice(0, 2);
    setCantidadHoras(sanitized);
  };

  // La selección debe pertenecer a las asignaciones cargadas para este curso.
  const asignacionActual = asignacionesDisponibles.find((a) => a.id === selectedAsignacionId)
    ?? (asignacionesDisponibles.length === 1 ? asignacionesDisponibles[0] : undefined);
  const puedeIniciarClase = !assignmentsLoading && !assignmentError && !!asignacionActual && !saving;

  let mensajeBloqueo = '';
  if (assignmentsLoading) {
    mensajeBloqueo = 'Cargando asignaciones…';
  } else if (assignmentError) {
    mensajeBloqueo = assignmentError;
  } else if (asignacionesDisponibles.length === 0) {
    mensajeBloqueo = 'No hay asignaciones disponibles para este curso.';
  } else if (asignacionesDisponibles.length > 1 && !selectedAsignacionId) {
    mensajeBloqueo = 'Elegí primero tu asignación.';
  }

  const horarioFinal = classEndTime(horario, Number(cantidadHoras));

  async function create(e: FormEvent) {
    e.preventDefault();
    if (submitting.current) return;
    if (!data.selCurso || !puedeIniciarClase) {
      setStatus(mensajeBloqueo || 'No puedes iniciar clases en este momento.');
      return;
    }
    submitting.current = true; setSaving(true);
    try {
      const asignacionUsada = selectedAsignacionId ?? asignacionActual?.id ?? null;
      await createClass({
        cursoId: data.selCurso.id, asignacionId: asignacionUsada, etapa: data.selEtapa, instrumentoId,
        horaInicio: horario, horasCatedra: cantidadHoras ? Number(cantidadHoras) : null, modalidad, observaciones,
        tema, justificacionAtraso: requiereJustificacion ? justificacionAtraso : undefined,
        alumnosAusentes: ausentes, codigosPorAlumno,
      });
      if (asignacionUsada) recordMateriaReciente(asignacionUsada);
      clearForm();
      setStatus('Clase registrada.');
      await reload();
    } catch (err) {
      if (err instanceof ApiError && err.status === 400 && err.message === 'Se requiere justificar el atraso para este tema.') {
        setRequiereJustificacion(true);
        setStatus('');
      } else {
        setStatus(err instanceof ApiError ? err.message : 'No se pudo registrar la clase.');
      }
    } finally { submitting.current = false; setSaving(false); }
  }

  function clearForm() {
    scheduleTouched.current = true;
    temaTouched.current = true; setAutoTemaAplicado(false);
    setCodigosPorAlumno({});
    setTema('');
    setHorario('');
    setCantidadHoras('');
    setModalidad('Presencial');
    setInstrumentoId(0);
    setAusentes([]);
    setObservaciones('');
    setRequiereJustificacion(false);
    setJustificacionAtraso('');
    setStatus('');
  }

  return (
    <div className="two-column">
      {canManageCodes && <CatalogoConductaPanel onCodesChange={setCodigosConducta} />}
      {!puedeIniciarClase && mensajeBloqueo && (
        <div className="panel" style={{ gridColumn: '1 / -1', marginBottom: 0 }}>
          <div className="notice error" style={{ marginBottom: 12 }}>
            <p style={{ margin: 0 }}>{mensajeBloqueo}</p>
          </div>
        </div>
      )}
      {claseActual?.hasClaseAhora && claseActual.cursoId === data.selCurso?.id && claseActual.asignacionId === selectedAsignacionId && (
        <div className="panel" style={{ gridColumn: '1 / -1', marginBottom: 0 }}>
          <div className="notice" role="status" style={{ margin: 0 }}>
            <p style={{ margin: 0 }}>
              <strong>Clase en curso según tu horario:</strong> {claseActual.materia} — {claseActual.cursoDescripcion}
              {claseActual.horaInicio ? ` (${claseActual.horaInicio}${claseActual.horaFin ? '–' + claseActual.horaFin : ''})` : ''}
              {autoTemaAplicado ? '. Se autocompletó el tema según tu plan curricular aprobado.' : '.'}
            </p>
          </div>
        </div>
      )}
      <form className="panel class-register-form" onSubmit={create} aria-busy={saving} style={{ display: 'grid', gap: 12, gridColumn: '1 / -1' }}>
        <input type="hidden" name="action" value="create-rasgo-planilla" />
        <input type="hidden" name="cursoId" value={data.selCurso ? String(data.selCurso.id) : ''} id="formCursoId" />
        <input type="hidden" name="etapa" value={String(data.selEtapa)} />

        <div className="class-card">
          <div className="class-card-head">
            <div><span className="eyebrow">Inicio de clase</span><h2>Registro de clase</h2><p>Confirmá la asignación, completá el contenido y registrá la asistencia.</p></div>
            <button type="button" className="button secondary" id="clearButton" disabled={saving} onClick={clearForm}>Limpiar formulario</button>
          </div>
          <div className="class-grid">
            <div className="class-field">
              <label>Asignación / materia</label>
              <AnimatedSelect ariaLabel="Asignación de la clase" value={selectedAsignacionId ?? ''} placeholder={assignmentsLoading ? "Cargando asignaciones…" : "Seleccioná la asignación"} disabled={assignmentsLoading || saving || asignacionesDisponibles.length === 0} onChange={(value) => { setSelectedAsignacionId(value ? Number(value) : null); setTema(''); temaTouched.current = false; setAutoTemaAplicado(false); autoFieldsApplied.current = null; scheduleTouched.current = false; setHorario(''); setCantidadHoras(''); }} options={asignacionesDisponibles.map((a) => ({ value: a.id, label: a.materiaNombre ?? ('Asignación ' + a.id) }))} />
            </div>
            <div className="class-field">
              <label htmlFor="disciplinaClase">Disciplina</label>
              <input id="disciplinaClase" value={disciplina} readOnly placeholder="Se completa con la asignación" />
            </div>
            <fieldset className="class-field--full class-schedule"><legend>Horario de la clase</legend>
              <div className="class-schedule-grid">
                <div className="class-field"><label>Inicio de clase</label><AnimatedSelect ariaLabel="Inicio de clase" value={horario} placeholder="Seleccioná el horario" onChange={(value) => { scheduleTouched.current = true; setHorario(value); }} options={HORARIOS_CATEDRA.map((hora) => ({ value: hora, label: hora }))} /></div>
                <div className="class-field"><label htmlFor="cantidadHoras">Horas cátedra</label><input id="cantidadHoras" type="number" min={1} max={8} value={cantidadHoras} onChange={(e) => handleCantidadHorasInput(e.target.value)} placeholder="Ej.: 2" /></div>
                <div className="class-field"><label htmlFor="horarioFinalClase">Final de la clase</label><input id="horarioFinalClase" value={horarioFinal} placeholder="Según inicio y duración" readOnly /></div>
              </div>
              {!!horario && !!cantidadHoras && !horarioFinal && <small>La duración debe corresponder a horas disponibles dentro del mismo turno.</small>}
            </fieldset>
            <div className="class-field">
              <label>Modalidad</label>
              <AnimatedSelect ariaLabel="Modalidad de la clase" value={modalidad} onChange={setModalidad} options={[{ value: 'Presencial', label: 'Presencial' }, { value: 'Virtual', label: 'Virtual' }]} />
            </div>
            <div className="class-field">
              <label htmlFor="instrumentoId">Tipo de clase</label>
              <AnimatedSelect ariaLabel="Instrumento" value={instrumentoId} onChange={(value) => setInstrumentoId(Number(value))} options={[{ value: 0, label: 'Sin instrumento' }, ...data.instrumentos.map((item) => ({ value: item.id, label: item.nombre }))]} />
            </div>
            <div className="class-field class-field--full">
              <label htmlFor="observacionesGenerales">Observaciones generales</label>
              <textarea id="observacionesGenerales" rows={3} value={observaciones} onChange={(e) => setObservaciones(e.target.value)} placeholder="Cualquier eventualidad general de la clase..." style={{ resize: 'none' }} />
            </div>
          </div>
        </div>

        <RasgosAsistenciaEditor
          alumnos={data.rasgoAlumnosValidos}
          tema={tema}
          onTemaChange={(value) => { temaTouched.current = true; setAutoTemaAplicado(false); setTema(value); }}
          ausentes={ausentes}
          onAusenteChange={(alumnoId, ausente) => setAusentes((current) => ausente ? [...current, alumnoId] : current.filter((id) => id !== alumnoId))}
          codigosPorAlumno={codigosPorAlumno}
          onCodigoChange={toggleCodigo}
          codigosConducta={codigosConducta}
          codigosConductaError={codigosConductaError}
          onRetryCodigosConducta={() => setCodigosConductaAttempt((n) => n + 1)}
        />

        {requiereJustificacion && (
          <div className="class-card">
            <div className="notice" role="status" style={{ marginBottom: 12 }}>
              <p style={{ margin: 0 }}>El tema está atrasado según el plan curricular. Contá el motivo para poder registrar la clase.</p>
            </div>
            <div className="class-field class-field--full">
              <label htmlFor="justificacionAtraso">Justificación del atraso</label>
              <textarea
                ref={justificacionRef}
                id="justificacionAtraso"
                rows={3}
                value={justificacionAtraso}
                onChange={(event) => setJustificacionAtraso(event.target.value)}
                placeholder="Contá brevemente el motivo del atraso."
                style={{ resize: 'none' }}
              />
            </div>
          </div>
        )}

        {status && <p className="notice" role="status">{status}</p>}

        <div className="class-card" style={{ display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
          <button type="submit" className="button" disabled={!puedeIniciarClase} title={!puedeIniciarClase ? mensajeBloqueo : undefined}>{saving ? "Guardando…" : "Guardar inicio de clase"}</button>
          {assignmentError && <button type="button" className="button secondary" onClick={() => setAssignmentAttempt((n) => n + 1)}>Reintentar asignaciones</button>}
        </div>

      </form>

      {/* Right-side attendance & history panel removed as requested */}
    </div>
  );
}
