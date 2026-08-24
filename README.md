
# SCA — Sistema de Carpetas Académicas

Sistema de gestión académica del **Colegio Técnico Nacional (CTN)**: administra especialidades, cursos, materias, horarios, planillas de evaluación y notas, plan curricular con aprobación y verificación de tema por clase, con integración a **Google Classroom**, portal para padres/encargados, notificaciones push y soporte de **PWA** (instalable en el celular).

- Inicio del desarrollo: 18/06/2026
- Migración a Spring Boot + React: completada
- **Versión actual: 1.0.0** (24/08/2026) — primera versión estable de la nueva base. Ver [`CHANGELOG.md`](./CHANGELOG.md) para el historial completo (incluye el historial previo del sistema legado JSP, ya retirado).

## Roles del sistema

| Rol                         | Nivel | Qué puede hacer                                                                                                                                                                                                                                                                |
| --------------------------- | ----- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Profesor**          | 1     | Gestiona su perfil, sus materias y asignaciones, carga y aprueba su horario, sube su plan curricular, inicia clases (bloqueado sin plan aprobado) y gestiona sus planillas de evaluación y notas; conecta su cuenta de Google Classroom para importar tareas y calificaciones. |
| **Evaluador**         | 2     | Descarga planillas para revisión, aprueba o rechaza (con observaciones) los planes curriculares de los profesores, y revisa los casos dudosos de verificación de tema por clase.                                                                                              |
| **Administrador**     | 3     | Gestiona usuarios, materias, asignaciones, alumnos y horarios desde un panel único con 7 bloques; ve el estado técnico del sistema. Es el único rol inmutable (no editable/eliminable desde la UI).                                                                          |
| **Padre / Encargado** | 4     | Consulta el resumen académico y las notas de su hijo/a vinculado.                                                                                                                                                                                                              |

## Funcionalidades principales

- **Autenticación**: login con usuario/contraseña (BCrypt), **2FA con TOTP**, JWT con refresh token, login con Google OAuth2.
- **Perfil**: datos personales, foto, firma (solo profesor/evaluador), configuración de seguridad, estado de conexión con Google, notificaciones push, panel de actividad reciente (leído desde el registro de actividad en `.txt` por usuario).
- **Libro de Cátedra**:
  - **Horario**: carga por hora cátedra con detección de conflictos por profesor y por curso; exportación por curso.
  - **Plan curricular**: plantilla descargable pre-rellenada por asignación, carga y validación automática contra la asignación, aprobación/rechazo por evaluación con observaciones, vista de temas aprobados.
  - **Iniciar clase**: bloqueado hasta tener un plan curricular aprobado para la etapa vigente; verificación automática (no bloqueante) del tema ingresado contra el próximo tema pendiente del plan, con bandeja de casos dudosos para evaluación.
- **Materias y asignaciones**: alta/edición/eliminación de materias (comunes o específicas por especialidad), vínculo profesor–materia–curso.
- **Planillas y evaluación**: registro de tareas/instrumentos por curso, período y sección, carga de notas por alumno, distinción visual y de permisos entre tareas locales y tareas importadas de Classroom.
- **Exportación a Excel**: exportación de planillas individuales o masivas por especialidad/curso/sección/período (Apache POI), con encabezado dinámico adaptado al ancho real de la tabla.
- **Integración con Google Classroom**: login/vinculación OAuth2, detección de cursos por nivel+sección, sincronización de tareas y notas, vinculación de alumnos por correo/nombre.
- **Portal de padres**: resumen y notas del alumno vinculado, con visibilidad controlada por rol.
- **Panel de Administración** (7 bloques): Materias, Usuarios, Asignaciones, Alumnos (navegación especialidad → curso → sección), Horarios, Estado del sistema, Sistema de diseño.
- **Notificaciones push** (Web Push/VAPID): pruebas desde el perfil, y notificaciones sobre el flujo de plan curricular (aprobación/rechazo para el profesor, nuevo plan subido para evaluación).
- **PWA instalable**: manifest, service worker, iconos, funcionamiento offline básico.

