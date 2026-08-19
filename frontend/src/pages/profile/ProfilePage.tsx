import { useCallback, useEffect, useLayoutEffect, useRef, useState, type FormEvent } from 'react';
import QRCode from 'qrcode';
import { changePassword, confirmTotp, disableTotp, getProfile, prepareTotp, saveProfile, type ProfileResponse, getGoogleAuthorizeUrl } from '../../api/profile';

const SIGNATURE_PERSISTENCE_MAX_BYTES = 1_500_000;
const SIGNATURE_MOBILE_MEDIA_QUERY = '(max-width: 680px), (max-height: 680px) and (pointer: coarse)';
import { ApiError } from '../../api/client';
import AppShell from '../../components/AppShell';
import PasswordInput from '../../components/PasswordInput';
import { useAuth } from '../../context/AuthContext';

type ProfileTab = 'profile' | 'security' | 'subjects' | 'app' | 'activity';
const message = (error: unknown, fallback: string) => error instanceof ApiError ? error.message : fallback;

export default function ProfilePage() {
  const { refreshUserIdentity } = useAuth();
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
  const finish = async (text: string) => { setStatus(text); await Promise.all([load(), refreshUserIdentity()]); };

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
  const [form, setForm] = useState({ correo: owner.correo || '', telefono: owner.telefono || '', celular: owner.celular || '', usuario: owner.usuario || '', nombre: owner.nombre || '', apellido: owner.apellido || '', ci: owner.ci, nivel: null, firmaImagen: owner.firmaImagen ?? null });
  const [signatureError, setSignatureError] = useState('');
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const isDrawingRef = useRef(false);
  const signatureBeforeModalRef = useRef<string | null>(null);
  const [isSignatureMobile, setIsSignatureMobile] = useState(() => window.matchMedia(SIGNATURE_MOBILE_MEDIA_QUERY).matches);
  const [isSignatureModalOpen, setIsSignatureModalOpen] = useState(false);
  const haveSignature = Boolean(form.firmaImagen);

  async function submit(event: FormEvent) { event.preventDefault(); try { await saveProfile(form); await done('Datos del perfil guardados.'); } catch (error) { setStatus(message(error, 'No se pudo guardar el perfil.')); } }

  function normalizeSignatureDataUrl(dataUrl: string): string | null {
    if (!dataUrl || !dataUrl.startsWith('data:image/')) return null;
    const matched = /^data:image\/(png|jpeg|jpg|webp);base64,/.exec(dataUrl);
    if (!matched) return null;
    const payload = dataUrl.substring(dataUrl.indexOf(',') + 1);
    const decodedBytes = typeof window !== 'undefined' ? atob(payload).length : 0;
    if (decodedBytes > SIGNATURE_PERSISTENCE_MAX_BYTES) {
      setSignatureError('La firma es demasiado grande. Probá con una imagen más pequeña.');
      return null;
    }
    setSignatureError('');
    return dataUrl;
  }

  function handleFileUpload(file: File | null) {
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      setSignatureError('Solo se permiten imágenes para la firma.');
      return;
    }
    // Compress and normalize image before storing to avoid large uploads.
    void (async () => {
      try {
        const compressed = await compressImageFile(file, 1000, 0.85);
        if (!compressed) return;
        const normalized = normalizeSignatureDataUrl(compressed);
        if (normalized) setForm({ ...form, firmaImagen: normalized });
      } catch (err) {
        setSignatureError('No se pudo procesar la imagen. Intentá con otra imagen.');
      }
    })();
  }

  async function compressImageFile(file: File, maxWidth = 1000, quality = 0.85): Promise<string | null> {
    return new Promise((resolve, reject) => {
      const url = URL.createObjectURL(file);
      const img = new Image();
      img.onload = () => {
        try {
          const ratio = img.width / img.height || 1;
          const targetWidth = Math.min(img.width, maxWidth);
          const targetHeight = Math.round(targetWidth / ratio);
          const canvas = document.createElement('canvas');
          canvas.width = targetWidth;
          canvas.height = targetHeight;
          const ctx = canvas.getContext('2d');
          if (!ctx) {
            URL.revokeObjectURL(url);
            return resolve(null);
          }
          ctx.fillStyle = '#ffffff';
          ctx.fillRect(0, 0, canvas.width, canvas.height);
          ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
          const dataUrl = canvas.toDataURL('image/jpeg', quality);
          URL.revokeObjectURL(url);
          // Quick size check (base64 payload length -> approx bytes)
          const payload = dataUrl.substring(dataUrl.indexOf(',') + 1);
          const approxBytes = Math.round((payload.length * 3) / 4);
          if (approxBytes > SIGNATURE_PERSISTENCE_MAX_BYTES) {
            setSignatureError('La firma es demasiado grande tras la compresión. Probá con una imagen más pequeña.');
            return resolve(null);
          }
          setSignatureError('');
          resolve(dataUrl);
        } catch (ex) {
          URL.revokeObjectURL(url);
          reject(ex);
        }
      };
      img.onerror = () => {
        URL.revokeObjectURL(url);
        reject(new Error('Error al cargar la imagen'));
      };
      img.src = url;
    });
  }

  const prepareCanvas = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const context = canvas.getContext('2d');
    if (!context) return;
    const rect = canvas.getBoundingClientRect();
    const pixelRatio = Math.min(window.devicePixelRatio || 1, 2);
    const width = Math.min(Math.max(Math.round(rect.width * pixelRatio), 280), 1200);
    const height = Math.min(Math.max(Math.round(rect.height * pixelRatio), 140), 520);
    canvas.width = width;
    canvas.height = height;
    context.fillStyle = '#ffffff';
    context.fillRect(0, 0, width, height);
    context.lineCap = 'round';
    context.lineJoin = 'round';
    context.lineWidth = 2.5;
    context.strokeStyle = '#111827';
    if (form.firmaImagen) {
      const img = new Image();
      img.onload = () => {
        context.clearRect(0, 0, width, height);
        context.drawImage(img, 0, 0, width, height);
      };
      img.src = form.firmaImagen;
    }
  }, [form.firmaImagen]);

  useEffect(() => {
    const media = window.matchMedia(SIGNATURE_MOBILE_MEDIA_QUERY);
    const updateMode = () => {
      setIsSignatureMobile(media.matches);
      if (!media.matches) setIsSignatureModalOpen(false);
    };
    media.addEventListener('change', updateMode);
    return () => media.removeEventListener('change', updateMode);
  }, []);

  useEffect(() => {
    if (!isSignatureMobile || isSignatureModalOpen) prepareCanvas();
  }, [isSignatureMobile, isSignatureModalOpen, prepareCanvas]);

  useEffect(() => {
    if (!isSignatureModalOpen) return;
    const scrollY = window.scrollY;
    const previous = {
      overflow: document.body.style.overflow,
      position: document.body.style.position,
      top: document.body.style.top,
      width: document.body.style.width,
    };
    document.body.style.overflow = 'hidden';
    document.body.style.position = 'fixed';
    document.body.style.top = `-${scrollY}px`;
    document.body.style.width = '100%';
    return () => {
      document.body.style.overflow = previous.overflow;
      document.body.style.position = previous.position;
      document.body.style.top = previous.top;
      document.body.style.width = previous.width;
      window.scrollTo(0, scrollY);
    };
  }, [isSignatureModalOpen]);

  function openSignatureModal() {
    signatureBeforeModalRef.current = form.firmaImagen;
    setIsSignatureModalOpen(true);
  }

  function closeSignatureModal(keepChanges: boolean) {
    isDrawingRef.current = false;
    if (!keepChanges) setForm((current) => ({ ...current, firmaImagen: signatureBeforeModalRef.current }));
    setIsSignatureModalOpen(false);
  }

  function drawStart(event: React.PointerEvent<HTMLCanvasElement>) {
    const canvas = canvasRef.current;
    const context = canvas?.getContext('2d');
    if (!canvas || !context) return;
    event.preventDefault();
    canvas.setPointerCapture(event.pointerId);
    isDrawingRef.current = true;
    const rect = canvas.getBoundingClientRect();
    const x = (event.clientX - rect.left) * (canvas.width / rect.width);
    const y = (event.clientY - rect.top) * (canvas.height / rect.height);
    context.beginPath();
    context.moveTo(x, y);
    context.lineTo(x, y);
    context.stroke();
  }

  function drawMove(event: React.PointerEvent<HTMLCanvasElement>) {
    if (!isDrawingRef.current) return;
    event.preventDefault();
    const canvas = canvasRef.current;
    const context = canvas?.getContext('2d');
    if (!canvas || !context) return;
    const rect = canvas.getBoundingClientRect();
    const x = (event.clientX - rect.left) * (canvas.width / rect.width);
    const y = (event.clientY - rect.top) * (canvas.height / rect.height);
    context.lineTo(x, y);
    context.stroke();
  }

  function finishDrawing(event?: React.PointerEvent<HTMLCanvasElement>) {
    if (!isDrawingRef.current) return;
    isDrawingRef.current = false;
    const canvas = canvasRef.current;
    if (!canvas) return;
    if (event && canvas.hasPointerCapture(event.pointerId)) canvas.releasePointerCapture(event.pointerId);
    const dataUrl = canvas.toDataURL('image/png');
    const normalized = normalizeSignatureDataUrl(dataUrl);
    if (normalized) setForm({ ...form, firmaImagen: normalized });
  }

  function clearSignature() {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const context = canvas.getContext('2d');
    if (!context) return;
    context.clearRect(0, 0, canvas.width, canvas.height);
    context.fillStyle = '#ffffff';
    context.fillRect(0, 0, canvas.width, canvas.height);
    setForm({ ...form, firmaImagen: null });
    setSignatureError('');
  }

  return <form className="profile-card-grid" onSubmit={submit}>
    <section className="panel form-grid"><Heading number="01" title="Información personal" detail="Datos que identifican tu cuenta." /><label>Nombre<input value={form.nombre} disabled={!data.canEditAdminOnlyProfileFields} onChange={(e) => setForm({ ...form, nombre: e.target.value })} /></label><label>Apellido<input value={form.apellido} disabled={!data.canEditAdminOnlyProfileFields} onChange={(e) => setForm({ ...form, apellido: e.target.value })} /></label><label>Cédula<input value={form.ci ?? ''} disabled={!data.canEditAdminOnlyProfileFields} inputMode="numeric" onChange={(e) => setForm({ ...form, ci: e.target.value ? Number(e.target.value) : null })} /></label></section>
    <section className="panel form-grid"><Heading number="02" title="Contacto" detail="Canales para comunicaciones del colegio." /><label>Correo electrónico<input type="email" value={form.correo} onChange={(e) => setForm({ ...form, correo: e.target.value })} /></label><label>Teléfono<input inputMode="numeric" value={form.telefono} onChange={(e) => setForm({ ...form, telefono: e.target.value })} /></label>{data.isStaffProfile && <label>Celular<input inputMode="numeric" value={form.celular} onChange={(e) => setForm({ ...form, celular: e.target.value })} /></label>}</section>
    <section className="panel form-grid"><Heading number="03" title="Cuenta" detail="Nombre utilizado para iniciar sesión." /><label>Usuario<input value={form.usuario} required onChange={(e) => setForm({ ...form, usuario: e.target.value })} /></label><div className="account-role"><span>Rol asignado</span><strong>{data.profileRoleLabel}</strong></div></section>
    {data.isProfessorProfile && <section className="panel form-grid"><Heading number="04" title="Firma del docente" detail="Se usa en la exportación y se limpia automáticamente si no hay dato." />
      {!isSignatureMobile && <div className="signature-box">
        <canvas ref={canvasRef} onPointerDown={drawStart} onPointerMove={drawMove} onPointerUp={finishDrawing} onPointerCancel={finishDrawing} />
      </div>}
      {isSignatureMobile && <div className="signature-mobile-entry">
        <div className="signature-preview" aria-label={haveSignature ? 'Vista previa de la firma guardada' : 'No hay una firma dibujada'}>
          {haveSignature ? <img src={form.firmaImagen ?? ''} alt="Firma del docente" /> : <span>Sin firma</span>}
        </div>
        <button className="button signature-open-button" type="button" onClick={openSignatureModal}>Firmar en pantalla completa</button>
      </div>}
      <div className="signature-actions">
        <label className="button secondary upload-button"><input type="file" accept="image/*" onChange={(e) => handleFileUpload(e.target.files?.[0] ?? null)} />Subir imagen</label>
        <button className="button secondary" type="button" onClick={clearSignature}>Borrar</button>
      </div>
      {signatureError && <p className="muted-copy error-copy">{signatureError}</p>}
      {haveSignature && <p className="muted-copy">Se usará la firma en la exportación; si no existe, se mostrará tu nombre.</p>}
      {isSignatureMobile && isSignatureModalOpen && <div className="signature-modal" role="dialog" aria-modal="true" aria-labelledby="signature-modal-title">
        <div className="signature-modal-header">
          <div><span>Firma del docente</span><h2 id="signature-modal-title">Firmá dentro del recuadro</h2></div>
          <button className="signature-modal-close" type="button" aria-label="Cancelar y cerrar" onClick={() => closeSignatureModal(false)}>×</button>
        </div>
        <div className="signature-modal-canvas">
          <canvas ref={canvasRef} onPointerDown={drawStart} onPointerMove={drawMove} onPointerUp={finishDrawing} onPointerCancel={finishDrawing} />
        </div>
        <p>Usá el dedo o un lápiz táctil. La página permanecerá fija mientras escribís.</p>
        <div className="signature-modal-actions">
          <button className="button secondary" type="button" onClick={() => closeSignatureModal(false)}>Cancelar</button>
          <button className="button secondary" type="button" onClick={clearSignature}>Borrar</button>
          <button className="button" type="button" onClick={() => closeSignatureModal(true)}>Usar firma</button>
        </div>
      </div>}
    </section>}
    {data.showGoogleClassroomPanel && <section className="panel form-grid"><Heading number={data.isProfessorProfile ? '05' : '04'} title="Google Classroom" detail="Vinculación académica del profesor." /><State active={data.googleClassroomConnected} title={data.googleClassroomConnected ? 'Cuenta conectada' : 'Sin conexión'} detail={data.profileOwner.googleEmail || 'Todavía no hay una cuenta de Google vinculada.'} />{data.googleClassroomCourses.length > 0 && <p className="muted-copy">{data.googleClassroomCourses.length} curso(s) compatible(s) disponibles.</p>}
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

function TOTPSetupCard({ provisioningUri, secret, code, onCodeChange, onConfirm }: { provisioningUri: string | null; secret: string; code: string; onCodeChange: (value: string) => void; onConfirm: () => void }) {
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null);

  useEffect(() => {
    if (!provisioningUri) {
      setQrDataUrl(null);
      return;
    }

    let disposed = false;
    QRCode.toDataURL(provisioningUri, {
      errorCorrectionLevel: 'M',
      margin: 1,
      width: 220,
      color: { dark: '#111827', light: '#ffffff' }
    }).then((url) => {
      if (!disposed) setQrDataUrl(url);
    }).catch(() => {
      if (!disposed) setQrDataUrl(null);
    });

    return () => {
      disposed = true;
    };
  }, [provisioningUri]);

  return <>
    {qrDataUrl ? <div className="totp-qr-box"><img src={qrDataUrl} alt="Código QR para autenticación de dos factores" /></div> : <code className="secret">{secret}</code>}
    <label>Código de la app<input inputMode="numeric" value={code} onChange={(e) => onCodeChange(e.target.value)} /></label>
    <button className="button" type="button" onClick={onConfirm}>Confirmar activación</button>
  </>;
}

