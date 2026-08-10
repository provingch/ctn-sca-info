export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'sca-theme';

export function getInitialTheme(): Theme {
  const saved = localStorage.getItem(STORAGE_KEY);
  if (saved === 'light' || saved === 'dark') return saved;
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

export function applyTheme(theme: Theme): void {
  document.documentElement.dataset.theme = theme;
  document.documentElement.style.colorScheme = theme;
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', theme === 'dark' ? '#101521' : '#f3f6f9');
}

export function persistTheme(theme: Theme): void {
  localStorage.setItem(STORAGE_KEY, theme);
  applyTheme(theme);
}

export function normalizeSpecialty(value?: string | null): string {
  const normalized = (value || 'general').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim().replace(/[_\s]+/g, '-').replace(/[^a-z0-9-]/g, '');
  const aliases: Record<string, string> = { 'construcciones-civiles': 'construcciones', 'construccion-civil': 'construcciones', 'quimica-industrial': 'quimica' };
  return aliases[normalized] || normalized || 'general';
}
