import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Independent preview build: does not write to backend/static or import the app.
export default defineConfig({
  plugins: [react()],
  publicDir: false,
  build: { outDir: 'design-system-dist', emptyOutDir: true, rollupOptions: { input: 'design-system.html' } },
});
