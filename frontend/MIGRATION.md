# Cobertura de migración JSP → React

Las rutas de esta tabla son resueltas por `AppRoutes.tsx` y reenviadas al shell de la SPA por `SpaForwardController`.

| JSP legacy | Destino React | Observación |
|---|---|---|
| `index.jsp` | `/login` | Login, recordarme y redirección por rol |
| `TotpChallenge.jsp` | `/login` | Segundo paso integrado en `LoginPage` |
| `Inicio.jsp` | `/home` | Selector entre clase y planillas |
| `Home.jsp` | `/home?view=planillas` | Cursos y acceso a planillas |
| `InicioClase.jsp` | `/home?view=clase` | Registro de clase, asistencia e historial |
| `RasgoForm.jsp` | `/home?view=clase` | Edición de asistencia integrada |
| `Planilla.jsp` | `/planilla/:planillaId` | Notas, tareas y Classroom |
| `Tarea.jsp` | `/planilla/:planillaId/tarea[/:tareaId]` | Alta, edición y eliminación |
| `Evaluacion.jsp` | `/evaluacion` | Selección para exportación |
| `Profile.jsp` | `/profile` | Datos, contraseña, 2FA y actividad |
| `Parent.jsp` | `/padre` | Hijos, materias, notas y tareas |
| `Admin.jsp` | `/admin` | Panel general |
| `AdminMaterias.jsp` | `/admin/materias` | Catálogo y alta |
| `AdminUsuarios.jsp` | `/admin/usuarios` | Listado y alta |
| `AdminAsignaciones.jsp` | `/admin/asignaciones` | Alta y eliminación |
| `AdminIngresantes.jsp` | `/admin/ingresantes` | Listado y alta |
| `PrivacyPolicy.jsp` | `/privacidad` | Página pública |
| `TermsOfService.jsp` | `/terminos` | Página pública |
| `styleguide.jsp` | `/styleguide` | Muestra del sistema visual |
| `manifest.jsp` | `/manifest.webmanifest` | Manifest estático generado por Vite |

Los fragmentos `.jspf` (`head`, `navbar`, `footer` y estilos de página) fueron reemplazados por `AppShell.tsx` y `index.css`.
