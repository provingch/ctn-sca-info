import { Link, useLocation } from 'react-router-dom';
import ThemeToggle from '../../components/ThemeToggle';

const privacy = [
  ['Datos que recopilamos', 'El sistema almacena datos de identificación, contacto y trayectoria académica necesarios para prestar el servicio.'],
  ['Uso de los datos', 'Los datos se usan exclusivamente para la gestión académica, autenticación, comunicación y elaboración de reportes del CTN.'],
  ['Integración con Google Classroom', 'Cuando el usuario autoriza la conexión, SCA accede solamente a los cursos, tareas y calificaciones necesarios para sincronizar las planillas.'],
  ['Seguridad', 'Se aplican controles de acceso por rol, cifrado de credenciales y verificación en dos pasos.'],
  ['Contacto', 'Para consultas sobre privacidad escribí a provingchill@gmail.com.'],
];
const terms = [
  ['Acceso', 'El acceso está reservado a usuarios autorizados por el Colegio Técnico Nacional. Cada usuario es responsable de proteger sus credenciales.'],
  ['Uso de Google Classroom', 'La integración es opcional y queda sujeta también a las condiciones de Google. El usuario puede desconectarla desde su perfil.'],
  ['Responsabilidades', 'La información académica debe cargarse y revisarse de acuerdo con las normas institucionales. No se permite un uso ajeno a la actividad educativa.'],
  ['Modificaciones', 'Estas condiciones pueden actualizarse para reflejar cambios funcionales, normativos o de seguridad.'],
];
export default function LegalPage() {
  const isPrivacy = useLocation().pathname.includes('privacidad'); const sections = isPrivacy ? privacy : terms;
  return <main className="legal-page"><div className="legal-toolbar"><Link to="/">← Volver al inicio</Link><ThemeToggle /></div><article><p className="eyebrow">SCA · Colegio Técnico Nacional</p><h1>{isPrivacy ? 'Política de Privacidad' : 'Condiciones del Servicio'}</h1><p className="lead">Última actualización: 10 de agosto de 2026</p>{sections.map(([title, copy], i) => <section key={title}><h2>{i + 1}. {title}</h2><p>{copy}</p></section>)}<hr/><Link to={isPrivacy ? '/terminos' : '/privacidad'}>{isPrivacy ? 'Ver Condiciones del Servicio' : 'Ver Política de Privacidad'}</Link></article></main>;
}
