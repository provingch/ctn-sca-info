<%-- 
    Document   : Admin
    Created on : Oct 2, 2025, 6:45:45 AM
    Author     : jonat
--%>

<%@page import="java.time.format.DateTimeFormatter"%>
<%@page import="java.time.LocalDateTime"%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle" value="Evaluación" scope="request" />
<%@ include file="/WEB-INF/includes/head.jspf" %>

<body data-page="evaluacion" data-user-level="${sessionScope.user.level}" data-specialty="${empty sessionScope.scaSpecialty ? 'general' : sessionScope.scaSpecialty}" data-specialty-source="session">
  <%@ include file="/WEB-INF/includes/navbar.jspf" %>

  <main>
    <section class="container page-shell">
      <div class="info-bar">
        <span>Bienvenido/a ${sessionScope.user.fullName}</span>
        <span>
          <c:out value="${nowFormatted}" />
        </span>
      </div>
      <div class="top-section planilla-hero hero-shell">
        <div class="planilla-hero__header">
          <div class="planilla-hero__info">
            <span class="badge"><span class="dot"></span>Evaluación</span>
            <h1>Descargar planillas</h1>
            <p class="planilla-subtitle">Exporta informes por especialidad, curso y periodo.</p>
          </div>
        </div>
      </div>

      <c:url var="exportUrl" value="/planilla/export-course" />

      <form id="exportCourseForm" action="${exportUrl}" method="get">
        <div class="table-card card tareas-grid">
<!--          <div class="table-header">Etapa</div>
          <div class="cell">
            <select name="etapa" required>
              <option value="" selected disabled>Seleccione una etapa</option>
              <option value="primera">Primera etapa</option>
              <option value="segunda">Segunda etapa</option>
            </select>
          </div>-->

          <div class="table-header">Especialidad</div>
          <div class="cell">
            <select name="especialidad" data-tab-specialty-key="scaEvaluacionSpecialty" required>
              <option value="" ${empty selEspecialidad ? 'selected' : ''}>Seleccione una especialidad</option>
              <c:forEach var="e" items="${especialidades}">
                <option value="${e.id}"
                    <c:if test="${not empty selEspecialidad and e.id == selEspecialidad.id}">selected</c:if>>
                  <c:out value="${e}" />
                </option>
              </c:forEach>
            </select>
          </div>

          <div class="table-header">Curso</div>
          <div class="cell">
            <select name="curso" id="curso-select" required>
              <option value="" selected disabled>Seleccione un curso</option>
              <option value="1">1º</option>
              <option value="2">2º</option>
              <option value="3">3º</option>
            </select>
          </div>

          <div class="table-header">Sección</div>
          <div class="cell">
            <select name="seccion" id="seccion-select" required>
              <option value="" selected disabled>Seleccione una sección</option>
              <option value="A">A</option>
              <option value="B">B</option>
              <option value="C">C</option>
            </select>
          </div>

          <div class="table-header">Periodo</div>
          <div class="cell">
            <!-- required: admin must enter periodo (used to compute promocion) -->
            <input type="number" name="periodo" id="periodo-input" placeholder="2025" min="2000" required />
          </div>

          <div class="buttons-row table-header">
            <c:url var="backUrl" value="/planilla">
              <c:param name="planillaId" value="${planillaId}" />
            </c:url>

            <button type="submit" id="downloadCourseBtn" class="btn-primary" title="Descargar planillas del curso">
              <img class="download-icon" src="${pageContext.request.contextPath}/icons/download-icon.svg" alt="Descargar">
              Descargar
            </button>
          </div>
        </div>
      </form>

      <c:if test="${not empty clasesRegistradas}">
        <div class="table-card card" style="margin-top:16px;">
          <div class="table-header">Clases registradas</div>
          <div class="cell" style="padding:12px 0 0;">
            <table class="table table-striped">
              <thead>
                <tr>
                  <th>Tema</th>
                  <th>Fecha</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="clase" items="${clasesRegistradas}">
                  <tr>
                    <td><c:out value="${clase.tema}" /></td>
                    <td><c:out value="${clase.fechaClase}" /></td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </div>
      </c:if>

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
