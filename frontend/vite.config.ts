import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Dev server: proxea /api al backend Spring Boot local para poder
    // desarrollar sin tener que rebuildear el jar en cada cambio.
    // Cookies (SCA_REMEMBER) requieren same-origin real para producción;
    // en dev, changeOrigin + este proxy alcanza porque el browser sigue
    // viendo todo como localhost:5173.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    // El build final se sirve directo desde el jar de Spring Boot
    // (ver contexto de sesión: "Deploy propuesto"). deploy.sh no se toca;
    // sigue empaquetando lo que encuentre en resources/static tal cual.
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true,
  },
});
