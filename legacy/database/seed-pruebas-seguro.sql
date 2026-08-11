-- Seed de pruebas seguro e idempotente para ctndb.
--
-- Garantias:
--   * NO elimina datos ni reinicia AUTO_INCREMENT.
--   * NO actualiza ni elimina filas existentes de profesor o padre.
--   * NO modifica usuarios, contrasenias, roles, especialidades de usuario ni 2FA existentes.
--   * Crea cuentas de demostracion separadas, solo si todavía no existen.
--   * Puede ejecutarse varias veces: solo agrega los datos de prueba que falten.
--
-- Credenciales de las dos cuentas de demostracion:
--   usuario: sca.demo.profesor / contraseña: ctn2025
--   usuario: sca.demo.padre    / contraseña: ctn2025

SET NAMES utf8mb4;
START TRANSACTION;

-- ============================================================
-- ESPECIALIDADES
-- Conserva cualquier fila existente, incluso si usa el mismo id.
-- ============================================================
INSERT INTO especialidad (id, nombre)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM especialidad), 'Construcciones Civiles'
WHERE NOT EXISTS (SELECT 1 FROM especialidad WHERE nombre = 'Construcciones Civiles');
INSERT INTO especialidad (id, nombre)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM especialidad), 'Electricidad'
WHERE NOT EXISTS (SELECT 1 FROM especialidad WHERE nombre = 'Electricidad');
INSERT INTO especialidad (id, nombre)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM especialidad), 'Electrónica'
WHERE NOT EXISTS (SELECT 1 FROM especialidad WHERE nombre = 'Electrónica');
INSERT INTO especialidad (id, nombre)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM especialidad), 'Electromecánica'
WHERE NOT EXISTS (SELECT 1 FROM especialidad WHERE nombre = 'Electromecánica');
INSERT INTO especialidad (id, nombre)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM especialidad), 'Informática'
WHERE NOT EXISTS (SELECT 1 FROM especialidad WHERE nombre = 'Informática');
INSERT INTO especialidad (id, nombre)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM especialidad), 'Mecánica General'
WHERE NOT EXISTS (SELECT 1 FROM especialidad WHERE nombre = 'Mecánica General');
INSERT INTO especialidad (id, nombre)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM especialidad), 'Mecánica Automotriz'
WHERE NOT EXISTS (SELECT 1 FROM especialidad WHERE nombre = 'Mecánica Automotriz');
INSERT INTO especialidad (id, nombre)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM especialidad), 'Química Industrial'
WHERE NOT EXISTS (SELECT 1 FROM especialidad WHERE nombre = 'Química Industrial');

-- ============================================================
-- CURSOS DEL PERIODO ACTUAL
-- promocion = periodo + 2 (1ro), +1 (2do), periodo (3ro).
-- Construcciones, Electrónica y Química tienen A/B/C; las demás A/B.
-- La restricción UNIQUE de curso evita duplicados.
-- ============================================================
INSERT IGNORE INTO curso (especialidad_id, promocion, seccion)
SELECT e.id, YEAR(CURDATE()) + niveles.desfase, secciones.seccion
FROM especialidad e
CROSS JOIN (
    SELECT 2 AS desfase
    UNION ALL SELECT 1
    UNION ALL SELECT 0
) niveles
CROSS JOIN (
    SELECT 'A' AS seccion
    UNION ALL SELECT 'B'
    UNION ALL SELECT 'C'
) secciones
WHERE e.nombre IN (
    'Construcciones Civiles', 'Electricidad', 'Electrónica',
    'Electromecánica', 'Informática', 'Mecánica General',
    'Mecánica Automotriz', 'Química Industrial'
)
AND (
    secciones.seccion <> 'C'
    OR e.nombre IN ('Construcciones Civiles', 'Electrónica', 'Química Industrial')
);

