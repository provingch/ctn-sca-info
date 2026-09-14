import { api, apiDownload } from './client';

export interface QuejaItem {
  id: number;
  profesorId: number;
  profesorNombre?: string | null;
  profesorApellido?: string | null;
  cursoId: number;
  cursoEspecialidad?: string | null;
  cursoSeccion?: string | null;
  cursoNivel?: number | null;
  especialidadId: number;
  motivo: string;
  creadaPor: number;
  creadaEn: string;
  estado?: 'pendiente' | 'revisada' | 'resuelta';
  revisadaEn?: string | null;
  revisadaPor?: number | null;
  conclusion?: string | null;
  procesoRevision?: string | null;
  solucionAplicada?: string | null;
  corregidaPorNombre?: string | null;
  resueltaEn?: string | null;
}

export interface QuejaRevision {
  estado: 'revisada';
  revisadaEn: string;
  revisadaPor: number;
  conclusion: string;
}

export interface QuejaResolucionInput { procesoRevision: string; solucionAplicada: string; corregidaPorNombre: string }
export interface QuejaResolucion extends QuejaResolucionInput { estado: 'resuelta'; resueltaEn: string }
export const quejaEstado = (queja: QuejaItem) => queja.estado === 'resuelta' || queja.resueltaEn ? 'resuelta' : queja.estado === 'revisada' || queja.revisadaEn ? 'revisada' : 'pendiente';
export const isQuejaReviewed = (queja: QuejaItem) => quejaEstado(queja) !== 'pendiente';

export const getAdminQuejas = () => api.get<QuejaItem[]>('/api/admin/quejas');
export const createQueja = (payload: { profesorId: number; cursoId: number; motivo: string }) => api.post<void>('/api/home/quejas', payload);
export const reviewQueja = (id: number, conclusion: string) => api.put<QuejaRevision>(`/api/admin/quejas/${id}/revision`, { conclusion });
export const resolveQueja = (id: number, input: QuejaResolucionInput) => api.put<QuejaResolucion>(`/api/admin/quejas/${id}/resolucion`, input);
export const downloadQuejaPdf = (id: number) => apiDownload(`/api/admin/quejas/${id}/reporte-solucion.pdf`, `reporte-solucion-${id}.pdf`);
export const downloadQuejaExcel = (id: number) => apiDownload(`/api/admin/quejas/${id}/solicitud-revision.xlsx`, `solicitud-revision-${id}.xlsx`);
