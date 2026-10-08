-- Datos de ejemplo para capturas y videos de los manuales. Se borran con demo-cleanup.sql (los busca por contenido, no por id).
-- Uso: ssh -p 2251 sca@186.17.107.54 'mariadb --default-character-set=utf8mb4 ctndb' < manuales/datos-demo/demo-insert.sql
-- Graciela (81): asig 70 = Algorítmica 3°A (curso 31 / curso_base 5), asig 71 = 3°B (curso_base 6). Planilla 3 = Algorítmica 3°A primera.
-- Evaluador = 136, admin_informatica = 6, cpdg1 = 140, Federico = 66. Thiago (hijo de la cuenta de padres) = alumno 291.
-- No dispara notificaciones: es SQL directo, no pasa por la app.
SET NAMES utf8mb4;
START TRANSACTION;

-- ── Plan aprobado 3°A etapa 2, con temas ─────────────────────────────────────
INSERT INTO plan_curricular (asignacion_id, etapa, anio_lectivo, archivo_nombre, archivo_contenido, estado, fecha_subida, fecha_revision, evaluador_id, observaciones_evaluador)
SELECT 70, '2', 2026, 'plan-algoritmica-3A-etapa2.xlsx', archivo_contenido, 'APROBADO', '2026-07-08 10:15:00', '2026-07-10 09:00:00', 136, NULL FROM plan_curricular WHERE id = 1;
SET @plan_ok = LAST_INSERT_ID();

INSERT INTO tema_plan_curricular (plan_curricular_id, mes, orden_mes, bloque, capacidades, temas_contenidos, actividades, instrumentos_evaluacion, indicador_conceptual, indicador_procedimental, indicador_actitudinal, estado_cobertura, fecha_cobertura) VALUES
(@plan_ok, 'Julio', 1, 1, 'Recupera los conceptos básicos de la primera etapa.', 'Repaso de conceptos de la primera etapa', 'Resolución guiada de ejercicios de repaso.', 'Trabajos en clase', 'Identifica los conceptos previos.', 'Resuelve ejercicios simples.', 'Participa activamente.', 'CUBIERTO', '2026-07-20 07:00:00'),
(@plan_ok, 'Agosto', 2, 1, 'Representa algoritmos de forma gráfica.', 'Diagramas de flujo y pseudocódigo', 'Construcción de diagramas en grupo.', 'Fichas de trabajo', 'Reconoce los símbolos del diagrama.', 'Diseña diagramas de flujo.', 'Trabaja en equipo.', 'CUBIERTO', '2026-08-18 07:00:00'),
(@plan_ok, 'Agosto', 2, 2, 'Utiliza variables y tipos de datos.', 'Variables, constantes y tipos de datos', 'Ejercicios en el laboratorio.', 'Trabajos en clase', 'Distingue tipos de datos.', 'Declara variables correctamente.', 'Respeta las consignas.', 'CUBIERTO', '2026-08-25 07:00:00'),
(@plan_ok, 'Septiembre', 3, 1, 'Aplica estructuras de decisión.', 'Estructuras condicionales: if / else', 'Resolución de problemas con decisiones.', 'Prueba Sumativa', 'Comprende la lógica condicional.', 'Codifica estructuras if / else.', 'Persevera ante errores.', 'CUBIERTO', '2026-09-29 07:00:00'),
(@plan_ok, 'Septiembre', 3, 2, 'Aplica decisiones múltiples.', 'Estructuras condicionales anidadas y switch', 'Ejercicios de menús de opciones.', 'Trabajos en clase', 'Reconoce decisiones múltiples.', 'Codifica switch.', 'Colabora con sus compañeros.', 'PENDIENTE', NULL),
(@plan_ok, 'Octubre', 4, 1, 'Aplica estructuras repetitivas.', 'Estructuras repetitivas: while y do-while', 'Ejercicios de acumuladores y contadores.', 'Trabajos en clase', 'Comprende la repetición condicionada.', 'Codifica ciclos while.', 'Organiza su trabajo.', 'PENDIENTE', NULL),
(@plan_ok, 'Octubre', 4, 2, 'Aplica ciclos con contador.', 'Estructuras repetitivas: for', 'Ejercicios con tablas y series.', 'Prueba Sumativa', 'Comprende el ciclo for.', 'Codifica ciclos for.', 'Cumple los plazos.', 'PENDIENTE', NULL),
(@plan_ok, 'Noviembre', 5, 1, 'Maneja colecciones de datos.', 'Arreglos unidimensionales', 'Carga y recorrido de vectores.', 'Trabajos en clase', 'Comprende la estructura de un arreglo.', 'Recorre arreglos.', 'Ayuda a sus compañeros.', 'PENDIENTE', NULL),
(@plan_ok, 'Noviembre', 5, 2, 'Integra lo aprendido en un proyecto.', 'Proyecto integrador', 'Desarrollo de un programa completo en grupo.', 'Trabajo de investigación grupal', 'Integra los contenidos de la etapa.', 'Desarrolla un programa completo.', 'Asume responsabilidades en el grupo.', 'PENDIENTE', NULL);
SET @tema_if = (SELECT id FROM tema_plan_curricular WHERE plan_curricular_id = @plan_ok AND orden_mes = 3 AND bloque = 1);
SET @tema_switch = (SELECT id FROM tema_plan_curricular WHERE plan_curricular_id = @plan_ok AND orden_mes = 3 AND bloque = 2);

