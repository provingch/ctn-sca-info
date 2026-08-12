import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { completeGoogleCallback } from '../../api/profile';
import AppShell from '../../components/AppShell';

export default function GoogleCallbackPage() {
  const navigate = useNavigate();
  const [status, setStatus] = useState('Procesando conexión con Google...');

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const code = params.get('code') || undefined;
    const state = params.get('state') || undefined;
    const error = params.get('error') || undefined;

    (async () => {
      try {
        const res = await completeGoogleCallback({ code, state, error });
        setStatus(res?.message || 'Cuenta vinculada correctamente.');
        // después de un corto delay, volver al perfil
        setTimeout(() => navigate('/profile', { replace: true }), 1200);
      } catch (err: any) {
        setStatus(err?.message || 'Error al completar la conexión con Google.');
      }
    })();
  }, [navigate]);

  return <AppShell title="Conexión con Google"> 
    <section className="panel">{status}</section>
  </AppShell>;
}
