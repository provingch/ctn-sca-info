<%@ page contentType="text/html" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle" value="Administrador" scope="request" />
<%@ include file="/WEB-INF/includes/head.jspf" %>
<body class="admin-page" data-page="admin-home" data-user-level="${sessionScope.user.level}" data-specialty="general" data-specialty-source="system">
  <%@ include file="/WEB-INF/includes/navbar.jspf" %>
  <main>
    <section class="container page-shell">
      <div class="admin-hero">
        <div>
          <span class="admin-eyebrow">Administración</span>
          <h1>Panel general</h1>
          <p>Bienvenido/a ${sessionScope.user.fullName}</p>
        </div>
      </div>
      <div class="admin-metric-grid">
        <div class="admin-metric"><span>Profesores</span><strong><c:out value="${profesorCount}" /></strong></div>
        <div class="admin-metric"><span>Cursos</span><strong><c:out value="${cursoCount}" /></strong></div>
        <div class="admin-metric"><span>Especialidades</span><strong><c:out value="${especialidadCount}" /></strong></div>
      </div>
      <div class="admin-nav-grid">
        <a class="admin-nav-card" href="${pageContext.request.contextPath}/admin/materias"><strong>Materias</strong><span>Catálogo, categorías y merges</span></a>
        <a class="admin-nav-card" href="${pageContext.request.contextPath}/admin/usuarios"><strong>Usuarios</strong><span>Altas, roles y contraseñas</span></a>
        <a class="admin-nav-card" href="${pageContext.request.contextPath}/admin/asignaciones"><strong>Asignaciones</strong><span>Profesor, materia y curso</span></a>
        <a class="admin-nav-card" href="${pageContext.request.contextPath}/admin/ingresantes"><strong>Ingresantes</strong><span>Carga de nuevos alumnos con meta de 28</span></a>
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
