<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html data-theme="light">
<head>
    <title>Admin - Usuarios</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <link rel="manifest" href="${pageContext.request.contextPath}/manifest.jsp">
    <meta name="theme-color" content="#1f2d3d">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
    <meta name="apple-mobile-web-app-title" content="SCA">
    <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/icons/pwa/apple-touch-icon.png">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/vendor/flat-ui/css/flat-ui.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/ctn-theme.css?v=250">
    <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/images/ctn-logo.svg" />
</head>
<body class="admin-page" data-page="admin-usuarios" data-user-level="${sessionScope.user.level}" data-specialty="general" data-specialty-source="system">
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
                <h1>Usuarios</h1>
                <p>Gestiona altas, roles, especialidades y accesos.</p>
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

<form class="admin-card admin-form" method="post" action="AdminUsuariosServlet">
    <div class="admin-card-header">
        <h2>Crear usuario</h2>
    </div>
    <div class="admin-form-grid">
    <input type="hidden" name="action" value="create" />
    <label>Nombre</label>
    <input type="text" name="nombre" required />
    <label>Apellido</label>
    <input type="text" name="apellido" required />
    <label>Usuario</label>
    <input type="text" name="usuario" required />
    <label>Contraseña</label>
    <input type="password" name="contrasenia" placeholder="password por defecto si está vacío" />
    <label>Rol</label>
    <select name="nivel" required>
        <option value="1">Profesor</option>
        <option value="2">Evaluador</option>
        <option value="3">Administrador</option>
    </select>
    <label>CI</label>
    <input type="text" name="ci" />
    <label>Teléfono</label>
    <input type="text" name="telefono" />
    <label>Celular</label>
    <input type="text" name="celular" />
    <label>Correo</label>
    <input type="email" name="correo" />
    <label>Especialidad</label>
    <select name="especialidadId">
        <option value="">Ninguna</option>
        <c:forEach var="e" items="${especialidades}">
            <option value="${e.id}">${e.nombre}</option>
        </c:forEach>
    </select>
    </div>
    <button class="btn-primary admin-submit" type="submit">Crear usuario</button>
</form>

<c:if test="${editMode and not empty editProfesor}">
    <form class="admin-card admin-form" method="post" action="${pageContext.request.contextPath}/AdminUsuariosServlet">
        <div class="admin-card-header">
            <h2>Editar usuario</h2>
        </div>
        <div class="admin-form-grid">
        <input type="hidden" name="action" value="edit" />
        <input type="hidden" name="profesorId" value="${editProfesor.id}" />
        <label>Nombre</label>
        <input type="text" name="nombre" value="${editProfesor.nombre}" required />
        <label>Apellido</label>
        <input type="text" name="apellido" value="${editProfesor.apellido}" required />
        <label>Usuario</label>
        <input type="text" name="usuario" value="${editProfesor.usuario}" required />
        <label>Contraseña nueva</label>
        <input type="password" name="contrasenia" placeholder="Dejar en blanco para no cambiar" />
        <label>Rol</label>
        <select name="nivel" required>
            <option value="1" ${editProfesor.nivel == 1 ? 'selected' : ''}>Profesor</option>
            <option value="2" ${editProfesor.nivel == 2 ? 'selected' : ''}>Evaluador</option>
            <option value="3" ${editProfesor.nivel == 3 ? 'selected' : ''}>Administrador</option>
        </select>
        <label>CI</label>
        <input type="text" name="ci" value="${editProfesor.ci}" />
        <label>Teléfono</label>
        <input type="text" name="telefono" value="${editProfesor.telefono}" />
        <label>Celular</label>
        <input type="text" name="celular" value="${editProfesor.celular}" />
        <label>Correo</label>
        <input type="email" name="correo" value="${editProfesor.correo}" />
        <label>Especialidad</label>
        <select name="especialidadId">
            <option value="">Ninguna</option>
            <c:forEach var="e" items="${especialidades}">
                <option value="${e.id}" ${editProfesor.especialidadId == e.id ? 'selected' : ''}>${e.nombre}</option>
            </c:forEach>
        </select>
        </div>
        <button class="btn-primary admin-submit" type="submit">Guardar cambios</button>
    </form>
</c:if>

<div class="admin-card admin-table-card">
<div class="admin-card-header">
    <h2>Usuarios</h2>
</div>
<div class="admin-table-wrap">
<table class="admin-table">
    <thead>
        <tr>
            <th>ID</th>
            <th>Nombre</th>
            <th>Usuario</th>
            <th>Rol</th>
            <th>Especialidad</th>
            <th>Acciones</th>
        </tr>
    </thead>
    <tbody>
        <c:forEach var="profesor" items="${profesores}">
            <tr>
                <td>${profesor.id}</td>
                <td>${profesor.fullName}</td>
                <td>${profesor.usuario}</td>
                <td>
                    <c:choose>
                        <c:when test="${profesor.nivel == 1}">Profesor</c:when>
                        <c:when test="${profesor.nivel == 2}">Evaluador</c:when>
                        <c:when test="${profesor.nivel == 3}">Administrador</c:when>
                        <c:otherwise>Desconocido</c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${not empty profesor.especialidadId}">
                            <c:forEach var="e" items="${especialidades}">
                                <c:if test="${e.id == profesor.especialidadId}">${e.nombre}</c:if>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>--</c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <div class="admin-row-actions">
                    <a class="btn-secondary btn-compact" href="${pageContext.request.contextPath}/AdminUsuariosServlet?editId=${profesor.id}">Editar</a>
                    <form class="admin-inline-form" method="post" action="${pageContext.request.contextPath}/AdminUsuariosServlet">
                        <input type="hidden" name="action" value="delete" />
                        <input type="hidden" name="profesorId" value="${profesor.id}" />
                        <button class="btn-danger btn-compact" type="submit" onclick="return confirm('Eliminar usuario ${profesor.fullName}?');">Eliminar</button>
                    </form>
                    <form class="admin-inline-form" method="post" action="${pageContext.request.contextPath}/AdminUsuariosServlet">
                        <input type="hidden" name="action" value="reset" />
                        <input type="hidden" name="profesorId" value="${profesor.id}" />
                        <button class="btn-secondary btn-compact" type="submit">Restablecer</button>
                    </form>
                    </div>
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
