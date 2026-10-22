# SCA — app Android

App nativa (Kotlin + Jetpack Compose), **versión 4.0.0**. Una sola app "SCA" para
cuatro roles: **Profesor** (nivel 1), **Evaluador** (2), **Padre/Encargado** (4) y
**Coordinación Pedagógica** (5). Las cuentas de **Administración** (3) se rechazan
en el login con *"Las cuentas de Administración se usan desde la computadora, en la
web del SCA."*

Antes era sólo "SCA Padres". El `applicationId` sigue siendo
`py.edu.ctn.sca.padres` a propósito: cambiarlo rompería FCM y las actualizaciones
sobre lo ya instalado.

Consume el mismo backend Spring Boot del repo:

| Uso | Endpoint |
|-----|----------|
| Login | `POST /api/auth/login` → (2FA) `POST /api/auth/2fa/verify` |
| Sesión | `POST /api/auth/refresh`, `POST /api/auth/logout` |
| Push | `POST /api/push/fcm`, `POST /api/push/fcm/unregister` |
| Avisos (todos) | `GET /api/notificaciones`, `GET /api/notificaciones/contador`, `POST /api/notificaciones/{id}/leer`, `POST /api/notificaciones/leer-todas` |
| Perfil (todos) | `GET /api/profile` + flujos de datos, contraseña y 2FA |
| Profesor · Hoy | `GET /api/home/mi-horario/hoy`, `GET /api/plan-curricular/mios`, `GET /api/home/alumnos-riesgo` |
| Profesor · Registro de clase | `POST /api/home/create-rasgo-planilla` (ausentes y códigos de conducta), `GET /api/codigos-conducta`, `GET /api/plan-curricular/mi-plan` (sugiere contenido) |
| Profesor · Clases | `GET /api/home/mis-clases`, `GET /api/home/mis-clases/{id}` |
| Profesor · Planillas y tareas | `GET /api/home`, `GET /api/planillas/{id}`, `GET /api/planillas/{id}/tareas`, `POST /api/planillas/{id}/notas` (autoguardado) |
| Padre · Hijos y Notas | `GET /api/padre?alumnoId=`, reportes PDF `reporte-mensual` y `libreta` |
| Padre · Conducta | `GET /api/padre/alumnos/{id}/conducta`, `GET /api/padre/alumnos/{id}/anotaciones`, `GET .../anotaciones/{anotacionId}/foto`, `POST .../visto` |
| Evaluador · Planes | `GET /api/plan-curricular/pendientes`, `GET /api/plan-curricular/{id}`, `GET /{id}/documento` (xlsx), `POST /{id}/aprobar`, `POST /{id}/rechazar?observaciones=` |
| Evaluador · Seguimiento | `GET /api/evaluacion/incumplimientos`, `POST /api/evaluacion/incumplimientos/{id}/resolver` |
| Coordinación · Quejas | `GET /api/admin/quejas`, `PUT /api/admin/quejas/{id}/aceptacion`, `PUT /api/admin/quejas/{id}/rechazo` |
| Coordinación · Anotaciones | `GET /api/admin/anotaciones?limite=200`, `GET /api/admin/anotaciones/{id}/foto` |

El refresh token viaja como cookie httpOnly; la app la persiste cifrada
(`EncryptedSharedPreferences` + `PersistentCookieJar`) y la reenvía en `/refresh`.
El access token vive sólo en memoria.

---

## Roles y pantallas

Rediseño según "SCA pantallas reales": acento **bordó** para Profesor y Padre,
**azul** para Evaluador, Coordinación y el login. Tipografía Inter (`res/font`,
licencia OFL en `licenses/Inter-OFL.txt`) y barra inferior propia de cada rol.

| Rol | Pantallas |
|-----|-----------|
| Todos | Avisos (notificaciones), Perfil (datos, contraseña, 2FA) |
| Profesor | Hoy, Registro de clase, Clases, Planillas y corrección de tareas |
| Padre/Encargado | Hijos, Notas (con reportes PDF), Conducta y detalle de anotación con foto |
| Evaluador | Inicio, Planes (aprobar/rechazar), Seguimiento de incumplimientos |
| Coordinación | Inicio, Quejas (aceptar/rechazar), Anotaciones (búsqueda y filtros del lado del celular) |

Los ViewModels viven en un store por sesión y se limpian al cerrar sesión.

## Requisitos

- **JDK 17–21** para correr Gradle (JDK 25 no). Lo más simple es usar el JBR que
  trae Android Studio, o `sudo pacman -S jdk21-openjdk`.
- **Android SDK** con Platform 36 y Build-Tools 35. Definí la ruta en
  `android/local.properties` (`sdk.dir=...`).

## Configuración (una sola vez)

```sh
cd android
cp local.properties.sample local.properties     # editá sdk.dir y sca.baseUrl
cp keystore.properties.sample keystore.properties   # ver "Firma de release"
```

### 1. Backend por HTTPS

