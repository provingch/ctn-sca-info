import { useState } from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeAll, describe, expect, it, vi } from 'vitest';
import DatePicker from './DatePicker';

beforeAll(() => {
  HTMLDialogElement.prototype.showModal = function () { this.setAttribute('open', ''); };
  HTMLDialogElement.prototype.close = function () { this.removeAttribute('open'); };
});
function Controlled({ initial = '2026-09-08' }: { initial?: string }) {
  const [value, setValue] = useState(initial);
  return <DatePicker value={value} onChange={setValue} ariaLabel="Cierre Etapa 1" />;
}
const open = () => fireEvent.click(screen.getByRole('button', { name: 'Cierre Etapa 1' }));
describe('DatePicker', () => {
  it('abre con la fecha seleccionada y guarda el día local en formato ISO', () => {
    const change = vi.fn();
    render(<DatePicker value="2026-09-08" onChange={change} ariaLabel="Cierre Etapa 1" />);
    open();
    expect(screen.getByRole('button', { name: 'martes, 8 de septiembre de 2026' })).toHaveAttribute('aria-pressed', 'true');
    fireEvent.click(screen.getByRole('button', { name: 'miércoles, 9 de septiembre de 2026' }));
    expect(change).toHaveBeenCalledWith('2026-09-09');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('navega por teclado entre meses y permite elegir con el foco en el nuevo día', async () => {
    render(<Controlled initial="2026-09-30" />);
    open();
    fireEvent.keyDown(screen.getByRole('button', { name: 'miércoles, 30 de septiembre de 2026' }), { key: 'ArrowRight' });
    const next = screen.getByRole('button', { name: 'jueves, 1 de octubre de 2026' });
    await waitFor(() => expect(next).toHaveFocus());
    fireEvent.click(next);
    expect(screen.getByRole('button', { name: 'Cierre Etapa 1' })).toHaveTextContent('01/10/2026');
  });

  it('conserva el último día válido al cambiar de mes en año bisiesto', () => {
    render(<Controlled initial="2024-01-31" />);
    open();
    fireEvent.click(screen.getByRole('button', { name: 'Mes siguiente' }));
    expect(screen.getByRole('button', { name: 'jueves, 29 de febrero de 2024' })).toHaveAttribute('tabindex', '0');
  });

  it('permite borrar y seleccionar hoy sin confirmar el cierre de etapa', () => {
    render(<Controlled />);
    open();
    fireEvent.click(screen.getByRole('button', { name: 'Borrar' }));
    expect(screen.getByRole('button', { name: 'Cierre Etapa 1' })).toHaveTextContent('Seleccioná una fecha');
    open();
    fireEvent.click(screen.getByRole('button', { name: 'Hoy' }));
    expect(screen.getByRole('button', { name: 'Cierre Etapa 1' })).toHaveTextContent(new Intl.DateTimeFormat('es', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date()));
  });

  it('cierra sin cambiar la fecha y devuelve el foco', () => {
    const change = vi.fn();
    render(<DatePicker value="2026-09-08" onChange={change} ariaLabel="Cierre Etapa 1" />);
    open();
    fireEvent.click(screen.getByRole('button', { name: 'Cerrar' }));
    expect(change).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Cierre Etapa 1' })).toHaveFocus();
  });

  it('no abre cuando la etapa está bloqueada', () => {
    render(<DatePicker value="2026-09-08" onChange={vi.fn()} ariaLabel="Cierre Etapa 1" disabled />);
    open();
    expect(screen.getByRole('button', { name: 'Cierre Etapa 1' })).toBeDisabled();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
