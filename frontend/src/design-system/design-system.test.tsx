import { act, fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ActivityItem, Badge, Button, DesignSystem, Icon, Skeleton, TopNav, accentTokens, contrastRatio, designVariables, specialties, tokens } from '.';
import { Gallery } from './demo/Gallery';

describe('Contraste del sistema de diseño', () => {
  const surfaces = [tokens.color.background, tokens.color.surface1, tokens.color.surface2, tokens.color.surface3];
  it('todos los niveles de texto cumplen AA incluso para texto pequeño', () => {
    for (const text of [tokens.color.text, tokens.color.secondary, tokens.color.tertiary]) {
      for (const surface of surfaces) expect(contrastRatio(text, surface)).toBeGreaterThanOrEqual(4.5);
    }
    for (const name of ['success', 'warning', 'error', 'info'] as const) {
      expect(contrastRatio(tokens.color[name], tokens.color[`${name}Surface`])).toBeGreaterThanOrEqual(4.5);
    }
    for (const surface of surfaces) {
      expect(contrastRatio(tokens.color.controlBorder, surface)).toBeGreaterThanOrEqual(3);
      expect(contrastRatio(tokens.color.focus, surface)).toBeGreaterThanOrEqual(3);
    }
  });
  it('deriva acentos legibles para cada especialidad y colores extremos', () => {
    const colors = [...Object.values(specialties).map((entry) => entry.accent), '#000000', '#ffffff', '#ffff00', '#ff0000', '#777777'];
    for (const color of colors) {
      const result = accentTokens(color);
      expect(contrastRatio(result.accent, result['on-accent'])).toBeGreaterThanOrEqual(4.5);
      for (const surface of [...surfaces, result['accent-soft']]) expect(contrastRatio(result['accent-ink'], surface)).toBeGreaterThanOrEqual(4.5);
    }
    expect(() => accentTokens('red')).toThrow('#RRGGBB');
  });
  it('reconoce alias y usa un valor general para especialidades desconocidas', () => {
    expect(designVariables('Mecánica industrial')).toEqual(designVariables('mecanica-general'));
    expect(designVariables('desconocida')).toEqual(designVariables('general'));
  });
});

describe('Componentes base', () => {
  it('bloquea acciones durante carga y expone un nombre accesible', () => {
    const action = vi.fn();
    render(<DesignSystem><Button variant="icon" aria-label="Guardar" loading loadingLabel="Guardando" icon={<Icon name="check" />} onClick={action} /></DesignSystem>);
    const button = screen.getByRole('button', { name: 'Guardar: Guardando' });
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute('aria-busy', 'true');
    fireEvent.click(button);
    expect(action).not.toHaveBeenCalled();
  });
  it('cambia el tema local sin tocar el documento ni los estados semánticos', () => {
    const previous = document.documentElement.getAttribute('style');
    const { container, rerender } = render(<DesignSystem specialty="Informática"><Badge tone="success">Revisada</Badge></DesignSystem>);
    expect((container.firstChild as HTMLElement).style.getPropertyValue('--ds-accent')).toBe('#7a1f2b');
    rerender(<DesignSystem accent="#ffff00"><Badge tone="success">Revisada</Badge></DesignSystem>);
    expect((container.firstChild as HTMLElement).style.getPropertyValue('--ds-accent')).toBe('#ffff00');
    expect(screen.getByText('Revisada')).toHaveClass('ds-badge--success');
    expect(document.documentElement.getAttribute('style')).toBe(previous);
  });
  it('ofrece navegación real, perfil y fecha legible por máquinas', () => {
    render(<DesignSystem><TopNav brand="SCA" activeId="inicio" items={[{ id: 'inicio', label: 'Inicio', href: '#inicio' }]} user={{ name: 'María', role: 'Docente', initials: 'MG', href: '#perfil' }} /><ul><ActivityItem type="class" text="Clase registrada" timestamp="09:20" dateTime="2026-09-16T09:20:00-03:00" /></ul><Skeleton label="Cargando clases" /></DesignSystem>);
    expect(screen.getByRole('link', { name: 'Inicio' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Ver perfil de María' })).toHaveAttribute('href', '#perfil');
    expect(screen.getByText('09:20')).toHaveAttribute('datetime', '2026-09-16T09:20:00-03:00');
    expect(screen.getByRole('status')).toHaveTextContent('Cargando clases');
  });
  it('la galería aplica el acento dinámico y completa la acción de muestra', () => {
    vi.useFakeTimers();
    try {
      const { container } = render(<Gallery />);
      fireEvent.change(screen.getByLabelText('Especialidad'), { target: { value: 'electricidad' } });
      expect((container.firstChild as HTMLElement).style.getPropertyValue('--ds-accent')).toBe(specialties.electricidad.accent);
      for (const button of screen.getAllByRole('button')) expect(button.getAttribute('aria-label')).toBeTruthy();
      fireEvent.click(screen.getByRole('button', { name: 'Guardar ejemplo' }));
      expect(screen.getByRole('button', { name: 'Guardar ejemplo: Guardando' })).toBeDisabled();
      act(() => { vi.advanceTimersByTime(1500); });
      expect(screen.getByRole('button', { name: 'Guardar ejemplo' })).toBeEnabled();
      expect(screen.getByText('Ejemplo completado. No se guardaron datos reales.')).toBeInTheDocument();
    } finally { vi.useRealTimers(); }
  });
});
