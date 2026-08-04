<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Admin - Asignaciones" scope="request" />
<%@ include file="/WEB-INF/includes/head.jspf" %>
<body class="admin-page" data-page="admin-asignaciones" data-user-level="${sessionScope.user.level}" data-specialty="general" data-specialty-source="system">
<c:set var="navbarHomeUrl" value="${pageContext.request.contextPath}/admin" />
<%@ include file="/WEB-INF/includes/navbar.jspf" %>
<main>
    <section class="container page-shell">
        <div class="admin-hero">
            <div>
                <span class="admin-eyebrow">Administración</span>
                <h1>Asignaciones</h1>
                <p>Vincula profesores con materias y cursos.</p>
            </div>
            <a class="btn-secondary" href="${pageContext.request.contextPath}/admin">Volver al panel</a>
        </div>

        <c:if test="${not empty errors}">
            <div class="errors"><ul><c:forEach var="e" items="${errors}"><li>${e}</li></c:forEach></ul></div>
        </c:if>
        <c:if test="${not empty flashMessage}"><div class="flash">${flashMessage}</div></c:if>

        <form class="admin-card admin-form" method="post" action="${pageContext.request.contextPath}/admin/asignaciones" id="createForm">
            <div class="admin-card-header"><h2>Crear asignación</h2></div>
            <div class="admin-form-grid">
            <input type="hidden" name="action" value="crear" />
            <label>Profesor</label>
            <select name="profesorId" required>
                <option value="">Seleccione profesor</option>
                <c:forEach var="p" items="${profesores}">
                    <option value="${p.id}">${p.apellido} ${p.nombre}</option>
                </c:forEach>
            </select>

            <label>Materia</label>
            <select name="materiaId" required>
                <option value="">Seleccione materia</option>
                <c:forEach var="m" items="${materias}">
                    <option value="${m.id}">${m.nombre}</option>
                </c:forEach>
            </select>

            <label>Especialidad</label>
            <select id="selEspecialidad"></select>

            <label>Curso</label>
            <select id="selCursoNivel"></select>

            <label>Sección</label>
            <select id="selSeccion"></select>

            <input type="hidden" name="cursoId" id="cursoIdHidden" />
            </div>
            <button class="btn-primary admin-submit" type="submit" id="createBtn">Crear asignación</button>
        </form>

        <div class="admin-card admin-table-card">
        <div class="admin-card-header"><h2>Asignaciones existentes</h2></div>
        <div class="admin-table-wrap">
        <table class="admin-table">
            <thead>
                <tr><th>ID</th><th>Profesor</th><th>Materia</th><th>Curso</th><th>Acciones</th></tr>
            </thead>
            <tbody>
                <c:forEach var="a" items="${asignaciones}">
                    <tr>
                        <td>${a.id}</td>
                        <td>${a.profesorNombre}</td>
                        <td>${a.materiaNombre}</td>
                        <td>${a.cursoDescripcion}</td>
                        <td>
                            <form class="admin-inline-form" method="post" action="${pageContext.request.contextPath}/admin/asignaciones">
                                <input type="hidden" name="action" value="eliminar" />
                                <input type="hidden" name="id" value="${a.id}" />
                                <button class="btn-danger btn-compact" type="submit" onclick="return confirm('Eliminar asignación?');">Eliminar</button>
                            </form>
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

<!-- Dump cursos to JS for cascaded selector -->
<script>
const CURSOS = [];
<c:forEach var="cu" items="${cursos}">
CURSOS.push({"id": ${cu.id}, "especialidad": "<c:out value='${cu.especialidad}'/>", "nivel": Number("<c:out value='${cu.nivel}'/>"), "seccion": "<c:out value='${cu.seccion}'/>"});
</c:forEach>

const ESPECIALIDADES = [];
<c:forEach var="esp" items="${especialidades}">
ESPECIALIDADES.push("<c:out value='${esp.nombre}'/>");
</c:forEach>

const SECCIONES_TRES = new Set(['quimica industrial', 'construcciones civiles', 'electronica']);

function normalizeText(text) {
    return (text || '')
        .toString()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .trim()
        .toLowerCase();
}

function uniqueEspecialidades() {
    const seen = new Set();
    const out = [];
    ESPECIALIDADES.forEach(e => {
        if (e && !seen.has(e)) {
            seen.add(e);
            out.push(e);
        }
    });
    CURSOS.forEach(c => {
        if (c.especialidad && !seen.has(c.especialidad)) {
            seen.add(c.especialidad);
            out.push(c.especialidad);
        }
    });
    out.sort((a, b) => a.localeCompare(b, 'es', { sensitivity: 'base' }));
    return out;
}

function populateEspecialidad() {
    const sel = document.getElementById('selEspecialidad');
    sel.innerHTML = '';
    sel.appendChild(new Option('Seleccione especialidad',''));
    uniqueEspecialidades().forEach(e => sel.appendChild(new Option(e,e)));
}

function populateCursoNivel() {
    const esp = document.getElementById('selEspecialidad').value;
    const sel = document.getElementById('selCursoNivel');
    sel.innerHTML = '';
    sel.appendChild(new Option('Seleccione curso',''));
    if (!esp) return;
    const niveles = [1, 2, 3];
    niveles.forEach(n => sel.appendChild(new Option(n + 'º', n)));
}

function populateSeccion() {
    const esp = document.getElementById('selEspecialidad').value;
    const nivel = parseInt(document.getElementById('selCursoNivel').value);
    const sel = document.getElementById('selSeccion');
    sel.innerHTML = '';
    sel.appendChild(new Option('Seleccione sección',''));
    if (!esp || !nivel) return;
    const secciones = normalizeText(esp) && SECCIONES_TRES.has(normalizeText(esp)) ? ['A', 'B', 'C'] : ['A', 'B'];
    secciones.forEach(s => sel.appendChild(new Option(s,s)));
    updateHiddenCursoId();
}

function updateHiddenCursoId() {
    const esp = document.getElementById('selEspecialidad').value;
    const nivel = parseInt(document.getElementById('selCursoNivel').value);
    const seccion = document.getElementById('selSeccion').value;
    const hidden = document.getElementById('cursoIdHidden');
    hidden.value = '';
    if (!esp || !nivel || !seccion) return;
    const found = CURSOS.find(c => c.especialidad===esp && c.nivel===nivel && c.seccion===seccion);
    if (found) hidden.value = found.id;
}

document.addEventListener('DOMContentLoaded', function(){
    populateEspecialidad();
    document.getElementById('selEspecialidad').addEventListener('change', function(){ populateCursoNivel(); document.getElementById('selSeccion').innerHTML=''; updateHiddenCursoId(); });
    document.getElementById('selCursoNivel').addEventListener('change', function(){ populateSeccion(); });
    document.getElementById('selSeccion').addEventListener('change', updateHiddenCursoId);
    document.getElementById('createForm').addEventListener('submit', function(e){ if (!document.getElementById('cursoIdHidden').value) { e.preventDefault(); alert('Seleccione una combinación válida de especialidad/curso/sección que corresponda a un curso real.'); } });
});
</script>
    <%@ include file="/WEB-INF/includes/footer-scripts.jspf" %>
</body>
</html>
