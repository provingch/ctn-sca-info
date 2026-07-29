<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:choose>
  <c:when test="${empty sessionScope.user}">
    <c:url var="homeUrl" value="/index.jsp" />
  </c:when>
  <c:when test="${sessionScope.user.level == 2}">
    <c:url var="homeUrl" value="/evaluacion" />
  </c:when>
  <c:when test="${sessionScope.user.level == 3}">
    <c:url var="homeUrl" value="/admin" />
  </c:when>
  <c:when test="${sessionScope.user.level == 4}">
    <c:url var="homeUrl" value="/padre" />
  </c:when>
  <c:otherwise>
    <c:url var="homeUrl" value="/inicio" />
  </c:otherwise>
</c:choose>
<!DOCTYPE html>
<html data-theme="light">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Condiciones del Servicio | SCA</title>
  <link rel="manifest" href="${pageContext.request.contextPath}/manifest.jsp">
  <meta name="theme-color" content="#1f2d3d">
  <meta name="apple-mobile-web-app-capable" content="yes">
  <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
  <meta name="apple-mobile-web-app-title" content="SCA">
  <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/icons/pwa/apple-touch-icon.png">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/vendor/flat-ui/css/flat-ui.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/ctn-theme.css?v=251">
  <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/images/ctn-logo.svg">
</head>
<body class="legal-page" data-page="terms-of-service" data-user-level="${empty sessionScope.user ? '1' : sessionScope.user.level}" data-specialty="general" data-specialty-source="system">
  <header class="navbar navbar-default navbar-fixed-top ctn-navbar" role="navigation">
    <div class="container-fluid">
      <div class="navbar-header">
        <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#ctnNavbarMenu" aria-expanded="false">
          <span class="sr-only">Abrir navegación</span>
          <span class="icon-bar"></span>
          <span class="icon-bar"></span>
          <span class="icon-bar"></span>
        </button>
        <a class="navbar-brand ctn-navbar-brand" href="${homeUrl}" aria-label="Ir al inicio">
          <img class="header-logo" src="${pageContext.request.contextPath}/images/ctn-logo.svg" alt="CTN">
          <span>Colegio Técnico Nacional</span>
        </a>
      </div>
      <div class="collapse navbar-collapse" id="ctnNavbarMenu">
        <ul class="nav navbar-nav navbar-right ctn-navbar-actions">
          <li class="ctn-theme-item"></li>
        </ul>
      </div>
    </div>
  </header>

  <main class="page-shell legal-shell">
    <a class="legal-back-link" href="${homeUrl}">← Volver al inicio</a>
    <h1>Condiciones del Servicio</h1>
    <p>El uso del sistema SCA está sujeto a estas condiciones. Al ingresar y usar la plataforma, aceptas estos términos y la política de privacidad asociada.</p>

    <h2>1. Acceso</h2>
    <p>Solo los usuarios autorizados del Colegio Técnico Nacional pueden ingresar con sus credenciales institucionales. Está prohibido compartir el acceso con terceros no autorizados.</p>

    <h2>2. Uso de Google Classroom</h2>
    <p>Cuando autorizás la conexión con Google Classroom, la aplicación solicitará permisos mediante OAuth. Los datos recibidos se usan exclusivamente para mostrar cursos y tareas integradas dentro del servicio.</p>

    <h2>3. Responsabilidades</h2>
    <ul>
      <li>Mantener la confidencialidad de tus credenciales.</li>
      <li>Usar el sistema conforme a las normas internas del colegio.</li>
      <li>No intentar vulnerar la seguridad de la plataforma ni acceder a cuentas ajenas.</li>
    </ul>

    <h2>4. Modificaciones</h2>
    <p>El colegio puede actualizar estas condiciones en cualquier momento. Te recomendamos revisar esta página periódicamente.</p>

    <div class="legal-page-links">
      <a href="${pageContext.request.contextPath}/privacidad">Ver Política de Privacidad</a>
    </div>
  </main>
  <script src="${pageContext.request.contextPath}/vendor/flat-ui/js/vendor/jquery.min.js"></script>
  <script src="${pageContext.request.contextPath}/vendor/flat-ui/js/flat-ui.js"></script>
  <script src="${pageContext.request.contextPath}/scripts/sca-theme.js?v=170"></script>
</body>
</html>
