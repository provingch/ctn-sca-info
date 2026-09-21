import InformationLayout from '../../components/InformationLayout';

const team = [
  { user: '@provingch', name: 'Thiago Estigarribia', github: 'provingch' },
  { user: '@Sh1b0', name: 'Joshua Mongelos', github: 'Sh1b0' },
  { user: '@schmidtsamuel626', name: 'Samuel Schmidt', github: 'schmidtsamuel626' },
  { user: '@Macrosss', name: 'Marcos Molinas', github: 'tukp5678' },
];
const modules = ['Planillas', 'Tareas', 'Planificación', 'Comunicación', 'Evaluación', 'Coordinación', 'Administración', 'Familias'];
const milestones = [
  { date: '2026-06-18', label: '18 de junio de 2026', title: 'Inicio del desarrollo' },
  { date: '2026-06-29', label: '29 de junio de 2026', title: 'Propuesta aceptada' },
  { date: '2026-07-28', label: '28 de julio de 2026', title: 'Fusión de dos proyectos en uno' },
];
// Version taken from the latest release in the repository's CHANGELOG.md.
const documentedVersion = '2.0.0';
const repository = 'https://github.com/provingch/ctn-sca-info';

/** Public credits, with the regular account navigation for signed-in users. */
export default function AboutPage() {
  return <InformationLayout title="Acerca de SCA" lead="Una plataforma para gestionar la vida académica del CTN." introduction={<>
      <p>El Sistema de Carpeta Académica conecta a docentes, equipos de gestión y familias.</p>
      <ul className="about-modules" aria-label="Módulos del sistema">{modules.map((module) => <li key={module}>{module}</li>)}</ul>
    </>}>
    <section aria-labelledby="about-team-title">
      <h2 id="about-team-title">Equipo</h2>
      <p>Las personas detrás del proyecto.</p>
      <ul className="about-team">{team.map((person) => <li key={person.github}>
        <div className="about-avatar" aria-hidden="true">
          <span>{person.name.split(' ').map((part) => part[0]).join('')}</span>
          <img src={`https://github.com/${person.github}.png?size=96`} alt="" width="56" height="56" loading="lazy" referrerPolicy="no-referrer" onError={(event) => { event.currentTarget.hidden = true; }} />
        </div>
        <div><h3>{person.name}</h3><p>Programador</p><a href={`https://github.com/${person.github}`} target="_blank" rel="noreferrer noopener" aria-label={`${person.user}, GitHub de ${person.name} (nueva pestaña)`}>{person.user} <span aria-hidden="true">↗</span></a></div>
      </li>)}</ul>
    </section>
    <div className="about-details">
      <section aria-labelledby="about-history-title">
        <h2 id="about-history-title">Nuestra historia</h2>
        <ol className="about-timeline">{milestones.map((milestone) => <li key={milestone.date}>
          <time dateTime={milestone.date}>{milestone.label}</time><h3>{milestone.title}</h3>
        </li>)}</ol>
      </section>
      <section className="about-project" aria-labelledby="about-project-title">
        <h2 id="about-project-title">Sobre el proyecto</h2>
        <dl><div><dt>Versión documentada</dt><dd>{documentedVersion}</dd></div></dl>
        <p>Según el registro de cambios del proyecto.</p>
        <a href={repository} target="_blank" rel="noreferrer noopener">Repositorio público en GitHub <span aria-hidden="true">↗</span></a>
        <h3>Contacto</h3>
        <p>Encontrá al equipo en los perfiles de GitHub de esta página y consultá el código en el repositorio público.</p>
      </section>
    </div>
  </InformationLayout>;
}
