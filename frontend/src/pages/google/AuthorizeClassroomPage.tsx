import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppShell from '../../components/AppShell';
import { getGoogleAuthorizeUrl } from '../../api/profile';

export default function AuthorizeClassroomPage() {
  const [url, setUrl] = useState<string | null>(null);
  const [status, setStatus] = useState('Cargando…');
  const navigate = useNavigate();

  useEffect(() => {
    let mounted = true;
    getGoogleAuthorizeUrl().then((res) => {
      if (!mounted) return;
      setUrl(res.url);
      setStatus('Listo para conectar Classroom.');
    }).catch(() => {
      if (!mounted) return;
      setStatus('No se pudo obtener la URL de autorización.');
    });
    return () => { mounted = false; };
  }, []);

  function openAuth() {
    if (!url) return;
    // Abrir en nueva pestaña para que el flujo OAuth complete fuera de la SPA
    window.open(url, '_blank', 'noopener');
    // Volver al inicio para que el usuario continúe cuando termine
    navigate('/home');
  }

  return (
    <AppShell title="Conectar Google Classroom">
      <div className="panel">
        <h2>Permisos requeridos</h2>
        <p>Esta acción requiere permiso para acceder a las tareas y calificaciones de Google Classroom. Sin estos permisos no es posible sincronizar las planillas.</p>
        <p><strong>Estado:</strong> {status}</p>
        <div style={{ marginTop: 16 }}>
          <button className="button primary" type="button" onClick={openAuth} disabled={!url}>Conectar Classroom</button>
          <button className="button secondary" type="button" onClick={() => navigate('/home')} style={{ marginLeft: 8 }}>Volver</button>
        </div>
      </div>
    </AppShell>
  );
}
