-- Seed de datos de prueba para validación manual end-to-end.
-- Reglas:
--   * Idempotente: limpia los registros de la ejecución anterior con prefijo seed_/qa_.
--   * No usa TRUNCATE ni toca filas fuera del prefijo/IDs reservados de seed.
--   * Compatible con el esquema real de la rama actual: usuario.nivel, especialidad_id, planilla.etapa, planilla_rasgo.estado_verificacion_tema, etc.
--   * Contraseña conocida para todos los usuarios de prueba: ctn2025
--     Hash reutilizado del proyecto: $2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y

SET NAMES utf8mb4;
START TRANSACTION;

-- ============================================================
-- LIMPIEZA DE LA CORRIDA ANTERIOR (solo seed/qa)
-- ============================================================
DELETE FROM puntaje
WHERE tarea_id IN (
    SELECT t.id
    FROM tarea t
    JOIN planilla p ON p.id = t.planilla_id
    WHERE p.id IN (7001, 7002, 7003, 7004)
);

DELETE FROM registro
WHERE planilla_id IN (7001, 7002, 7003, 7004);

DELETE FROM tarea
WHERE planilla_id IN (7001, 7002, 7003, 7004);

DELETE FROM classroom_sync_log
WHERE id IN (2001, 2002, 2003);

DELETE FROM tema_plan_curricular
WHERE id IN (8101, 8102, 8103, 8104);

DELETE FROM plan_curricular
WHERE id IN (8001, 8002, 8003);

DELETE FROM planilla_rasgo
WHERE id IN (6001, 6002, 6003, 6004)
   OR tema LIKE 'seed_%'
   OR tema LIKE 'qa_%';

DELETE FROM planilla
WHERE id IN (7001, 7002, 7003, 7004)
   OR curso_id IN (9001, 9002, 9101, 9102);

DELETE FROM asignacion
WHERE id IN (9201, 9202, 9203, 9301, 9302)
   OR (usuario_id IN (9004, 9005, 9006) AND curso_id IN (9001, 9002, 9101, 9102));

DELETE FROM materia_especialidad
WHERE materia_id IN (9501, 9502, 9503, 9504)
   OR materia_id IN (
       SELECT id FROM materia WHERE nombre LIKE 'seed_%' OR nombre LIKE 'qa_%'
   );

DELETE FROM alumno_usuario
WHERE alumno_id IN (10001, 10002, 10003, 10004, 10005, 10006, 10007, 10008, 10009, 10010, 10011, 10012, 10013, 10014, 10015, 10016)
   OR usuario_id IN (9009, 9010);

DELETE FROM alumno
WHERE id IN (10001, 10002, 10003, 10004, 10005, 10006, 10007, 10008, 10009, 10010, 10011, 10012, 10013, 10014, 10015, 10016)
   OR nombre LIKE 'seed_%'
   OR nombre LIKE 'qa_%';

DELETE FROM curso
WHERE id IN (9001, 9002, 9101, 9102)
   OR (especialidad_id IN (3, 5) AND promocion = YEAR(CURDATE()) AND seccion IN ('A', 'B'));

DELETE FROM materia
WHERE id IN (9501, 9502, 9503, 9504)
   OR nombre LIKE 'seed_%'
   OR nombre LIKE 'qa_%';

DELETE FROM usuario
WHERE id IN (9001, 9002, 9003, 9004, 9005, 9006, 9007, 9008, 9009, 9010)
   OR usuario LIKE 'seed_%'
   OR usuario LIKE 'qa_%'
   OR correo LIKE 'seed_%'
   OR correo LIKE 'qa_%';

-- ============================================================
-- ESPECIALIDADES REALES DEL SISTEMA
-- ============================================================
-- Se usan los IDs reales ya creados por migraciones: Informática=5, Electrónica=3.

-- ============================================================
-- CURSOS PARA VALIDAR ESPECIALIDAD -> CURSO -> SECCIÓN
-- ============================================================
INSERT INTO curso (id, especialidad_id, promocion, seccion) VALUES
    (9001, 5, YEAR(CURDATE()), 'A'),
    (9002, 5, YEAR(CURDATE()), 'B'),
    (9101, 3, YEAR(CURDATE()), 'A'),
    (9102, 3, YEAR(CURDATE()), 'B')
