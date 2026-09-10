# SCA Padres — app Android

App nativa (Kotlin + Jetpack Compose) que replica la **vista de padres** del SCA:
login con 2FA, resumen académico por hijo, materias por etapa, detalle de tareas
y notificaciones push (FCM) cuando se publica o califica una tarea.

Consume el mismo backend Spring Boot del repo:

| Uso | Endpoint |
|-----|----------|
| Login | `POST /api/auth/login` → (2FA) `POST /api/auth/2fa/verify` |
| Sesión | `POST /api/auth/refresh`, `POST /api/auth/logout` |
| Datos | `GET /api/padre?alumnoId=` |
| Push | `POST /api/push/fcm`, `POST /api/push/fcm/unregister` *(agregados en esta rama)* |

El refresh token viaja como cookie httpOnly; la app la persiste cifrada
(`EncryptedSharedPreferences` + `PersistentCookieJar`) y la reenvía en `/refresh`.
El access token vive sólo en memoria.

---

## Requisitos

- **JDK 17–21** para correr Gradle. Lo más simple es usar el JBR que trae
  Android Studio, o `sudo pacman -S jdk21-openjdk`. (El sistema trae sólo JDK 25;
  si querés usarlo, subí Gradle a 9.1+ en `gradle/wrapper/gradle-wrapper.properties`
  y AGP a una versión compatible.)
- **Android SDK** con Platform 35+ y Build-Tools 35+. Definí la ruta en
  `android/local.properties` (`sdk.dir=...`).

## Configuración (una sola vez)

```sh
cd android
cp local.properties.sample local.properties     # editá sdk.dir y sca.baseUrl
cp keystore.properties.sample keystore.properties
keytool -genkeypair -v -keystore sca-padres.jks -alias sca \
  -keyalg RSA -keysize 2048 -validity 10000       # y completá keystore.properties
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

### Endpoints de push (backend, en esta rama)

| | |
|---|---|
| `POST /api/push/fcm` `{token, platform}` | registra/renueva el token del dispositivo |
| `POST /api/push/fcm/unregister` `{token}` | baja el token (logout) |
| `POST /api/push/fcm/test` | envía un push de prueba a los dispositivos del usuario |

Disparadores automáticos: alta de tarea (`POST /api/planillas/{id}/tareas`) y
carga de notas (`POST /api/planillas/{id}/notas`) notifican a los padres de los
alumnos afectados. **Requiere vínculos en `alumno_usuario`** (hoy vacía en prod).

## Compilar el APK

```sh
cd android
./gradlew :app:assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

Debug (sin firma propia, para probar rápido):

```sh
./gradlew :app:assembleDebug
```

Override de URL sin tocar archivos:

```sh
./gradlew :app:assembleRelease -PscaBaseUrl=https://otra.url/
```

## Sideload

Pasá el `.apk` por link/USB. En el teléfono: *Ajustes → Apps → Acceso especial →
Instalar apps desconocidas* para la app desde donde se abra el archivo.

## Estructura

```
app/src/main/java/py/edu/ctn/sca/padres/
  core/      Session, TokenStore, PersistentCookieJar, Network (OkHttp+Retrofit, 401→refresh)
  data/      DTOs (mirror del backend), Retrofit APIs, repositorios
  ui/auth/   LoginFlow + AuthViewModel (credenciales → 2FA)
  ui/parent/ ParentDashboardScreen + ParentViewModel (réplica de ParentPage.tsx)
  fcm/       ScaMessagingService (recibe push, registra rotación de token)
  ScaApp.kt  Application + service locator (Graph)
```
