import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import { SpecialtyProvider } from '../context/SpecialtyContext';
import { ProtectedRoute } from './ProtectedRoute';
import RoleLanding from './RoleLanding';
import LoginPage from '../pages/auth/LoginPage';
import HomePage from '../pages/home/HomePage';
import ProfilePage from '../pages/profile/ProfilePage';
import PlanillaPage from '../pages/planilla/PlanillaPage';
import TareaPage from '../pages/planilla/TareaPage';
import EvaluacionPage from '../pages/evaluacion/EvaluacionPage';
import LegalPage from '../pages/legal/LegalPage';
import AdminPage from '../pages/admin/AdminPage';
import ParentPage from '../pages/parent/ParentPage';
import StyleguidePage from '../pages/styleguide/StyleguidePage';

const protect = (element: React.ReactNode) => <ProtectedRoute>{element}</ProtectedRoute>;

export default function AppRoutes() {
  return <BrowserRouter><SpecialtyProvider><AuthProvider><Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route path="/privacidad" element={<LegalPage />} />
    <Route path="/terminos" element={<LegalPage />} />
    <Route path="/" element={<RoleLanding />} />
    <Route path="/home" element={protect(<HomePage />)} />
    <Route path="/inicio" element={protect(<HomePage />)} />
    <Route path="/profile" element={protect(<ProfilePage />)} />
    <Route path="/perfil" element={protect(<ProfilePage />)} />
    <Route path="/planilla/:planillaId" element={protect(<PlanillaPage />)} />
    <Route path="/planilla/:planillaId/tarea" element={protect(<TareaPage />)} />
    <Route path="/planilla/:planillaId/tarea/:tareaId" element={protect(<TareaPage />)} />
    <Route path="/evaluacion" element={protect(<EvaluacionPage />)} />
    <Route path="/admin" element={protect(<AdminPage />)} />
    <Route path="/admin/materias" element={protect(<AdminPage />)} />
    <Route path="/admin/usuarios" element={protect(<AdminPage />)} />
    <Route path="/admin/asignaciones" element={protect(<AdminPage />)} />
    <Route path="/admin/ingresantes" element={protect(<AdminPage />)} />
    <Route path="/padre" element={protect(<ParentPage />)} />
    <Route path="/styleguide" element={protect(<StyleguidePage />)} />
    <Route path="*" element={<RoleLanding />} />
  </Routes></AuthProvider></SpecialtyProvider></BrowserRouter>;
}
