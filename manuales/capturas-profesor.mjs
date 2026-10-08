// Capturas del manual del profesor. Uso: node capturas-profesor.mjs <credenciales.env> [nombre ...]
import fs from 'node:fs';
import { execFileSync } from 'node:child_process';
import { annotate } from './lib.mjs';
import { run } from './capturas.mjs';

const ESP = '?especialidadId=5';

await run('profesor', ({ page, role, btn, settle, go, pick, shot, OUT }) => ({
  async inicio() {
    await go('/home' + ESP);
    await shot('03-inicio', [
      { target: btn('Libro de Cátedra'), n: 1, label: 'Clases y plan curricular', side: 'bottom' },
      { target: btn('Gestionar planillas'), n: 2, label: 'Notas y tareas', side: 'bottom' },
      { target: page.getByRole('region', { name: 'Actividad reciente' }), n: 3, label: 'Tus últimas acciones', side: 'left' },
    ], page.getByRole('heading', { name: 'Hola, Graciela' }));
    await shot('04-tus-materias', [
      { target: page.getByRole('heading', { name: 'Tus materias' }), n: 1, label: 'Tocá una materia para ir a su planilla', side: 'right' },
      { target: page.locator('main').getByText('Plan sin cargar').first(), n: 2, label: 'Aviso del plan curricular', side: 'right' },
    ], page.getByRole('heading', { name: 'Tus materias' }).locator('xpath=..'));
  },
  async barra() {
    await go('/home' + ESP);
    await shot('02-barra-superior', [
      { target: role('navigation', 'Navegación principal'), n: 1, label: 'Secciones', side: 'bottom' },
      { target: btn('Cambiar a modo oscuro'), n: 2, label: 'Modo claro / oscuro', side: 'bottom' },
      { target: page.getByRole('button', { name: /^Notificaciones/ }), n: 3, label: 'Notificaciones', side: 'bottom' },
      { target: btn('Menú de Graciela López'), n: 4, label: 'Tu menú', side: 'bottom' },
    ], page.locator('header').first(), 0);
    await btn('Menú de Graciela López').click();
    await page.waitForTimeout(300);
    const menu = page.getByRole('menu').first();
    await shot('05-menu-usuario', [
      { target: menu.getByText('Manual', { exact: false }).first(), n: 1, label: 'Abre este manual', side: 'left' },
    ], menu);
    await page.keyboard.press('Escape');
    await page.getByRole('button', { name: /^Notificaciones/ }).click();
    await page.waitForTimeout(500);
    const panel = page.getByRole('dialog').first();
    await shot('06-notificaciones', [], panel);
    await page.keyboard.press('Escape');
  },
  async catedra() {
    await go('/home' + ESP + '&view=catedra&subview=clase');
    const tabs = page.getByRole('tablist', { name: 'Secciones del libro de cátedra' });
    await shot('07-pestanas-catedra', ['Iniciar clase', 'Plan curricular Sin cargar', 'Clases dadas', 'RSA', 'Quejas']
      .map((t, i) => ({ target: role('tab', t), n: i + 1 })), tabs);
    await shot('08-horario-hoy', [
      { target: btn('Iniciar clase distinta'), n: 1, label: 'Clase fuera de tu horario', side: 'right' },
    ], role('tabpanel', 'Iniciar clase'));
  },
  async clase() {
    await go('/home' + ESP + '&view=catedra&subview=clase');
    await btn('Iniciar clase distinta').click();
    await settle();
    await shot('09-clase-elegir-curso', [
      { target: btn('Curso'), n: 1, side: 'bottom', label: 'Curso' },
      { target: btn('Sección'), n: 2, side: 'bottom', label: 'Sección' },
      { target: btn('Volver al horario de hoy'), n: 3, side: 'bottom', label: 'Volver al horario' },
    ], btn('Especialidad').locator('xpath=ancestor::*[contains(@class,"card") or contains(@class,"filter")][1]'));
    await pick('Curso', '3°');
    await pick('Sección', 'A');
    const panel = role('tabpanel', 'Iniciar clase');
    await shot('10-clase-formulario', [
      { target: btn('Asignación de la clase'), n: 1, label: 'Materia', side: 'right' },
      { target: page.getByRole('group', { name: 'Horario de la clase' }), n: 2, label: 'Inicio y horas cátedra', side: 'right' },
      { target: btn('Modalidad de la clase'), n: 3, label: 'Modalidad', side: 'right' },
      { target: btn('Instrumento'), n: 4, label: 'Tipo de clase', side: 'left' },
      { target: role('textbox', 'Contenido específico desarrollado'), n: 5, label: 'Tema de la clase', side: 'right' },
    ], panel.getByRole('heading', { name: 'Registro de clase' }).locator('xpath=../..'), 0);
    const asist = page.getByRole('group', { name: 'Asistencia de alumnos' });
    await shot('11-clase-asistencia', [
      { target: asist.getByRole('button', { name: /, presente$/ }).first(), n: 1, label: 'Tocá el nombre para marcarlo ausente (y otra vez para volverlo presente)', side: 'right' },
      { target: asist.getByRole('button', { name: /^Rasgos conductuales/ }).nth(1), n: 2, label: 'Zona de rasgos: asignar un código de conducta', side: 'bottom' },
      { target: page.getByRole('button', { name: /Guardar inicio de clase/ }), n: 3, label: 'Guardar', side: 'left' },
    ], page.getByRole('heading', { name: 'Asistencia general y justificativos' }));
  },
  async plan() {
    await go('/home' + ESP + '&view=catedra&subview=plan-curricular');
    await shot('12-plan-descargar', [
      { target: btn('Curso de Informática'), n: 1, side: 'bottom', label: 'Curso' },
      { target: btn('Sección de Informática'), n: 2, side: 'bottom', label: 'Sección' },
      { target: btn('Materia de Informática'), n: 3, side: 'bottom', label: 'Materia' },
      { target: btn('Etapa de Informática'), n: 4, side: 'bottom', label: 'Etapa' },
      { target: btn('Configurar y descargar'), n: 5, side: 'bottom', label: 'Descargar' },
    ], page.getByRole('heading', { name: 'Descargar plantilla' }).locator('xpath=..'));
    await pick('Curso de Informática', '3º');
    await pick('Sección de Informática', 'A');
    await pick('Materia de Informática', 'Algorítmica');
    await btn('Configurar y descargar').click();
    await page.waitForTimeout(500);
    const dlg = page.getByRole('dialog').first();
    await shot('13-plan-dialogo', [
      { target: dlg.getByText('Marzo', { exact: true }).locator('xpath=ancestor::*[.//input[@type="checkbox"]][1]'), n: 1, label: 'Tildá los meses que vas a completar', side: 'left' },
      { target: dlg.getByRole('spinbutton').first(), n: 2, label: 'Bloques por mes', side: 'right' },
      { target: dlg.getByRole('button', { name: 'Descargar plantilla' }), n: 3, label: 'Descargar', side: 'left' },
    ], dlg);
    await page.keyboard.press('Escape');
    await shot('14-plan-subir', [
      { target: page.getByRole('button', { name: /^Arrastrá tu plan curricular/ }), n: 1, label: 'Elegí el archivo .xlsx', side: 'bottom' },
      { target: btn('Subir plan curricular'), n: 2, label: 'Subir', side: 'right' },
      { target: page.getByRole('heading', { name: 'Entregas realizadas' }), n: 3, label: 'Estado de tus planes', side: 'right' },
    ], page.getByRole('heading', { name: 'Subir plan' }).locator('xpath=../..'));
  },
  async rsa() {
    // resolve es buscar-o-crear: se responde acá con la planilla 3 (3° A · Algorítmica · Etapa 1, ya existe)
    await page.route('**/api/planillas/resolve', r => r.fulfill({ json: { planillaId: 3 } }));
    await go('/home' + ESP + '&view=catedra&subview=rsa');
    await pick('Curso', '3º');
    await pick('Sección', 'A');
    await pick('Materia', 'Algorítmica');
    await pick('Etapa', 'Etapa 1');
    const panel = role('tabpanel', 'RSA');
    await shot('15-rsa', [
      { target: page.getByRole('region', { name: '1. Elegí la planilla' }), n: 1, label: 'Elegí la planilla', side: 'right' },
      { target: page.getByRole('checkbox', { name: /Activar RSA/ }), n: 2, label: 'Activar RSA', side: 'right' },
      { target: btn('Guardar configuración'), n: 3, label: 'Guardar', side: 'right' },
    ], panel);
  },
  async quejas() {
    await go('/home' + ESP + '&view=catedra&subview=quejas');
    await pick('Curso', '3º');
    await pick('Sección', 'A');
    await shot('16-quejas', [], role('tabpanel', 'Quejas'));
  },
  async planillas() {
    await go('/home' + ESP + '&view=planillas&subview=planillas');
    const cards = page.locator('main');
    await shot('17-planillas', [
      { target: cards.getByText(/A · Etapa 1/).first().locator('xpath=ancestor::*[self::a or self::button or self::article][1]'), n: 1, label: 'Planilla existente', side: 'bottom' },
      { target: cards.getByText('Crear planilla').first().locator('xpath=ancestor::*[self::a or self::button or self::article][1]'), n: 2, label: 'Crear planilla', side: 'bottom' },
    ], cards);
    await go('/home' + ESP + '&view=planillas&subview=riesgo');
    await shot('18-riesgo', [], page.locator('main'));
  },
  async planilla() {
    await go('/planilla/3');
    const main = page.locator('main');
    // el aviso aparece solo porque la captura bloquea la sincronización real
    await main.getByText('No se pudo sincronizar Classroom').locator('xpath=ancestor::*[@role="alert" or @role="status" or contains(@class,"alert") or contains(@class,"banner")][1]').evaluate(e => e.remove()).catch(() => {});
    await shot('19-planilla-cabecera', [
      { target: btn('Etapa'), n: 1, label: 'Etapa', side: 'bottom' },
      { target: btn('Cierre Etapa 1'), n: 2, label: 'Fecha de cierre', side: 'bottom' },
      { target: btn('Confirmar Etapa 1'), n: 3, label: 'Cerrar la etapa', side: 'bottom' },
      { target: btn('Sincronizar Classroom'), n: 4, label: 'Classroom', side: 'top' },
      { target: role('link', 'Agregar tarea'), n: 5, label: 'Tarea propia', side: 'top' },
      { target: btn('Descargar'), n: 6, label: 'Excel', side: 'left' },
    ], page.getByRole('heading', { name: 'Algorítmica', level: 1 }));
    await shot('20-planilla-tabla', [
      { target: page.getByText('Inmovilizar alumnos', { exact: true }).locator('xpath=ancestor-or-self::label[1]'), n: 1, label: 'Fijar nombres', side: 'right' },
      { target: page.getByRole('searchbox', { name: 'Buscar alumno' }), n: 2, label: 'Buscar alumno', side: 'right' },
      { target: page.getByRole('spinbutton').first(), n: 3, label: 'Escribí la nota', side: 'right' },
      { target: btn('Ordenar por Nota'), n: 4, label: 'Ordenar', side: 'left' },
    ], page.getByRole('table'));
  },
  async perfil() {
    // Ventana alta: el contenido del perfil tiene scroll propio y si no, se corta antes de la firma.
    await page.setViewportSize({ width: 1280, height: 1500 });
    await go('/profile');
    const main = page.locator('main');
    const seccion = titulo => page.getByRole('heading', { name: titulo, exact: true }).locator('xpath=ancestor::section[1]');
    // Firma de ejemplo dibujada en el recuadro (no se guarda: "Guardar cambios" no se toca).
    const canvas = seccion('Firma del docente').locator('canvas');
    const b = await canvas.boundingBox();
    await page.mouse.move(b.x + b.width * .15, b.y + b.height * .65);
    await page.mouse.down();
    for (let t = 0; t <= 1; t += .02) {
      await page.mouse.move(b.x + b.width * (.15 + .7 * t), b.y + b.height * (.55 + .22 * Math.sin(t * 14) * (1 - t * .5)));
    }
    await page.mouse.up();
    await shot('21-perfil', [
      { target: seccion('Información personal'), n: 1, label: 'Datos personales' },
      { target: seccion('Contacto'), n: 2, label: 'Contacto' },
      { target: seccion('Cuenta'), n: 3, label: 'Usuario' },
      { target: seccion('Foto de perfil'), n: 4, label: 'Foto' },
      { target: seccion('Firma del docente'), n: 5, label: 'Firma' },
      { target: seccion('Google Classroom'), n: 6, label: 'Classroom' },
      { target: btn('Guardar cambios'), n: 7, label: 'Guardar' },
    ], page.getByRole('button', { name: /^Perfil/ }).first()); // recorte = pestañas + marcas, sin el vacío de abajo
    await shot('21b-perfil-firma', [
      { target: canvas, n: 1, label: 'Dibujá tu firma' },
      { target: page.getByText('Subir imagen', { exact: true }).locator('xpath=ancestor-or-self::label[1]'), n: 2, label: 'Subir imagen' },
      { target: seccion('Firma del docente').getByRole('button', { name: 'Borrar' }), n: 3, label: 'Borrar' },
    ], seccion('Firma del docente'));
    await page.setViewportSize({ width: 1280, height: 800 });
    const tabs = [['Seguridad', '22-perfil-seguridad'], ['Materias', '23-perfil-materias'], ['Aplicación', '24-perfil-aplicacion'], ['Registros', '25-perfil-registros']];
    for (const [t, name] of tabs) {
      const tab = page.getByRole('button', { name: new RegExp('^' + t) }).first();
      await tab.click();
      await settle();
      await shot(name, [{ target: tab, n: 1, label: t }], main);
    }
  },
  // Dónde aparece la firma: el pie de la planilla exportada a Excel (descarga = solo lectura).
  async excel() {
    await go('/planilla/3');
    const [dl] = await Promise.all([page.waitForEvent('download'), btn('Descargar').click()]);
    const tmp = fs.mkdtempSync('/tmp/sca-excel-');
    await dl.saveAs(tmp + '/planilla.xlsx');
    const info = JSON.parse(execFileSync('python3', ['-I', new URL('./excel-a-imagen.py', import.meta.url).pathname, tmp + '/planilla.xlsx', tmp + '/planilla.png'], { encoding: 'utf8' }));
    const img = 'data:image/png;base64,' + fs.readFileSync(tmp + '/planilla.png').toString('base64');
    fs.rmSync(tmp, { recursive: true });
    const p = await page.context().newPage();
    await p.setViewportSize({ width: 1200, height: 900 });
    const k = 1200 / info.ancho;
    const f = info.firma;
    await p.setContent(`<body style="margin:0"><div style="position:relative"><img id="hoja" src="${img}" style="width:1200px;display:block">
      <div id="firma" style="position:absolute;left:${f.x * k}px;top:${f.y * k}px;width:${f.w * k}px;height:${f.h * k}px"></div></div></body>`);
    // recorte al contenido (sin los márgenes blancos de la hoja)
    const box = await p.evaluate(() => {
      const i = document.getElementById('hoja'), c = document.createElement('canvas');
      c.width = i.naturalWidth; c.height = i.naturalHeight;
      const g = c.getContext('2d'); g.drawImage(i, 0, 0);
      const d = g.getImageData(0, 0, c.width, c.height).data, s = i.width / c.width;
      let x0 = c.width, y0 = c.height, x1 = 0, y1 = 0;
      for (let y = 0; y < c.height; y += 2) for (let x = 0; x < c.width; x += 2) {
        const o = (y * c.width + x) * 4;
        if (d[o] < 230 || d[o + 1] < 230 || d[o + 2] < 230) { x0 = Math.min(x0, x); y0 = Math.min(y0, y); x1 = Math.max(x1, x); y1 = Math.max(y1, y); }
      }
      return { x: x0 * s - 24, y: y0 * s - 24, width: (x1 - x0) * s + 48, height: (y1 - y0) * s + 48 };
    });
    await p.setViewportSize({ width: 1200, height: Math.ceil(box.y + box.height + 40) });
    await annotate(p, [{ target: p.locator('#firma'), n: 1 }]);
    await p.screenshot({ path: OUT + '26-firma-excel.jpg', quality: 85, clip: box });
    await p.close();
    console.log('✓ 26-firma-excel');
  },
  // Pantallas que solo tienen contenido con clases, planes y quejas cargados.
  async datos() {
    await go('/home' + ESP + '&view=catedra&subview=mis-clases');
    const panel = role('tabpanel', 'Clases dadas');
    await shot('27-clases-dadas', [
      { target: btn('Filtrar por curso'), n: 1 },
      { target: btn('Filtrar por especialidad'), n: 2 },
      { target: btn('Orden por fecha'), n: 3 },
      { target: page.getByRole('button', { name: /^Ver clase:.*Estructuras condicionales: if/ }), n: 4 },
    ], panel);
    await page.getByRole('button', { name: /^Ver clase:.*Estructuras condicionales: if/ }).click();
    await page.waitForTimeout(700);
    const detalle = page.getByRole('dialog', { name: 'Detalle de clase' });
    // pasados 7 días ya no hay "Editar": se marca solo lo que está
    await shot('28-clase-detalle', [
      { target: detalle.getByRole('button', { name: /^Justificar ausencia/ }).first(), n: 1 },
    ], detalle);
    await page.keyboard.press('Escape');

    await go('/home' + ESP + '&view=catedra&subview=plan-curricular');
    const incon = page.getByRole('region', { name: /Clases previas al plan con incongruencias/ });
    await incon.getByRole('textbox').first().fill('El grupo tenía dificultades con los diagramas y se priorizó reforzarlos.');
    await shot('29-plan-incongruencias', [
      { target: incon.getByRole('textbox').first(), n: 1 },
      { target: incon.getByRole('button', { name: /justificación/ }).first(), n: 2 },
    ], incon);
    const entregas = page.getByRole('heading', { name: 'Entregas realizadas' }).locator('xpath=..');
    await shot('30-plan-entregas', [
      { target: btn('Ver detalle de plan-algoritmica-3A-etapa2.xlsx'), n: 1 },
      { target: page.getByRole('table').filter({ hasText: 'plan-algoritmica' }).getByText(/Aprobado/i).first(), n: 2 },
      { target: page.getByRole('table').filter({ hasText: 'plan-algoritmica' }).getByText(/Pendiente/i).first(), n: 3 },
    ], entregas);
    await btn('Ver detalle de plan-algoritmica-3A-etapa2.xlsx').click();
    await page.waitForTimeout(700);
    await shot('31-plan-detalle', [], page.getByRole('dialog').filter({ visible: true }).first());
    await page.keyboard.press('Escape');

    await go('/home' + ESP + '&view=catedra&subview=quejas');
    await shot('32-quejas-historial', [], page.getByRole('heading', { name: 'Tus quejas registradas' }).locator('xpath=ancestor::section[1]'));

  },
}));
