# SCA — Sistema de Carpetas Académicas

Sistema de informes académicos del **Colegio Técnico Nacional (CTN)**: gestiona especialidades, cursos, materias, planillas de evaluación y notas, con integración a **Google Classroom**, portal para padres/encargados y soporte de **PWA** (instalable en el celular).

- Inicio del desarrollo: 18/06/2026
- Propuesta aceptada: 29/06/2026
- Versión actual: **2.0.0** (27/07/2026) — ver [`CHANGELOG.md`](./CHANGELOG.md) para el historial completo.

## Roles del sistema

| Rol | Qué puede hacer |
|---|---|
| **Profesor** | Gestiona su perfil, sus materias, sus planillas de evaluación y notas; conecta su cuenta de Google Classroom para importar tareas y calificaciones. |
| **Administrador** | Gestiona usuarios, materias y asignaciones de profesor–materia–curso desde paneles dedicados (`Admin.jsp`, `AdminUsuarios.jsp`, `AdminMaterias.jsp`, `AdminAsignaciones.jsp`). |
| **Padre / Encargado** | Consulta el resumen académico y las notas de su hijo/a vinculado (`Parent.jsp`). |
| **Usuario de integración** (uno por especialidad, ej. `informatica-itg`, `electricidad-itg`) | Corrige manualmente los correos de Google de los alumnos de su especialidad antes de sincronizar con Classroom. |

## Funcionalidades principales

- **Autenticación**: login con usuario/contraseña (hash BCrypt, con compatibilidad hacia registros antiguos en texto plano), **2FA con TOTP**, sesión "recordarme" y filtro de autenticación (`AuthFilter`).
- **Perfil de profesor**: datos personales, configuración de seguridad, estado de conexión con Google, panel de actividad reciente.
- **Materias y asignaciones**: alta/edición/eliminación de materias por especialidad, vínculo profesor–materia–curso.
- **Planillas y evaluación**: registro de tareas/instrumentos de evaluación por curso, período (1º/2º) y sección, carga de notas por alumno (`registro`, `puntaje`).
- **Exportación a Excel**: exportación de planillas individuales o masivas por especialidad/curso/sección/período (Apache POI).
- **Integración con Google Classroom**:
  - Login OAuth2 y vinculación/desvinculación de cuenta de Google.
  - Detección de cursos de Classroom por nivel + sección (convención de nombres, ver `flowcharts/README.md`).
  - Sincronización manual de tareas y notas desde la planilla.
  - Vinculación de alumnos locales con estudiantes de Classroom por correo/nombre, con corrección manual vía el rol de integración.
- **Portal de padres**: resumen y notas del alumno vinculado.
- **Notificaciones push** (Web Push/VAPID) y **PWA instalable** (manifest, service worker, iconos).
- **Manuales de usuario en PDF** por rol: administrador, evaluador, padres y profesor (`src/main/webapp/pdfs/`).

## Stack tecnológico

| Componente | Detalle |
|---|---|
| Lenguaje / plataforma | Java 17, Jakarta EE 11 (Servlets + JSP) |
| Build | Maven (`pom.xml`), plugin `tomcat10-maven-plugin` (puerto 8080) |
| Base de datos | MySQL (`mysql-connector-j`) |
| Vistas | JSP + JSTL, CSS propio (`ctn-theme.css`) + Flat UI / Bootstrap como base |
| Seguridad | `jbcrypt` (hash de contraseñas), `bouncycastle` + TOTP (2FA) |
| Reportes | Apache POI (`poi-ooxml`) para exportar Excel |
| Notificaciones | `web-push` (VAPID) |
| Integraciones Google | `google-api-client`, `google-api-services-classroom`, `google-api-services-oauth2`, OAuth2 |
| Testing | JUnit 5 (Jupiter) |
| IDE | NetBeans (`nb-configuration.xml`, `nbactions.xml`) |

## Estructura del proyecto

