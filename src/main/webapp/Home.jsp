<%-- 
    Document   : Home
    Created on : Aug 3, 2025, 4:39:40 PM
    Author     : jonat
--%>

<%@page import="java.time.format.DateTimeFormatter"%>
<%@page import="java.time.LocalDateTime"%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE html>



<html data-theme="light">

<head>
  <title>CTNPortal - Profesores</title>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="manifest" href="${pageContext.request.contextPath}/manifest.jsp">
  <meta name="theme-color" content="#1f2d3d">
  <meta name="apple-mobile-web-app-capable" content="yes">
  <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
  <meta name="apple-mobile-web-app-title" content="CTN Portal">
  <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/icons/pwa/apple-touch-icon.png">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/vendor/flat-ui/css/flat-ui.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/ctn-theme.css?v=236">
  <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/images/ctn-logo.svg">
</head>

<body data-specialty="${empty sessionScope.siaSpecialty ? 'informatica' : sessionScope.siaSpecialty}">
  <c:url var="profileUrl" value="/ProfileServlet" />
  <c:url var="logoutUrl" value="/LogoutServlet" />
  <header class="navbar navbar-default navbar-fixed-top ctn-navbar" role="navigation">
    <div class="container-fluid">
      <div class="navbar-header">
        <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#ctnNavbarMenu" aria-expanded="false">
          <span class="sr-only">Abrir navegación</span>
          <span class="icon-bar"></span>
          <span class="icon-bar"></span>
          <span class="icon-bar"></span>
        </button>
        <a class="navbar-brand ctn-navbar-brand" href="${pageContext.request.contextPath}/HomeServlet" aria-label="Ir a inicio">
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
              <li><a role="menuitem" class="session-logout" href="${logoutUrl}">Cerrar Sesión</a></li>
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
        <c:if test="${not empty selCurso}">
          <span class="info-bar-divider">•</span>
          <span>${selCurso.getCurso()}.<sup>o</sup> "${selCurso.seccion}" | ${selCurso.especialidad}</span>
        </c:if>
        <span class="info-bar-spacer"></span>
        <span><c:out value="${nowFormatted}" /></span>
      </div>
      <div class="top-section planilla-hero hero-shell">
        <div class="planilla-hero__header">
          <div class="planilla-hero__info">
            <span class="badge"><span class="dot"></span>${selCurso.especialidad}</span>
            <h1>Panel del curso</h1>
            <p class="planilla-subtitle">Gestiona planillas, revisa cursos y tareas de tu especialidad.</p>
          </div>
        </div>
        <div class="menu-container">
          <form id="cursoSelectionForm" action="HomeServlet" method="get" class="curso-selection-form">
            <label for="selEspecialidad" style="font-weight:600;margin-right:0.5rem;">Especialidad</label>
            <select id="selEspecialidad" name="especialidad"></select>
            <label for="selCursoNivel" style="font-weight:600;margin-right:0.5rem;">Curso</label>
            <select id="selCursoNivel" name="promocion" disabled></select>
            <label for="selSeccion" style="font-weight:600;margin-right:0.5rem;">Sección</label>
            <select id="selSeccion" name="seccion" disabled></select>
            <input type="hidden" name="cursoId" id="cursoIdHidden" value="${empty selCurso ? '' : selCurso.id}" />
            <input type="hidden" name="etapa" value="${selEtapa}" />
          </form>
        </div>
      </div>

      <c:if test="${not googleClassroomConnected and empty planillas}">
        <div class="empty-state-wrapper">
          <div class="empty-state empty-state-card empty-state-card--compact">
            <c:out value="${googleClassroomPlaceholder}" />
          </div>
        </div>
      </c:if>

      <div class="grid-container">
        <c:if test="${not empty googleClassroomError}">
          <div class="empty-state empty-state-card">
            <c:out value="${googleClassroomError}" />
          </div>
        </c:if>

        <c:if test="${not empty googleClassroomVisibilityNotice}">
          <div class="empty-state empty-state-card empty-state-card--compact">
            <c:out value="${googleClassroomVisibilityNotice}" />
          </div>
        </c:if>

        <c:choose>
          <c:when test="${showPlanillaCards}">
            <div class="section-block">
              <div class="section-heading">Planillas del curso</div>
              <div class="planilla-grid">
                <c:forEach var="planilla" items="${planillas}">
                  <a class="planilla-card-link" href="${pageContext.request.contextPath}/PlanillaServlet?planillaId=${planilla.id}&cursoId=${selCurso.id}&materiaId=${planilla.materiaId}&etapa=${selEtapa}">
                    <div class="subject-card">
                      <div class="subject-card__header">
                        <div class="subject-card__title"><c:out value="${planilla.nombre}" /></div>
                      </div>
                      <div class="subject-card__meta">
                        <span class="subject-card__chip">Periodo <strong><c:out value="${planilla.periodo}" /></strong></span>
                        <span class="subject-card__chip">Tareas <strong><c:out value="${planilla.tareasCount}" /></strong></span>
                      </div>
                      <span class="subject-card__action">Abrir planilla</span>
                    </div>
                  </a>
                </c:forEach>
              </div>
            </div>
          </c:when>
          <c:otherwise>
            <c:if test="${googleClassroomConnected and empty googleClassroomCourses}">
              <div class="empty-state empty-state-card">
                No hay planillas para este curso y etapa. Los bloques de Google Classroom aparecerán cuando haya conexión activa.
              </div>
            </c:if>
          </c:otherwise>
        </c:choose>
      </div>

      <c:if test="${not empty materiasDetectadas}">
        <div class="section-block">
          <div class="section-heading">Materias disponibles para asignar</div>
          <div class="planilla-grid">
            <c:forEach var="materia" items="${materiasDetectadas}">
              <a class="planilla-card-link" href="${pageContext.request.contextPath}/PlanillaServlet?cursoId=${selCurso.id}&materiaId=${materia.id}&etapa=${selEtapa}">
                <div class="subject-card">
                  <div class="subject-card__header">
                    <div class="subject-card__title"><c:out value="${materia.nombre}" /></div>
                    <span class="subject-card__status">Sin planilla</span>
                  </div>
                  <div class="subject-card__meta">
                    <span class="subject-card__chip">Categoría <strong><c:out value="${materia.categoria}" /></strong></span>
                  </div>
                  <span class="subject-card__action">Crear planilla</span>
                </div>
              </a>
            </c:forEach>
          </div>
        </div>
      </c:if>

      <c:if test="${googleClassroomConnected and not empty googleClassroomCourses}">
        <div class="section-block">
          <div class="section-heading">Cursos de Google Classroom</div>
          <div class="planilla-grid">
            <c:forEach var="course" items="${googleClassroomCourses}">
              <c:set var="courseId" value="${course.id}" />
              <c:set var="planillaId" value="${classroomPlanillaMap[courseId]}" />
              <c:set var="materiaId" value="${classroomPlanillaMateriaMap[courseId]}" />
              <c:choose>
                <c:when test="${not empty planillaId}">
                  <c:url var="courseLink" value="/PlanillaServlet">
                    <c:param name="planillaId" value="${planillaId}" />
                    <c:param name="cursoId" value="${selCurso.id}" />
                    <c:param name="materiaId" value="${materiaId}" />
                    <c:param name="etapa" value="${selEtapa}" />
                  </c:url>
                </c:when>
                <c:when test="${not empty materiaId}">
                  <c:url var="courseLink" value="/PlanillaServlet">
                    <c:param name="cursoId" value="${selCurso.id}" />
                    <c:param name="materiaId" value="${materiaId}" />
                    <c:param name="etapa" value="${selEtapa}" />
                  </c:url>
                </c:when>
                <c:otherwise>
                  <c:set var="courseLink" value="${pageContext.request.contextPath}/HomeServlet?cursoId=${selCurso.id}&etapa=${selEtapa}" />
                </c:otherwise>
              </c:choose>
              <c:choose>
                <c:when test="${not empty planillaId or not empty materiaId}">
                  <a class="planilla-card-link" href="${courseLink}" style="display:block;color:inherit;text-decoration:none;">
                    <div class="card-surface" style="border:none;border-left:4px solid var(--accent);cursor:pointer;transition:transform 120ms ease,border-color 120ms ease;">
                      <div class="head" style="padding:10px 14px;font-weight:600;border-bottom:1px solid var(--color-border);background:transparent;display:flex;justify-content:space-between;align-items:center;color:inherit;">
                        <c:out value="${course.name}" />
                      </div>
                      <div class="body">
                        <div class="info-grid">
                          <span class="total-tareas label">Sección</span>
                          <span class="total-tareas colon">:</span>
                          <span class="total-tareas value"><c:out value="${empty course.section ? 'Sin sección' : course.section}" /></span>
                        </div>
                      </div>
                    </div>
                  </a>
                </c:when>
                <c:otherwise>
                  <div class="card-surface" style="border:none;border-left:4px solid var(--accent);opacity:0.6;" aria-disabled="true">
                    <div class="head" style="padding:10px 14px;font-weight:600;border-bottom:1px solid var(--color-border);background:transparent;display:flex;justify-content:space-between;align-items:center;color:inherit;">
                      <div class="card-title-row">
                        <c:out value="${course.name}" />
                        <span class="badge-warning">Sin vincular</span>
                      </div>
                    </div>
                    <div class="body">
                      <div class="info-grid">
                        <span class="total-tareas label">Sección</span>
                        <span class="total-tareas colon">:</span>
                        <span class="total-tareas value"><c:out value="${empty course.section ? 'Sin sección' : course.section}" /></span>
                      </div>
                    </div>
                  </div>
                </c:otherwise>
              </c:choose>
            </c:forEach>
          </div>
        </div>
      </c:if>

    </section>



    <footer class="footer">
      <hr>
      <p>Colegio Técnico Nacional</p>
    </footer>


  </main>

