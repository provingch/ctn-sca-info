// Tutorial del profesor: escenas con sus pasos. Cada paso = una frase narrada (voz) + lo que se hace mientras se dice.
// Pensado para el SCA local con los datos de manuales/datos-demo (Graciela López, Algorítmica 3°A/3°B).
const ESP = '?especialidadId=5';
const catedra = sub => `/home${ESP}&view=catedra&subview=${sub}`;

// abre "Iniciar clase distinta" para un curso/sección
async function claseDistinta(h, curso, seccion) {
  await h.clic(h.btn('Iniciar clase distinta'), 900);
  await h.elegir('Curso', curso);
  await h.elegir('Sección', seccion);
  await h.vis(h.page.getByRole('heading', { name: 'Registro de clase' })).waitFor({ timeout: 15000 });
}

export const escenas = [
  {
    id: 'inicio', titulo: 'Tu panel de inicio',
    preparar: h => h.go(`/home${ESP}`),
    pasos: [
      { voz: 'Este es el tutorial del profesor del Sistema de Carpeta Académica. Al entrar con tu usuario y contraseña llegás a tu panel de inicio.', hacer: h => h.pausa(500) },
      { voz: 'Desde el Libro de Cátedra iniciás tus clases y cargás tu plan curricular.', hacer: h => h.apuntar(h.btn('Libro de Cátedra')) },
      { voz: 'Desde Gestionar planillas cargás las tareas y las notas de tus alumnos.', hacer: h => h.apuntar(h.btn('Gestionar planillas')) },
      { voz: 'Más abajo están todas tus materias, con el estado de su plan curricular. Prestá atención a esas etiquetas: te avisan si un plan está pendiente o falta cargarlo.', hacer: h => h.bajar(420) },
      { voz: 'Y arriba a la derecha, la campana te muestra las notificaciones nuevas, por ejemplo cuando evaluación aprueba o rechaza tu plan.', hacer: async h => { await h.bajar(-600); await h.clic(h.vis(h.page.getByRole('button', { name: /^Notificaciones/ })), 1200); await h.page.keyboard.press('Escape'); } },
    ],
  },
  {
    id: 'clase', titulo: 'Registrar una clase',
    preparar: h => h.go(catedra('clase')),
    pasos: [
      { voz: 'Para registrar una clase, entrá a Iniciar clase. Arriba aparece tu horario de hoy.', hacer: h => h.apuntar(h.page.getByRole('heading', { name: 'Tu horario de hoy' })) },
      {
        voz: 'Tocá el bloque que vas a dar: la materia, el curso y el horario ya vienen completos.',
        hacer: async h => {
          const bloque = h.vis(h.page.getByRole('tabpanel', { name: 'Iniciar clase' }).getByRole('listitem').getByRole('button'));
          // si hoy no hay bloques en el horario, se usa una clase fuera de horario de 2° B
          if (await bloque.count()) await h.clic(bloque, 1500); else await claseDistinta(h, '2°', 'B');
        },
      },
      { voz: 'Si intentás guardar sin completar el tipo de clase y el tema, el sistema te avisa qué falta y no guarda nada.', hacer: async h => { await h.bajar(900); await h.clic(h.page.getByRole('button', { name: /Guardar inicio de clase/ }), 1800); } },
      { voz: 'Entonces elegí el tipo de clase y escribí el contenido que desarrollaste.', hacer: async h => { await h.bajar(-900); await h.elegir('Instrumento', 'Trabajos en clase'); await h.escribir(h.role('textbox', 'Contenido específico desarrollado'), 'Estructuras repetitivas: while y do-while'); } },
      { voz: 'Para la asistencia, tocá el nombre de cada alumno que faltó. El resumen de abajo se actualiza solo.', hacer: async h => { await h.bajar(380); const a = h.page.getByRole('button', { name: /, presente$/ }); await h.clic(a.nth(3), 500); await h.clic(a.nth(8), 500); } },
      { voz: 'Si un alumno tuvo un problema de conducta, tocá su zona de rasgos y elegí el código que corresponde.', hacer: async h => { await h.clic(h.page.getByRole('button', { name: /^Rasgos conductuales de/ }).nth(5), 800); await h.clic(h.vis(h.page.getByRole('listbox', { name: /^Códigos para/ }).getByRole('option', { name: /^N6/ })), 700); await h.page.keyboard.press('Escape'); } },
      { voz: 'Al final, tocá Guardar inicio de clase. El sistema confirma que la clase quedó registrada.', hacer: async h => { await h.bajar(900); await h.clic(h.page.getByRole('button', { name: /Guardar inicio de clase/ }), 2000); } },
    ],
  },
  {
    id: 'bloqueo', titulo: 'Clase fuera de horario y bloqueos',
    preparar: h => h.go(catedra('clase')),
    pasos: [
      { voz: 'Si la clase no está en tu horario, por ejemplo una recuperación, tocá Iniciar clase distinta y elegí el curso y la sección.', hacer: h => claseDistinta(h, '3°', 'B') },
      { voz: 'Completá los datos como siempre: horario, horas cátedra, tipo de clase y tema.', hacer: async h => { await h.elegir('Inicio de clase', '7:00'); await h.escribir(h.role('spinbutton', 'Horas cátedra'), '2'); await h.elegir('Instrumento', 'Trabajos en clase'); await h.escribir(h.role('textbox', 'Contenido específico desarrollado'), 'Repaso general'); } },
      { voz: 'Atención con este caso: cuando en una materia se acumulan atrasos rechazados por evaluación, el sistema bloquea el inicio de clases en ella.', hacer: async h => { await h.bajar(1400); await h.clic(h.page.getByRole('button', { name: /Guardar inicio de clase/ }), 2200); } },
      { voz: 'No es un error del formulario. Comunicate con evaluación: cuando revisen tu caso y reactiven la materia, vas a poder registrar clases de nuevo.', hacer: h => h.pausa(500) },
    ],
  },
  {
    id: 'atraso', titulo: 'Tema atrasado',
    preparar: h => h.go(catedra('clase')),
    pasos: [
      { voz: 'Otro caso importante: el tema atrasado. Si el tema que cargás quedó atrás de lo que marca tu plan curricular, el sistema te lo avisa al guardar.', hacer: async h => { await claseDistinta(h, '3°', 'A'); await h.elegir('Inicio de clase', '7:00'); await h.escribir(h.role('spinbutton', 'Horas cátedra'), '2'); await h.elegir('Instrumento', 'Trabajos en clase'); await h.escribir(h.role('textbox', 'Contenido específico desarrollado'), 'Repaso de estructuras condicionales'); await h.bajar(1400); await h.clic(h.page.getByRole('button', { name: /Guardar inicio de clase/ }), 2000); } },
      { voz: 'Escribí el motivo del atraso en Justificación del atraso y volvé a tocar Guardar inicio de clase.', hacer: async h => { const j = h.vis(h.page.getByRole('textbox', { name: /Justificación del atraso/ })); await h.escribir(j, 'Se suspendieron dos clases por el acto del aniversario.'); await h.clic(h.page.getByRole('button', { name: /Guardar inicio de clase/ }), 2000); } },
      { voz: 'Ojo: justificar no lo resuelve automáticamente. Tu justificación queda pendiente y evaluación la acepta o la rechaza. Si se rechazan varias, la materia se bloquea.', hacer: h => h.pausa(500) },
    ],
  },
  {
    id: 'plan', titulo: 'Plan curricular',
    preparar: h => h.go(catedra('plan-curricular')),
    pasos: [
      { voz: 'En la pestaña Plan curricular descargás la plantilla de cada asignación: elegí curso, sección, materia y etapa.', hacer: async h => { await h.elegir('Curso de Informática', '3º'); await h.elegir('Sección de Informática', 'A'); await h.elegir('Materia de Informática', 'Algorítmica'); } },
      { voz: 'Tocá Configurar y descargar, marcá los meses que vas a completar y cuántos bloques necesitás en cada uno.', hacer: async h => { await h.clic(h.btn('Configurar y descargar'), 1500); await h.page.keyboard.press('Escape'); } },
      { voz: 'Completá la plantilla y subila desde Subir plan. Si el archivo no es la plantilla o le faltan datos, el sistema te lo indica y no la registra.', hacer: async h => { await h.apuntar(h.page.getByRole('button', { name: /^Arrastrá tu plan curricular/ })); await h.page.locator('input[type=file]').first().setInputFiles(new URL('../../../backend/src/main/resources/plan-curricular-plantilla-etapa2.xlsx', import.meta.url).pathname); await h.pausa(600); await h.clic(h.btn('Subir plan curricular'), 2200); } },
      { voz: 'En Entregas realizadas ves cada plan con su estado: pendiente mientras evaluación lo revisa, aprobado o rechazado con sus observaciones.', hacer: h => h.apuntar(h.page.getByRole('heading', { name: 'Entregas realizadas' })) },
      { voz: 'Tocá un plan aprobado para ver sus temas mes por mes: los cubiertos, con la fecha en que se dieron, y los pendientes.', hacer: async h => { await h.clic(h.btn('Ver detalle de plan-algoritmica-3A-etapa2.xlsx'), 1500); await h.clic(h.vis(h.page.getByRole('dialog').getByRole('button', { name: /^Septiembre/ })), 1500); } },
      { voz: 'Si un tema queda sin dar cuando la etapa se cierra, figura como no cumplido.', hacer: async h => { await h.pausa(400); await h.page.keyboard.press('Escape'); } },
    ],
  },
  {
    id: 'incongruencias', titulo: 'Clases antes del plan',
    preparar: h => h.go(catedra('plan-curricular')),
    pasos: [
      { voz: 'Si diste clases antes de que aprobaran tu plan, al aprobarse el sistema las compara con lo planificado. Las que no coinciden aparecen en este panel.', hacer: h => h.apuntar(h.page.getByRole('region', { name: /Clases previas al plan con incongruencias/ })) },
      { voz: 'Podés escribir por qué la clase no siguió el plan y tocar Actualizar justificación. Evaluación la ve al revisar el caso.', hacer: async h => { const r = h.page.getByRole('region', { name: /Clases previas al plan con incongruencias/ }); await h.escribir(r.getByRole('textbox').first(), 'El grupo necesitaba reforzar los diagramas antes del repaso.'); await h.clic(r.getByRole('button', { name: /Actualizar justificación/ }), 1800); } },
    ],
  },
  {
    id: 'clases-dadas', titulo: 'Clases dadas',
    preparar: h => h.go(catedra('mis-clases')),
    pasos: [
      { voz: 'En Clases dadas tenés el historial de todas tus clases. Podés buscarlas y filtrarlas por curso.', hacer: h => h.apuntar(h.btn('Filtrar por curso')) },
      { voz: 'Tocá una clase para ver la asistencia completa y los rasgos de cada alumno.', hacer: h => h.clic(h.page.getByRole('button', { name: /^Ver clase:.*Estructuras condicionales: if/ }), 1500) },
      { voz: 'Si un alumno faltó con motivo, tocá Justificar en su fila: la falta pasa a justificada al instante.', hacer: async h => { await h.clic(h.vis(h.page.getByRole('button', { name: /^Justificar ausencia/ })), 1500); } },
      { voz: 'Fijate en este aviso: pasados siete días de la clase ya no se puede editar.', hacer: async h => { await h.apuntar(h.vis(h.page.getByText(/plazo de edición/i))); await h.page.keyboard.press('Escape'); } },
      { voz: 'Dentro de esos siete días aparece el botón Editar, para corregir el tema, la asistencia o los rasgos.', hacer: async h => { await h.clic(h.page.getByRole('button', { name: /^Ver clase:.*Repaso de estructuras/ }).first(), 1400); await h.apuntar(h.vis(h.page.getByRole('dialog').getByRole('button', { name: 'Editar' }))); await h.page.keyboard.press('Escape'); } },
    ],
  },
  {
    id: 'planilla', titulo: 'Planillas y notas',
    preparar: h => h.go(`/home${ESP}&view=planillas&subview=planillas`),
    pasos: [
      { voz: 'En Gestionar planillas hay una tarjeta por cada planilla. Las que tienen un signo más todavía no existen: tocala para crearla.', hacer: async h => { await h.apuntar(h.vis(h.page.getByText('Crear planilla'))); await h.clic(h.vis(h.page.getByText('Crear planilla')).locator('xpath=ancestor::*[self::button or self::a][1]'), 2500); } },
      { voz: 'Con Agregar tarea creás una tarea propia: elegí el instrumento y completá el título, la fecha y el puntaje total.', hacer: async h => { await h.clic(h.role('link', 'Agregar tarea'), 1800); await h.elegir('Instrumento', 'Trabajos en clase'); await h.escribir(h.vis(h.page.getByPlaceholder(/Trabajo práctico/)), 'Trabajo práctico: ciclos'); const p = h.role('spinbutton', 'Puntaje total'); await h.clic(p, 150); await h.page.keyboard.press('Control+A'); await h.page.keyboard.type('10', { delay: 40 }); } },
      { voz: 'Al guardar, la tarea aparece como una columna nueva. Para cargar una nota, escribila en la celda: se guarda sola al salir.', hacer: async h => { await h.clic(h.vis(h.page.getByRole('button', { name: /^(Guardar|Crear)/ })), 2500); const n = h.page.getByRole('spinbutton', { name: /^Nota para/ }).filter({ visible: true }); await h.escribir(n.first(), '8'); await h.page.keyboard.press('Tab'); await h.page.keyboard.type('6', { delay: 40 }); await h.page.keyboard.press('Tab'); await h.pausa(800); } },
      { voz: 'Para cerrar la etapa elegís la fecha de cierre. El botón Confirmar etapa recién se habilita cuando llega esa fecha: la planilla nunca se cierra sola.', hacer: h => h.apuntar(h.vis(h.page.getByRole('button', { name: /^Confirmar Etapa/ }))) },
    ],
  },
  {
    id: 'etapa-cerrada', titulo: 'Etapa cerrada',
    // la planilla abre en la etapa sugerida (la 2): para ver la 1, cerrada, se cambia el selector
    preparar: async h => { await h.go('/planilla/3'); await h.elegir('Etapa', 'Primera etapa'); },
    pasos: [
      { voz: 'Así se ve una planilla con la etapa cerrada: el aviso arriba lo indica y las notas ya no se pueden modificar.', hacer: h => h.apuntar(h.vis(h.page.getByText(/cerrada, no se pueden modificar/))) },
      { voz: 'Solo evaluación o la administración pueden reabrirla. Si lo hacen, te llega una notificación con el motivo.', hacer: async h => { await h.bajar(380); } },
    ],
  },
  {
    id: 'firma', titulo: 'Tu firma',
    preparar: h => h.go('/profile'),
    pasos: [
      { voz: 'Por último, en Mi perfil cargá tu firma. El sistema la pone sola al pie de las planillas que descargás en Excel.', hacer: async h => { const s = h.page.getByRole('heading', { name: 'Firma del docente' }).locator('xpath=ancestor::section[1]'); await h.apuntar(s); } },
      {
        voz: 'Dibujala en el recuadro, o subí una foto de tu firma, y tocá Guardar cambios.',
        hacer: async h => {
          const canvas = h.page.getByRole('heading', { name: 'Firma del docente' }).locator('xpath=ancestor::section[1]').locator('canvas');
          await h.apuntar(canvas);
          const b = await canvas.boundingBox();
          await h.page.mouse.move(b.x + b.width * .15, b.y + b.height * .62, { steps: 8 });
          await h.page.mouse.down();
          for (let t = 0; t <= 1; t += .02) await h.page.mouse.move(b.x + b.width * (.15 + .7 * t), b.y + b.height * (.55 + .22 * Math.sin(t * 14) * (1 - t * .5)), { steps: 2 });
          await h.page.mouse.up();
          await h.clic(h.btn('Guardar cambios'), 1800);
        },
      },
      { voz: 'Si tenés dudas, el manual completo está en tu nombre, arriba a la derecha, en la opción Manual.', hacer: async h => { await h.bajar(-1500); await h.clic(h.vis(h.page.getByRole('button', { name: /^Menú de/ })), 900); await h.apuntar(h.vis(h.page.getByRole('menuitem', { name: /Manual/ }).or(h.page.getByText('Manual', { exact: true })))); await h.page.keyboard.press('Escape'); } },
    ],
  },
];
