import { siGoogleclassroom } from 'simple-icons';

interface ClassroomBadgeProps {
  className?: string;
  label?: string;
  iconOnly?: boolean;
}

export default function ClassroomBadge({ className = '', label = 'Classroom', iconOnly = false }: ClassroomBadgeProps) {
  return (
    <span
      className={`classroom-badge ${iconOnly ? 'classroom-badge--icon-only' : ''} ${className}`.trim()}
      aria-label={iconOnly ? label : 'Tarea importada desde Google Classroom'}
    >
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d={siGoogleclassroom.path} />
      </svg>
      {!iconOnly && <span>{label}</span>}
    </span>
  );
}
