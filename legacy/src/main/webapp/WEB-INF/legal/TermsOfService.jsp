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
<c:set var="pageTitle" value="Condiciones del Servicio" scope="request" />
<%@ include file="/WEB-INF/includes/head.jspf" %>
<body class="legal-page" data-page="terms-of-service" data-user-level="${empty sessionScope.user ? '1' : sessionScope.user.level}" data-specialty="general" data-specialty-source="system">
  <c:set var="navbarHomeUrl" value="${homeUrl}" />
  <c:set var="navbarHomeAriaLabel" value="Ir al inicio" />
  <c:set var="navbarShowSessionMenu" value="false" />
  <%@ include file="/WEB-INF/includes/navbar.jspf" %>

  <main class="page-shell legal-shell">
    <a class="legal-back-link" href="${homeUrl}">← Volver al inicio</a>
    <h1>Condiciones del Servicio</h1>
    <p>El uso del sistema SCA está sujeto a estas condiciones. Al ingresar y usar la plataforma, aceptas estos términos y la política de privacidad asociada.</p>

    <h2>1. Acceso</h2>
    <p>Solo los usuarios autorizados del Colegio T&eacute;cnico Nacional pueden ingresar con sus credenciales institucionales. Está prohibido compartir el acceso con terceros no autorizados.</p>

    <h2>2. Uso de Google Classroom</h2>
    <p>Cuando autorizás la conexión con Google Classroom, la aplicaci&oacute;n solicitará permisos mediante OAuth. Los datos recibidos se usan exclusivamente para mostrar cursos y tareas integradas dentro del servicio.</p>

    <h2>3. Responsabilidades</h2>
    <ul>
      <li>Mantener la confidencialidad de tus credenciales.</li>
      <li>Usar el sistema conforme a las normas internas del colegio.</li>
      <li>No intentar vulnerar la seguridad de la plataforma ni acceder a cuentas ajenas.</li>
    </ul>

    <h2>4. Modificaciones</h2>
    <p>El colegio puede actualizar estas condiciones en cualquier momento. Te recomendamos revisar esta página periódicamente.</p>

    <div class="legal-page-links">
      <a href="${pageContext.request.contextPath}/privacidad">Ver Pol&iacute;tica de Privacidad</a>
    </div>
  </main>
  <%@ include file="/WEB-INF/includes/footer-scripts.jspf" %>
</body>
</html>
