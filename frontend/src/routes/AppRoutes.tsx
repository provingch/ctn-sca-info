import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import { SpecialtyProvider } from '../context/SpecialtyContext';
import { ProtectedRoute } from './ProtectedRoute';
import RoleLanding from './RoleLanding';
import LoginPage from '../pages/auth/LoginPage';
import HomePage from '../pages/home/HomePage';
import ProfilePage from '../pages/profile/ProfilePage';
import GoogleCallbackPage from '../pages/google/GoogleCallbackPage';
import AuthorizeClassroomPage from '../pages/google/AuthorizeClassroomPage';
import PlanillaPage from '../pages/planilla/PlanillaPage';
import TareaPage from '../pages/planilla/TareaPage';
import EvaluacionPage from '../pages/evaluacion/EvaluacionPage';
import LegalPage from '../pages/legal/LegalPage';
import AdminPage from '../pages/admin/AdminPage';
import ParentPage from '../pages/parent/ParentPage';
import StyleguidePage from '../pages/styleguide/StyleguidePage';
import OfflinePage from '../pages/OfflinePage';

const protect = (element: React.ReactNode, allowedLevels?: number[]) => <ProtectedRoute allowedLevels={allowedLevels}>{element}</ProtectedRoute>;

export default function AppRoutes() {
  return <BrowserRouter><SpecialtyProvider><AuthProvider><Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route path="/privacidad" element={<LegalPage />} />
    <Route path="/terminos" element={<LegalPage />} />
    <Route path="/" element={<RoleLanding />} />
    <Route path="/home" element={protect(<HomePage />, [1])} />
    <Route path="/inicio" element={protect(<HomePage />, [1])} />
    <Route path="/profile" element={protect(<ProfilePage />, [1, 2, 3, 4])} />
    <Route path="/perfil" element={protect(<ProfilePage />, [1, 2, 3, 4])} />
    <Route path="/google/callback" element={protect(<GoogleCallbackPage />, [1, 2, 3, 4])} />
    <Route path="/google/authorize" element={protect(<AuthorizeClassroomPage />, [1, 2, 3, 4])} />
    <Route path="/planilla/:planillaId" element={protect(<PlanillaPage />, [1])} />
    <Route path="/planilla/:planillaId/tarea" element={protect(<TareaPage />, [1])} />
    <Route path="/planilla/:planillaId/tarea/:tareaId" element={protect(<TareaPage />, [1])} />
    <Route path="/evaluacion" element={protect(<EvaluacionPage />, [2])} />
    <Route path="/admin" element={protect(<AdminPage />, [3])} />
    <Route path="/admin/materias" element={protect(<AdminPage />, [3])} />
    <Route path="/admin/usuarios" element={protect(<AdminPage />, [3])} />
    <Route path="/admin/asignaciones" element={protect(<AdminPage />, [3])} />
    <Route path="/admin/alumnos" element={protect(<AdminPage />, [3])} />
    <Route path="/admin/horarios" element={protect(<AdminPage />, [3])} />
    <Route path="/admin/sistema" element={protect(<AdminPage />, [3])} />
    <Route path="/admin/ingresantes" element={protect(<Navigate to="/admin/alumnos" replace />, [3])} />
    <Route path="/padre" element={protect(<ParentPage />, [4])} />
    <Route path="/styleguide" element={protect(<StyleguidePage />, [3])} />
    <Route path="/offline" element={<OfflinePage />} />
    <Route path="*" element={<RoleLanding />} />
  </Routes></AuthProvider></SpecialtyProvider></BrowserRouter>;
}
