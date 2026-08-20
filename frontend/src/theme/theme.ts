export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'sca-theme';
export const THEME_CHANGE_EVENT = 'sca-theme-change';

export function getInitialTheme(): Theme {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === 'light' || saved === 'dark') return saved;
  } catch {
    // Algunos navegadores embebidos bloquean el acceso a localStorage.
    // El tema no debe impedir que se pueda iniciar sesión.
  }
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

export function applyTheme(theme: Theme): void {
  document.documentElement.dataset.theme = theme;
  document.documentElement.style.colorScheme = theme;
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', theme === 'dark' ? '#101521' : '#f3f6f9');
}

export function persistTheme(theme: Theme): void {
  try {
    localStorage.setItem(STORAGE_KEY, theme);
  } catch {
    // El cambio se aplica para la pestaña actual aunque no pueda persistirse.
  }
  applyTheme(theme);
  window.dispatchEvent(new CustomEvent<Theme>(THEME_CHANGE_EVENT, { detail: theme }));
}

export function normalizeSpecialty(value?: string | null): string {
  const normalized = (value || 'general').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim().replace(/[_\s]+/g, '-').replace(/[^a-z0-9-]/g, '').replace(/-+/g, '-').replace(/^-|-$/g, '');
  const aliases: Record<string, string> = {
    'construcciones-civiles': 'construcciones',
    'construccion-civil': 'construcciones',
    'quimica-industrial': 'quimica',
    'mecanica-industrial': 'mecanica-general',
    'mecanica-general': 'mecanica-general'
  };
  return aliases[normalized] || normalized || 'general';
}