-- ============================================================
-- MATERIAS E INSTRUMENTOS
-- INSERT IGNORE aprovecha las claves únicas y nunca reemplaza datos.
-- ============================================================
INSERT IGNORE INTO materia (nombre, categoria) VALUES
    ('Programación de prueba', 'especifico'),
    ('Redes de prueba', 'especifico'),
    ('Proyecto interdisciplinario de prueba', 'comun');

INSERT INTO instrumento (id, nombre)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM instrumento), 'Trabajo práctico (prueba)'
WHERE NOT EXISTS (SELECT 1 FROM instrumento WHERE nombre = 'Trabajo práctico (prueba)');
INSERT INTO instrumento (id, nombre)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM instrumento), 'Prueba escrita (prueba)'
WHERE NOT EXISTS (SELECT 1 FROM instrumento WHERE nombre = 'Prueba escrita (prueba)');
INSERT INTO instrumento (id, nombre)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM instrumento), 'Presentación oral (prueba)'
WHERE NOT EXISTS (SELECT 1 FROM instrumento WHERE nombre = 'Presentación oral (prueba)');

SET @sca_instrumento_tp_id := (
    SELECT id FROM instrumento WHERE nombre = 'Trabajo práctico (prueba)' ORDER BY id LIMIT 1
);
SET @sca_instrumento_prueba_id := (
    SELECT id FROM instrumento WHERE nombre = 'Prueba escrita (prueba)' ORDER BY id LIMIT 1
);

-- ============================================================
-- CUENTAS NUEVAS DE DEMOSTRACIÓN
-- Nunca se usa UPDATE ni ON DUPLICATE KEY UPDATE: si el usuario existe,
-- todos sus campos permanecen exactamente como estaban.
-- El hash BCrypt corresponde a la contraseña de prueba "ctn2025".
-- ============================================================
INSERT IGNORE INTO profesor
    (nombre, apellido, usuario, contrasenia, ci, telefono, celular, correo,
     google_email, google_access_token, google_refresh_token, google_token_expiry,
     materias_manual, totp_secret, especialidad_id, nivel)
SELECT 'Profesor', 'Demostración', 'sca.demo.profesor',
       '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y',
       9900001, NULL, NULL, 'profesor.demo@example.invalid',
       NULL, NULL, NULL, NULL, NULL, NULL, e.id, 1
FROM especialidad e
WHERE e.nombre = 'Informática'
  AND NOT EXISTS (SELECT 1 FROM profesor WHERE usuario = 'sca.demo.profesor')
LIMIT 1;

INSERT IGNORE INTO padre
    (ci, nombre, apellido, usuario, contrasenia, telefono, correo, totp_secret)
SELECT 9900002, 'Padre', 'Demostración', 'sca.demo.padre',
       '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y',
       '0971000099', 'padre.demo@example.invalid', NULL
WHERE NOT EXISTS (SELECT 1 FROM padre WHERE usuario = 'sca.demo.padre');

-- Relaciones de las materias de prueba con Informática.
INSERT IGNORE INTO materia_especialidad (materia_id, especialidad_id)
SELECT m.id, e.id
FROM materia m
JOIN especialidad e ON e.nombre = 'Informática'
WHERE m.nombre IN ('Programación de prueba', 'Redes de prueba', 'Proyecto interdisciplinario de prueba');

-- La materia común también queda visible en Electricidad y Construcciones.
INSERT IGNORE INTO materia_especialidad (materia_id, especialidad_id)
SELECT m.id, e.id
FROM materia m
JOIN especialidad e ON e.nombre IN ('Electricidad', 'Construcciones Civiles')
WHERE m.nombre = 'Proyecto interdisciplinario de prueba';

