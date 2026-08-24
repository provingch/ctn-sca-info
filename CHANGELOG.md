
# Registro de Cambios

Todos los cambios notables de este proyecto serán documentados en este archivo.

El formato se basa en [Mantener un Registro de Cambios](https://keepachangelog.com/es/1.0.0/),
y este proyecto se adhiere a [Versionado Semántico](https://semver.org/spec/v2.0.0.html).

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
