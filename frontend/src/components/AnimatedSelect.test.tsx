import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import AnimatedSelect from './AnimatedSelect';

const options = [
  { value: 'primera', label: 'Primera etapa' },
  { value: 'bloqueada', label: 'Etapa bloqueada', disabled: true },
  { value: 'segunda', label: 'Segunda etapa' },
];

describe('AnimatedSelect', () => {
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
