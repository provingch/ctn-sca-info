// Graba un tutorial completo en UNA toma continua, al ritmo de la narración: cada paso arranca con su frase y la
// grabación espera a que la frase termine antes de pasar al siguiente. Pensado para el SCA local (manuales/datos-demo/levantar-local.sh):
// ahí las acciones se guardan de verdad. Contra prod, lib.mjs bloquea las escrituras.
//
// Uso: SCA_BASE=http://localhost:5173 node scripts/grabar-tutorial.mjs <rol> [--ensayo] [escena ...]
//   --ensayo  recorre el guion rápido, sin grabar ni esperar la voz, y guarda captura de cada paso que falla.
// Entrada: scripts/guiones/<rol>.mjs y public/tutoriales/<rol>/voz/linea.json (de narrar-tutorial.mjs).
// Salida:  public/tutoriales/<rol>/video.webm + grabacion.json (momento real de cada escena y cada paso).
import fs from 'node:fs';
import { execFileSync } from 'node:child_process';
import { chromium } from '../../manuales/node_modules/playwright/index.mjs';
import { BASE, LOCAL, creds, login, open } from '../../manuales/lib.mjs';

const [, , rol, ...resto] = process.argv;
const ENSAYO = resto.includes('--ensayo');
const solo = resto.filter(a => !a.startsWith('--'));
const { escenas } = await import(`./guiones/${rol}.mjs`);
const CRED = new URL(`../../.credenciales/${LOCAL ? 'local/' : ''}${rol}.env`, import.meta.url).pathname;
const DIR = new URL(`../public/tutoriales/${rol}/`, import.meta.url).pathname;
fs.mkdirSync(DIR + '.crudo', { recursive: true });

// duración de cada frase (de la narración); en ensayo, fija
const voz = !ENSAYO ? JSON.parse(fs.readFileSync(DIR + 'voz/linea.json', 'utf8')) : null;
const durDe = id => ENSAYO ? 0.3 : (voz.pasos.find(p => p.id === id)?.dur ?? (() => { throw new Error(`Sin voz para ${id}: correr narrar-tutorial.mjs`); })());

// sesión iniciada antes de grabar (no se ve en el video)
process.env.SCA_STATE = CRED + '.state.json';
{ const s = await open({ width: 1280, height: 720, anon: true }); await login(s.page, creds(CRED)); await s.ctx.storageState({ path: process.env.SCA_STATE }); await s.browser.close(); }

