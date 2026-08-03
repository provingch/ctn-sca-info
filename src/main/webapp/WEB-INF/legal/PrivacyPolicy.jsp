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
<c:set var="pageTitle" value="Política de Privacidad" scope="request" />
<%@ include file="/WEB-INF/includes/head.jspf" %>
<body class="legal-page" data-page="privacy-policy" data-user-level="${empty sessionScope.user ? '1' : sessionScope.user.level}" data-specialty="general" data-specialty-source="system">
  <c:set var="navbarHomeUrl" value="${homeUrl}" />
  <c:set var="navbarHomeAriaLabel" value="Ir al inicio" />
  <c:set var="navbarShowSessionMenu" value="false" />
  <%@ include file="/WEB-INF/includes/navbar.jspf" %>

  <main class="page-shell legal-shell">
    <a class="legal-back-link" href="${homeUrl}">← Volver al inicio</a>
    <h1>Pol&iacute;tica de Privacidad</h1>
    <p>En Colegio T&eacute;cnico Nacional valoramos tu privacidad. Esta página describe cómo recopilamos, usamos y protegemos tus datos cuando utilizas el sistema SCA.</p>

    <div class="legal-transparency-section">
      <%@ include file="/WEB-INF/includes/app-transparency.jspf" %>
    </div>

    <h2>1. Datos que recopilamos</h2>
    <ul>
      <li>Información personal básica registrada en el colegio.</li>
      <li>Credenciales de acceso únicamente para la autenticación y autorización.</li>
      <li>Datos de Google Classroom cuando el usuario autoriza la integración.</li>
    </ul>

    <h2>2. Uso de los datos</h2>
    <p>Los datos se usan para brindar servicios académicos, mostrar información de cursos, asociar planillas con usuarios y ejecutar la integración autorizada con Google Classroom.</p>

    <h2>3. Integración con Google Classroom</h2>
    <p>El acceso a Google Classroom se realiza solo cuando el profesor autoriza la conexión. El sistema usa el cliente OAuth configurado en la aplicaci&oacute;n para solicitar permiso a Google y almacenar tokens seguros en la base de datos.</p>

    <h2>4. Seguridad</h2>
    <p>Se toman medidas razonables para proteger la información contra accesos no autorizados y para mantenerla segura durante su uso dentro de la aplicaci&oacute;n.</p>

    <h2>5. Contacto</h2>
    <p>Si tenés dudas sobre esta política o sobre tus datos, dirigite al equipo de administración del colegio para obtener más información.</p>

    <div class="legal-page-links">
      <a href="${pageContext.request.contextPath}/terminos">Ver Condiciones del Servicio</a>
    </div>
  </main>
  <%@ include file="/WEB-INF/includes/footer-scripts.jspf" %>
</body>
</html>
