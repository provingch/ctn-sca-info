import { useCallback, useEffect, useRef, useState, useSyncExternalStore, type FormEvent, type RefObject } from 'react';
import QRCode from 'qrcode';
import { changePassword, confirmTotp, disableTotp, disconnectGoogle, getProfile, prepareTotp, saveProfile, type ProfileResponse, getGoogleAuthorizeUrl } from '../../api/profile';

const SIGNATURE_PERSISTENCE_MAX_BYTES = 1_500_000;
const PHOTO_PERSISTENCE_MAX_BYTES = 1_500_000;
const SIGNATURE_MOBILE_MEDIA_QUERY = '(max-width: 680px), (max-height: 680px) and (pointer: coarse)';
import { ApiError } from '../../api/client';
import AppShell from '../../components/AppShell';
import CtnLogo from '../../components/CtnLogo';
import PasswordInput from '../../components/PasswordInput';
import SpecialtyIcon from '../../components/SpecialtyIcon';
import ConnectionState from '../../components/ui/ConnectionState';
import ContentState from '../../components/ui/ContentState';
import SectionHeading from '../../components/ui/SectionHeading';
import { useAuth } from '../../context/AuthContext';
import { getPushSubscriptionStatus, removePushSubscription, savePushSubscription, sendPushTest, toPushPayload, urlBase64ToUint8Array } from '../../api/push';
import { getPwaInstallSnapshot, promptPwaInstall, registerPwaServiceWorker, subscribePwaInstall } from '../../pwa/pwa';
import useAccessibleDialog from '../../hooks/useAccessibleDialog';
import { normalizeSpecialty } from '../../theme/theme';

type ProfileTab = 'profile' | 'security' | 'subjects' | 'app' | 'activity';
const message = (error: unknown, fallback: string) => error instanceof ApiError ? error.message : fallback;

