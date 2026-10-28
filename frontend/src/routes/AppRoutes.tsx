import { lazy, Suspense } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import { SpecialtyProvider } from '../context/SpecialtyContext';
import { ToastProvider } from '../context/ToastContext';
import { ProtectedRoute } from './ProtectedRoute';
import RoleLanding from './RoleLanding';
import LoginPage from '../pages/auth/LoginPage';
import OfflinePage from '../pages/OfflinePage';

// Cada rol usa solo sus páginas: se cargan al entrar, no todas en el bundle inicial.
const HomePage = lazy(() => import('../pages/home/HomePage'));
const ProfilePage = lazy(() => import('../pages/profile/ProfilePage'));
const GoogleCallbackPage = lazy(() => import('../pages/google/GoogleCallbackPage'));
const AuthorizeClassroomPage = lazy(() => import('../pages/google/AuthorizeClassroomPage'));
const PlanillaPage = lazy(() => import('../pages/planilla/PlanillaPage'));
const TareaPage = lazy(() => import('../pages/planilla/TareaPage'));
const EvaluacionPage = lazy(() => import('../pages/evaluacion/EvaluacionPage'));
const CoordinacionPage = lazy(() => import('../pages/coordinacion/CoordinacionPage'));
const LegalPage = lazy(() => import('../pages/legal/LegalPage'));
const AboutPage = lazy(() => import('../pages/about/AboutPage'));
const AdminPage = lazy(() => import('../pages/admin/AdminPage'));
const ParentPage = lazy(() => import('../pages/parent/ParentPage'));
const StyleguidePage = lazy(() => import('../pages/styleguide/StyleguidePage'));

const protect = (element: React.ReactNode, allowedLevels?: number[]) => <ProtectedRoute allowedLevels={allowedLevels}>{element}</ProtectedRoute>;

export default function AppRoutes() {
  return <BrowserRouter><SpecialtyProvider><ToastProvider><AuthProvider><Suspense fallback={null}><Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route path="/privacidad" element={<LegalPage />} />
    <Route path="/terminos" element={<LegalPage />} />
    <Route path="/acerca-de" element={<AboutPage />} />
    <Route path="/" element={<RoleLanding />} />
    <Route path="/home" element={protect(<HomePage />, [1])} />
    <Route path="/inicio" element={protect(<HomePage />, [1])} />
    <Route path="/profile" element={protect(<ProfilePage />, [1, 2, 3, 4, 5])} />
    <Route path="/perfil" element={protect(<ProfilePage />, [1, 2, 3, 4, 5])} />
    <Route path="/google/callback" element={protect(<GoogleCallbackPage />, [1, 2, 3, 4])} />
    <Route path="/google/authorize" element={protect(<AuthorizeClassroomPage />, [1, 2, 3, 4])} />
    <Route path="/planilla/:planillaId" element={protect(<PlanillaPage />, [1])} />
    <Route path="/planilla/:planillaId/tarea" element={protect(<TareaPage />, [1])} />
    <Route path="/planilla/:planillaId/tarea/:tareaId" element={protect(<TareaPage />, [1])} />
    <Route path="/evaluacion" element={protect(<EvaluacionPage />, [2])} />
    <Route path="/coordinacion" element={protect(<CoordinacionPage />, [5])} />
    <Route path="/admin" element={protect(<AdminPage />, [3])} />
    <Route path="/admin/materias" element={protect(<AdminPage />, [3])} />
    <Route path="/admin/usuarios" element={protect(<AdminPage />, [3])} />
    <Route path="/admin/asignaciones" element={protect(<AdminPage />, [3])} />
    <Route path="/admin/alumnos" element={protect(<AdminPage />, [3])} />
    <Route path="/admin/horarios" element={protect(<AdminPage />, [3])} />
    <Route path="/admin/horarios/:cursoId" element={protect(<AdminPage />, [3])} />
    <Route path="/admin/carga-horaria" element={protect(<AdminPage />, [3])} />
    <Route path="/admin/carga-horaria/:usuarioId" element={protect(<AdminPage />, [3])} />
    <Route path="/admin/salas" element={protect(<AdminPage />, [3])} />
    <Route path="/admin/quejas" element={protect(<AdminPage />, [3])} />
    <Route path="/admin/clases" element={protect(<AdminPage />, [3])} />
    <Route path="/admin/sistema" element={protect(<AdminPage />, [3])} />
    <Route path="/admin/ingresantes" element={protect(<Navigate to="/admin/alumnos" replace />, [3])} />
    <Route path="/padre" element={protect(<ParentPage />, [4])} />
    <Route path="/styleguide" element={protect(<StyleguidePage />, [3])} />
    <Route path="/offline" element={<OfflinePage />} />
    <Route path="*" element={<RoleLanding />} />
  </Routes></Suspense></AuthProvider></ToastProvider></SpecialtyProvider></BrowserRouter>;
}