function Security({ data, done }: { data: ProfileResponse; done: (text: string) => Promise<void> }) {
  const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [code, setCode] = useState('');
  async function password(e: FormEvent) { e.preventDefault(); try { await changePassword(passwords); setPasswords({ currentPassword: '', newPassword: '', confirmPassword: '' }); await done('Contraseña actualizada.'); } catch (error) { setStatusWithFallback(error, done, 'No se pudo actualizar la contraseña.'); } }
  async function start2fa() { try { await prepareTotp(); await done('Clave de configuración generada.'); } catch (error) { setStatusWithFallback(error, done, 'No se pudo preparar 2FA.'); } }
  async function verify2fa() { try { await confirmTotp(code); await done('Verificación en dos pasos activada.'); } catch (error) { setStatusWithFallback(error, done, 'Código inválido.'); } }
  async function turnOff() { try { await disableTotp(); await done('Verificación en dos pasos desactivada.'); } catch (error) { setStatusWithFallback(error, done, 'No se pudo desactivar 2FA.'); } }
  return <div className="two-column"><form className="panel form-grid" onSubmit={password}><Heading number="01" title="Cambiar contraseña" detail="Usá al menos seis caracteres." /><label>Contraseña actual<PasswordInput required value={passwords.currentPassword} onChange={(e) => setPasswords({ ...passwords, currentPassword: e.target.value })} /></label><label>Nueva contraseña<PasswordInput required minLength={6} value={passwords.newPassword} onChange={(e) => setPasswords({ ...passwords, newPassword: e.target.value })} /></label><label>Confirmar nueva contraseña<PasswordInput required value={passwords.confirmPassword} onChange={(e) => setPasswords({ ...passwords, confirmPassword: e.target.value })} /></label><button className="button">Actualizar contraseña</button></form><section className="panel form-grid security-2fa-panel"><Heading number="02" title="Verificación en dos pasos" detail="Protegé el acceso con tu app autenticadora." /><div className="security-2fa-status"><State active={data.totpEnabled} title={data.totpEnabled ? 'Activa' : 'Inactiva'} /></div>{data.pendingTotpSecret && <TOTPSetupCard provisioningUri={data.totpProvisioningUri} secret={data.pendingTotpSecret} code={code} onCodeChange={setCode} onConfirm={verify2fa} />}{data.totpEnabled ? <button className="button danger security-2fa-action" type="button" onClick={turnOff}>Desactivar 2FA</button> : !data.pendingTotpSecret && <button className="button secondary security-2fa-action" type="button" onClick={start2fa}>Configurar 2FA</button>}</section></div>;
}

