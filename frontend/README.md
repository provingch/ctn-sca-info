<p align="center">
  <img src="backend/src/main/resources/static/logo-institucional.png" alt="Colegio Técnico Nacional" height="92" />
      
  <img src="backend/src/main/resources/static/logo-sca-color.png" alt="Sistema de Carpetas Académicas" height="92" />
</p>

<h1 align="center">Sistema de Carpetas Académicas (SCA)</h1>

<p align="center">
  <img src="https://img.shields.io/badge/versi%C3%B3n-2.0.0-0057B7" alt="Versión 2.0.0" />
  <img src="https://img.shields.io/badge/licencia-GPL--3.0-0057B7" alt="Licencia GPL-3.0" />
  <img src="https://img.shields.io/badge/backend-Java%2017%20%C2%B7%20Spring%20Boot%204-C8102E" alt="Backend Java 17 + Spring Boot 4" />
  <img src="https://img.shields.io/badge/frontend-React%2019%20%C2%B7%20TypeScript-0057B7" alt="Frontend React 19 + TypeScript" />
  <img src="https://img.shields.io/badge/PWA-instalable-C8102E" alt="PWA instalable" />
</p>

Sistema de gestión académica del **Colegio Técnico Nacional (CTN)**: administra especialidades, cursos, materias, horarios, planillas de evaluación y notas, plan curricular con aprobación y verificación de tema por clase, control académico (incumplimientos, quejas y Coordinación Pedagógica), integración con **Google Classroom**, portal para padres/encargados, notificaciones push y soporte de **PWA** (instalable en el celular).

- Inicio del desarrollo: 18/06/2026
- Licencia: GPL-3.0 (ver [`LICENSE`](./LICENSE))
- Ver [`CHANGELOG.md`](./CHANGELOG.md) para el historial de versiones.

## Índice

