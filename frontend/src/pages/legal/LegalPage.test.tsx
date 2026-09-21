import { render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { expect, it, vi } from 'vitest';
import LegalPage from './LegalPage';
const session = vi.hoisted(() => ({ user: null as null | { level: number } }));
vi.mock('../../context/AuthContext', () => ({ useAuth: () => session }));
vi.mock('../../context/SpecialtyContext', () => ({ useSpecialty: () => ({ name: 'Informática' }) }));
vi.mock('../../components/AppNavbar', () => ({ default: () => <header aria-label="Navegación de la cuenta" /> }));
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

it.each([{ path: '/privacidad', title: 'Privacidad', sections: privacy }, { path: '/terminos', title: 'Términos', sections: terms }])('conserva texto, orden y anclas en $path', ({ path, title, sections }) => {
  session.user = null;
  const { container } = render(<MemoryRouter initialEntries={[path]}><LegalPage /></MemoryRouter>);
  expect(screen.getByRole('heading', { level: 1, name: title })).toBeInTheDocument();
  expect([...container.querySelectorAll('article section')].map(section => [section.querySelector('h2')?.textContent, section.querySelector('p')?.textContent])).toEqual(sections);
  const links = within(screen.getByRole('navigation', { name: 'Índice de contenidos' })).getAllByRole('link');
  links.forEach(link => expect(container.querySelector(link.getAttribute('href')!)).not.toBeNull());
  expect(screen.getByText('10 de agosto de 2026')).toHaveAttribute('datetime', '2026-08-10');
  expect(screen.queryByLabelText('Navegación de la cuenta')).not.toBeInTheDocument();
  ['Privacidad', 'Términos', 'Acerca de'].forEach(name => expect(screen.getAllByRole('link', { name })).toHaveLength(1));
});
it('con sesión conserva el encabezado de la cuenta y un solo footer global', () => {
  session.user = { level: 1 };
  render(<MemoryRouter initialEntries={['/privacidad']}><LegalPage /></MemoryRouter>);
  expect(screen.getByLabelText('Navegación de la cuenta')).toBeInTheDocument();
  expect(screen.getAllByRole('link', { name: 'Términos' })).toHaveLength(1);
  session.user = null;
});
