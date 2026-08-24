import type { ReactNode } from 'react';
import { normalizeGrade } from './grade';

interface GradeChipProps {
  grade: number;
  label?: ReactNode;
  title?: string;
  className?: string;
}

export default function GradeChip({ grade, label, title, className = '' }: GradeChipProps) {
  const normalizedGrade = normalizeGrade(grade);
  return <span
    className={`grade-chip grade-chip--${normalizedGrade}${className ? ` ${className}` : ''}`}
    title={title}
    aria-label={label == null ? `Nota ${grade}` : undefined}
  >
    {label ?? grade}
  </span>;
}