export default function ProfilePage() {
  const { refreshUserIdentity } = useAuth();
  const [data, setData] = useState<ProfileResponse | null>(null);
  const [status, setStatus] = useState('');
  const [tab, setTab] = useState<ProfileTab>('profile');
  const profilePageRef = useRef<HTMLDivElement>(null);
  const profileContentRef = useRef<HTMLDivElement>(null);
  const load = useCallback(async () => {
    try {
      const response = await getProfile();
      if (!response?.profileOwner) {
        throw new Error('La respuesta del perfil no contiene los datos de la cuenta.');
      }
      setData({
        ...response,
        googleClassroomCourses: response.googleClassroomCourses ?? [],
        teacherMaterias: response.teacherMaterias ?? [],
        misAsignaciones: response.misAsignaciones ?? [],
        activityLog: response.activityLog ?? [],
      });
    } catch (error) {
      setStatus(message(error, 'Error al cargar el perfil.'));
    }
  }, []);
  useEffect(() => { void load(); }, [load]);
  // ResizeObserver and dynamic --profile-identity-height removed: obsolete with fixed flex layout

  if (!data) return <AppShell><ContentState tone={status ? 'error' : 'loading'} title={status || 'Cargando perfil…'} detail={status ? 'Recargá la página para volver a intentarlo.' : 'Estamos preparando los datos de tu cuenta.'} /></AppShell>;

  const owner = data.profileOwner;
  const ownerEspecialidad = data.especialidades.find((especialidad) => especialidad.id === owner.especialidadId)?.nombre ?? null;
  // `completion` removed — progress UI was eliminated from the profile header
  const finish = async (text: string) => { setStatus(text); await Promise.all([load(), refreshUserIdentity()]); };

  return <AppShell subtitle={`Cuenta de ${owner.usuario || 'usuario'} · ${data.profileRoleLabel}`}>
    <div className="profile-page" ref={profilePageRef}>
      <div className="profile-workspace">
        <aside className="profile-menu" aria-label="Secciones del perfil">
          <Tab active={tab === 'profile'} onClick={() => setTab('profile')} title="Perfil" detail="Datos personales" />
          {data.showSecurityPanel && <Tab active={tab === 'security'} onClick={() => setTab('security')} title="Seguridad" detail="Contraseña y 2FA" />}
          {data.showMateriasPanel && <Tab active={tab === 'subjects'} onClick={() => setTab('subjects')} title="Materias" detail="Asignaciones" />}
          <Tab active={tab === 'app'} onClick={() => setTab('app')} title="Aplicación" detail="Estado y avisos" />
          {data.showActivityPanel && <Tab active={tab === 'activity'} onClick={() => setTab('activity')} title="Registros" detail="Actividad" />}
        </aside>
        <div className="profile-content" ref={profileContentRef}>
          {status && <div className="notice" role="status">{status}</div>}
          {tab === 'profile' && <ProfileForm data={data} done={finish} setStatus={setStatus} />}
          {tab === 'security' && <Security data={data} done={finish} />}
          {tab === 'subjects' && <Subjects data={data} />}
          {tab === 'app' && <AppStatus data={data} />}
          {tab === 'activity' && <Activity entries={data.activityLog} />}
        </div>
        <aside className="profile-preview" aria-label="Vista previa del perfil" data-specialty={normalizeSpecialty(ownerEspecialidad)}>
          <span className="profile-preview-kicker">Vista previa</span>
          <div className="avatar" data-specialty={normalizeSpecialty(ownerEspecialidad)}>
            {owner.fotoPerfil
              ? <img src={owner.fotoPerfil} alt="Foto de perfil" />
              : ownerEspecialidad
                ? <SpecialtyIcon name={ownerEspecialidad} className="avatar-specialty-icon" />
                : <CtnLogo className="avatar-specialty-icon" />}
          </div>
          <div className="profile-identity-copy">
            <span className="badge">{data.profileRoleLabel}</span>
            <h2>{owner.fullName?.trim() || owner.usuario || 'Usuario SCA'}</h2>
            <strong>@{owner.usuario || 'sin-usuario'}</strong>
            <p>{ownerEspecialidad || 'Colegio Técnico Nacional'}</p>
          </div>
        </aside>
      </div>
      <ScrollToTopButton contentRef={profileContentRef} />
    </div>
  </AppShell>;
}

function Tab({ active, onClick, title, detail }: { active: boolean; onClick: () => void; title: string; detail: string }) {
  return <button type="button" className={active ? 'active' : ''} onClick={onClick}><strong>{title}</strong><small>{detail}</small></button>;
}

