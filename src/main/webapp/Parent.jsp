<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html data-theme="light">
<head>
  <title>SCA - Padres</title>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="manifest" href="${pageContext.request.contextPath}/manifest.jsp"><meta name="theme-color" content="#1f2d3d">
  <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/icons/pwa/apple-touch-icon.png">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/vendor/flat-ui/css/flat-ui.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/ctn-theme.css?v=252">
  <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/images/ctn-logo.svg">
</head>
<body class="parent-portal" data-user-level="${sessionScope.user.level}" data-specialty="general" data-specialty-source="parent-portal">
<header class="navbar navbar-default navbar-fixed-top ctn-navbar" role="navigation"><div class="container-fluid"><div class="navbar-header">
  <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#ctnNavbarMenu" aria-expanded="false"><span class="sr-only">Abrir navegación</span><span class="icon-bar"></span><span class="icon-bar"></span><span class="icon-bar"></span></button>
  <a class="navbar-brand ctn-navbar-brand" href="${pageContext.request.contextPath}/padre" aria-label="Ir a inicio"><img class="header-logo" src="${pageContext.request.contextPath}/images/ctn-logo.svg" alt="CTN"><span>Colegio Técnico Nacional</span></a>
</div><div class="collapse navbar-collapse" id="ctnNavbarMenu"><ul class="nav navbar-nav navbar-right ctn-navbar-actions">
  <li class="ctn-theme-item"></li><li><a class="manual-link" href="${pageContext.request.contextPath}/pdfs/manual-padres.pdf" target="_blank" rel="noopener noreferrer">Manual</a></li>
  <li class="dropdown"><a href="#" id="sessionButton" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">Sesión <span class="caret"></span></a><ul class="dropdown-menu" id="sessionMenu" role="menu" aria-labelledby="sessionButton"><li><a role="menuitem" href="${pageContext.request.contextPath}/perfil">Mi perfil</a></li><li><a role="menuitem" href="${pageContext.request.contextPath}/logout">Cerrar sesión</a></li></ul></li>
</ul></div></div></header>
<main><section class="container page-shell">
  <div class="info-bar"><span>Bienvenido/a ${sessionScope.user.fullName}</span></div>
  <div class="top-section planilla-hero hero-shell parent-hero" data-specialty="${multipleHijos ? 'general' : selectedEspecialidad}"><div class="planilla-hero__header"><div class="planilla-hero__info"><span class="badge"><span class="dot"></span>Padres</span><h1>Notas de mis hijos</h1><p class="planilla-subtitle">Consultá el rendimiento y las tareas de cada alumno.</p></div></div></div>
  <c:if test="${empty hijos}"><div class="empty-state empty-state-card">No hay hijos asociados a este usuario padre.</div></c:if>
  <c:if test="${not empty hijos}">
    <c:if test="${multipleHijos}"><section class="parent-child-selector" aria-label="Seleccionar alumno"><div class="parent-child-selector__heading"><h2>Mis hijos</h2></div><div class="parent-child-grid"><c:forEach var="hijo" items="${hijos}"><c:url var="childUrl" value="/padre"><c:param name="alumnoId" value="${hijo.id}" /></c:url><a class="parent-child-card ${hijo.id == selectedAlumnoId ? 'is-active' : ''}" href="${childUrl}" data-specialty="${hijo.especialidadNombre}"><span class="parent-child-card__accent"></span><span class="parent-child-card__content"><span class="parent-child-card__specialty"><span class="parent-specialty-dot"></span><c:out value="${hijo.especialidadNombre}" /></span><span class="parent-child-card__name"><c:out value="${hijo.nombre}" /> <c:out value="${hijo.apellido}" /></span><span class="parent-child-card__average">Promedio general: <strong><c:out value="${promedioPorAlumno[hijo.id]}" />%</strong></span></span></a></c:forEach></div></section></c:if>
    <section class="section-block card parent-summary-card" data-specialty="${selectedEspecialidad}"><div class="parent-summary-card__heading"><div class="section-heading">Materias de <c:forEach var="hijo" items="${hijos}"><c:if test="${hijo.id == selectedAlumnoId}"><c:out value="${hijo.nombre}" /></c:if></c:forEach></div></div>
      <c:choose><c:when test="${not empty selectedSummary}"><div class="parent-subject-list"><c:forEach var="item" items="${selectedSummary}"><details class="parent-subject-accordion" ${item.planillaId == selectedPlanillaId ? 'open' : ''}><summary><span class="parent-subject-accordion__title"><strong><c:out value="${item.materiaNombre}" /></strong></span><span class="parent-subject-accordion__metric"><small>Tareas</small><strong><c:out value="${item.tareasCount}" /></strong></span><span class="parent-subject-accordion__metric"><small>Puntos</small><strong><c:out value="${item.puntos}" /> / <c:out value="${item.totalPosible}" /></strong></span><span class="parent-subject-accordion__metric"><small>Nota</small><strong><c:out value="${item.nota}" /></strong></span><span class="parent-subject-accordion__metric"><small>Porcentaje</small><strong><c:out value="${item.porcentaje}" />%</strong></span></summary><c:choose><c:when test="${not empty tareasPorPlanilla[item.planillaId]}"><div class="parent-task-table-wrap"><table class="grade-table parent-task-table"><thead><tr><th>Tarea</th><th>Fecha</th><th>Puntos</th></tr></thead><tbody><c:forEach var="grade" items="${tareasPorPlanilla[item.planillaId]}"><tr><td><c:out value="${grade.tareaTitulo}" /></td><td><c:out value="${grade.fecha}" /></td><td><strong><c:out value="${grade.puntos}" /> / <c:out value="${grade.total}" /></strong></td></tr></c:forEach></tbody></table></div></c:when><c:otherwise><div class="parent-task-empty">Esta materia todavía no tiene tareas cargadas.</div></c:otherwise></c:choose></details></c:forEach></div></c:when><c:otherwise><div class="empty-state">No hay materias con tareas cargadas para este alumno.</div></c:otherwise></c:choose>
    </section>
  </c:if>
</section><footer class="footer"><hr><p>Colegio Técnico Nacional</p><p><a href="${pageContext.request.contextPath}/privacidad">Política de privacidad</a> | <a href="${pageContext.request.contextPath}/terminos">Términos de servicio</a></p></footer></main>
<script src="${pageContext.request.contextPath}/vendor/flat-ui/js/vendor/jquery.min.js"></script><script src="${pageContext.request.contextPath}/vendor/flat-ui/js/flat-ui.js"></script><script src="${pageContext.request.contextPath}/scripts/sca-theme.js?v=170"></script>
<script>if ('serviceWorker' in navigator) { window.addEventListener('load', () => navigator.serviceWorker.register('${pageContext.request.contextPath}/sw.js')); }</script>
</body></html>
