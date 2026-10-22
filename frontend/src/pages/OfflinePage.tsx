import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import '../components/plan-views.css';

export default function OfflinePage() {
  const navigate = useNavigate();
  const [status, setStatus] = useState('Sin conexión. Volvé cuando tengas red o pulsa Reintentar.');

  const tryRestore = useCallback(async () => {
    setStatus('Comprobando sesión...');
    try {
      const res = await fetch('/api/auth/refresh', { method: 'POST', credentials: 'include', headers: { 'Accept': 'application/json' } });
      if (res.ok) { navigate('/home'); return; }
      if (res.status === 401) { navigate('/login'); return; }
      setStatus('Conexión disponible pero no se pudo restaurar la sesión.');
    } catch {
      setStatus('Sin conexión. Reintentando cuando vuelva la red...');
    }
  }, [navigate]);

  useEffect(() => {
    const onOnline = () => { void tryRestore(); };
    window.addEventListener('online', onOnline);
    if (navigator.onLine) setTimeout(() => { void tryRestore(); }, 500);
    return () => window.removeEventListener('online', onOnline);
  }, [tryRestore]);

  return (
    <main className="offline-page">
      <div className="offline-card">
        <h1>Sin conexión temporal</h1>
        <p>La app está intentando volver a cargar, pero por ahora no hay acceso a Internet. Reintentá más tarde o volvé al inicio cuando la conexión esté disponible.</p>
        <div className="inline-actions">
          <button type="button" className="button" onClick={() => { void tryRestore(); }}>Reintentar ahora</button>
          <a className="button secondary" href="/home">Ir al inicio</a>
        </div>
        <div className="offline-status" role="status">{status}</div>
      </div>
    </main>
  );
}
