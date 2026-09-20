function primeraPalabra(s?: string | null): string {
  if (!s) return '';
  const t = s.trim();
  const sp = t.indexOf(' ');
  return sp < 0 ? t : t.slice(0, sp);
}

/** Primera palabra del nombre + primera palabra del apellido. Sólo para pantallas. */
export function nombreCorto(nombre?: string | null, apellido?: string | null): string {
  const n = primeraPalabra(nombre);
  const a = primeraPalabra(apellido);
  return [n, a].filter(Boolean).join(' ');
}