ON DUPLICATE KEY UPDATE
    especialidad_id = VALUES(especialidad_id),
    promocion = VALUES(promocion),
    seccion = VALUES(seccion);

-- ============================================================
-- MATERIAS: 2 exclusivas + 1 común vinculada a ambas especialidades
-- ============================================================
INSERT INTO materia (id, nombre, categoria) VALUES
    (9501, 'seed_materia_programacion_avanzada', 'especifico'),
    (9502, 'seed_materia_redes_infraestructura', 'especifico'),
    (9503, 'seed_materia_electronica_digital', 'especifico'),
    (9504, 'seed_materia_comun_digital', 'comun')
ON DUPLICATE KEY UPDATE
    categoria = VALUES(categoria),
    nombre = VALUES(nombre);

INSERT INTO materia_especialidad (materia_id, especialidad_id) VALUES
    (9501, 5),
    (9502, 5),
    (9503, 3),
    (9504, 3),
    (9504, 5)
ON DUPLICATE KEY UPDATE
    especialidad_id = VALUES(especialidad_id);

-- ============================================================
-- USUARIOS: administrador global, admins de especialidad, profesores,
-- evaluadores, padres y activity_log_path con formato real:
-- usuario-<id>.txt
-- ============================================================
INSERT INTO usuario (
    id, nombre, apellido, usuario, contrasenia, ci, telefono, celular, correo,
    google_email, nivel, activity_log_path, especialidad_id
) VALUES
    (9001, 'Admin', 'Global', 'seed_admin_global', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 100001, '0990000001', '0990000001', 'seed_admin_global@qa.local', NULL, 3, 'usuario-9001.txt', NULL),
    (9002, 'Admin', 'Informática', 'seed_admin_informatica', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 100002, '0990000002', '0990000002', 'seed_admin_informatica@qa.local', NULL, 3, 'usuario-9002.txt', 5),
    (9003, 'Admin', 'Electrónica', 'seed_admin_electronica', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 100003, '0990000003', '0990000003', 'seed_admin_electronica@qa.local', NULL, 3, 'usuario-9003.txt', 3),
    (9004, 'Prof', 'Informática 1', 'seed_prof_informatica_1', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 100004, '0990000004', '0990000004', 'seed_prof_informatica_1@qa.local', NULL, 1, 'usuario-9004.txt', 5),
    (9005, 'Prof', 'Informática 2', 'seed_prof_informatica_2', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 100005, '0990000005', '0990000005', 'seed_prof_informatica_2@qa.local', NULL, 1, 'usuario-9005.txt', 5),
    (9006, 'Prof', 'Electrónica 1', 'seed_prof_electronica_1', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 100006, '0990000006', '0990000006', 'seed_prof_electronica_1@qa.local', NULL, 1, 'usuario-9006.txt', 3),
    (9007, 'Eval', 'Informática', 'seed_evaluador_1', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 100007, '0990000007', '0990000007', 'seed_evaluador_1@qa.local', NULL, 2, 'usuario-9007.txt', 5),
    (9008, 'Eval', 'Electrónica', 'seed_evaluador_2', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 100008, '0990000008', '0990000008', 'seed_evaluador_2@qa.local', NULL, 2, 'usuario-9008.txt', 3),
    (9009, 'Padre', 'Uno', 'seed_padre_1', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 100009, '0990000009', '0990000009', 'seed_padre_1@qa.local', NULL, 4, 'usuario-9009.txt', NULL),
    (9010, 'Padre', 'Dos', 'seed_padre_2', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 100010, '0990000010', '0990000010', 'seed_padre_2@qa.local', NULL, 4, 'usuario-9010.txt', NULL)
ON DUPLICATE KEY UPDATE
    nombre = VALUES(nombre),
    apellido = VALUES(apellido),
    usuario = VALUES(usuario),
    contrasenia = VALUES(contrasenia),
    ci = VALUES(ci),
    telefono = VALUES(telefono),
    celular = VALUES(celular),
    correo = VALUES(correo),
    google_email = VALUES(google_email),
    nivel = VALUES(nivel),
    activity_log_path = VALUES(activity_log_path),
    especialidad_id = VALUES(especialidad_id);

