<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>SCA - Formulario de asistencia</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/vendor/flat-ui/css/flat-ui.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/ctn-theme.css?v=240">
</head>
<body data-user-level="${sessionScope.user.level}" data-specialty="${empty sessionScope.scaSpecialty ? 'informatica' : sessionScope.scaSpecialty}" data-specialty-source="session">
  <main class="container" style="padding-top:40px;max-width:760px;">
    <h1>Formulario de asistencia</h1>
    <p>Complete su respuesta de presencia o ausencia para la clase.</p>

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

      <form action="${pageContext.request.contextPath}/HomeServlet" method="post" class="panel panel-default">
        <div class="panel-body">
          <input type="hidden" name="action" value="submit-rasgo-asistencia" />
          <input type="hidden" name="asistenciaId" value="${rasgoAsistencia.id}" />

          <div class="form-group">
            <label style="display:block;">Selecciona tu estado</label>
            <label style="margin-right:16px;">
              <input type="radio" name="estado" value="presente" checked> Presente
            </label>
            <label>
              <input type="radio" name="estado" value="ausente"> Ausente
            </label>
          </div>

          <button type="submit" class="btn btn-primary">Enviar respuesta</button>
        </div>
      </form>
    </c:if>
  </main>
  <script src="${pageContext.request.contextPath}/scripts/sca-theme.js?v=166"></script>
</body>
</html>
