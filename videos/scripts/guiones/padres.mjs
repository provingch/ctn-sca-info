// Tutorial para padres y encargados. Datos de ejemplo: Thiago (3°A) con notas en Algorítmica y Laboratorio Java,
// asistencia y una nota de conducta.
const inicio = async h => { await h.go('/padre'); await h.clic(h.btn('Primera etapa'), 900); };

export const escenas = [
  {
    id: 'resumen', titulo: 'Resumen de tu hijo',
    preparar: h => h.go('/padre'),
    pasos: [
      { voz: 'Este es el tutorial para padres y encargados. Al entrar ves a tu hijo o hija con su promedio general.', hacer: h => h.apuntar(h.vis(h.page.getByRole('button', { name: /ESTIGARRIBIA DELGADILLO/ }))) },
      { voz: 'Si tenés más de un hijo en el colegio, aparecen todos en estas tarjetas: tocá el que quieras ver.', hacer: h => h.pausa(300) },
      { voz: 'El resumen muestra el promedio general, el de cada etapa y cuántas tareas están por revisar: sin entregar o sin calificar.', hacer: h => h.apuntar(h.vis(h.page.getByText('Promedio general', { exact: true }))) },
      { voz: 'Elegí un mes y tocá Descargar reporte mensual para bajarlo en PDF.', hacer: async h => { await h.elegir('Mes del reporte', 'Octubre'); await h.clic(h.btn('Descargar reporte mensual'), 2200); } },
      { voz: 'La libreta completa se habilita recién cuando el colegio cierra la segunda etapa. Hasta entonces, el botón aparece gris.', hacer: h => h.apuntar(h.btn('Descargar libreta')) },
    ],
  },
  {
    id: 'materias', titulo: 'Materias y tareas',
    preparar: inicio,
    pasos: [
      { voz: 'Más abajo elegís la etapa. Todo lo que sigue se actualiza según la etapa elegida.', hacer: h => h.apuntar(h.vis(h.page.getByRole('group', { name: 'Filtrar materias por etapa' }))) },
      { voz: 'Hay una tarjeta por materia, con su nota y su porcentaje. Tocá una para ver sus tareas.', hacer: h => h.clic(h.vis(h.page.getByRole('button', { name: /^Materia Algorítmica/ })), 1400) },
      { voz: 'Las tareas están agrupadas por mes. Cada una muestra si está calificada, entregada sin calificar, no entregada o pendiente, y los puntos que obtuvo.', hacer: async h => { await h.bajar(420); await h.pausa(600); } },
      { voz: 'Prestá atención a las no entregadas: son las que bajan el promedio.', hacer: h => h.apuntar(h.vis(h.page.getByText(/No entregada/).first())) },
      { voz: 'Abajo podés desplegar cómo se calcula el promedio.', hacer: async h => { const c = h.vis(h.page.getByText('¿Cómo se calcula el promedio?')); await h.clic(c, 1200); } },
    ],
  },
  {
    id: 'conducta', titulo: 'Notas de conducta',
    preparar: async h => { await inicio(h); await h.page.getByRole('region', { name: 'Notas de conducta', exact: true }).scrollIntoViewIfNeeded(); },
    pasos: [
      { voz: 'Al final están las notas de conducta que los profesores registraron en clase: fecha, materia, profesor y el código con su descripción.', hacer: h => h.apuntar(h.vis(h.page.getByRole('region', { name: 'Notas de conducta', exact: true }).getByRole('table'))) },
      { voz: 'Podés filtrarlas por materia y por rango de fechas.', hacer: async h => { await h.clic(h.btn('Materia de la nota de conducta'), 900); await h.page.keyboard.press('Escape'); await h.apuntar(h.btn('Conducta desde')); } },
    ],
  },
  {
    id: 'avisos', titulo: 'Avisos en el celular',
    preparar: h => h.go('/profile'),
    pasos: [
      { voz: 'Para que te lleguen los avisos del colegio al celular, entrá a Mi perfil, pestaña Aplicación.', hacer: h => h.clic(h.vis(h.page.getByRole('button', { name: /^Aplicación/ })), 1200) },
      { voz: 'Desde ahí instalás el sistema como una aplicación y activás las notificaciones. Se configura en cada dispositivo.', hacer: h => h.apuntar(h.vis(h.page.getByRole('button', { name: /Activar notificaciones|Instalar aplicación/ }))) },
      { voz: 'Si tenés dudas, el manual completo está en tu nombre, arriba a la derecha, en la opción Manual.', hacer: async h => { await h.clic(h.vis(h.page.getByRole('button', { name: /^Menú de/ })), 900); await h.apuntar(h.vis(h.page.getByText('Manual', { exact: true }))); await h.page.keyboard.press('Escape'); } },
    ],
  },
];
