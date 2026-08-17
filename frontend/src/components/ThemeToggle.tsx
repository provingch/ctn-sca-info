import { useEffect, useState } from 'react';
import { applyTheme, getInitialTheme, persistTheme, THEME_CHANGE_EVENT, type Theme } from '../theme/theme';

export default function ThemeToggle({ compact = false }: { compact?: boolean }) {
  const [theme, setTheme] = useState<Theme>(getInitialTheme);
  const isDark = theme === 'dark';

  useEffect(() => {
    applyTheme(theme);
  }, [theme]);

  useEffect(() => {
    const syncTheme = (event: Event) => setTheme((event as CustomEvent<Theme>).detail || getInitialTheme());
    const syncStoredTheme = () => setTheme(getInitialTheme());
    window.addEventListener(THEME_CHANGE_EVENT, syncTheme);
    window.addEventListener('storage', syncStoredTheme);
    return () => {
      window.removeEventListener(THEME_CHANGE_EVENT, syncTheme);
      window.removeEventListener('storage', syncStoredTheme);
    };
  }, []);

  function toggle() {
    const next = isDark ? 'light' : 'dark';
    setTheme(next);
    persistTheme(next);
  }
  return <button className={`theme-toggle ${compact ? 'compact' : ''}`} type="button" onClick={toggle} aria-label={isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'} title={isDark ? 'Modo claro' : 'Modo oscuro'}>
    {isDark ? <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 4V2m0 20v-2m8-8h2M2 12h2m13.66-5.66 1.42-1.42M4.92 19.08l1.42-1.42m11.32 0 1.42 1.42M4.92 4.92l1.42 1.42M16.5 12a4.5 4.5 0 1 1-9 0 4.5 4.5 0 0 1 9 0Z" /></svg> : <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20.5 14.2A8.5 8.5 0 0 1 9.8 3.5 8.5 8.5 0 1 0 20.5 14.2Z" /></svg>}
    {!compact && <span>{isDark ? 'Claro' : 'Oscuro'}</span>}
  </button>;
}
