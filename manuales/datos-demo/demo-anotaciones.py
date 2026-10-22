"""Anotaciones de ejemplo para Thiago (alumno 291), una con foto de evidencia ficticia (evidencia-demo.jpg).
Las tablas de anotaciones las crean las migraciones del backend: correr DESPUÉS de que el backend arrancó.
Solo para la base local de grabación: NUNCA contra prod.

Uso: python3 -I manuales/datos-demo/demo-anotaciones.py | mariadb -uroot -S /tmp/sca-local/s.sock ctndb
"""
from pathlib import Path

FOTO = Path(__file__).resolve().parent / "evidencia-demo.jpg"
blob = FOTO.read_bytes()

print("SET NAMES utf8mb4; START TRANSACTION;")
print("DELETE FROM anotacion WHERE alumno_id = 291;")
# (tipo por descripción: los id dependen de las migraciones)
for fecha, prof, materia, tipo, obs in [
    ("2026-10-20 07:15:00", 81, 41, "Uniforme incompleto", "Vino sin corbata"),
    ("2026-10-08 10:40:00", 66, 43, "Está fuera de clase en horario establecido", None),
    ("2026-09-15 07:20:00", 81, None, "Otro...", "Llegó sin el cuaderno de comunicaciones firmado"),
]:
    o = "NULL" if obs is None else f"'{obs}'"
    m = "NULL" if materia is None else materia
    print(f"""INSERT INTO anotacion (alumno_id, usuario_id, tipo_id, materia_id, fecha_hora, observacion)
SELECT 291, {prof}, id, {m}, '{fecha}', {o} FROM tipo_anotacion WHERE descripcion = '{tipo}' AND activo;""")
    if obs == "Vino sin corbata":
        print(f"INSERT INTO anotacion_foto (anotacion_id, imagen, tipo_mime, tamano_bytes) VALUES (LAST_INSERT_ID(), 0x{blob.hex()}, 'image/jpeg', {len(blob)});")
print("COMMIT;")