<script>
const CURSOS = [
    <c:forEach var="cu" items="${cursos}" varStatus="s">
        {"id": ${cu.id}, "especialidad": "<c:out value='${cu.especialidad}'/>", "nivel": ${cu.curso}, "seccion": "<c:out value='${cu.seccion}'/>"}<c:if test="${!s.last}">,</c:if>
    </c:forEach>
];

(function () {
  const selEspecialidad = document.getElementById('selEspecialidad');
  const selCursoNivel = document.getElementById('selCursoNivel');
  const selSeccion = document.getElementById('selSeccion');
  const cursoIdHidden = document.getElementById('cursoIdHidden');
  const selectedCursoId = ${empty selCurso ? 0 : selCurso.id};

  function uniqueEspecialidades() {
    const seen = new Set();
    const out = [];
    CURSOS.forEach(c => {
      if (!seen.has(c.especialidad)) {
        seen.add(c.especialidad);
        out.push(c.especialidad);
      }
    });
    return out;
  }

  function populateEspecialidad() {
    selEspecialidad.innerHTML = '';
    selEspecialidad.appendChild(new Option('--Seleccione especialidad--',''));
    uniqueEspecialidades().forEach(e => selEspecialidad.appendChild(new Option(e,e)));
    selCursoNivel.innerHTML = '<option value="">--Seleccione curso--</option>';
    selCursoNivel.disabled = true;
    selSeccion.innerHTML = '<option value="">--Seleccione sección--</option>';
    selSeccion.disabled = true;
  }

  function formatCursoNivel(n) {
    return n + 'º';
  }

  function populateCursoNivel() {
    const esp = selEspecialidad.value;
    selCursoNivel.innerHTML = '';
    selCursoNivel.appendChild(new Option('--Seleccione curso--',''));
    if (!esp) {
      selCursoNivel.disabled = true;
      selSeccion.innerHTML = '<option value="">--Seleccione sección--</option>';
      selSeccion.disabled = true;
      return;
    }
    const niveles = [...new Set(CURSOS.filter(c => c.especialidad === esp).map(c => c.nivel))].sort((a,b)=>a-b);
    niveles.forEach(n => selCursoNivel.appendChild(new Option(formatCursoNivel(n), n)));
    selCursoNivel.disabled = false;
    selSeccion.innerHTML = '<option value="">--Seleccione sección--</option>';
    selSeccion.disabled = true;
  }

  function populateSeccion() {
    const esp = selEspecialidad.value;
    const nivel = parseInt(selCursoNivel.value);
    selSeccion.innerHTML = '';
    selSeccion.appendChild(new Option('--Seleccione sección--',''));
    if (!esp || !nivel) {
      selSeccion.disabled = true;
      return;
    }
    const secciones = [...new Set(CURSOS.filter(c => c.especialidad === esp && c.nivel === nivel).map(c => c.seccion))];
    secciones.forEach(s => selSeccion.appendChild(new Option(s,s)));
    selSeccion.disabled = false;
  }

  function updateHiddenCursoId(submit) {
    const esp = selEspecialidad.value;
    const nivel = parseInt(selCursoNivel.value);
    const seccion = selSeccion.value;
    cursoIdHidden.value = '';
    if (!esp || !nivel || !seccion) {
      return;
    }
    const found = CURSOS.find(c => c.especialidad === esp && c.nivel === nivel && c.seccion === seccion);
    if (found) {
      cursoIdHidden.value = found.id;
      if (submit) {
        document.getElementById('cursoSelectionForm').submit();
      }
    }
  }

  function preselectCurso() {
    if (!selectedCursoId) return;
    const found = CURSOS.find(c => c.id === selectedCursoId);
    if (!found) return;
    selEspecialidad.value = found.especialidad;
    populateCursoNivel();
    selCursoNivel.value = found.nivel;
    populateSeccion();
    selSeccion.value = found.seccion;
    updateHiddenCursoId(false);
  }

  populateEspecialidad();
  preselectCurso();
  selEspecialidad.addEventListener('change', function () {
    populateCursoNivel();
    updateHiddenCursoId(false);
  });
  selCursoNivel.addEventListener('change', function () {
    populateSeccion();
    updateHiddenCursoId(false);
  });
  selSeccion.addEventListener('change', function () {
    updateHiddenCursoId(true);
  });


})();
</script>
  <script src="${pageContext.request.contextPath}/vendor/flat-ui/js/vendor/jquery.min.js"></script>
  <script src="${pageContext.request.contextPath}/vendor/flat-ui/js/flat-ui.js"></script>
  <script src="${pageContext.request.contextPath}/scripts/sia-theme.js?v=164"></script>
  <script>
    if ('serviceWorker' in navigator) {
      window.addEventListener('load', () => {
        navigator.serviceWorker.register('${pageContext.request.contextPath}/sw.js');
      });
    }
  </script>
</body>

</html>
