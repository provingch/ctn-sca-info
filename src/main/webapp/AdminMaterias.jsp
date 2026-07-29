<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html data-theme="light">
<head>
        <title>Admin - Materias</title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <link rel="manifest" href="${pageContext.request.contextPath}/manifest.jsp">
        <meta name="theme-color" content="#1f2d3d">
        <meta name="apple-mobile-web-app-capable" content="yes">
        <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
        <meta name="apple-mobile-web-app-title" content="SCA">
        <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/icons/pwa/apple-touch-icon.png">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/vendor/flat-ui/css/flat-ui.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/ctn-theme.css?v=247">
        <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/images/ctn-logo.svg" />
</head>
<body class="admin-page" data-user-level="${sessionScope.user.level}" data-specialty="${empty sessionScope.scaSpecialty ? 'informatica' : sessionScope.scaSpecialty}" data-specialty-source="session">
<c:url var="profileUrl" value="/ProfileServlet" />
<header class="navbar navbar-default navbar-fixed-top ctn-navbar" role="navigation">
    <div class="container-fluid">
        <div class="navbar-header">
            <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#ctnNavbarMenu" aria-expanded="false">
                <span class="sr-only">Abrir navegación</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand ctn-navbar-brand" href="${pageContext.request.contextPath}/AdminServlet" aria-label="Ir a inicio">
                <img class="header-logo" src="${pageContext.request.contextPath}/images/ctn-logo.svg" alt="CTN">
                <span>Colegio Técnico Nacional</span>
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
                        <li><a role="menuitem" class="session-logout" href="${pageContext.request.contextPath}/LogoutServlet">Cerrar Sesión</a></li>
                    </ul>
                </li>
            </ul>
        </div>
    </div>
</header>
<main>
    <section class="container page-shell">
        <div class="admin-hero">
            <div>
                <span class="admin-eyebrow">Administración</span>
                <h1>Materias</h1>
                <p>Gestiona catálogo, categorías, especialidades y merges.</p>
            </div>
            <a class="btn-secondary" href="${pageContext.request.contextPath}/AdminServlet">Volver al panel</a>
        </div>

<c:if test="${not empty errors}">
    <div class="errors">
        <ul>
            <c:forEach var="e" items="${errors}">
                <li>${e}</li>
            </c:forEach>
        </ul>
    </div>
</c:if>

<c:if test="${not empty flashMessage}">
    <div class="flash">${flashMessage}</div>
</c:if>

<form class="admin-card admin-form" method="post" action="AdminMateriasServlet">
    <div class="admin-card-header"><h2>Crear nueva materia</h2></div>
    <div class="admin-form-grid">
    <input type="hidden" name="action" value="create" />
    <label>Nombre:</label>
    <input type="text" name="nombre" required />
    <label>Categoría:</label>
    <select name="categoria">
        <option value="comun">comun</option>
        <option value="especifico">especifico</option>
    </select>
    <div class="admin-field-wide">
        <label>Especialidades:</label>
        <div class="admin-check-grid">
        <c:forEach var="e" items="${especialidades}">
            <label><input type="checkbox" name="especialidades" value="${e.id}" /> ${e.nombre}</label>
        </c:forEach>
        </div>
    </div>
    </div>
    <button class="btn-primary admin-submit" type="submit">Crear</button>
</form>

<form class="admin-card admin-form" method="post" action="AdminMateriasServlet">
    <div class="admin-card-header"><h2>Comprobar merge</h2></div>
    <div class="admin-form-grid">
    <input type="hidden" name="action" value="check" />
    <label>From (source) materia:</label>
    <select name="fromId" required>
        <option value="">Seleccionar</option>
        <c:forEach var="m" items="${materias}">
            <option value="${m.id}" ${m.id == fromId ? 'selected' : ''}>${m.id} - ${m.nombre} (${m.categoria})</option>
        </c:forEach>
    </select>

    <label>To (target) materia:</label>
    <select name="toId" required>
        <option value="">Seleccionar</option>
        <c:forEach var="m" items="${materias}">
            <option value="${m.id}" ${m.id == toId ? 'selected' : ''}>${m.id} - ${m.nombre} (${m.categoria})</option>
        </c:forEach>
    </select>

    </div>
    <button class="btn-secondary admin-submit" type="submit">Comprobar conflictos</button>
</form>

