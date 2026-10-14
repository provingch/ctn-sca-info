// Tutorial de Coordinación Pedagógica. Datos de ejemplo: quejas contra Graciela (pendiente, revisada, rechazada) y
// contra Federico (aceptada, resuelta), más una queja de Graciela sobre 3°B.
const quejas = async h => { await h.go('/coordinacion'); await h.clic(h.btn('Quejas por profesor'), 1200); };
const profesor = async (h, nombre) => { await quejas(h); await h.clic(h.vis(h.page.getByRole('main').getByRole('button', { name: new RegExp(nombre) })), 1500); };
const queja = (h, texto) => h.vis(h.page.getByRole('listitem').filter({ hasText: texto }));

export const escenas = [
  {
    id: 'inicio', titulo: 'Panel de Coordinación',
    preparar: h => h.go('/coordinacion'),
    pasos: [
      { voz: 'Este es el tutorial de Coordinación Pedagógica. Tenés dos apartados: las quejas y los reportes conductuales.', hacer: h => h.apuntar(h.btn('Quejas por profesor')) },
      { voz: 'Las quejas las carga Administración sobre un profesor, o los profesores sobre un curso. Vos las revisás de principio a fin.', hacer: h => h.apuntar(h.btn('Reportes conductuales')) },
    ],
  },
  {
    id: 'lista', titulo: 'Quejas',
    preparar: quejas,
    pasos: [
      { voz: 'Arriba elegís qué ver: las quejas contra profesores o las quejas sobre cursos.', hacer: async h => { await h.clic(h.btn('Sobre cursos'), 1200); await h.clic(h.btn('Contra profesores'), 1000); } },
      { voz: 'Cada tarjeta muestra al profesor, su última queja y cuántas tiene. Tocá una para ver todas.', hacer: h => h.clic(h.vis(h.page.getByRole('main').getByRole('button', { name: /Graciela López/ })), 1500) },
      { voz: 'Las quejas están agrupadas por estado: pendientes, aceptadas, revisadas, resueltas y rechazadas. Podés filtrarlas con estos botones.', hacer: h => h.apuntar(h.vis(h.page.getByRole('group', { name: 'Filtrar quejas por estado' }))) },
    ],
  },
  {
    id: 'pendiente', titulo: 'Aceptar o rechazar',
    preparar: h => profesor(h, 'Graciela López'),
    pasos: [
      { voz: 'Una queja pendiente se acepta, si corresponde darle curso, o se rechaza.', hacer: h => h.apuntar(queja(h, 'Los alumnos informan').getByRole('button', { name: 'Aceptar' })) },
      { voz: 'Para rechazarla es obligatorio escribir el motivo: el botón Confirmar rechazo se habilita recién cuando lo completás.', hacer: async h => { const q = queja(h, 'Los alumnos informan'); await h.clic(q.getByRole('button', { name: 'Rechazar' }), 800); await h.apuntar(q.getByRole('button', { name: 'Confirmar rechazo' })); await h.escribir(q.getByRole('textbox'), 'La situación ya se resolvió en la reunión de padres.'); } },
      { voz: 'En este caso vamos a darle curso: cancelamos el rechazo y tocamos Aceptar.', hacer: async h => { const q = queja(h, 'Los alumnos informan'); await h.clic(q.getByRole('button', { name: 'Cancelar' }), 700); await h.clic(q.getByRole('button', { name: 'Aceptar' }), 2000); } },
    ],
  },
  {
    id: 'revisar', titulo: 'Revisar una queja',
    preparar: h => profesor(h, 'Federico Gónzalez'),
    pasos: [
      { voz: 'Cuando terminás de investigar una queja aceptada, tocá Revisar queja.', hacer: h => h.clic(queja(h, 'Inasistencias reiteradas').getByRole('button', { name: /Revisar/ }), 900) },
      { voz: 'Escribí la conclusión: qué se verificó y a qué se llegó. Al completar la revisión se registran la fecha y quién la hizo.', hacer: async h => { const q = queja(h, 'Inasistencias reiteradas'); await h.escribir(q.getByRole('textbox').first(), 'Se verificaron las planillas de asistencia del docente con la dirección. Se acordó un aviso previo obligatorio.'); await h.clic(q.getByRole('button', { name: 'Completar revisión' }), 2200); } },
    ],
  },
  {
    id: 'resolver', titulo: 'Registrar la solución',
    preparar: h => profesor(h, 'Federico Gónzalez'),
    pasos: [
      { voz: 'Una queja revisada se cierra registrando la solución. Tocá Registrar solución.', hacer: h => h.clic(queja(h, 'Inasistencias reiteradas').getByRole('button', { name: 'Registrar solución' }), 900) },
      { voz: 'Completá qué se revisó, la solución aplicada y el nombre de quien corrigió la situación.', hacer: async h => { const c = queja(h, 'Inasistencias reiteradas').getByRole('textbox'); await h.escribir(c.nth(0), 'Se revisaron las planillas de asistencia de las últimas dos semanas.'); await h.escribir(c.nth(1), 'El docente avisa sus ausencias con un día de anticipación.'); await h.escribir(c.nth(2), 'Federico González'); } },
      { voz: 'Tocá Marcar como resuelta. Queda disponible el reporte de solución en PDF.', hacer: async h => { const q = queja(h, 'Inasistencias reiteradas'); await h.clic(q.getByRole('button', { name: 'Marcar como resuelta' }), 2200); await h.apuntar(q.getByRole('button', { name: /reporte de solución/ })); } },
      { voz: 'Ojo: una queja resuelta o rechazada no se puede reabrir. Si hace falta revisar algo de nuevo, Administración tiene que cargar una queja nueva.', hacer: h => h.pausa(400) },
    ],
  },
  {
    id: 'cursos', titulo: 'Quejas sobre cursos',
    preparar: async h => { await quejas(h); await h.clic(h.btn('Sobre cursos'), 1200); },
    pasos: [
      { voz: 'Las quejas sobre cursos las cargan los profesores, por situaciones de un grupo. Se gestionan igual: abrí el curso y revisá la queja.', hacer: h => h.clic(h.vis(h.page.getByRole('main').getByRole('button', { name: /3° · B|3°.*B/ })), 1500) },
    ],
  },
  {
    id: 'conducta', titulo: 'Códigos de conducta',
    preparar: async h => { await h.go('/coordinacion'); await h.clic(h.btn('Reportes conductuales'), 1200); },
    pasos: [
      { voz: 'En Reportes conductuales administrás los códigos que usan los profesores al tomar asistencia.', hacer: h => h.apuntar(h.vis(h.page.getByRole('heading', { name: 'Catálogo de conducta' }))) },
      { voz: 'Para crear uno, escribí el código, que empieza con N, y su descripción, y tocá Agregar código.', hacer: async h => { await h.escribir(h.role('textbox', 'Código'), 'N9'); await h.escribir(h.role('textbox', 'Descripción'), 'Usa el celular en clase sin autorización'); await h.clic(h.btn('Agregar código'), 2000); } },
      { voz: 'Un código que ya no se usa se desactiva. Los registros que ya lo tenían no se borran.', hacer: async h => { await h.clic(h.btn('Desactivar'), 900); const c = h.vis(h.page.getByRole('dialog').getByRole('button', { name: /Desactivar|Confirmar|Aceptar/ }).last()); if (await c.count()) await h.clic(c, 1800); } },
    ],
  },
];
