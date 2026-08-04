VERIFICATION CHECKLIST — Google OAuth Consent

Objetivo: preparar y evidenciar lo necesario para que Google verifique la pantalla de consentimiento y acepte la aplicación.

Pasos principales:

1) Verificar el dominio público
- Añadir un registro DNS TXT o subir el archivo HTML que Google provee a la raíz pública del sitio.
- Verificación requerida en Google Search Console.

2) Homepage pública coherente
- URL: https://ctn-sca.ddns.net/ (o el host público que uses)
- Contenido requerido:
  - Nombre de la app exactamente como figura en la pantalla de consentimiento: "Sistema de Carpeta Academica"
  - Breve descripción de la función de la app (1-2 frases).
  - Datos de contacto (email de soporte/propietario).
  - Links visibles a Política de Privacidad y Términos de Servicio: /privacidad y /terminos
  - Opcional: captura de pantalla del flujo de la app.

3) Privacy policy & Terms
- Asegurarse que /privacidad y /terminos sean accesibles públicamente y no requieran inicio de sesión.
- Incluir texto que explique el uso de datos (qué se recopila, por qué, y cómo se comparte con Google Classroom si aplica).

4) OAuth consent screen config
- En Google Cloud Console > OAuth consent screen:
  - App name: Sistema de Carpeta Academica
  - Homepage URL: https://ctn-sca.ddns.net/
  - Privacy policy URL: https://ctn-sca.ddns.net/privacidad
  - Terms of service URL: https://ctn-sca.ddns.net/terminos
  - Support email: (usar email de contacto)
- Scopes: listar solo los scopes necesarios (ej: Google Classroom readonly o drive si aplica) y justificar cada scope.

5) OAuth credentials
- En Credentials > OAuth 2.0 Client IDs:
  - Authorized redirect URIs: https://ctn-sca.ddns.net/google/callback (ajustar si usas otro path)

6) Evidencias a subir/adjuntar para verificación de Google
- Captura de pantalla de la homepage mostrando el app name y la descripción.
- Captura de pantalla de /privacidad y /terminos.
- Prueba de verificación de dominio (registro TXT o captura de Search Console).
- Archivo `manifest.jsp` y/o meta `google-site-verification` si aplica.

Notas técnicas para este repo:
- index.jsp actualizado con título, meta description y sección visible que muestra el nombre de la app y descripción.
- Las páginas de política están ubicadas en /WEB-INF/legal y expuestas mediante LegalPageServlet en /privacidad y /terminos; verifica que no haya filtros que entren a bloquear acceso anónimo.
- Asegúrate de que las rutas no requieran autenticación por defecto en AuthFilter.

Siguiente paso recomendado (puedo hacerlo si confirmás):
- Reemplazo de los `action="LoginServlet"` y otros `*Servlet` restantes por rutas amigables `/login`, `/admin/...` etc. (automatizable).
- Preparar y tomar screenshots públicas si me das acceso temporal al host o subís las imágenes aquí.
