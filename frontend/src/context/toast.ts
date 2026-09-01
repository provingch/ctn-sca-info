import { createContext, useContext } from 'react';

export type ToastTone = 'warning' | 'error' | 'success';

export interface ToastContextValue {
  showToast: (message: string, opts?: { tone?: ToastTone; autoDismiss?: boolean }) => string;
  dismissToast: (id: string) => void;
}

export const ToastContext = createContext<ToastContextValue | null>(null);

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) throw new Error('useToast must be used within ToastProvider');
  return context;
}
