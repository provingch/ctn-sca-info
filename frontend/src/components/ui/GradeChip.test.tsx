import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import GradeChip from './GradeChip';
import { normalizeGrade } from './grade';

describe('GradeChip', () => {
  it('limita visualmente las notas al rango de 1 a 5', () => {
    expect(normalizeGrade(0)).toBe(1);
    expect(normalizeGrade(3)).toBe(3);
    expect(normalizeGrade(8)).toBe(5);
  });

  it('mantiene el valor original en el contenido y la etiqueta accesible', () => {
    render(<GradeChip grade={4} />);

    expect(screen.getByLabelText('Nota 4')).toHaveClass('grade-chip--4');
  });
});