-- ============================================================
-- ALUMNOS SINTÉTICOS
-- Usa CI reservadas para el seed y dominios example.invalid.
-- Solo se relacionan con la cuenta nueva sca.demo.padre.
-- ============================================================
SET @sca_curso_prueba_id := (
    SELECT c.id
    FROM curso c
    JOIN especialidad e ON e.id = c.especialidad_id
    WHERE e.nombre = 'Informática'
      AND c.promocion = YEAR(CURDATE()) + 2
      AND c.seccion = 'A'
    LIMIT 1
);

INSERT IGNORE INTO alumno
    (ci, nombre, apellido, curso_id, correo_encargado, correo_encargado2, google_user_id, google_email)
SELECT 9901001, 'ANA', 'DEMOSTRACIÓN', @sca_curso_prueba_id,
       'encargado.ana@example.invalid', NULL, NULL, NULL
WHERE @sca_curso_prueba_id IS NOT NULL;

INSERT IGNORE INTO alumno
    (ci, nombre, apellido, curso_id, correo_encargado, correo_encargado2, google_user_id, google_email)
SELECT 9901002, 'BRUNO', 'DEMOSTRACIÓN', @sca_curso_prueba_id,
       'encargado.bruno@example.invalid', NULL, NULL, NULL
WHERE @sca_curso_prueba_id IS NOT NULL;

INSERT IGNORE INTO alumno
    (ci, nombre, apellido, curso_id, correo_encargado, correo_encargado2, google_user_id, google_email)
SELECT 9901003, 'CAMILA', 'DEMOSTRACIÓN', @sca_curso_prueba_id,
       'encargado.camila@example.invalid', NULL, NULL, NULL
WHERE @sca_curso_prueba_id IS NOT NULL;

INSERT IGNORE INTO alumno
    (ci, nombre, apellido, curso_id, correo_encargado, correo_encargado2, google_user_id, google_email)
SELECT 9901004, 'DIEGO', 'DEMOSTRACIÓN', @sca_curso_prueba_id,
       'encargado.diego@example.invalid', NULL, NULL, NULL
WHERE @sca_curso_prueba_id IS NOT NULL;

SET @sca_padre_id := (
    SELECT id
    FROM padre
    WHERE usuario = 'sca.demo.padre'
      AND ci = 9900002
      AND correo = 'padre.demo@example.invalid'
    LIMIT 1
);

INSERT IGNORE INTO alumno_padre (alumno_id, padre_id, parentesco)
SELECT a.id, @sca_padre_id, 'padre'
FROM alumno a
WHERE @sca_padre_id IS NOT NULL
  AND a.ci BETWEEN 9901001 AND 9901004
  AND a.apellido = 'DEMOSTRACIÓN';

-- ============================================================
-- DATOS ACADÉMICOS
-- Se usa exclusivamente la cuenta nueva reservada para demostración.
-- ============================================================
SET @sca_profesor_id := (
    SELECT id
    FROM profesor
    WHERE usuario = 'sca.demo.profesor'
      AND ci = 9900001
      AND correo = 'profesor.demo@example.invalid'
    LIMIT 1
);

INSERT IGNORE INTO profesor_materia (profesor_id, materia_id)
SELECT @sca_profesor_id, m.id
FROM materia m
WHERE @sca_profesor_id IS NOT NULL
  AND m.nombre IN ('Programación de prueba', 'Redes de prueba', 'Proyecto interdisciplinario de prueba');

INSERT IGNORE INTO asignacion (profesor_id, materia_id, curso_id)
SELECT @sca_profesor_id, m.id, @sca_curso_prueba_id
FROM materia m
WHERE @sca_profesor_id IS NOT NULL
  AND @sca_curso_prueba_id IS NOT NULL
  AND m.nombre IN ('Programación de prueba', 'Redes de prueba', 'Proyecto interdisciplinario de prueba');

SET @sca_materia_prueba_id := (
    SELECT id FROM materia WHERE nombre = 'Programación de prueba' LIMIT 1
);

INSERT IGNORE INTO planilla
    (curso_id, materia_id, periodo, etapa, profesor_id, google_course_id)
