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
  googleEmail: string | null;
  gcAccessToken: string | null;
}

export interface ProfileMateriaDto { id: number; nombre: string; categoria: string | null; }
export interface ProfileAssignmentDto { id: number; materiaNombre: string; cursoDescripcion: string; }
export interface ProfileClassroomCourseDto { id: string; name: string; section: string | null; room: string | null; }

export interface ProfileResponse {
  profileOwner: ProfileOwnerDto;
  isProfessorProfile: boolean;
  isParentProfile: boolean;
  isStaffProfile: boolean;
  profileRoleLabel: string;
  profileAccessDescription: string;
  showMateriasPanel: boolean;
  showGoogleClassroomPanel: boolean;
  showSecurityPanel: boolean;
  showActivityPanel: boolean;
  canEditAdminOnlyProfileFields: boolean;
  googleClassroomConnected: boolean;
  googleClassroomCourses: ProfileClassroomCourseDto[];
  teacherMaterias: ProfileMateriaDto[];
  misAsignaciones: ProfileAssignmentDto[];
  profesorEspecialidadNombre: string;
  activityLog: string[];
  totpEnabled: boolean;
  pendingTotpSecret: string | null;
  totpProvisioningUri: string | null;
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

export function saveProfile(payload: { correo: string; telefono: string; celular: string; usuario: string; nombre: string; apellido: string; ci: number | null; nivel: number | null }): Promise<void> {
  return apiRequest<void>('/api/profile/save-profile', { method: 'POST', body: payload });
}

export const prepareTotp = () => apiRequest<void>('/api/profile/prepare-totp', { method: 'POST' });
export const confirmTotp = (totpSetupCode: string) => apiRequest<void>('/api/profile/confirm-totp', { method: 'POST', body: { totpSetupCode } });
export const disableTotp = () => apiRequest<void>('/api/profile/disable-totp', { method: 'POST' });
