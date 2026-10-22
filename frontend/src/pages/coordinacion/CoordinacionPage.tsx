import SectionNavigation from '../../components/SectionNavigation';
import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import AppShell from '../../components/AppShell';
import ContentState from '../../components/ui/ContentState';
import { ApiError } from '../../api/client';
import { acceptQueja, getAdminQuejas, quejaEstado, rejectQueja, type QuejaAceptacion, type QuejaItem, type QuejaRechazo, type QuejaResolucion, type QuejaRevision } from '../../api/quejas';
import { getAdminCatalog } from '../../api/admin';
import { useSearchParams } from 'react-router-dom';
import { formatSqlDateTime, shortWhen } from '../../utils/date';
import CatalogoConductaPanel from '../../components/CatalogoConductaPanel';
import ComplaintReview from '../../components/quejas/ComplaintReview';
import ComplaintDocuments from '../../components/quejas/ComplaintDocuments';
import ComplaintGroups from '../../components/quejas/ComplaintGroups';
import LauncherCards from '../../components/LauncherCards';
import { launcherIcons } from '../../components/launcherIcons';
import DashboardActivity from '../../components/DashboardActivity';
import { DashboardLayout, DashboardStats, DashboardTasks, type DashboardTask } from '../../components/Dashboard';
import { getAnotacionesRecientes, type Anotacion } from '../../api/anotaciones';
import AnotacionesRecientes from './AnotacionesRecientes';
import { nombreCorto } from '../../utils/nombre';

const ESTADO_LABEL: Record<ReturnType<typeof quejaEstado>, string> = {
  pendiente: 'Pendiente', aceptada: 'Aceptada', revisada: 'Revisada', resuelta: 'Resuelta', rechazada: 'Rechazada',
};

function viewFromParam(value: string | null): 'menu' | 'quejas' | 'conducta' | 'anotaciones' {
  return value === 'quejas' || value === 'conducta' || value === 'anotaciones' ? value : 'menu';
}

function QuejaPendienteActions({ queja, onAccepted, onRejected }: {
  queja: QuejaItem;
  onAccepted: (result: QuejaAceptacion) => void;
  onRejected: (result: QuejaRechazo) => void;
}) {
  const [rejecting, setRejecting] = useState(false);
  const [motivo, setMotivo] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const inFlight = useRef(false);

  async function accept() {
    if (inFlight.current) return;
    inFlight.current = true; setBusy(true); setError('');
    try {
      onAccepted(await acceptQueja(queja.id));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo aceptar la queja. Reintentá.');
    } finally {
      inFlight.current = false; setBusy(false);
    }
  }

  async function confirmReject(event: FormEvent) {
    event.preventDefault();
    if (inFlight.current || !motivo.trim()) return;
    inFlight.current = true; setBusy(true); setError('');
    try {
      onRejected(await rejectQueja(queja.id, motivo.trim()));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo rechazar la queja. El motivo se conservó para reintentar.');
    } finally {
      inFlight.current = false; setBusy(false);
    }
  }

  if (rejecting) return <form className="complaint-review-form form-grid" aria-label={`Rechazo de queja #${queja.id}`} onSubmit={confirmReject}>
    <label htmlFor={`motivo-rechazo-${queja.id}`}>Motivo del rechazo</label>
    <textarea id={`motivo-rechazo-${queja.id}`} autoFocus rows={3} required disabled={busy} value={motivo} onChange={(e) => setMotivo(e.target.value)} placeholder="Explicá por qué se rechaza esta queja." />
    {error && <p className="notice error" role="alert">{error}</p>}
    <div className="complaint-review-buttons">
      <button className="button secondary" type="button" disabled={busy} onClick={() => { setRejecting(false); setError(''); }}>Cancelar</button>
      <button className="button" type="submit" disabled={busy || !motivo.trim()}>{busy ? 'Rechazando…' : 'Confirmar rechazo'}</button>
    </div>
  </form>;

  return <div className="complaint-review-buttons">
    <button className="button secondary" type="button" disabled={busy} onClick={() => setRejecting(true)}>Rechazar</button>
    <button className="button" type="button" disabled={busy} onClick={() => void accept()}>{busy ? 'Aceptando…' : 'Aceptar'}</button>
    {error && <p className="notice error" role="alert">{error}</p>}
  </div>;
}

