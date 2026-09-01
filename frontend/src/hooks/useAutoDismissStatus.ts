import { useEffect, useRef, useState } from 'react';

export interface UseAutoDismissStatusOptions {
  delay?: number;
  autoDismiss?: boolean;
}

export function useAutoDismissStatus(initialStatus = '', options: UseAutoDismissStatusOptions = {}) {
  const { delay = 4000, autoDismiss = true } = options;
  const [status, setStatus] = useState(initialStatus);
  const timeoutRef = useRef<number | null>(null);

  useEffect(() => {
    if (!status || !autoDismiss) return;

    if (timeoutRef.current !== null) {
      window.clearTimeout(timeoutRef.current);
    }

    timeoutRef.current = window.setTimeout(() => {
      setStatus('');
      timeoutRef.current = null;
    }, delay);

    return () => {
      if (timeoutRef.current !== null) {
        window.clearTimeout(timeoutRef.current);
        timeoutRef.current = null;
      }
    };
  }, [autoDismiss, delay, status]);

  return { status, setStatus };
}
