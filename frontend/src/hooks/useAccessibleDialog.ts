import { useEffect, useRef } from 'react';

const FOCUSABLE_SELECTOR = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled]):not([type="hidden"])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',');

export default function useAccessibleDialog(open: boolean, onClose: () => void) {
  const dialogRef = useRef<HTMLDivElement>(null);
  const onCloseRef = useRef(onClose);
  onCloseRef.current = onClose;

  useEffect(() => {
    if (!open) return;

    const dialog = dialogRef.current;
    const previouslyFocused = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const previousOverflow = document.body.style.overflow;
    const inertedSiblings: Array<{ element: HTMLElement; wasInert: boolean }> = [];
    document.body.style.overflow = 'hidden';

    let activeBranch: HTMLElement | null = dialog;
    let ancestor = activeBranch?.parentElement ?? null;
    while (activeBranch && ancestor && ancestor !== document.body) {
      for (const sibling of ancestor.children) {
        if (sibling !== activeBranch && sibling instanceof HTMLElement) {
          inertedSiblings.push({ element: sibling, wasInert: sibling.inert });
          sibling.inert = true;
        }
      }
      activeBranch = ancestor;
      ancestor = ancestor.parentElement;
    }

    const focusFirstControl = window.requestAnimationFrame(() => {
      const preferred = dialog?.querySelector<HTMLElement>('[data-dialog-initial-focus]');
      const first = preferred ?? dialog?.querySelector<HTMLElement>(FOCUSABLE_SELECTOR) ?? dialog;
      first?.focus();
    });

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        event.preventDefault();
        onCloseRef.current();
        return;
      }
      if (event.key !== 'Tab' || !dialog) return;

      const controls = [...dialog.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR)]
        .filter((element) => element.getClientRects().length > 0);
      if (controls.length === 0) {
        event.preventDefault();
        dialog.focus();
        return;
      }

      const first = controls[0];
      const last = controls[controls.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }

    document.addEventListener('keydown', handleKeyDown);
    return () => {
      window.cancelAnimationFrame(focusFirstControl);
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = previousOverflow;
      inertedSiblings.forEach(({ element, wasInert }) => { element.inert = wasInert; });
      if (previouslyFocused?.isConnected) previouslyFocused.focus();
    };
  }, [open]);

  return dialogRef;
}
