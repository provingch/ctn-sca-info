// <figure data-fig="nombre" data-legend="uno|dos"> → imagen + leyenda numerada.
// Las capturas altas llevan la leyenda al costado para no desperdiciar la página.
const base = document.body.dataset.img;
for (const f of document.querySelectorAll('figure[data-fig]')) {
  const img = document.createElement('img');
  img.src = base + f.dataset.fig + '.jpg';
  img.alt = '';
  f.append(img);
  const items = (f.dataset.legend ?? '').split('|').filter(Boolean);
  if (items.length) {
    const ol = document.createElement('ol');
    ol.className = 'leyenda';
    for (const t of items) ol.append(Object.assign(document.createElement('li'), { textContent: t }));
    f.append(ol);
  }
  img.decode().then(() => {
    const r = img.naturalHeight / img.naturalWidth;
    if (r > 1.1) f.classList.add('muy-alta');
    else if (r > 0.55 && items.length) f.classList.add('alta');
    else if (r < 0.2) f.classList.add('tira');
    f.dataset.listo = '1';
  });
}
