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
  tipo: 'CONTRA_PROFESOR' | 'CONTRA_CURSO';
  motivo: string;
  creadaPor: number;
  creadaEn: string;
  estado?: 'pendiente' | 'aceptada' | 'revisada' | 'resuelta' | 'rechazada';
  aceptadaEn?: string | null;
  aceptadaPor?: number | null;
  rechazadaEn?: string | null;
  rechazadaPor?: number | null;
  motivoRechazo?: string | null;
  revisadaEn?: string | null;
  revisadaPor?: number | null;
  conclusion?: string | null;
  procesoRevision?: string | null;
  solucionAplicada?: string | null;
  corregidaPorNombre?: string | null;
  resueltaEn?: string | null;
}

export interface QuejaAceptacion {
  aceptadaEn: string;
  aceptadaPor: number;
}

export interface QuejaRechazo {
  rechazadaEn: string;
  rechazadaPor: number;
  motivoRechazo: string;
}

export interface QuejaRevision {
  estado: 'revisada';
  revisadaEn: string;
  revisadaPor: number;
  conclusion: string;
}

export interface QuejaResolucionInput { procesoRevision: string; solucionAplicada: string; corregidaPorNombre: string }
export interface QuejaResolucion extends QuejaResolucionInput { estado: 'resuelta'; resueltaEn: string }

// Orden de evaluación: rechazada primero (es terminal), después resuelta, revisada, aceptada, pendiente.
export const quejaEstado = (queja: QuejaItem): 'pendiente' | 'aceptada' | 'revisada' | 'resuelta' | 'rechazada' =>
  queja.estado === 'rechazada' || queja.rechazadaEn ? 'rechazada'
    : queja.estado === 'resuelta' || queja.resueltaEn ? 'resuelta'
    : queja.estado === 'revisada' || queja.revisadaEn ? 'revisada'
    : queja.estado === 'aceptada' || queja.aceptadaEn ? 'aceptada'
    : 'pendiente';
export const isQuejaReviewed = (queja: QuejaItem) => quejaEstado(queja) === 'revisada' || quejaEstado(queja) === 'resuelta';

export const getAdminQuejas = () => api.get<QuejaItem[]>('/api/admin/quejas');
export const getMisQuejas = () => api.get<QuejaItem[]>('/api/home/quejas');
export const createQuejaContraProfesor = (payload: { profesorId: number; cursoId: number; motivo: string }) =>
  api.post<void>('/api/home/quejas', payload);
export const createQuejaSobreCurso = (payload: { cursoId: number; motivo: string }) =>
  api.post<void>('/api/home/quejas', payload);
export const acceptQueja = (id: number) => api.put<QuejaAceptacion>(`/api/admin/quejas/${id}/aceptacion`);
export const rejectQueja = (id: number, motivoRechazo: string) => api.put<QuejaRechazo>(`/api/admin/quejas/${id}/rechazo`, { motivoRechazo });
export const reviewQueja = (id: number, conclusion: string) => api.put<QuejaRevision>(`/api/admin/quejas/${id}/revision`, { conclusion });
export const resolveQueja = (id: number, input: QuejaResolucionInput) => api.put<QuejaResolucion>(`/api/admin/quejas/${id}/resolucion`, input);
export const downloadQuejaPdf = (id: number) => apiDownload(`/api/admin/quejas/${id}/reporte-solucion.pdf`, `reporte-solucion-${id}.pdf`);
export const downloadQuejaExcel = (id: number) => apiDownload(`/api/admin/quejas/${id}/solicitud-revision.xlsx`, `solicitud-revision-${id}.xlsx`);
