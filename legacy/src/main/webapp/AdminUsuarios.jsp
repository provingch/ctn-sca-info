<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Admin - Usuarios" scope="request" />
<%@ include file="/WEB-INF/includes/head.jspf" %>
<body class="admin-page" data-page="admin-usuarios" data-user-level="${sessionScope.user.level}" data-specialty="general" data-specialty-source="system">
<c:set var="navbarHomeUrl" value="${pageContext.request.contextPath}/admin" />
<%@ include file="/WEB-INF/includes/navbar.jspf" %>
<main>
    <section class="container page-shell">
        <div class="admin-hero">
            <div>
                <span class="admin-eyebrow">Administración</span>
                <h1>Usuarios</h1>
                <p>Gestiona altas, roles, especialidades y accesos.</p>
            </div>
            <a class="btn-secondary" href="${pageContext.request.contextPath}/admin">Volver al panel</a>
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

<form class="admin-card admin-form" method="post" action="${pageContext.request.contextPath}/admin/usuarios">
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
    <form class="admin-card admin-form" method="post" action="${pageContext.request.contextPath}/admin/usuarios">
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
                    <a class="btn-secondary btn-compact" href="${pageContext.request.contextPath}/admin/usuarios?editId=${profesor.id}">Editar</a>
                    <form class="admin-inline-form" method="post" action="${pageContext.request.contextPath}/admin/usuarios">
                        <input type="hidden" name="action" value="delete" />
                        <input type="hidden" name="profesorId" value="${profesor.id}" />
                        <button class="btn-danger btn-compact" type="submit" onclick="return confirm('Eliminar usuario ${profesor.fullName}?');">Eliminar</button>
                    </form>
                    <form class="admin-inline-form" method="post" action="${pageContext.request.contextPath}/admin/usuarios">
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
          <p>Colegio T&eacute;cnico Nacional</p>
    <p><a href="${pageContext.request.contextPath}/privacidad">Pol&iacute;tica de privacidad</a> | <a href="${pageContext.request.contextPath}/terminos">T&eacute;rminos de servicio</a></p>
    </footer>
</main>
    <%@ include file="/WEB-INF/includes/footer-scripts.jspf" %>
</body>
</html>