function ScrollToTopButton({ contentRef }: { contentRef: RefObject<HTMLDivElement | null> }) {
  const [visible, setVisible] = useState(false);
  const [desktop, setDesktop] = useState(() => window.matchMedia('(min-width: 901px)').matches);

  useEffect(() => {
    const mediaQuery = window.matchMedia('(min-width: 901px)');
    const onChange = () => setDesktop(mediaQuery.matches);
    mediaQuery.addEventListener('change', onChange);
    return () => mediaQuery.removeEventListener('change', onChange);
  }, []);

  useEffect(() => {
    const target = desktop ? contentRef.current : window;
    if (!target) return;
    const updateVisibility = () => setVisible(desktop ? (contentRef.current?.scrollTop ?? 0) > 200 : window.scrollY > 200);
    target.addEventListener('scroll', updateVisibility, { passive: true });
    updateVisibility();
    return () => target.removeEventListener('scroll', updateVisibility);
  }, [contentRef, desktop]);

  function scrollToTop() {
    if (desktop) contentRef.current?.scrollTo({ top: 0, behavior: 'smooth' });
    else window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  return <button type="button" className={`profile-scroll-top${visible ? ' visible' : ''}`} onClick={scrollToTop} aria-label="Volver arriba" aria-hidden={!visible} tabIndex={visible ? 0 : -1}>
    <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><path d="m6 15 6-6 6 6" /></svg>
  </button>;
}

function ProfileForm({ data, done, setStatus }: { data: ProfileResponse; done: (text: string) => Promise<void>; setStatus: (value: string) => void }) {
  const owner = data.profileOwner;
  const [form, setForm] = useState({ correo: owner.correo || '', telefono: owner.telefono || '', celular: owner.celular || '', usuario: owner.usuario || '', nombre: owner.nombre || '', apellido: owner.apellido || '', ci: owner.ci, nivel: null, firmaImagen: owner.firmaImagen ?? null, fotoPerfil: owner.fotoPerfil ?? null });
  const [signatureError, setSignatureError] = useState('');
  const [photoError, setPhotoError] = useState('');
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const isDrawingRef = useRef(false);
  const signatureBeforeModalRef = useRef<string | null>(null);
  const [isSignatureMobile, setIsSignatureMobile] = useState(() => window.matchMedia(SIGNATURE_MOBILE_MEDIA_QUERY).matches);
  const [isSignatureModalOpen, setIsSignatureModalOpen] = useState(false);
  const signatureDialogRef = useAccessibleDialog(isSignatureModalOpen, () => closeSignatureModal(false));
  const haveSignature = Boolean(form.firmaImagen);

  async function submit(event: FormEvent) {
    event.preventDefault();
    try {
      await saveProfile({ ...form, firmaImagen: form.firmaImagen ?? '' });
      await done('Datos del perfil guardados.');
    } catch (error) {
      setStatus(message(error, 'No se pudo guardar el perfil.'));
    }
  }

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
    const nameLower = (file.name || '').toLowerCase();
    if (file.type === 'image/heic' || file.type === 'image/heif' || nameLower.endsWith('.heic') || nameLower.endsWith('.heif')) {
      setSignatureError('Ese formato (HEIC/HEIF) no es compatible. Exportá la foto como JPG o PNG.');
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
        console.error('Error al procesar imagen de firma:', err);
        setSignatureError('No se pudo procesar la imagen. Intentá con otra imagen.');
      }
    })();
  }

  function normalizePhotoDataUrl(dataUrl: string): string | null {
    if (!dataUrl || !dataUrl.startsWith('data:image/')) return null;
    const matched = /^data:image\/(png|jpeg|jpg|webp);base64,/.exec(dataUrl);
    if (!matched) return null;
    const payload = dataUrl.substring(dataUrl.indexOf(',') + 1);
    const decodedBytes = typeof window !== 'undefined' ? atob(payload).length : 0;
    if (decodedBytes > PHOTO_PERSISTENCE_MAX_BYTES) {
      setPhotoError('La foto es demasiado grande. Probá con una imagen más pequeña.');
      return null;
    }
    setPhotoError('');
    return dataUrl;
  }

  function handlePhotoUpload(file: File | null) {
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      setPhotoError('Solo se permiten imágenes para la foto de perfil.');
      return;
    }
    const nameLower = (file.name || '').toLowerCase();
    if (file.type === 'image/heic' || file.type === 'image/heif' || nameLower.endsWith('.heic') || nameLower.endsWith('.heif')) {
      setPhotoError('Ese formato (HEIC/HEIF) no es compatible. Exportá la foto como JPG o PNG.');
      return;
    }
    void (async () => {
      try {
        const compressed = await compressAndCropImageFile(file, 600, 0.85);
        if (!compressed) return;
        const normalized = normalizePhotoDataUrl(compressed);
        if (normalized) setForm({ ...form, fotoPerfil: normalized });
      } catch (err) {
        console.error('Error al procesar foto de perfil:', err);
        setPhotoError('No se pudo procesar la foto. Intentá con otra imagen.');
      }
    })();
  }

  async function compressAndCropImageFile(file: File, maxSide = 600, quality = 0.85): Promise<string | null> {
    return new Promise((resolve, reject) => {
      const url = URL.createObjectURL(file);
      const img = new Image();
      img.onload = () => {
        try {
          const side = Math.min(img.width, img.height);
          const sx = Math.floor((img.width - side) / 2);
          const sy = Math.floor((img.height - side) / 2);
          const targetSide = Math.min(side, maxSide);
          const canvas = document.createElement('canvas');
          canvas.width = targetSide;
          canvas.height = targetSide;
          const ctx = canvas.getContext('2d');
          if (!ctx) { URL.revokeObjectURL(url); return resolve(null); }
          ctx.fillStyle = '#ffffff';
          ctx.fillRect(0, 0, canvas.width, canvas.height);
          ctx.drawImage(img, sx, sy, side, side, 0, 0, targetSide, targetSide);
          const dataUrl = canvas.toDataURL('image/jpeg', quality);
          URL.revokeObjectURL(url);
          const payload = dataUrl.substring(dataUrl.indexOf(',') + 1);
          const approxBytes = Math.round((payload.length * 3) / 4);
          if (approxBytes > PHOTO_PERSISTENCE_MAX_BYTES) {
            setPhotoError('La foto es demasiado grande tras la compresión. Probá con una imagen más pequeña.');
            return resolve(null);
          }
          setPhotoError('');
          resolve(dataUrl);
        } catch (ex) {
          URL.revokeObjectURL(url);
          reject(ex);
        }
      };
      img.onerror = () => { URL.revokeObjectURL(url); reject(new Error('Error al cargar la imagen')); };
      img.src = url;
    });
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
        // Draw image preserving aspect ratio and center it to avoid stretching
        const imgRatio = (img.width && img.height) ? (img.width / img.height) : 1;
        const canvasRatio = width / height;
        let dw = width;
        let dh = height;
        if (imgRatio > canvasRatio) {
          // image is wider than canvas: fit by width
          dw = width;
          dh = Math.round(width / imgRatio);
        } else {
          // image is taller than canvas: fit by height
          dh = height;
          dw = Math.round(height * imgRatio);
        }
        const dx = Math.round((width - dw) / 2);
        const dy = Math.round((height - dh) / 2);
        context.fillStyle = '#ffffff';
        context.fillRect(0, 0, width, height);
        context.drawImage(img, 0, 0, img.width, img.height, dx, dy, dw, dh);
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
    if (canvas) {
      const context = canvas.getContext('2d');
      if (context) {
        context.clearRect(0, 0, canvas.width, canvas.height);
        context.fillStyle = '#ffffff';
        context.fillRect(0, 0, canvas.width, canvas.height);
      }
    }
    setForm((current) => ({ ...current, firmaImagen: null }));
    setSignatureError('');
  }

  return <form className="profile-card-grid" onSubmit={submit}>
    <section className="panel form-grid"><SectionHeading number="01" title="Información personal" detail="Datos que identifican tu cuenta." /><label>Nombre<input value={form.nombre} disabled={!data.canEditAdminOnlyProfileFields} onChange={(e) => setForm({ ...form, nombre: e.target.value })} /></label><label>Apellido<input value={form.apellido} disabled={!data.canEditAdminOnlyProfileFields} onChange={(e) => setForm({ ...form, apellido: e.target.value })} /></label><label>Cédula<input value={form.ci ?? ''} disabled={!data.canEditAdminOnlyProfileFields} inputMode="numeric" onChange={(e) => setForm({ ...form, ci: e.target.value ? Number(e.target.value) : null })} /></label></section>
    <section className="panel form-grid"><SectionHeading number="02" title="Contacto" detail="Canales para comunicaciones del colegio." /><label>Correo electrónico<input type="email" value={form.correo} onChange={(e) => setForm({ ...form, correo: e.target.value })} /></label><label>Teléfono<input inputMode="numeric" value={form.telefono} onChange={(e) => setForm({ ...form, telefono: e.target.value })} /></label>{data.isStaffProfile && <label>Celular<input inputMode="numeric" value={form.celular} onChange={(e) => setForm({ ...form, celular: e.target.value })} /></label>}</section>
    <section className="panel form-grid"><SectionHeading number="03" title="Cuenta" detail="Nombre utilizado para iniciar sesión." /><label>Usuario<input value={form.usuario} required onChange={(e) => setForm({ ...form, usuario: e.target.value })} /></label><div className="account-role"><span>Rol asignado</span><strong>{data.profileRoleLabel}</strong></div></section>
    {data.isProfessorProfile && (
      <section className="panel form-grid">
        <SectionHeading number="04" title="Foto de perfil" detail="Se mostrará en la barra de navegación." />
        <div className="photo-section" style={{ gridColumn: '1 / -1' }}>
          <div className="photo-preview-row">
            <div className="photo-preview-circle" aria-hidden="true">
              {form.fotoPerfil ? <img src={form.fotoPerfil} alt="Foto de perfil" /> : <span className="initials">{`${owner.nombre?.[0] || owner.usuario?.[0] || 'S'}${owner.apellido?.[0] || ''}`.toUpperCase()}</span>}
            </div>
            <div className="photo-actions">
              <label className="button secondary upload-button"><input type="file" accept="image/*" onChange={(e) => handlePhotoUpload(e.target.files?.[0] ?? null)} />Subir foto</label>
              <button className="button secondary" type="button" onClick={() => setForm({ ...form, fotoPerfil: null })}>Quitar foto</button>
            </div>
          </div>
          {!form.fotoPerfil && <p className="muted-copy">No tienes foto de perfil.</p>}
          {photoError && <p className="muted-copy error-copy">{photoError}</p>}
        </div>
      </section>
    )}
    {data.showSignaturePanel && (
      <section className="panel form-grid">
        <SectionHeading number={data.isProfessorProfile ? '05' : '04'} title="Firma del docente" detail="Se usa en la exportación y se limpia automáticamente si no hay dato." />
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
        {isSignatureMobile && isSignatureModalOpen && <div ref={signatureDialogRef} className="signature-modal" role="dialog" aria-modal="true" aria-labelledby="signature-modal-title" tabIndex={-1}>
          <div className="signature-modal-header">
            <div><span>Firma del docente</span><h2 id="signature-modal-title">Firmá dentro del recuadro</h2></div>
            <button className="signature-modal-close" type="button" aria-label="Cancelar y cerrar" data-dialog-initial-focus onClick={() => closeSignatureModal(false)}>×</button>
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
      </section>
    )}
    {data.showGoogleClassroomPanel && <section className="panel form-grid"><SectionHeading number={data.isProfessorProfile ? '05' : '04'} title="Google Classroom" detail="Vinculación académica del profesor." /><ConnectionState active={data.googleClassroomConnected} title={data.googleClassroomConnected ? 'Cuenta conectada' : 'Sin conexión'} detail={data.profileOwner.googleEmail || 'Todavía no hay una cuenta de Google vinculada.'} />{data.googleClassroomCourses.length > 0 && <p className="muted-copy">{data.googleClassroomCourses.length} curso(s) compatible(s) disponibles.</p>}
      <div>
        {!data.googleClassroomConnected && <button className="button" type="button" onClick={async () => {
          try {
            const res = await getGoogleAuthorizeUrl();
            if (res?.url) window.location.href = res.url;
          } catch (err: any) {
            setStatus(err instanceof ApiError ? err.message : 'No se pudo iniciar el flujo de Google.');
          }
        }}>Conectar con Google</button>}
        {data.googleClassroomConnected && <button className="button danger" type="button" onClick={async () => {
          const confirmed = window.confirm('¿Desconectar Google Classroom? Se eliminarán los tokens y la asociación de esta cuenta. Las planillas locales no se borrarán.');
          if (!confirmed) return;
          try {
            await disconnectGoogle();
            await done('Google Classroom desconectado. Los tokens fueron eliminados.');
          } catch (err) {
            setStatus(message(err, 'No se pudo desconectar Google Classroom.'));
          }
        }}>Desconectar</button>}
      </div>
      {data.googleClassroomConnected && data.googleClassroomCourses.length > 0 && <div className="classroom-course-list" aria-label="Cursos disponibles en Google Classroom">
        <strong>Cursos disponibles</strong>
        {data.googleClassroomCourses.map((course) => <div className="classroom-course-row" key={course.id}><span>{course.name}{course.section ? ` · ${course.section}` : ''}</span><small>{course.room || 'Sin aula informada'}</small></div>)}
      </div>}
    </section>}
    <div className="profile-form-actions"><button className="button" type="submit">Guardar cambios</button></div>
  </form>;
}

