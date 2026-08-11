import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import * as authApi from '../api/auth';
import { setAccessToken, setOnAuthExpired } from '../api/client';

export interface AuthUser {
  level: number;
}

interface AuthContextValue {
  user: AuthUser | null;
  /** true mientras se intenta hidratar la sesión al montar la app (vía /auth/refresh) */
  isBootstrapping: boolean;
  login: (username: string, password: string, rememberMe: boolean) => Promise<authApi.LoginResponse>;
  verify2fa: (tempToken: string, code: string, rememberMe: boolean) => Promise<authApi.LoginResponse>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isBootstrapping, setIsBootstrapping] = useState(true);

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

  // Si un refresh automático (disparado por un 401 durante uso normal)
  // también falla, client.ts llama a este callback para forzar logout.
  useEffect(() => {
    setOnAuthExpired(() => {
      setUser(null);
    });
    return () => setOnAuthExpired(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isBootstrapping,
      async login(username, password, rememberMe) {
        const res = await authApi.login({ username, password, rememberMe });
        if (!res.requiere2fa && res.accessToken && res.level !== null) {
          setAccessToken(res.accessToken);
          setUser({ level: res.level });
        }
        return res;
      },
      async verify2fa(tempToken, code, rememberMe) {
        const res = await authApi.verify2fa({ tempToken, code, rememberMe });
        if (res.accessToken && res.level !== null) {
          setAccessToken(res.accessToken);
          setUser({ level: res.level });
        }
        return res;
      },
      async logout() {
        try {
          await authApi.logout();
        } finally {
          setAccessToken(null);
          setUser(null);
        }
      },
    }),
    [user, isBootstrapping],
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
