<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html data-theme="light">
<head>
    <title>SCA - Padres</title>
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
<body data-user-level="${sessionScope.user.level}" data-specialty="${empty sessionScope.scaSpecialty ? 'informatica' : sessionScope.scaSpecialty}" data-specialty-source="session">
<header class="navbar navbar-default navbar-fixed-top ctn-navbar" role="navigation">
    <div class="container-fluid">
      <div class="navbar-header">
        <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#ctnNavbarMenu" aria-expanded="false">
          <span class="sr-only">Abrir navegación</span>
          <span class="icon-bar"></span>
          <span class="icon-bar"></span>
          <span class="icon-bar"></span>
        </button>
        <a class="navbar-brand ctn-navbar-brand" href="${pageContext.request.contextPath}/ParentServlet" aria-label="Ir a inicio">
          <img class="header-logo" src="${pageContext.request.contextPath}/images/ctn-logo.svg" alt="CTN">
          <span>Colegio Técnico Nacional</span>
        </a>
      </div>
      <div class="collapse navbar-collapse" id="ctnNavbarMenu">
        <ul class="nav navbar-nav navbar-right ctn-navbar-actions">
          <li class="ctn-theme-item"></li>
          <li class="dropdown">
            <a href="#" id="sessionButton" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">Sesión <span class="caret"></span></a>
            <ul class="dropdown-menu" id="sessionMenu" role="menu" aria-labelledby="sessionButton">
              <li><a role="menuitem" href="${pageContext.request.contextPath}/ProfileServlet">Mi Perfil</a></li>
              <li><a role="menuitem" href="${pageContext.request.contextPath}/LogoutServlet">Cerrar Sesión</a></li>
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
        </div>

        <div class="top-section planilla-hero hero-shell">
            <div class="planilla-hero__header">
                <div class="planilla-hero__info">
                    <span class="badge"><span class="dot"></span>Padres</span>
                    <h1>Notas de mis hijos</h1>
                    <p class="planilla-subtitle">Resumen de materias, notas y tareas por alumno.</p>
                </div>
            </div>
        </div>

        <c:if test="${empty hijos}">
            <div class="empty-state empty-state-card">No hay hijos asociados a este usuario padre.</div>
        </c:if>

        <c:if test="${not empty hijos}">
            <c:forEach var="entry" items="${summaryByEspecialidad}">
                <div class="section-block card specialty-scope" data-specialty="${entry.key}">
                    <div class="section-heading"><c:out value="${entry.key}" /></div>
                    <table class="grade-table">
                        <thead>
                        <tr>
                            <th>Alumno</th>
                            <th>Materia</th>
                            <th>Puntos</th>
                            <th>Nota</th>
                            <th>Porcentaje</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="item" items="${entry.value}">
                            <c:url var="detailUrl" value="/ParentServlet">
                                <c:param name="alumnoId" value="${item.alumnoId}" />
                                <c:param name="materiaId" value="${item.materiaId}" />
                                <c:param name="planillaId" value="${item.planillaId}" />
                            </c:url>
                            <tr>
                                <td><a href="${detailUrl}"><c:out value="${item.alumnoNombre}" /></a></td>
                                <td><a href="${detailUrl}"><c:out value="${item.materiaNombre}" /></a></td>
                                <td><c:out value="${item.puntos}" /> / <c:out value="${item.totalPosible}" /></td>
                                <td><c:out value="${item.nota}" /></td>
                                <td><c:out value="${item.porcentaje}" />%</td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:forEach>
        </c:if>

        <c:if test="${not empty tareasPorAlumno}">
            <div class="section-block card">
                <div class="section-heading">Detalle de tareas</div>
                <table class="grade-table">
                    <thead>
                    <tr>
                        <th>Tarea</th>
                        <th>Fecha</th>
                        <th>Puntos</th>
                        <th>Total</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="grade" items="${tareasPorAlumno}">
                        <tr>
                            <td><c:out value="${grade.tareaTitulo}" /></td>
                            <td><c:out value="${grade.fecha}" /></td>
                            <td><c:out value="${grade.puntos}" /></td>
                            <td><c:out value="${grade.total}" /></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:if>
    </section>

    <footer class="footer">
        <hr>
        <p>Colegio Técnico Nacional</p>
    </footer>
</main>
<script src="${pageContext.request.contextPath}/vendor/flat-ui/js/vendor/jquery.min.js"></script>
  <script src="${pageContext.request.contextPath}/vendor/flat-ui/js/flat-ui.js"></script>
  <script src="${pageContext.request.contextPath}/scripts/sca-theme.js?v=168"></script>
  <script>
    if ('serviceWorker' in navigator) {
      window.addEventListener('load', () => {
        navigator.serviceWorker.register('${pageContext.request.contextPath}/sw.js');
      });
    }
  </script>
</body>
</html>