-- ── Plan pendiente 3°B etapa 2 ───────────────────────────────────────────────
INSERT INTO plan_curricular (asignacion_id, etapa, anio_lectivo, archivo_nombre, archivo_contenido, estado, fecha_subida)
SELECT 71, '2', 2026, 'plan-algoritmica-3B-etapa2.xlsx', archivo_contenido, 'PENDIENTE', '2026-10-06 18:40:00' FROM plan_curricular WHERE id = 1;
SET @plan_pend = LAST_INSERT_ID();
INSERT INTO tema_plan_curricular (plan_curricular_id, mes, orden_mes, bloque, capacidades, temas_contenidos, actividades, instrumentos_evaluacion, estado_cobertura) VALUES
(@plan_pend, 'Julio', 1, 1, 'Recupera los conceptos básicos.', 'Repaso de la primera etapa', 'Ejercicios de repaso.', 'Trabajos en clase', 'PENDIENTE'),
(@plan_pend, 'Agosto', 2, 1, 'Representa algoritmos.', 'Diagramas de flujo y pseudocódigo', 'Diagramas en grupo.', 'Fichas de trabajo', 'PENDIENTE'),
(@plan_pend, 'Septiembre', 3, 1, 'Aplica estructuras de decisión.', 'Estructuras condicionales', 'Problemas con decisiones.', 'Prueba Sumativa', 'PENDIENTE'),
(@plan_pend, 'Octubre', 4, 1, 'Aplica estructuras repetitivas.', 'Estructuras repetitivas', 'Acumuladores y contadores.', 'Trabajos en clase', 'PENDIENTE'),
(@plan_pend, 'Noviembre', 5, 1, 'Integra lo aprendido.', 'Proyecto integrador', 'Programa completo en grupo.', 'Trabajo de investigación grupal', 'PENDIENTE');

-- ── Clases de Graciela en 3°A con asistencia ────────────────────────────────
INSERT INTO planilla_rasgo (curso_id, usuario_id, tema, hora_inicio, horas_catedra, hora_fin, modalidad, observaciones, instrumento_id, fecha_clase, asignacion_id, estado_verificacion_tema, tema_plan_curricular_id, justificacion_atraso)
VALUES (31, 81, 'Introducción a los algoritmos: diagramas de flujo', '07:00', 2, '08:20', 'Presencial', NULL, 10, '2026-06-30', 70, 'SIN_PLAN', NULL, NULL);
SET @c1 = LAST_INSERT_ID();
INSERT INTO planilla_rasgo (curso_id, usuario_id, tema, hora_inicio, horas_catedra, hora_fin, modalidad, observaciones, instrumento_id, fecha_clase, asignacion_id, estado_verificacion_tema, tema_plan_curricular_id, justificacion_atraso)
VALUES (31, 81, 'Estructuras condicionales: if / else', '07:00', 2, '08:20', 'Presencial', 'Se trabajó en el laboratorio 2.', 5, '2026-09-29', 70, 'OK', @tema_if, NULL);
SET @c2 = LAST_INSERT_ID();
INSERT INTO planilla_rasgo (curso_id, usuario_id, tema, hora_inicio, horas_catedra, hora_fin, modalidad, observaciones, instrumento_id, fecha_clase, asignacion_id, estado_verificacion_tema, tema_plan_curricular_id, justificacion_atraso)
VALUES (31, 81, 'Repaso de estructuras condicionales', '07:00', 2, '08:20', 'Presencial', NULL, 10, '2026-10-06', 70, 'ATRASADO', @tema_switch,
        'Se suspendieron las clases del 1 y 2 de octubre por el acto del aniversario; se repasó el tema anterior antes de avanzar.');
