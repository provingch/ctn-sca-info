export interface NavigationItem {
  label: string;
  to: string;
  end?: boolean;
  activePrefixes?: string[];
}

export interface RoleNavigationConfig {
  roleLabel: string;
  manualPath: string;
  primaryItems: NavigationItem[];
}

const profileItem: NavigationItem = { label: 'Mi perfil', to: '/profile' };

export const navigationByRole: Record<number, RoleNavigationConfig> = {
  1: {
    roleLabel: 'Profesor',
    manualPath: '/pdfs/manual-profesor.pdf',
    primaryItems: [{ label: 'Cursos', to: '/home', activePrefixes: ['/inicio', '/planilla'] }, profileItem],
  },
  2: {
    roleLabel: 'Evaluador',
    manualPath: '/pdfs/manual-evaluador.pdf',
    primaryItems: [{ label: 'Evaluación', to: '/evaluacion' }, profileItem],
  },
  3: {
    roleLabel: 'Administrador',
    manualPath: '/pdfs/manual-administrador.pdf',
    primaryItems: [{ label: 'Administración', to: '/admin' }, profileItem],
  },
  4: {
    roleLabel: 'Padre / Encargado',
    manualPath: '/pdfs/manual-padres.pdf',
    primaryItems: [{ label: 'Mis hijos', to: '/padre' }, profileItem],
  },
  5: {
    roleLabel: 'Coordinación Pedagógica',
    manualPath: '/pdfs/manual-coordinacion.pdf',
    primaryItems: [{ label: 'Coordinación', to: '/coordinacion' }, profileItem],
  },
};

export function getRoleNavigation(level?: number | null): RoleNavigationConfig {
  return navigationByRole[level ?? 0] ?? {
    roleLabel: 'Usuario',
    manualPath: '/pdfs/manual-profesor.pdf',
    primaryItems: [profileItem],
  };
}
