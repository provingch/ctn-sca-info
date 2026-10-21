import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './index.css';
import './components/ui-utilities.css';
import './components/redesign.css';
import AppRoutes from './routes/AppRoutes';
import { applyTheme, getInitialTheme } from './theme/theme';
import { registerPwaServiceWorker } from './pwa/pwa';

applyTheme(getInitialTheme());
void registerPwaServiceWorker();

// Tras un deploy los chunks viejos se borran: una pestaña abierta que pida uno ya no lo encuentra.
// Recargar trae el index.html nuevo; una sola vez por pestaña para no entrar en bucle si falla de verdad.
window.addEventListener('vite:preloadError', () => {
  try {
    if (sessionStorage.getItem('sca-chunk-reload')) return;
    sessionStorage.setItem('sca-chunk-reload', '1');
  } catch {
    return;
  }
  window.location.reload();
});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AppRoutes />
  </StrictMode>,
);