function Subjects({ data }: { data: ProfileResponse }) {
  return <div className="profile-section-stack">
    <section className="summary-grid">
      <article className="metric"><span>Asignaciones</span><strong>{data.misAsignaciones.length}</strong></article>
    </section>
    <section className="panel">
      <SectionHeading number="01" title="Asignaciones de materias" detail="Materias y cursos vinculados a tu perfil." />
      {data.misAsignaciones.length === 0 ? <ContentState compact title="Sin asignaciones" detail="Administración todavía no vinculó materias y cursos a este perfil." /> : (
        <div className="profile-list">
          <div className="profile-list-header"><span>Materia</span><span>Especialidad</span><span>Curso</span></div>
          {data.misAsignaciones.map((item) => <div key={item.id} data-specialty={normalizeSpecialty(item.especialidad)}>
            <strong>{item.materiaNombre}</strong>
            <span className="profile-list-especialidad">
              <SpecialtyIcon name={item.especialidad || 'general'} className="profile-list-especialidad-icon" />
              {item.especialidad || 'General'}
            </span>
            <span>{(item.cursoNivel ?? '') + (item.cursoSeccion ? (' · ' + item.cursoSeccion) : '')}</span>
          </div>)}
        </div>
      )}
    </section>
  </div>;
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
  return <div className="two-column"><form className="panel form-grid" onSubmit={password}><SectionHeading number="01" title="Cambiar contraseña" detail="Usá al menos seis caracteres." /><label>Contraseña actual<PasswordInput required value={passwords.currentPassword} onChange={(e) => setPasswords({ ...passwords, currentPassword: e.target.value })} /></label><label>Nueva contraseña<PasswordInput required minLength={6} value={passwords.newPassword} onChange={(e) => setPasswords({ ...passwords, newPassword: e.target.value })} /></label><label>Confirmar nueva contraseña<PasswordInput required value={passwords.confirmPassword} onChange={(e) => setPasswords({ ...passwords, confirmPassword: e.target.value })} /></label><button className="button">Actualizar contraseña</button></form><section className="panel form-grid security-2fa-panel"><SectionHeading number="02" title="Verificación en dos pasos" detail="Protegé el acceso con tu app autenticadora." /><div className="security-2fa-status"><ConnectionState active={data.totpEnabled} title={data.totpEnabled ? 'Activa' : 'Inactiva'} /></div>{data.pendingTotpSecret && <TOTPSetupCard provisioningUri={data.totpProvisioningUri} secret={data.pendingTotpSecret} code={code} onCodeChange={setCode} onConfirm={verify2fa} />}{data.totpEnabled ? <button className="button danger security-2fa-action" type="button" onClick={turnOff}>Desactivar 2FA</button> : !data.pendingTotpSecret && <button className="button secondary security-2fa-action" type="button" onClick={start2fa}>Configurar 2FA</button>}</section></div>;
}