SET @c3 = LAST_INSERT_ID();
UPDATE tema_plan_curricular SET planilla_rasgo_id = @c2 WHERE id = @tema_if;

INSERT INTO rasgo_asistencia (planilla_rasgo_id, alumno_id, alumno_nombre, alumno_apellido, alumno_email, estado)
SELECT c.id, a.id, a.nombre, a.apellido, '', 'presente' FROM alumno a JOIN (SELECT @c1 id UNION SELECT @c2 UNION SELECT @c3) c WHERE a.curso_id = 31;
UPDATE rasgo_asistencia SET estado = 'ausente' WHERE planilla_rasgo_id = @c2 AND alumno_id IN (284, 296);
UPDATE rasgo_asistencia SET estado = 'ausente_justificado' WHERE planilla_rasgo_id = @c1 AND alumno_id = 300;
-- la app guarda los códigos de conducta solo en rasgo_asistencia_codigo (falta_codigo queda NULL)
INSERT INTO rasgo_asistencia_codigo (rasgo_asistencia_id, codigo)
SELECT id, 'N1' FROM rasgo_asistencia WHERE planilla_rasgo_id = @c3 AND alumno_id = 291
UNION ALL SELECT id, 'N2' FROM rasgo_asistencia WHERE planilla_rasgo_id = @c2 AND alumno_id = 288;

-- ── Incumplimientos ─────────────────────────────────────────────────────────
INSERT INTO incumplimiento_revision (asignacion_id, usuario_id, tema_plan_curricular_id, planilla_rasgo_id, tipo, estado, descripcion, justificacion_profesor, created_at) VALUES
(70, 81, @tema_switch, @c3, 'ATRASO', 'PENDIENTE',
 'Atraso justificado por el profesor: Se suspendieron las clases del 1 y 2 de octubre por el acto del aniversario; se repasó el tema anterior antes de avanzar.',
 'Se suspendieron las clases del 1 y 2 de octubre por el acto del aniversario; se repasó el tema anterior antes de avanzar.', '2026-10-06 08:25:00');
INSERT INTO incumplimiento_revision (asignacion_id, usuario_id, tema_plan_curricular_id, planilla_rasgo_id, tipo, estado, descripcion, justificacion_profesor, created_at) VALUES
(70, 81, @tema_if, @c1, 'INCONGRUENCIA_RETROACTIVA', 'PENDIENTE',
 'Clase del 30/06/2026 dada antes de subir el plan: tema ingresado "Introducción a los algoritmos: diagramas de flujo", tema esperado según el plan "Repaso de conceptos de la primera etapa".',
 'El grupo tenía dificultades con los diagramas y se priorizó reforzarlos antes del repaso.', '2026-07-10 09:00:00');
INSERT INTO incumplimiento_revision (asignacion_id, usuario_id, tipo, estado, descripcion, created_at) VALUES
(71, 81, 'BLOQUEO_ATRASO_TIEMPO_REAL', 'PENDIENTE', 'Se alcanzaron 3 faltas por atrasos justificados — Iniciar clase bloqueado hasta revisión de evaluación.', '2026-10-05 11:00:00');

