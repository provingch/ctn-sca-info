
# Registro de Cambios

Todos los cambios notables de este proyecto serán documentados en este archivo.

El formato se basa en [Mantener un Registro de Cambios](https://keepachangelog.com/es/1.0.0/),
y este proyecto se adhiere a [Versionado Semántico](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2026-09-01

### Añadido

- **Administrador por especialidad**: nuevo alcance de administrador limitado a su propia especialidad (Alumnos, Horarios y Quejas), separado del administrador global (único con acceso a Materias, Usuarios, Asignaciones, Estado del sistema y Salas); protecciones de alcance e inmutabilidad en todos los endpoints del panel.
- **Control académico — reforma completa**: comparación directa del tema desarrollado contra el plan curricular (ya no por similitud aproximada), justificación obligatoria cuando un tema se desarrolla fuera de fecha, revisión de incumplimientos por evaluación a partir de 3 atrasos acumulados en la misma asignación con posibilidad de suspender al profesor por un período concreto, plantilla de plan curricular descargable para ambas etapas lectivas (Marzo–Junio y Julio–Noviembre), y seguimiento por evaluación de los planes ya aprobados con fecha de cumplimiento por tema.
- **Sistema de quejas**: la administración de cada especialidad puede cargar quejas contra un profesor indicando curso y motivo; quedan agrupadas por profesor con umbral de alerta.
- **Coordinación Pedagógica** (nuevo rol, nivel 5): revisa las quejas cargadas por especialidad para evaluar el desempeño de cada profesor.
- **Bandeja de notificaciones**: además de las notificaciones push, cada usuario puede revisar sus notificaciones anteriores, marcarlas como leídas (individualmente o todas a la vez) y acceder directamente al lugar donde se originó cada una.
- **Catálogo de salas**: alta de salas comunes y por pabellón de especialidad, con validación de conflicto de horario por sala además del conflicto por profesor y por curso.
- **Rediseño completo de la exportación de horarios a PDF**: layout ajustado al formato real del colegio, dos logos en el encabezado (institucional y de la especialidad), color de fila de receso según la especialidad, fila de salas al pie de cada bloque de horas, y manejo de nombres/materias largos sin desbordar la tabla.
- **Creación automática de cursos**: al pasar de año lectivo, se generan automáticamente las filas de curso faltantes para la nueva promoción de cada especialidad, sin necesidad de carga manual.
- **Registro de actividad**: cobertura ampliada a más acciones (cambio de contraseña, entre otras) en todos los controladores relevantes.
- **Bloqueo de inicio de sesión tras intentos fallidos**: el backend limita los reintentos y devuelve un tiempo de espera, mostrado como cuenta regresiva en el login.
- **Invalidación de sesión al cambiar la contraseña**: un cambio de contraseña invalida automáticamente las sesiones activas anteriores.
- **Notificaciones flotantes (toasts)**: los avisos temporales ahora flotan sobre todo el contenido y no se ven afectados por el scroll de la página; los mensajes de error muestran el detalle real devuelto por el backend en lugar de un texto genérico.
- **Identidad visual por especialidad**: ícono y color propios por especialidad en horarios, tarjetas del panel de administración y avatar del usuario.

### Cambiado

- **Modelo de cursos**: se separó el curso "abstracto" (especialidad + nivel + sección, sin año) del curso concreto ligado a una promoción y sus alumnos; las asignaciones y el horario ahora se vinculan al curso abstracto.
- **Exportación de horarios**: pasa a generarse una sola página por curso (antes se partía en dos), con la columna de horas realineada.

### Eliminado

- **Exportación de horarios a Excel**: se retiró por completo; la exportación de horarios queda únicamente en PDF.

### Corregido

- **Error 500 intermitente al refrescar sesión**: las llamadas concurrentes de renovación de token ya no compiten entre sí por el mismo token de un solo uso.
- **Encabezado del Excel de planillas**: se corrigió un error de geometría que podía posicionar mal los bloques de Especialidad/Curso/Año en algunas planillas.
- **Envío de notificaciones y umbral de quejas**: se corrigió un caso en el que no se disparaban correctamente.
- **Datos de quejas**: se corrigieron tipos de datos inconsistentes y el cálculo del nivel/curso asociado a cada queja.

---

## [1.0.0] - 2026-08-24

Primera versión estable de la base **Spring Boot + React**. Cierra la migración completa desde el stack legado (Servlets/JSP) y consolida todas las funcionalidades desarrolladas durante el proceso como release 1.0. La numeración se reinicia respecto del historial de la aplicación legada (ver versiones 2.0.0 y anteriores más abajo, correspondientes al sistema JSP ya retirado).

### Añadido

- **Migración completa a Spring Boot + React**: backend reescrito en Spring Boot 4 (Java 17) con autenticación JWT, y frontend nuevo en React 19 + TypeScript + Vite, reemplazando la totalidad de Servlets/JSP legados.
- **Libro de Cátedra**: nuevo módulo que reemplaza la entrada "Iniciar una clase", con dos flujos diferenciados:
  - **Plan curricular**: el profesor descarga su plantilla pre-rellenada por asignación, la completa y la sube; el sistema la parsea, valida contra la asignación real y la envía a evaluación. Evaluación puede aprobar o rechazar (con observaciones obligatorias) desde una pantalla dedicada de revisión.
  - **Iniciar clase**: bloqueado mientras la asignación no tenga un plan curricular aprobado para la etapa vigente.
- **Verificación automática de tema de clase**: al iniciar una clase, el tema ingresado se compara contra el próximo tema pendiente del plan curricular aprobado (similitud de texto normalizada, sin bloquear al profesor); los casos dudosos quedan visibles para evaluación en una bandeja de revisión, con acciones de confirmar/descartar.
- **Horario por horas cátedra**: carga de horario semanal por asignación con detección de conflictos por profesor y por curso, selección de rango de horas en un solo paso, y exportación dinámica del horario por curso.
- **Panel de Administración en 7 bloques**: Materias, Usuarios, Asignaciones, Alumnos, Horarios, Estado del sistema y Sistema de diseño, cada uno con su propio flujo de alta/edición/eliminación.
  - **Alumnos**: navegación por bloques especialidad → curso → sección antes de llegar a la tabla de alumnos, con vinculación de padres/encargados por búsqueda.
  - **Estado del sistema**: salud técnica (conexión a BD, migraciones aplicadas, última sincronización con Classroom, espacio ocupado por logs).
- **Registro de actividad real por usuario**: cada acción relevante (login, cambios de perfil, carga/aprobación/rechazo de plan curricular, creación de clase, sincronización con Classroom, altas/bajas desde Admin) se escribe en un archivo `.txt` individual por usuario, referenciado desde la base de datos.
- **Notificaciones push sobre el flujo de plan curricular**: el profesor recibe una notificación cuando su plan es aprobado o rechazado; evaluación recibe una notificación cuando un profesor sube un nuevo plan curricular para revisar.
- **Íconos e indicadores de origen de tareas**: distinción visual entre tareas locales y tareas importadas de Google Classroom, con restricción de edición/eliminación para estas últimas.

### Corregido

- **Indicador de plan curricular incorrecto tras la carga**: la plantilla descargable tenía la etapa hardcodeada como "1°" sin importar la etapa lectiva real, por lo que un plan cargado en la segunda etapa quedaba guardado con `etapa=1` y la consulta del estado (que sí usa la etapa real) nunca lo encontraba — el profesor volvía a ver el formulario de carga aunque el plan estuviera guardado. Se corrigió para que la plantilla use la etapa vigente real.
- **Botón "Ir a cargar/revisar Plan curricular" sin efecto**: navegaba cambiando el hash de la URL en lugar de actualizar los parámetros de búsqueda que usa el router de la aplicación, por lo que nunca cambiaba de vista.
- **Cálculo de rangos de notas**: se corrigieron dos bugs en `computeGradeRanges` que producían rangos superpuestos/duplicados y hacían inalcanzable la nota máxima en planillas con pocos puntos totales.
- **Error 500 al exportar planillas angostas**: se evitaba fusionar una única celda de encabezado (rechazado por Apache POI) en planillas con pocas tareas.
- **Encabezado dinámico del Excel de planilla**: el título y los bloques de Especialidad/Curso/Año ya no quedan cortados ni superpuestos en planillas angostas; se corrigió además la rotación vertical incorrecta del nombre "Mayo" y el estilo del instrumento "Repaso".
- **Backend caído en producción**: `AuthController` tenía dos constructores públicos sin anotar, lo que impedía que Spring resolviera cuál usar tras agregar el registro de actividad.
- **Carga de plan curricular (500 → 415)**: el cliente HTTP forzaba `Content-Type: application/json` incluso cuando el cuerpo era un `FormData` de subida de archivo.
- **Crash en la vista de plan curricular**: acceso a una propiedad de un valor `undefined` al no manejar el caso de "sin plan cargado".

---

## Créditos

Proyecto desarrollado por:

* [@provingch] [https://github.com/provingch](https://github.com/provingch)
* [@Sh1b0] [https://github.com/Sh1b0](https://github.com/Sh1b0)
* [@schmidtsamuel626] [https://github.com/schmidtsamuel626](https://github.com/schmidtsamuel626)
* [@Macrosss] [https://github.com/tukp5678](https://github.com/tukp5678)

Inicio del desarrollo: 18 de junio de 2026.
Propuesta aceptada: 29 de junio de 2026.
Fusion desarrollada: 28 de julio de 2026
