import type { ParentSubject, RasgoConducta } from '../../api/parent';
import { coincideBusqueda } from '../../utils/texto';

/** Con menos materias que esto el grid se ve entero de una: el buscador sólo aparece cuando la lista es larga. */
export const MATERIAS_PARA_BUSCADOR = 6;

export function filtrarMaterias(materias: ParentSubject[], busqueda: string): ParentSubject[] {
  return materias.filter((materia) => coincideBusqueda(busqueda, materia.materia));
}

export interface FiltroConducta {
  materia: string;
  /** YYYY-MM-DD; vacío = sin límite */
  desde: string;
  hasta: string;
}

export const SIN_FILTRO_CONDUCTA: FiltroConducta = { materia: '', desde: '', hasta: '' };

export function hayFiltroConducta(filtro: FiltroConducta): boolean {
  return Boolean(filtro.materia || filtro.desde || filtro.hasta);
}

/** "Desde" posterior a "hasta": no hay ninguna fecha que cumpla, es un error del usuario y no un resultado vacío. */
export function rangoInvalido(filtro: FiltroConducta): boolean {
  return Boolean(filtro.desde && filtro.hasta && filtro.desde > filtro.hasta);
}

/** Materias que realmente aparecen en las notas del hijo (así ninguna opción del filtro da una lista vacía). */
export function materiasDeConducta(notas: RasgoConducta[]): string[] {
  return [...new Set(notas.map((nota) => nota.materia?.trim()).filter((materia): materia is string => Boolean(materia)))]
    .sort((a, b) => a.localeCompare(b, 'es'));
}

export function filtrarConducta(notas: RasgoConducta[], filtro: FiltroConducta): RasgoConducta[] {
  if (rangoInvalido(filtro)) return [];
  const conRango = Boolean(filtro.desde || filtro.hasta);
  return notas.filter((nota) => {
    if (filtro.materia && nota.materia !== filtro.materia) return false;
    if (!conRango) return true;
    const fecha = nota.fechaClase?.slice(0, 10);
    if (!fecha) return false; // una nota sin fecha no puede estar dentro de un rango
    return (!filtro.desde || fecha >= filtro.desde) && (!filtro.hasta || fecha <= filtro.hasta);
  });
}
