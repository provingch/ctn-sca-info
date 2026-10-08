// Corre las capturas anotadas de un manual contra prod (solo lectura: lib.mjs aborta escrituras).
// Cada capturas-<rol>.mjs llama a run(rol, h => ({ nombre: async () => {...} })).
// Uso: node capturas-<rol>.mjs <credenciales.env> [nombre ...]   (la sesión se guarda en <credenciales.env>.state.json)
import fs from 'node:fs';
import { open, login, creds, BASE, blurPII, annotate, annotatedBox, clear } from './lib.mjs';

export async function run(rol, makeShots) {
  const credFile = process.argv[2];
  if (!credFile) throw new Error('Falta el archivo de credenciales');
  process.env.SCA_STATE = credFile + '.state.json';
  const OUT = new URL(`./img/${rol}/`, import.meta.url).pathname;
  fs.mkdirSync(OUT, { recursive: true });
  const only = process.argv.slice(3);

  const { browser, page } = await open({ width: 1280, height: 800, scale: 1.25 });
  const role = (r, name, exact = true) => page.getByRole(r, { name, exact }).filter({ visible: true }).first();
  const btn = name => role('button', name);
  async function settle() {
    await page.waitForLoadState('networkidle');
    await page.addStyleTag({ content: 'footer,a[href="#main-content"]{display:none!important} header{position:static!important} *{transition:none!important;animation:none!important;caret-color:transparent!important}' });
    await page.waitForTimeout(500);
  }
  async function go(path) {
    await page.goto(BASE + path);
    await settle();
  }
  // Elige una opción en los selects propios del sistema (botón + listbox).
  async function pick(button, option) {
    await btn(button).click();
    const exacta = role('option', option);
    await (await exacta.count() ? exacta : role('option', option, false)).click();
    await settle();
  }
  // Difumina datos sensibles, dibuja las marcas y recorta a `region` + marcas, con margen.
  async function shot(name, marks = [], region, margin = 20) {
    await page.evaluate(() => document.activeElement?.blur());
    await (region ?? marks[0]?.target)?.scrollIntoViewIfNeeded();
    await blurPII(page);
    await annotate(page, marks); // `label` es solo referencia: la leyenda vive en el HTML del manual
    const b = await annotatedBox(page, region);
    const W = page.viewportSize().width;
    const clip = b && { x: Math.max(0, b.x - margin), y: Math.max(0, b.y - margin) };
    if (clip) Object.assign(clip, { width: Math.min(W - clip.x, b.width + 2 * margin), height: b.height + 2 * margin });
    await page.screenshot({ path: OUT + name + '.jpg', clip, fullPage: !!clip, quality: 85 });
    await clear(page);
    console.log('✓', name);
  }
  const h = { page, role, btn, settle, go, pick, shot, OUT };
  const shots = makeShots(h);

  // El login se captura antes de entrar, en un contexto sin sesión.
  if (!only.length || only.includes('login')) {
    const anon = await open({ width: 1280, height: 800, anon: true, scale: 1.25 });
    const p = anon.page;
    await p.goto(BASE + '/login');
    await p.waitForLoadState('networkidle');
    await annotate(p, [
      { target: p.locator('#login-username'), n: 1 },
      { target: p.locator('#login-password'), n: 2 },
      { target: p.locator('#remember-me'), n: 3 },
      { target: p.locator('button[type=submit]'), n: 4 },
    ]);
    await p.screenshot({ path: OUT + '01-login.jpg', quality: 85 });
    console.log('✓ 01-login');
    await anon.browser.close();
  }

  await login(page, creds(credFile));
  let failed = 0;
  for (const [k, fn] of Object.entries(shots)) {
    if (only.length && !only.includes(k)) continue;
    try { await fn(); } catch (e) { failed++; console.error('✗', k, e.message.split('\n')[0]); }
  }
  await page.context().storageState({ path: process.env.SCA_STATE });
  await browser.close();
  process.exit(failed ? 1 : 0);
}
