// Capturas del manual de padres. Uso: node capturas-padres.mjs <credenciales.env> [nombre ...]
import { run } from './capturas.mjs';

await run('padres', ({ page, role, btn, settle, go, shot }) => {
  const region = name => page.getByRole('region', { name, exact: true });
  const inicio = async () => { await go('/padre'); await btn('Primera etapa').click(); await settle(); };
  return {
    async resumen() {
      await inicio();
      await shot('03-hijo-resumen', [
        { target: page.getByRole('button', { name: /ESTIGARRIBIA DELGADILLO/ }).first(), n: 1 },
        { target: btn('Mes del reporte'), n: 2 },
        { target: btn('Descargar reporte mensual'), n: 3 },
        { target: btn('Descargar libreta'), n: 4 },
        { target: btn('Imprimir resumen'), n: 5 },
        { target: page.getByText('Promedio general', { exact: true }).locator('xpath=ancestor::article[1]'), n: 6 },
      ], page.getByRole('heading', { name: /^Hola/ }));
    },
    async materias() {
      await inicio();
      await shot('04-materias', [
        { target: page.getByRole('group', { name: 'Filtrar materias por etapa' }), n: 1 },
        { target: page.getByRole('button', { name: /^Materia Algorítmica/ }), n: 2 },
      ], region('Promedios de primera etapa'));
      const detalle = region('Algorítmica');
      await shot('05-tareas', [
        { target: btn('Mes de las tareas'), n: 1 },
        { target: detalle.getByText('Trabajo práctico: diagramas de flujo').locator('xpath=ancestor::*[.//strong[contains(.," / ")]][1]'), n: 2 },
      ], detalle);
      const como = page.getByText('¿Cómo se calcula el promedio?');
      await como.click();
      await page.waitForTimeout(300);
      await shot('06-como-se-calcula', [], como.locator('xpath=ancestor::details[1]'));
    },
    async conducta() {
      await inicio();
      const r = region('Notas de conducta');
      await shot('07-conducta', [
        { target: btn('Materia de la nota de conducta'), n: 1 },
        { target: btn('Conducta desde'), n: 2 },
        { target: btn('Conducta hasta'), n: 3 },
        { target: r.getByRole('table'), n: 4 },
      ], r);
    },
  };
});
