import { apiRequest } from './client';

export interface ProfileOwnerDto {
  id: number | null;
  nombre: string | null;
  apellido: string | null;
  fullName: string | null;
  ci: number | null;
  correo: string | null;
  telefono: string | null;
  celular: string | null;
  usuario: string | null;
  especialidadId: number | null;
  googleEmail: string | null;
  gcAccessToken: string | null;
  firmaImagen: string | null;
  fotoPerfil: string | null;
}

export interface ProfileMateriaDto { id: number; nombre: string; categoria: string | null; }
export interface ProfileAssignmentDto { id: number; materiaNombre: string; cursoDescripcion: string; especialidad?: string | null; cursoNivel?: number | null; cursoSeccion?: string | null; }
export interface ProfileClassroomCourseDto { id: string; name: string; section: string | null; room: string | null; }
export interface ProfileSpecialtyDto { id: number; nombre: string; }

export interface ProfileResponse {
  profileOwner: ProfileOwnerDto;
  isProfessorProfile: boolean;
  isParentProfile: boolean;
  isStaffProfile: boolean;
  profileRoleLabel: string;
  profileAccessDescription: string;
  showMateriasPanel: boolean;
  showGoogleClassroomPanel: boolean;
  showSignaturePanel: boolean;
  showSecurityPanel: boolean;
  showActivityPanel: boolean;
  canEditAdminOnlyProfileFields: boolean;
  googleClassroomConnected: boolean;
  googleClassroomCourses: ProfileClassroomCourseDto[];
  teacherMaterias: ProfileMateriaDto[];
  misAsignaciones: ProfileAssignmentDto[];
  especialidades: ProfileSpecialtyDto[];
  activityLog: string[];
  totpEnabled: boolean;
  pendingTotpSecret: string | null;
  totpProvisioningUri: string | null;
  pushPublicKey: string;
  pushEnabled: boolean;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export function getProfile(): Promise<ProfileResponse> {
  return apiRequest<ProfileResponse>('/api/profile', { method: 'GET' });
}

export function changePassword(payload: ChangePasswordRequest): Promise<void> {
  return apiRequest<void>('/api/profile/change-password', { method: 'POST', body: payload });
}

export function saveProfile(payload: { correo: string; telefono: string; celular: string; usuario: string; nombre: string; apellido: string; ci: number | null; nivel: number | null; firmaImagen?: string | null; fotoPerfil?: string | null }): Promise<void> {
  return apiRequest<void>('/api/profile/save-profile', { method: 'POST', body: payload });
}

export const prepareTotp = () => apiRequest<void>('/api/profile/prepare-totp', { method: 'POST' });
export const confirmTotp = (totpSetupCode: string) => apiRequest<void>('/api/profile/confirm-totp', { method: 'POST', body: { totpSetupCode } });
export const disableTotp = () => apiRequest<void>('/api/profile/disable-totp', { method: 'POST' });

export function getGoogleAuthorizeUrl(): Promise<{ url: string }> {
  return apiRequest<{ url: string }>('/api/google/oauth/authorize-url', { method: 'GET' });
}

export function disconnectGoogle(): Promise<void> {
  return apiRequest<void>('/api/profile/google/disconnect', { method: 'POST' });
}

export function completeGoogleCallback(payload: { code?: string; state?: string; error?: string }): Promise<{ status: string; message: string }> {
  return apiRequest<{ status: string; message: string }>('/api/google/oauth/callback', { method: 'POST', body: payload });
}
