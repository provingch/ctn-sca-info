"""Genera el SQL que deja una base LOCAL (schema + ctn-official-seed.sql) igual a prod en lo que usan los manuales:
cuentas de demo con contraseña conocida, el padre d.estigarribia, las planillas 1-3, sus registros y el plan 1.
Después se aplica demo-insert.sql encima. Solo para la base local de grabación: NUNCA contra prod.

Uso: python3 -I manuales/datos-demo/local-setup.py | mariadb -uroot -S /tmp/sca-local/s.sock ctndb
"""
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
PLANTILLA = RAIZ / "backend/src/main/resources/plan-curricular-plantilla-etapa2.xlsx"
# BCrypt de "demo1234" (contraseña solo local)
HASH = "$2a$10$cnk9M4rngOsG6hxBJApNMOAiyEHZUJV.qCSud/HvJd5jJCLnqIk.a"
CUENTAS = ["graciela.lopez", "evaluador2", "admin_informatica", "global_admin", "cpdg1", "federico.gonzalez"]

print("SET NAMES utf8mb4; START TRANSACTION;")
print("CREATE TABLE IF NOT EXISTS schema_migrations (version VARCHAR(255) PRIMARY KEY, applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;")
print("INSERT IGNORE INTO schema_migrations (version) VALUES ('006_add_gema_tarea_id.sql');  -- el schema ya trae la columna")
print(f"UPDATE usuario SET contrasenia = '{HASH}', totp_secret = NULL WHERE usuario IN ({', '.join(repr(c) for c in CUENTAS)});")

# Padre de Thiago (alumno 291). En prod es el usuario 149, cargado a mano (no está en el seed). Datos de contacto ficticios.
print(f"""INSERT INTO usuario (id, ci, nombre, apellido, usuario, contrasenia, nivel, correo, telefono)
VALUES (149, 1000149, 'Domiciano', 'Estigarribia Duarte', 'd.estigarribia', '{HASH}', 4, 'padre.demo@ejemplo.com', '0981000149');""")
print("INSERT INTO alumno_usuario (alumno_id, usuario_id, parentesco) VALUES (291, 149, 'Padre');")

# Planillas como en prod: 1-2 Laboratorio Java (Federico) primera/segunda, 3 Algorítmica (Graciela) primera; todas 3°A (curso 31).
print("""INSERT INTO planilla (id, curso_id, materia_id, periodo, etapa, usuario_id) VALUES
(1, 31, 43, 2026, 'primera', 66), (2, 31, 43, 2026, 'segunda', 66), (3, 31, 41, 2026, 'primera', 81);""")
print("INSERT INTO registro (planilla_id, alumno_id) SELECT p.id, a.id FROM planilla p JOIN alumno a ON a.curso_id = p.curso_id;")

# Tareas de Laboratorio Java con notas, para que la vista de padres tenga una segunda materia con datos.
print("""INSERT INTO tarea (planilla_id, instrumento_id, fecha, total, titulo, fecha_inicio, fecha_limite) VALUES
(1, 2, '2026-05-14', 10, 'Laboratorio: clases y objetos', '2026-05-07', '2026-05-14'),
(1, 4, '2026-07-02', 20, 'Prueba de cierre: herencia', '2026-07-02', '2026-07-02');""")
print("""INSERT INTO puntaje (registro_id, tarea_id, puntos)
SELECT r.id, t.id, LEAST(t.total, FLOOR(t.total * (0.55 + ((r.alumno_id * 13) % 45) / 100))) FROM registro r JOIN tarea t ON t.planilla_id = r.planilla_id WHERE r.planilla_id = 1;""")

# Plan 1: Federico, Laboratorio Java 3°A etapa 2, aprobado. Su archivo se copia a los planes de ejemplo.
blob = PLANTILLA.read_bytes().hex()
print(f"""INSERT INTO plan_curricular (id, asignacion_id, etapa, anio_lectivo, archivo_nombre, archivo_contenido, estado, fecha_subida, fecha_revision, evaluador_id)
VALUES (1, 74, '2', 2026, 'plan-curricular-plantilla.xlsx', 0x{blob}, 'APROBADO', '2026-09-30 09:00:00', '2026-09-30 12:00:00', 136);""")
print("""INSERT INTO tema_plan_curricular (plan_curricular_id, mes, orden_mes, bloque, temas_contenidos, estado_cobertura)
VALUES (1, 'Septiembre', 3, 1, 'Herencia y polimorfismo', 'PENDIENTE');""")
# (demo-insert agrega las tareas de Algorítmica; la "no entregada" de Thiago se marca en levantar-local.sh, después)
print("COMMIT;")
