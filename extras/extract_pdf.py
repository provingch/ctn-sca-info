from pathlib import Path
import sys
import os

sys.path.append(r"C:\Users\soysa\AppData\Roaming\Python\Python314\site-packages")

pdf_path = Path(r"c:\Users\soysa\Desktop\ProyectoPrime\extras\Proyectada.pdf")
print('exists', pdf_path.exists())
if not pdf_path.exists():
    sys.exit(1)

try:
    from pypdf import PdfReader
except Exception as e:
    print('pypdf import failed:', e)
    sys.exit(2)

reader = PdfReader(str(pdf_path))
print('pages', len(reader.pages))
for i, page in enumerate(reader.pages, 1):
    text = page.extract_text() or ''
    print(f'--- PAGE {i} ---')
    print(text[:5000])
    print()
