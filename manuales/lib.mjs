// Navegación de solo lectura contra prod: cualquier escritura que no sea login/refresh se aborta.
import { chromium } from 'playwright';
import fs from 'node:fs';

export const BASE = process.env.SCA_BASE ?? 'https://ctn-sca.ddns.net';
const ALLOWED_WRITES = /\/api\/auth\/(login|refresh)$/;

export function creds(file) {
  return Object.fromEntries(fs.readFileSync(file, 'utf8').trim().split('\n').map(l => {
    const i = l.indexOf('='); return [l.slice(0, i), l.slice(i + 1)];
  }));
}

// Sesión reutilizable entre ejecuciones (evita un login por corrida).
const STATE = process.env.SCA_STATE;

export async function open({ width = 1366, height = 820, dark = false, anon = false, scale = 2 } = {}) {
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ storageState: !anon && STATE && fs.existsSync(STATE) ? STATE : undefined, viewport: { width, height }, deviceScaleFactor: scale, colorScheme: dark ? 'dark' : 'light', locale: 'es-PY' });
  await ctx.route('**/*', route => {
    const r = route.request();
    if (r.method() !== 'GET' && r.method() !== 'HEAD' && r.method() !== 'OPTIONS' && !ALLOWED_WRITES.test(new URL(r.url()).pathname)) {
      console.warn('BLOQUEADO', r.method(), r.url());
      return route.abort();
    }
    return route.continue();
  });
  const page = await ctx.newPage();
  return { browser, ctx, page };
}

export async function login(page, { SCA_USER, SCA_PASS }) {
  await page.goto(BASE + '/');
  await page.waitForLoadState('networkidle');
  if (!new URL(page.url()).pathname.startsWith('/login')) return;
  await page.goto(BASE + '/login');
  await page.fill('#login-username', SCA_USER);
  await page.fill('#login-password', SCA_PASS);
  await page.click('button[type=submit]');
  await page.waitForURL(u => !u.pathname.startsWith('/login'), { timeout: 20000 });
  await page.waitForLoadState('networkidle');
  if (STATE) await page.context().storageState({ path: STATE });
}

// Difumina CI, teléfonos, correos e IPs (texto visible y valores de inputs).
export async function blurPII(page) {
  await page.evaluate(() => {
    const re = [/[\w.+-]+@[\w-]+\.[\w.]+/, /\b\d{1,3}(\.\d{1,3}){3}\b/, /(\+?595|\b0)?\s?9\d{2}[\s-]?\d{3}[\s-]?\d{3}\b/, /\b\d{1,2}\.?\d{3}\.?\d{3}\b/, /\b\d{6,8}\b/];
    const hit = s => re.some(r => r.test(s));
    const blur = el => { el.style.filter = 'blur(6px)'; };
    // ¿El rectángulo se ve, o lo recorta algún contenedor con scroll/overflow?
    const visible = (el, b) => {
      if (!b.width || !b.height) return false;
      for (let p = el; p && p !== document.body; p = p.parentElement) {
        if (getComputedStyle(p).overflow === 'visible') continue;
        const c = p.getBoundingClientRect();
        if (b.bottom <= c.top || b.top >= c.bottom || b.right <= c.left || b.left >= c.right) return false;
      }
      return true;
    };
    // Solo el fragmento sensible queda tapado: una capa difuminada encima, sin tocar el DOM de React.
    const any = new RegExp(re.map(r => r.source).join('|'), 'g');
    // con zoom en <html> (videos), las coordenadas absolutas se escalan: compensarlo
    const z = parseFloat(getComputedStyle(document.documentElement).zoom) || 1;
    const w = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
    for (let n; (n = w.nextNode());) {
      if (!hit(n.textContent) || n.parentElement?.closest('.mx-ann, .mx-pii')) continue;
      for (const m of n.textContent.matchAll(any)) {
        const r = document.createRange();
        r.setStart(n, m.index); r.setEnd(n, m.index + m[0].length);
        for (const b of r.getClientRects()) {
          if (!visible(n.parentElement, b)) continue;
          const d = document.createElement('div');
          d.className = 'mx-pii';
          Object.assign(d.style, { position: 'absolute', left: (b.x + scrollX) / z - 2 + 'px', top: (b.y + scrollY) / z - 1 + 'px', width: b.width / z + 4 + 'px', height: b.height / z + 2 + 'px', backdropFilter: 'blur(8px)', background: 'rgba(214,219,228,.92)', borderRadius: '3px', zIndex: 99998, pointerEvents: 'none' });
          document.body.append(d);
        }
      }
    }
    const sensitive = /mail|correo|tel[eé]fono|phone|celular|c[eé]dula|\bci\b|documento/i;
    // columnas de tablas con datos sensibles: se tapa la celda entera
    for (const t of document.querySelectorAll('table')) {
      const heads = [...t.querySelectorAll('thead th, tr:first-child th')];
      heads.forEach((th, i) => {
        if (!sensitive.test(th.textContent)) return;
        for (const row of t.querySelectorAll('tbody tr')) { const c = row.children[i]; if (c) blur(c); }
      });
    }
    for (const i of document.querySelectorAll('input, textarea')) {
      const label = (i.labels?.[0]?.textContent ?? '') + (i.closest('label')?.textContent ?? '') + (i.getAttribute('aria-label') ?? '');
      if (hit(i.value) || sensitive.test(i.name + ' ' + i.id + ' ' + i.type + ' ' + label)) blur(i);
    }
  });
}