```
ctn-sca-info/
├── database/
│   ├── db-tables-properties.sql   # Esquema completo (DDL) de la BD ctndb
│   └── seed.sql                    # Datos de ejemplo (especialidades, cursos, etc.)
├── flowcharts/
│   ├── README.md                   # Manual de integración con Google Classroom
│   └── classroom_integration_architecture.png
├── src/
│   ├── main/java/ctn/informatica/sca/
│   │   ├── config/       # AppConfig, StartupListener
│   │   ├── clases/       # conexion.java (JDBC)
│   │   ├── dao/          # DAOs por entidad (Alumno, Curso, Profesor, Planilla, Tarea, ...)
│   │   ├── model/        # Modelos (Alumno, Curso, Materia, Profesor, Tarea, User, ...)
│   │   ├── filter/       # AuthFilter, DateFilter
│   │   ├── google/       # GoogleClassroomService / SyncService / Utils
│   │   ├── servlets/     # Servlets (Home, Login, Profile, Planilla, Admin*, Parent, Google*, ...)
│   │   └── util/         # PasswordUtil, TotpUtils, PushNotificationService, ...
│   ├── main/webapp/      # JSPs, assets estáticos, manifest.json, sw.js, manuales PDF
│   └── test/java/...     # Pruebas unitarias (DAOs, servlets, utilidades)
├── pom.xml
├── CHANGELOG.md
└── LICENSE                # GPL-3.0
```

## Configuración

### Base de datos
La conexión (`conexion.java`) toma estos valores de variables de entorno (con defaults locales si no están definidas):

| Variable de entorno | Default |
|---|---|
| `CTN_DB_HOST` | `localhost:3306` |
| `CTN_DB_NAME` | `ctndb` |
| `CTN_DB_USER` | `testadmin` |
| `CTN_DB_PASSWORD` | *(vacío)* |

### PWA y notificaciones push

La instalación de la PWA funciona sin configuración adicional. Para habilitar las notificaciones Web Push, el servidor necesita un par VAPID:

| Variable de entorno | Contenido |
|---|---|
| `CTN_VAPID_PUBLIC_KEY` | Clave pública VAPID en Base64 URL-safe |
| `CTN_VAPID_PRIVATE_KEY` | Clave privada VAPID correspondiente |

El script `deploy.sh` conserva estas claves en el archivo de entorno del servicio. Deben configurarse juntas; la clave privada nunca debe incorporarse al repositorio ni enviarse al frontend.

### Google OAuth / Classroom
`AppConfig` carga `/WEB-INF/config.properties`, que **debe crearse manualmente** (no está versionado) con estas claves:

```properties
google.client.id=TU_CLIENT_ID
google.client.secret=TU_CLIENT_SECRET
google.redirect.uri=http://localhost:8080/GoogleCallbackServlet
```

Se obtienen creando credenciales OAuth 2.0 en Google Cloud Console con la **Google Classroom API** habilitada.

## Instalación y ejecución

### Requisitos
- JDK 17
- Maven
- MySQL 8+
- Un servidor compatible con Jakarta EE 11 / Servlet 6.0 (el proyecto trae embebido Tomcat 10 vía plugin de Maven)

### Pasos

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/provingch/ctn-sca-info.git
   cd ctn-sca-info
   ```

2. Crear la base de datos y cargar el esquema:
   ```bash
   mysql -u root -p < database/db-tables-properties.sql
   mysql -u root -p ctndb < database/seed.sql   # opcional: datos de ejemplo
   ```

3. Crear `src/main/webapp/WEB-INF/config.properties` con las credenciales de Google (ver sección anterior).

4. Definir las variables de entorno de conexión a la base de datos si difieren de los defaults locales.

5. Compilar y levantar con el plugin de Tomcat embebido:
   ```bash
   mvn tomcat10:run
   ```
   La app queda disponible en `http://localhost:8080/`.

   Alternativamente, generar el `.war` y desplegarlo en un Tomcat 10 externo:
   ```bash
   mvn clean package
   # copiar target/*.war al directorio webapps/ de Tomcat
   ```

### Tests

```bash
cd backend
./mvnw test

cd ../frontend
npm ci
npm test
npm run lint -- --deny-warnings
npm run build
```

Las pruebas de horarios que requieren MySQL se ejecutan cuando la base de integración está disponible; en un entorno sin esas credenciales se informan como omitidas. La automatización de GitHub ejecuta las comprobaciones de frontend y backend en cada pull request y cada cambio a `main`.

### Diseño compartido

Los componentes reutilizables viven en `frontend/src/components/ui`. La referencia visual está disponible para administradores en `/styleguide` e incluye tokens, botones, formularios, notas y estados comunes. Los estilos deben reutilizar las variables de `frontend/src/index.css` y verificarse en tema claro, oscuro y mobile.

## Documentación adicional

- **Historial de cambios**: [`CHANGELOG.md`](./CHANGELOG.md)
- **Manuales de usuario en PDF** (dentro de la app, `/pdfs/`): administrador, evaluador, padres, profesor

## Licencia

[GPL-3.0](./LICENSE)

## Autores

- [@provingch](https://github.com/provingch)
- [@Sh1b0](https://github.com/Sh1b0)
