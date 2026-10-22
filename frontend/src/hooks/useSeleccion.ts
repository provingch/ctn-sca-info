import { useCallback, useEffect, useMemo, useState } from 'react';

/**
 * Selección múltiple de filas de una tabla. `visibles` son los ids que se ven ahora (con filtros):
 * "seleccionar todo" actúa sobre ellos y los ids que dejan de existir se descartan solos.
 */
export default function useSeleccion<T extends number | string>(visibles: T[]) {
  const [seleccion, setSeleccion] = useState<Set<T>>(() => new Set());
  const clave = visibles.join('|');

  // Al filtrar o recargar, que no queden elegidas filas que ya no se ven.
  useEffect(() => {
    setSeleccion((actual) => {
      const vigentes = new Set(visibles);
      const siguiente = new Set([...actual].filter((id) => vigentes.has(id)));
      return siguiente.size === actual.size ? actual : siguiente;
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clave]);

  const toggle = useCallback((id: T) => setSeleccion((actual) => {
    const siguiente = new Set(actual);
    if (!siguiente.delete(id)) siguiente.add(id);
    return siguiente;
  }), []);

  const todos = visibles.length > 0 && visibles.every((id) => seleccion.has(id));
  const algunos = !todos && visibles.some((id) => seleccion.has(id));

  const toggleTodos = useCallback(() => setSeleccion(todos ? new Set() : new Set(visibles)),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [todos, clave]);
  const limpiar = useCallback(() => setSeleccion(new Set()), []);

  return useMemo(() => ({
    seleccion,
    ids: [...seleccion],
    cantidad: seleccion.size,
    estaSeleccionado: (id: T) => seleccion.has(id),
    toggle,
    toggleTodos,
    limpiar,
    todos,
    algunos,
  }), [seleccion, toggle, toggleTodos, limpiar, todos, algunos]);
}
