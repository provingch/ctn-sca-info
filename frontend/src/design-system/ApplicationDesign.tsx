import { useLayoutEffect, type ReactNode } from 'react';
import { useSpecialty } from '../context/SpecialtyContext';
import { THEME_CHANGE_EVENT } from '../theme/theme';
import { accentTokens, designVariables, specialties } from './tokens';
import './styles.css';
import './application.css';

// Existing cards and portalled selects carry data-specialty. Keep their local
// identity rather than making every card inherit the selected teacher's color.
const specialtyRules = Object.entries(specialties).map(([key, entry]) => {
  const values = accentTokens(entry.accent);
  const variables = Object.entries(values).map(([name, value]) => `--ds-${name}:${value}`).join(';');
  return `:root.sca-app[data-theme="dark"] [data-specialty="${key}"]{${variables};--accent:var(--ds-accent-ink);--accent-deep:var(--ds-accent-ink);--accent-soft:var(--ds-accent-soft);--accent-contrast:#10141d;--hero-tone:var(--ds-accent);--hero-contrast:var(--ds-on-accent)}`;
}).join('\n');

/** Production adapter: shares tokens with the gallery, including body portals. */
export function ApplicationDesign({ children }: { children: ReactNode }) {
  const { name } = useSpecialty();
  useLayoutEffect(() => {
    const root = document.documentElement;
    const values = designVariables(name ?? 'general') as Record<string, string>;
    const previous = Object.fromEntries(Object.keys(values).map((key) => [key, root.style.getPropertyValue(key)]));
    const hadClass = root.classList.contains('sca-app');
    const apply = () => {
      for (const [key, value] of Object.entries(values)) root.style.setProperty(key, value);
      // Preserve the existing light-mode preference and switch. Dark is the
      // approved palette; light continues to use the application's light tokens.
      if (root.dataset.theme !== 'dark') {
        const light: Record<string, string> = {
          background: 'var(--bg)', surface1: 'var(--paper)', surface2: 'var(--bg-soft)', surface3: '#e2e8f0',
          text: 'var(--ink)', secondary: '#475569', tertiary: '#526176', border: 'var(--line)', controlBorder: '#64748b', focus: 'var(--accent-deep)',
          success: '#166534', successSurface: '#dcfce7', warning: '#854d0e', warningSurface: '#fef3c7', error: '#991b1b', errorSurface: '#fee2e2', info: '#1e40af', infoSurface: '#dbeafe',
        };
        for (const [key, value] of Object.entries(light)) root.style.setProperty(`--ds-color-${key}`, value);
        root.style.setProperty('--ds-accent-ink', 'var(--accent-deep)');
        root.style.setProperty('--ds-accent-soft', 'var(--accent-soft)');
      }
      root.classList.add('sca-app');
    };
    apply();
    window.addEventListener(THEME_CHANGE_EVENT, apply);
    return () => {
      window.removeEventListener(THEME_CHANGE_EVENT, apply);
      for (const [key, value] of Object.entries(previous)) {
        if (value) root.style.setProperty(key, value); else root.style.removeProperty(key);
      }
      if (!hadClass) root.classList.remove('sca-app');
    };
  }, [name]);
  return <><style>{specialtyRules}</style>{children}</>;
}
