// React import not required in modern JSX runtimes
import { render, screen, act } from '@testing-library/react';
import { describe, it, expect, vi, afterEach } from 'vitest';
import { ToastProvider, useToast } from './ToastContext';

function Trigger({ message, opts }: { message: string; opts?: any }) {
  const { showToast } = useToast();
  return <button onClick={() => showToast(message, opts)}>Trigger</button>;
}

describe('ToastProvider', () => {
  afterEach(() => vi.useRealTimers());

  it('auto-dismisses toasts after 4000ms', () => {
    vi.useFakeTimers();
    render(
      <ToastProvider>
        <Trigger message="Hello" />
      </ToastProvider>
    );

    act(() => { screen.getByText('Trigger').click(); });
    expect(screen.getByText('Hello')).toBeInTheDocument();

    act(() => { vi.advanceTimersByTime(4000); });
    expect(screen.queryByText('Hello')).toBeNull();
  });

  it('does not auto-dismiss when autoDismiss is false and shows close button', () => {
    vi.useFakeTimers();
    render(
      <ToastProvider>
        <Trigger message="Persistent" opts={{ autoDismiss: false }} />
      </ToastProvider>
    );

    act(() => { screen.getByText('Trigger').click(); });
    expect(screen.getByText('Persistent')).toBeInTheDocument();
    expect(screen.getByLabelText('Cerrar')).toBeInTheDocument();

    act(() => { vi.advanceTimersByTime(10000); });
    expect(screen.getByText('Persistent')).toBeInTheDocument();
  });

  it('prevents duplicate identical toasts and removes single toast after delay', () => {
    vi.useFakeTimers();
    render(
      <ToastProvider>
        <Trigger message="Dup" />
        <Trigger message="Dup" />
      </ToastProvider>
    );

    act(() => { const buttons = screen.getAllByText('Trigger'); buttons[0].click(); buttons[1].click(); });

    const items = screen.getAllByText('Dup');
    expect(items.length).toBe(1);

    act(() => { vi.advanceTimersByTime(4000); });
    expect(screen.queryByText('Dup')).toBeNull();
  });
});
