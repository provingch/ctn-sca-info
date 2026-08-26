import { siGoogleclassroom } from 'simple-icons';

interface ClassroomBadgeProps {
  className?: string;
  label?: string;
}

export default function ClassroomBadge({ className = '', label = 'Classroom' }: ClassroomBadgeProps) {
  return (
    <span className={`classroom-badge ${className}`.trim()} aria-label="Tarea importada desde Google Classroom">
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d={siGoogleclassroom.path} />
      </svg>
      <span>{label}</span>
    </span>
  );
}
