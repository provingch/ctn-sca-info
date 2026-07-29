<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html data-theme="light">
  <head>
    <title>Verificación 2FA</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="manifest" href="${pageContext.request.contextPath}/manifest.jsp">
    <meta name="theme-color" content="#1f2d3d">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
    <meta name="apple-mobile-web-app-title" content="SCA">
    <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/icons/pwa/apple-touch-icon.png">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/vendor/flat-ui/css/flat-ui.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/ctn-theme.css?v=244">
    <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/images/ctn-logo.svg">
  </head>

  <body class="login-page" data-user-level="${sessionScope.user.level}" data-specialty="${empty sessionScope.scaSpecialty ? 'informatica' : sessionScope.scaSpecialty}" data-specialty-source="session">
    <header class="navbar navbar-default navbar-fixed-top ctn-navbar" role="navigation">
      <div class="container-fluid">
        <div class="navbar-header">
          <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#ctnNavbarMenu" aria-expanded="false">
            <span class="sr-only">Abrir navegación</span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </button>
          <a class="navbar-brand ctn-navbar-brand" href="${pageContext.request.contextPath}/index.jsp" aria-label="Ir a inicio">
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
          <form class="login-form" action="${pageContext.request.contextPath}/TotpServlet" method="post">
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
      <p>Colegio Técnico Nacional</p>
    </footer>

    <script src="${pageContext.request.contextPath}/vendor/flat-ui/js/vendor/jquery.min.js"></script>
    <script src="${pageContext.request.contextPath}/vendor/flat-ui/js/flat-ui.js"></script>
    <script src="${pageContext.request.contextPath}/scripts/sca-theme.js?v=168"></script>
    <script>
      if ('serviceWorker' in navigator) {
        window.addEventListener('load', () => {
          navigator.serviceWorker.register('${pageContext.request.contextPath}/sw.js').catch(console.error);
        });
      }
    </script>
  </body>
</html>
