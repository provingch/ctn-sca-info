// Graba los tramos de los videos tutoriales contra prod, en solo lectura (lib.mjs aborta toda escritura salvo el login).
// Uso: node scripts/grabar.mjs <rol> <credenciales.env> [tramo ...]
// Salida: public/tutoriales/<rol>/<tramo>.webm + public/tutoriales/<rol>/tramos.json (archivo, inicio en segundos, texto narrado).
import fs from 'node:fs';
import { chromium } from '../../manuales/node_modules/playwright/index.mjs';
import { BASE, creds, login, blurPII, open } from '../../manuales/lib.mjs';
import { guiones } from './guiones.mjs';

const [, , rol, credFile, ...solo] = process.argv;
const guion = guiones[rol];
if (!guion || !credFile) throw new Error('Uso: node scripts/grabar.mjs <rol> <credenciales.env> [tramo ...]');
process.env.SCA_STATE = credFile + '.state.json';
const OUT = new URL(`../public/tutoriales/${rol}/`, import.meta.url).pathname;
fs.mkdirSync(OUT, { recursive: true });
const indice = OUT + 'tramos.json';
const tramos = fs.existsSync(indice) ? JSON.parse(fs.readFileSync(indice, 'utf8')) : {};

// Sesión: se inicia una vez (sin grabar) y cada tramo la reutiliza.
{ const s = await open({ width: 1280, height: 720 }); await login(s.page, creds(credFile)); await s.ctx.storageState({ path: process.env.SCA_STATE }); await s.browser.close(); }

// Cursor visible y efecto de clic, dibujados dentro de la página (la grabación de Playwright no muestra el mouse).
const CURSOR = () => {
  const init = () => {
    if (document.getElementById('mx-cursor')) return;
    // la grabación toma píxeles CSS: zoom 1,5 = la interfaz de una pantalla de 1280 llenando 1920×1080
    document.documentElement.style.zoom = '1.5';
    const c = document.createElement('div');
    c.id = 'mx-cursor';
    c.innerHTML = '<svg width="26" height="26" viewBox="0 0 24 24"><path d="M4 2l16 9-7 2-3 7z" fill="#fff" stroke="#1d2433" stroke-width="1.6" stroke-linejoin="round"/></svg>';
    Object.assign(c.style, { position: 'fixed', left: '-40px', top: '-40px', zIndex: 2147483647, pointerEvents: 'none', filter: 'drop-shadow(0 2px 3px rgba(0,0,0,.35))' });
    document.documentElement.append(c);
    const Z = 1.5; // las posiciones fixed se escalan con el zoom de <html>
    addEventListener('mousemove', e => { c.style.left = e.clientX / Z - 3 + 'px'; c.style.top = e.clientY / Z - 2 + 'px'; }, true);
    addEventListener('mousedown', e => {
      const r = document.createElement('div');
      Object.assign(r.style, { position: 'fixed', left: e.clientX / Z - 22 + 'px', top: e.clientY / Z - 22 + 'px', width: '44px', height: '44px', borderRadius: '50%', border: '3px solid #e8590c', zIndex: 2147483646, pointerEvents: 'none', transition: 'transform .45s ease-out, opacity .45s ease-out' });
      document.documentElement.append(r);
      requestAnimationFrame(() => { r.style.transform = 'scale(1.8)'; r.style.opacity = '0'; });
      setTimeout(() => r.remove(), 500);
    }, true);
  };
  if (document.readyState === 'loading') addEventListener('DOMContentLoaded', init); else init();
};