<c:if test="${not empty conflicts}">
    <div class="admin-card admin-card-warning">
    <div class="admin-card-header"><h2>Conflictos detectados</h2></div>
    <ul class="admin-list">
        <c:forEach var="c" items="${conflicts}">
            <li>${c}</li>
        </c:forEach>
    </ul>
    </div>
</c:if>

<c:if test="${empty conflicts && not empty fromId && not empty toId}">
    <form class="admin-card admin-form" method="post" action="AdminMateriasServlet">
        <div class="admin-card-header"><h2>Confirmar merge</h2></div>
        <input type="hidden" name="action" value="merge" />
        <input type="hidden" name="fromId" value="${fromId}" />
        <input type="hidden" name="toId" value="${toId}" />
        <p class="admin-note">No se detectaron conflictos. Puede confirmar el merge.</p>
        <button class="btn-primary admin-submit" type="submit">Confirmar merge</button>
    </form>
</c:if>

<c:if test="${editMode and not empty editMateria}">
    <form class="admin-card admin-form" method="post" action="${pageContext.request.contextPath}/AdminMateriasServlet">
        <div class="admin-card-header"><h2>Editar materia</h2></div>
        <div class="admin-form-grid">
        <input type="hidden" name="action" value="edit" />
        <input type="hidden" name="materiaId" value="${editMateria.id}" />
        <label>Nombre:</label>
        <input type="text" value="${editMateria.nombre}" disabled />
        <label>Categoría:</label>
        <select name="categoria">
            <option value="">No cambiar</option>
            <option value="comun" ${editMateria.categoria eq 'comun' ? 'selected' : ''}>comun</option>
            <option value="especifico" ${editMateria.categoria eq 'especifico' ? 'selected' : ''}>especifico</option>
        </select>
        <div class="admin-field-wide">
            <label>Especialidades:</label>
            <div class="admin-check-grid">
            <c:forEach var="e" items="${especialidades}">
                <c:set var="isChecked" value="false" />
                <c:forEach var="selectedId" items="${editEspecialidadIds}">
                    <c:if test="${selectedId == e.id}">
                        <c:set var="isChecked" value="true" />
                    </c:if>
                </c:forEach>
                <label><input type="checkbox" name="especialidades" value="${e.id}" ${isChecked ? 'checked' : ''} /> ${e.nombre}</label>
            </c:forEach>
            </div>
        </div>
        </div>
        <button class="btn-primary admin-submit" type="submit">Guardar cambios</button>
    </form>
</c:if>

<div class="admin-card admin-table-card">
<div class="admin-card-header"><h2>Catálogo</h2></div>
<div class="admin-table-wrap">
<table class="admin-table">
    <thead><tr><th>ID</th><th>Nombre</th><th>Categoria</th><th>Especialidades</th><th>Profesores</th><th>Acciones</th></tr></thead>
    <tbody>
    <c:forEach var="m" items="${materias}">
        <tr>
            <td>${m.id}</td>
            <td>${m.nombre}</td>
            <td>${m.categoria}</td>
            <td>
                <c:choose>
                    <c:when test="${not empty materiaEspecialidadesTexto[m.id]}">
                        <c:out value="${materiaEspecialidadesTexto[m.id]}" />
                    </c:when>
                    <c:otherwise>—</c:otherwise>
                </c:choose>
            </td>
            <td><c:out value="${profCounts[m.id] != null ? profCounts[m.id] : 0}" /></td>
            <td>
                <a class="btn-secondary btn-compact" href="${pageContext.request.contextPath}/AdminMateriasServlet?editId=${m.id}">Editar</a>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>
</div>
</div>
    </section>
    <footer class="footer">
      <hr>
      <p>Colegio Técnico Nacional</p>
    </footer>
</main>
  <script src="${pageContext.request.contextPath}/vendor/flat-ui/js/vendor/jquery.min.js"></script>
  <script src="${pageContext.request.contextPath}/vendor/flat-ui/js/flat-ui.js"></script>
  <script src="${pageContext.request.contextPath}/scripts/sca-theme.js?v=170"></script>
  <script>
    if ('serviceWorker' in navigator) {
      window.addEventListener('load', () => {
        navigator.serviceWorker.register('${pageContext.request.contextPath}/sw.js').catch(console.error);
      });
    }
  </script>
</body>
</html>
