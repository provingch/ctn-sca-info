"""Convierte la planilla exportada (.xlsx) en una imagen de una sola página y
devuelve dónde quedó el bloque de la firma, para marcarlo en el manual.

Uso: python3 -I excel-a-imagen.py <planilla.xlsx> <salida.png>
Imprime JSON: {"ancho", "alto", "firma": {x, y, w, h}} en píxeles de la imagen.
Requiere LibreOffice (soffice) y poppler (pdftoppm, pdftotext).
"""
import json
import re
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

DPI = 200
xlsx, png = Path(sys.argv[1]), Path(sys.argv[2])
tmp = Path(tempfile.mkdtemp())

# Ajustar la hoja a una página apaisada: sin esto LibreOffice la parte en varias.
fit = tmp / "planilla.xlsx"
with zipfile.ZipFile(xlsx) as src, zipfile.ZipFile(fit, "w", zipfile.ZIP_DEFLATED) as out:
    for name in src.namelist():
        data = src.read(name)
        if re.fullmatch(r"xl/worksheets/sheet\d+\.xml", name):
            x = data.decode()
            x = re.sub(r"(<worksheet[^>]*>)", r'\1<sheetPr><pageSetUpPr fitToPage="1"/></sheetPr>', x, count=1)
            setup = '<pageSetup paperSize="9" orientation="landscape" fitToWidth="1" fitToHeight="1"/>'
            if "<pageMargins" in x:
                x = re.sub(r"(<pageMargins[^>]*/>)", r"\1" + setup, x, count=1)
            else:
                x = re.sub(r"(<drawing |<legacyDrawing|</worksheet>)", setup + r"\1", x, count=1)
            data = x.encode()
        out.writestr(name, data)

subprocess.run(["soffice", "--headless", f"-env:UserInstallation=file://{tmp}/lo", "--convert-to", "pdf",
                "--outdir", str(tmp), str(fit)], check=True, capture_output=True, timeout=120)
pdf = tmp / "planilla.pdf"
subprocess.run(["pdftoppm", "-r", str(DPI), "-png", "-singlefile", "-f", "1", "-l", "1", str(pdf), str(png.with_suffix(""))], check=True)

# Ubicar "Firma del Docente" y lo que está justo arriba (la imagen o el nombre del profesor).
bbox = subprocess.run(["pdftotext", "-bbox", "-f", "1", "-l", "1", str(pdf), "-"], check=True, capture_output=True, text=True).stdout
page = re.search(r'<page width="([\d.]+)" height="([\d.]+)"', bbox)
words = [(float(a), float(b), float(c), float(d), w) for a, b, c, d, w in
         re.findall(r'<word xMin="([\d.]+)" yMin="([\d.]+)" xMax="([\d.]+)" yMax="([\d.]+)">([^<]*)</word>', bbox)]
firma = next(w for w in words if w[4] == "Firma")
k = DPI / 72
x0, y1 = firma[0], firma[3]
y0 = y1 - 48  # la firma (o el nombre) queda justo encima del rótulo
print(json.dumps({
    "ancho": round(float(page[1]) * k), "alto": round(float(page[2]) * k),
    "firma": {"x": round((x0 - 6) * k), "y": round(y0 * k), "w": round(190 * k), "h": round((y1 - y0 + 4) * k)},
}))