-- ── Quejas ───────────────────────────────────────────────────────────────────
INSERT INTO queja (profesor_id, curso_id, especialidad_id, tipo, motivo, creada_por, creada_en) VALUES
(81, 5, 5, 'CONTRA_PROFESOR', 'Los alumnos informan que las notas de la prueba del 25 de septiembre todavía no fueron devueltas ni cargadas en el sistema.', 6, '2026-10-06 10:20:00');
INSERT INTO queja (profesor_id, curso_id, especialidad_id, tipo, motivo, creada_por, creada_en, aceptada_en, aceptada_por) VALUES
(66, 5, 5, 'CONTRA_PROFESOR', 'Inasistencias reiteradas sin aviso previo en el turno mañana durante la última semana.', 6, '2026-10-02 08:10:00', '2026-10-03 09:00:00', 140);
INSERT INTO queja (profesor_id, curso_id, especialidad_id, tipo, motivo, creada_por, creada_en, aceptada_en, aceptada_por, revisada_en, revisada_por, conclusion) VALUES
(81, 6, 5, 'CONTRA_PROFESOR', 'Los padres reclaman que no se comunicó con anticipación la fecha de la prueba sumativa.', 6, '2026-09-25 11:00:00', '2026-09-26 08:30:00', 140, '2026-09-29 10:00:00', 140,
 'Se verificó que la fecha se anunció en clase pero no por escrito. Se acordó con la profesora anunciar las pruebas también en el sistema.');
INSERT INTO queja (profesor_id, curso_id, especialidad_id, tipo, motivo, creada_por, creada_en, aceptada_en, aceptada_por, revisada_en, revisada_por, conclusion, proceso_revision, solucion_aplicada, corregida_por_nombre, resuelta_en, resolucion_registrada_por) VALUES
(66, 6, 5, 'CONTRA_PROFESOR', 'Error en la carga de notas del laboratorio: varios alumnos figuran con cero habiendo entregado.', 6, '2026-09-15 09:00:00', '2026-09-15 12:00:00', 140, '2026-09-17 08:00:00', 140,
 'Se confirmó un error de carga en tres alumnos.', 'Se revisaron las entregas originales junto con el profesor.', 'El profesor corrigió las notas en la planilla.', 'Federico González', '2026-09-18 10:30:00', 140);
INSERT INTO queja (profesor_id, curso_id, especialidad_id, tipo, motivo, creada_por, creada_en, rechazada_en, rechazada_por, motivo_rechazo) VALUES
(81, 5, 5, 'CONTRA_PROFESOR', 'Un alumno indica que la profesora no le permitió salir del aula.', 6, '2026-09-20 10:00:00', '2026-09-21 08:00:00', 140,
 'La situación corresponde al reglamento de convivencia y no constituye una falta de la profesora.');
INSERT INTO queja (profesor_id, curso_id, especialidad_id, tipo, motivo, creada_por, creada_en) VALUES
(81, 6, 5, 'CONTRA_CURSO', 'El grupo llega tarde de forma sistemática después del recreo, lo que reduce el tiempo de clase de la segunda hora.', 81, '2026-10-05 09:40:00');

-- ── Tareas con notas en la planilla 3 (vista de padres) ─────────────────────
INSERT INTO tarea (planilla_id, instrumento_id, fecha, total, titulo, fecha_inicio, fecha_limite) VALUES
(3, 10, '2026-06-10', 10, 'Trabajo práctico: diagramas de flujo', '2026-06-03', '2026-06-10');
SET @t1 = LAST_INSERT_ID();
INSERT INTO tarea (planilla_id, instrumento_id, fecha, total, titulo, fecha_inicio, fecha_limite) VALUES
(3, 5, '2026-07-01', 20, 'Prueba sumativa: variables y tipos de datos', '2026-07-01', '2026-07-01');
SET @t2 = LAST_INSERT_ID();
INSERT INTO puntaje (registro_id, tarea_id, puntos) SELECT r.id, @t1, 5 + (r.alumno_id * 7) % 6 FROM registro r WHERE r.planilla_id = 3;
INSERT INTO puntaje (registro_id, tarea_id, puntos) SELECT r.id, @t2, 10 + (r.alumno_id * 11) % 11 FROM registro r WHERE r.planilla_id = 3;

-- ── Etapa 1 de la planilla 3 cerrada (para mostrar "Reabrir"); la limpieza la vuelve a 0 / NULL ──
UPDATE planilla SET etapa1_confirmada = 1, fecha_cierre_etapa1 = '2026-07-10' WHERE id = 3;

SELECT @plan_ok plan_ok, @plan_pend plan_pend, @c1 c1, @c2 c2, @c3 c3, @t1 t1, @t2 t2;
COMMIT;
