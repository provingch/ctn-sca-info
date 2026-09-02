import { beforeEach, describe, expect, it, vi } from 'vitest';
import { applyTheme, getInitialTheme, normalizeSpecialty, persistTheme, THEME_CHANGE_EVENT } from './theme';

describe('theme', () => {
  beforeEach(() => {
    if (typeof localStorage === 'undefined' || localStorage === null) {
      // minimal in-memory localStorage shim for test environment
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      global.localStorage = (() => {
        let store: Record<string, string> = {};
        return {
          getItem: (k: string) => (Object.prototype.hasOwnProperty.call(store, k) ? store[k] : null),
          setItem: (k: string, v: string) => { store[k] = String(v); },
          removeItem: (k: string) => { delete store[k]; },
          clear: () => { store = {}; },
        };
      })();
    }
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    document.head.innerHTML = '<meta name="theme-color" content="">';
  });

  it('normaliza especialidades y alias usados por las paletas', () => {
    expect(normalizeSpecialty('Química Industrial')).toBe('quimica');
    expect(normalizeSpecialty('Mecánica Industrial')).toBe('mecanica-general');
    expect(normalizeSpecialty(null)).toBe('general');
  });

  it('prioriza el tema guardado', () => {
    localStorage.setItem('sca-theme', 'dark');
    expect(getInitialTheme()).toBe('dark');
  });

  it('aplica y comunica el cambio de tema', () => {
    const listener = vi.fn();
    window.addEventListener(THEME_CHANGE_EVENT, listener);

    persistTheme('light');

    expect(document.documentElement.dataset.theme).toBe('light');
    expect(localStorage.getItem('sca-theme')).toBe('light');
    expect(document.querySelector('meta[name="theme-color"]')).toHaveAttribute('content', '#f3f6f9');
    expect(listener).toHaveBeenCalledOnce();
    window.removeEventListener(THEME_CHANGE_EVENT, listener);
  });

  it('configura el color de navegador del tema oscuro', () => {
    applyTheme('dark');
    expect(document.documentElement.style.colorScheme).toBe('dark');
    expect(document.querySelector('meta[name="theme-color"]')).toHaveAttribute('content', '#101521');
  });
});
