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
}

export const getAdminQuejas = () => api.get<QuejaItem[]>('/api/admin/quejas');
export const createQueja = (payload: { profesorId: number; cursoId: number; motivo: string }) => api.post<void>('/api/home/quejas', payload);
