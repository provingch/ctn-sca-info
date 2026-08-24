import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

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
    <main style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', padding: 24, background: '#f3f6fb' }}>
      <div style={{ maxWidth: 560, background: '#fff', borderRadius: 16, padding: 32, textAlign: 'center', boxShadow: '0 16px 40px rgba(20,35,59,0.12)' }}>
        <h1>Sin conexión temporal</h1>
        <p>La app está intentando volver a cargar, pero por ahora no hay acceso a Internet. Reintentá más tarde o volvé al inicio cuando la conexión esté disponible.</p>
        <div style={{ display: 'flex', gap: 8, justifyContent: 'center', marginTop: 12 }}>
          <button onClick={() => { void tryRestore(); }} style={{ padding: '10px 16px', borderRadius: 999, background: '#2b6cb0', color: '#fff', border: 'none', fontWeight: 600, cursor: 'pointer' }}>
            Reintentar ahora
          </button>
          <a href="/home" style={{ display: 'inline-block', padding: '10px 16px', borderRadius: 999, background: '#7a1f2b', color: '#fff', textDecoration: 'none', fontWeight: 600 }}>
            Ir al inicio
          </a>
        </div>
        <div style={{ marginTop: 12, color: '#666', fontSize: '0.95rem' }}>{status}</div>
      </div>
    </main>
  );
}
