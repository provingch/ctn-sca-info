import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';

export type ToastTone = 'warning' | 'error' | 'success';
export interface ToastItem {
  id: string;
  message: string;
  tone: ToastTone;
  autoDismiss: boolean;
  exiting?: boolean;
}

interface ToastContextValue {
  showToast: (message: string, opts?: { tone?: ToastTone; autoDismiss?: boolean }) => string;
  dismissToast: (id: string) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const timers = useRef<Record<string, number | null>>({});
  const exitTimers = useRef<Record<string, number | null>>({});
  const nextId = useRef(1);

  const finalizeRemove = useCallback((id: string) => {
    setToasts((current) => current.filter((t) => t.id !== id));
    const timer = timers.current[id];
    if (timer) {
      window.clearTimeout(timer);
      timers.current[id] = null;
    }
    const exitTimer = exitTimers.current[id];
    if (exitTimer) {
      window.clearTimeout(exitTimer);
      exitTimers.current[id] = null;
    }
  }, []);

  const startExit = useCallback((id: string) => {
    // mark toast as exiting so CSS can animate out
    setToasts((current) => current.map((t) => (t.id === id ? { ...t, exiting: true } : t)));
    // clear any auto-dismiss timer
    const timer = timers.current[id];
    if (timer) {
      window.clearTimeout(timer);
      timers.current[id] = null;
    }
    // schedule final removal after fade duration
    const FADE_MS = 240;
    exitTimers.current[id] = window.setTimeout(() => finalizeRemove(id), FADE_MS);
  }, [finalizeRemove]);

  const showToast = useCallback((message: string, opts?: { tone?: ToastTone; autoDismiss?: boolean }) => {
    const tone = opts?.tone ?? 'warning';
    const autoDismiss = opts?.autoDismiss ?? true;

    // If an identical toast exists (same message + tone), reset its timer instead of adding a duplicate
    let existingId: string | undefined;
    setToasts((current) => {
      const found = current.find((t) => t.message === message && t.tone === tone);
      if (found) {
        existingId = found.id;
        return current;
      }
      const id = String(nextId.current++);
      return [...current, { id, message, tone, autoDismiss }];
    });

    if (existingId) {
      // reset timer for existing (restart auto-dismiss)
      const timer = timers.current[existingId];
      if (timer) {
        window.clearTimeout(timer);
      }
      if (autoDismiss) {
        timers.current[existingId] = window.setTimeout(() => startExit(existingId!), 4000);
      }
      return existingId;
    }

    const id = String(nextId.current - 1);
    if (autoDismiss) {
      const timer = window.setTimeout(() => startExit(id), 4000);
      timers.current[id] = timer;
    }
    return id;
  }, [startExit]);

  const dismissToast = useCallback((id: string) => startExit(id), [startExit]);

  useEffect(() => {
    return () => {
      // cleanup timers
      Object.values(timers.current).forEach((t) => { if (t) window.clearTimeout(t); });
      Object.values(exitTimers.current).forEach((t) => { if (t) window.clearTimeout(t); });
      timers.current = {};
      exitTimers.current = {};
    };
  }, []);

  return (
    <ToastContext.Provider value={{ showToast, dismissToast }}>
      {children}
      <div aria-live="polite" style={{ position: 'fixed', inset: '12px 0 auto 0', display: 'flex', justifyContent: 'center', pointerEvents: 'none', zIndex: 1200 }}>
        <div style={{ width: 'min(900px, calc(100% - 24px))', display: 'grid', gap: 8 }}>
          {toasts.map((t) => (
            <div key={t.id} style={{ pointerEvents: 'auto' }}>
              <div className={`notice ${t.tone === 'error' ? 'error' : t.tone === 'success' ? 'success' : ''} ${t.exiting ? 'exiting' : ''}`} role={t.tone === 'error' ? 'alert' : 'status'}>
                <div style={{ display: 'flex', gap: 12, alignItems: 'center', justifyContent: 'space-between' }}>
                  <div style={{ flex: 1 }}>{t.message}</div>
                  {!t.autoDismiss && <button type="button" aria-label="Cerrar" onClick={() => startExit(t.id)} className="button secondary" style={{ marginLeft: 12 }}>×</button>}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </ToastContext.Provider>
  );
}

export default ToastProvider;