// Cursor visible, efecto de clic y difuminado de datos sensibles en cada cambio del DOM.
const ESCENARIO = () => {
  const Z = 1.5;
  const init = () => {
    if (document.getElementById('mx-cursor')) return;
    // la grabación toma píxeles CSS: zoom 1,5 = la interfaz de una pantalla de 1280 llenando 1920×1080
    document.documentElement.style.zoom = String(Z);
    const c = document.createElement('div');
    c.id = 'mx-cursor';
    c.innerHTML = '<svg width="26" height="26" viewBox="0 0 24 24"><path d="M4 2l16 9-7 2-3 7z" fill="#fff" stroke="#1d2433" stroke-width="1.6" stroke-linejoin="round"/></svg>';
    Object.assign(c.style, { position: 'fixed', left: '-40px', top: '-40px', zIndex: 2147483647, pointerEvents: 'none', filter: 'drop-shadow(0 2px 3px rgba(0,0,0,.35))' });
    document.documentElement.append(c);
    const ultimo = sessionStorage.getItem('mx-cursor');
    if (ultimo) { const [x, y] = ultimo.split(','); c.style.left = x; c.style.top = y; }
    addEventListener('mousemove', e => { c.style.left = e.clientX / Z - 3 + 'px'; c.style.top = e.clientY / Z - 2 + 'px'; sessionStorage.setItem('mx-cursor', c.style.left + ',' + c.style.top); }, true);
    addEventListener('mousedown', e => {
      const r = document.createElement('div');
      Object.assign(r.style, { position: 'fixed', left: e.clientX / Z - 22 + 'px', top: e.clientY / Z - 22 + 'px', width: '44px', height: '44px', borderRadius: '50%', border: '3px solid #e8590c', zIndex: 2147483646, pointerEvents: 'none', transition: 'transform .45s ease-out, opacity .45s ease-out' });
      document.documentElement.append(r);
      requestAnimationFrame(() => { r.style.transform = 'scale(1.8)'; r.style.opacity = '0'; });
      setTimeout(() => r.remove(), 500);
    }, true);
  };
  const sensible = /mail|correo|tel[eé]fono|phone|celular|c[eé]dula|\bci\b|documento/i;
  const dato = /[\w.+-]+@[\w-]+\.[\w.]+|\b\d{1,3}(\.\d{1,3}){3}\b|\b\d{1,2}\.?\d{3}\.?\d{3}\b|\b\d{6,8}\b|(\+?595|\b0)?\s?9\d{2}[\s-]?\d{3}[\s-]?\d{3}\b/;
  const tapar = el => { if (el && el.style.filter !== 'blur(7px)') el.style.filter = 'blur(7px)'; };
  const barrer = () => {
    for (const i of document.querySelectorAll('input, textarea')) {
      const label = (i.labels?.[0]?.textContent ?? '') + (i.closest('label')?.textContent ?? '') + (i.getAttribute('aria-label') ?? '') + (i.placeholder ?? '');
      if ((sensible.test(i.name + ' ' + i.id + ' ' + i.type + ' ' + label) && !/buscar/i.test(label)) || dato.test(i.value)) tapar(i);
    }
    for (const t of document.querySelectorAll('table')) {
      [...t.querySelectorAll('thead th')].forEach((th, k) => {
        if (sensible.test(th.textContent)) for (const row of t.querySelectorAll('tbody tr')) tapar(row.children[k]);
      });
    }
    const w = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
    for (let n; (n = w.nextNode());) if (dato.test(n.textContent) && n.parentElement && !n.parentElement.closest('#mx-cursor')) tapar(n.parentElement);
  };
  new MutationObserver(barrer).observe(document, { subtree: true, childList: true, characterData: true });
  setInterval(barrer, 300); // por si un valor de input cambia sin mutación del DOM
  if (document.readyState === 'loading') addEventListener('DOMContentLoaded', init); else init();
};

const browser = await chromium.launch();
const ctx = await browser.newContext({
  storageState: process.env.SCA_STATE, viewport: { width: 1920, height: 1080 }, locale: 'es-PY', acceptDownloads: true,
  ...(ENSAYO ? {} : { recordVideo: { dir: DIR + '.crudo/', size: { width: 1920, height: 1080 } } }),
});
await ctx.addInitScript(ESCENARIO);
if (!LOCAL) await ctx.route('**/*', r => !['GET', 'HEAD', 'OPTIONS'].includes(r.request().method()) && !/\/api\/auth\/refresh$/.test(r.request().url()) ? r.abort() : r.continue());
const t0 = Date.now();
const page = await ctx.newPage();
const ahora = () => (Date.now() - t0) / 1000;
const h = ayudantes(page);

const linea = { escenas: [], pasos: [] };
let inicioVideo = null, fallos = 0;
for (const e of escenas) {
  if (solo.length && !solo.includes(e.id)) continue;
  try {
    await e.preparar?.(h); // navegación previa de la escena: sí se ve, la cubre la primera frase
  } catch (err) { fallos++; await falla(e.id + '-preparar', err); continue; }
  if (inicioVideo === null) { inicioVideo = ahora(); await page.mouse.move(960, 620); }
  linea.escenas.push({ id: e.id, titulo: e.titulo, t: ahora() - inicioVideo });
  for (const [k, p] of e.pasos.entries()) {
    const id = `${e.id}.${k + 1}`;
    const inicio = ahora();
    linea.pasos.push({ id, t: inicio - inicioVideo });
    try { await p.hacer?.(h); } catch (err) { fallos++; await falla(id, err); }
    const resto = durDe(id) + (p.pausa ?? 0.35) - (ahora() - inicio);
    if (resto > 0) await page.waitForTimeout(resto * 1000);
  }
}
await page.waitForTimeout(ENSAYO ? 200 : 1500);
const video = ENSAYO ? null : page.video();
await ctx.close();
await browser.close();

