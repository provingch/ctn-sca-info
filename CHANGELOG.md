# Registro de Cambios

Todos los cambios notables de este proyecto serán documentados en este archivo.

El formato se basa en [Mantener un Registro de Cambios](https://keepachangelog.com/es/1.0.0/),
y este proyecto se adhiere a [Versionado Semántico](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2026-07-27

### Añadido
- **Mejoras de perfil y seguridad**: se consolidó la experiencia del perfil con soporte para configuración de seguridad, autenticación de dos factores TOTP, estado de PWA y registro de actividad.
- **Soporte PWA**: se integró la experiencia de aplicación instalable con manifest, icono y metadatos móviles para mejorar el uso desde navegador y dispositivo móvil.
- **Exportación masiva de planillas**: ahora el flujo de exportación permite generar archivos Excel por especialidad, curso, sección y período con una estructura más consistente para el usuario.
- **Mejoras de usabilidad en evaluación**: se incorporó un manejo más claro de los cursos y niveles de formación en la interfaz de evaluación.

### Mejorado
- **Etiquetas de cursos**: los niveles de curso ahora se muestran de forma uniforme como 1º, 2º y 3º en la interfaz y en el modelo de datos.
- **Experiencia de perfil**: se ajustó la vista para evitar errores de renderizado y mejorar la lectura de los paneles de seguridad y configuración.
- **Robustez del exportador**: se fortaleció la validación de parámetros de curso y promoción para evitar solicitudes inválidas o inconsistentes al generar planillas.
- **Seguridad de credenciales**: se reforzó el manejo de contraseñas mediante hashing BCrypt, manteniendo compatibilidad con registros previos en texto plano.

### Corregido
- **Error de sintaxis en el perfil**: se corrigió un problema de renderizado en Profile.jsp que impedía interpretar correctamente parte del contenido dinámico.
- **Flujo de exportación**: se evitó la generación de planillas para combinaciones de curso/promoción inválidas.
- **Consistencia visual**: se ajustaron detalles de presentación y textos para mantener una experiencia más clara y coherente entre módulos.

## [1.7.1] - 2026-07-24

### Mejorado
- **UI/UX**: Rediseño de interfaz para mejorar claridad y consistencia visual.
  - Actualizado el encabezado principal de `Home.jsp` de "Colegio Técnico Nacional" a "Panel de materias" para mejor claridad de propósito.
  - Modernizado formulario de perfil en `Profile.jsp`: migrado de layout table-grid a card-based layout con campos etiquetados apropiadamente.
  - Actualizado encabezado de login en `index.jsp` de "Acceso institucional" a "Iniciar sesión" (más claro).
  - Agregado indicador visual "Sin vincular" para cursos de Google Classroom sin correspondencia de materia.

### Cambiado
- **Comportamiento de navegación**: Cursos de Google Classroom sin materia vinculada ahora redirigen a `PlanillaServlet` (en lugar de ir a Classroom externamente).
- **Estilos CSS**: Refactorizado `styles/profile-grid.css` para usar sistema de componentes moderno (`.form-card`, `.form-field`) mientras se mantiene compatibilidad retroactiva para formularios de seguridad.
- **Componentes**: Agregados nuevos estilos CSS en `components.css`: `.card-title-row`, `.badge-warning` para indicadores visuales consistentes.

### Notas técnicas
- Preservadas todas las dependencias de autoguard JavaScript (nombres de campos inmutables): `correo`, `telefono`, `celular`, `usuario`, `especialidadId`.
- Google Classroom status ahora usa clases `.classroom-status--connected` y `.classroom-status--disconnected` con colores coherentes.

## [1.7.0] - 2026-07-24

### Corregido
- Se actualizaron dependencias con vulnerabilidades conocidas para reducir el riesgo de seguridad del proyecto.
- Se actualizó `mysql-connector-j` de 8.0.33 a 8.2.0.
- Se actualizó `poi-ooxml` de 5.2.3 a 5.4.0.
- Se registró la remediación de seguridad en la línea base de la versión 1.7 del changelog.

## [1.6.0] - 2026-07-15

### Añadido
- Diseño visual unificado basado en el sistema SCA, con tokens, componentes y tema claro/oscuro compartido.
- Nueva experiencia visual para las vistas principales: Home, Planilla, Perfil, Admin, Parent y Tarea.
- Banner de consentimiento de cookies y soporte para cambio de contraseña desde el perfil.

### Modificado
- Se centralizaron los estilos base y el comportamiento del tema en recursos compartidos.
- Se actualizaron las vistas principales para una experiencia más consistente y profesional.
- Se modernizó la dependencia de MySQL y se eliminó una dependencia obsoleta relacionada con Google OAuth.

### Corregido
- Se mejoró la consistencia visual entre encabezados, tablas, formularios y tarjetas.
- Se ajustaron detalles de renderizado y navegación para reducir inconsistencias en distintos módulos.

## [v1.5] - 2026-07-13

### Añadido
- **Mejora visual general**: se pulió la interfaz de Home, Planilla, Perfil y Login con una estética más profesional, consistente y alineada con la identidad institucional.
- **Banner de consentimiento de cookies**: se incorporó una advertencia visual y funcional para informar sobre el uso de cookies y mejorar la experiencia inicial del usuario.
- **Cambio de contraseña desde el perfil**: ahora el usuario puede actualizar su contraseña directamente desde la sección de perfil.

### Modificado
- **Experiencia de la planilla**: se reorganizó la estructura visual de la vista, mejorando la jerarquía del encabezado, los controles y la lectura de la tabla.
- **Persistencia de sesión**: se reforzó el flujo de autenticación para mantener la sesión activa de forma más estable tras reingresar al sistema.
- **Manual de uso**: se actualizó y ajustó el manual para reflejar los cambios de la interfaz y los nuevos flujos de sesión y perfil.

### Corregido
- **Compatibilidad visual**: se ajustaron estilos compartidos para que la navegación y los formularios se vean más limpios y coherentes en diferentes vistas.

## [v1.0] - 2026-07-13

Muchas gracias a aquellos que han apoyado este proyecto :), estas palabras de agradecimiento son para todos aquellos que hicieron posible las ganas y las fuerzas para estar tanto por este proyecto un mes entero, a partir de aqui se implementaran features nuevas, pero el proyecto base ya estaria completo.

### Añadido
- **Versión estable del sistema**: consolidación de las mejoras de las versiones 0.9.x en una entrega más completa y consistente para uso general.
- **Portal para padres**: nueva vista para consultar notas por familia, con resumen por materia y detalle por tarea por hijo.
- **Gestión de perfil del profesor**: ahora es posible actualizar datos personales, administrar materias manuales y cambiar la contraseña desde el perfil.
- **Retención de sesión y preferencia de cookies**: se incorporó la persistencia de sesión mediante cookies funcionales y un banner de consentimiento inicial.

### Modificado
- **Experiencia de Home y Planilla**: se simplificó la interfaz, se mejoró el layout de la tabla y se redujeron elementos visuales innecesarios.
- **Flujo de tareas y etapas**: se reforzó la coherencia entre la planilla, las etapas y el formulario de tareas.
- **Integración con Google Classroom**: se mejoró la carga de cursos, la asociación de tareas y la experiencia cuando no hay conexión disponible.
- **Diseño responsive y modo oscuro**: se ajustó la experiencia visual para pantallas pequeñas y se pulió la interfaz general.

### Corregido
- **Problemas de sesión**: se corrigió el flujo de login y logout para que la autenticación se mantenga de forma más estable.
- **Errores en la carga de planillas y tareas**: se corrigieron problemas de títulos, etapas, valores por defecto y visualización de información.
- **Sincronización con Classroom**: se resolvieron fallos relacionados con la conexión, el matching de cursos y la visualización de datos incompletos.

## [v0.9] - 2026-07-12

### Añadido
- **Portal para padres**: se incorporó una vista nueva orientada a la consulta de notas por familia, con una primera pantalla de resumen por materia y un acceso posterior al detalle por tarea para cada hijo asociado.
- **Rol de usuario padre**: se habilitó el flujo de autenticación y redirección para usuarios con nivel de padre, reutilizando la infraestructura de usuarios ya existente en el sistema.
- **Resumen y detalle de calificaciones**: se añadieron modelos y consultas para construir un resumen por materia y el detalle de notas por tarea, permitiendo una navegación más clara desde el padre hacia la información del alumno.
- **Registro de ejemplo para padres**: se actualizó la seed local para incluir una relación de ejemplo entre un usuario padre y su hijo, facilitando la validación del flujo en entorno de desarrollo.

### Modificado
- **Persistencia de materias manuales del profesor**: la materia escrita por el profesor desde el perfil ya no queda solo en memoria de sesión, sino que se integra con la persistencia del profesor para que el sistema pueda reconocerla de forma estable en home y planillas.
- **Título de la planilla**: la cabecera y el título se alinean con la materia realmente seleccionada, evitando que el encabezado tome un nombre residual o derivado de un contexto anterior.
- **Control de etapas en tareas**: la etapa seleccionada se mantiene coherente durante la navegación entre la planilla y el formulario de tareas, evitando cambios inesperados al volver al listado.
- **Refactor de rutas de servlet**: se unificó la ruta principal del flujo de tareas y se separó la implementación legacy para evitar confusión entre dos clases con responsabilidades casi equivalentes.
- **UI de Home y Planilla**: se limpiaron los bloques y mensajes redundantes, dejando una vista más limpia cuando la conexión a Google Classroom no está disponible.
- **Perfil del profesor**: se dejó el ingreso manual de materias dentro del menú de perfil como un campo de texto útil y persistible, alineado con el concepto de materias del profesor en la aplicación.

### Corregido
- **Fallback hardcodeado de “Algorítmica”**: se eliminó la aparición de un bloque fijo o un nombre artificial cuando la conexión con Google Classroom no estaba activa, reemplazándolo por un estado de placeholder más claro.
- **Bloques de Classroom vacíos**: se removió la visualización de mensajes y bloques innecesarios cuando no hay conexión con Classroom, reduciendo ruido en la pantalla de inicio.
- **Botones y textos en la planilla**: se corrigió la presentación de acciones y textos dentro de la vista de planilla para que fueran visibles y coherentes con el flujo actual.
- **Fecha límite en la planilla**: se quitó el cuadro de texto asociado a la fecha límite del contexto visual de la planilla, dejando la vista más limpia y enfocada en las notas.
- **Routing de inicio**: se dejó el acceso al logo de la página como enlace directo a la home, mejorando la navegación principal del sistema.

### Validado
- Se ejecutó la verificación del proyecto con Maven y quedó en estado estable para esta entrega.
- Resultado verificado: `BUILD SUCCESS`, con `13` pruebas ejecutadas, `0` fallos, `0` errores y `0` omisiones.

## [v0.8] - 2026-07-11

### Corregido
- **Título de la planilla**: se dejó el encabezado y el título HTML alineados con el nombre de la materia, evitando que la vista tome un contexto de título inconsistente o mezclado.
- **Flujo de etapas**: se preservó la etapa seleccionada (`primera` o `segunda`) al abrir y volver desde el formulario de tareas, evitando cambios accidentales de contexto entre etapas.
- **Formulario de tareas**: se reforzó el paso del parámetro `etapa` en el `POST` y el redirect hacia `PlanillaServlet`, para mantener la navegación coherente con la planilla activa.
- **Rutas de servlet**: se dejó la ruta principal activa en `TareaServlet` y la implementación legacy renombrada explícitamente a `LegacyTareaServlet`, eliminando la ambigüedad entre dos clases de nombres casi iguales.

## [v0.7] - 2025-07-10
- **Nombre automático**: Ahora, en la generación de planillas, el nombre de la materia será igual a la materia que elija el profesor, incluyendo el nombre de la planilla que dice "Tareas - ".
Se corrigieron los archivos HomeServlet.java, Home.jsp

## [v0.6.5] - 2026-07-10
- **Tareas no entregadas**: Ahora, cuando un alumno no tiene una tarea entregada, automáticamente su puntaje es 0 en vez de esperar una respuesta para dar el puntaje.
Se corrigieron los archivos: StudentRowDao.java, PlanillaServlet.java y StudentRowDaoTest.java

## [v0.6.3] - 2026-07-09
- **Correción de color**: Se cambió el tono de campos y tarjetas con color blanco a un tono más oscuro y también el tono de las letras para que sean legibles.
Se corrigió el archivo: dark-mode.css

## [v0.6.4] - 2026-07-10
- **Responsive UI**: Se añadieron reglas responsive para header, login y layout principal para mejorar la experiencia en móviles y tabletas. Ajustes realizados:
  - `styles/header.css`: backdrop blur, compact layout y medias queries para varios breakpoints.
  - `styles/login.css`: tarjeta de login adaptable, formularios y botones con tamaño y paddings reducidos en pantallas pequeñas.
  - `styles/general.css`: padding y ancho máximo adaptativos para `main`.
- **Estética**: refinamiento de paleta y sombras en `styles/theme.css` para mejor contraste y legibilidad.
- **Build**: actualizado `pom.xml` a la versión `0.6.4`.

## [v0.6.1] - 2026-07-09
- **Botón que habilita el modo oscuro**: El botón no aparecía como debería, entonces se agregó un estilo más llamativo para que se encuentre.
Se corrigio Profile.jsp

## [v0.6] - 2026-07-09
- **Implementación del modo oscuro**: Se implementó un botón en la sección del perfil que permite cambiar la paleta de colores de la página a un modo oscuro. Se agregó un script java y un css que permite los cambios.
También se implementó el script en los siguientes archivos:
admin.jsp, Home.jsp, index.jsp, Planilla.jsp, Profile.jsp, Tarea.jsp

## [v0.5.1.1] - 2026-07-09
- **Error de googlecredentials**: El sistema construía GoogleCredentials simple usando solo el acces token, lanzando un error 500. Ahora se usa UserRefreshCredentials y si no hay refresh token o el token expira, el servicio lanza una excepción controlada y se muestra un error amigable.

## [v0.5.1] - 2026-07-09
- **Prueba de modo oscuro**: Se agregó un botón de modo oscuro en la sección de 'Mi Perfil' para comprobar la visualización y eficacia.

## [v0.5] - 2026-07-09

### Quitado
- **Panel de integración administrativa**: se eliminó `IntegralServlet`, `Integral.jsp` y las cuentas de integración por especialidad (`*-itg`) que permitían corregir manualmente el correo de Google de los alumnos.
- **Login especial de cuentas de integración**: se quitó de `UserDao` el mecanismo de contraseña por defecto asociado a esas cuentas, junto con la clase `IntegrationAdminUtils`.

### Modificado
- **Vinculación de alumnos con Classroom**: la asignación del correo de Google de cada alumno vuelve a depender exclusivamente del emparejamiento automático por nombre de usuario (correo ya vinculado o nombre completo exacto, en cualquier orden apellido/nombre) que ya realizaba `GoogleClassroomService`. Si un alumno no tiene coincidencia, ninguna casilla de su planilla se completa automáticamente (las notas importadas de Classroom para alumnos sin vincular se omiten).
- **Inicio de sesión**: los usuarios de nivel 3 ya no tienen un destino propio tras autenticarse, ya que su único flujo (el panel de integración) fue retirado.

## [v0.4] - 2026-07-08

### Añadido
- **Flujo de integración administrativa**: se incorporó un panel dedicado para corregir manualmente los correos de Google de los alumnos por especialidad antes de sincronizar con Classroom.
- **Usuarios de integración por especialidad**: ahora se pueden usar cuentas como `informatica-itg` y `electricidad-itg` para gestionar esa vinculación desde un rol separado.
- **Persistencia de correos de Google por alumno**: los cambios realizados en el panel quedan guardados en la base de datos para reutilizarse en futuras sincronizaciones.

### Modificado
- **Autorización de acceso**: los usuarios de integración ahora se redirigen a un flujo propio y no dependen del panel de exportación administrativa tradicional.
- **Documentación**: se actualizó la guía de uso para reflejar el nuevo flujo manual de vinculación de alumnos.

### Corregido
- **Vinculación de alumnos**: se redujo la dependencia exclusiva del emparejamiento automático por correo, dejando un mecanismo manual y controlado para casos donde el email no coincide con el registro local.

## [v0.3] - 2026-07-08

### Añadido
- **Metadatos de tareas de Classroom**: se incorporaron fecha de inicio, fecha límite y enlace directo a la tarea en Classroom para las tareas importadas.
- **Migración de esquema**: se agregó un script para incorporar las nuevas columnas en bases existentes sin perder los datos previos.

### Modificado
- **Vista de planilla**: las tareas ahora muestran un tooltip con los detalles de la tarea y abren directamente la tarea de Classroom cuando existe un enlace disponible.
- **Control de etapas**: se incorporó una regla de transición por fecha para pasar de primera a segunda etapa desde el 15 de julio.

### Corregido
- **Asignación de tareas por clase**: se reforzó la sincronización para que las tareas importadas se asocien al curso/planilla correcto y no se mezclen entre clases.

## [v0.2] - 2026-07-08

### Añadido
- **Sincronización inicial con Classroom desde la planilla**: la vista de planilla ahora intenta vincularse y sincronizar tareas automáticamente cuando la planilla aún no tiene tareas o no tiene un curso de Classroom asociado.

### Modificado
- **PlanillaServlet**: se ajustó el flujo para usar el profesor real de sesión/base de datos, resolver correctamente el curso de Classroom y cargar el contexto de la planilla según su curso real.
- **LoginServlet**: al iniciar sesión se guarda el profesor en sesión para que la vista de planilla pueda validar la conexión con Classroom sin depender de datos incompletos.
- **Carga de registros y alumnos**: se reforzó la carga para que los estudiantes y registros se creen solo para el curso correspondiente a la planilla, evitando mezclas entre cursos.
- **Vista de planilla**: las tareas ahora muestran tooltip con inicio/límite y abren directamente la tarea de Classroom cuando existe un enlace disponible.
- **Control de etapas**: se incorporó una regla de transición por fecha para pasar de primera a segunda etapa desde el 15 de julio.

### Corregido
- **Conexión a Google Classroom**: se corrigió el problema que impedía conectar y sincronizar desde la pantalla de planilla.
- **Carga de tareas y alumnos**: se solucionó la ausencia de tareas y la falta de alumnos al abrir una planilla cuando el contexto del curso no estaba correctamente resuelto.
- **Validación de build**: se verificó el proyecto con Maven y quedó en estado estable para la versión v0.2.

## [v0.1] - 2026-07-08

### Modificado
- **Home y planillas**: se reforzó la carga para mostrar solo las planillas de materias realmente vinculadas al profesor, evitando mezclar materias no autorizadas o ajenas al contexto del curso.
- **PlanillaDao**: se ajustó la consulta para filtrar por la relación real de profesor-materia y por el curso/etapa seleccionados.

### Corregido
- **Carga de planillas**: se corrigió el flujo para que no aparezcan planillas de materias no registradas aunque coincidan por nombre o por contexto general.

## [v0.0.9.5] - 2026-07-08

### Modificado
- **Home**: se ajustó la carga de cursos y planillas para que el selector use el catálogo real de cursos del profesor y la vista muestre las planillas del curso seleccionado.
- **CursoDao**: se mejoró la consulta para incluir cursos asociados por especialidad del profesor incluso cuando aún no existan planillas directas para ese curso.

### Corregido
- **HomeServlet / Home.jsp**: se corrigió el flujo para que al cambiar de curso y etapa se carguen correctamente las planillas correspondientes.

## [v0.0.9] - 2026-07-08

### Añadido
- **Manual de integración de cursos con Google Classroom**: se reemplazó el README por una guía más detallada con la convención recomendada para nombrar cursos y detectar correctamente su correspondencia con las planillas.

### Modificado
- **Home.jsp**: se eliminó la vista local de planillas para dejar la página centrada exclusivamente en los bloques detectados desde Google Classroom.
- **HomeServlet**: se añadió el mapeo de cursos de Classroom a planillas y se filtraron las planillas ya representadas para evitar bloques duplicados.
- **GoogleClassroomService**: se mejoró el matching de materias multi-palabra como "Laboratorio Redes" usando normalización de frases.
- **UX de Home**: los bloques detectados desde Classroom ahora enlazan a la planilla correspondiente cuando existe coincidencia.

### Corregido
- **Duplicación de bloques**: se evitó mostrar dos tarjetas equivalentes para una misma materia cuando ya existía un bloque detectado desde Classroom.
- **Repetición de texto**: se quitó la repetición del nombre de la materia dentro del cuerpo de los bloques de Classroom.

## [v0.0.8] - 2026-07-08

### Modificado
- **Home.jsp**: se eliminó la vista local de planillas para dejar la página centrada exclusivamente en los bloques detectados desde Google Classroom.
- **HomeServlet**: se añadió el mapeo de cursos de Classroom a planillas y se filtraron las planillas ya representadas para evitar bloques duplicados.
- **GoogleClassroomService**: se mejoró el matching de materias multi-palabra como "Laboratorio Redes" usando normalización de frases.
- **UX de Home**: los bloques detectados desde Classroom ahora enlazan a la planilla correspondiente cuando existe coincidencia.

### Corregido
- **Duplicación de bloques**: se evitó mostrar dos tarjetas equivalentes para una misma materia cuando ya existía un bloque detectado desde Classroom.
- **Repetición de texto**: se quitó la repetición del nombre de la materia dentro del cuerpo de los bloques de Classroom.

## [v0.0.7.2] - 2026-07-06

### Añadido
- **Tabla padre en base de datos**: Para que el padre pueda ingresar también a la página y revisar la planilla del alumno.
- **Tabla alumno_padre**: Por medio de este se relacionan las tablas de alumno y padre.

## [v0.0.7] - 2026-07-06

### Modificado
- **Home.jsp**: mejorada la presentación del estado de Google Classroom, selección de curso/etapa y tarjetas visuales para la pantalla de inicio.
- **Profile.jsp**: reorganizada la experiencia del perfil con tarjetas de datos, conexión a Classroom y listado más claro de materias asignadas.
- **Planilla.jsp**: pulida la cabecera de planilla, estado de sincronización de Classroom y mensajes de ayuda cuando no hay conexión.
- **UX general**: se añadió feedback más claro para conectar Classroom desde el perfil antes de sincronizar tareas.
- **Build validado**: compilación exitosa con Maven (`mvn -DskipTests compile`).

## [v0.0.6.7] - 2026-07-06

### Añadido
- **Selector de materias en perfil**: ahora el formulario de agregar materias muestra las opciones disponibles desde la base de datos y permite seleccionar una materia real para vincular al profesor.

### Modificado
- **ProfileServlet** y **Profile.jsp**: se alineó la vista de materias del perfil con la lista real de materias registradas por el profesor.
- **HomeServlet**: se ajustó el flujo de carga de datos para que la pantalla de inicio use la información real del profesor y de sus materias vinculadas.
- **PlanillaDao**: se corrigió la consulta de planillas para trabajar con los valores de etapa almacenados en la base de datos (`primera`/`segunda`).

### Corregido
- **Desajuste de etapas en Home y Planillas**: se solucionó el problema por el cual la selección de etapa no coincidía con los valores esperados por la base de datos.
- **Materias no visibles en perfil/home**: se corrigió la carga y renderizado de las materias asociadas al profesor para que se reflejen correctamente en la interfaz.

## [v0.0.6.6] - 2026-07-06

### Añadido
- **Materia.java**: Estructura minima para materias
- **MareriaDao.java**: DAO para Materia.java

## Modificado
- **ProfesorMateriaDao.java**: Se agrega la vinculacion entre profesor y materia
- **HomeServlet.java**: Se modifica el listado de materias relacionadas entre los profesores
- **ProfileServlet.java**: Se muestran las materias que el profesor cargo especificamente en el apartado de Perfil -> Materias
- **db-tables-properties.sql**: Se modifico la estructura de las materias para cargarlas con el nuevo modelo

## [v0.0.6.5] - 2026-07-06

### Modificado
- **GoogleClassroomService**: Accion de creacion constante de credenciales a cada llamado para evitar errores en la API

## [v0.0.6.4] - 2026-07-05

### Añadido
- **Persistencia de materias del profesor**: nueva tabla `profesor_materia` en la base de datos para guardar permanentemente las materias que el profesor registra desde su perfil.
- **ProfesorMateriaDao**: nueva clase DAO para leer y guardar materias persistidas asociadas a cada profesor.
- **Fallback de matching con Classroom**: el sistema ahora detecta y empareja cursos de Classroom que incluyen el nivel en el nombre (ej. "Algorítmica Segundo") aunque no tengan la sección explícita.
- **Logs de diagnóstico**: se añadieron trazas DEBUG en `GoogleClassroomService.listTeacherCourses()` para diagnosticar qué cursos devuelve la API de Classroom y cómo se procesa cada uno.

### Modificado
- **ProfileServlet**: ahora carga y persiste materias del profesor en `profesor_materia`, con fallback a sesión y luego a `planilla` (materias derivadas de planillas en BD).
- **Profile.jsp**: se eliminó el placeholder de ejemplo ("Ej. Programación") del input de agregar materia.
- **GoogleClassroomService.listAllowedCourses()**: añadido fallback `tryExtractLevel()` para aceptar cursos sin `CourseKey` completo si el nivel se detecta en el nombre.
- **Esquema de BD**: se incorporó la tabla `profesor_materia` con FK a `profesor` y cascada de actualización/eliminación.

### Corregido
- **Errores de tipo en GoogleClassroomService**: cambió `seenCourseIds` de `LinkedHashSet<String>` a `LinkedHashSet<Integer>` para aceptar `curso.getId()` (int).

## [v0.0.6.3] - 2026-07-05

### Añadido
- **Nueva estructura de perfil**: el perfil del profesor ahora presenta tres secciones diferenciadas: Perfil, Materias y Registros.
- **Gestión de materias**: en la sección Materias se pueden agregar y eliminar materias desde la interfaz.
- **Registro de actividad**: se añadió un panel de registros con los movimientos recientes del usuario en el perfil.

### Modificado
- **ProfileServlet**: ahora gestiona las acciones de agregar y eliminar materias, además de mantener un registro simple de actividad en sesión.
- **Profile.jsp**: se reorganizó la vista del perfil con navegación por pestañas y una interfaz más completa para la administración de materias.

## [v0.0.6.2] - 2026-07-05

### Modificado
- **Arreglo de bugs**: Bug arreglado en HomeServlet.java con referencia a metodo antiguo no existente (actualmente) en GoogleClassroomService.java.
- **Fuente de verdad de materias**: HomeServlet y ProfileServlet ahora obtienen las materias para filtrar cursos de Classroom desde las materias ya registradas por el profesor en la base de datos, en lugar de usar una lista fija en el código.
- **Consulta de materias del profesor**: se añadió un acceso desde PlanillaDao para recuperar las materias asociadas al profesor y usarlas en el matching con Classroom.

## [v0.0.6.1] - 2026-07-05

### Modificado
- **Filtrado por materias registradas en perfil**: el matching con Classroom ahora considera las materias que el profesor registra desde su perfil como fuente de verdad para decidir qué cursos mostrar y enlazar.
- **Integración con Classroom**: se ajustó la lógica para comparar los cursos de Classroom con las materias registradas por el profesor, considerando además el nivel y la sección.

## [v0.0.6] - 2026-07-05

### Modificado
- **Emparejamiento de cursos con Classroom**: ahora el sistema usa la Sala de Classroom como señal de especialidad para separar materias compartidas entre varias especialidades cuando coinciden en nivel y sección.
- **Parseo de cursos**: se extendió la lógica para considerar la Sala junto con el nivel y la sección al identificar a qué curso local corresponde un curso de Classroom.

## [v0.0.5.1] - 2026-07-05

### Corregido
- **`TareaDao`**: ahora maneja la ausencia de `google_coursework_id` en bases de datos antiguas para evitar errores internos 500 al cargar planillas.

## [v0.0.5] - 2026-07-05

### Añadido
- **Sincronización manual desde la planilla**: se agregó un botón en la vista de planilla para disparar una sincronización manual con Google Classroom desde `PlanillaServlet`.
- **Persistencia de IDs de Classroom**: ahora se almacenan `google_course_id` en las planillas y `google_coursework_id` en las tareas para vincular registros locales con recursos de Classroom.
- **Soporte de normalización de títulos**: se incorporó una normalización básica de nombres de tareas para facilitar el emparejamiento con Classroom.

### Modificado
- **`PlanillaServlet`**: ahora expone la acción de sincronización manual y muestra el botón solo cuando el profesor está conectado a Google Classroom.
- **`Planilla.jsp`**: se agregó la acción visual para ejecutar la sincronización desde la interfaz.
- **Esquema de base de datos**: se añadieron columnas para guardar los IDs de Classroom en `planilla` y `tarea`.

## [v0.0.4] - 2026-07-05

### Añadido
- **Integración inicial de Google Classroom en `HomeServlet`**: el sistema ahora muestra cursos de Classroom filtrados según los cursos locales del profesor.
- **Visualización de cursos de Classroom en perfil**: `ProfileServlet` y `Profile.jsp` listan los cursos de Google Classroom compatibles con los cursos locales.
- **Notificación de errores de Classroom**: `HomeServlet` captura errores de conexión y expone un mensaje de error en la interfaz.
- **Desconexión de Classroom**: nuevo endpoint `GoogleDisconnectServlet` y botón en `Profile.jsp` para desvincular la cuenta de Classroom.

### Modificado
- **`HomeServlet`**: se agregó carga de cursos de Google Classroom mediante `GoogleClassroomService`.
- **`ProfileServlet`**: ahora muestra los cursos de Classroom asociados al profesor conectado.
- **`Profile.jsp`**: se eliminó el botón de guardar y se agregó guardado automático tras editar los datos del perfil.
- **JSPs de UI**: `Home.jsp` y `Profile.jsp` presentan los cursos de Classroom disponibles.

## [v0.0.3] - 2026-07-02

### Añadido
- **Utilidades de Parseo de Google Classroom**: Clase `GoogleClassroomUtils` para extraer nivel (Primero/Segundo/Tercero) y sección (A/B/C) del nombre de cursos de Classroom [GoogleClassroomUtils.java](./src/main/java/ctn/informatica/sca/google/GoogleClassroomUtils.java)
- **Servicio de Sincronización Google Classroom**: Clase `GoogleClassroomService` para conectarse a Google Classroom API, listar cursos del profesor y filtrar solo aquellos que coinciden con los cursos locales [GoogleClassroomService.java](./src/main/java/ctn/informatica/sca/google/GoogleClassroomService.java)
- **Métodos de Mapeo de Curso**: Métodos `matchesCourseKey()`, `getNivel()` y `getCourseKey()` en modelo `Curso` para validar si un curso de Classroom corresponde a un curso local [Curso.java (Líneas 29-47)](./src/main/java/ctn/informatica/sca/model/Curso.java#L29-L47)

### Modificado
- **Estructura del Modelo Curso**: Se agregó soporte para validar nivel y sección contra claves de curso de Classroom, permitiendo filtrado bidireccional [Curso.java](./src/main/java/ctn/informatica/sca/model/Curso.java)

## [v0.0.2.1] - 2026-07-01

### Modificado
- **Mejor manejo de errores**: Se mejoro conexion.java para que se registre correctamente el error y mejorar la trazabilidad [conexion.java (Lineas 42-52)](./src/main/java/ctn/informatica/sca/clases/conexion.java#L42-52)
- **Evitar NPE**: Se modifico UserDao.java para capturar la excepcion SQL y envolverlo en un mensaje correcto y claro [UserDao.java (Lineas 36-37)](./src/main/java/ctn/informatica/sca/dao/UserDao.java#L36-37)

## [v0.0.2] - 2026-07-01

### Añadido
- **Interfaz de Conexión Google Classroom**: Sección en perfil de profesor para conectar/reconectar cuenta de Google Classroom [Profile.jsp (Líneas 144-160)](./src/main/webapp/Profile.jsp#L144-L160)
- **Esquema de Base de Datos para Google OAuth**: Columnas `google_email`, `google_access_token`, `google_refresh_token`, `google_token_expiry` en tabla profesor [db-tables-properties.sql (Líneas 49-52)](./database/db-tables-properties.sql#L49-L52)
- **Métodos de Gestión de Tokens Google**: Métodos `updateGoogleTokens()` y `findByGoogleEmail()` en ProfesorDao para gestionar credenciales OAuth [ProfesorDao.java (Líneas 61-77, 82-95)](./src/main/java/ctn/informatica/sca/dao/ProfesorDao.java#L61-L95)
- **Extracción de Columnas Google OAuth**: Mapeo de columnas Google en método `map()` de ProfesorDao [ProfesorDao.java (Líneas 22-27)](./src/main/java/ctn/informatica/sca/dao/ProfesorDao.java#L22-27)
- **Preservación de Sesión en Flujo OAuth**: GoogleCallbackServlet preserva la sesión de usuario existente durante el flujo OAuth [GoogleCallbackServlet.java (Líneas 90-110)](./src/main/java/ctn/informatica/sca/GoogleCallbackServlet.java#L90-L110)

### Modificado
- **Resolución Dinámica de Redirect URI**: GoogleLoginServlet ahora resuelve dinámicamente el redirect_uri desde AppConfig o construcción basada en request, eliminando placeholder hardcodeado [GoogleLoginServlet.java (Líneas 52-61)](./src/main/java/ctn/informatica/sca/GoogleLoginServlet.java#L52-L61)
- **Redirects de Callback OAuth**: GoogleCallbackServlet redirige a rutas válidas de la aplicación (ProfileServlet) en lugar de endpoints no-existentes [GoogleCallbackServlet.java (Línea 41)](./src/main/java/ctn/informatica/sca/GoogleCallbackServlet.java#L41)
- **API de Autenticación Google**: Implementada autenticación OAuth2 usando GoogleCredential con access token para obtener información de usuario [GoogleCallbackServlet.java (Líneas 71-77)](./src/main/java/ctn/informatica/sca/GoogleCallbackServlet.java#L71-77)
- **Consultas SELECT en ProfesorDao**: Todas las queries incluyen las columnas Google OAuth en SELECT [ProfesorDao.java (Líneas 42-44)](./src/main/java/ctn/informatica/sca/dao/ProfesorDao.java#L42-44)
- **Cambio en CHANGELOG.md**: Reformatado de CHANGELOG.md en español.

### Eliminado
- Placeholder redirect_uri hardcodeado en GoogleLoginServlet
- Inicialización manual de `HttpCredentialsAdapter` que no está disponible en dependencias actuales

## [v0.0.1] - 2026-06-29

### Añadido
- Implementada la integración de autenticación con Google (GoogleAuth).

### Modificado
- Renombrado el paquete principal.
- Excluidos datos personales sensibles del repositorio Git.
- Eliminado el archivo/registro de `dbinsert`.
- Ajustado el flujo de autenticación para usar redirect URI dinámico y rutas válidas en la aplicación.

## Créditos
Proyecto desarrollado por:
[@provingch] https://github.com/provingch
[@Sh1b0] https://github.com/Sh1b0

Inicio del desarrollo: 18 de junio de 2026.
Propuesta aceptada: 29 de junio de 2026.
