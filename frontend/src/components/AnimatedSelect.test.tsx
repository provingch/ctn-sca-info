import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import AnimatedSelect from './AnimatedSelect';

const options = [
  { value: 'primera', label: 'Primera etapa' },
  { value: 'bloqueada', label: 'Etapa bloqueada', disabled: true },
  { value: 'segunda', label: 'Segunda etapa' },
];

describe('AnimatedSelect', () => {
  it('superpone el menú hacia arriba junto a la última fila y se recoloca al desplazar', () => {
    const { container } = render(<AnimatedSelect portal ariaLabel="Rasgos" value="" options={options} onChange={() => undefined} />);
    const trigger = screen.getByRole('button', { name: 'Rasgos' });
    const bounds = vi.spyOn(trigger, 'getBoundingClientRect').mockReturnValue({ top: window.innerHeight - 100, bottom: window.innerHeight - 50, left: 20, width: 200 } as DOMRect);
    fireEvent.click(trigger);
    const popover = screen.getByRole('listbox').parentElement!;
    expect(container.contains(popover)).toBe(false);
    expect(popover.style.position).toBe('fixed');
    expect(popover.style.top).toBe('auto');
    expect(popover.style.bottom).toBe('106px');
    bounds.mockReturnValue({ top: 20, bottom: 70, left: 20, width: 200 } as DOMRect);
    fireEvent.scroll(document);
    expect(popover.style.top).toBe('76px');
    expect(popover.style.bottom).toBe('auto');
    bounds.mockRestore();
  });

  it('muestra el valor actual y permite elegir una opción', () => {
    const onChange = vi.fn();
    render(<AnimatedSelect ariaLabel="Etapa" value="primera" options={options} onChange={onChange} />);

    fireEvent.click(screen.getByRole('button', { name: 'Etapa' }));
    expect(screen.getByRole('listbox', { name: 'Etapa' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('option', { name: 'Segunda etapa' }));

    expect(onChange).toHaveBeenCalledWith('segunda');
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
  });

  it('omite opciones deshabilitadas al navegar con teclado', () => {
    const onChange = vi.fn();
    render(<AnimatedSelect ariaLabel="Etapa" value="primera" options={options} onChange={onChange} />);
    const trigger = screen.getByRole('button', { name: 'Etapa' });

    fireEvent.keyDown(trigger, { key: 'ArrowDown' });
    fireEvent.keyDown(trigger, { key: 'ArrowDown' });
    fireEvent.keyDown(trigger, { key: 'Enter' });

    expect(onChange).toHaveBeenCalledWith('segunda');
  });

  it('no abre cuando el control está deshabilitado', () => {
    render(<AnimatedSelect ariaLabel="Curso" value="" options={options} onChange={() => undefined} disabled />);
    const trigger = screen.getByRole('button', { name: 'Curso' });

    expect(trigger).toBeDisabled();
    fireEvent.click(trigger);
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
  });
});
