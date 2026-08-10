/**
 * Espejo parcial de ProfileController.java / ProfileResponse.
 * Igual que en home.ts: tipamos lo esencial para Bloque 2 y dejamos el
 * resto (Google Classroom, TOTP, materias, asignaciones) como unknown
 * hasta que la pantalla de perfil completa los necesite.
 */
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

export interface ProfileResponse {
  profileOwner: ProfileOwnerDto;
  isProfessorProfile: boolean;
  isParentProfile: boolean;
  isStaffProfile: boolean;
  profileRoleLabel: string;
  profileAccessDescription: string;
  totpEnabled: boolean;
  pushEnabled: boolean;
  // showMateriasPanel, showGoogleClassroomPanel, showSecurityPanel,
  // showActivityPanel, canEditAdminOnlyProfileFields, googleClassroom*,
  // teacherMaterias, misAsignaciones, availableMaterias, especialidades,
  // profesorEspecialidadNombre, manualTeacherSubjectsText, activityLog,
  // pendingTotpSecret, totpProvisioningUri, pushPublicKey:
  // pendientes de tipar cuando la pantalla de perfil los consuma
  // (ver backend ProfileResponse.java para los nombres exactos).
  [key: string]: unknown;
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
