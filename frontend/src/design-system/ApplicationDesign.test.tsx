import { act, render, screen, fireEvent } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import { SpecialtyProvider, useSpecialty } from '../context/SpecialtyContext';
import { ApplicationDesign } from './ApplicationDesign';
import { applyTheme, THEME_CHANGE_EVENT } from '../theme/theme';

function Selection() {
  const { selectSpecialty } = useSpecialty();
  return <button onClick={() => selectSpecialty('Informática')}>Especialidad</button>;
}
afterEach(() => { sessionStorage.clear(); document.documentElement.removeAttribute('data-theme'); });
describe('Aplicación del diseño', () => {
  it('aplica y actualiza la identidad también en portales sin modificar contenido', () => {
    applyTheme('dark');
    const { unmount } = render(<SpecialtyProvider><ApplicationDesign><Selection /><h1>Inicio</h1></ApplicationDesign></SpecialtyProvider>);
    fireEvent.click(screen.getByText('Especialidad'));
    expect(document.documentElement).toHaveClass('sca-app');
    expect(document.documentElement.style.getPropertyValue('--ds-accent')).toBe('#7a1f2b');
    expect(screen.getByRole('heading')).toHaveTextContent('Inicio');
    expect(screen.queryByText(/Una base común/)).not.toBeInTheDocument();
    unmount();
    expect(document.documentElement).not.toHaveClass('sca-app');
    expect(document.documentElement.style.getPropertyValue('--ds-accent')).toBe('');
  });
  it('respeta el cambio de modo claro a oscuro', () => {
    applyTheme('light');
    render(<SpecialtyProvider><ApplicationDesign>Contenido</ApplicationDesign></SpecialtyProvider>);
    expect(document.documentElement.style.getPropertyValue('--ds-color-background')).toBe('var(--bg)');
    act(() => { applyTheme('dark'); window.dispatchEvent(new Event(THEME_CHANGE_EVENT)); });
    expect(document.documentElement.style.getPropertyValue('--ds-color-background')).toBe('#10141d');
  });
});
