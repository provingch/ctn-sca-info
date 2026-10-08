-- Borra los datos de ejemplo de demo-insert.sql buscándolos por su contenido (no por id) y restaura la planilla 3.
-- Uso: ssh -p 2251 sca@186.17.107.54 'mariadb --default-character-set=utf8mb4 ctndb' < manuales/datos-demo/demo-cleanup.sql
-- No toca nada que no tenga exactamente los textos/fechas de ejemplo (por ejemplo, tareas o planillas reales de Graciela).
SET NAMES utf8mb4;
START TRANSACTION;
SET @clases = (SELECT GROUP_CONCAT(id) FROM planilla_rasgo WHERE usuario_id = 81 AND asignacion_id = 70 AND
  ((tema = 'Introducción a los algoritmos: diagramas de flujo' AND fecha_clase = '2026-06-30') OR
   (tema = 'Estructuras condicionales: if / else' AND fecha_clase = '2026-09-29') OR
   (tema = 'Repaso de estructuras condicionales' AND fecha_clase = '2026-10-06')));
SET @planes = (SELECT GROUP_CONCAT(id) FROM plan_curricular WHERE asignacion_id IN (70, 71) AND archivo_nombre IN ('plan-algoritmica-3A-etapa2.xlsx', 'plan-algoritmica-3B-etapa2.xlsx'));
SET @tareas = (SELECT GROUP_CONCAT(id) FROM tarea WHERE planilla_id = 3 AND titulo IN ('Trabajo práctico: diagramas de flujo', 'Prueba sumativa: variables y tipos de datos'));
SELECT @clases clases, @planes planes, @tareas tareas;

DELETE FROM puntaje WHERE FIND_IN_SET(tarea_id, @tareas);
DELETE FROM tarea WHERE FIND_IN_SET(id, @tareas);
DELETE FROM incumplimiento_revision WHERE usuario_id = 81 AND (FIND_IN_SET(planilla_rasgo_id, @clases)
  OR (asignacion_id = 71 AND tipo = 'BLOQUEO_ATRASO_TIEMPO_REAL' AND created_at = '2026-10-05 11:00:00'));
DELETE rac FROM rasgo_asistencia_codigo rac JOIN rasgo_asistencia ra ON ra.id = rac.rasgo_asistencia_id WHERE FIND_IN_SET(ra.planilla_rasgo_id, @clases);
DELETE FROM rasgo_asistencia WHERE FIND_IN_SET(planilla_rasgo_id, @clases);
UPDATE tema_plan_curricular SET planilla_rasgo_id = NULL WHERE FIND_IN_SET(plan_curricular_id, @planes);
DELETE FROM planilla_rasgo WHERE FIND_IN_SET(id, @clases);
DELETE FROM tema_plan_curricular WHERE FIND_IN_SET(plan_curricular_id, @planes);
DELETE FROM plan_curricular WHERE FIND_IN_SET(id, @planes);
DELETE FROM queja WHERE especialidad_id = 5 AND motivo IN (
  'Los alumnos informan que las notas de la prueba del 25 de septiembre todavía no fueron devueltas ni cargadas en el sistema.',
  'Inasistencias reiteradas sin aviso previo en el turno mañana durante la última semana.',
  'Los padres reclaman que no se comunicó con anticipación la fecha de la prueba sumativa.',
  'Error en la carga de notas del laboratorio: varios alumnos figuran con cero habiendo entregado.',
  'Un alumno indica que la profesora no le permitió salir del aula.',
  'El grupo llega tarde de forma sistemática después del recreo, lo que reduce el tiempo de clase de la segunda hora.');
UPDATE planilla SET etapa1_confirmada = 0, fecha_cierre_etapa1 = NULL WHERE id = 3;
-- comprobación: nada de los datos de ejemplo debería quedar (todo en 0) y la planilla 3 en 0/NULL
SELECT (SELECT COUNT(*) FROM planilla_rasgo WHERE FIND_IN_SET(id, @clases)) clases_demo,
       (SELECT COUNT(*) FROM plan_curricular WHERE FIND_IN_SET(id, @planes)) planes_demo,
       (SELECT COUNT(*) FROM tarea WHERE FIND_IN_SET(id, @tareas)) tareas_demo,
       (SELECT COUNT(*) FROM incumplimiento_revision WHERE usuario_id = 81) incumplimientos_graciela,
       (SELECT CONCAT(etapa1_confirmada, '/', IFNULL(fecha_cierre_etapa1, 'NULL')) FROM planilla WHERE id = 3) planilla3;
COMMIT;
