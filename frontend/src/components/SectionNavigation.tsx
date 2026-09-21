import { Link } from 'react-router-dom';

type Section = { key: string; label: string } & ({ href: string; onSelect?: never } | { onSelect: () => void; href?: never });

/** Links/actions with the same visual treatment as the professor's section bar. */
export default function SectionNavigation({ sections, active, label }: { sections: Section[]; active?: string; label: string }) {
  return <nav className="catedra-tablist account-section-navigation" aria-label={label}>{sections.map(section => {
    const className = `catedra-tab${active === section.key ? ' active' : ''}`;
    return section.href ? <Link key={section.key} className={className} to={section.href} aria-current={active === section.key ? 'page' : undefined}>{section.label}</Link>
      : <button key={section.key} type="button" className={className} aria-current={active === section.key ? 'page' : undefined} onClick={section.onSelect}>{section.label}</button>;
  })}</nav>;
}
