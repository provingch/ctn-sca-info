// Capturas del manual del administrador (cuenta global: ve todos los módulos).
// Uso: node capturas-administrador.mjs <credenciales.env> [nombre ...]
import { run } from './capturas.mjs';

await run('administrador', ({ page, role, btn, settle, go, pick, shot }) => {
  const main = page.locator('main');
  const dialogo = () => page.getByRole('dialog').filter({ visible: true }).first();
  const cerrar = async () => { await page.keyboard.press('Escape'); await page.waitForTimeout(300); };
  const tarjeta = nombre => main.getByRole('button', { name: new RegExp(`^Especialidad Especialidad ${nombre}`) }).first();
  return {
    async inicio() {
      await go('/admin');
      const modulos = ['Materias', 'Usuarios', 'Asignaciones', 'Alumnos', 'Horarios', 'Estado del sistema', 'Salas', 'Quejas', 'Clases dadas'];
      await shot('03-inicio', modulos.map((m, i) => ({ target: role('link', m), n: i + 1 })), page.getByRole('region', { name: 'Gestión del sistema' }));
    },
    async usuarios() {
      await go('/admin/usuarios');
      await shot('04-usuarios', [
        { target: btn('Filtrar por rol'), n: 1 },
        { target: role('searchbox', 'Buscar'), n: 2 },
        { target: btn('Usuarios por página'), n: 3 },
        { target: btn('Crear registro'), n: 4 },
        { target: btn('Editar'), n: 5 },
        { target: btn('Más acciones'), n: 6 },
        { target: page.getByRole('navigation', { name: 'Paginación de usuarios' }), n: 7 },
      ], main.getByRole('table', { name: 'Usuarios registrados' }));
      await btn('Crear registro').click();
      await page.waitForTimeout(500);
      await shot('05-usuario-crear', [], dialogo());
      await cerrar();
      await pick('Filtrar por rol', 'Padres');
      await role('searchbox', 'Buscar').fill('Estigarribia');
      await settle();
      await btn('Editar').click();
      await page.waitForTimeout(800);
      await shot('06-usuario-padre', [], dialogo());
      await cerrar();
    },
    async materias() {
      await go('/admin/materias');
      await shot('07-materias', [
        { target: btn('Crear materia'), n: 1 },
        { target: role('searchbox', 'Buscar por nombre'), n: 2 },
        { target: btn('Filtrar por tipo'), n: 3 },
        { target: btn('Filtrar por especialidad'), n: 4 },
        { target: btn('Editar Algorítmica'), n: 5 },
      ], page.getByRole('region', { name: 'Catálogo de materias' }));
      await btn('Crear materia').click();
      await page.waitForTimeout(500);
      await shot('08-materia-crear', [], dialogo());
      await cerrar();
    },
    async asignaciones() {
      await go('/admin/asignaciones');
      await shot('09-asignaciones', [
        { target: role('searchbox', 'Buscar profesor'), n: 1 },
        { target: btn('Filtrar por asignaciones'), n: 2 },
        { target: page.getByRole('table', { name: 'Profesores y cantidad de asignaciones' }), n: 3 },
      ], page.getByRole('region', { name: 'Profesores y asignaciones' }));
      await role('searchbox', 'Buscar profesor').fill('Graciela');
      await settle();
      await main.getByRole('button', { name: /^Ver detalle de López Molinas/ }).first().click();
      await settle();
      await shot('10-asignaciones-profesor', [
        { target: main.getByRole('button', { name: /Crear asignación/ }).first(), n: 1 },
      ], main);
    },
    async alumnos() {
      await go('/admin/alumnos');
      await shot('11-alumnos', [{ target: tarjeta('Informática'), n: 1 }], main);
      await tarjeta('Informática').click();
      await settle();
      await main.getByRole('button', { name: /^Curso 3°/ }).click();
      await settle();
      await main.getByRole('link', { name: /^Sección A/ }).or(main.getByRole('button', { name: /^Sección A/ })).first().click();
      await settle();
      await shot('12-alumnos-seccion', [
        { target: main.getByRole('searchbox').first(), n: 1 },
        { target: main.getByRole('button', { name: /Agregar alumno/ }).first(), n: 2 },
        { target: btn('Editar'), n: 3 },
      ]); // recorte = marcas: con 28 filas la tabla entera queda ilegible
    },
    async horarios() {
      await go('/admin/horarios');
      await tarjeta('Informática').click();
      await settle();
      await shot('13-horarios-cursos', [
        { target: main.getByRole('button', { name: /^Curso 3°/ }), n: 1 },
        { target: btn('PDF de la especialidad'), n: 2 },
      ], main);
      await main.getByRole('button', { name: /^Curso 3°/ }).click();
      await settle();
      await main.getByRole('link', { name: /^Sección A/ }).click();
      await settle();
      await shot('14-horario', [], main);
    },
    async sistema() {
      await go('/admin/sistema');
      await shot('15-sistema', [
        { target: btn('Actualizar'), n: 1 },
        { target: page.getByRole('region', { name: 'Resumen del sistema' }), n: 2 },
        { target: page.getByRole('heading', { name: 'Migraciones aplicadas' }), n: 3 },
      ], main);
    },
    async salas() {
      await go('/admin/salas');
      await shot('16-salas', [
        { target: btn('＋ Agregar sala'), n: 1 },
        { target: role('searchbox', 'Buscar sala en todo el catálogo'), n: 2 },
        { target: main.getByRole('button', { name: /Comunes/ }).first(), n: 3 },
      ], main);
    },
    async quejas() {
      await go('/admin/quejas');
      const registrar = page.getByRole('region', { name: 'Registrar queja' });
      await shot('17-quejas-registrar', [
        { target: btn('Curso'), n: 1 },
        { target: btn('Profesor'), n: 2 },
        { target: role('textbox', 'Motivo'), n: 3 },
        { target: btn('Registrar queja'), n: 4 },
      ], registrar);
      await shot('18-quejas-historial', [], page.getByRole('region', { name: /^Quejas registradas/ }));
    },
    async clases() {
      await go('/admin/clases');
      await role('searchbox', 'Buscar clases').fill('Algorítmica');
      await settle();
      await shot('19-clases', [{ target: role('searchbox', 'Buscar clases'), n: 1 }], main);
      await main.getByRole('row').nth(1).click().catch(() => {});
      await page.waitForTimeout(800);
      if (await page.getByRole('dialog').filter({ visible: true }).count()) await shot('20-clase-detalle', [], dialogo());
    },
  };
});
