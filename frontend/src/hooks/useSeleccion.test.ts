import { act, renderHook } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import useSeleccion from './useSeleccion';

describe('useSeleccion', () => {
  it('marca filas sueltas, todas las visibles y queda a medias', () => {
    const { result } = renderHook(({ ids }) => useSeleccion(ids), { initialProps: { ids: [1, 2, 3] } });
    act(() => result.current.toggle(2));
    expect(result.current.ids).toEqual([2]);
    expect(result.current.algunos).toBe(true);
    act(() => result.current.toggleTodos());
    expect(result.current.todos).toBe(true);
    expect(result.current.cantidad).toBe(3);
    act(() => result.current.toggleTodos());
    expect(result.current.cantidad).toBe(0);
  });

  it('descarta las filas que dejan de verse al filtrar', () => {
    const { result, rerender } = renderHook(({ ids }) => useSeleccion(ids), { initialProps: { ids: [1, 2, 3] } });
    act(() => result.current.toggleTodos());
    rerender({ ids: [2, 3] });
    expect(result.current.ids.sort()).toEqual([2, 3]);
    expect(result.current.todos).toBe(true);
  });
});
