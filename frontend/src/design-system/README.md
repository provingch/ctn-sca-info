# SCA · Sistema de diseño

Biblioteca reutilizable con galería independiente y adaptación a la aplicación. Las reglas de los componentes usan `.sca-ds` y variables `--ds-*`.

## Integración en la aplicación

`ApplicationDesign` se monta dentro de `SpecialtyProvider` y antes de los proveedores de autenticación y notificaciones. Publica los tokens en el documento para incluir menús portados a `body`, sigue los cambios de especialidad y respeta el selector claro/oscuro. Limpia sus variables al desmontarse.

`application.css` adapta los componentes existentes al diseño: navegación con permisos y notificaciones, encabezados, tarjetas, formularios, botones, tablas, perfil y pantallas de acceso. La estructura funcional de cada pantalla se conserva. El inicio utiliza `Card`, `ActivityItem`, `EmptyState` y `Skeleton`; el acceso usa `Button` y los estados compartidos usan `Skeleton`. Las especialidades locales en tarjetas conservan su identidad mediante `data-specialty`.

Los textos comerciales y datos ficticios de la galería no se importan a producción. La frase inicial de la muestra no forma parte de la aplicación. No se modificaron endpoints, permisos ni reglas académicas.

## Revisar la propuesta

Desde `frontend`, ejecutar `npm run dev` y abrir **http://localhost:5173/design-system.html** (usar el puerto que indique Vite). No requiere iniciar sesión. Los datos son ficticios y las acciones no se conectan al backend.

Compilación independiente, sin escribir en los recursos del backend:

```sh
npx tsc -b
npx vite build --config vite.design-system.config.ts
npx vite preview --config vite.design-system.config.ts
```

Abrir `/design-system.html` en el servidor de preview. La salida `design-system-dist/` es un artefacto local; no se debe versionar ni publicar con el build normal.

## Tokens y acento dinámico

`tokens.ts` es la única fuente de colores, tipografía, espaciado, radios y duraciones. `DesignSystem` transforma esos valores en variables CSS dentro de su contenedor. No establece variables en `:root`.

```tsx
import { DesignSystem, Card, Badge, Button, Icon } from './design-system';

<DesignSystem specialty="Informática">
  <Card title="Próxima clase" variant="featured" action={<Badge tone="brand">Informática</Badge>}>
    <Button aria-label="Iniciar clase" icon={<Icon name="plus" />} onClick={iniciarClase}>
      Iniciar clase
    </Button>
  </Card>
</DesignSystem>

// La futura integración puede inyectar el color del docente desde React:
<DesignSystem specialty={docente.especialidad} accent="#7a1f2b">{children}</DesignSystem>
```

`accent` es opcional y acepta únicamente `#RRGGBB`. Sin él, se resuelve la especialidad con la normalización existente; las desconocidas usan General. El color base conserva la identidad institucional (Informática: bordo). Se derivan automáticamente:

| Variable | Uso |
| --- | --- |
| `--ds-accent` | Fondo del botón principal, avatar y marca en header |
| `--ds-on-accent` | Negro o blanco, el que da mayor contraste sobre el acento |
| `--ds-accent-ink` | Variante clara para texto, borde destacado, foco de hover y tab activo |
| `--ds-accent-soft` | Fondo tonal de tab activo, badge de marca e íconos de actividad |

No sobrescribir únicamente `--ds-accent` a mano: usar la prop `accent` para recalcular también sus pares accesibles. El texto de marca cumple 4.5:1 sobre las superficies previstas; el botón escoge un primer plano de al menos 4.5:1. No usar el acento para representar éxito o error. Los badges semánticos conservan colores propios y siempre llevan texto.

## Fundamentos

