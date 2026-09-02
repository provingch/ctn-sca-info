> **Nota:*** este reporte describe el diseño original del export de horario a Excel
> (`HorarioWorkbookBuilder`), que fue reemplazado por el export a PDF. Se conserva
> solo como referencia histórica.

# Paso 0 — Investigación y decisiones (resumen)

Fecha: 2026-08-29

Hallazgos principales:

- El export actual está implementado por `HorarioController` y `HorarioWorkbookBuilder` (generación programática via Apache POI).
- La tabla `sala` existe y `horario_slot` ya contiene `sala_id` con FK (ver `database/db-tables-properties.sql`). No se necesita migración DB.
- La paleta de colores por especialidad vive en `frontend/src/index.css` como variables CSS (`--accent`, `--hero-tone`).
- Los logos fuente se centralizaron en `backend/src/main/resources/logos-source/` para no depender de hashes de build de Vite. Para incrustarlos en XLSX via POI se convierten a PNG raster y se colocan en `backend/src/main/resources/static/assets/png/` con nombres estables `logo-especialidad-{normalized}.png`, además de `backend/src/main/resources/static/logo-institucional.png`.

Decisiones de implementación:

1. El builder de horarios ahora usa estilos por hoja y aplica el color de `RECESO` según la especialidad. Se añadió `SpecialtyColors` (mapa de especialidad normalizada -> HEX) en `backend/src/main/java/ctn/informatica/sca/util/SpecialtyColors.java`.
2. `HorarioWorkbookBuilder` fue actualizado para:
   - Crear `Styles` por hoja usando la especialidad del `CursoBase`, de modo que la fila `RECESO` usa el color correspondiente.
   - Intentar insertar `logo-institucional.png` y, si existe, `logo-especialidad-{normalized}.png` en la cabecera.
   - Mantener la generación programática de filas/columnas, pero reutilizando estilos (ahora por hoja) en vez de valores hardcodeados.
3. `HorarioController.export` fue modificado para que, al pedir exportar un `cursoId`, el endpoint genere una hoja por cada `curso_base` que tenga la misma `especialidad_id` y `nivel` (es decir, todas las secciones del mismo curso). Para esto reutiliza `HorarioWorkbookBuilder.buildEspecialidad(...)`.
4. Añadí un script `scripts/convert-logos.sh` que toma los SVGs estables desde `backend/src/main/resources/logos-source/`, escribe `backend/src/main/resources/static/logo-institucional.png` para el logo institucional y genera `backend/src/main/resources/static/assets/png/logo-especialidad-*.png` para las especialidades.

Archivos nuevos/modificados importantes:

- Añadidos:
  - `backend/src/main/java/ctn/informatica/sca/util/SpecialtyColors.java`  (mapa de colores por especialidad)
  - `scripts/convert-logos.sh` (script de conversión SVG→PNG, usa `rsvg-convert` o `inkscape`)
  - `backend/src/main/resources/static/assets/README.md` (doc sobre logos)
  - `docs/PASO0_REPORT.md` (este archivo)

- Modificados:
  - `backend/src/main/java/ctn/informatica/sca/util/HorarioWorkbookBuilder.java` (uso de estilos por hoja, inserción de logo de especialidad, color RECESO según especialidad)
  - `backend/src/main/java/ctn/informatica/sca/controller/HorarioController.java` (export ahora genera hoja por sección del mismo nivel+especialidad)

Pasos recomendados para finalizar y commitear:

1. Ejecutar la conversión de SVGs a PNGs (en el workspace):

```bash
chmod +x scripts/convert-logos.sh
./scripts/convert-logos.sh
```

2. Verificar que los PNGs resultantes estén en `backend/src/main/resources/static/assets/png/` con nombres `logo-especialidad-{normalized}.png` y que exista `backend/src/main/resources/static/logo-institucional.png`. El export buscará esas rutas.

3. Revisar y correr tests/build:

```bash
mvn -q -DskipTests=false package
```

4. Commit de los cambios con mensaje que incluya este reporte Paso 0:

```bash
git add -A
git commit -m "Export horario: plantilla y builder — Paso 0 research + cambios iniciales. Incluye SpecialtyColors, builder/controller updates, logo conversion script. Ver docs/PASO0_REPORT.md"
```

Notas:
- El mapeo de colores en `SpecialtyColors` debe mantenerse sincronizado con `frontend/src/index.css`. Si en el futuro se desea única fuente de verdad, podemos extraer tokens CSS a un archivo JSON compartido.
- La inserción de imágenes espera PNGs raster; POI no inserta SVG directamente. Por eso se agregó el script de conversión.
