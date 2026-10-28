# API de Enfermería: consultas de funcionarios

Endpoints para que Enfermería (SHME) atienda a los funcionarios del colegio: registrar sus consultas, ver su historial y completar el **Formulario de Consulta para Docentes y Administrativos**. El PDF lo genera SHME; la API solo devuelve los datos.

Los endpoints de alumnos (`alumnos_buscar`, `ficha`, `consultas`, `consulta_*`, `motivos`, `consultas_reporte`, …) no cambian.

## Quién es "funcionario"

Un registro de la tabla `usuario` del colegio con `nivel` 1, 2, 3 o 5. Las familias (`nivel` 4) quedan afuera.

| `nivel` | `rol` |
|---|---|
| 1 | Profesor |
| 2 | Evaluador |
| 3 | Administrador |
| 5 | Coordinación Pedagógica |

- Enfermería **no copia** funcionarios: las consultas guardan `personal_id` = `usuario.id`, y nombre, CI y rol se leen siempre de `usuario`.
- El `rol` que devuelve la API es el **actual**. Si a alguien le cambian el nivel, su historial sigue asociado a su `usuario.id`.
- La API nunca devuelve contraseñas, tokens, secretos 2FA, cuentas de Google, firma ni foto.
- Un funcionario que tiene consultas en Enfermería **no se puede borrar** desde el panel del colegio (la base lo impide), igual que pasa con quien tiene anotaciones. Así no se pierde el historial médico.

## Convenciones (las mismas de toda la API de Enfermería)

- Base: `/api/enfermeria/`, sin `.php`.
- Parámetros por query (`GET`) o por formulario (`POST`, `application/x-www-form-urlencoded`). Los de lectura aceptan `GET` o `POST`; los que escriben, solo `POST`.
- Cabeceras en **todas** las llamadas:
  - `X-API-Key: <clave de la app>`
  - `Authorization: Bearer <token de /login>`
- Respuesta correcta: `{"ok": true, "datos": ...}`. Error: `{"ok": false, "error": "mensaje"}` con el código HTTP correspondiente.
- Fechas `AAAA-MM-DD`; horas `HH:MM:SS` (también se acepta `HH:MM`). Se devuelven como texto.
- Textos: se recortan los espacios; vacío = `null`.

### Permisos

Todos los endpoints de esta sección los pueden usar los roles de Enfermería `administrador` y `enfermeria`. La autorización la valida el servidor en cada llamada (clave de la app + token válido de un usuario de Enfermería). No hay endpoints exclusivos de administrador.

## Endpoints

| Endpoint | Método | Función |
|---|---|---|
| `personal_buscar` | GET/POST | Buscar funcionarios por CI, nombre o apellido |
| `personal` | GET/POST | Datos de un funcionario |
| `personal_consultas` | GET/POST | Historial de consultas de un funcionario |
| `personal_consulta` | GET/POST | Una consulta, con insumos y datos del funcionario (para el formulario) |
| `personal_consulta_crear` | POST | Registrar una consulta (descuenta stock) |
| `personal_consulta_editar` | POST | Editar una consulta (ajusta stock) |
| `personal_consulta_eliminar` | POST | Eliminar una consulta (devuelve stock) |
| `personal_consultas_reporte` | GET/POST | Consultas de un rango de fechas, para reportes |

---

### `personal_buscar`

Busca entre los funcionarios (niveles 1, 2, 3 y 5) por CI, nombre, apellido o "nombre apellido", sin distinguir mayúsculas. La CI se compara también sin puntos ni guiones: `1.234.567` encuentra `1234567`.

| Parámetro | Oblig. | Default | Descripción |
|---|---|---|---|
| `q` | no | `""` | Texto a buscar. Vacío lista a todos. |
| `pagina` | no | `1` | Página, desde 1. |
| `limite` | no | `20` | Resultados por página, de 1 a 100. |

