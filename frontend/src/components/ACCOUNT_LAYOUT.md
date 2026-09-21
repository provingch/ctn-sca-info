# Apariencia común de las cuentas

`AppShell` aplica `account-ui` y carga `account-layout.css` en las pantallas autenticadas de profesor, evaluación, administración global y por especialidad, coordinación y familias. Perfil y los apartados internos usan la misma base.

- La referencia es la interfaz actual del profesor: `PageBanner`, tarjetas `class-card`, controles `AnimatedSelect` y accesos `LauncherCards`.
- Las portadas pueden activar `welcome` para reutilizar el saludo y la fecha. El nombre y el contexto provienen de la cuenta; no se agregan métricas ficticias.
- `LauncherCards` acepta `href` para navegación con enlaces o `onSelect` para acciones. Conserva apertura en otra pestaña y navegación por teclado en los enlaces.
- `SectionNavigation` comparte el estilo del Libro de Cátedra en Administración, Evaluación y Coordinación. Recibe únicamente las secciones permitidas por el rol y marca el destino actual con `aria-current`.
- Los colores siguen siendo las variables de la cuenta y los ámbitos `data-specialty`. Esta capa no asigna un acento fijo.
- Los apartados comparten superficie, radio, títulos, espaciado, controles y foco. Los selectores CSS de formularios excluyen los controles dentro de tablas.
- Tablas, listas de registros, estados semánticos, permisos y lógica de cada módulo quedan en sus componentes. Las reglas comunes solo se aplican a pantalla para respetar la impresión.
- En móvil, los accesos se apilan y las barras y pestañas permiten varias filas.

Para nuevas pantallas, usar `AppShell` y estos componentes en lugar de copiar estilos locales. El adaptador antiguo `design-system/ApplicationDesign` no se activa: altera tablas y paletas que esta integración debe conservar.
