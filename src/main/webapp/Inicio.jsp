<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:set var="pageTitle" value="Inicio" scope="request" />
<%@ include file="/WEB-INF/includes/head.jspf" %>
<body data-user-level="${sessionScope.user.level}" data-specialty="${empty sessionScope.scaSpecialty ? 'general' : sessionScope.scaSpecialty}" data-specialty-source="session">
  <%@ include file="/WEB-INF/includes/navbar.jspf" %>

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

  <%@ include file="/WEB-INF/includes/footer-scripts.jspf" %>
</body>
</html>
