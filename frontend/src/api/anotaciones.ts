import { api, apiBlob } from './client';

/** Anotación cargada desde la app Android (uniforme, conducta…); la foto es evidencia, no un retrato. */
export interface Anotacion {
  id: number;
  alumnoId: number;
  alumnoNombre: string;
  alumnoApellido: string;
  curso: string | null;
  fechaHora: string | null;
  tipo: string;
  observacion: string | null;
  materia: string | null;
  profesor: string | null;
  tieneFoto: boolean;
}

export const getAnotacionesRecientes = (limite = 100) => api.get<Anotacion[]>(`/api/admin/anotaciones?limite=${limite}`);
export const getFotoAnotacionCoordinacion = (anotacionId: number) => apiBlob(`/api/admin/anotaciones/${anotacionId}/foto`);
export const getAnotacionesHijo = (alumnoId: number) => api.get<Anotacion[]>(`/api/padre/alumnos/${alumnoId}/anotaciones`);
export const getFotoAnotacionHijo = (alumnoId: number, anotacionId: number) => apiBlob(`/api/padre/alumnos/${alumnoId}/anotaciones/${anotacionId}/foto`);
/** Ficha del alumno en Admin (alcance por especialidad): la foto se pide recién al abrir la anotación. */
export const getAnotacionesAlumnoAdmin = (alumnoId: number) => api.get<Anotacion[]>(`/api/admin/alumnos/${alumnoId}/anotaciones`);
export const getFotoAnotacionAlumnoAdmin = (alumnoId: number, anotacionId: number) => apiBlob(`/api/admin/alumnos/${alumnoId}/anotaciones/${anotacionId}/foto`);
