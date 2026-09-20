export type CoberturaTema =
  | { estado: 'cumplido'; fecha: string | null }
  | { estado: 'no-cumplido' }
  | { estado: 'pendiente' };

/**
 * Estado de un tema del plan curricular. Un tema sin cubrir se llama "No cumplido" cuando su etapa ya cerró
 * (alguna planilla de la asignación tiene la etapa confirmada) y "Pendiente" mientras la etapa sigue corriendo.
 */
export function coberturaTema(
  tema: { estadoCobertura?: string | null; fechaCobertura?: string | null },
  etapaCerrada: boolean | undefined,
): CoberturaTema {
  if (tema.estadoCobertura === 'CUBIERTO') return { estado: 'cumplido', fecha: tema.fechaCobertura ?? null };
  return etapaCerrada ? { estado: 'no-cumplido' } : { estado: 'pendiente' };
}
