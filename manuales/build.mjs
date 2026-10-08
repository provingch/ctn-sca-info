// Genera el PDF de un manual: node build.mjs profesor → ../frontend/public/pdfs/manual-profesor.pdf
import { chromium } from 'playwright';
import fs from 'node:fs';

const rol = process.argv[2] ?? 'profesor';
const src = new URL(`./${rol}.html`, import.meta.url);
const out = new URL(`../frontend/public/pdfs/manual-${rol}.pdf`, import.meta.url).pathname;

// <!-- incluir: archivo.html --> se reemplaza por ese fragmento (partes comunes a varios manuales)
const html = fs.readFileSync(src, 'utf8').replace(/<!-- incluir: ([\w.-]+) -->/g, (_, f) => fs.readFileSync(new URL(`./${f}`, import.meta.url), 'utf8'));
const tmp = new URL(`./.${rol}.build.html`, import.meta.url);
fs.writeFileSync(tmp, html);
const browser = await chromium.launch();
const page = await browser.newPage();
await page.goto(tmp.href);
await page.waitForFunction(() => [...document.querySelectorAll('figure[data-fig]')].every(f => f.dataset.listo));
const broken = await page.evaluate(() => [...document.images].filter(i => !i.naturalWidth).map(i => i.src));
if (broken.length) throw new Error('Imágenes faltantes: ' + broken.join(', '));
await page.pdf({
  path: out, format: 'A4', printBackground: true, preferCSSPageSize: true,
  displayHeaderFooter: true, headerTemplate: '<span></span>',
  footerTemplate: `<div style="font:8pt Inter,sans-serif;color:#8a93a3;width:100%;padding:0 16mm;display:flex;justify-content:space-between"><span>SCA · ${(await page.title()).split(' — ')[0]}</span><span><span class="pageNumber"></span> / <span class="totalPages"></span></span></div>`,
});
await browser.close();
fs.rmSync(tmp);
console.log('✓', out);