- [Roles del sistema](#roles-del-sistema)
- [Funcionalidades principales](#funcionalidades-principales)
- [Identidad visual por especialidad](#identidad-visual-por-especialidad)
- [Capturas](#capturas)
- [Stack tecnológico](#stack-tecnológico)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Configuración](#configuración)
- [Despliegue](#despliegue)
- [Otros módulos del repositorio](#otros-módulos-del-repositorio)

## Roles del sistema

| Rol                                 | Nivel | Qué puede hacer                                                                                                                                                                                                                                                                                                                                                                |
| ----------------------------------- | ----- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Profesor**                  | 1     | Gestiona su perfil, sus materias y asignaciones, carga y aprueba su horario, sube su plan curricular, inicia clases (bloqueado sin plan aprobado para la etapa vigente) y gestiona sus planillas de evaluación y notas; si desarrolla un tema fuera de fecha debe justificar el atraso; conecta su cuenta de Google Classroom para importar tareas y calificaciones.           |
| **Evaluador**                 | 2     | Descarga planillas para revisión, aprueba o rechaza (con observaciones) los planes curriculares, hace seguimiento de los planes ya aprobados con la fecha de cumplimiento de cada tema, revisa los casos dudosos de verificación de tema, y resuelve los incumplimientos acumulados de un profesor (autorizar la continuidad o aplicar una suspensión con fechas concretas). |
| **Administrador**             | 3     | Gestiona el sistema desde un panel de 8 bloques. Puede ser**global** (acceso a todo) o **por especialidad** (alcance limitado a Horarios, Alumnos y Quejas de su propia especialidad). Es el único rol inmutable (no editable/eliminable desde la UI).                                                                                                             |
| **Padre / Encargado**         | 4     | Consulta el resumen académico y las notas de su hijo/a vinculado.                                                                                                                                                                                                                                                                                                              |
| **Coordinación Pedagógica** | 5     | Revisa las quejas cargadas contra cada profesor, agrupadas por profesor y con umbral de alerta, para evaluar su desempeño en el aula.                                                                                                                                                                                                                                          |

## Funcionalidades principales

- **Autenticación**: login con usuario/contraseña (BCrypt), **2FA con TOTP**, JWT con refresh token, login con Google OAuth2.
- **Perfil**: datos personales, foto, firma (exclusiva de profesor y evaluador), configuración de seguridad, estado de conexión con Google, notificaciones push, panel de actividad reciente (leído desde el registro de actividad en `.txt` por usuario).
- **Libro de Cátedra**:
  - **Horario**: carga por hora cátedra con detección de conflictos por profesor y por curso; catálogo de salas (comunes y por pabellón de especialidad) con validación de conflicto por sala; exportación a PDF por curso o por especialidad completa (una página por curso, con las salas al pie de cada bloque de horas).
  - **Plan curricular**: plantilla descargable pre-rellenada por asignación, con soporte para ambas etapas lectivas (Marzo–Junio y Julio–Noviembre); carga y validación automática contra la asignación real; aprobación o rechazo por evaluación con observaciones obligatorias; vista de planes aprobados con su estado de cobertura por tema.
  - **Iniciar clase**: bloqueado hasta tener un plan curricular aprobado para la etapa vigente; el tema ingresado se compara de forma directa contra el próximo tema pendiente del plan; si el tema desarrollado corresponde a un mes anterior al esperado, el profesor debe justificar el atraso.
- **Control académico**: acumular 3 o más atrasos justificados en una misma asignación genera una revisión de incumplimiento a cargo del evaluador, que puede autorizar la continuidad de las clases o aplicar una suspensión con fecha de inicio y fin; sistema de quejas cargado por la administración de cada especialidad identificando curso y profesor, con revisión agrupada y umbral de alerta para Coordinación Pedagógica; bandeja de notificaciones con indicador de no leídas y enlace directo al lugar donde se originó cada aviso, además de notificaciones push.
- **Materias y asignaciones**: alta/edición/eliminación de materias (comunes o específicas de una o más especialidades), vínculo profesor–materia–curso.
- **Planillas y evaluación**: registro de tareas/instrumentos por curso, período y sección, carga de notas por alumno, distinción visual y de permisos entre tareas locales y tareas importadas de Classroom.
- **Exportación**: planillas individuales o masivas por especialidad/curso/sección/período a Excel (Apache POI), con encabezado dinámico adaptado al ancho real de la tabla; horarios a PDF (Apache PDFBox) por curso o por especialidad completa.
- **Integración con Google Classroom**: login/vinculación OAuth2, detección de cursos por nivel+sección, sincronización de tareas y notas, vinculación de alumnos por correo/nombre.
- **Portal de padres**: resumen y notas del alumno vinculado, con visibilidad controlada por rol.
- **Panel de Administración** (8 bloques): Materias, Usuarios, Asignaciones, Horarios, Estado del sistema y Salas son de uso exclusivo del administrador global; Alumnos (navegación especialidad → curso → sección) y Quejas están disponibles también para el administrador por especialidad, con alcance limitado a la suya propia.
- **Notificaciones push** (Web Push/VAPID): prueba de envío desde el perfil, y notificaciones automáticas sobre el flujo de plan curricular, incumplimientos y quejas.
- **PWA instalable**: manifest, service worker, iconos, funcionamiento offline básico.

## Identidad visual por especialidad

Cada una de las 8 especialidades del CTN tiene su propio ícono y color, usados de forma consistente en horarios, tarjetas del panel de administración, avatares de usuario y encabezados de exportación (PDF/Excel):

<p align="center">
  <img src="backend/src/main/resources/static/assets/png/logo-especialidad-informatica.png" alt="Informática" width="110" />
  <img src="backend/src/main/resources/static/assets/png/logo-especialidad-electronica.png" alt="Electrónica" width="110" />
  <img src="backend/src/main/resources/static/assets/png/logo-especialidad-electricidad.png" alt="Electricidad" width="110" />
  <img src="backend/src/main/resources/static/assets/png/logo-especialidad-electromecanica.png" alt="Electromecánica" width="110" />
  <br />
  <img src="backend/src/main/resources/static/assets/png/logo-especialidad-mecanica-automotriz.png" alt="Mecánica Automotriz" width="110" />
  <img src="backend/src/main/resources/static/assets/png/logo-especialidad-mecanica-general.png" alt="Mecánica Industrial" width="110" />
  <img src="backend/src/main/resources/static/assets/png/logo-especialidad-construcciones.png" alt="Construcciones Civiles" width="110" />
  <img src="backend/src/main/resources/static/assets/png/logo-especialidad-quimica.png" alt="Química Industrial" width="110" />
</p>

Informática · Electrónica · Electricidad · Electromecánica · Mecánica Automotriz · Mecánica Industrial · Construcciones Civiles · Química Industrial

Los SVG fuente viven en `backend/src/main/resources/logos-source/` (logos institucional, SCA y por especialidad); `scripts/convert-logos.sh` los convierte a los PNG que usa el backend para las exportaciones.

## Capturas

<p align="center">
  <img src="videos/public/screenshots/safe/plantilla.png" alt="Planilla de proceso exportada a Excel" width="700" />
</p>

<p align="center"><sub>Planilla de proceso exportada a Excel, con encabezado dinámico, logo institucional y de especialidad (datos de alumnos ocultos).</sub></p>

## Stack tecnológico

| Componente            | Backend                                                                                  | Frontend                     |
| --------------------- | ---------------------------------------------------------------------------------------- | ---------------------------- |
| Lenguaje / plataforma | Java 17, Spring Boot 4                                                                   | TypeScript, React 19         |
| Build                 | Maven (`mvnw`)                                                                         | Vite                         |
| Base de datos         | MySQL (`mysql-connector-j`)                                                            | — (consumida vía API REST) |
| Seguridad             | Spring Security, JWT (`jjwt`), `jbcrypt`, `bouncycastle` + TOTP (2FA)              | —                           |
| Reportes              | Apache POI (`poi-ooxml`, planillas en Excel), Apache PDFBox (horarios en PDF)          | —                           |
| Notificaciones        | `web-push` (VAPID)                                                                     | Service Worker (`sw.js`)   |
| Integraciones Google  | `google-api-client`, `google-api-services-classroom`, `google-api-services-oauth2` | —                           |
| Testing               | JUnit 5 (Jupiter)                                                                        | —                           |

## Estructura del proyecto

```
ctn-sca-info/
├── backend/                        # Spring Boot (paquete ctn.informatica.sca)
│   └── src/main/java/ctn/informatica/sca/
│       ├── web/ · controller/       # Controladores REST
│       ├── service/                 # Lógica de negocio (parsers, plantillas, verificación de tema, logs, notificaciones)
│       ├── dao/                     # Acceso a datos (JDBC)
│       ├── dto/ · model/            # DTOs y modelos
│       ├── security/ · config/      # JWT, autenticación, configuración
│       └── util/                    # Utilidades (AcademicPeriod, PasswordUtil, TotpUtils, PushNotificationService, generación de Excel/PDF, ...)
├── frontend/                        # React + Vite + TypeScript
│   └── src/
│       ├── pages/                   # Home (Libro de Cátedra, Planillas), Admin, Evaluación, Coordinación, Perfil, Parent
│       ├── api/                     # Cliente HTTP y llamadas por dominio
│       └── components/              # Componentes compartidos
├── database/
│   ├── db-tables-properties.sql     # Esquema completo (DDL) de la BD ctndb — instalación limpia
│   ├── ctn-official-seed.sql        # Seed oficial con datos reales del colegio
│   └── minimal-work-seed.sql        # Seed mínimo de datos de ejemplo
├── CHANGELOG.md
└── LICENSE                          # GPL-3.0
```

## Configuración

### Base de datos

Para una instalación nueva, ejecutar `database/db-tables-properties.sql` (esquema completo y actualizado). Las migraciones en `database/migrations/` solo se aplican sobre una base **ya existente en producción** para llevarla al día; no son necesarias en una instalación limpia.

### Variables de entorno (backend)

| Variable                                                                  | Uso                                                              | Valor por defecto                                                       |
| ------------------------------------------------------------------------- | ---------------------------------------------------------------- | ----------------------------------------------------------------------- |
| `JWT_SECRET`                                                            | Firma de los tokens JWT                                          | *(obligatorio configurar en producción)*                             |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` / `GOOGLE_REDIRECT_URI` | OAuth2 con Google Classroom                                      | `REDIRECT_URI` → `http://localhost:8081/api/google/oauth/callback` |
| `CTN_VAPID_PUBLIC_KEY` / `CTN_VAPID_PRIVATE_KEY`                      | Par de claves VAPID para notificaciones Web Push                 | —*(sin ellas, el envío queda deshabilitado)*                        |
| `SCA_ACTIVITY_LOGS_DIR`                                                 | Directorio donde se guardan los`.txt` de actividad por usuario | `/var/lib/ctn/activity-logs`                                          |
| `SCA_LOAD_DEMO_DATA`                                                    | Si es`true`, carga datos de demostración al iniciar           | `false`                                                               |

### PWA y notificaciones push

La instalación de la PWA funciona sin configuración adicional. Para notificaciones Web Push el servidor necesita el par de claves VAPID (`CTN_VAPID_PUBLIC_KEY` / `CTN_VAPID_PRIVATE_KEY`) configuradas en el backend.

## Despliegue

- Backend: servicio systemd `sca-backend.service` (jar en `/opt/ctn-sca-info/backend/sca-backend.jar`).
- Frontend: build estático (`npm run build`) servido junto al backend o vía proxy.

En Linux, `./deploy.sh` abre la consola interactiva del CTN cuando se ejecuta desde una terminal. Desde el menú se puede actualizar el sistema, reconstruir la base con `db-tables-properties.sql` + `ctn-official-seed.sql`, editar el override de systemd y abrir el monitor de salud. La carga por defecto reemplaza la base seleccionada, exige escribir `RESET` y crea un respaldo previo cuando `mariadb-dump` o `mysqldump` está disponible.

Para automatización también se mantienen acciones no interactivas:

```bash
./deploy.sh --update
SCA_CONFIRM_DB_RESET=RESET ./deploy.sh --load-default-db
./deploy.sh --edit-service
./deploy.sh --health
```

Los valores `REPO_DIR`, `PROJECT_DIR`, `SERVICE_NAME`, `APP_URL`, `SCA_DB_NAME`, `SCA_DB_HOST`, `SCA_DB_PORT`, `SCA_DB_USER` y `BACKUP_DIR` permiten adaptar el mismo script a diferentes instalaciones MySQL/MariaDB.

## Otros módulos del repositori

<p align="center">
  <img src="backend/src/main/resources/static/logo-institucional.png" alt="Colegio Técnico Nacional" height="92" />
      
  <img src="backend/src/main/resources/static/logo-sca-color.png" alt="Sistema de Carpetas Académicas" height="92" />
</p>

<h1 align="center">Sistema de Carpetas Académicas (SCA)</h1>

<p align="center">
  <img src="https://img.shields.io/badge/versi%C3%B3n-2.0.0-0057B7" alt="Versión 2.0.0" />
  <img src="https://img.shields.io/badge/licencia-GPL--3.0-0057B7" alt="Licencia GPL-3.0" />
  <img src="https://img.shields.io/badge/backend-Java%2017%20%C2%B7%20Spring%20Boot%204-C8102E" alt="Backend Java 17 + Spring Boot 4" />
  <img src="https://img.shields.io/badge/frontend-React%2019%20%C2%B7%20TypeScript-0057B7" alt="Frontend React 19 + TypeScript" />
  <img src="https://img.shields.io/badge/PWA-instalable-C8102E" alt="PWA instalable" />
</p>

Sistema de gestión académica del **Colegio Técnico Nacional (CTN)**: administra especialidades, cursos, materias, horarios, planillas de evaluación y notas, plan curricular con aprobación y verificación de tema por clase, control académico (incumplimientos, quejas y Coordinación Pedagógica), integración con **Google Classroom**, portal para padres/encargados, notificaciones push y soporte de **PWA** (instalable en el celular).

- Inicio del desarrollo: 18/06/2026
- Licencia: GPL-3.0 (ver [`LICENSE`](./LICENSE))
- Ver [`CHANGELOG.md`](./CHANGELOG.md) para el historial de versiones.

## Índice

- [Roles del sistema](#roles-del-sistema)
- [Funcionalidades principales](#funcionalidades-principales)
- [Identidad visual por especialidad](#identidad-visual-por-especialidad)
- [Capturas](#capturas)
- [Stack tecnológico](#stack-tecnológico)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Configuración](#configuración)
- [Despliegue](#despliegue)
- [Otros módulos del repositorio](#otros-módulos-del-repositorio)

## Roles del sistema

| Rol                                 | Nivel | Qué puede hacer                                                                                                                                                                                                                                                                                                                                                                |
| ----------------------------------- | ----- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Profesor**                  | 1     | Gestiona su perfil, sus materias y asignaciones, carga y aprueba su horario, sube su plan curricular, inicia clases (bloqueado sin plan aprobado para la etapa vigente) y gestiona sus planillas de evaluación y notas; si desarrolla un tema fuera de fecha debe justificar el atraso; conecta su cuenta de Google Classroom para importar tareas y calificaciones.           |
| **Evaluador**                 | 2     | Descarga planillas para revisión, aprueba o rechaza (con observaciones) los planes curriculares, hace seguimiento de los planes ya aprobados con la fecha de cumplimiento de cada tema, revisa los casos dudosos de verificación de tema, y resuelve los incumplimientos acumulados de un profesor (autorizar la continuidad o aplicar una suspensión con fechas concretas). |
| **Administrador**             | 3     | Gestiona el sistema desde un panel de 8 bloques. Puede ser**global** (acceso a todo) o **por especialidad** (alcance limitado a Horarios, Alumnos y Quejas de su propia especialidad). Es el único rol inmutable (no editable/eliminable desde la UI).                                                                                                             |
| **Padre / Encargado**         | 4     | Consulta el resumen académico y las notas de su hijo/a vinculado.                                                                                                                                                                                                                                                                                                              |
| **Coordinación Pedagógica** | 5     | Revisa las quejas cargadas contra cada profesor, agrupadas por profesor y con umbral de alerta, para evaluar su desempeño en el aula.                                                                                                                                                                                                                                          |

## Funcionalidades principales

- **Autenticación**: login con usuario/contraseña (BCrypt), **2FA con TOTP**, JWT con refresh token, login con Google OAuth2.
- **Perfil**: datos personales, foto, firma (exclusiva de profesor y evaluador), configuración de seguridad, estado de conexión con Google, notificaciones push, panel de actividad reciente (leído desde el registro de actividad en `.txt` por usuario).
- **Libro de Cátedra**:
  - **Horario**: carga por hora cátedra con detección de conflictos por profesor y por curso; catálogo de salas (comunes y por pabellón de especialidad) con validación de conflicto por sala; exportación a PDF por curso o por especialidad completa (una página por curso, con las salas al pie de cada bloque de horas).
  - **Plan curricular**: plantilla descargable pre-rellenada por asignación, con soporte para ambas etapas lectivas (Marzo–Junio y Julio–Noviembre); carga y validación automática contra la asignación real; aprobación o rechazo por evaluación con observaciones obligatorias; vista de planes aprobados con su estado de cobertura por tema.
  - **Iniciar clase**: bloqueado hasta tener un plan curricular aprobado para la etapa vigente; el tema ingresado se compara de forma directa contra el próximo tema pendiente del plan; si el tema desarrollado corresponde a un mes anterior al esperado, el profesor debe justificar el atraso.
- **Control académico**: acumular 3 o más atrasos justificados en una misma asignación genera una revisión de incumplimiento a cargo del evaluador, que puede autorizar la continuidad de las clases o aplicar una suspensión con fecha de inicio y fin; sistema de quejas cargado por la administración de cada especialidad identificando curso y profesor, con revisión agrupada y umbral de alerta para Coordinación Pedagógica; bandeja de notificaciones con indicador de no leídas y enlace directo al lugar donde se originó cada aviso, además de notificaciones push.
- **Materias y asignaciones**: alta/edición/eliminación de materias (comunes o específicas de una o más especialidades), vínculo profesor–materia–curso.
- **Planillas y evaluación**: registro de tareas/instrumentos por curso, período y sección, carga de notas por alumno, distinción visual y de permisos entre tareas locales y tareas importadas de Classroom.
- **Exportación**: planillas individuales o masivas por especialidad/curso/sección/período a Excel (Apache POI), con encabezado dinámico adaptado al ancho real de la tabla; horarios a PDF (Apache PDFBox) por curso o por especialidad completa.
- **Integración con Google Classroom**: login/vinculación OAuth2, detección de cursos por nivel+sección, sincronización de tareas y notas, vinculación de alumnos por correo/nombre.
- **Portal de padres**: resumen y notas del alumno vinculado, con visibilidad controlada por rol.
- **Panel de Administración** (8 bloques): Materias, Usuarios, Asignaciones, Horarios, Estado del sistema y Salas son de uso exclusivo del administrador global; Alumnos (navegación especialidad → curso → sección) y Quejas están disponibles también para el administrador por especialidad, con alcance limitado a la suya propia.
- **Notificaciones push** (Web Push/VAPID): prueba de envío desde el perfil, y notificaciones automáticas sobre el flujo de plan curricular, incumplimientos y quejas.
- **PWA instalable**: manifest, service worker, iconos, funcionamiento offline básico.

## Identidad visual por especialidad

Cada una de las 8 especialidades del CTN tiene su propio ícono y color, usados de forma consistente en horarios, tarjetas del panel de administración, avatares de usuario y encabezados de exportación (PDF/Excel):

<p align="center">
  <img src="backend/src/main/resources/static/assets/png/logo-especialidad-informatica.png" alt="Informática" width="110" />
  <img src="backend/src/main/resources/static/assets/png/logo-especialidad-electronica.png" alt="Electrónica" width="110" />
  <img src="backend/src/main/resources/static/assets/png/logo-especialidad-electricidad.png" alt="Electricidad" width="110" />
  <img src="backend/src/main/resources/static/assets/png/logo-especialidad-electromecanica.png" alt="Electromecánica" width="110" />
  <br />
  <img src="backend/src/main/resources/static/assets/png/logo-especialidad-mecanica-automotriz.png" alt="Mecánica Automotriz" width="110" />
  <img src="backend/src/main/resources/static/assets/png/logo-especialidad-mecanica-general.png" alt="Mecánica Industrial" width="110" />
  <img src="backend/src/main/resources/static/assets/png/logo-especialidad-construcciones.png" alt="Construcciones Civiles" width="110" />
  <img src="backend/src/main/resources/static/assets/png/logo-especialidad-quimica.png" alt="Química Industrial" width="110" />
</p>

Informática · Electrónica · Electricidad · Electromecánica · Mecánica Automotriz · Mecánica Industrial · Construcciones Civiles · Química Industrial

Los SVG fuente viven en `backend/src/main/resources/logos-source/` (logos institucional, SCA y por especialidad); `scripts/convert-logos.sh` los convierte a los PNG que usa el backend para las exportaciones.

## Capturas

<p align="center">
  <img src="videos/public/screenshots/safe/plantilla.png" alt="Planilla de proceso exportada a Excel" width="700" />
</p>

<p align="center"><sub>Planilla de proceso exportada a Excel, con encabezado dinámico, logo institucional y de especialidad (datos de alumnos ocultos).</sub></p>

## Stack tecnológico

| Componente            | Backend                                                                                  | Frontend                     |
| --------------------- | ---------------------------------------------------------------------------------------- | ---------------------------- |
| Lenguaje / plataforma | Java 17, Spring Boot 4                                                                   | TypeScript, React 19         |
| Build                 | Maven (`mvnw`)                                                                         | Vite                         |
| Base de datos         | MySQL (`mysql-connector-j`)                                                            | — (consumida vía API REST) |
| Seguridad             | Spring Security, JWT (`jjwt`), `jbcrypt`, `bouncycastle` + TOTP (2FA)              | —                           |
| Reportes              | Apache POI (`poi-ooxml`, planillas en Excel), Apache PDFBox (horarios en PDF)          | —                           |
| Notificaciones        | `web-push` (VAPID)                                                                     | Service Worker (`sw.js`)   |
| Integraciones Google  | `google-api-client`, `google-api-services-classroom`, `google-api-services-oauth2` | —                           |
| Testing               | JUnit 5 (Jupiter)                                                                        | —                           |

## Estructura del proyecto

```
ctn-sca-info/
├── backend/                        # Spring Boot (paquete ctn.informatica.sca)
│   └── src/main/java/ctn/informatica/sca/
│       ├── web/ · controller/       # Controladores REST
│       ├── service/                 # Lógica de negocio (parsers, plantillas, verificación de tema, logs, notificaciones)
│       ├── dao/                     # Acceso a datos (JDBC)
│       ├── dto/ · model/            # DTOs y modelos
│       ├── security/ · config/      # JWT, autenticación, configuración
│       └── util/                    # Utilidades (AcademicPeriod, PasswordUtil, TotpUtils, PushNotificationService, generación de Excel/PDF, ...)
├── frontend/                        # React + Vite + TypeScript
│   └── src/
│       ├── pages/                   # Home (Libro de Cátedra, Planillas), Admin, Evaluación, Coordinación, Perfil, Parent
│       ├── api/                     # Cliente HTTP y llamadas por dominio
│       └── components/              # Componentes compartidos
├── database/
│   ├── db-tables-properties.sql     # Esquema completo (DDL) de la BD ctndb — instalación limpia
│   ├── ctn-official-seed.sql        # Seed oficial con datos reales del colegio
│   └── minimal-work-seed.sql        # Seed mínimo de datos de ejemplo
├── CHANGELOG.md
└── LICENSE                          # GPL-3.0
```

## Configuración

### Base de datos

Para una instalación nueva, ejecutar `database/db-tables-properties.sql` (esquema completo y actualizado). Las migraciones en `database/migrations/` solo se aplican sobre una base **ya existente en producción** para llevarla al día; no son necesarias en una instalación limpia.

### Variables de entorno (backend)

| Variable                                                                  | Uso                                                              | Valor por defecto                                                       |
| ------------------------------------------------------------------------- | ---------------------------------------------------------------- | ----------------------------------------------------------------------- |
| `JWT_SECRET`                                                            | Firma de los tokens JWT                                          | *(obligatorio configurar en producción)*                             |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` / `GOOGLE_REDIRECT_URI` | OAuth2 con Google Classroom                                      | `REDIRECT_URI` → `http://localhost:8081/api/google/oauth/callback` |
| `CTN_VAPID_PUBLIC_KEY` / `CTN_VAPID_PRIVATE_KEY`                      | Par de claves VAPID para notificaciones Web Push                 | —*(sin ellas, el envío queda deshabilitado)*                        |
| `SCA_ACTIVITY_LOGS_DIR`                                                 | Directorio donde se guardan los`.txt` de actividad por usuario | `/var/lib/ctn/activity-logs`                                          |
| `SCA_LOAD_DEMO_DATA`                                                    | Si es`true`, carga datos de demostración al iniciar           | `false`                                                               |

### PWA y notificaciones push

La instalación de la PWA funciona sin configuración adicional. Para notificaciones Web Push el servidor necesita el par de claves VAPID (`CTN_VAPID_PUBLIC_KEY` / `CTN_VAPID_PRIVATE_KEY`) configuradas en el backend.

## Despliegue

- Backend: servicio systemd `sca-backend.service` (jar en `/opt/ctn-sca-info/backend/sca-backend.jar`).
- Frontend: build estático (`npm run build`) servido junto al backend o vía proxy.

En Linux, `./deploy.sh` abre la consola interactiva del CTN cuando se ejecuta desde una terminal. Desde el menú se puede actualizar el sistema, reconstruir la base con `db-tables-properties.sql` + `ctn-official-seed.sql`, editar el override de systemd y abrir el monitor de salud. La carga por defecto reemplaza la base seleccionada, exige escribir `RESET` y crea un respaldo previo cuando `mariadb-dump` o `mysqldump` está disponible.

Para automatización también se mantienen acciones no interactivas:

```bash
./deploy.sh --update
SCA_CONFIRM_DB_RESET=RESET ./deploy.sh --load-default-db
./deploy.sh --edit-service
./deploy.sh --health
```

Los valores `REPO_DIR`, `PROJECT_DIR`, `SERVICE_NAME`, `APP_URL`, `SCA_DB_NAME`, `SCA_DB_HOST`, `SCA_DB_PORT`, `SCA_DB_USER` y `BACKUP_DIR` permiten adaptar el mismo script a diferentes instalaciones MySQL/MariaDB.

## Otros módulos del repositori

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

### Generación manual de PNG de logos

Los PNG que usa el PDF de horario se generan desde los SVG fuente de `backend/src/main/resources/logos-source/` y se escriben en `backend/src/main/resources/static/` y `backend/src/main/resources/static/assets/png/`.

Ese paso es manual y no forma parte del build automatizado de Vite porque `emptyOutDir` queda desactivado para evitar borrar esos assets del backend.

```bash
bash ../scripts/convert-logos.sh
```

Ejecutarlo solo cuando se modifiquen los SVG fuentes de logos.

## Componentes compartidos

- `src/components/ui/ContentState.tsx`: carga, error y ausencia de datos.
- `src/components/ui/ConnectionState.tsx`: conexiones y capacidades activas/inactivas.
- `src/components/ui/GradeChip.tsx`: notas con la escala visual institucional.
- `src/components/ui/SectionHeading.tsx`: encabezados numerados de panel.
- `src/components/AnimatedSelect.tsx`: combobox accesible y responsive.

La ruta protegida `/styleguide` funciona como catálogo vivo. Reutilizar los tokens de `src/index.css`; no agregar colores aislados cuando ya existe una variable semántica.
