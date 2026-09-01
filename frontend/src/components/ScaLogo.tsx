export default function ScaLogo({ className = '' }: { className?: string }) {
  return <img
    className={`sca-logo ${className}`.trim()}
    src="/favicon.svg"
    alt="Logo del Sistema de Carpetas Académicas"
  />;
}
