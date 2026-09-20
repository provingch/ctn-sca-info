import type { ParentSubject, ParentTask, RasgoConducta } from '../../api/parent';
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

// ---- Detalle de tareas: filtro por mes y agrupación por mes ------------------------------------------------

export const NOMBRES_MESES = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];

/** Clave de las tareas sin fecha válida; van al final de la lista. */
export const SIN_FECHA = 'sin-fecha';

/** "YYYY-MM" de la fecha de una tarea (o SIN_FECHA). Se calcula en el cliente a partir de `task.fecha`. */
export function mesDeTarea(fecha?: string | null): string {
  const coincidencia = fecha?.match(/^(\d{4})-(\d{2})/);
  if (!coincidencia) return SIN_FECHA;
  const mes = Number(coincidencia[2]);
  return mes >= 1 && mes <= 12 ? `${coincidencia[1]}-${coincidencia[2]}` : SIN_FECHA;
}

export function etiquetaMes(mes: string): string {
  if (mes === SIN_FECHA) return 'Sin fecha';
  const [anio, numero] = mes.split('-');
  return `${NOMBRES_MESES[Number(numero) - 1]} ${anio}`;
}

export interface TareaNumerada {
  tarea: ParentTask;
  /** Posición en la lista cronológica completa: el número de una tarea no cambia al filtrar por mes. */
  numero: number;
}

/** Cronológico (por fecha y después por id), con las tareas sin fecha al final. */
export function ordenarTareas(tareas: ParentTask[]): TareaNumerada[] {
  return [...tareas]
    .sort((a, b) => {
      const mesA = mesDeTarea(a.fecha) === SIN_FECHA;
      const mesB = mesDeTarea(b.fecha) === SIN_FECHA;
      if (mesA !== mesB) return mesA ? 1 : -1;
      return (a.fecha ?? '').localeCompare(b.fecha ?? '') || a.id - b.id;
    })
    .map((tarea, indice) => ({ tarea, numero: indice + 1 }));
}

/** Los meses que realmente tienen tareas, en orden cronológico. */
export function mesesConTareas(tareas: ParentTask[]): Array<{ value: string; label: string }> {
  const meses = [...new Set(ordenarTareas(tareas).map(({ tarea }) => mesDeTarea(tarea.fecha)))];
  return meses.map((value) => ({ value, label: etiquetaMes(value) }));
}

export interface GrupoDeTareas {
  mes: string;
  label: string;
  tareas: TareaNumerada[];
}

/** Con `mes` vacío ("Todos") agrupa todas por mes; con un mes puntual queda sólo ese grupo. */
export function agruparTareasPorMes(tareas: ParentTask[], mes = ''): GrupoDeTareas[] {
  const grupos = new Map<string, GrupoDeTareas>();
  for (const numerada of ordenarTareas(tareas)) {
    const clave = mesDeTarea(numerada.tarea.fecha);
    if (mes && clave !== mes) continue;
    const grupo = grupos.get(clave) ?? { mes: clave, label: etiquetaMes(clave), tareas: [] };
    grupo.tareas.push(numerada);
    grupos.set(clave, grupo);
  }
  return [...grupos.values()];
}
