import { api } from './client';

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
  estado?: 'pendiente' | 'revisada';
  revisadaEn?: string | null;
  revisadaPor?: number | null;
  conclusion?: string | null;
}

export interface QuejaRevision {
  estado: 'revisada';
  revisadaEn: string;
  revisadaPor: number;
  conclusion: string;
}

export const isQuejaReviewed = (queja: QuejaItem) => queja.estado === 'revisada' || !!queja.revisadaEn;

export const getAdminQuejas = () => api.get<QuejaItem[]>('/api/admin/quejas');
export const createQueja = (payload: { profesorId: number; cursoId: number; motivo: string }) => api.post<void>('/api/home/quejas', payload);
export const reviewQueja = (id: number, conclusion: string) => api.put<QuejaRevision>(`/api/admin/quejas/${id}/revision`, { conclusion });
