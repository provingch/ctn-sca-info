// Tutorial del administrador (cuenta global: ve todos los módulos). Crea registros de verdad en la base LOCAL.
const main = h => h.page.getByRole('main');
const dialogo = h => h.vis(h.page.getByRole('dialog'));
const campo = (h, etiqueta) => h.vis(dialogo(h).getByLabel(etiqueta));
const especialidad = (h, nombre) => h.vis(main(h).getByRole('button', { name: new RegExp(`^Especialidad Especialidad ${nombre}`) }));

export const escenas = [
  {
    id: 'inicio', titulo: 'Panel general',
    preparar: h => h.go('/admin'),
    pasos: [
      { voz: 'Este es el tutorial del administrador. El panel general tiene un módulo para cada tarea.', hacer: h => h.apuntar(h.vis(h.page.getByRole('region', { name: 'Gestión del sistema' }))) },
      { voz: 'El administrador global ve todos los módulos. Un administrador de especialidad ve solo alumnos, horarios, quejas y clases dadas de su especialidad.', hacer: async h => { for (const m of ['Usuarios', 'Materias', 'Asignaciones']) { await h.apuntar(h.role('link', m)); await h.pausa(300); } } },
    ],
  },
  {
    id: 'usuarios', titulo: 'Usuarios',
    preparar: h => h.go('/admin/usuarios'),
    pasos: [
      { voz: 'En Usuarios filtrás por rol y buscás por nombre, cédula o usuario.', hacer: async h => { await h.elegir('Filtrar por rol', 'Profesores'); await h.escribir(h.role('searchbox', 'Buscar'), 'López'); } },
      { voz: 'Para dar de alta una cuenta, tocá Crear registro y completá nombres, apellidos, cédula y usuario.', hacer: async h => { await h.clic(h.btn('Crear registro'), 900); await h.escribir(campo(h, 'Nombres'), 'Lucía'); await h.escribir(campo(h, 'Apellidos'), 'Benítez Ramos'); await h.escribir(campo(h, 'Cédula'), '5123456'); await h.escribir(campo(h, 'Nombre de usuario'), 'lucia.benitez'); } },
      { voz: 'Elegí el rol y tocá Crear usuario. El sistema le asigna una contraseña inicial.', hacer: async h => { await h.clic(h.vis(dialogo(h).getByRole('button', { name: /^Crear usuario/ })), 2200); } },
      { voz: 'Para un padre, abrilo con Editar y vinculá a sus hijos desde el buscador de alumnos: tocá Agregar junto a cada uno.', hacer: async h => { await h.elegir('Filtrar por rol', 'Padres'); await h.page.getByRole('searchbox', { name: 'Buscar' }).fill(''); await h.clic(h.btn('Editar'), 1300); await h.apuntar(h.vis(dialogo(h).getByText('Alumno/s vinculados'))); await h.page.keyboard.press('Escape'); } },
    ],
  },
  {
    id: 'materias', titulo: 'Materias',
    preparar: h => h.go('/admin/materias'),
    pasos: [
      { voz: 'En Materias está el catálogo completo. Podés buscar y filtrar por tipo y especialidad.', hacer: h => h.apuntar(h.btn('Filtrar por tipo')) },
      { voz: 'Una materia común se dicta en varias especialidades; una específica, en una sola.', hacer: async h => { await h.clic(h.btn('Crear materia'), 900); await h.escribir(h.vis(dialogo(h).getByRole('textbox').first()), 'Ética profesional'); await h.pausa(400); await h.page.keyboard.press('Escape'); } },
      { voz: 'Atención: una materia que tiene asignaciones no se puede eliminar. Primero hay que quitar esas asignaciones.', hacer: async h => { await h.escribir(h.role('searchbox', 'Buscar por nombre'), 'Algorítmica'); await h.clic(h.btn('Eliminar Algorítmica'), 900); const c = h.vis(h.page.getByRole('dialog').getByRole('button', { name: /Eliminar|Confirmar/ }).last()); if (await c.count()) await h.clic(c, 2200); } },
    ],
  },
  {
    id: 'asignaciones', titulo: 'Asignaciones',
    preparar: h => h.go('/admin/asignaciones'),
    pasos: [
      { voz: 'En Asignaciones definís qué materia dicta cada profesor y en qué cursos. La lista muestra cuántas asignaciones tiene cada uno.', hacer: async h => { await h.escribir(h.role('searchbox', 'Buscar profesor'), 'Graciela'); } },
      { voz: 'Entrá al detalle de un profesor y tocá Crear asignación: elegís la materia y marcás uno o varios cursos y secciones.', hacer: async h => { await h.clic(h.vis(h.page.getByRole('button', { name: /^Ver detalle de López Molinas/ })), 1500); await h.clic(h.vis(main(h).getByRole('button', { name: /Crear asignación/ })), 1200); await h.pausa(500); await h.page.keyboard.press('Escape'); } },
    ],
  },
  {
    id: 'alumnos', titulo: 'Alumnos',
    preparar: h => h.go('/admin/alumnos'),
    pasos: [
      { voz: 'En Alumnos navegás por especialidad, curso y sección.', hacer: async h => { await h.clic(especialidad(h, 'Informática'), 1200); await h.clic(h.vis(main(h).getByRole('button', { name: /^Curso 3°/ })), 1200); await h.clic(h.vis(main(h).getByRole('link', { name: /^Sección A/ }).or(main(h).getByRole('button', { name: /^Sección A/ }))), 1500); } },
      { voz: 'Con Agregar alumno cargás uno nuevo en esa sección. Con Editar corregís sus datos, lo cambiás de curso o le vinculás a sus padres.', hacer: async h => { await h.clic(h.vis(main(h).getByRole('button', { name: /Agregar alumno/ })), 1200); await h.pausa(500); await h.page.keyboard.press('Escape'); } },
    ],
  },
  {
    id: 'horarios', titulo: 'Horarios',
    preparar: h => h.go('/admin/horarios'),
    pasos: [
      { voz: 'En Horarios ves el horario semanal de cada sección. Se carga desde un Excel, con una vista previa que marca los conflictos antes de confirmar.', hacer: async h => { await h.clic(especialidad(h, 'Informática'), 1200); await h.clic(h.vis(main(h).getByRole('button', { name: /^Curso 3°/ })), 1200); await h.clic(h.vis(main(h).getByRole('link', { name: /^Sección A/ })), 2000); } },
      { voz: 'También podés agregar o quitar bloques a mano, y descargar el horario en PDF.', hacer: h => h.bajar(500) },
    ],
  },
  {
    id: 'salas', titulo: 'Salas',
    preparar: h => h.go('/admin/salas'),
    pasos: [
      { voz: 'En Salas está el catálogo de aulas comunes y de cada especialidad. Con Agregar sala cargás una nueva.', hacer: async h => { await h.clic(h.btn('＋ Agregar sala'), 1000); await h.pausa(500); await h.page.keyboard.press('Escape'); } },
      { voz: 'Una sala con bloques de horario asignados no se puede eliminar hasta reasignarlos.', hacer: h => h.apuntar(h.vis(main(h).getByRole('button', { name: /Comunes/ }))) },
    ],
  },
  {
    id: 'quejas', titulo: 'Quejas y clases dadas',
    preparar: h => h.go('/admin/quejas'),
    pasos: [
      { voz: 'En Quejas registrás una queja sobre un profesor: elegí el curso, el profesor y escribí el motivo con detalle.', hacer: async h => { await h.elegir('Curso', 'Informática · 3° A'); await h.elegir('Profesor', 'López'); await h.escribir(h.role('textbox', 'Motivo'), 'Los alumnos informan que no se devolvieron las pruebas corregidas del mes pasado.'); } },
      { voz: 'Al registrarla, Coordinación Pedagógica se encarga de revisarla. Desde acá ves el historial y en qué estado está cada una.', hacer: async h => { await h.clic(h.btn('Registrar queja'), 2200); await h.apuntar(h.vis(h.page.getByRole('region', { name: /^Quejas registradas/ }))); } },
      { voz: 'Y en Clases dadas tenés el historial de todas las clases registradas por los profesores, con la asistencia de cada una.', hacer: async h => { await h.go('/admin/clases'); await h.escribir(h.role('searchbox', 'Buscar clases'), 'Algorítmica'); } },
    ],
  },
];