Ya está: producción vive en `https://ctn-sca.ddns.net/` (nginx + Let's Encrypt).
Es el valor por defecto de `sca.baseUrl`, así que no hace falta configurar nada
para apuntar ahí.

Android bloquea tráfico en claro; los builds **debug** lo permiten
(`app/src/debug/AndroidManifest.xml`) sólo para poder probar contra un backend
sin TLS (p. ej. `http://10.0.2.2:8080/` desde el emulador). Release siempre HTTPS.

### 2. Firebase Cloud Messaging

1. Creá un proyecto en <https://console.firebase.google.com>.
2. Agregá una app Android con package `py.edu.ctn.sca.padres` (y
   `py.edu.ctn.sca.padres.debug` si querés push en debug).
3. Descargá `google-services.json` y ponelo en `android/app/`.
   Sin ese archivo la app compila igual, sólo que sin push.
4. En el backend, exponé las credenciales del **service account** de Firebase
   por env (cualquiera de las tres, en este orden de prioridad):
   - `FIREBASE_CREDENTIALS_JSON` — el JSON del service account, inline
   - `FIREBASE_CREDENTIALS` — ruta al archivo JSON
   - `GOOGLE_APPLICATION_CREDENTIALS` — el default del SDK
   Sin credenciales el backend arranca igual; el push queda deshabilitado y
   `/api/push/fcm` sólo guarda tokens.

### Endpoints de push (backend)

| | |
|---|---|
| `POST /api/push/fcm` `{token, platform}` | registra/renueva el token del dispositivo |
| `POST /api/push/fcm/unregister` `{token}` | baja el token (logout) |
| `POST /api/push/fcm/test` | envía un push de prueba a los dispositivos del usuario |

Disparadores automáticos: alta de tarea (`POST /api/planillas/{id}/tareas`) y
carga de notas (`POST /api/planillas/{id}/notas`) notifican a los padres de los
alumnos afectados. **Requiere vínculos en `alumno_usuario`** (hoy vacía en prod).

## Versionado

En `app/build.gradle.kts`: `versionName` es `mayor.menor.parche` (hoy `4.0.0`) y
`versionCode = mayor*10000 + menor*100 + parche` (hoy `40000`). Android sólo deja
actualizar si el `versionCode` sube.

## Compilar el APK

```sh
cd android
JAVA_HOME=<jdk21> ./gradlew :app:assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

El release está minificado con R8 (reglas en `app/proguard-rules.pro`). Se fijó
`androidx.fragment` 1.8.5 porque el lint de release lo exige para las Activity
Result APIs.

Debug (sin firma propia, para probar rápido):

```sh
JAVA_HOME=<jdk21> ./gradlew :app:assembleDebug
```

Override de URL sin tocar archivos:

```sh
./gradlew :app:assembleRelease -PscaBaseUrl=https://otra.url/
```

## Firma de release

El keystore va **fuera del repo**, por ejemplo `~/.android-keys/sca-release.jks`
con alias `sca`:

```sh
mkdir -p ~/.android-keys
keytool -genkeypair -v -keystore ~/.android-keys/sca-release.jks -alias sca \
  -keyalg RSA -keysize 2048 -validity 10000
```

Después completá `android/keystore.properties` (está en `.gitignore`; el molde es
`keystore.properties.sample`) con `storeFile` (ruta **absoluta**), `storePassword`,
`keyAlias` y `keyPassword`.

> **Guardá un respaldo del keystore y las contraseñas.** Sin la misma clave, las
> actualizaciones no se instalan encima de la app ya instalada: hay que
> desinstalar primero (y se pierde la sesión).

## Publicar una versión nueva

1. Subir `versionCode` y `versionName` en `app/build.gradle.kts`.
2. `JAVA_HOME=<jdk21> ./gradlew :app:assembleRelease`.
3. Copiar `app/build/outputs/apk/release/app-release.apk` a
   `frontend/public/app/sca.apk`.
4. Commit y deploy.

El APK queda en <https://ctn-sca.ddns.net/app/sca.apk>. Es público
(`SecurityConfig` permite `GET /app/**`) y el service worker no lo cachea. En la
web aparece "Descargar app Android" en el menú de usuario, junto a "Manual", y en
Mi perfil → App, para los niveles 1, 2, 4 y 5 (constante `APP_ANDROID_URL` en
`frontend/src/config/navigation.ts`).

## Instalación en el celular

1. Abrir <https://ctn-sca.ddns.net/app/sca.apk> en el celular.
2. Si avisa que puede ser dañino, tocar "Descargar de todas formas".
3. Permitir instalar apps de este origen (*Ajustes → Apps → Acceso especial →
   Instalar apps desconocidas*) y tocar Instalar.
4. En Xiaomi, aceptar además el escáner de seguridad de MIUI.

Las actualizaciones se instalan encima, sin perder la sesión.

## Limitaciones conocidas

Datos que el backend todavía no tiene:

- El Evaluador no ve el "cumplimiento del mes".
- La corrección de tareas no muestra fechas de entrega.
- El curso del hijo se toma de sus anotaciones.
- El documento del plan curricular es un xlsx (se abre con otra app).

## Estructura

```
app/src/main/java/py/edu/ctn/sca/padres/
  core/        Session, TokenStore, PersistentCookieJar, Network (OkHttp+Retrofit, 401→refresh), Role.kt
  data/<rol>/  DTOs, Retrofit APIs y repositorios por rol (profesor, padre, evaluador, coordinacion)
  ui/auth/     LoginFlow + AuthViewModel (credenciales → 2FA)
  ui/common/   Componentes, RoleScaffold, Avisos, Perfil
  ui/<rol>/    Pantallas y ViewModels por rol (profesor, padre, evaluador, coordinacion)
  fcm/         ScaMessagingService (recibe push, registra rotación de token)
  ScaApp.kt    Application + service locator (Graph)
```
