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
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_PATH || undefined });
const page = await browser.newPage();
await page.goto(tmp.href);
await page.addStyleTag({ content: `@page { @bottom-left { content: ${JSON.stringify('SCA · ' + (await page.title()).split(' — ')[0])}; } } @page portada { @bottom-left { content: none; } }` });
await page.waitForFunction(() => [...document.querySelectorAll('figure[data-fig]')].every(f => f.dataset.listo));
const broken = await page.evaluate(() => [...document.images].filter(i => !i.naturalWidth).map(i => i.src));
if (broken.length) throw new Error('Imágenes faltantes: ' + broken.join(', '));
await page.pdf({
  path: out, format: 'A4', printBackground: true, preferCSSPageSize: true,
  // el pie (manual y número de página) lo pone manual.css con @page, así la portada queda sin pie
});
await browser.close();
fs.rmSync(tmp);
console.log('✓', out);
