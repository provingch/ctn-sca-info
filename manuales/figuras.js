// <figure data-fig="nombre" data-url="/padre" data-legend="Título: texto|…" data-pie="…"> → captura enmarcada + leyenda.
// La captura va dentro de una ventana de navegador (o de un celular, con class="celular"); en cada ítem de
// la leyenda, lo que va antes de ": " sale en negrita. Las capturas altas llevan la leyenda al costado.
const base = document.body.dataset.img;
const sitio = 'ctn-sca.ddns.net';
for (const f of document.querySelectorAll('figure[data-fig]')) {
  const marco = Object.assign(document.createElement('div'), { className: 'marco' });
  const barra = Object.assign(document.createElement('div'), { className: 'barra' });
  barra.append(...[0, 1, 2].map(() => document.createElement('i')), Object.assign(document.createElement('span'), { textContent: sitio + (f.dataset.url ?? '') }));
  const img = Object.assign(document.createElement('img'), { src: base + f.dataset.fig + '.jpg', alt: '' });
  marco.append(barra, img);
  const cuadro = document.createElement('div');
  cuadro.append(marco);
  if (f.dataset.pie) cuadro.append(Object.assign(document.createElement('figcaption'), { textContent: f.dataset.pie }));
  f.append(cuadro);
  const items = (f.dataset.legend ?? '').split('|').filter(Boolean);
  if (items.length) {
    const ol = Object.assign(document.createElement('ol'), { className: 'leyenda' + (items.length < 3 ? ' una' : '') });
    for (const t of items) {
      const li = document.createElement('li');
      const i = t.indexOf(': ');
      if (i > 0) li.append(Object.assign(document.createElement('b'), { textContent: t.slice(0, i) }), t[i + 2].toUpperCase() + t.slice(i + 3));
      else li.textContent = t;
      ol.append(li);
    }
    f.append(ol);
  }
  img.decode().then(() => {
    const r = img.naturalHeight / img.naturalWidth;
    if (items.length && (f.classList.contains('celular') || r > 1.1)) f.classList.add('lado', 'angosta');
    else if (items.length && r > 0.62) f.classList.add('lado');
    f.dataset.listo = '1';
  });
}