if (!ENSAYO) {
  // se corta lo previo a la primera escena (carga inicial): no queda guardado en ningún lado
  execFileSync('ffmpeg', ['-v', 'error', '-ss', inicioVideo.toFixed(2), '-i', await video.path(), '-c:v', 'libvpx', '-b:v', '6M', '-an', '-y', DIR + 'video.webm']);
  fs.rmSync(await video.path());
  linea.duracion = +execFileSync('ffprobe', ['-v', 'error', '-show_entries', 'format=duration', '-of', 'csv=p=0', DIR + 'video.webm'], { encoding: 'utf8' }).trim();
  fs.writeFileSync(DIR + 'grabacion.json', JSON.stringify(linea, null, 1));
  console.log(`✓ ${rol}: ${linea.escenas.length} escenas, ${linea.pasos.length} pasos, ${linea.duracion.toFixed(1)} s`);
}
console.log(fallos ? `✗ ${fallos} paso(s) fallaron (capturas en ${DIR}.crudo/)` : '✓ sin fallos');
process.exit(fallos ? 1 : 0);

async function falla(id, err) {
  console.error('✗', id, err.message.split('\n')[0]);
  await page.screenshot({ path: `${DIR}.crudo/fallo-${id}.png` }).catch(() => {});
}

function ayudantes(page) {
  const vis = l => l.filter({ visible: true }).first();
  const role = (r, name, exact = true) => vis(page.getByRole(r, { name, exact }));
  const btn = name => role('button', name);
  const pausa = ms => page.waitForTimeout(ENSAYO ? Math.min(ms, 400) : ms);
  const listo = () => page.waitForLoadState('domcontentloaded');
  async function go(path) { await page.goto(BASE + path); await page.waitForLoadState('networkidle', { timeout: 8000 }).catch(() => {}); }
  async function apuntar(loc) {
    await loc.waitFor({ timeout: 10000 });
    // solo si queda fuera de la franja segura (debajo del encabezado y arriba del pie fijo, que tapa lo de abajo);
    // desplazar sin necesidad cierra listas desplegables abiertas
    const movio = await loc.evaluate(el => {
      const r = el.getBoundingClientRect();
      if (r.top >= 110 && r.bottom <= innerHeight - 110) return false;
      el.scrollIntoView({ block: 'center', behavior: 'instant' });
      return true;
    });
    await page.waitForTimeout(movio ? 350 : 60);
    const b = await loc.boundingBox();
    await page.mouse.move(b.x + b.width / 2, b.y + b.height / 2, { steps: ENSAYO ? 2 : 24 });
    await pausa(300);
  }
  async function clic(loc, despues = 700) {
    await apuntar(loc);
    await page.mouse.down(); await pausa(80); await page.mouse.up();
    await listo();
    await pausa(despues);
  }
  async function escribir(loc, texto) {
    await clic(loc, 150);
    await page.keyboard.type(texto, { delay: ENSAYO ? 0 : 40 });
    await pausa(300);
  }
  async function elegir(boton, opcion) {
    await clic(typeof boton === 'string' ? btn(boton) : boton, 400);
    const o = role('option', opcion);
    await clic(await o.count() ? o : role('option', opcion, false), 600);
  }
  async function bajar(px = 400) { await page.mouse.wheel(0, px); await pausa(700); }
  // texto visible en algún lado de la página (para esperar avisos o resultados)
  const ver = (texto, timeout = 8000) => vis(page.getByText(texto)).waitFor({ timeout });
  return { page, vis, role, btn, pausa, go, apuntar, clic, escribir, elegir, bajar, ver };
}