async function grabarTramo(t) {
  const b = await chromium.launch();
  const c = await b.newContext({
    storageState: process.env.SCA_STATE, viewport: { width: 1920, height: 1080 }, deviceScaleFactor: 1, locale: 'es-PY',
    recordVideo: { dir: OUT + '.crudo/', size: { width: 1920, height: 1080 } },
  });
  await c.route('**/*', route => {
    const r = route.request();
    const escritura = !['GET', 'HEAD', 'OPTIONS'].includes(r.method()) && !/\/api\/auth\/refresh$/.test(new URL(r.url()).pathname);
    if (escritura) { console.warn('  BLOQUEADO', r.method(), new URL(r.url()).pathname); return route.abort(); }
    return route.continue();
  });
  await c.addInitScript(CURSOR);
  const t0 = Date.now();
  const p = await c.newPage();
  const h = ayudantes(p);
  let inicio;
  try {
    await t.preparar?.(h);          // navegar hasta el punto de partida (se recorta del video)
    await h.limpio();
    inicio = (Date.now() - t0) / 1000;
    await p.mouse.move(960, 600);
    await p.waitForTimeout(600);
    await t.accion(h);              // lo que se ve en el video
    await p.waitForTimeout(1200);
  } catch (e) {
    await p.screenshot({ path: OUT + '.crudo/fallo-' + t.id + '.png' }).catch(() => {});
    console.error('  url al fallar:', p.url());
    await c.close(); await b.close();
    throw e;
  }
  const video = p.video();
  await c.storageState({ path: process.env.SCA_STATE }); // el refresh rota el token: guardar el vigente
  await c.close();
  await b.close();
  fs.renameSync(await video.path(), OUT + t.id + '.webm');
  tramos[t.id] = { archivo: `tutoriales/${rol}/${t.id}.webm`, inicio: +inicio.toFixed(2), texto: t.texto };
  console.log('✓', t.id, `(inicio ${inicio.toFixed(1)} s)`);
}

function ayudantes(page) {
  const role = (r, name, exact = true) => page.getByRole(r, { name, exact }).filter({ visible: true }).first();
  const btn = name => role('button', name);
  const pausa = ms => page.waitForTimeout(ms);
  // ojo: no esperar 'networkidle' acá: la app consulta periódicamente y cada espera congelaba ~5 s el video
  async function limpio() {
    await page.waitForLoadState('domcontentloaded');
    await page.addStyleTag({ content: 'footer,a[href="#main-content"]{display:none!important}' }).catch(() => {});
    await page.evaluate(() => document.querySelectorAll('.mx-pii').forEach(e => e.remove()));
    await blurPII(page);
  }
  async function go(path) { await page.goto(BASE + path); await page.waitForLoadState('networkidle', { timeout: 8000 }).catch(() => {}); await limpio(); }
  // Mueve el cursor en curva suave hasta el centro del elemento.
  async function apuntar(loc) {
    await loc.scrollIntoViewIfNeeded();
    await pausa(250);
    await limpio();
    const b = await loc.boundingBox();
    await page.mouse.move(b.x + b.width / 2, b.y + b.height / 2, { steps: 28 });
    await pausa(350);
  }
  async function clic(loc, despues = 900) {
    await apuntar(loc);
    await page.mouse.down(); await pausa(90); await page.mouse.up();
    await pausa(despues);
    await limpio();
  }
  async function escribir(loc, texto) {
    await clic(loc, 200);
    await page.keyboard.type(texto, { delay: 45 });
    await pausa(500);
  }
  async function elegir(boton, opcion) {
    await clic(btn(boton), 500);
    const o = role('option', opcion);
    await clic(await o.count() ? o : role('option', opcion, false), 700);
  }
  async function bajar(px = 400) { await page.mouse.wheel(0, px); await pausa(900); await limpio(); }
  return { page, role, btn, pausa, limpio, go, apuntar, clic, escribir, elegir, bajar };
}

let fallos = 0;
for (const t of guion) {
  if (solo.length && !solo.includes(t.id)) continue;
  try { await grabarTramo(t); } catch (e) { fallos++; console.error('✗', t.id, e.message.split('\n')[0]); }
  fs.writeFileSync(indice, JSON.stringify(tramos, null, 1));
}
if (!fallos) fs.rmSync(OUT + '.crudo/', { recursive: true, force: true });
process.exit(fallos ? 1 : 0);
