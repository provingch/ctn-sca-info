import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { ToastContext, type ToastTone } from './toast';

interface ToastItem {
  id: string;
  message: string;
  tone: ToastTone;
  autoDismiss: boolean;
  exiting?: boolean;
}

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const timers = useRef<Record<string, number | null>>({});
  const exitTimers = useRef<Record<string, number | null>>({});
  const toastIdsByKey = useRef(new Map<string, string>());
  const toastKeysById = useRef(new Map<string, string>());
  const nextId = useRef(1);
  const FADE_MS = 240;

  const finalizeRemove = useCallback((id: string) => {
    setToasts((current) => current.filter((t) => t.id !== id));
    const key = toastKeysById.current.get(id);
    if (key) toastIdsByKey.current.delete(key);
    toastKeysById.current.delete(id);
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
    setToasts((current) => current.map((t) => (t.id === id ? { ...t, exiting: true } : t)));
    const timer = timers.current[id];
    if (timer) {
      window.clearTimeout(timer);
      timers.current[id] = null;
    }
    const previousExitTimer = exitTimers.current[id];
    if (previousExitTimer) window.clearTimeout(previousExitTimer);
    exitTimers.current[id] = window.setTimeout(() => finalizeRemove(id), FADE_MS);
  }, [FADE_MS, finalizeRemove]);

  const showToast = useCallback((message: string, opts?: { tone?: ToastTone; autoDismiss?: boolean }) => {
    const tone = opts?.tone ?? 'warning';
    const autoDismiss = opts?.autoDismiss ?? true;

    const key = `${tone}\u0000${message}`;
    const existingId = toastIdsByKey.current.get(key);

    if (existingId) {
      const timer = timers.current[existingId];
      if (timer) window.clearTimeout(timer);
      const exitTimer = exitTimers.current[existingId];
      if (exitTimer) window.clearTimeout(exitTimer);
      exitTimers.current[existingId] = null;
      setToasts((current) => current.map((toast) => toast.id === existingId
        ? { ...toast, autoDismiss, exiting: false }
        : toast));
      if (autoDismiss) {
        const delay = Math.max(0, 4000 - FADE_MS);
        timers.current[existingId] = window.setTimeout(() => startExit(existingId), delay);
      } else timers.current[existingId] = null;
      return existingId;
    }

    const id = String(nextId.current++);
    toastIdsByKey.current.set(key, id);
    toastKeysById.current.set(id, key);
    setToasts((current) => [...current, { id, message, tone, autoDismiss }]);
    if (autoDismiss) {
      const delay = Math.max(0, 4000 - FADE_MS);
      const timer = window.setTimeout(() => startExit(id), delay);
      timers.current[id] = timer;
    }
    return id;
  }, [startExit]);

  const dismissToast = useCallback((id: string) => startExit(id), [startExit]);
  const contextValue = useMemo(() => ({ showToast, dismissToast }), [dismissToast, showToast]);

  useEffect(() => {
    const autoDismissTimers = timers.current;
    const removalTimers = exitTimers.current;
    const idsByKey = toastIdsByKey.current;
    const keysById = toastKeysById.current;
    return () => {
      Object.values(autoDismissTimers).forEach((timer) => { if (timer) window.clearTimeout(timer); });
      Object.values(removalTimers).forEach((timer) => { if (timer) window.clearTimeout(timer); });
      idsByKey.clear();
      keysById.clear();
    };
  }, []);

  return (
    <ToastContext.Provider value={contextValue}>
      {children}
      <div className="toast-region" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`toast toast--${t.tone}${t.exiting ? ' exiting' : ''}`} role={t.tone === 'error' ? 'alert' : 'status'}>
            <span className="toast-icon" aria-hidden="true">{t.tone === 'error' ? '!' : t.tone === 'success' ? '✓' : 'i'}</span>
            <div className="toast-message">{t.message}</div>
            <button type="button" aria-label="Cerrar" onClick={() => dismissToast(t.id)} className="toast-close">×</button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export default ToastProvider;
