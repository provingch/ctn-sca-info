import type { CSSProperties } from 'react';
import { normalizeSpecialty } from '../theme/theme';

/** Single source of truth. Rem units respect the user's browser font size. */
export const tokens = {
  color: {
    background: '#10141d', surface1: '#181e29', surface2: '#202836', surface3: '#293344',
    text: '#f3f5fa', secondary: '#c0cada', tertiary: '#a5b2c6',
    border: '#3d4a60', controlBorder: '#79879d', focus: '#e1eaff',
    success: '#89e0b2', successSurface: '#17382c',
    warning: '#f5cf7a', warningSurface: '#3b301b',
    error: '#ffaaaa', errorSurface: '#40252c',
    info: '#a6d3ff', infoSurface: '#20344a',
  },
  font: { family: 'Inter, Aptos, "Segoe UI", system-ui, sans-serif', caption: '.75rem', small: '.875rem', body: '1rem', h3: '1.25rem', h2: '1.5rem', h1: '2rem', display: '2.75rem' },
  weight: { regular: '400', medium: '500', semibold: '600', bold: '700' },
  leading: { tight: '1.2', heading: '1.3', body: '1.6' },
  space: { 0: '0', 1: '.25rem', 2: '.5rem', 3: '.75rem', 4: '1rem', 5: '1.25rem', 6: '1.5rem', 8: '2rem', 10: '2.5rem', 12: '3rem', 16: '4rem' },
  radius: { small: '.5rem', medium: '.75rem', large: '1rem', pill: '999px' },
  motion: { fast: '140ms', normal: '200ms' },
} as const;

/** Institutional identities from the existing specialty theme. */
export const specialties = {
  general: { label: 'General', accent: '#5267f7' },
  informatica: { label: 'Informática', accent: '#7a1f2b' },
  construcciones: { label: 'Construcciones civiles', accent: '#806826' },
  quimica: { label: 'Química industrial', accent: '#52677a' },
  electronica: { label: 'Electrónica', accent: '#626975' },
  'mecanica-automotriz': { label: 'Mecánica automotriz', accent: '#233c75' },
  'mecanica-general': { label: 'Mecánica industrial', accent: '#1e5a38' },
  electromecanica: { label: 'Electromecánica', accent: '#3347c5' },
  electricidad: { label: 'Electricidad', accent: '#1f7399' },
} as const;

const rgb = (hex: string) => [1, 3, 5].map((offset) => parseInt(hex.slice(offset, offset + 2), 16));
function luminance(hex: string) {
  const channels = rgb(hex).map((channel) => { const value = channel / 255; return value <= .04045 ? value / 12.92 : ((value + .055) / 1.055) ** 2.4; });
  return channels[0] * .2126 + channels[1] * .7152 + channels[2] * .0722;
}
export function contrastRatio(a: string, b: string) {
  const first = luminance(a), second = luminance(b);
  return (Math.max(first, second) + .05) / (Math.min(first, second) + .05);
}
function mix(a: string, b: string, amount: number) {
  const other = rgb(b);
  return '#' + rgb(a).map((channel, index) => Math.round(channel * (1 - amount) + other[index] * amount).toString(16).padStart(2, '0')).join('');
}
export function accentTokens(accent: string) {
  if (!/^#[\da-f]{6}$/i.test(accent)) throw new Error('El acento debe usar el formato hexadecimal #RRGGBB.');
  const soft = mix(tokens.color.surface1, accent, .18);
  let ink = accent;
  for (let step = 0; step <= 100; step++) {
    ink = mix(accent, '#ffffff', step / 100);
    if ([tokens.color.surface3, soft].every((surface) => contrastRatio(ink, surface) >= 4.5)) break;
  }
  const onAccent = contrastRatio('#ffffff', accent) >= contrastRatio('#000000', accent) ? '#ffffff' : '#000000';
  return { accent, 'accent-ink': ink, 'accent-soft': soft, 'on-accent': onAccent };
}

export function designVariables(specialty = 'general', accent?: string): CSSProperties {
  const key = normalizeSpecialty(specialty) as keyof typeof specialties;
  const values: Record<string, string> = {};
  for (const [group, entries] of Object.entries(tokens)) {
    for (const [name, value] of Object.entries(entries)) values[`--ds-${group}-${name}`] = value;
  }
  for (const [name, value] of Object.entries(accentTokens(accent ?? specialties[key]?.accent ?? specialties.general.accent))) values[`--ds-${name}`] = value;
  return values as CSSProperties;
}
