import { Link } from 'react-router-dom';
import ThemeToggle from '../../components/ThemeToggle';

const team = [
  { user: '@provingch', name: 'Thiago Estigarribia', url: 'https://github.com/provingch' },
  { user: '@Sh1b0', name: 'Joshua Mongelos', url: 'https://github.com/Sh1b0' },
  { user: '@schmidtsamuel626', name: 'Samuel Schmidt', url: 'https://github.com/schmidtsamuel626' },
  { user: '@Macrosss', name: 'Marcos Molinas', url: 'https://github.com/tukp5678' },
];

/** Créditos del proyecto. Pública, como /privacidad y /terminos. */
export default function AboutPage() {
  const year = new Date().getFullYear();
  return (
    <main className="legal-page">
      <div className="legal-toolbar">
        <Link to="/">← Volver al inicio</Link>
        <ThemeToggle />
      </div>
      <article>
        <p className="eyebrow">SCA · Colegio Técnico Nacional de Asunción</p>
        <h1>Acerca de</h1>
        <p className="lead">Sistema de Carpeta Académica (SCA): gestión de planillas, tareas, planificación curricular y comunicación entre profesores, evaluación, coordinación pedagógica, administración y familias del CTN.</p>

        <section>
          <h2>1. Equipo</h2>
          <p>Proyecto desarrollado por:</p>
          <ul>
            {team.map((person) => (
              <li key={person.user}>
                <a href={person.url} target="_blank" rel="noreferrer noopener">{person.user}</a> — {person.name}
              </li>
            ))}
          </ul>
        </section>

        <section>
          <h2>2. Cronología</h2>
          <ul>
            <li>Inicio del desarrollo: 18 de junio de 2026.</li>
            <li>Propuesta aceptada: 29 de junio de 2026.</li>
            <li>Fusión desarrollada: 28 de julio de 2026.</li>
          </ul>
        </section>

        <hr />
        <p className="lead">© {year} Colegio Técnico Nacional de Asunción.</p>
      </article>
    </main>
  );
}