function AppStatus({ data }: { data: ProfileResponse }) {
  const install = useSyncExternalStore(subscribePwaInstall, getPwaInstallSnapshot, getPwaInstallSnapshot);
  const pushSupported = 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window;
  const [pushPermission, setPushPermission] = useState<NotificationPermission>(() => pushSupported ? Notification.permission : 'denied');
  const [serverSubscribed, setServerSubscribed] = useState(data.pushEnabled);
  const [deviceSubscribed, setDeviceSubscribed] = useState(false);
  const [vapidConfigured, setVapidConfigured] = useState(Boolean(data.pushPublicKey));
  const [busyAction, setBusyAction] = useState<'install' | 'enable' | 'disable' | 'test' | null>(null);
  const [feedback, setFeedback] = useState('');

  const syncPushState = useCallback(async () => {
    if (!pushSupported) return;
    try {
      const status = await getPushSubscriptionStatus();
      const registration = await registerPwaServiceWorker();
      const subscription = await registration?.pushManager.getSubscription();
      setVapidConfigured(Boolean(status.publicKey));
      setServerSubscribed(status.subscribed);
      setDeviceSubscribed(Boolean(subscription));
      setPushPermission(Notification.permission);
    } catch (error) {
      setFeedback(message(error, 'No se pudo consultar el estado de las notificaciones.'));
    }
  }, [pushSupported]);

  useEffect(() => { void syncPushState(); }, [syncPushState]);

  const installCopy = {
    installed: { title: 'Instalada', detail: 'SCA ya se está ejecutando como aplicación.' },
    ready: { title: 'Lista para instalar', detail: 'Podés agregar SCA a este dispositivo con un solo toque.' },
    'ios-manual': { title: 'Instalación manual', detail: 'En Safari, tocá Compartir y luego “Agregar a pantalla de inicio”.' },
    unavailable: { title: 'No disponible', detail: 'Este navegador no admite la instalación de aplicaciones web.' },
    waiting: { title: 'Disponible desde el navegador', detail: 'Si el botón aún no está activo, usá la opción “Instalar aplicación” del menú del navegador.' },
  }[install.status];

  const notificationsActive = pushSupported && pushPermission === 'granted' && serverSubscribed && deviceSubscribed;
  const notificationTitle = !pushSupported
    ? 'No compatibles'
    : pushPermission === 'denied'
      ? 'Bloqueadas en el navegador'
      : !vapidConfigured
        ? 'Configuración pendiente'
        : notificationsActive ? 'Activadas en este dispositivo' : 'Desactivadas';

  async function installApplication() {
    setBusyAction('install');
    setFeedback('');
    try {
      const result = await promptPwaInstall();
      setFeedback(result === 'accepted' ? 'Instalación iniciada.' : result === 'dismissed' ? 'La instalación fue cancelada.' : 'La instalación todavía no está disponible en este navegador.');
    } finally {
      setBusyAction(null);
    }
  }

  async function enableNotifications() {
    setBusyAction('enable');
    setFeedback('');
    try {
      if (!pushSupported) throw new Error('Tu navegador no soporta notificaciones push.');
      const status = await getPushSubscriptionStatus();
      if (!status.publicKey) throw new Error('El servidor todavía no tiene configuradas las claves VAPID.');
      const permission = await Notification.requestPermission();
      setPushPermission(permission);
      if (permission !== 'granted') throw new Error('Se necesita permiso del navegador para mostrar notificaciones.');
      const registration = await registerPwaServiceWorker();
      if (!registration) throw new Error('El service worker solo está disponible en la versión publicada de SCA.');
      const previous = await registration.pushManager.getSubscription();
      if (previous) await previous.unsubscribe();
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(status.publicKey),
      });
      await savePushSubscription(toPushPayload(subscription));
      setServerSubscribed(true);
      setDeviceSubscribed(true);
      setVapidConfigured(true);
      setFeedback('Notificaciones activadas en este dispositivo.');
    } catch (error) {
      setFeedback(error instanceof Error ? error.message : 'No se pudieron activar las notificaciones.');
      await syncPushState();
    } finally {
      setBusyAction(null);
    }
  }

  async function disableNotifications() {
    setBusyAction('disable');
    setFeedback('');
    try {
      const registration = pushSupported ? await navigator.serviceWorker.getRegistration('/') : null;
      const subscription = await registration?.pushManager.getSubscription();
      if (subscription) await subscription.unsubscribe();
      await removePushSubscription();
      setServerSubscribed(false);
      setDeviceSubscribed(false);
      setFeedback('Notificaciones desactivadas para esta cuenta.');
    } catch (error) {
      setFeedback(message(error, 'No se pudieron desactivar las notificaciones.'));
    } finally {
      setBusyAction(null);
    }
  }

  async function testNotifications() {
    setBusyAction('test');
    setFeedback('');
    try {
      await sendPushTest();
      setFeedback('Notificación de prueba enviada.');
    } catch (error) {
      setFeedback(message(error, 'No se pudo enviar la notificación de prueba.'));
    } finally {
      setBusyAction(null);
    }
  }

  return <div className="two-column pwa-settings-grid">
    <section className="panel pwa-setting-card">
      <SectionHeading number="01" title="Aplicación SCA" detail="Acceso rápido desde este dispositivo." />
      <ConnectionState active={install.status === 'installed' || install.status === 'ready'} title={installCopy.title} detail={installCopy.detail} />
      <div className="pwa-actions">
        <button className="button" type="button" disabled={!install.canInstall || busyAction !== null} onClick={installApplication}>{busyAction === 'install' ? 'Abriendo…' : install.status === 'installed' ? 'Aplicación instalada' : 'Instalar aplicación'}</button>
      </div>
      <p className="muted-copy">Al instalarla, SCA aparecerá junto a tus otras aplicaciones y podrá abrirse sin la barra del navegador.</p>
    </section>
    <section className="panel pwa-setting-card">
      <SectionHeading number="02" title="Notificaciones" detail="Avisos asociados a tu cuenta y este dispositivo." />
      <ConnectionState active={notificationsActive} title={notificationTitle} detail={!vapidConfigured ? 'El administrador debe configurar las claves VAPID del servidor.' : notificationsActive ? 'Este navegador puede recibir avisos incluso con SCA cerrada.' : 'Activá los avisos para recibir novedades importantes.'} />
      <div className="pwa-actions">
        {!notificationsActive && <button className="button" type="button" disabled={!pushSupported || pushPermission === 'denied' || !vapidConfigured || busyAction !== null} onClick={enableNotifications}>{busyAction === 'enable' ? 'Activando…' : 'Activar notificaciones'}</button>}
        {notificationsActive && <button className="button secondary" type="button" disabled={busyAction !== null} onClick={testNotifications}>{busyAction === 'test' ? 'Enviando…' : 'Enviar prueba'}</button>}
        {(notificationsActive || serverSubscribed) && <button className="button danger" type="button" disabled={busyAction !== null} onClick={disableNotifications}>{busyAction === 'disable' ? 'Desactivando…' : 'Desactivar'}</button>}
      </div>
      {pushPermission === 'denied' && <p className="muted-copy">Los avisos están bloqueados. Habilitalos desde la configuración del sitio en tu navegador.</p>}
      {feedback && <p className="pwa-feedback" role="status">{feedback}</p>}
    </section>
  </div>;
}
function Activity({ entries }: { entries: string[] }) { return <section className="panel"><SectionHeading number="01" title="Actividad reciente" detail="Movimientos registrados para esta cuenta." />{entries.length === 0 ? <ContentState compact title="Aún no hay movimientos" detail="La actividad de tu cuenta aparecerá aquí." /> : <div className="profile-history-list" aria-label="Registro de actividad">{entries.map((entry, index) => <p className="history-row" key={`${entry}-${index}`}>{entry}</p>)}</div>}</section>; }
function setStatusWithFallback(error: unknown, done: (text: string) => Promise<void>, fallback: string) { void done(message(error, fallback)); }
