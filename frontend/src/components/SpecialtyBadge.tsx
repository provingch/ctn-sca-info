import { normalizeSpecialty } from '../theme/theme';
import './SpecialtyBadge.css';

export default function SpecialtyBadge({ name, count }: { name: string; count?: number }) {
  return <span className="specialty-badge" data-specialty={name === 'Comunes' || name === 'Sin especialidad' ? 'general' : normalizeSpecialty(name)}><i aria-hidden="true" />{name}{count !== undefined && <strong>{count}</strong>}</span>;
}