SELECT @sca_curso_prueba_id, @sca_materia_prueba_id, YEAR(CURDATE()), 'primera', @sca_profesor_id, NULL
WHERE @sca_curso_prueba_id IS NOT NULL
  AND @sca_materia_prueba_id IS NOT NULL
  AND @sca_profesor_id IS NOT NULL;

SET @sca_planilla_prueba_id := (
    SELECT id
    FROM planilla
    WHERE curso_id = @sca_curso_prueba_id
      AND materia_id = @sca_materia_prueba_id
      AND periodo = YEAR(CURDATE())
      AND etapa = 'primera'
    LIMIT 1
);

-- Tareas: tarea no posee clave única, por eso se comprueba título + planilla.
INSERT INTO tarea
    (planilla_id, instrumento_id, fecha, total, titulo, fecha_inicio, fecha_limite)
SELECT @sca_planilla_prueba_id, @sca_instrumento_tp_id, CURDATE(), 10,
       '[SCA-PRUEBA] Variables y tipos', DATE_SUB(CURDATE(), INTERVAL 7 DAY), CURDATE()
WHERE @sca_planilla_prueba_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM tarea
      WHERE planilla_id = @sca_planilla_prueba_id
        AND titulo = '[SCA-PRUEBA] Variables y tipos'
  );

INSERT INTO tarea
    (planilla_id, instrumento_id, fecha, total, titulo, fecha_inicio, fecha_limite)
SELECT @sca_planilla_prueba_id, @sca_instrumento_prueba_id, CURDATE(), 20,
       '[SCA-PRUEBA] Evaluación de fundamentos', DATE_SUB(CURDATE(), INTERVAL 3 DAY), CURDATE()
WHERE @sca_planilla_prueba_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM tarea
      WHERE planilla_id = @sca_planilla_prueba_id
        AND titulo = '[SCA-PRUEBA] Evaluación de fundamentos'
  );

-- Un registro por cada alumno sintético y por la planilla de prueba.
INSERT INTO registro (planilla_id, alumno_id)
SELECT @sca_planilla_prueba_id, a.id
FROM alumno a
WHERE @sca_planilla_prueba_id IS NOT NULL
  AND a.ci BETWEEN 9901001 AND 9901004
  AND a.apellido = 'DEMOSTRACIÓN'
  AND NOT EXISTS (
      SELECT 1 FROM registro r
      WHERE r.planilla_id = @sca_planilla_prueba_id
        AND r.alumno_id = a.id
  );

-- Puntajes deterministas para poder comprobar tablas y exportaciones.
INSERT IGNORE INTO puntaje (registro_id, tarea_id, puntos)
SELECT r.id, t.id,
       CASE
           WHEN t.total = 10 THEN 6 + MOD(a.ci, 5)
           ELSE 12 + MOD(a.ci, 9)
       END
FROM registro r
JOIN alumno a ON a.id = r.alumno_id
JOIN tarea t ON t.planilla_id = r.planilla_id
WHERE r.planilla_id = @sca_planilla_prueba_id
  AND a.ci BETWEEN 9901001 AND 9901004
  AND a.apellido = 'DEMOSTRACIÓN'
  AND t.titulo IN (
      '[SCA-PRUEBA] Variables y tipos',
      '[SCA-PRUEBA] Evaluación de fundamentos'
  );

COMMIT;

-- Resumen de verificación. No altera ningún dato.
SELECT
    @sca_profesor_id AS profesor_demo,
    @sca_padre_id AS padre_demo,
    @sca_curso_prueba_id AS curso_prueba,
    @sca_planilla_prueba_id AS planilla_prueba,
    CASE
        WHEN @sca_profesor_id IS NULL
            THEN 'No se pudo crear la cuenta demo: se omitieron asignaciones y evaluaciones'
        ELSE 'Seed seguro aplicado correctamente; las cuentas no fueron modificadas'
    END AS resultado;
