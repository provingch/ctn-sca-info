import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './index.css';
import AppRoutes from './routes/AppRoutes';
import { applyTheme, getInitialTheme } from './theme/theme';
import { registerPwaServiceWorker } from './pwa/pwa';

applyTheme(getInitialTheme());
void registerPwaServiceWorker();

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AppRoutes />
  </StrictMode>,
);