export default function CoordinacionPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [view, setView] = useState<'menu' | 'quejas' | 'conducta' | 'anotaciones' | 'detalle'>(() => viewFromParam(searchParams.get('view')));
  const [status, setStatus] = useState('');
  const [quejas, setQuejas] = useState<QuejaItem[]>([]);
  const [usuariosPorId, setUsuariosPorId] = useState<Map<number, string>>(new Map());
  const [especialidadesPorId, setEspecialidadesPorId] = useState<Map<number, string>>(new Map());
  const [selectedProfesorId, setSelectedProfesorId] = useState<number | null>(null);
  const [selectedCursoId, setSelectedCursoId] = useState<number | null>(null);
  const [direccion, setDireccion] = useState<'contra_profesor' | 'sobre_curso'>('contra_profesor');
  const UMBRAL = 5; // valor visual por defecto si backend no expone el umbral

  useEffect(() => {
    const nextView = searchParams.get('view');
    setView(viewFromParam(nextView));
  }, [searchParams]);

  function changeView(nextView: 'menu' | 'quejas' | 'conducta' | 'anotaciones') {
    setView(nextView);
    setSearchParams(nextView === 'menu' ? {} : { view: nextView });
  }

  const [anotaciones, setAnotaciones] = useState<Anotacion[] | null>(null);
  const [quejasCargadas, setQuejasCargadas] = useState(false);
  const [busqueda, setBusqueda] = useState('');
  const [soloPendientes, setSoloPendientes] = useState(false);

  useEffect(() => {
    if (view !== 'menu') return;
    let active = true;
    void getAnotacionesRecientes(200).then((list) => { if (active) setAnotaciones(list); }).catch(() => { if (active) setAnotaciones([]); });
    return () => { active = false; };
  }, [view]);

  useEffect(() => {
    if (view !== 'quejas' && view !== 'menu') return;
    getAdminQuejas().then((list) => { setQuejas(list); setQuejasCargadas(true); }).catch((error: unknown) => setStatus(error instanceof Error ? error.message : 'No se pudieron cargar las quejas.'));
    getAdminCatalog().then((catalog) => {
      const map = new Map<number, string>();
      catalog.usuarios.forEach((u) => map.set(u.id, `${u.nombre} ${u.apellido}`.trim()));
      setUsuariosPorId(map);
      setEspecialidadesPorId(new Map(catalog.especialidades.map(item => [item.id, item.nombre])));
    }).catch(() => { /* el nombre de quien cargó la queja queda con fallback */ });
  }, [view]);

  function nombreCreador(creadaPor: number): string {
    return usuariosPorId.get(creadaPor) ?? `Usuario #${creadaPor}`;
  }

  function updateQueja(id: number, changes: Partial<QuejaItem>) {
    setQuejas((current) => current.map((item) => item.id === id ? { ...item, ...changes } : item));
  }

  const quejasFiltradas = useMemo(() => {
    const texto = busqueda.trim().toLocaleLowerCase('es');
    return quejas.filter((q) => (direccion === 'contra_profesor' ? q.tipo !== 'CONTRA_CURSO' : q.tipo === 'CONTRA_CURSO'))
      .filter((q) => !soloPendientes || quejaEstado(q) === 'pendiente')
      .filter((q) => !texto || [q.profesorNombre, q.profesorApellido, q.cursoEspecialidad, q.cursoSeccion, q.motivo].filter(Boolean).join(' ').toLocaleLowerCase('es').includes(texto));
  }, [quejas, direccion, busqueda, soloPendientes]);

  const agrupadas = useMemo(() => {
    if (direccion === 'contra_profesor') {
      const map = new Map<number, { profesorNombre: string; profesorApellido?: string | null; nombreVisible: string; count: number; quejas: QuejaItem[] }>();
      quejasFiltradas.forEach((q) => {
        const pid = q.profesorId;
        const cur = map.get(pid) ?? { profesorNombre: q.profesorNombre ?? `Profesor #${pid}`, profesorApellido: q.profesorApellido ?? '', nombreVisible: nombreCorto(q.profesorNombre, q.profesorApellido) || `Profesor #${pid}`, count: 0, quejas: [] };
        cur.count += 1;
        cur.quejas.push(q);
        map.set(pid, cur);
      });
      return Array.from(map.entries()).map(([profesorId, data]) => ({ key: profesorId, profesorId, ...data })).sort((a, b) => b.count - a.count);
    }
    // Sobre curso: agrupar por curso_id (la cohorte, así 3° A de distintas promociones no se mezclan), mostrando quién lo cargó en cada item.
    const map = new Map<number, { nombreVisible: string; count: number; quejas: QuejaItem[] }>();
    quejasFiltradas.forEach((q) => {
      const cid = q.cursoId;
      const nombre = [q.cursoEspecialidad, q.cursoNivel ? q.cursoNivel + '°' : null, q.cursoSeccion, q.cursoPromocion ? 'Promoción ' + q.cursoPromocion : null].filter(Boolean).join(' · ') || `Curso #${cid}`;
      const cur = map.get(cid) ?? { nombreVisible: nombre, count: 0, quejas: [] };
      cur.count += 1;
      cur.quejas.push(q);
      map.set(cid, cur);
    });
    return Array.from(map.entries()).map(([cursoId, data]) => ({ key: cursoId, cursoId, ...data })).sort((a, b) => b.count - a.count);
  }, [quejasFiltradas, direccion]);

  function openDetalle(key: number, dir: typeof direccion = direccion) {
    setDireccion(dir);
    setBusqueda('');
    setSoloPendientes(false);
    if (dir === 'contra_profesor') setSelectedProfesorId(key);
    else setSelectedCursoId(key);
    setView('detalle');
  }

  const grupoSeleccionado = direccion === 'contra_profesor'
    ? agrupadas.find((g) => 'profesorId' in g && g.profesorId === selectedProfesorId)
    : agrupadas.find((g) => 'cursoId' in g && g.cursoId === selectedCursoId);
  const detalleQuejas = grupoSeleccionado?.quejas ?? [];

  const navigation = <SectionNavigation label="Apartados de coordinación" active={view === 'detalle' ? 'quejas' : view} sections={[
    { key: 'quejas', label: 'Quejas por profesor', onSelect: () => changeView('quejas') },
    { key: 'conducta', label: 'Reportes conductuales', onSelect: () => changeView('conducta') },
    { key: 'anotaciones', label: 'Anotaciones', onSelect: () => changeView('anotaciones') },
  ]} />;

  if (view === 'menu') {
    const pendientes = quejas.filter((q) => quejaEstado(q) === 'pendiente');
    const porProfesor = new Map<number, number>();
    quejas.filter((q) => q.tipo !== 'CONTRA_CURSO').forEach((q) => porProfesor.set(q.profesorId, (porProfesor.get(q.profesorId) ?? 0) + 1));
    const haceUnaSemana = Date.now() - 7 * 86_400_000;
    const anotacionesSemana = anotaciones?.filter((a) => a.fechaHora && new Date(a.fechaHora.replace(' ', 'T')).getTime() >= haceUnaSemana) ?? null;
    const tasks: DashboardTask[] | null = !quejasCargadas && !status ? null : [
      ...pendientes.map((q): DashboardTask => ({
        key: `queja-${q.id}`,
        kind: 'Queja',
        tone: 'warning',
        title: q.tipo === 'CONTRA_CURSO' ? 'Sobre el curso' : nombreCorto(q.profesorNombre, q.profesorApellido) || `Profesor #${q.profesorId}`,
        detail: `${[q.cursoEspecialidad, q.cursoNivel ? `${q.cursoNivel}°` : null, q.cursoSeccion].filter(Boolean).join(' ')} · ${q.motivo}`,
        when: shortWhen(q.creadaEn),
        action: { label: 'Revisar', onSelect: () => openDetalle(q.tipo === 'CONTRA_CURSO' ? q.cursoId : q.profesorId, q.tipo === 'CONTRA_CURSO' ? 'sobre_curso' : 'contra_profesor') },
      })),
      ...(anotacionesSemana ?? []).slice(0, 5).map((a): DashboardTask => ({
        key: `anotacion-${a.id}`,
        kind: 'Anotación',
        tone: 'accent',
        title: `${a.alumnoApellido}, ${a.alumnoNombre}`,
        detail: [a.curso, a.tipo, a.observacion].filter(Boolean).join(' · '),
        when: shortWhen(a.fechaHora),
        action: { label: 'Ver', onSelect: () => changeView('anotaciones') },
      })),
    ];
    const porEspecialidad = new Map<string, { aceptadas: number; pendientes: number; rechazadas: number; total: number }>();
    quejas.forEach((q) => {
      const nombre = q.cursoEspecialidad?.trim() || 'Sin especialidad';
      const fila = porEspecialidad.get(nombre) ?? { aceptadas: 0, pendientes: 0, rechazadas: 0, total: 0 };
      const estado = quejaEstado(q);
      if (estado === 'pendiente') fila.pendientes += 1;
      else if (estado === 'rechazada') fila.rechazadas += 1;
      else fila.aceptadas += 1;
      fila.total += 1;
      porEspecialidad.set(nombre, fila);
    });
    return <AppShell title="Coordinación Pedagógica" subtitle="Quejas y acompañamiento socioacadémico" welcome>
      <DashboardLayout
        stats={<DashboardStats stats={[
          { key: 'pendientes', label: 'Quejas pendientes', value: quejasCargadas ? pendientes.length : status ? undefined : null, tone: pendientes.length ? 'warning' : undefined },
          { key: 'reiteradas', label: `Profesores con más de ${UMBRAL} quejas`, value: quejasCargadas ? [...porProfesor.values()].filter((n) => n > UMBRAL).length : status ? undefined : null, tone: [...porProfesor.values()].some((n) => n > UMBRAL) ? 'danger' : undefined },
          { key: 'anotaciones', label: 'Anotaciones en los últimos 7 días', value: anotacionesSemana === null ? null : anotacionesSemana.length },
          { key: 'total', label: 'Quejas registradas', value: quejasCargadas ? quejas.length : status ? undefined : null },
        ]} />}
        tasks={<DashboardTasks title="Para revisar hoy" tasks={tasks} emptyText="No hay quejas pendientes ni anotaciones nuevas esta semana." link={{ label: 'Ver todas las quejas', onSelect: () => changeView('quejas') }} />}
        side={<>
          <LauncherCards options={[
            { key: 'quejas', icon: launcherIcons.quejas, title: 'Quejas por profesor', description: 'Revisar, aceptar o rechazar.', onSelect: () => changeView('quejas') },
            { key: 'conducta', icon: launcherIcons.conducta, title: 'Reportes conductuales', description: 'Catálogo de códigos N.', onSelect: () => changeView('conducta') },
            { key: 'anotaciones', icon: launcherIcons.alumnos, title: 'Anotaciones', description: 'Uniforme y conducta, con foto.', onSelect: () => changeView('anotaciones') },
          ]} />
          <DashboardActivity />
        </>}
      >
        {porEspecialidad.size > 0 && <section className="panel dash-summary" aria-label="Quejas por especialidad">
          <header className="dash-tasks-head"><h2>Quejas por especialidad</h2></header>
          <table className="dash-table">
            <thead><tr><th scope="col">Especialidad</th><th scope="col" className="num">Aceptadas</th><th scope="col" className="num">Pendientes</th><th scope="col" className="num">Rechazadas</th><th scope="col" className="num">Total</th></tr></thead>
            <tbody>{[...porEspecialidad.entries()].sort((a, b) => b[1].total - a[1].total).map(([nombre, fila]) => <tr key={nombre}>
              <td><strong>{nombre}</strong></td><td className="num">{fila.aceptadas}</td><td className="num">{fila.pendientes}</td><td className="num">{fila.rechazadas}</td><td className="num"><strong>{fila.total}</strong></td>
            </tr>)}</tbody>
          </table>
        </section>}
      </DashboardLayout>
    </AppShell>;
  }

  if (view === 'quejas') {
    return <AppShell title="Quejas" onBack={() => changeView('menu')} backLabel="Coordinación" navigation={navigation}>
      {status && <div className="notice error" role="alert">{status}</div>}
      <div className="complaint-state-filters" role="group" aria-label="Dirección de las quejas">
        <button type="button" className={`button ${direccion === 'contra_profesor' ? '' : 'secondary'}`} aria-pressed={direccion === 'contra_profesor'} onClick={() => setDireccion('contra_profesor')}>Contra profesores</button>
        <button type="button" className={`button ${direccion === 'sobre_curso' ? '' : 'secondary'}`} aria-pressed={direccion === 'sobre_curso'} onClick={() => setDireccion('sobre_curso')}>Sobre cursos</button>
        <label className="complaint-search">
          <span className="visually-hidden">Buscar quejas</span>
          <input type="search" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} placeholder={direccion === 'contra_profesor' ? 'Buscar profesor, curso o motivo' : 'Buscar curso o motivo'} />
        </label>
        <label className="complaint-only-pending"><input type="checkbox" checked={soloPendientes} onChange={(e) => setSoloPendientes(e.target.checked)} /> Solo pendientes</label>
      </div>
      {agrupadas.length === 0 ? (
        <ContentState title="No hay quejas" detail={busqueda || soloPendientes ? 'Ninguna queja coincide con la búsqueda o el filtro.' : 'No se registraron quejas en este alcance.'} tone="empty" />
      ) : (
        <section>
          <div className="card-grid">
            {agrupadas.map((g) => (
              <button
                key={g.key}
                type="button"
                className={`nav-card${g.count > UMBRAL ? ' flagged' : ''}`}
                onClick={() => openDetalle(g.key)}
              >
                <div className="activity-row activity-row--card">
                  <div className="avatar">{g.nombreVisible.slice(0, 1)}</div>
                  <div className="activity-row-body">
                    <h2>{g.nombreVisible}</h2>
                    <p className="activity-row-summary">Última: {g.quejas[0]?.motivo ?? '-'}</p>
                  </div>
                  <span className={`badge${g.count > UMBRAL ? ' badge--danger' : ''}`}>{g.count}</span>
                </div>
              </button>
            ))}
          </div>
        </section>
      )}
    </AppShell>;
  }

  if (view === 'anotaciones') {
    return <AppShell title="Anotaciones" onBack={() => changeView('menu')} backLabel="Coordinación" navigation={navigation}>
      <AnotacionesRecientes />
    </AppShell>;
  }

  if (view === 'conducta') {
    return <AppShell title="Reportes conductuales" onBack={() => changeView('menu')} backLabel="Coordinación" navigation={navigation}>
      <CatalogoConductaPanel />
    </AppShell>;
  }

  return <AppShell title="Detalle de Quejas" onBack={() => changeView('quejas')} backLabel="Quejas" navigation={navigation}>
    <section className="panel">
      <header className="planilla-table-heading">
        <div>
          <span>Detalle</span>
          <h2>Quejas de {grupoSeleccionado?.nombreVisible ?? (direccion === 'contra_profesor' ? 'este profesor' : 'este curso')}</h2>
        </div>
        <small className="muted-copy">{detalleQuejas.length} queja(s) registrada(s)</small>
      </header>
      {detalleQuejas.length === 0 ? (
        <ContentState title="Sin quejas" detail="No se encontraron quejas para este alcance." tone="empty" />
      ) : (
        <ComplaintGroups key={grupoSeleccionado?.key} quejas={detalleQuejas} specialtyName={q => especialidadesPorId.get(q.especialidadId) || q.cursoEspecialidad?.trim() || 'Especialidad no disponible'}>{items => <ul className="activity-list">
          {items.map((q) => {
            const estado = quejaEstado(q);
            return <li key={q.id} className="activity-row">
              <div className="avatar activity-row-avatar">{(q.cursoEspecialidad ?? 'C').slice(0, 1)}</div>
              <div className="activity-row-body">
                <div className="activity-row-head">
                  <strong>{q.motivo}</strong>
                  <span className={`complaint-status ${estado}`}>{ESTADO_LABEL[estado]}</span>
                  <span className="badge">{`${q.cursoEspecialidad ?? ''} ${q.cursoNivel ?? ''}° ${q.cursoSeccion ?? ''}${q.cursoPromocion ? ` · Promoción ${q.cursoPromocion}` : ''}`.trim()}</span>
                </div>
                <div className="activity-row-meta">
                  <small>Cargada por {nombreCreador(q.creadaPor)} — {formatSqlDateTime(q.creadaEn)}</small>
                </div>
                <div className="activity-row-actions">
                  {estado === 'pendiente' && (
                    <QuejaPendienteActions
                      queja={q}
                      onAccepted={(result) => updateQueja(q.id, { ...result, estado: 'aceptada' })}
                      onRejected={(result) => updateQueja(q.id, { ...result, estado: 'rechazada' })}
                    />
                  )}
                  {estado === 'rechazada' && (
                    <div className="complaint-resolution">
                      <strong>Motivo del rechazo</strong>
                      <p>{q.motivoRechazo || 'Sin motivo registrado.'}</p>
                      <small>Rechazada el {formatSqlDateTime(q.rechazadaEn)}{q.rechazadaPor != null && <> · Por {nombreCreador(q.rechazadaPor)}</>}</small>
                    </div>
                  )}
                  {estado === 'aceptada' && (
                    <ComplaintReview queja={q} onReviewed={(revision: QuejaRevision) => updateQueja(q.id, revision)} />
                  )}
                  {(estado === 'revisada' || estado === 'resuelta') && (
                    <>
                      <ComplaintReview queja={q} reviewerName={q.revisadaPor != null ? nombreCreador(q.revisadaPor) : undefined} onReviewed={(revision: QuejaRevision) => updateQueja(q.id, revision)} />
                      <ComplaintDocuments queja={q} onResolved={(resolution: QuejaResolucion) => updateQueja(q.id, resolution)} />
                    </>
                  )}
                </div>
              </div>
            </li>;
          })}
        </ul>}</ComplaintGroups>
      )}
    </section>
  </AppShell>;
}
