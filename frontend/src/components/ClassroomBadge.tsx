interface ClassroomBadgeProps {
  className?: string;
  label?: string;
}

export default function ClassroomBadge({ className = '', label = 'Classroom' }: ClassroomBadgeProps) {
  return (
    <span className={`classroom-badge ${className}`.trim()} aria-label="Tarea importada desde Google Classroom">
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M12 3.5a2.5 2.5 0 0 1 2.44 1.8l.54 2.1 2.25-.7a.85.85 0 0 1 1.08.95v5.4a.85.85 0 0 1-1.08.96l-2.25-.7-.54 2.1A2.5 2.5 0 0 1 12 20.5a2.5 2.5 0 0 1-2.43-1.79l-.54-2.1-2.25.7a.85.85 0 0 1-1.08-.96V8.66a.85.85 0 0 1 1.08-.95l2.25.7.54-2.1A2.5 2.5 0 0 1 12 3.5Zm-1.22 5.18-.5 2.13a1.14 1.14 0 0 0 .84 1.37l1.52.38a1.14 1.14 0 0 0 1.36-.83l.5-2.13-1.4-.94-1.42.94Z"/>
      </svg>
      <span>{label}</span>
    </span>
  );
}