Orden: apellido, nombre, id. `total` es la cantidad de coincidencias sin paginar (va al lado de `datos`).

```http
GET /api/enfermeria/personal_buscar?q=gonz&pagina=1&limite=20
```

```json
{
  "ok": true,
  "datos": [
    {"id": 30, "ci": "1234567", "nombre": "Laura", "apellido": "González", "nivel": 1, "rol": "Profesor"},
    {"id": 41, "ci": "2345678", "nombre": "Mario", "apellido": "González", "nivel": 3, "rol": "Administrador"}
  ],
  "total": 2
}
```

`ci` puede ser `null` si el colegio no la cargó.

### `personal`

| Parámetro | Oblig. | Descripción |
|---|---|---|
| `id` | sí | `usuario.id` del funcionario |

```json
{
  "ok": true,
  "datos": {
    "id": 30,
    "ci": "1234567",
    "nombre": "Laura",
    "apellido": "González",
    "nivel": 1,
    "rol": "Profesor",
    "correo": "laura.gonzalez@ejemplo.edu.py",
    "telefono": null,
    "celular": "981123456",
    "especialidad": null
  }
}
```

- `correo`, `telefono`, `celular` y `especialidad` pueden ser `null`.
- `especialidad`: solo la tienen los administradores asignados a una especialidad. No es un dato de "dependencia" o "área".
- 404 `El funcionario no existe` si el id no existe o es de una familia.

### `personal_consultas`

Historial de un funcionario, de la más reciente a la más antigua (fecha, hora de entrada, id). No filtra por nivel: muestra todo lo registrado para ese `usuario.id`.

| Parámetro | Oblig. | Descripción |
|---|---|---|
| `personal` | sí | `usuario.id` del funcionario |

```json
{
  "ok": true,
  "datos": [
    {
      "idConsulta": 8,
      "personal_id": 30,
      "motivo": "Cefalea",
      "fecha": "2026-10-28",
      "tratamiento": "Reposo 20 minutos y paracetamol 500 mg",
      "horaEntrada": "10:15:00",
      "horaSalida": "10:40:00",
      "observaciones": "PA 120/80, FC 78. Se retira a su domicilio.",
      "idUsuario": 5,
      "usuario": "ana"
    }
  ]
}
```

Lista vacía (`[]`) si no tiene consultas o si el id no existe. `idUsuario`/`usuario` son la cuenta de Enfermería que registró la consulta (`null` si esa cuenta se borró). Este listado no trae insumos: están en `personal_consulta`.

### `personal_consulta`

Una consulta con todo lo necesario para el formulario.

| Parámetro | Oblig. | Descripción |
|---|---|---|
| `id` | sí | `idConsulta` |

```json
{
  "ok": true,
  "datos": {
    "idConsulta": 8,
    "personal_id": 30,
    "motivo": "Cefalea",
    "fecha": "2026-10-28",
    "tratamiento": "Reposo 20 minutos y paracetamol 500 mg",
    "horaEntrada": "10:15:00",
    "horaSalida": "10:40:00",
    "observaciones": "PA 120/80, FC 78. Se retira a su domicilio.",
    "idUsuario": 5,
    "usuario": "ana",
    "insumos": [
      {"id_insumo": 3, "nombre": "Paracetamol 500 mg", "cantidad": 1}
    ],
    "funcionario": {
      "id": 30, "ci": "1234567", "nombre": "Laura", "apellido": "González",
      "nivel": 1, "rol": "Profesor",
      "correo": "laura.gonzalez@ejemplo.edu.py", "telefono": null, "celular": "981123456",
      "especialidad": null
    }
  }
}
```

404 `La consulta no existe`. Los ids de consultas de funcionarios son una numeración propia: el `idConsulta` 8 de un funcionario no tiene nada que ver con el `idConsulta` 8 de un alumno. Nunca uses los endpoints de alumnos (`consulta`, `consulta_editar`, …) con ids de funcionarios ni al revés.

