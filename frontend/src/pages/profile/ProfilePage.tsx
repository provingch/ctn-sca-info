import { useCallback, useEffect, useLayoutEffect, useRef, useState, type FormEvent } from 'react';
import { changePassword, confirmTotp, disableTotp, getProfile, prepareTotp, saveProfile, type ProfileResponse, getGoogleAuthorizeUrl } from '../../api/profile';
import { ApiError } from '../../api/client';
import AppShell from '../../components/AppShell';
import PasswordInput from '../../components/PasswordInput';

type ProfileTab = 'profile' | 'security' | 'subjects' | 'app' | 'activity';
const message = (error: unknown, fallback: string) => error instanceof ApiError ? error.message : fallback;

export default function ProfilePage() {
  const [data, setData] = useState<ProfileResponse | null>(null);
  const [status, setStatus] = useState('');
  const [tab, setTab] = useState<ProfileTab>('profile');
  const profilePageRef = useRef<HTMLDivElement>(null);
  const identityRef = useRef<HTMLElement>(null);
  const load = useCallback(async () => { try { setData(await getProfile()); } catch (error) { setStatus(message(error, 'Error al cargar el perfil.')); } }, []);
  useEffect(() => { void load(); }, [load]);
  useLayoutEffect(() => {
    const page = profilePageRef.current;
    const identity = identityRef.current;
    if (!page || !identity) return;

    const updateIdentityHeight = () => {
      page.style.setProperty('--profile-identity-height', `${Math.ceil(identity.getBoundingClientRect().height)}px`);
    };

    updateIdentityHeight();
    const observer = new ResizeObserver(updateIdentityHeight);
    observer.observe(identity);
    return () => observer.disconnect();
  }, [data]);

  if (!data) return <AppShell><section className="panel">{status || 'Cargando…'}</section></AppShell>;

  const owner = data.profileOwner;
  const initials = `${owner.nombre?.[0] || owner.usuario?.[0] || 'S'}${owner.apellido?.[0] || ''}`.toUpperCase();
  const completion = Math.round((Number(Boolean(owner.correo)) + Number(Boolean(owner.telefono)) + Number(Boolean(owner.usuario))) / 3 * 100);
  const finish = async (text: string) => { setStatus(text); await load(); };

  return <AppShell subtitle={`Cuenta de ${owner.usuario || 'usuario'} · ${data.profileRoleLabel}`}>
    <div className="profile-page" ref={profilePageRef}>
    <section className="profile-identity" ref={identityRef}>
      <div className="avatar" aria-hidden="true">{initials}</div>
      <div className="profile-identity-copy"><span className="badge">{data.profileRoleLabel}</span><h2>{owner.fullName?.trim() || owner.usuario || 'Usuario SCA'}</h2><strong>@{owner.usuario || 'sin-usuario'}</strong><p>{data.profileAccessDescription}</p></div>
      <div className="profile-completion"><span>Perfil completado</span><strong>{completion}%</strong><div><i style={{ width: `${completion}%` }} /></div></div>
    </section>
    <div className="profile-workspace">
      <aside className="profile-menu" aria-label="Secciones del perfil">
        <Tab active={tab === 'profile'} onClick={() => setTab('profile')} title="Perfil" detail="Datos personales" />
        {data.showSecurityPanel && <Tab active={tab === 'security'} onClick={() => setTab('security')} title="Seguridad" detail="Contraseña y 2FA" />}
        {data.showMateriasPanel && <Tab active={tab === 'subjects'} onClick={() => setTab('subjects')} title="Materias" detail="Asignaciones" />}
        <Tab active={tab === 'app'} onClick={() => setTab('app')} title="Aplicación" detail="Estado y avisos" />
        {data.showActivityPanel && <Tab active={tab === 'activity'} onClick={() => setTab('activity')} title="Registros" detail="Actividad" />}
      </aside>
      <div className="profile-content">
        {status && <div className="notice" role="status">{status}</div>}
        {tab === 'profile' && <ProfileForm data={data} done={finish} setStatus={setStatus} />}
        {tab === 'security' && <Security data={data} done={finish} />}
        {tab === 'subjects' && <Subjects data={data} />}
        {tab === 'app' && <AppStatus data={data} />}
        {tab === 'activity' && <Activity entries={data.activityLog} />}
      </div>
    </div>
    </div>
  </AppShell>;
}

function Tab({ active, onClick, title, detail }: { active: boolean; onClick: () => void; title: string; detail: string }) {
  return <button type="button" className={active ? 'active' : ''} onClick={onClick}><strong>{title}</strong><small>{detail}</small></button>;
}

