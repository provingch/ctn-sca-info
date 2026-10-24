import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { normalizeSpecialty } from '../theme/theme';

interface SpecialtySelection {
  id: number | null;
  name: string | null;
}

interface SpecialtyContextValue extends SpecialtySelection {
  /** Color de un apartado puntual (planilla, clase, panel de una especialidad). */
  selectSpecialty: (name: string, id?: number | null) => void;
  /** Sale del apartado: vuelve al color base del usuario. */
  resetSpecialty: () => void;
  /**
   * Color base del usuario para las pantallas generales: su única especialidad, o
   * null (institucional) si tiene varias o ninguna. Lo fija la identidad del usuario.
   */
  setBaseSpecialty: (name: string | null, id?: number | null) => void;
}

const NO_SELECTION: SpecialtySelection = { id: null, name: null };
const SpecialtyContext = createContext<SpecialtyContextValue | undefined>(undefined);

export function SpecialtyProvider({ children }: { children: ReactNode }) {
  const [base, setBase] = useState<SpecialtySelection>(NO_SELECTION);
  const [override, setOverride] = useState<SpecialtySelection | null>(null);
  const selection = override ?? base;

  const selectSpecialty = useCallback((name: string, id: number | null = null) => {
    setOverride((current) => current?.name === name && current.id === id ? current : { id, name });
  }, []);
  const resetSpecialty = useCallback(() => setOverride(null), []);
  const setBaseSpecialty = useCallback((name: string | null, id: number | null = null) => {
    setBase((current) => current.name === name && current.id === (name ? id : null) ? current : { id: name ? id : null, name });
  }, []);

  // La paleta nunca se persiste: cada pantalla general arranca en el color base del usuario.
  useEffect(() => {
    document.documentElement.dataset.specialty = normalizeSpecialty(selection.name);
  }, [selection]);

  const value = useMemo<SpecialtyContextValue>(() => ({
    ...selection,
    selectSpecialty,
    resetSpecialty,
    setBaseSpecialty,
  }), [selection, selectSpecialty, resetSpecialty, setBaseSpecialty]);

  return <SpecialtyContext.Provider value={value}>{children}</SpecialtyContext.Provider>;
}

// oxlint-disable-next-line react/only-export-components -- hook y provider comparten el mismo contexto privado.
export function useSpecialty(): SpecialtyContextValue {
  const context = useContext(SpecialtyContext);
  if (!context) throw new Error('useSpecialty debe usarse dentro de <SpecialtyProvider>');
  return context;
}
