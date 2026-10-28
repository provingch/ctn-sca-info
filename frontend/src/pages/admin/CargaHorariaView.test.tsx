import { render, screen } from '@testing-library/react';
import { expect, it } from 'vitest';
import CargaHorariaView from './CargaHorariaView';
import type { CargaHorariaItem } from '../../api/admin';

const data: CargaHorariaItem = {
  usuarioId: 1,
  profesorNombre: 'Ana Pérez',
  catalogo: [
    { id: 1, numero: 1, etiqueta: 'M', horaInicio: '07:00', horaFin: '07:35' },
    { id: 2, numero: 2, etiqueta: 'M', horaInicio: '07:40', horaFin: '08:15' },
  ],
  slots: [
    { id: 1, diaSemana: 1, horaCatedraId: 1, horaCatedraNumero: 1, horaInicio: '07:00', horaFin: '07:35', materiaNombre: 'Matemática', comun: true, especialidadId: 2, especialidadNombre: 'Informática', cursoNivel: 1, cursoSeccion: 'A', salaNombre: null },
    { id: 2, diaSemana: 2, horaCatedraId: 2, horaCatedraNumero: 2, horaInicio: '07:40', horaFin: '08:15', materiaNombre: 'Redes', comun: false, especialidadId: 2, especialidadNombre: 'Informática', cursoNivel: 3, cursoSeccion: 'B', salaNombre: 'Lab 1' },
  ],
};

it('pinta comunes en gris, específicas por especialidad y resume las horas', () => {
  const { container } = render(<CargaHorariaView data={data} />);
  expect(container.querySelector('.carga-slot-comun')).toHaveTextContent('Matemática');
  const especifica = container.querySelector('.carga-slot-especifica');
  expect(especifica).toHaveTextContent('Redes');
  expect(especifica).toHaveAttribute('data-specialty', 'informatica');
  expect(screen.getByTestId('carga-total')).toHaveTextContent('2');
});