function Heading({ number, title, detail }: { number: string; title: string; detail: string }) {
  return <header className="panel-heading"><span>{number}</span><div><h2>{title}</h2><p>{detail}</p></div></header>;
}

function ProfileForm({ data, done, setStatus }: { data: ProfileResponse; done: (text: string) => Promise<void>; setStatus: (value: string) => void }) {
  const owner = data.profileOwner;
  const [form, setForm] = useState({ correo: owner.correo || '', telefono: owner.telefono || '', celular: owner.celular || '', usuario: owner.usuario || '', nombre: owner.nombre || '', apellido: owner.apellido || '', ci: owner.ci, nivel: null });
  async function submit(event: FormEvent) { event.preventDefault(); try { await saveProfile(form); await done('Datos del perfil guardados.'); } catch (error) { setStatus(message(error, 'No se pudo guardar el perfil.')); } }
  return <form className="profile-card-grid" onSubmit={submit}>
    <section className="panel form-grid"><Heading number="01" title="Información personal" detail="Datos que identifican tu cuenta." /><label>Nombre<input value={form.nombre} disabled={!data.canEditAdminOnlyProfileFields} onChange={(e) => setForm({ ...form, nombre: e.target.value })} /></label><label>Apellido<input value={form.apellido} disabled={!data.canEditAdminOnlyProfileFields} onChange={(e) => setForm({ ...form, apellido: e.target.value })} /></label><label>Cédula<input value={form.ci ?? ''} disabled={!data.canEditAdminOnlyProfileFields} inputMode="numeric" onChange={(e) => setForm({ ...form, ci: e.target.value ? Number(e.target.value) : null })} /></label></section>
    <section className="panel form-grid"><Heading number="02" title="Contacto" detail="Canales para comunicaciones del colegio." /><label>Correo electrónico<input type="email" value={form.correo} onChange={(e) => setForm({ ...form, correo: e.target.value })} /></label><label>Teléfono<input inputMode="numeric" value={form.telefono} onChange={(e) => setForm({ ...form, telefono: e.target.value })} /></label>{data.isStaffProfile && <label>Celular<input inputMode="numeric" value={form.celular} onChange={(e) => setForm({ ...form, celular: e.target.value })} /></label>}</section>
    <section className="panel form-grid"><Heading number="03" title="Cuenta" detail="Nombre utilizado para iniciar sesión." /><label>Usuario<input value={form.usuario} required onChange={(e) => setForm({ ...form, usuario: e.target.value })} /></label><div className="account-role"><span>Rol asignado</span><strong>{data.profileRoleLabel}</strong></div></section>
    {data.showGoogleClassroomPanel && <section className="panel form-grid"><Heading number="04" title="Google Classroom" detail="Vinculación académica del profesor." /><State active={data.googleClassroomConnected} title={data.googleClassroomConnected ? 'Cuenta conectada' : 'Sin conexión'} detail={data.profileOwner.googleEmail || 'Todavía no hay una cuenta de Google vinculada.'} />{data.googleClassroomCourses.length > 0 && <p className="muted-copy">{data.googleClassroomCourses.length} curso(s) compatible(s) disponibles.</p>}
      <div>
        {!data.googleClassroomConnected && <button className="button" type="button" onClick={async () => {
          try {
            const res = await getGoogleAuthorizeUrl();
            if (res?.url) window.location.href = res.url;
          } catch (err: any) {
            setStatus(err instanceof ApiError ? err.message : 'No se pudo iniciar el flujo de Google.');
          }
        }}>Conectar con Google</button>}
        {data.googleClassroomConnected && <button className="button secondary" type="button" onClick={() => { /* Desconectar no implementado aquí */ }}>Desconectar</button>}
      </div>
    </section>}
    <div className="profile-form-actions"><button className="button" type="submit">Guardar cambios</button></div>
  </form>;
}

function Subjects({ data }: { data: ProfileResponse }) {
  return <div className="profile-section-stack"><section className="summary-grid"><article className="metric"><span>Especialidad</span><strong>{data.profesorEspecialidadNombre || 'Sin especialidad'}</strong></article><article className="metric"><span>Materias</span><strong>{data.teacherMaterias.length}</strong></article><article className="metric"><span>Asignaciones</span><strong>{data.misAsignaciones.length}</strong></article></section><section className="panel"><Heading number="01" title="Asignaciones de materias" detail="Materias y cursos vinculados a tu perfil." />{data.misAsignaciones.length === 0 ? <Empty title="Sin asignaciones" detail="Administración todavía no vinculó materias y cursos a este perfil." /> : <div className="profile-list"><div className="profile-list-header"><span>Materia</span><span>Curso</span></div>{data.misAsignaciones.map((item) => <div key={item.id}><strong>{item.materiaNombre}</strong><span>{item.cursoDescripcion}</span></div>)}</div>}</section>{data.teacherMaterias.length > 0 && <section className="panel"><h2>Materias asociadas</h2><div className="chip-list">{data.teacherMaterias.map((item) => <span key={item.id}>{item.nombre}<small>{item.categoria}</small></span>)}</div></section>}</div>;
}

