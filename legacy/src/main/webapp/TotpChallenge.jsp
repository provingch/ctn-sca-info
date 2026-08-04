<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Verificación 2FA" scope="request" />
<%@ include file="/WEB-INF/includes/head.jspf" %>

  <body class="login-page" data-user-level="${sessionScope.user.level}" data-specialty="${empty sessionScope.scaSpecialty ? 'general' : sessionScope.scaSpecialty}" data-specialty-source="session">
    <c:set var="navbarHomeUrl" value="${pageContext.request.contextPath}/index.jsp" />
    <c:set var="navbarShowSessionMenu" value="false" />
    <%@ include file="/WEB-INF/includes/navbar.jspf" %>

    <main class="login-main">
      <div class="login-wrapper">
        <div class="login-card">
          <div class="login-logo-container">
            <img class="login-logo" src="${pageContext.request.contextPath}/images/ctn-logo-2.svg" alt="CTN logo">
          </div>
          <div class="login-heading">
            <h1>Verificación en dos pasos</h1>
            <p>Ingresa el código de tu app de autenticación para continuar.</p>
          </div>
          <c:if test="${not empty verifyError}">
            <div class="login-error">${verifyError}</div>
          </c:if>
          <form class="login-form" action="${pageContext.request.contextPath}/totp" method="post">
            <label for="totpCode">Código de autenticación</label>
            <input class="form-password" type="text" id="totpCode" name="totpCode" maxlength="6" placeholder="123456" required autofocus>
            <input class="form-submit" type="submit" value="Verificar código">
          </form>
          <p class="login-info">Usuario: <strong><c:out value="${pendingUsername}"/></strong></p>
          <p class="login-info"><a href="${pageContext.request.contextPath}/index.jsp">Volver al inicio de sesión</a></p>
        </div>
      </div>
    </main>

    <footer class="footer">
      <hr>
          <p>Colegio T&eacute;cnico Nacional</p>
    <p><a href="${pageContext.request.contextPath}/privacidad">Pol&iacute;tica de privacidad</a> | <a href="${pageContext.request.contextPath}/terminos">T&eacute;rminos de servicio</a></p>
    </footer>

    <%@ include file="/WEB-INF/includes/footer-scripts.jspf" %>
  </body>
</html>
