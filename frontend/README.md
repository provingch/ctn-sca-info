# Frontend SCA

Aplicación React + TypeScript construida con Vite.

## Comandos

```bash
npm ci
npm run dev
npm test
npm run lint -- --deny-warnings
npm run build
```

`npm test` ejecuta Vitest y Testing Library en modo no interactivo. Para desarrollar una prueba mientras cambia el código, usar `npm run test:watch`.

## Componentes compartidos

- `src/components/ui/ContentState.tsx`: carga, error y ausencia de datos.
- `src/components/ui/ConnectionState.tsx`: conexiones y capacidades activas/inactivas.
- `src/components/ui/GradeChip.tsx`: notas con la escala visual institucional.
- `src/components/ui/SectionHeading.tsx`: encabezados numerados de panel.
- `src/components/AnimatedSelect.tsx`: combobox accesible y responsive.

La ruta protegida `/styleguide` funciona como catálogo vivo. Reutilizar los tokens de `src/index.css`; no agregar colores aislados cuando ya existe una variable semántica.