## Roadmap / próximos pasos

- **Administrador por especialidad**: rol de administrador con alcance limitado a su propia especialidad (sus alumnos, materias y asignaciones), dejando el rol de administrador actual como administrador global. En diseño — ver `prompt-backend-v1.md` / `prompt-frontend-v1.md`.

## Stack tecnológico

| Componente            | Backend                                                                                  | Frontend                     |
| --------------------- | ---------------------------------------------------------------------------------------- | ---------------------------- |
| Lenguaje / plataforma | Java 17, Spring Boot 4                                                                   | TypeScript, React 19         |
| Build                 | Maven (`mvnw`)                                                                         | Vite                         |
| Base de datos         | MySQL (`mysql-connector-j`)                                                            | — (consumida vía API REST) |
| Seguridad             | Spring Security, JWT,`jbcrypt`, `bouncycastle` + TOTP (2FA)                          | —                           |
| Reportes              | Apache POI (`poi-ooxml`)                                                               | —                           |
| Notificaciones        | `web-push` (VAPID)                                                                     | Service Worker (`sw.js`)   |
| Integraciones Google  | `google-api-client`, `google-api-services-classroom`, `google-api-services-oauth2` | —                           |
| Testing               | JUnit 5 (Jupiter)                                                                        | —                           |

## Estructura del proyecto

```
ctn-sca-info/
├── backend/                        # Spring Boot (paquete ctn.informatica.sca)
│   └── src/main/java/ctn/informatica/sca/
│       ├── web/ · controller/       # Controladores REST
│       ├── service/                 # Lógica de negocio (parsers, plantillas, verificación de tema, logs)
│       ├── dao/                     # Acceso a datos (JDBC)
│       ├── dto/ · model/            # DTOs y modelos
│       ├── security/ · config/      # JWT, autenticación, configuración
│       └── util/                    # Utilidades (AcademicPeriod, PasswordUtil, TotpUtils, PushNotificationService, ...)
├── frontend/                        # React + Vite + TypeScript
│   └── src/
│       ├── pages/                   # Home (Libro de Cátedra, Planillas), Admin, Evaluación, Perfil, Parent
│       ├── api/                     # Cliente HTTP y llamadas por dominio
│       └── components/              # Componentes compartidos
├── legacy/                          # Servlets/JSP original — en retiro, mantenido solo como referencia
├── database/
│   ├── db-tables-properties.sql     # Esquema completo (DDL) de la BD ctndb — instalación limpia
│   ├── migrations/                  # Migraciones incrementales, solo para bases ya desplegadas en producción
│   └── minimal-work-seed.sql        # Seed mínimo de datos de ejemplo
├── CHANGELOG.md
└── LICENSE                          # GPL-3.0
```

## Configuración

### Base de datos

Para una instalación nueva, ejecutar `database/db-tables-properties.sql` (esquema completo y actualizado). Las migraciones en `database/migrations/` solo se aplican sobre una base **ya existente en producción** para llevarla al día; no son necesarias en una instalación limpia.

### Variables de entorno (backend)

| Variable                                                                  | Uso                                                                                                 |
| ------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| `JWT_SECRET`                                                            | Firma de tokens JWT                                                                                 |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` / `GOOGLE_REDIRECT_URI` | OAuth2 con Google Classroom                                                                         |
| `SCA_ACTIVITY_LOGS_DIR`                                                 | Directorio donde se guardan los`.txt` de actividad por usuario (default `./data/activity-logs`) |

### PWA y notificaciones push

La instalación de la PWA funciona sin configuración adicional. Para notificaciones Web Push el servidor necesita un par de claves VAPID configuradas en el backend.

## Despliegue

- Backend: servicio systemd `sca-backend.service` (jar en `/opt/ctn-sca-info/backend/sca-backend.jar`).
- Frontend: build estático (`npm run build`) servido junto al backend o vía proxy.