function AppStatus({ data }: { data: ProfileResponse }) { return <div className="two-column"><section className="panel"><Heading number="01" title="Aplicación SCA" detail="Acceso rápido desde este dispositivo." /><p>Podés instalar SCA desde el menú de tu navegador para usarla como una aplicación.</p></section><section className="panel"><Heading number="02" title="Notificaciones" detail="Estado asociado a este usuario." /><State active={data.pushEnabled} title={data.pushEnabled ? 'Activadas' : 'Desactivadas'} /></section></div>; }
function Activity({ entries }: { entries: string[] }) { return <section className="panel"><Heading number="01" title="Actividad reciente" detail="Movimientos registrados para esta cuenta." />{entries.length === 0 ? <Empty title="Aún no hay movimientos" detail="La actividad de tu cuenta aparecerá aquí." /> : entries.map((entry, index) => <p className="history-row" key={`${entry}-${index}`}>{entry}</p>)}</section>; }
function State({ active, title, detail }: { active: boolean; title: string; detail?: string }) { return <div className={`connection-state ${active ? 'connected' : ''}`}><i /><div><strong>{title}</strong>{detail && <span>{detail}</span>}</div></div>; }
function Empty({ title, detail }: { title: string; detail: string }) { return <div className="empty-state"><h3>{title}</h3><p>{detail}</p></div>; }
function setStatusWithFallback(error: unknown, done: (text: string) => Promise<void>, fallback: string) { void done(message(error, fallback)); }
