import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
    css: true,
    maxWorkers: 1,
    fileParallelism: false,
  },
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
    // El build final se sirve directo desde el jar de Spring Boot.
    // No se borra el contenido de resources/static porque ese directorio
    // también incluye assets estáticos del backend (logos PNG, plantillas, etc.).
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: false,
  },
});