- Fondo: `#10141d`. Superficies 1/2/3: `#181e29`, `#202836`, `#293344`.
- Texto primario/secundario/terciario: `#f3f5fa`, `#c0cada`, `#a5b2c6`. Todos se prueban a ≥4.5:1 contra fondo y las tres superficies; no se baja opacidad para representar texto secundario.
- Familia: Inter si está instalada, Aptos, Segoe UI, system-ui. No se descargan fuentes externas.
- Escala: display 44, h1 32, h2 24, h3 20, cuerpo 16, secundario 14, caption 12 px equivalentes, expresados en rem. Display solo para presentación; h1 móvil baja a 24.
- Pesos: regular 400, medio 500, semibold 600, bold 700. Interlineado: títulos 1.2–1.3; cuerpo 1.6.
- Espaciado: 0/4/8/12/16/20/24/32/40/48/64 px equivalentes. Padding habitual de tarjetas 24, móvil 16. Radios: 8/12/16 y pill.
- Límites decorativos sutiles; bordes de controles y foco tienen contraste ≥3:1. Altura mínima de botones y enlaces de navegación: 44 px.

## API pública

Todos los componentes y tipos se exportan desde `index.ts`. Cada archivo declara sus props. Ejemplos ejecutables de **todos** los componentes: `demo/Gallery.tsx`.

| Componente | Props principales / contrato |
| --- | --- |
| `DesignSystem` | `specialty?`, `accent?`, `children`, `className?`; necesario alrededor de la biblioteca |
| `Card` | `title?`, `description?`, `variant=standard/featured`, `level=1/2/3`, `headingLevel=2/3/4`, `action?`, `children` |
| `Badge` | `tone=neutral/success/warning/error/info/brand`, `children`; texto obligatorio por convención |
| `Button` | `variant=primary/secondary/icon`, `aria-label` obligatorio en todas las variantes, `loading`, `loadingLabel`, `disabled`, eventos y atributos nativos. Variante icon exige `icon` y no admite children |
| `ActivityItem` | `type=class/grades/review/profile`, `text`, `detail?`, `dateTime` ISO, `timestamp` visible, `href?`. Renderizar en ul/ol con clase `ds-activity-list` |
| `TopNav` | `brand`, `subtitle?`, `logo?`, `items[{id,label,href}]`, `activeId`, `user{name,role,initials,href}`. Enlaces reales; la aplicación suministra la ruta activa |
| `EmptyState` | `title`, `description`, `icon?`, `action?`; no se anuncia automáticamente como alerta |
| `Skeleton` | `label`, `lines=3` (1–12); un único status accesible por bloque |
| `Icon` | `name=book/check/document/user/bell/plus/arrow/empty`; SVG decorativo, el control aporta su nombre |

## Estados y accesibilidad

- Mantener `aria-label` coherente con el texto visible. Para icon-only describir la acción, no la forma del icono.
- Los botones son nativos; por defecto `type=button`, evitando envíos involuntarios. `loading` bloquea activación y expone `aria-busy` con etiqueta de carga. El componente padre anuncia la finalización en una región de estado (ver galería).
- Navegación superior: enlaces con `aria-current=page`, no tabs ARIA que requieran paneles y navegación adicional. Activación por Enter; botones por Enter/Espacio.
- `:focus-visible` de 3 px con separación de 4 px. No se elimina el outline. Hover no sustituye al foco.
- Skeleton e iconos son decorativos; el estado de carga tiene texto para lectores de pantalla. Las animaciones se desactivan con `prefers-reduced-motion`.
- Mobile ≤640 px: navegación y cabeceras se apilan; timestamps quedan bajo el texto. La galería pasa a una columna a 800 px. Evitar anchos fijos al integrar.
- Los portales futuros deben renderizarse dentro del contenedor temático o recibir su propio `DesignSystem`; las variables son locales.
- La verificación automática de contraste cubre los pares soportados. Contenido, imágenes, combinaciones nuevas y navegación completa requerirán revisión al migrar cada pantalla.

## Validación y límite de fase

`npx vitest run src/design-system/design-system.test.tsx` verifica contrastes WCAG, acento dinámico, alias, aislamiento, estados y semántica. Revisar también la galería con teclado, zoom y viewport móvil.

La aplicación ya utiliza la base aprobada; la galería sigue disponible para revisar futuras variantes sin modificar datos reales. Ejecutar además `ApplicationDesign.test.tsx` para comprobar identidad, aislamiento y alternancia de tema.