function Security({ data, done }: { data: ProfileResponse; done: (text: string) => Promise<void> }) {
  const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [code, setCode] = useState('');
  async function password(e: FormEvent) { e.preventDefault(); try { await changePassword(passwords); setPasswords({ currentPassword: '', newPassword: '', confirmPassword: '' }); await done('Contraseña actualizada.'); } catch (error) { setStatusWithFallback(error, done, 'No se pudo actualizar la contraseña.'); } }
  async function start2fa() { try { await prepareTotp(); await done('Clave de configuración generada.'); } catch (error) { setStatusWithFallback(error, done, 'No se pudo preparar 2FA.'); } }
  async function verify2fa() { try { await confirmTotp(code); await done('Verificación en dos pasos activada.'); } catch (error) { setStatusWithFallback(error, done, 'Código inválido.'); } }
  async function turnOff() { try { await disableTotp(); await done('Verificación en dos pasos desactivada.'); } catch (error) { setStatusWithFallback(error, done, 'No se pudo desactivar 2FA.'); } }
  return <div className="two-column"><form className="panel form-grid" onSubmit={password}><Heading number="01" title="Cambiar contraseña" detail="Usá al menos seis caracteres." /><label>Contraseña actual<PasswordInput required value={passwords.currentPassword} onChange={(e) => setPasswords({ ...passwords, currentPassword: e.target.value })} /></label><label>Nueva contraseña<PasswordInput required minLength={6} value={passwords.newPassword} onChange={(e) => setPasswords({ ...passwords, newPassword: e.target.value })} /></label><label>Confirmar nueva contraseña<PasswordInput required value={passwords.confirmPassword} onChange={(e) => setPasswords({ ...passwords, confirmPassword: e.target.value })} /></label><button className="button">Actualizar contraseña</button></form><section className="panel form-grid"><Heading number="02" title="Verificación en dos pasos" detail="Protegé el acceso con tu app autenticadora." /><State active={data.totpEnabled} title={data.totpEnabled ? 'Activa' : 'Inactiva'} />{data.pendingTotpSecret && <><code className="secret">{data.pendingTotpSecret}</code><label>Código de la app<input inputMode="numeric" value={code} onChange={(e) => setCode(e.target.value)} /></label><button className="button" type="button" onClick={verify2fa}>Confirmar activación</button></>}{data.totpEnabled ? <button className="button danger" type="button" onClick={turnOff}>Desactivar 2FA</button> : !data.pendingTotpSecret && <button className="button secondary" type="button" onClick={start2fa}>Configurar 2FA</button>}</section></div>;
}

function AppStatus({ data }: { data: ProfileResponse }) { return <div className="two-column"><section className="panel"><Heading number="01" title="Aplicación SCA" detail="Acceso rápido desde este dispositivo." /><p>Podés instalar SCA desde el menú de tu navegador para usarla como una aplicación.</p></section><section className="panel"><Heading number="02" title="Notificaciones" detail="Estado asociado a este usuario." /><State active={data.pushEnabled} title={data.pushEnabled ? 'Activadas' : 'Desactivadas'} /></section></div>; }
function Activity({ entries }: { entries: string[] }) { return <section className="panel"><Heading number="01" title="Actividad reciente" detail="Movimientos registrados para esta cuenta." />{entries.length === 0 ? <Empty title="Aún no hay movimientos" detail="La actividad de tu cuenta aparecerá aquí." /> : entries.map((entry, index) => <p className="history-row" key={`${entry}-${index}`}>{entry}</p>)}</section>; }
function State({ active, title, detail }: { active: boolean; title: string; detail?: string }) { return <div className={`connection-state ${active ? 'connected' : ''}`}><i /><div><strong>{title}</strong>{detail && <span>{detail}</span>}</div></div>; }
function Empty({ title, detail }: { title: string; detail: string }) { return <div className="empty-state"><h3>{title}</h3><p>{detail}</p></div>; }
function setStatusWithFallback(error: unknown, done: (text: string) => Promise<void>, fallback: string) { void done(message(error, fallback)); }
