import { describe, expect, it } from 'vitest';
import { splitActivityLine } from './activityLine';

describe('splitActivityLine', () => {
  it('separa fecha, IP y mensaje de una línea con IP', () => {
    expect(splitActivityLine('[2026-09-20 10:15:00] (ip: 203.0.113.9) Registró clase — 1° A — tema: Redes'))
      .toEqual({ date: '2026-09-20 10:15:00', ip: '203.0.113.9', message: 'Registró clase — 1° A — tema: Redes' });
  });

  it('sigue leyendo las líneas viejas, sin IP', () => {
    expect(splitActivityLine('[2026-01-01 10:00:00] Inició sesión'))
      .toEqual({ date: '2026-01-01 10:00:00', ip: null, message: 'Inició sesión' });
  });

  it('el mensaje de un inicio de sesión queda exacto para poder filtrarlo, con o sin IP', () => {
    expect(splitActivityLine('[2026-09-20 10:15:00] (ip: 2001:db8::1) Inició sesión')?.message).toBe('Inició sesión');
    expect(splitActivityLine('[2026-09-20 10:15:00] (ip: desconocida) Inició sesión')?.message).toBe('Inició sesión');
  });

  it('un paréntesis dentro de la acción no se confunde con la IP', () => {
    expect(splitActivityLine('[2026-09-20 10:15:00] (ip: 1.2.3.4) Editó asignación #3 (materia 2, curso 5)')?.message)
      .toBe('Editó asignación #3 (materia 2, curso 5)');
  });

  it('una línea que no tiene el formato devuelve null', () => {
    expect(splitActivityLine('texto suelto')).toBeNull();
    expect(splitActivityLine('[sin cierre')).toBeNull();
  });
});
