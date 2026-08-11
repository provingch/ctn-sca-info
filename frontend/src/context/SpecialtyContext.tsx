import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { normalizeSpecialty } from '../theme/theme';

const STORAGE_KEY = 'sca-session-specialty';

interface SpecialtySelection {
  id: number | null;
  name: string | null;
}

interface SpecialtyContextValue extends SpecialtySelection {
  selectSpecialty: (name: string, id?: number | null) => void;
  resetSpecialty: () => void;
}

const DEFAULT_SELECTION: SpecialtySelection = { id: null, name: null };
const SpecialtyContext = createContext<SpecialtyContextValue | undefined>(undefined);

function readSelection(): SpecialtySelection {
  try {
    const stored = sessionStorage.getItem(STORAGE_KEY);
    if (!stored) return DEFAULT_SELECTION;
    const parsed = JSON.parse(stored) as Partial<SpecialtySelection>;
    return typeof parsed.name === 'string'
      ? { id: typeof parsed.id === 'number' ? parsed.id : null, name: parsed.name }
      : DEFAULT_SELECTION;
  } catch {
    return DEFAULT_SELECTION;
  }
}

export function SpecialtyProvider({ children }: { children: ReactNode }) {
  const [selection, setSelection] = useState<SpecialtySelection>(readSelection);
  const selectSpecialty = useCallback((name: string, id: number | null = null) => {
    setSelection((current) => current.name === name && current.id === id ? current : { id, name });
  }, []);
  const resetSpecialty = useCallback(() => setSelection(DEFAULT_SELECTION), []);

  useEffect(() => {
    document.documentElement.dataset.specialty = normalizeSpecialty(selection.name);
    if (selection.name) sessionStorage.setItem(STORAGE_KEY, JSON.stringify(selection));
    else sessionStorage.removeItem(STORAGE_KEY);
  }, [selection]);

  const value = useMemo<SpecialtyContextValue>(() => ({
    ...selection,
    selectSpecialty,
    resetSpecialty,
  }), [selection, selectSpecialty, resetSpecialty]);

  return <SpecialtyContext.Provider value={value}>{children}</SpecialtyContext.Provider>;
}

// oxlint-disable-next-line react/only-export-components -- hook y provider comparten el mismo contexto privado.
export function useSpecialty(): SpecialtyContextValue {
  const context = useContext(SpecialtyContext);
  if (!context) throw new Error('useSpecialty debe usarse dentro de <SpecialtyProvider>');
  return context;
}
