import { api } from './client';

export interface QuejaItem {
  id: number;
  profesorId: number;
  profesorNombre?: string | null;
  cursoId: number;
  cursoDescripcion?: string | null;
  especialidadId: number;
  especialidadNombre?: string | null;
  motivo: string;
  creadoPor?: string | null;
  fecha?: string | null;
}

export const getAdminQuejas = () => api.get<QuejaItem[]>('/api/admin/quejas');
export const createQueja = (payload: { profesorId: number; cursoId: number; especialidadId: number; motivo: string }) => api.post<{ id: number }>('/quejas', payload);