-- ============================================================
-- ALUMNOS + RELACIÓN CON PADRES (alumno_usuario)
-- ============================================================
INSERT INTO alumno (id, ci, nombre, apellido, curso_id, correo_encargado, correo_encargado2, google_user_id, google_email) VALUES
    (10001, 200001, 'seed_alumno_info_a_01', 'QA', 9001, 'seed_padre_1@qa.local', NULL, NULL, NULL),
    (10002, 200002, 'seed_alumno_info_a_02', 'QA', 9001, 'seed_padre_1@qa.local', NULL, NULL, NULL),
    (10003, 200003, 'seed_alumno_info_a_03', 'QA', 9001, 'seed_padre_1@qa.local', NULL, NULL, NULL),
    (10004, 200004, 'seed_alumno_info_a_04', 'QA', 9001, 'seed_padre_1@qa.local', NULL, NULL, NULL),
    (10005, 200005, 'seed_alumno_info_b_01', 'QA', 9002, 'seed_padre_2@qa.local', NULL, NULL, NULL),
    (10006, 200006, 'seed_alumno_info_b_02', 'QA', 9002, 'seed_padre_2@qa.local', NULL, NULL, NULL),
    (10007, 200007, 'seed_alumno_info_b_03', 'QA', 9002, 'seed_padre_2@qa.local', NULL, NULL, NULL),
    (10008, 200008, 'seed_alumno_info_b_04', 'QA', 9002, 'seed_padre_2@qa.local', NULL, NULL, NULL),
    (10009, 200009, 'seed_alumno_elec_a_01', 'QA', 9101, 'seed_padre_1@qa.local', NULL, NULL, NULL),
    (10010, 200010, 'seed_alumno_elec_a_02', 'QA', 9101, 'seed_padre_1@qa.local', NULL, NULL, NULL),
    (10011, 200011, 'seed_alumno_elec_a_03', 'QA', 9101, 'seed_padre_1@qa.local', NULL, NULL, NULL),
    (10012, 200012, 'seed_alumno_elec_a_04', 'QA', 9101, 'seed_padre_1@qa.local', NULL, NULL, NULL),
    (10013, 200013, 'seed_alumno_elec_b_01', 'QA', 9102, 'seed_padre_2@qa.local', NULL, NULL, NULL),
    (10014, 200014, 'seed_alumno_elec_b_02', 'QA', 9102, 'seed_padre_2@qa.local', NULL, NULL, NULL),
    (10015, 200015, 'seed_alumno_elec_b_03', 'QA', 9102, 'seed_padre_2@qa.local', NULL, NULL, NULL),
    (10016, 200016, 'seed_alumno_elec_b_04', 'QA', 9102, 'seed_padre_2@qa.local', NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE
    ci = VALUES(ci),
    nombre = VALUES(nombre),
    apellido = VALUES(apellido),
    curso_id = VALUES(curso_id),
    correo_encargado = VALUES(correo_encargado),
    correo_encargado2 = VALUES(correo_encargado2),
    google_user_id = VALUES(google_user_id),
    google_email = VALUES(google_email);

INSERT INTO alumno_usuario (alumno_id, usuario_id, parentesco) VALUES
    (10001, 9009, 'padre'),
    (10002, 9009, 'padre'),
    (10003, 9009, 'padre'),
    (10004, 9009, 'padre'),
    (10005, 9010, 'padre'),
    (10006, 9010, 'padre'),
    (10007, 9010, 'padre'),
    (10008, 9010, 'padre'),
    (10009, 9009, 'padre'),
    (10010, 9009, 'padre'),
    (10011, 9010, 'padre'),
    (10012, 9010, 'padre'),
    (10013, 9010, 'padre'),
    (10014, 9010, 'padre'),
    (10015, 9009, 'padre'),
    (10016, 9009, 'padre')
ON DUPLICATE KEY UPDATE
    parentesco = VALUES(parentesco);

-- ============================================================
-- ASIGNACIONES: una por profesor + materia + curso; profesor 9004
-- tiene múltiples asignaciones para probar el filtro por especialidad.
-- ============================================================
INSERT INTO asignacion (id, usuario_id, materia_id, curso_id) VALUES
    (9201, 9004, 9501, 9001),
    (9202, 9004, 9502, 9002),
    (9203, 9005, 9504, 9001),
    (9301, 9006, 9503, 9101),
    (9302, 9006, 9504, 9102)
ON DUPLICATE KEY UPDATE
    usuario_id = VALUES(usuario_id),
    materia_id = VALUES(materia_id),
    curso_id = VALUES(curso_id);

-- ============================================================
-- PLANILLAS
-- ============================================================
INSERT INTO planilla (id, curso_id, materia_id, periodo, etapa, usuario_id, google_course_id) VALUES
    (7001, 9001, 9501, YEAR(CURDATE()), 'segunda', 9004, NULL),
    (7002, 9002, 9502, YEAR(CURDATE()), 'primera', 9004, NULL),
    (7003, 9101, 9503, YEAR(CURDATE()), 'segunda', 9006, NULL),
    (7004, 9102, 9504, YEAR(CURDATE()), 'primera', 9006, NULL)
ON DUPLICATE KEY UPDATE
    curso_id = VALUES(curso_id),
    materia_id = VALUES(materia_id),
    periodo = VALUES(periodo),
    etapa = VALUES(etapa),
    usuario_id = VALUES(usuario_id),
    google_course_id = VALUES(google_course_id);

-- ============================================================
-- PLAN CURRICULAR: cubre PENDIENTE, APROBADO y RECHAZADO,
-- con etapas primera/segunda.
-- ============================================================
INSERT INTO plan_curricular (
    id, asignacion_id, etapa, anio_lectivo, archivo_nombre, archivo_contenido,
    estado, fecha_subida, fecha_revision, evaluador_id, observaciones_evaluador
) VALUES
    (8001, 9201, 'primera', YEAR(CURDATE()), 'seed_plan_pendiente_primera.pdf', 0x504c414e2050454e4449454e5445, 'PENDIENTE', NOW(), NULL, 9007, NULL),
    (8002, 9202, 'segunda', YEAR(CURDATE()), 'seed_plan_aprobado_segunda.pdf', 0x504c414e204150524f4241444f, 'APROBADO', NOW(), NOW(), 9007, NULL),
    (8003, 9301, 'primera', YEAR(CURDATE()), 'seed_plan_rechazado_primera.pdf', 0x504c414e2052454348415a41444f, 'RECHAZADO', NOW(), NOW(), 9008, 'Faltan indicadores de evaluación y actividades para la segunda parte.')
ON DUPLICATE KEY UPDATE
    asignacion_id = VALUES(asignacion_id),
    etapa = VALUES(etapa),
    anio_lectivo = VALUES(anio_lectivo),
    archivo_nombre = VALUES(archivo_nombre),
    archivo_contenido = VALUES(archivo_contenido),
    estado = VALUES(estado),
    evaluador_id = VALUES(evaluador_id),
    observaciones_evaluador = VALUES(observaciones_evaluador);

INSERT INTO tema_plan_curricular (
    id, plan_curricular_id, mes, orden_mes, bloque, capacidades, temas_contenidos,
    actividades, instrumentos_evaluacion, indicador_conceptual,
    indicador_procedimental, indicador_actitudinal, estado_cobertura, fecha_cobertura, planilla_rasgo_id
) VALUES
    (8101, 8001, 'Marzo', 1, 1, 'Capacidad 1', 'seed_tema_plan_pendiente', 'Actividad de diagnóstico', 'Prueba escrita', 'Reconoce conceptos básicos', 'Aplica procedimientos', 'Participa con respeto', 'PENDIENTE', NULL, NULL),
    (8102, 8002, 'Marzo', 1, 1, 'Capacidad 1', 'seed_tema_plan_aprobado_cubierto', 'Actividad práctica guiada', 'Proyecto', 'Reconoce conceptos básicos', 'Aplica procedimientos', 'Participa con respeto', 'CUBIERTO', NOW(), NULL),
    (8103, 8002, 'Abril', 2, 1, 'Capacidad 2', 'seed_tema_plan_aprobado_pendiente', 'Actividad de consolidación', 'Observación', 'Analiza información', 'Diseña procedimientos', 'Colabora en equipo', 'PENDIENTE', NULL, NULL),
    (8104, 8003, 'Marzo', 1, 1, 'Capacidad 1', 'seed_tema_plan_rechazado', 'Actividad de revisión', 'Rúbrica', 'Reconoce conceptos básicos', 'Evalúa procedimientos', 'Responsabilidad', 'CUBIERTO', NOW(), NULL)
ON DUPLICATE KEY UPDATE
    mes = VALUES(mes),
    orden_mes = VALUES(orden_mes),
    bloque = VALUES(bloque),
    capacidades = VALUES(capacidades),
    temas_contenidos = VALUES(temas_contenidos),
    actividades = VALUES(actividades),
    instrumentos_evaluacion = VALUES(instrumentos_evaluacion),
    indicador_conceptual = VALUES(indicador_conceptual),
    indicador_procedimental = VALUES(indicador_procedimental),
    indicador_actitudinal = VALUES(indicador_actitudinal),
    estado_cobertura = VALUES(estado_cobertura),
    fecha_cobertura = VALUES(fecha_cobertura),
    planilla_rasgo_id = VALUES(planilla_rasgo_id);

-- ============================================================
-- PLANILLAS RASGO: cubre los 4 estados de verificación de tema,
-- incluyendo SIN_PLAN con asignacion_id y tema_plan_curricular_id nulos.
-- ============================================================
INSERT INTO planilla_rasgo (
    id, curso_id, usuario_id, asignacion_id, tema, fecha_clase, created_at,
    estado_verificacion_tema, tema_plan_curricular_id
) VALUES
    (6001, 9001, 9004, 9201, 'seed_qa_tema_ok', '2026-08-10', NOW(), 'OK', 8102),
    (6002, 9002, 9005, 9203, 'seed_qa_tema_dudoso', '2026-08-12', NOW(), 'DUDOSO', 8103),
    (6003, 9101, 9006, 9301, 'seed_qa_tema_no_coincide', '2026-08-14', NOW(), 'NO_COINCIDE', 8104),
    (6004, 9102, 9006, NULL, 'seed_qa_tema_sin_plan', '2026-08-15', NOW(), 'SIN_PLAN', NULL)
ON DUPLICATE KEY UPDATE
    curso_id = VALUES(curso_id),
    usuario_id = VALUES(usuario_id),
    asignacion_id = VALUES(asignacion_id),
    tema = VALUES(tema),
    fecha_clase = VALUES(fecha_clase),
    estado_verificacion_tema = VALUES(estado_verificacion_tema),
    tema_plan_curricular_id = VALUES(tema_plan_curricular_id);

UPDATE tema_plan_curricular
SET planilla_rasgo_id = 6001
WHERE id = 8102;

UPDATE tema_plan_curricular
SET planilla_rasgo_id = 6002
WHERE id = 8103;

-- ============================================================
-- classroom_sync_log
-- ============================================================
INSERT INTO classroom_sync_log (id, planilla_id, usuario_id, tareas_creadas, calificaciones_actualizadas, synced_at) VALUES
    (2001, 7001, 9004, 3, 7, NOW()),
    (2002, 7002, 9005, 2, 5, NOW()),
    (2003, 7003, 9006, 4, 9, NOW())
ON DUPLICATE KEY UPDATE
    planilla_id = VALUES(planilla_id),
    usuario_id = VALUES(usuario_id),
    tareas_creadas = VALUES(tareas_creadas),
    calificaciones_actualizadas = VALUES(calificaciones_actualizadas),
    synced_at = VALUES(synced_at);

COMMIT;

-- ============================================================
-- RECORDATORIO DE CREDENCIALES
-- ============================================================
-- Todos los usuarios semilla tienen la misma contraseña de prueba:
--   usuario: seed_*/qa_* / contraseña: ctn2025
--
-- Tiene una validación manual de roles esperados:
--   - 9001: admin global (nivel=3, especialidad_id=NULL)
--   - 9002: admin de especialidad Informática (nivel=3, especialidad_id=5)
--   - 9003: admin de especialidad Electrónica (nivel=3, especialidad_id=3)
--   - 9004 / 9005: profesores de Informática (nivel=1)
--   - 9006: profesor de Electrónica (nivel=1)
--   - 9007 / 9008: evaluadores (nivel=2)
--   - 9009 / 9010: padres (nivel=4)
