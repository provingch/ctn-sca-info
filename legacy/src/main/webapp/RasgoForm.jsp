<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:set var="pageTitle" value="Formulario de asistencia" scope="request" />
<%@ include file="/WEB-INF/includes/head.jspf" %>
<body data-user-level="${sessionScope.user.level}" data-specialty="${empty sessionScope.scaSpecialty ? 'general' : sessionScope.scaSpecialty}" data-specialty-source="session">
  <main class="container" style="padding-top:40px;max-width:760px;">
    <h1>Formulario de asistencia</h1>
    <p>La presencia o ausencia se define desde la lista inicial de clase. Aquí solo se agregan los rasgos conductuales (códigos N) y una anotación opcional.</p>

    <c:if test="${rasgoSubmitSuccess}">
      <div class="alert alert-success">Tu asistencia fue registrada correctamente.</div>
    </c:if>

    <c:if test="${not empty rasgoAsistencia}">
      <div class="panel panel-default" style="margin-bottom:16px;">
        <div class="panel-body">
          <p><strong>Tema:</strong> <c:out value="${rasgoAsistencia.tema}" /></p>
          <p><strong>Alumno:</strong> <c:out value="${rasgoAsistencia.alumnoNombreCompleto}" /></p>
          <p><strong>Correo:</strong> <c:out value="${rasgoAsistencia.alumnoEmail}" /></p>
          <p><strong>Estado actual:</strong> <c:out value="${rasgoAsistencia.estado}" /></p>
        </div>
      </div>

      <form action="${pageContext.request.contextPath}/inicio" method="post" class="panel panel-default">
        <div class="panel-body">
          <input type="hidden" name="action" value="assign-falta-codigo" />
          <input type="hidden" name="asistenciaId" value="${rasgoAsistencia.id}" />

          <div class="form-group">
            <label for="faltaCodigo">Código conductual (solo N)</label>
            <select id="faltaCodigo" name="faltaCodigo" class="form-control">
              <option value="">Sin código conductual</option>
              <option value="N1">N1 — Llegada tardía</option>
              <option value="N2">N2 — Sale del aula sin autorización</option>
              <option value="N3">N3 — No realiza la tarea asignada en clase</option>
              <option value="N4">N4 — No dispone de los materiales necesarios</option>
              <option value="N5">N5 — No presenta las tareas asignadas para la casa</option>
              <option value="N6">N6 — Utiliza vocabulario indebido en clase</option>
              <option value="N7">N7 — Charla mucho en clase</option>
              <option value="N8">N8 — No utiliza el uniforme establecido</option>
              <option value="N9">N9 — Ausente en clase, presente en la Institución</option>
            </select>
          </div>

          <div class="form-group">
            <label for="faltaObservacion">Anotación breve (opcional)</label>
            <textarea id="faltaObservacion" name="faltaObservacion" class="form-control" rows="3" placeholder="Detalle breve de la situación..."></textarea>
          </div>

          <button type="submit" class="btn btn-primary">Guardar rasgo conductual</button>
        </div>
      </form>
    </c:if>
  </main>
  <script src="${pageContext.request.contextPath}/scripts/sca-theme.js?v=${assetVersion}"></script>
</body>
</html>


