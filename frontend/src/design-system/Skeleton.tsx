export interface SkeletonProps { label?: string; lines?: number; decorative?: boolean }
/** One accessible status per block; decorative lines never enter the a11y tree. */
export function Skeleton({ label = 'Cargando contenido…', lines = 3, decorative = false }: SkeletonProps) {
  return <div className="ds-skeleton" role={decorative ? undefined : 'status'} aria-hidden={decorative || undefined}>{!decorative && <span className="ds-sr-only">{label}</span>}<div aria-hidden="true">{Array.from({ length: Math.min(12, Math.max(1, Math.floor(lines) || 1)) }, (_, index) => <span className="ds-skeleton-line" key={index} />)}</div></div>;
}
