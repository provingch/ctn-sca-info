import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import * as authApi from '../api/auth';
import { getProfile } from '../api/profile';
import { setAccessToken, setOnAuthExpired } from '../api/client';
import { useSpecialty } from './SpecialtyContext';
import { resolveIdentitySpecialty } from './authIdentity';

export interface AuthUser {
  level: number;
  displayName?: string;
  username?: string;
  initials?: string;
  fotoPerfil?: string;
  especialidadId?: number | null;
  especialidadNombre?: string | null;
}

type IdentityStatus = 'idle' | 'loading' | 'ready' | 'error';

interface AuthContextValue {
  user: AuthUser | null;
  /** true mientras se intenta hidratar la sesión al montar la app (vía /auth/refresh) */
  isBootstrapping: boolean;
  identityStatus: IdentityStatus;
  login: (username: string, password: string, rememberMe: boolean) => Promise<authApi.LoginResponse>;
  verify2fa: (tempToken: string, code: string, rememberMe: boolean) => Promise<authApi.LoginResponse>;
  logout: () => Promise<void>;
  refreshUserIdentity: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const { resetSpecialty } = useSpecialty();
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isBootstrapping, setIsBootstrapping] = useState(true);
  const [identityStatus, setIdentityStatus] = useState<IdentityStatus>('idle');

  // Al montar: no hay access token en memoria todavía (se perdió en el reload).
  // El backend emite una cookie httpOnly de renovación: SCA_SESSION durante la
  // sesión normal del navegador o SCA_REMEMBER cuando se eligió "recordarme".
  // /api/auth/refresh restaura el access token en memoria después de un reload.
  // Si ninguna cookie existe (o expiró), arrancamos deslogueados.
  useEffect(() => {
    let cancelled = false;

    authApi
      .refresh()
      .then((res) => {
        if (cancelled) return;
        setAccessToken(res.accessToken);
        setUser({ level: res.level });
      })
      .catch(() => {
        // No había sesión persistente (o expiró). Estado inicial: deslogueado.
      })
      .finally(() => {
        if (!cancelled) setIsBootstrapping(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const refreshUserIdentity = useCallback(async () => {
    const activeLevel = user?.level;
    if (!activeLevel) return;
    setIdentityStatus('loading');
    try {
      const profile = await getProfile();
      const owner = profile.profileOwner;
      const displayName = owner.fullName?.trim() || owner.usuario?.trim() || 'Usuario SCA';
      const initials = `${owner.nombre?.trim()[0] || owner.usuario?.trim()[0] || 'S'}${owner.apellido?.trim()[0] || ''}`.toUpperCase();
      const specialty = resolveIdentitySpecialty(activeLevel, owner, profile.especialidades);
      setUser((current) => current?.level === activeLevel
        ? { ...current, displayName, username: owner.usuario?.trim() || undefined, initials, fotoPerfil: owner.fotoPerfil ?? undefined, especialidadId: specialty.id, especialidadNombre: specialty.name }
        : current);
      setIdentityStatus('ready');
    } catch {
      setIdentityStatus('error');
    }
  }, [user?.level]);

  useEffect(() => {
    void refreshUserIdentity();
  }, [refreshUserIdentity]);

  // Si un refresh automático (disparado por un 401 durante uso normal)
  // también falla, client.ts llama a este callback para forzar logout.
  useEffect(() => {
    setOnAuthExpired(() => {
      setUser(null);
      setIdentityStatus('idle');
    });
    return () => setOnAuthExpired(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isBootstrapping,
      identityStatus,
      async login(username, password, rememberMe) {
        setUser(null);
        setIdentityStatus('idle');
        const res = await authApi.login({ username, password, rememberMe });
        if (!res.requiere2fa && res.accessToken && res.level !== null) {
          resetSpecialty();
          setAccessToken(res.accessToken);
          setUser({ level: res.level });
          // El refresh inicial puede seguir pendiente cuando el usuario envía el
          // formulario. No debe ocultar una sesión que acabamos de validar.
          setIsBootstrapping(false);
        }
        return res;
      },
      async verify2fa(tempToken, code, rememberMe) {
        setUser(null);
        setIdentityStatus('idle');
        const res = await authApi.verify2fa({ tempToken, code, rememberMe });
        if (res.accessToken && res.level !== null) {
          resetSpecialty();
          setAccessToken(res.accessToken);
          setUser({ level: res.level });
          setIsBootstrapping(false);
        }
        return res;
      },
      async logout() {
        try {
          await authApi.logout();
        } finally {
          setAccessToken(null);
          setUser(null);
          setIdentityStatus('idle');
          resetSpecialty();
        }
      },
      refreshUserIdentity,
    }),
    [user, isBootstrapping, identityStatus, resetSpecialty, refreshUserIdentity],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// oxlint-disable-next-line react/only-export-components -- hook y provider comparten el mismo contexto privado.
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth debe usarse dentro de <AuthProvider>');
  }
  return ctx;
}
