export interface ActivityLine {
  date: string;
  /** IP desde la que se hizo la acción; las líneas anteriores al registro de IP no la traen. */
  ip: string | null;
  message: string;
}

/**
 * Línea del registro de actividad (ActivityLogService): {@code [fecha hora] (ip: 1.2.3.4) acción}. Las líneas viejas no
 * tienen el tramo de la IP. La IP se separa del mensaje para que la actividad reciente muestre sólo la acción.
 */
export function splitActivityLine(line: string): ActivityLine | null {
  if (!line.startsWith('[')) return null;
  const closeIdx = line.indexOf('] ');
  if (closeIdx <= 0) return null;
  const date = line.slice(1, closeIdx);
  const rest = line.slice(closeIdx + 2);
  const match = rest.match(/^\(ip: ([^)]*)\) (.*)$/s);
  return match ? { date, ip: match[1], message: match[2] } : { date, ip: null, message: rest };
}
