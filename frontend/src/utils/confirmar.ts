// Reemplazo de window.confirm: el navegador lo puede silenciar ("evitar que esta página cree diálogos")
// y ahí devuelve false sin mostrar nada, así que el botón parece muerto.
export function confirmar(mensaje: string, aceptar = 'Aceptar'): Promise<boolean> {
  return new Promise((resolve) => {
    const dialog = document.createElement('dialog');
    dialog.className = 'confirm-dialog';
    const texto = document.createElement('p');
    texto.textContent = mensaje;
    const acciones = document.createElement('div');
    acciones.className = 'confirm-dialog-actions';
    const cancel = Object.assign(document.createElement('button'), { type: 'button', className: 'button secondary', textContent: 'Cancelar' });
    const ok = Object.assign(document.createElement('button'), { type: 'button', className: 'button', textContent: aceptar });
    acciones.append(cancel, ok);
    dialog.append(texto, acciones);

    let result = false;
    cancel.onclick = () => dialog.close();
    ok.onclick = () => { result = true; dialog.close(); };
    dialog.onclose = () => { dialog.remove(); resolve(result); }; // Esc también cierra = cancelar
    dialog.onkeydown = (event) => event.stopPropagation(); // que el Esc no cierre también el modal de abajo
    document.body.append(dialog);
    dialog.showModal();
    ok.focus();
  });
}
