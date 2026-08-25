import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import SpecialtyIcon from './SpecialtyIcon';

describe('SpecialtyIcon', () => {
  it('matches known specialty variants without accents', () => {
    render(<SpecialtyIcon name="Mecánica General" />);

    expect(screen.getByRole('img', { name: 'Especialidad Mecánica General' }).querySelector('svg')).not.toBeNull();
  });

  it('uses an initial fallback for an unknown specialty', () => {
    render(<SpecialtyIcon name="Especialidad nueva" />);

    expect(screen.getByLabelText('Especialidad Especialidad nueva')).toHaveTextContent('E');
  });
});
