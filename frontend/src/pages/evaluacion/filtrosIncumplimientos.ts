import * as evaluacionApi from '../../api/evaluacion';
import { coincideBusqueda } from '../../utils/texto';

export type CategoriaIncumplimiento = 'bloqueos' | 'atrasos' | 'retroactivas';

export interface FiltroIncumplimientos {
  busqueda: string;
  /** '' = todas las categorías */
  categoria: '' | CategoriaIncumplimiento;
}

export const SIN_FILTRO_INCUMPLIMIENTOS: FiltroIncumplimientos = { busqueda: '', categoria: '' };

export function hayFiltroIncumplimientos(filtro: FiltroIncumplimientos): boolean {
  return Boolean(filtro.busqueda.trim() || filtro.categoria);
}

/** En qué grupo de la lista cae un caso; un tipo desconocido no cae en ninguno. */
export function categoriaDe(tipo: string): CategoriaIncumplimiento | null {
  if (tipo === evaluacionApi.TIPO_ATRASO) return 'atrasos';
  if (tipo === evaluacionApi.TIPO_INCONGRUENCIA_RETROACTIVA) return 'retroactivas';
  if (tipo === evaluacionApi.TIPO_BLOQUEO_INCONGRUENCIA_RETROACTIVA || tipo === evaluacionApi.TIPO_BLOQUEO_ATRASO_TIEMPO_REAL) return 'bloqueos';
  return null;
}

export function filtrarIncumplimientos(
  items: evaluacionApi.IncumplimientoPendiente[],
  filtro: FiltroIncumplimientos,
  nombreDe: (item: evaluacionApi.IncumplimientoPendiente) => string,
): evaluacionApi.IncumplimientoPendiente[] {
  return items.filter((item) => {
    if (filtro.categoria && categoriaDe(item.tipo) !== filtro.categoria) return false;
    return coincideBusqueda(filtro.busqueda, nombreDe(item), item.materiaNombre, item.descripcion);
  });
}