// marks: [{ target: Locator, n?: number, label?: string, side?: 'right'|'left'|'top'|'bottom', pad?: number }]
export async function annotate(page, marks) {
  const boxes = [];
  for (const m of marks) {
    const b = await m.target.boundingBox();
    if (!b) throw new Error('Sin caja para marca ' + (m.label ?? m.n));
    boxes.push({ ...b, x: b.x + await page.evaluate(() => scrollX), y: b.y + await page.evaluate(() => scrollY), n: m.n, label: m.label, side: m.side ?? 'right', pad: m.pad ?? 6 });
  }
  await page.evaluate(boxes => {
    document.querySelectorAll('.mx-ann').forEach(e => e.remove());
    const C = '#e8590c';
    for (const b of boxes) {
      const x = b.x - b.pad, y = b.y - b.pad, w = b.width + 2 * b.pad, h = b.height + 2 * b.pad;
      const r = document.createElement('div');
      r.className = 'mx-ann';
      Object.assign(r.style, { position: 'absolute', left: x + 'px', top: y + 'px', width: w + 'px', height: h + 'px', border: `3px solid ${C}`, borderRadius: '10px', boxShadow: '0 0 0 4px rgba(232,89,12,.18)', zIndex: 99999, pointerEvents: 'none' });
      document.body.append(r);
      if (b.n != null) {
        const d = document.createElement('div');
        d.className = 'mx-ann'; d.textContent = b.n;
        Object.assign(d.style, { position: 'absolute', left: (x + w - 16) + 'px', top: (y - 14) + 'px', width: '28px', height: '28px', borderRadius: '50%', background: C, color: '#fff', font: '700 15px/28px system-ui', textAlign: 'center', zIndex: 100000, boxShadow: '0 2px 6px rgba(0,0,0,.3)' });
        document.body.append(d);
      }
      if (b.label && b.drawLabel) {
        const l = document.createElement('div');
        l.className = 'mx-ann'; l.textContent = b.label;
        Object.assign(l.style, { position: 'absolute', background: C, color: '#fff', font: '600 14px/1.3 system-ui', padding: '6px 10px', borderRadius: '8px', maxWidth: '260px', zIndex: 100000, boxShadow: '0 2px 8px rgba(0,0,0,.25)' });
        document.body.append(l);
        const lw = l.offsetWidth, lh = l.offsetHeight, vw = innerWidth;
        let lx = { right: x + w + 12, left: x - lw - 12, top: x, bottom: x }[b.side];
        let ly = { right: y + h / 2 - lh / 2, left: y + h / 2 - lh / 2, top: y - lh - 10, bottom: y + h + 10 }[b.side];
        lx = Math.max(8, Math.min(lx, vw - lw - 8)); ly = Math.max(8, ly);
        Object.assign(l.style, { left: lx + 'px', top: ly + 'px' });
      }
    }
  }, boxes);
}

export const clear = page => page.evaluate(() => document.querySelectorAll('.mx-ann, .mx-pii').forEach(e => e.remove()));

// Caja que cubre el elemento `region` (si hay) y todas las marcas dibujadas.
export async function annotatedBox(page, region) {
  const rects = await page.evaluate(() => [...document.querySelectorAll('.mx-ann')].map(e => { const r = e.getBoundingClientRect(); return { x: r.x + scrollX, y: r.y + scrollY, width: r.width, height: r.height }; }));
  if (region) { const b = await region.boundingBox(); const [sx, sy] = await page.evaluate(() => [scrollX, scrollY]); rects.push({ ...b, x: b.x + sx, y: b.y + sy }); }
  if (!rects.length) return null;
  const x = Math.min(...rects.map(r => r.x)), y = Math.min(...rects.map(r => r.y));
  return { x, y, width: Math.max(...rects.map(r => r.x + r.width)) - x, height: Math.max(...rects.map(r => r.y + r.height)) - y };
}
