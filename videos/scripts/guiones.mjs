// Guiones de los videos tutoriales: por perfil, una lista de tramos.
// preparar(h): llega al punto de partida (no se ve en el video). accion(h): lo que se ve. texto: lo que se narra.
const ESP = '?especialidadId=5';
// el aviso aparece solo porque la grabación bloquea la sincronización real con Classroom
const sinAvisoClassroom = page => page.evaluate(() => {
  for (const el of document.querySelectorAll('main *')) if (el.children.length < 4 && /No se pudo sincronizar Classroom/.test(el.textContent)) { el.closest('[role=alert],[role=status],div')?.remove(); break; }
});

export const guiones = {
  profesor: [
    {
      id: '01-inicio',
      texto: 'Al entrar al sistema llegás a tu panel de inicio. Desde acá accedés al Libro de Cátedra, para iniciar clases y cargar tu plan curricular, y a la gestión de planillas, donde cargás tareas y notas. Las etiquetas te avisan lo que tenés pendiente, y más abajo están todas tus materias.',
      preparar: h => h.go('/home' + ESP),
      async accion(h) {
        await h.apuntar(h.btn('Libro de Cátedra')); await h.pausa(1500);
        await h.apuntar(h.btn('Gestionar planillas')); await h.pausa(1500);
        await h.bajar(450); await h.pausa(1500);
      },
    },
    {
      id: '02-iniciar-clase',
      texto: 'Para registrar una clase, entrá a Iniciar clase. Si la clase no está en tu horario de hoy, tocá Iniciar clase distinta y elegí el curso y la sección. Completá el horario, las horas cátedra, la modalidad, el tipo de clase y el tema que desarrollaste. Para la asistencia, tocá el nombre de cada alumno ausente. Al final, tocá Guardar inicio de clase.',
      preparar: h => h.go('/home' + ESP + '&view=catedra&subview=clase'),
      async accion(h) {
        await h.clic(h.btn('Iniciar clase distinta'));
        await h.elegir('Curso', '3°');
        await h.elegir('Sección', 'A');
        await h.clic(h.btn('Inicio de clase'), 500);
        await h.clic(h.page.getByRole('option').filter({ visible: true }).first(), 600);
        await h.escribir(h.role('spinbutton', 'Horas cátedra'), '2');
        await h.elegir('Instrumento', 'Trabajos en clase');
        await h.escribir(h.role('textbox', 'Contenido específico desarrollado'), 'Estructuras repetitivas: while y do-while');
        await h.bajar(380);
        await h.clic(h.page.getByRole('button', { name: /, presente$/ }).nth(4), 700);
        await h.bajar(600);
        await h.apuntar(h.page.getByRole('button', { name: /Guardar inicio de clase/ })); await h.pausa(1500);
      },
    },
    {
      id: '03-plan-curricular',
      texto: 'En la pestaña Plan curricular descargás la plantilla de tu asignación: elegí curso, sección, materia y etapa, y tocá Configurar y descargar para indicar los meses y los bloques. Después la completás y la subís desde Subir plan. En Entregas realizadas ves si cada plan está pendiente, aprobado o rechazado.',
      preparar: h => h.go('/home' + ESP + '&view=catedra&subview=plan-curricular'),
      async accion(h) {
        await h.elegir('Curso de Informática', '3º');
        await h.elegir('Sección de Informática', 'A');
        await h.elegir('Materia de Informática', 'Algorítmica');
        await h.clic(h.btn('Configurar y descargar'), 1800);
        await h.page.keyboard.press('Escape'); await h.pausa(600);
        await h.apuntar(h.page.getByRole('button', { name: /^Arrastrá tu plan curricular/ })); await h.pausa(1200);
        await h.apuntar(h.page.getByRole('heading', { name: 'Entregas realizadas' })); await h.pausa(2000);
      },
    },
    {
      id: '04-clases-dadas',
      texto: 'En Clases dadas tenés el historial de todas tus clases. Tocá una para ver el detalle con la asistencia de cada alumno. Si un alumno faltó con motivo, tocá Justificar en su fila. Durante los siete días posteriores a la clase también podés editarla.',
      preparar: h => h.go('/home' + ESP + '&view=catedra&subview=mis-clases'),
      async accion(h) {
        await h.clic(h.page.getByRole('button', { name: /^Ver clase:.*Estructuras condicionales: if/ }), 1500);
        await h.apuntar(h.page.getByRole('button', { name: /^Justificar ausencia/ }).first()); await h.pausa(2000);
        await h.page.keyboard.press('Escape'); await h.pausa(600);
      },
    },
    {
      id: '05-planilla',
      texto: 'En Gestionar planillas abrís la planilla de cada materia. Ahí ves a tus alumnos, las tareas con su puntaje y la nota final. Para cargar una nota, escribila en la celda y se guarda sola. Con Sincronizar Classroom traés las tareas de Google Classroom, y con Descargar bajás la planilla en Excel.',
      preparar: async h => { await h.go('/planilla/3'); await sinAvisoClassroom(h.page); },
      async accion(h) {
        await h.apuntar(h.btn('Etapa')); await h.pausa(900);
        await h.apuntar(h.btn('Sincronizar Classroom')); await h.pausa(900);
        await h.apuntar(h.btn('Descargar')); await h.pausa(900);
        await h.bajar(380);
        await h.apuntar(h.page.getByRole('spinbutton').first()); await h.pausa(1500);
      },
    },
    {
      id: '06-firma',
      texto: 'Por último, en Mi perfil cargás tu firma: dibujala en el recuadro o subí una imagen, y tocá Guardar cambios. El sistema la agrega sola al pie de las planillas que descargás en Excel.',
      preparar: h => h.go('/profile'),
      async accion(h) {
        const canvas = h.page.getByRole('heading', { name: 'Firma del docente' }).locator('xpath=ancestor::section[1]').locator('canvas');
        await h.apuntar(canvas);
        const b = await canvas.boundingBox();
        await h.page.mouse.move(b.x + b.width * .15, b.y + b.height * .62, { steps: 10 });
        await h.page.mouse.down();
        for (let t = 0; t <= 1; t += .02) await h.page.mouse.move(b.x + b.width * (.15 + .7 * t), b.y + b.height * (.55 + .22 * Math.sin(t * 14) * (1 - t * .5)), { steps: 2 });
        await h.page.mouse.up();
        await h.pausa(900);
        await h.apuntar(h.btn('Guardar cambios')); await h.pausa(1500);
      },
    },
  ],

  evaluador: [
    {
      id: '01-inicio',
      texto: 'Como evaluador, tu panel tiene tres apartados: Ver planillas, para consultar y descargar las notas de cada curso; Revisar plan curricular, para aprobar o rechazar los planes de los profesores; y Seguimiento de profesores, donde controlás el cumplimiento y resolvés los incumplimientos.',
      preparar: h => h.go('/evaluacion'),
      async accion(h) {
        for (const t of ['Ver planillas', 'Revisar plan curricular', 'Seguimiento de profesores']) { await h.apuntar(h.btn(t)); await h.pausa(1300); }
      },
    },
    {
      id: '02-planillas',
      texto: 'En Ver planillas elegí la especialidad, el curso y la sección. Aparecen todas las planillas del curso, con el profesor y el estado de la etapa. Podés abrir cualquiera en modo solo lectura, descargarla en Excel o, si la etapa está cerrada y el profesor necesita corregir algo, reabrirla escribiendo el motivo.',
      preparar: async h => { await h.go('/evaluacion'); await h.page.getByRole('button', { name: 'Ver planillas', exact: true }).first().click(); await h.limpio(); },
      async accion(h) {
        await h.elegir('Especialidad', 'Informática');
        await h.elegir('Curso', '3°');
        await h.elegir('Sección', 'A');
        await h.clic(h.btn('Ver planilla de Algorítmica'), 1500);
        await h.apuntar(h.btn('Descargar esta planilla')); await h.pausa(900);
        await h.clic(h.btn('Reabrir Etapa 1'), 800);
        await h.escribir(h.page.getByRole('dialog').filter({ hasText: 'Motivo de la reapertura' }).getByRole('textbox'), 'La profesora debe corregir una nota cargada por error.');
        await h.apuntar(h.page.getByRole('dialog').filter({ hasText: 'Motivo de la reapertura' }).getByRole('button', { name: 'Reabrir Etapa 1' })); await h.pausa(1500);
        await h.page.keyboard.press('Escape'); await h.pausa(500);
      },
    },
    {
      id: '03-planes',
      texto: 'En Revisar plan curricular aparecen los planes pendientes. Abrí uno para ver los temas que cargó el profesor, mes por mes, o descargá el archivo original. Si está bien, tocá Aprobar. Si hay algo para corregir, escribí las observaciones y tocá Rechazar. El profesor recibe una notificación con tu decisión.',
      preparar: async h => { await h.go('/evaluacion'); await h.page.getByRole('button', { name: 'Revisar plan curricular' }).first().click(); await h.limpio(); },
      async accion(h) {
        await h.clic(h.page.getByRole('button', { name: /^Algorítmica Graciela/ }).first(), 1200);
        await h.clic(h.page.getByRole('button', { name: /^Agosto/ }).first(), 1000);
        await h.escribir(h.role('textbox', 'Observaciones'), 'Falta detallar las actividades de octubre.');
        await h.apuntar(h.btn('Aprobar')); await h.pausa(800);
        await h.apuntar(h.btn('Rechazar')); await h.pausa(1500);
      },
    },
    {
      id: '04-seguimiento',
      texto: 'En Seguimiento de profesores ves, para cada plan aprobado, qué temas ya se dieron y cuáles están pendientes. En la pestaña Incumplimientos aparecen los casos para resolver: bloqueos de iniciar clase, atrasos justificados e incongruencias. Abrí un caso, leé la justificación del profesor y aceptala o rechazala.',
      preparar: async h => { await h.go('/evaluacion'); await h.page.getByRole('button', { name: 'Seguimiento de profesores' }).first().click(); await h.limpio(); },
      async accion(h) {
        await h.clic(h.page.getByRole('button', { name: /^Algorítmica Graciela/ }).first(), 1800);
        await h.clic(h.page.getByRole('button', { name: /^Incumplimientos/ }).first(), 1200);
        await h.clic(h.page.getByRole('region', { name: 'Atrasos pendientes de revisión' }).getByRole('button').first(), 1500);
        await h.apuntar(h.btn('Aceptar')); await h.pausa(800);
        await h.apuntar(h.btn('Rechazar')); await h.pausa(1200);
        await h.clic(h.page.getByRole('region', { name: 'Bloqueos de Iniciar clase' }).getByRole('button').first(), 1200);
        await h.escribir(h.page.getByRole('textbox').filter({ visible: true }).last(), 'Se conversó con la profesora y se reprogramaron los temas.');
        await h.apuntar(h.btn('Reactivar Iniciar clase')); await h.pausa(1500);
      },
    },
  ],
  coordinacion: [
    {
      id: '01-inicio',
      texto: 'Coordinación Pedagógica tiene dos apartados: las quejas, que carga Administración sobre los profesores o los profesores sobre un curso, y los reportes conductuales, el catálogo de códigos que se usan al tomar asistencia.',
      preparar: h => h.go('/coordinacion'),
      async accion(h) { await h.apuntar(h.btn('Quejas por profesor')); await h.pausa(1500); await h.apuntar(h.btn('Reportes conductuales')); await h.pausa(1500); },
    },
    {
      id: '02-quejas',
      texto: 'En Quejas elegí si ver las quejas contra profesores o las quejas sobre cursos, y tocá un profesor para ver todas las suyas, ordenadas por estado. Una queja pendiente se acepta o se rechaza; si la rechazás, tenés que escribir el motivo.',
      preparar: async h => { await h.go('/coordinacion'); await h.page.getByRole('button', { name: 'Quejas por profesor' }).first().click(); await h.limpio(); },
      async accion(h) {
        await h.clic(h.btn('Sobre cursos'), 1500);
        await h.clic(h.btn('Contra profesores'), 1000);
        await h.clic(h.page.getByRole('main').getByRole('button', { name: /Graciela López/ }).first(), 1500);
        const pendiente = h.page.getByRole('listitem').filter({ hasText: 'Los alumnos informan' }).first();
        await h.clic(pendiente.getByRole('button', { name: 'Rechazar' }), 800);
        await h.escribir(pendiente.getByRole('textbox'), 'La situación ya se resolvió en la reunión de padres.');
        await h.apuntar(pendiente.getByRole('button', { name: 'Confirmar rechazo' })); await h.pausa(1200);
        await h.clic(pendiente.getByRole('button', { name: 'Cancelar' }), 800);
        await h.apuntar(pendiente.getByRole('button', { name: 'Aceptar' })); await h.pausa(1200);
      },
    },
    {
      id: '03-resolver',
      texto: 'Una queja revisada se cierra registrando la solución: qué se revisó, qué solución se aplicó y quién corrigió la situación. Al marcarla como resuelta, queda disponible el reporte de solución en PDF.',
      preparar: async h => { await h.go('/coordinacion'); await h.page.getByRole('button', { name: 'Quejas por profesor' }).first().click(); await h.limpio(); await h.page.getByRole('main').getByRole('button', { name: /Graciela López/ }).first().click(); await h.limpio(); },
      async accion(h) {
        const revisada = h.page.getByRole('listitem').filter({ hasText: 'Los padres reclaman' }).first();
        await h.clic(revisada.getByRole('button', { name: 'Registrar solución' }), 800);
        const campos = revisada.getByRole('textbox');
        await h.escribir(campos.nth(0), 'Se revisó el calendario de pruebas con la profesora.');
        await h.escribir(campos.nth(1), 'Las pruebas se anuncian también en el sistema con una semana de anticipación.');
        await h.escribir(campos.nth(2), 'Graciela López');
        await h.apuntar(revisada.getByRole('button', { name: 'Marcar como resuelta' })); await h.pausa(1500);
      },
    },
    {
      id: '04-conducta',
      texto: 'En Reportes conductuales creás los códigos: escribí el código, que empieza con N, y su descripción, y tocá Agregar código. Un código que ya no se usa se desactiva; los registros que ya tenía no se borran.',
      preparar: async h => { await h.go('/coordinacion'); await h.page.getByRole('button', { name: 'Reportes conductuales' }).first().click(); await h.limpio(); },
      async accion(h) {
        await h.escribir(h.role('textbox', 'Código'), 'N9');
        await h.escribir(h.role('textbox', 'Descripción'), 'Usa el celular en clase sin autorización');
        await h.apuntar(h.btn('Agregar código')); await h.pausa(1000);
        await h.apuntar(h.btn('Desactivar')); await h.pausa(1300);
      },
    },
  ],
  padres: [
    {
      id: '01-resumen',
      texto: 'Al entrar ves a tu hijo o hija con su promedio general. Si tenés más de un hijo en el colegio, elegí cuál ver en las tarjetas de arriba. Desde el resumen podés descargar el reporte del mes en PDF o imprimirlo.',
      preparar: h => h.go('/padre'),
      async accion(h) {
        await h.apuntar(h.page.getByRole('button', { name: /ESTIGARRIBIA DELGADILLO/ }).first()); await h.pausa(1200);
        await h.clic(h.btn('Mes del reporte'), 900); await h.page.keyboard.press('Escape'); await h.pausa(400);
        await h.apuntar(h.btn('Descargar reporte mensual')); await h.pausa(1200);
      },
    },
    {
      id: '02-materias',
      texto: 'Más abajo elegís la etapa y la materia. En el detalle aparecen todas las tareas, agrupadas por mes, con su estado y los puntos que obtuvo sobre el total.',
      preparar: h => h.go('/padre'),
      async accion(h) {
        await h.clic(h.btn('Primera etapa'), 1200);
        await h.clic(h.page.getByRole('button', { name: /^Materia Algorítmica/ }), 1200);
        await h.bajar(450); await h.pausa(1500);
      },
    },
    {
      id: '03-conducta',
      texto: 'Al final están las notas de conducta que los profesores registraron durante las clases, con la fecha, la materia, el código y su descripción. Podés filtrarlas por materia y por fechas.',
      preparar: async h => { await h.go('/padre'); await h.page.getByRole('region', { name: 'Notas de conducta', exact: true }).scrollIntoViewIfNeeded(); await h.limpio(); },
      async accion(h) { await h.clic(h.btn('Materia de la nota de conducta'), 900); await h.page.keyboard.press('Escape'); await h.apuntar(h.page.getByRole('region', { name: 'Notas de conducta', exact: true }).getByRole('table')); await h.pausa(1800); },
    },
  ],
  administrador: [
    {
      id: '01-inicio',
      texto: 'El panel de Administración tiene un módulo para cada tarea: materias, usuarios, asignaciones, alumnos, horarios, el estado del sistema, salas, quejas y clases dadas. Un administrador de especialidad ve solo los módulos de su especialidad.',
      preparar: h => h.go('/admin'),
      async accion(h) { for (const m of ['Materias', 'Usuarios', 'Asignaciones', 'Alumnos', 'Horarios']) { await h.apuntar(h.role('link', m)); await h.pausa(700); } },
    },
    {
      id: '02-usuarios',
      texto: 'En Usuarios filtrás por rol y buscás por nombre, cédula o usuario. Con Crear registro cargás una cuenta nueva. Para un padre, abrilo con Editar y vinculá a sus hijos desde el buscador de alumnos.',
      preparar: h => h.go('/admin/usuarios'),
      async accion(h) {
        await h.elegir('Filtrar por rol', 'Padres');
        await h.escribir(h.role('searchbox', 'Buscar'), 'Estigarribia');
        await h.clic(h.btn('Editar'), 1500);
        await h.apuntar(h.page.getByText('Alumno/s vinculados')); await h.pausa(1500);
        await h.page.keyboard.press('Escape'); await h.pausa(400);
      },
    },
    {
      id: '03-asignaciones',
      texto: 'En Asignaciones ves cuántas materias tiene cada profesor. Entrá al detalle de uno para crear una asignación: elegís la materia y marcás los cursos y secciones donde la dicta.',
      preparar: h => h.go('/admin/asignaciones'),
      async accion(h) {
        await h.escribir(h.role('searchbox', 'Buscar profesor'), 'Graciela');
        await h.clic(h.page.getByRole('button', { name: /^Ver detalle de López Molinas/ }).first(), 1500);
        await h.apuntar(h.page.getByRole('main').getByRole('button', { name: /Crear asignación/ }).first()); await h.pausa(1500);
      },
    },
    {
      id: '04-alumnos-horarios',
      texto: 'En Alumnos navegás por especialidad, curso y sección para ver, agregar o editar alumnos. En Horarios hacés lo mismo para ver el horario semanal de cada sección, cargarlo desde un Excel o descargarlo en PDF.',
      preparar: h => h.go('/admin/alumnos'),
      async accion(h) {
        const esp = h.page.getByRole('main').getByRole('button', { name: /^Especialidad Especialidad Informática/ }).first();
        await h.clic(esp, 1200);
        await h.clic(h.page.getByRole('main').getByRole('button', { name: /^Curso 3°/ }), 1200);
        await h.clic(h.page.getByRole('main').getByRole('link', { name: /^Sección A/ }).or(h.page.getByRole('main').getByRole('button', { name: /^Sección A/ })).first(), 1500);
        await h.apuntar(h.page.getByRole('main').getByRole('button', { name: /Agregar alumno/ }).first()); await h.pausa(1000);
        await h.go('/admin/horarios');
        await h.clic(h.page.getByRole('main').getByRole('button', { name: /^Especialidad Especialidad Informática/ }).first(), 1000);
        await h.clic(h.page.getByRole('main').getByRole('button', { name: /^Curso 3°/ }), 1000);
        await h.clic(h.page.getByRole('main').getByRole('link', { name: /^Sección A/ }), 2000);
      },
    },
    {
      id: '05-quejas-clases',
      texto: 'En Quejas registrás una queja sobre un profesor: elegí el curso, el profesor y escribí el motivo. Coordinación Pedagógica se encarga de revisarla. Y en Clases dadas tenés el historial de todas las clases registradas por los profesores.',
      preparar: h => h.go('/admin/quejas'),
      async accion(h) {
        await h.elegir('Curso', 'Informática · 3° A');
        await h.clic(h.btn('Profesor'), 900); await h.page.keyboard.press('Escape'); await h.pausa(300);
        await h.escribir(h.role('textbox', 'Motivo'), 'Los alumnos informan que no se devolvieron las pruebas corregidas.');
        await h.apuntar(h.btn('Registrar queja')); await h.pausa(1000);
        await h.go('/admin/clases');
        await h.escribir(h.role('searchbox', 'Buscar clases'), 'Algorítmica'); await h.pausa(1500);
      },
    },
  ],
};
