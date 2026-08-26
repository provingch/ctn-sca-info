import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import useAccessibleDialog from './useAccessibleDialog';

function DialogExample() {
  const [open, setOpen] = useState(false);
  const dialogRef = useAccessibleDialog(open, () => setOpen(false));
  return <>
    <button type="button" onClick={() => setOpen(true)}>Abrir</button>
    {open && <div ref={dialogRef} role="dialog" aria-label="Ejemplo" tabIndex={-1}>
      <button type="button" data-dialog-initial-focus>Primero</button>
      <button type="button" onClick={() => setOpen(false)}>Cerrar</button>
    </div>}
  </>;
}

describe('useAccessibleDialog', () => {
  it('mueve el foco al diálogo, lo cierra con Escape y restaura el origen', async () => {
    render(<DialogExample />);
    const opener = screen.getByRole('button', { name: 'Abrir' });
    opener.focus();
    fireEvent.click(opener);

    await waitFor(() => expect(screen.getByRole('button', { name: 'Primero' })).toHaveFocus());
    fireEvent.keyDown(document, { key: 'Escape' });

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(opener).toHaveFocus();
  });

  it('mantiene Tab dentro del diálogo', async () => {
    const rects = vi.spyOn(HTMLElement.prototype, 'getClientRects').mockReturnValue({
      length: 1,
      item: () => null,
      [Symbol.iterator]: function* iterator() { yield {} as DOMRect; },
    } as unknown as DOMRectList);
    render(<DialogExample />);
    fireEvent.click(screen.getByRole('button', { name: 'Abrir' }));
    const first = screen.getByRole('button', { name: 'Primero' });
    const last = screen.getByRole('button', { name: 'Cerrar' });
    await waitFor(() => expect(first).toHaveFocus());

    last.focus();
    fireEvent.keyDown(document, { key: 'Tab' });
    expect(first).toHaveFocus();

    first.focus();
    fireEvent.keyDown(document, { key: 'Tab', shiftKey: true });
    expect(last).toHaveFocus();
    rects.mockRestore();
  });
});
