// Capturas del manual de Coordinación Pedagógica. Uso: node capturas-coordinacion.mjs <credenciales.env> [nombre ...]
import { run } from './capturas.mjs';

await run('coordinacion', ({ page, role, btn, settle, go, shot }) => {
  const main = page.locator('main');
  // tarjeta de una queja: el bloque más chico que contiene su texto y algún botón o rótulo
  const queja = texto => page.getByRole('listitem').filter({ hasText: texto }).first();
  const quejas = async () => { await go('/coordinacion'); await btn('Quejas por profesor').click(); await settle(); };
  const profesor = async nombre => { await quejas(); await main.getByRole('button', { name: new RegExp(nombre) }).first().click(); await settle(); };
  return {
    async inicio() {
      await go('/coordinacion');
      await shot('03-inicio', [
        { target: btn('Quejas por profesor'), n: 1 },
        { target: btn('Reportes conductuales'), n: 2 },
      ], page.getByRole('heading', { name: /^Hola/ }));
    },
    async lista() {
      await quejas();
      await shot('04-quejas-lista', [
        { target: btn('Contra profesores'), n: 1 },
        { target: btn('Sobre cursos'), n: 2 },
        { target: main.getByRole('button', { name: /Graciela López/ }).first(), n: 3 },
      ], main.getByRole('group', { name: 'Dirección de las quejas' }));
      await btn('Sobre cursos').click();
      await settle();
      await shot('05-quejas-cursos', [], main);
    },
    async graciela() {
      await profesor('Graciela López');
      await shot('06-quejas-profesor', [
        { target: page.getByRole('group', { name: 'Filtrar quejas por estado' }), n: 1 },
        { target: queja('Los alumnos informan'), n: 2 },
        { target: queja('Los padres reclaman'), n: 3 },
        { target: queja('Un alumno indica'), n: 4 },
      ], page.getByRole('heading', { name: /Quejas de Graciela/ }));
      await shot('07-queja-pendiente', [
        { target: queja('Los alumnos informan').getByRole('button', { name: 'Aceptar' }), n: 1 },
        { target: queja('Los alumnos informan').getByRole('button', { name: 'Rechazar' }), n: 2 },
      ], queja('Los alumnos informan'));
      await queja('Los alumnos informan').getByRole('button', { name: 'Rechazar' }).click();
      await page.waitForTimeout(400);
      const rechazo = queja('Los alumnos informan');
      await rechazo.getByRole('textbox').first().fill('La situación ya fue resuelta en la reunión de padres del 3 de octubre.');
      await shot('08-queja-rechazar', [
        { target: rechazo.getByRole('textbox').first(), n: 1 },
        { target: rechazo.getByRole('button', { name: 'Confirmar rechazo' }), n: 2 },
      ], rechazo);
      await rechazo.getByRole('button', { name: 'Cancelar' }).click();
      await queja('Los padres reclaman').getByRole('button', { name: 'Registrar solución' }).click();
      await page.waitForTimeout(400);
      const solucion = queja('Los padres reclaman');
      const campos = solucion.getByRole('textbox');
      const textos = ['Se revisó el calendario de pruebas con la profesora.', 'Las pruebas se anuncian ahora también en el sistema con una semana de anticipación.', 'Graciela López'];
      for (let i = 0; i < Math.min(await campos.count(), textos.length); i++) await campos.nth(i).fill(textos[i]);
      const marcas = [];
      for (let i = 0; i < await campos.count(); i++) marcas.push({ target: campos.nth(i), n: i + 1 });
      marcas.push({ target: solucion.getByRole('button', { name: 'Marcar como resuelta' }), n: marcas.length + 1 });
      await shot('09-queja-solucion', marcas, solucion);
    },
    async federico() {
      await profesor('Federico Gónzalez');
      const aceptada = queja('Inasistencias reiteradas');
      await shot('10-queja-aceptada', [{ target: aceptada.getByRole('button', { name: /Revisar/ }).first(), n: 1 }], aceptada);
      await shot('11-queja-resuelta', [
        { target: queja('Error en la carga de notas').getByRole('button', { name: /reporte de solución/ }), n: 1 },
      ], queja('Error en la carga de notas'));
      await aceptada.getByRole('button', { name: /Revisar/ }).first().click();
      await page.waitForTimeout(400);
      const revision = queja('Inasistencias reiteradas');
      await revision.getByRole('textbox').first().fill('Se verificaron las planillas de asistencia del docente con la dirección. Se acordó un aviso previo obligatorio.');
      const botones = revision.getByRole('button');
      const marcasRev = [{ target: revision.getByRole('textbox').first(), n: 1 }];
      for (let i = 0; i < await botones.count(); i++) marcasRev.push({ target: botones.nth(i), n: marcasRev.length + 1 });
      await shot('12-queja-revisar', marcasRev, revision);
    },
    async conducta() {
      await go('/coordinacion');
      await btn('Reportes conductuales').click();
      await settle();
      await role('textbox', 'Código').fill('N9');
      await role('textbox', 'Descripción').fill('Usa el celular durante la clase sin autorización');
      await shot('13-conducta', [
        { target: role('textbox', 'Código'), n: 1 },
        { target: role('textbox', 'Descripción'), n: 2 },
        { target: btn('Agregar código'), n: 3 },
        { target: btn('Editar'), n: 4 },
        { target: btn('Desactivar'), n: 5 },
      ], page.getByRole('heading', { name: 'Catálogo de conducta' }).locator('xpath=..'));
    },
  };
});
