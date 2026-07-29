<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html data-theme="light">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Política de Privacidad | SCA</title>
  <link rel="manifest" href="${pageContext.request.contextPath}/manifest.jsp">
  <meta name="theme-color" content="#1f2d3d">
  <meta name="apple-mobile-web-app-capable" content="yes">
  <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
  <meta name="apple-mobile-web-app-title" content="SCA">
  <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/icons/pwa/apple-touch-icon.png">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/vendor/flat-ui/css/flat-ui.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/ctn-theme.css?v=238">
  <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/images/ctn-logo.svg">
  <style>
    .page-shell { max-width: 860px; margin: 0 auto; padding: 32px 24px; }
    .page-shell h1 { margin-bottom: 0.5rem; }
    .page-shell p, .page-shell li { line-height: 1.75; }
    .page-shell ul { margin: 1rem 0 1.5rem 1.2rem; }
    .page-shell .page-links { margin-top: 32px; }
    .page-links a { color: var(--accent); text-decoration: none; }
    .page-links a:hover { text-decoration: underline; }
  </style>
</head>
<body data-user-level="${empty sessionScope.user ? '1' : sessionScope.user.level}" data-specialty="${empty sessionScope.scaSpecialty ? 'informatica' : sessionScope.scaSpecialty}" data-specialty-source="session">
  <main class="page-shell">
    <h1>Política de Privacidad</h1>
    <p>En Colegio Técnico Nacional valoramos tu privacidad. Esta página describe cómo recopilamos, usamos y protegemos tus datos cuando utilizas el sistema SCA.</p>

    <h2>1. Datos que recopilamos</h2>
    <ul>
      <li>Información personal básica registrada en el colegio.</li>
      <li>Credenciales de acceso únicamente para la autenticación y autorización.</li>
      <li>Datos de Google Classroom cuando el usuario autoriza la integración.</li>
    </ul>

    <h2>2. Uso de los datos</h2>
    <p>Los datos se usan para brindar servicios académicos, mostrar información de cursos, asociar planillas con usuarios y ejecutar la integración autorizada con Google Classroom.</p>

    <h2>3. Integración con Google Classroom</h2>
    <p>El acceso a Google Classroom se realiza solo cuando el profesor autoriza la conexión. El sistema usa el cliente OAuth configurado en la aplicación para solicitar permiso a Google y almacenar tokens seguros en la base de datos.</p>

    <h2>4. Seguridad</h2>
    <p>Se toman medidas razonables para proteger la información contra accesos no autorizados y para mantenerla segura durante su uso dentro de la aplicación.</p>

    <h2>5. Contacto</h2>
    <p>Si tenés dudas sobre esta política o sobre tus datos, dirigite al equipo de administración del colegio para obtener más información.</p>

    <div class="page-links">
      <a href="${pageContext.request.contextPath}/terminos">Ver Condiciones del Servicio</a>
    </div>
  </main>
  <script src="${pageContext.request.contextPath}/vendor/flat-ui/js/vendor/jquery.min.js"></script>
  <script src="${pageContext.request.contextPath}/vendor/flat-ui/js/flat-ui.js"></script>
  <script src="${pageContext.request.contextPath}/scripts/sca-theme.js?v=170"></script>
</body>
</html>