### `personal_consulta_crear`

| Parámetro | Oblig. | Límite | Descripción |
|---|---|---|---|
| `personal` | sí | | `usuario.id` del funcionario (nivel 1, 2, 3 o 5) |
| `motivo` | sí | 150 car. | |
| `fecha` | sí | | `AAAA-MM-DD` |
| `tratamiento` | no | 65 535 car. | Atención brindada |
| `horaEntrada` | no | | `HH:MM[:SS]` |
| `horaSalida` | no | | `HH:MM[:SS]`, no anterior a `horaEntrada` |
| `observaciones` | no | 65 535 car. | Todo lo que registre la enfermera (signos vitales, derivación, etc.) |
| `insumos` | no | | `id:cantidad` separados por coma. Ej.: `3:1,7:2`. Un id repetido suma sus cantidades. Vacío = sin insumos. |

La consulta queda a nombre de la cuenta de Enfermería que inició sesión (`idUsuario`); no se manda como parámetro.

```http
POST /api/enfermeria/personal_consulta_crear
personal=30&motivo=Cefalea&fecha=2026-10-28&horaEntrada=10:15&horaSalida=10:40&tratamiento=Reposo&observaciones=PA+120/80&insumos=3:1
```

```json
{"ok": true, "datos": {"idConsulta": 8}}
```

### `personal_consulta_editar`

Reemplaza **todos** los campos de la consulta, incluidos los insumos: mandá siempre la consulta completa, igual que en `consulta_editar` de alumnos. Un campo opcional que no se manda queda en `null`, y si no mandás `insumos`, la consulta queda sin insumos y se devuelve su stock.

Parámetros: `id` (obligatorio, el `idConsulta`) + los mismos de `personal_consulta_crear`.

```json
{"ok": true, "datos": {"idConsulta": 8}}
```

404 `La consulta no existe` o `El funcionario no existe`.

### `personal_consulta_eliminar`

| Parámetro | Oblig. | Descripción |
|---|---|---|
| `id` | sí | `idConsulta` |

Devuelve al stock los insumos de la consulta y la borra.

```json
{"ok": true, "datos": {"idConsulta": 8}}
```

### `personal_consultas_reporte`

Consultas de funcionarios entre dos fechas (incluidas), con datos del funcionario e insumos. Orden: fecha, hora de entrada, id.

| Parámetro | Oblig. | Descripción |
|---|---|---|
| `desde` | sí | `AAAA-MM-DD` |
| `hasta` | sí | `AAAA-MM-DD`, no anterior a `desde` |
| `personal` | no | Solo ese funcionario |
| `motivo` | no | Solo ese motivo (texto exacto) |

```json
{
  "ok": true,
  "datos": [
    {
      "idConsulta": 8, "personal_id": 30, "motivo": "Cefalea", "fecha": "2026-10-28",
      "tratamiento": "Reposo", "horaEntrada": "10:15:00", "horaSalida": "10:40:00",
      "observaciones": "PA 120/80", "idUsuario": 5, "usuario": "ana",
      "ci": "1234567", "nombre": "Laura", "apellido": "González", "nivel": 1, "rol": "Profesor",
      "insumos": [{"id_insumo": 3, "nombre": "Paracetamol 500 mg", "cantidad": 1}]
    }
  ]
}
```

## Stock: cómo se registra y se revierte

El stock es el mismo (`enf_insumo`) para alumnos y funcionarios. Las consultas de funcionarios usan exactamente la misma lógica que las de alumnos:

