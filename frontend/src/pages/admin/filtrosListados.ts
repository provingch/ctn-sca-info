import type { EgresadoItem, StudentItem } from '../../api/admin';
import { quejaEstado, type QuejaItem } from '../../api/quejas';
import { coincideBusqueda } from '../../utils/texto';

export type EstadoQueja = ReturnType<typeof quejaEstado>;

export interface FiltroQuejas {
  busqueda: string;
  /** '' = todos */
  estado: '' | EstadoQueja;
  /** Fecha de registro de la queja, YYYY-MM-DD; vacío = sin límite */
  desde: string;
  hasta: string;
}

export const SIN_FILTRO_QUEJAS: FiltroQuejas = { busqueda: '', estado: '', desde: '', hasta: '' };

export function hayFiltroQuejas(filtro: FiltroQuejas): boolean {
  return Boolean(filtro.busqueda.trim() || filtro.estado || filtro.desde || filtro.hasta);
}

export function rangoDeFechasInvalido(desde: string, hasta: string): boolean {
  return Boolean(desde && hasta && desde > hasta);
}

/** `textoDe` arma el texto donde se busca (profesor, especialidad, curso, motivo…) porque parte depende del catálogo. */
export function filtrarQuejas(quejas: QuejaItem[], filtro: FiltroQuejas, textoDe: (queja: QuejaItem) => string): QuejaItem[] {
  if (rangoDeFechasInvalido(filtro.desde, filtro.hasta)) return [];
  return quejas.filter((queja) => {
    if (filtro.estado && quejaEstado(queja) !== filtro.estado) return false;
    if (filtro.desde || filtro.hasta) {
      const fecha = queja.creadaEn?.slice(0, 10);
      if (!fecha) return false;
      if (filtro.desde && fecha < filtro.desde) return false;
      if (filtro.hasta && fecha > filtro.hasta) return false;
    }
    return coincideBusqueda(filtro.busqueda, textoDe(queja));
  });
}

export function filtrarAlumnos(alumnos: StudentItem[], busqueda: string): StudentItem[] {
  return alumnos.filter((alumno) => coincideBusqueda(busqueda, alumno.apellido, alumno.nombre, alumno.ci));
}

export interface FiltroEgresados {
  busqueda: string;
  /** '' = todas */
  especialidad: string;
  /** '' = todos */
  promocion: string;
}

export const SIN_FILTRO_EGRESADOS: FiltroEgresados = { busqueda: '', especialidad: '', promocion: '' };

export function hayFiltroEgresados(filtro: FiltroEgresados): boolean {
  return Boolean(filtro.busqueda.trim() || filtro.especialidad || filtro.promocion);
}

export function filtrarEgresados(egresados: EgresadoItem[], filtro: FiltroEgresados): EgresadoItem[] {
  return egresados.filter((egresado) => {
    if (filtro.especialidad && egresado.especialidad !== filtro.especialidad) return false;
    if (filtro.promocion && String(egresado.promocion ?? '') !== filtro.promocion) return false;
    return coincideBusqueda(filtro.busqueda, egresado.apellido, egresado.nombre, egresado.ci);
  });
}
