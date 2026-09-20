import type { ReactNode } from 'react';

/** Un control de la barra de filtros con su etiqueta visible arriba. */
export function FilterField({ label, narrow = false, children }: { label: string; narrow?: boolean; children: ReactNode }) {
  return <label className={`filter-field${narrow ? ' filter-field--narrow' : ''}`}>{label}{children}</label>;
}

/**
 * Barra de filtros compartida (.toolbar.filters): los controles arriba de un listado largo, más el conteo
 * "x de y" y, si hay algún filtro activo, "Limpiar filtros". Los controles (AnimatedSelect, DatePicker, buscador
 * de texto) van como hijos dentro de FilterField.
 */
export default function FiltersToolbar({ ariaLabel, children, mostrando, total, unidad, activo, onLimpiar }: {
  ariaLabel: string;
  children: ReactNode;
  mostrando: number;
  total: number;
  /** Lo que se cuenta, en plural: "materias", "notas", "alumnos"… */
  unidad: string;
  activo: boolean;
  onLimpiar: () => void;
}) {
  return <div className="toolbar filters filters--fields" role="search" aria-label={ariaLabel}>
    {children}
    <div className="filters-summary" role="status" aria-live="polite">
      <span>{mostrando} de {total} {unidad}</span>
      {activo && <button type="button" className="button secondary" onClick={onLimpiar}>Limpiar filtros</button>}
    </div>
  </div>;
}
