// Capturas del manual de padres. Uso: node capturas-padres.mjs <credenciales.env> [nombre ...]
import { run } from './capturas.mjs';

await run('padres', ({ page, btn, settle, go, shot, OUT }) => {
  const seccion = id => page.locator(`section[aria-labelledby="${id}"]`);
  const inicio = async () => { await go('/padre'); await btn('Primera etapa').click(); await settle(); };
  return {
    // vista limpia de la pantalla, para la portada
    async portada() {
      await inicio();
      await page.evaluate(() => scrollTo(0, 0));
      await page.screenshot({ path: OUT + '00-portada.jpg', quality: 85 });
      console.log('✓ 00-portada');
    },
    async barra() {
      await inicio();
      const header = page.locator('header').first();
      await shot('02-barra-superior', [
        { target: page.getByRole('navigation', { name: 'Navegación principal' }), n: 1 },
        { target: page.locator('.theme-toggle').first(), n: 2 },
        { target: page.getByRole('button', { name: /^Notificaciones/ }), n: 3 },
        { target: page.getByRole('button', { name: /^Menú de / }), n: 4 },
      ], header, 6);
      await page.getByRole('button', { name: /^Menú de / }).click();
      await page.waitForTimeout(300);
      const menu = page.getByRole('menu');
      await shot('02b-menu-usuario', [], menu, 12);
      await page.keyboard.press('Escape');
    },
    async resumen() {
      await inicio();
      await shot('03-hijo-resumen', [
        { target: page.locator('.child-card').first(), n: 1 },
        { target: btn('Mes del reporte'), n: 2 },
        { target: btn('Descargar reporte mensual'), n: 3 },
        { target: btn('Descargar libreta'), n: 4 },
        { target: btn('Imprimir resumen'), n: 5 },
        { target: page.locator('.parent-overview-metrics'), n: 6 },
        { target: page.getByLabel('Promedio por etapa'), n: 7 },
      ], page.locator('.parent-overview'), 10);
    },
    async materias() {
      await inicio();
      const filtros = page.getByRole('group', { name: 'Filtrar materias por etapa' });
      await shot('04-materias', [
        { target: filtros, n: 1 },
        { target: page.locator('.parent-subject-card').first(), n: 2 },
        { target: page.locator('.parent-subject-card').first().locator('.parent-subject-grade'), n: 3, pad: 3 },
      ], page.locator('.parent-subject-section'));
      await page.locator('.parent-subject-card').first().click();
      await settle();
      const detalle = seccion('parent-subject-detail-title');
      await shot('05-tareas', [
        { target: btn('Mes de las tareas'), n: 1 },
        { target: detalle.locator('.parent-task-row').first().locator('.parent-task-result'), n: 2, pad: 4 },
        { target: detalle.locator('.parent-task-row.no-entregada .parent-task-status'), n: 3, pad: 4 },
      ], detalle);
      const como = page.getByText('¿Cómo se calcula el promedio?');
      await como.click();
      await page.waitForTimeout(300);
      await shot('06-como-se-calcula', [], page.locator('details.parent-calculation-note'), 12);
    },
    async conducta() {
      await inicio();
      const r = seccion('parent-conducta-title');
      await r.locator('.table-scroll tbody tr').first().waitFor();
      await shot('07-conducta', [
        { target: btn('Materia de la nota de conducta'), n: 1 },
        { target: btn('Conducta desde'), n: 2 },
        { target: btn('Conducta hasta'), n: 3 },
        { target: r.locator('.table-scroll tbody tr').first(), n: 4, pad: 2 },
      ], r);
    },
    async anotaciones() {
      await inicio();
      const r = seccion('parent-anotaciones-title');
      await r.locator('img').first().waitFor();
      await page.waitForTimeout(400);
      await shot('08-anotaciones', [
        { target: r.locator('.anotacion-foto').first(), n: 1, pad: 3 },
        { target: r.locator('h3').first(), n: 2, pad: 3 },
        { target: r.locator('h3').first().locator('xpath=following-sibling::*[last()]'), n: 3, pad: 3 },
      ], r);
    },
  };
});
