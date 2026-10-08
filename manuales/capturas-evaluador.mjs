// Capturas del manual del evaluador. Uso: node capturas-evaluador.mjs <credenciales.env> [nombre ...]
import { run } from './capturas.mjs';

await run('evaluador', ({ page, role, btn, settle, go, pick, shot }) => {
  const main = page.locator('main');
  const abrir = async tarjeta => { await go('/evaluacion'); await btn(tarjeta).click(); await settle(); };
  const item = texto => main.getByRole('button', { name: new RegExp(texto) }).first();
  return {
    async inicio() {
      await go('/evaluacion');
      await shot('03-inicio', [
        { target: btn('Ver planillas'), n: 1 },
        { target: btn('Revisar plan curricular'), n: 2 },
        { target: btn('Seguimiento de profesores'), n: 3 },
      ], page.getByRole('heading', { name: /^Hola/ }));
    },
    async planillas() {
      await abrir('Ver planillas');
      await shot('04-planillas-filtros', [
        { target: btn('Especialidad'), n: 1 },
        { target: btn('Curso'), n: 2 },
        { target: btn('Sección'), n: 3 },
        { target: btn('Etapa'), n: 4 },
        { target: btn('Materia'), n: 5 },
        { target: role('spinbutton', 'Período'), n: 6 },
        { target: btn('Descargar planillas'), n: 7 },
      ]);
      await pick('Especialidad', 'Informática');
      await pick('Curso', '3°');
      await pick('Sección', 'A');
      await shot('05-planillas-lista', [
        { target: btn('Ver planilla de Algorítmica'), n: 1 },
        { target: btn('Descargar planillas'), n: 2 },
      ], main);
      await btn('Ver planilla de Algorítmica').click();
      await settle();
      const reabrir = btn('Reabrir Etapa 1');
      await shot('06-planilla-detalle', [
        { target: btn('Descargar esta planilla'), n: 1 },
        { target: reabrir, n: 2 },
      ], main);
      await reabrir.click();
      await page.waitForTimeout(400);
      const dlg = page.getByRole('dialog').filter({ hasText: 'Motivo de la reapertura' });
      await dlg.getByRole('textbox').first().fill('La profesora debe corregir la nota de un alumno que entregó fuera de término con justificación.');
      await shot('07-reabrir-etapa', [
        { target: dlg.getByRole('textbox').first(), n: 1 },
        { target: dlg.getByRole('button', { name: /Reabrir Etapa/ }).last(), n: 2 },
      ], dlg);
    },
    async planes() {
      await abrir('Revisar plan curricular');
      await item('Algorítmica Graciela').click();
      await settle();
      await shot('08-plan-revisar', [
        { target: item('Algorítmica Graciela'), n: 1 },
        { target: btn('Descargar archivo original'), n: 2 },
        { target: page.getByRole('heading', { name: 'Temas por mes' }), n: 3 },
        { target: role('textbox', 'Observaciones'), n: 4 },
        { target: btn('Aprobar'), n: 5 },
        { target: btn('Rechazar'), n: 6 },
      ], main.getByRole('heading', { name: /Planes pendientes/ }));
    },
    async seguimiento() {
      await abrir('Seguimiento de profesores');
      await item('Algorítmica Graciela').click();
      await settle();
      await shot('09-seguimiento-plan', [
        { target: btn('Seguimiento de planes'), n: 1 },
        { target: item('Algorítmica Graciela'), n: 2 },
        { target: page.getByRole('table', { name: /Cumplimiento de temas/ }), n: 3 },
      ], main.getByRole('heading', { name: /Planes aprobados/ }));
      await item('^Incumplimientos').click();
      await settle();
      await shot('10-incumplimientos', [
        { target: role('searchbox', 'Buscar'), n: 1 },
        { target: btn('Tipo de caso'), n: 2 },
        { target: page.getByRole('region', { name: 'Bloqueos de Iniciar clase' }), n: 3 },
        { target: page.getByRole('region', { name: 'Atrasos pendientes de revisión' }), n: 4 },
        { target: page.getByRole('region', { name: 'Incongruencias retroactivas' }), n: 5 },
      ], main.getByRole('heading', { name: /Casos pendientes/ }));
      const casos = [
        ['Atrasos pendientes de revisión', '11-caso-atraso'],
        ['Incongruencias retroactivas', '12-caso-incongruencia'],
        ['Bloqueos de Iniciar clase', '13-caso-bloqueo'],
      ];
      for (const [region, nombre] of casos) {
        await page.getByRole('region', { name: region }).getByRole('button').first().click();
        await settle();
        // el detalle es el último bloque con título h3 (a la derecha de la lista)
        const detalle = main.getByRole('heading', { level: 3 }).last().locator('xpath=..');
        const acciones = detalle.getByRole('button', { name: /Aceptar|Rechazar|Reactivar/ });
        const marcas = [];
        const nota = detalle.getByRole('textbox').first();
        if (await nota.count()) marcas.push({ target: nota, n: marcas.length + 1 });
        for (let i = 0; i < await acciones.count(); i++) marcas.push({ target: acciones.nth(i), n: marcas.length + 1 });
        await shot(nombre, marcas, detalle);
      }
    },
  };
});
