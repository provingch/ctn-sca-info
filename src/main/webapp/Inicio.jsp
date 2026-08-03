<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!DOCTYPE html>
<html data-theme="light">
<head>
  <title>SCA - Inicio</title>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="manifest" href="${pageContext.request.contextPath}/manifest.jsp?v=${assetVersion}">
  <meta name="theme-color" content="#1f2d3d">
  <meta name="apple-mobile-web-app-capable" content="yes">
  <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
  <meta name="apple-mobile-web-app-title" content="SCA">
  <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/icons/pwa/apple-touch-icon.png">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/vendor/flat-ui/css/flat-ui.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/ctn-theme.css?v=${assetVersion}">
  <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/images/ctn-logo.svg">
</head>
<body data-user-level="${sessionScope.user.level}" data-specialty="${empty sessionScope.scaSpecialty ? 'general' : sessionScope.scaSpecialty}" data-specialty-source="session">
  <c:url var="profileUrl" value="/perfil" />
  <c:url var="logoutUrl" value="/logout" />
  <header class="navbar navbar-default navbar-fixed-top ctn-navbar" role="navigation">
    <div class="container-fluid">
      <div class="navbar-header">
        <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#ctnNavbarMenu" aria-expanded="false">
          <span class="sr-only">Abrir navegación</span>
          <span class="icon-bar"></span>
          <span class="icon-bar"></span>
          <span class="icon-bar"></span>
        </button>
        <a class="navbar-brand ctn-navbar-brand" href="${pageContext.request.contextPath}/inicio" aria-label="Ir a inicio">
          <img class="header-logo" src="${pageContext.request.contextPath}/images/ctn-logo.svg" alt="CTN">
          <span>Colegio T&eacute;cnico Nacional</span>
        </a>
      </div>
      <div class="collapse navbar-collapse" id="ctnNavbarMenu">
        <ul class="nav navbar-nav navbar-right ctn-navbar-actions">
          <li class="ctn-theme-item"></li>
          <c:choose>
            <c:when test="${sessionScope.user.level == 1}">
              <c:set var="manualHref" value="${pageContext.request.contextPath}/pdfs/manual-profesor.pdf" />
            </c:when>
            <c:when test="${sessionScope.user.level == 2}">
              <c:set var="manualHref" value="${pageContext.request.contextPath}/pdfs/manual-evaluador.pdf" />
            </c:when>
            <c:when test="${sessionScope.user.level == 3}">
              <c:set var="manualHref" value="${pageContext.request.contextPath}/pdfs/manual-administrador.pdf" />
            </c:when>
            <c:when test="${sessionScope.user.level == 4}">
              <c:set var="manualHref" value="${pageContext.request.contextPath}/pdfs/manual-padres.pdf" />
            </c:when>
            <c:otherwise>
              <c:set var="manualHref" value="${pageContext.request.contextPath}/pdfs/manual-profesor.pdf" />
            </c:otherwise>
          </c:choose>
          <li><a class="manual-link" href="${manualHref}" target="_blank" rel="noopener noreferrer">Manual</a></li>
          <li class="dropdown">
            <a href="#" id="sessionButton" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">Sesión <span class="caret"></span></a>
            <ul class="dropdown-menu" id="sessionMenu" role="menu" aria-labelledby="sessionButton">
              <li><a role="menuitem" href="${profileUrl}">Mi Perfil</a></li>
              <li><a role="menuitem" class="session-logout" href="${logoutUrl}">Cerrar Sesión</a></li>
            </ul>
          </li>
        </ul>
      </div>
    </div>
  </header>

  <main>
    <section class="container page-shell">
      <div class="info-bar">
        <span>Bienvenido/a ${sessionScope.user.fullName}</span>
        <span class="info-bar-spacer"></span>
        <span><c:out value="${nowFormatted}" /></span>
      </div>

      <div class="top-section planilla-hero hero-shell">
        <div class="planilla-hero__header">
          <div class="planilla-hero__info">
            <span class="badge"><span class="dot"></span>Accesos rápidos</span>
            <h1>Elegí cómo querés empezar</h1>
            <p class="planilla-subtitle">Selecciona una acción para abrir el panel con tu curso autoseleccionado.</p>
          </div>
        </div>
      </div>

      <div class="home-view-tabs" role="navigation" aria-label="Acciones principales">
        <a href="${pageContext.request.contextPath}/inicio?view=clase" class="home-view-tab home-view-tab--primary">
          <span class="home-view-tab__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" focusable="false"><path d="M7 4.8v14.4L19 12 7 4.8Z"/></svg>
          </span>
          <span class="home-view-tab__copy">
            <strong>Iniciar clase</strong>
            <small>Abrir asistencia y seguimiento de clase</small>
          </span>
        </a>

        <a href="${pageContext.request.contextPath}/inicio?view=planillas" class="home-view-tab">
          <span class="home-view-tab__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" focusable="false"><path d="M9 5h2.1a3 3 0 0 1 5.8 0H19a2 2 0 0 1 2 2v13a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h2.1A3 3 0 0 1 9 5Zm3-1.5A1.5 1.5 0 1 0 12 6a1.5 1.5 0 0 0 0-3ZM7 10v2h2v-2H7Zm4 0v2h6v-2h-6Zm-4 5v2h2v-2H7Zm4 0v2h6v-2h-6Z"/></svg>
          </span>
          <span class="home-view-tab__copy">
            <strong>Planillas de puntaje</strong>
            <small>Entrar al listado de planillas del curso</small>
          </span>
        </a>
      </div>
    </section>

    <footer class="footer">
      <hr>
      <p>Colegio T&eacute;cnico Nacional</p>
      <p><a href="${pageContext.request.contextPath}/privacidad">Pol&iacute;tica de privacidad</a> | <a href="${pageContext.request.contextPath}/terminos">T&eacute;rminos de servicio</a></p>
    </footer>
  </main>

  <script src="${pageContext.request.contextPath}/vendor/flat-ui/js/vendor/jquery.min.js"></script>
  <script src="${pageContext.request.contextPath}/vendor/flat-ui/js/flat-ui.js"></script>
  <script src="${pageContext.request.contextPath}/scripts/sca-theme.js?v=${assetVersion}"></script>
  <script>
    if ('serviceWorker' in navigator) {
      window.addEventListener('load', () => {
        navigator.serviceWorker.register('${pageContext.request.contextPath}/sw.js?v=${assetVersion}').catch(console.error);
      });
    }
  </script>
</body>
</html>
