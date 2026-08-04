<%@ page contentType="text/html" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<c:set var="pageTitle" value="Ingresantes" scope="request" />
<c:set var="headExtraFragment" value="/WEB-INF/includes/head-style-adminingresantes.jspf" scope="request" />
<%@ include file="/WEB-INF/includes/head.jspf" %>
<body class="admin-page" data-page="admin-ingresantes" data-user-level="${sessionScope.user.level}" data-specialty="general" data-specialty-source="system">
  <c:url var="backUrl" value="/admin" />
  <c:set var="navbarHomeUrl" value="${backUrl}" />
  <c:set var="navbarHomeAriaLabel" value="Ir al panel de administración" />
  <c:set var="manualHrefOverride" value="${pageContext.request.contextPath}/pdfs/manual-administrador.pdf" />
  <%@ include file="/WEB-INF/includes/navbar.jspf" %>
  <main>
    <section class="container page-shell">
      <div class="admin-hero">
        <div>
          <span class="admin-eyebrow">Administración</span>
          <h1>Carga de ingresantes</h1>
          <p>Alta de nuevos alumnos por curso/sección con una meta orientativa de ${targetCapacity} alumnos.</p>
        </div>
        <a class="btn-secondary" href="${backUrl}">Volver al panel</a>
      </div>

      <c:if test="${not empty sessionScope.flashMessage}">
        <div class="alert alert-success">${sessionScope.flashMessage}</div>
        <c:remove var="flashMessage" scope="session" />
      </c:if>
      <c:if test="${not empty sessionScope.errors}">
        <div class="alert alert-danger">${sessionScope.errors[0]}</div>
        <c:remove var="errors" scope="session" />
      </c:if>
      <c:if test="${not empty sessionScope.warnings}">
        <div class="alert alert-warning">${sessionScope.warnings[0]}</div>
        <c:remove var="warnings" scope="session" />
      </c:if>

      <div class="capacity-groups">
        <c:forEach var="especialidadGroup" items="${cursosAgrupados}">
          <section class="capacity-specialty" data-specialty="${specialtyTokenByName[especialidadGroup.key]}">
            <h2 class="capacity-specialty__header">
              <c:out value="${especialidadGroup.key}" />
            </h2>
            <c:forEach var="cursoGroup" items="${especialidadGroup.value}">
              <div class="capacity-course-group">
                <h3 class="capacity-course-group__title"><c:out value="${cursoGroup.key}" /> curso</h3>
                <div class="capacity-section-grid sections-${fn:length(cursoGroup.value) >= 3 ? '3' : fn:length(cursoGroup.value)}">
                  <c:forEach var="curso" items="${cursoGroup.value}">
                    <c:set var="status" value="${statusByCurso[curso.id]}" />
                    <article class="capacity-card ${status}">
                      <div class="capacity-v">
                        <c:out value="${curso.especialidad}" /> ·
                        <c:out value="${curso.cursoOrdinal}" /> ·
                        Sección <c:out value="${curso.seccion}" />
                      </div>
                      <div><strong><c:out value="${countsByCurso[curso.id]}" /></strong> alumnos cargados</div>
                      <div class="muted"><c:out value="${messageByCurso[curso.id]}" /></div>
                    </article>
                  </c:forEach>
                </div>
              </div>
            </c:forEach>
          </section>
        </c:forEach>
      </div>

      <form class="admin-card admin-form" method="post" action="${pageContext.request.contextPath}/admin/ingresantes">
        <input type="hidden" name="action" value="crear" />
        <div class="form-group">
          <label for="nombre">Nombre</label>
          <input id="nombre" name="nombre" required class="form-control" />
        </div>
        <div class="form-group">
          <label for="apellido">Apellido</label>
          <input id="apellido" name="apellido" required class="form-control" />
        </div>
        <div class="form-group">
          <label for="ci">CI</label>
          <input id="ci" name="ci" class="form-control" />
        </div>
        <div class="form-group">
          <label for="cursoId">Curso / Sección</label>
          <select id="cursoId" name="cursoId" class="form-control" required>
            <option value="">Seleccionar</option>
            <c:forEach var="curso" items="${cursos}">
              <option value="${curso.id}"><c:out value="${curso.especialidad}" /> · <c:out value="${curso.cursoOrdinal}" /> · Sección <c:out value="${curso.seccion}" /></option>
            </c:forEach>
          </select>
        </div>
        <div class="form-group">
          <label for="correoEncargado">Correo del encargado</label>
          <input id="correoEncargado" name="correoEncargado" class="form-control" />
        </div>
        <div class="form-group">
          <label for="correoEncargado2">Correo alternativo</label>
          <input id="correoEncargado2" name="correoEncargado2" class="form-control" />
        </div>
        <button class="btn btn-primary" type="submit">Crear alumno</button>
      </form>

      <div class="admin-card">
        <h3>Alumnos existentes</h3>
        <p class="muted">Podés editar nombre, apellido, CI, curso y correos directamente desde aquí.</p>
        <div class="filter-row">
          <div class="form-group">
            <label for="studentSearch">Buscar por nombre o apellido</label>
            <input id="studentSearch" class="form-control" placeholder="Ej. Ana" />
          </div>
          <div class="form-group">
            <label for="courseFilter">Filtrar por curso</label>
            <select id="courseFilter" class="form-control">
              <option value="">Todos los cursos</option>
              <c:forEach var="curso" items="${cursos}">
                <option value="${curso.id}"><c:out value="${curso.especialidad}" /> · <c:out value="${curso.cursoOrdinal}" /> · Sección <c:out value="${curso.seccion}" /></option>
              </c:forEach>
            </select>
          </div>
        </div>
        <div class="capacity-grid" id="studentList">
          <c:forEach var="alumno" items="${alumnos}">
            <form class="capacity-card student-card" data-name="<c:out value='${alumno.apellido} ${alumno.nombre}' />" data-course="${alumno.cursoId}" method="post" action="${pageContext.request.contextPath}/admin/ingresantes">
              <input type="hidden" name="action" value="editar" />
              <input type="hidden" name="alumnoId" value="${alumno.id}" />
              <div class="capacity-v"><c:out value="${alumno.apellido}" />, <c:out value="${alumno.nombre}" /></div>
              <div class="form-group">
                <label>Nombre</label>
                <input name="nombre" class="form-control" value="<c:out value='${alumno.nombre}' />" required />
              </div>
              <div class="form-group">
                <label>Apellido</label>
                <input name="apellido" class="form-control" value="<c:out value='${alumno.apellido}' />" required />
              </div>
              <div class="form-group">
                <label>CI</label>
                <input name="ci" class="form-control" value="${alumno.ci}" />
              </div>
              <div class="form-group">
                <label>Curso / Sección</label>
                <select name="cursoId" class="form-control" required>
                  <c:forEach var="curso" items="${cursos}">
                    <option value="${curso.id}" ${curso.id == alumno.cursoId ? 'selected' : ''}><c:out value="${curso.especialidad}" /> · <c:out value="${curso.cursoOrdinal}" /> · Sección <c:out value="${curso.seccion}" /></option>
                  </c:forEach>
                </select>
              </div>
              <div class="form-group">
                <label>Correo del encargado</label>
                <input name="correoEncargado" class="form-control" value="<c:out value='${alumno.correoEncargado}' />" />
              </div>
              <div class="form-group">
                <label>Correo alternativo</label>
                <input name="correoEncargado2" class="form-control" value="<c:out value='${alumno.correoEncargado2}' />" />
              </div>
              <button class="btn btn-primary" type="submit">Guardar</button>
            </form>
          </c:forEach>
        </div>
      </div>
    </section>
    <footer class="footer">
      <hr>
          <p>Colegio T&eacute;cnico Nacional</p>
    <p><a href="${pageContext.request.contextPath}/privacidad">Pol&iacute;tica de privacidad</a> | <a href="${pageContext.request.contextPath}/terminos">T&eacute;rminos de servicio</a></p>
    </footer>
  </main>
  <%@ include file="/WEB-INF/includes/footer-scripts.jspf" %>
  <script>
    const studentSearch = document.getElementById('studentSearch');
    const courseFilter = document.getElementById('courseFilter');
    const studentCards = Array.from(document.querySelectorAll('.student-card'));

    function applyStudentFilters() {
      const query = (studentSearch?.value || '').trim().toLowerCase();
      const courseValue = courseFilter?.value || '';

      studentCards.forEach(card => {
        const name = (card.dataset.name || '').toLowerCase();
        const course = card.dataset.course || '';
        const matchesQuery = !query || name.includes(query);
        const matchesCourse = !courseValue || course === courseValue;
        card.classList.toggle('hidden', !(matchesQuery && matchesCourse));
      });
    }

    studentSearch?.addEventListener('input', applyStudentFilters);
    courseFilter?.addEventListener('change', applyStudentFilters);
  </script>
</body>
</html>
