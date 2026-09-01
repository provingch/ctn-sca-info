import { act, renderHook } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useAutoDismissStatus } from './useAutoDismissStatus';

afterEach(() => {
  vi.useRealTimers();
});

describe('useAutoDismissStatus', () => {
  it('limpia el aviso después del tiempo configurado', () => {
    vi.useFakeTimers();
    const { result } = renderHook(() => useAutoDismissStatus('', { delay: 4000 }));

    act(() => {
      result.current.setStatus('Guardado correctamente.');
    });

    expect(result.current.status).toBe('Guardado correctamente.');

    act(() => {
      vi.advanceTimersByTime(4000);
    });

    expect(result.current.status).toBe('');
  });

  it('permite desactivar el auto-dismiss para avisos persistentes', () => {
    vi.useFakeTimers();
    const { result } = renderHook(() => useAutoDismissStatus('', { autoDismiss: false }));

    act(() => {
      result.current.setStatus('Error persistente');
    });

    act(() => {
      vi.advanceTimersByTime(10000);
    });

    expect(result.current.status).toBe('Error persistente');
  });
});
