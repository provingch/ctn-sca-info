# Seed de datos de prueba

Este directorio contiene un seed reproducible para validar manualmente el sistema completo sin tocar datos reales.

## Cómo cargarlo

```bash
mysql --default-character-set=utf8mb4 -u <usuario> -p <base_de_datos> < database/seed/seed-test-data.sql
```

Ejemplo local:

```bash
mysql --default-character-set=utf8mb4 -u root -p ctndb < database/seed/seed-test-data.sql
```

## Cómo revertirlo

El script se limpia a sí mismo al principio y borra registros con prefijo `seed_` y `qa_` antes de insertar. Por eso, para revertir el seed basta volver a ejecutar el mismo archivo; la primera parte elimina los registros de la corrida anterior.

Si querés limpiar manualmente sin reinsertar:

```sql
DELETE FROM puntaje WHERE tarea_id IN (SELECT t.id FROM tarea t JOIN planilla p ON p.id = t.planilla_id WHERE p.id IN (7001,7002,7003,7004));
DELETE FROM registro WHERE planilla_id IN (7001,7002,7003,7004);
DELETE FROM tarea WHERE planilla_id IN (7001,7002,7003,7004);
DELETE FROM classroom_sync_log WHERE id IN (2001,2002,2003);
DELETE FROM tema_plan_curricular WHERE id IN (8101,8102,8103,8104);
DELETE FROM plan_curricular WHERE id IN (8001,8002,8003);
DELETE FROM planilla_rasgo WHERE id IN (6001,6002,6003,6004);
DELETE FROM planilla WHERE id IN (7001,7002,7003,7004);
DELETE FROM asignacion WHERE id IN (9201,9202,9203,9301,9302);
DELETE FROM alumno_usuario WHERE alumno_id BETWEEN 10001 AND 10016;
DELETE FROM alumno WHERE id BETWEEN 10001 AND 10016;
DELETE FROM curso WHERE id IN (9001,9002,9101,9102);
DELETE FROM materia_especialidad WHERE materia_id IN (9501,9502,9503,9504);
DELETE FROM materia WHERE id IN (9501,9502,9503,9504);
DELETE FROM usuario WHERE usuario LIKE 'seed_%' OR usuario LIKE 'qa_%';
```

## Contraseñas y usuarios

Todos los usuarios semilla usan la misma contraseña de prueba:

- Contraseña en claro: `ctn2025`
- Hash reutilizado: `$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y`

Usuarios creados:

| Email | Rol | Usuario | Especialidad |
| --- | --- | --- | --- |
| seed_admin_global@qa.local | Administrador global | seed_admin_global | N/A |
| seed_admin_informatica@qa.local | Admin de especialidad | seed_admin_informatica | Informática |
| seed_admin_electronica@qa.local | Admin de especialidad | seed_admin_electronica | Electrónica |
| seed_prof_informatica_1@qa.local | Profesor | seed_prof_informatica_1 | Informática |
| seed_prof_informatica_2@qa.local | Profesor | seed_prof_informatica_2 | Informática |
| seed_prof_electronica_1@qa.local | Profesor | seed_prof_electronica_1 | Electrónica |
| seed_evaluador_1@qa.local | Evaluador | seed_evaluador_1 | Informática |
| seed_evaluador_2@qa.local | Evaluador | seed_evaluador_2 | Electrónica |
| seed_padre_1@qa.local | Padre | seed_padre_1 | N/A |
| seed_padre_2@qa.local | Padre | seed_padre_2 | N/A |

## Qué valida este seed

- 2 especialidades reales: Informática y Electrónica
- Cursos con distintas secciones para ambos perfiles
- Materias específicas y una materia común enlazada a ambas especialidades
- Usuarios de todos los niveles relevantes: 1, 2, 3, 4
- Asignaciones profesor-materia-curso
- Planes curriculares con estados `PENDIENTE`, `APROBADO` y `RECHAZADO`
- Temas con estados `CUBIERTO` y `PENDIENTE`
- Caso `SIN_PLAN` en `planilla_rasgo.estado_verificacion_tema`
- Registros de sincronización Classroom para historial
- Escenario mínimo para admin por especialidad y pruebas de 403 en acceso cruzado

## Nota de seguridad

El script está pensado para entornos de desarrollo o QA y no debe ejecutarse en producción. Solo elimina las filas creadas con prefijo `seed_` y `qa_` y no toca ninguna fila real fuera de ese alcance.
