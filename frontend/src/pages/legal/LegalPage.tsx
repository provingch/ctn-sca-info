import { useLocation } from 'react-router-dom';
import InformationLayout from '../../components/InformationLayout';

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
  const isPrivacy = useLocation().pathname.includes('privacidad');
  const sections = isPrivacy ? privacy : terms;
  const anchor = (index: number) => `${isPrivacy ? 'privacidad' : 'terminos'}-seccion-${index + 1}`;
  return <InformationLayout title={isPrivacy ? 'Privacidad' : 'Términos'} lead={isPrivacy ? 'Política de Privacidad' : 'Condiciones del Servicio'} reading introduction={<>
    <p>Última actualización: <time dateTime="2026-08-10">10 de agosto de 2026</time></p>
    {sections.length >= 4 && <nav aria-label="Índice de contenidos"><ul className="about-modules information-index">
      {sections.map(([title], index) => <li key={title}><a href={`#${anchor(index)}`}>{title}</a></li>)}
    </ul></nav>}
  </>}>
    {sections.map(([title, copy], index) => <section id={anchor(index)} className={title === 'Contacto' ? 'about-project' : undefined} aria-labelledby={`${anchor(index)}-title`} key={title}>
      <h2 id={`${anchor(index)}-title`}>{title}</h2><p>{copy}</p>
    </section>)}
  </InformationLayout>;
}
