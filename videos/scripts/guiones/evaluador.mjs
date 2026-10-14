// Tutorial del evaluador. Datos de ejemplo: plan pendiente de 3°B, plan aprobado de 3°A, tres incumplimientos
// (atraso, incongruencia retroactiva y bloqueo) y la planilla 3 (Algorítmica 3°A) con la Etapa 1 cerrada.
const abrir = async (h, tarjeta) => { await h.go('/evaluacion'); await h.clic(h.btn(tarjeta), 1200); };
const caso = (h, region) => h.page.getByRole('region', { name: region }).getByRole('button').first();

export const escenas = [
  {
    id: 'inicio', titulo: 'Panel de evaluación',
    preparar: h => h.go('/evaluacion'),
    pasos: [
      { voz: 'Este es el tutorial del evaluador. Tu panel tiene tres apartados.', hacer: h => h.pausa(400) },
      { voz: 'Ver planillas, para consultar y descargar las notas de cada curso.', hacer: h => h.apuntar(h.btn('Ver planillas')) },
      { voz: 'Revisar plan curricular, para aprobar o rechazar los planes que suben los profesores.', hacer: h => h.apuntar(h.btn('Revisar plan curricular')) },
      { voz: 'Y Seguimiento de profesores, donde controlás que se cumplan los planes y resolvés los incumplimientos.', hacer: h => h.apuntar(h.btn('Seguimiento de profesores')) },
    ],
  },
  {
    id: 'planillas', titulo: 'Ver y descargar planillas',
    preparar: h => abrir(h, 'Ver planillas'),
    pasos: [
      { voz: 'En Ver planillas elegí la especialidad, el curso y la sección. El botón de descarga queda gris hasta que elegís curso y sección.', hacer: async h => { await h.apuntar(h.btn('Descargar planillas')); await h.elegir('Especialidad', 'Informática'); await h.elegir('Curso', '3°'); await h.elegir('Sección', 'A'); } },
      { voz: 'Aparecen todas las planillas del curso, con el profesor y el estado de la etapa: abierta o cerrada.', hacer: h => h.apuntar(h.vis(h.page.getByRole('table', { name: 'Planillas del curso seleccionado' }))) },
      { voz: 'Con Descargar planillas bajás todas las del curso en un solo Excel.', hacer: h => h.clic(h.btn('Descargar planillas'), 1800) },
      { voz: 'Tocá Ver planilla para abrir una en modo solo lectura: ves las notas por tarea, el total y la nota final, pero no podés cambiar nada.', hacer: h => h.clic(h.btn('Ver planilla de Algorítmica'), 1800) },
    ],
  },
  {
    id: 'reabrir', titulo: 'Reabrir una etapa cerrada',
    preparar: async h => { await abrir(h, 'Ver planillas'); await h.elegir('Especialidad', 'Informática'); await h.elegir('Curso', '3°'); await h.elegir('Sección', 'A'); await h.clic(h.btn('Ver planilla de Algorítmica'), 1500); },
    pasos: [
      { voz: 'Si una etapa está cerrada y el profesor necesita corregir algo, vos podés reabrirla con el botón Reabrir etapa.', hacer: h => h.clic(h.btn('Reabrir Etapa 1'), 900) },
      { voz: 'El motivo es obligatorio: sin motivo no se puede confirmar. Escribilo claro, porque el profesor lo recibe en su notificación.', hacer: async h => { const d = h.page.getByRole('dialog').filter({ hasText: 'Motivo de la reapertura' }); await h.escribir(d.getByRole('textbox'), 'La profesora tiene que corregir la nota de un alumno que entregó con justificación.'); } },
      { voz: 'Al confirmar, el profesor vuelve a poder editar las notas y las tareas de esa etapa.', hacer: async h => { const d = h.page.getByRole('dialog').filter({ hasText: 'Motivo de la reapertura' }); await h.clic(d.getByRole('button', { name: 'Reabrir Etapa 1' }), 2200); } },
    ],
  },
  {
    id: 'planes', titulo: 'Revisar planes curriculares',
    preparar: h => abrir(h, 'Revisar plan curricular'),
    pasos: [
      { voz: 'En Revisar plan curricular están los planes pendientes. Podés agruparlos por especialidad o por profesor.', hacer: h => h.apuntar(h.btn('Profesor')) },
      { voz: 'Abrí uno para ver los temas que cargó el profesor, mes por mes. También podés descargar el archivo original.', hacer: async h => { await h.clic(h.vis(h.page.getByRole('button', { name: /^Algorítmica Graciela/ })), 1500); await h.clic(h.vis(h.page.getByRole('button', { name: /^Agosto/ })), 1200); } },
      { voz: 'Fijate que Rechazar está deshabilitado: para rechazar un plan es obligatorio escribir las observaciones.', hacer: async h => { await h.bajar(500); await h.apuntar(h.btn('Rechazar')); } },
      { voz: 'Escribí qué tiene que corregir el profesor y tocá Rechazar. Él recibe una notificación con tus observaciones y puede volver a subirlo.', hacer: async h => { await h.escribir(h.role('textbox', 'Observaciones'), 'Faltan las actividades de octubre y noviembre. Completalas y volvé a subir el plan.'); await h.clic(h.btn('Rechazar'), 900); const c = h.vis(h.page.getByRole('dialog').getByRole('button', { name: /Rechazar|Confirmar/ }).last()); if (await c.count()) await h.clic(c, 2000); } },
      { voz: 'Si el plan está bien, tocá Aprobar y confirmá. Desde ese momento empieza el seguimiento de sus temas.', hacer: h => h.pausa(400) },
    ],
  },
  {
    id: 'seguimiento', titulo: 'Seguimiento de planes',
    preparar: h => abrir(h, 'Seguimiento de profesores'),
    pasos: [
      { voz: 'En Seguimiento de profesores elegí un plan aprobado para ver cada tema con la fecha en que se dio, o pendiente si todavía no llegó.', hacer: h => h.clic(h.vis(h.page.getByRole('button', { name: /^Algorítmica Graciela/ })), 1800) },
      { voz: 'Cuando la etapa cierra, los temas que no se dieron quedan como no cumplidos.', hacer: h => h.apuntar(h.vis(h.page.getByRole('table', { name: /Cumplimiento de temas/ }))) },
    ],
  },
  {
    id: 'incumplimientos', titulo: 'Incumplimientos',
    preparar: async h => { await abrir(h, 'Seguimiento de profesores'); await h.clic(h.vis(h.page.getByRole('button', { name: /^Incumplimientos/ })), 1500); },
    pasos: [
      { voz: 'En la pestaña Incumplimientos están todos los casos que esperan tu decisión, en tres grupos.', hacer: h => h.apuntar(h.vis(h.page.getByRole('heading', { name: /Casos pendientes/ }))) },
      { voz: 'Primero, los atrasos: el profesor justificó que un tema llegó tarde. Leé la fecha, el tema esperado y su justificación.', hacer: h => h.clic(caso(h, 'Atrasos pendientes de revisión'), 1500) },
      { voz: 'Si la aceptás, no cuenta como falta. Si la rechazás, suma una falta, y al llegar a tres en la misma materia se bloquea Iniciar clase.', hacer: h => h.clic(h.btn('Aceptar'), 2000) },
      { voz: 'Segundo, las incongruencias: clases dadas antes de aprobar el plan que no coinciden con lo planificado. Se resuelven igual.', hacer: async h => { await h.clic(caso(h, 'Incongruencias retroactivas'), 1500); await h.clic(h.btn('Rechazar'), 2000); } },
      { voz: 'Y tercero, los bloqueos: no se levantan solos. Frenan el trabajo del profesor, así que conviene resolverlos primero.', hacer: h => h.clic(caso(h, 'Bloqueos de Iniciar clase'), 1500) },
      { voz: 'Escribí la nota de reactivación, que es obligatoria, y tocá Reactivar Iniciar clase. El profesor recibe tu nota y puede volver a dar clases.', hacer: async h => { await h.escribir(h.vis(h.page.getByRole('textbox').last()), 'Se conversó con la profesora y se reprogramaron los temas atrasados.'); await h.clic(h.btn('Reactivar Iniciar clase'), 2200); } },
    ],
  },
];
