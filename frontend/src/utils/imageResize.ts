const MAX_DIMENSION = 1200;
const QUALITY = 0.8;

function loadImageElement(file: File): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => { URL.revokeObjectURL(url); resolve(img); };
    img.onerror = () => { URL.revokeObjectURL(url); reject(new Error('No se pudo leer la imagen seleccionada.')); };
    img.src = url;
  });
}

/**
 * Redimensiona una imagen en el cliente antes de subirla: el lado mayor queda
 * en 1200px y se codifica en WEBP (calidad 0.8), con fallback a JPEG si el
 * navegador no soporta WEBP. Una foto de celular sin procesar (3-5 MB) suele
 * superar el tope del backend (600 KB); esto la deja muy por debajo.
 */
export async function resizeImageToDataUri(file: File): Promise<string> {
  const img = await loadImageElement(file);
  const scale = Math.min(1, MAX_DIMENSION / Math.max(img.naturalWidth, img.naturalHeight));
  const width = Math.max(1, Math.round(img.naturalWidth * scale));
  const height = Math.max(1, Math.round(img.naturalHeight * scale));

  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext('2d');
  if (!ctx) throw new Error('No se pudo procesar la imagen en este navegador.');
  ctx.drawImage(img, 0, 0, width, height);

  const webp = canvas.toDataURL('image/webp', QUALITY);
  if (webp.startsWith('data:image/webp')) return webp;
  return canvas.toDataURL('image/jpeg', QUALITY);
}