- **Crear**: en una sola transacción se guarda la consulta y, por cada insumo (en orden de id), se descuenta el stock **solo si alcanza** y se registra en el detalle de la consulta. Si un insumo no existe (422) o no alcanza (409), **se revierte todo**: no se crea la consulta y no se descuenta nada. El stock nunca queda negativo.
- **Editar**: en una transacción, se bloquea la consulta, se devuelve al stock lo que tenía, se actualizan los datos y se descuentan los insumos nuevos. Si algo falla (por ejemplo, no alcanza el stock nuevo), la consulta y el stock quedan como estaban.
- **Eliminar**: en una transacción, se devuelve al stock lo que tenía la consulta y se borra con su detalle.
- **Trazabilidad**: el detalle de cada consulta (`enf_detalle_consulta_personal`) registra qué insumos y cuántos consumió esa consulta y quién la registró. Refleja el estado **actual**: al editar se reemplaza y al eliminar se borra. No hay un libro de movimientos aparte; las consultas de alumnos funcionan igual.
- La validación de stock no mira si el insumo está activo, igual que en alumnos. SHME debería ofrecer solo los de `insumos` (activos).

## Errores

| HTTP | `error` (ejemplos) | Cuándo |
|---|---|---|
| 401 | `Clave de la app inválida o ausente` | Falta `X-API-Key` o es incorrecta |
| 401 | `Falta iniciar sesión` (sin token) | Falta el token, o es inválido o vencido (también tras cambiar la contraseña). Ante cualquier 401 con token, volver a llamar a `login`. |
| 404 | `El funcionario no existe` | `personal` no existe o es una familia |
| 404 | `La consulta no existe` | `id` de consulta inexistente |
| 409 | `No hay stock suficiente de Paracetamol 500 mg (quedan 0)` | Un insumo no alcanza; no se guardó nada |
| 413 | | El cuerpo supera 256 KB |
| 422 | `Falta el parámetro 'motivo'` | Falta un obligatorio |
| 422 | `El parámetro 'personal' debe ser un número entero` | Id no numérico |
| 422 | `'fecha' debe tener el formato AAAA-MM-DD` / `'horaEntrada' debe tener el formato HH:MM:SS` | Formato |
| 422 | `'horaSalida' no puede ser anterior a 'horaEntrada'` | |
| 422 | `'motivo' supera los 150 caracteres` | Largo máximo |
| 422 | `'insumos' debe ser 'id:cantidad' separados por coma` / `La cantidad de cada insumo debe ser 1 o más` / `El insumo 99 no existe` | Insumos |
| 422 | `'pagina' debe ser 1 o más y 'limite' entre 1 y 100` | Paginación |
| 422 | `'hasta' no puede ser anterior a 'desde'` | Reporte |
| 500 | `Error interno del servidor` | Error inesperado (queda en el log del servidor) |
| 503 | | La clave de la app no está configurada en el servidor |

## Formulario de Consulta para Docentes y Administrativos

| Dato del formulario | De dónde sale |
|---|---|
| ID, CI, nombre, apellido | `funcionario.id`, `.ci`, `.nombre`, `.apellido` |
| Rol actual | `funcionario.rol` |
| Correo, teléfono | `funcionario.correo`, `.telefono`, `.celular` (pueden ser `null`) |
| Dependencia o área | **No existe en la API**: completarlo en SHME |
| Fecha de nacimiento / edad, sexo | **No existen en la API**: completarlo en SHME |
| Fecha y horas | `fecha`, `horaEntrada`, `horaSalida` |
| Motivo | `motivo` |
| Atención brindada | `tratamiento` |
| Observaciones de la consulta | `observaciones` (texto completo) |
| Insumos utilizados | `insumos[]` (`nombre`, `cantidad`) |
| Enfermera que atendió | `usuario` |

Lo que SHME le pida a la enfermera para completar el PDF no se guarda en la base.

## Base de datos (migración `013_enfermeria_personal.sql`)

- `enf_consulta_personal`: los mismos campos que `enf_consulta`, con `personal_id` → `usuario.id` (`ON DELETE RESTRICT`) en lugar de `alumno_id`.
- `enf_detalle_consulta_personal`: insumos de cada consulta → `enf_insumo` (compartido con alumnos).

No modifica ninguna tabla existente.
