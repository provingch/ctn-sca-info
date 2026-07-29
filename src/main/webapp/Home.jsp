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
  <title>SCA - Profesores</title>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="manifest" href="${pageContext.request.contextPath}/manifest.jsp">
  <meta name="theme-color" content="#1f2d3d">
  <meta name="apple-mobile-web-app-capable" content="yes">
  <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
  <meta name="apple-mobile-web-app-title" content="SCA">
  <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/icons/pwa/apple-touch-icon.png">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/vendor/flat-ui/css/flat-ui.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/ctn-theme.css?v=236">
  <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/images/ctn-logo.svg">
</head>

<body data-specialty="${empty sessionScope.scaSpecialty ? 'informatica' : sessionScope.scaSpecialty}">
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
            <h1>Panel SCA del curso</h1>
            <p class="planilla-subtitle">Gestiona inicio de clase y planillas de puntaje conectadas a tus cursos.</p>
          </div>
        </div>
        <c:if test="${viewMode eq 'planillas'}">
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
              <input type="hidden" name="view" value="planillas" />
            </form>
          </div>
        </c:if>
      </div>

      <div class="section-block" style="margin-bottom:16px;">
        <div class="section-heading">Vista principal</div>
        <div style="display:flex;gap:10px;flex-wrap:wrap;">
          <a class="planilla-card-link" href="${pageContext.request.contextPath}/HomeServlet?view=clase&cursoId=${empty selCurso ? '' : selCurso.id}&etapa=${selEtapa}" style="padding:8px 12px;border:1px solid var(--color-border);border-radius:8px;${viewMode eq 'clase' ? 'font-weight:700;' : ''}">Iniciar clase</a>
          <a class="planilla-card-link" href="${pageContext.request.contextPath}/HomeServlet?view=planillas&cursoId=${empty selCurso ? '' : selCurso.id}&etapa=${selEtapa}" style="padding:8px 12px;border:1px solid var(--color-border);border-radius:8px;${viewMode eq 'planillas' ? 'font-weight:700;' : ''}">Planillas de puntaje</a>
        </div>
      </div>

      <c:if test="${viewMode eq 'clase'}">
        <div class="section-block">
          <div class="section-heading">Formulario de inicio de clase</div>

          <c:if test="${param.rasgoError eq 'tema'}">
            <div class="empty-state empty-state-card">Debes seleccionar curso, instrumento y tema a desarrollar.</div>
          </c:if>
          <c:if test="${param.rasgoError eq 'sin-alumnos'}">
            <div class="empty-state empty-state-card">No hay alumnos con nombre y apellido validos para el curso seleccionado.</div>
          </c:if>
          <c:if test="${param.rasgoOk eq 'created'}">
            <div class="empty-state empty-state-card">Clase registrada correctamente con asistencia inicial.</div>
          </c:if>
          <c:if test="${not empty rasgoErrorMessage}">
            <div class="empty-state empty-state-card"><c:out value="${rasgoErrorMessage}" /></div>
          </c:if>

          <form id="classSelectionForm" action="${pageContext.request.contextPath}/HomeServlet" method="get" class="curso-selection-form" style="margin-bottom:14px;display:flex;gap:12px;flex-wrap:wrap;align-items:flex-end;">
            <input type="hidden" name="view" value="clase" />
            <input type="hidden" name="etapa" value="${selEtapa}" />
            <div>
              <label for="classEspecialidad" style="font-weight:600;">Especialidad</label>
              <select id="classEspecialidad" name="classEspecialidad"></select>
            </div>
            <div>
              <label for="classCursoNivel" style="font-weight:600;">Curso</label>
              <select id="classCursoNivel" name="classCursoNivel" disabled></select>
            </div>
            <div>
              <label for="classSeccion" style="font-weight:600;">Sección</label>
              <select id="classSeccion" name="classSeccion" disabled></select>
            </div>
            <div>
              <label for="classTurno" style="font-weight:600;">Turno</label>
              <select id="classTurno" name="turno" class="form-control">
                <option value="">-- Seleccione turno --</option>
                <option value="mañana" ${param.turno eq 'mañana' ? 'selected' : ''}>Mañana</option>
                <option value="tarde" ${param.turno eq 'tarde' ? 'selected' : ''}>Tarde</option>
                <option value="noche" ${param.turno eq 'noche' ? 'selected' : ''}>Noche</option>
              </select>
            </div>
            <input type="hidden" name="cursoId" id="classCursoIdHidden" value="${empty selCurso ? '' : selCurso.id}" />
          </form>

          <form action="${pageContext.request.contextPath}/HomeServlet" method="post" style="margin-bottom:14px;display:grid;gap:10px;max-width:980px;">
            <input type="hidden" name="action" value="create-rasgo-planilla" />
            <input type="hidden" name="cursoId" value="${empty selCurso ? '' : selCurso.id}" id="formCursoId" />
            <input type="hidden" name="turno" value="${param.turno}" id="formTurno" />
            <input type="hidden" name="etapa" value="${selEtapa}" />

            <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(220px, 1fr));gap:10px;">
              <div>
                <label for="instrumentoId" style="font-weight:600;">Tipo de clase (Instrumento)</label>
                <select id="instrumentoId" name="instrumentoId" class="form-control" required>
                  <option value="">-- Seleccione instrumento --</option>
                  <c:forEach var="instrumento" items="${instrumentos}">
                    <option value="${instrumento.id}"><c:out value="${instrumento.nombre}" /></option>
                  </c:forEach>
                </select>
              </div>
              <div>
                <label for="temaRasgo" style="font-weight:600;">Tema a desarrollar</label>
                <input id="temaRasgo" name="tema" class="form-control" maxlength="150" placeholder="Ej.: Integrales definidas y aplicaciones" required />
              </div>
            </div>

            <div class="empty-state empty-state-card" style="text-align:left;">
              Marca ausentes en la lista. Los no marcados se guardan como presentes.
            </div>

            <div class="table-responsive" style="margin-bottom:8px;">
              <table class="table table-striped">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Alumno</th>
                    <th>Asistencia</th>
                  </tr>
                </thead>
                <tbody>
                  <c:forEach var="alumno" items="${rasgoAlumnosValidos}" varStatus="loop">
                    <tr>
                      <td>${loop.index + 1}</td>
                      <td><c:out value="${alumno.apellido}" />, <c:out value="${alumno.nombre}" /></td>
                      <td>
                        <label style="margin:0;display:flex;align-items:center;gap:6px;font-weight:500;">
                          <input type="checkbox" name="alumnosAusentes" value="${alumno.id}" />
                          Ausente
                        </label>
                      </td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>

            <button type="submit" class="btn btn-primary">Guardar inicio de clase</button>
          </form>

          <div style="margin-bottom:14px;">
            <span class="subject-card__chip">Alumnos habilitados <strong>${fn:length(rasgoAlumnosValidos)}</strong></span>
            <span class="subject-card__chip">Alumnos con datos incompletos <strong>${fn:length(rasgoAlumnosInvalidos)}</strong></span>
          </div>

          <c:if test="${not empty rasgoAlumnosInvalidos}">
            <div class="empty-state empty-state-card" style="margin-bottom:14px;">
              Estos alumnos no se incluyen hasta completar nombre y apellido.
            </div>
            <div class="table-responsive" style="margin-bottom:14px;">
              <table class="table table-striped">
                <thead>
                  <tr>
                    <th>Alumno</th>
                  </tr>
                </thead>
                <tbody>
                  <c:forEach var="alumno" items="${rasgoAlumnosInvalidos}">
                    <tr>
                      <td><c:out value="${alumno.nombre}" /> <c:out value="${alumno.apellido}" /></td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>
          </c:if>

          <c:if test="${not empty rasgoPlanillas}">
            <div style="margin-bottom:14px;">
              <label for="rasgoPlanillaSel" style="font-weight:600;">Clases registradas</label>
              <select id="rasgoPlanillaSel" class="form-control" style="max-width:520px;" onchange="location.href='${pageContext.request.contextPath}/HomeServlet?view=clase&cursoId=${selCurso.id}&etapa=${selEtapa}&rasgoPlanillaId=' + this.value;">
                <c:forEach var="rp" items="${rasgoPlanillas}">
                  <option value="${rp.id}" ${not empty rasgoPlanillaSeleccionada and rasgoPlanillaSeleccionada.id == rp.id ? 'selected' : ''}>${rp.tema} - ${rp.fechaClase}</option>
                </c:forEach>
              </select>
            </div>
          </c:if>

          <c:if test="${not empty rasgoAsistencias}">
            <div class="table-responsive">
              <table class="table table-striped">
                <thead>
                  <tr>
                    <th>Alumno</th>
                    <th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  <c:forEach var="asistencia" items="${rasgoAsistencias}">
                    <tr>
                      <td><c:out value="${asistencia.alumnoNombreCompleto}" /></td>
                      <td><c:out value="${asistencia.estado}" /></td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>
          </c:if>
        </div>
      </c:if>

      <c:if test="${viewMode eq 'planillas'}">

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

  function formatCursoNivel(n) {
    return n + 'º';
  }

  function setupCursoSelector(config) {
    const selEspecialidad = document.getElementById(config.especialidadId);
    const selCursoNivel = document.getElementById(config.cursoId);
    const selSeccion = document.getElementById(config.seccionId);
    const cursoIdHidden = document.getElementById(config.hiddenId);
    const form = document.getElementById(config.formId);

    if (!selEspecialidad || !selCursoNivel || !selSeccion || !cursoIdHidden || !form) {
      return;
    }

    function populateEspecialidad() {
      selEspecialidad.innerHTML = '';
      selEspecialidad.appendChild(new Option('--Seleccione especialidad--', ''));
      uniqueEspecialidades().forEach(e => selEspecialidad.appendChild(new Option(e, e)));
      selCursoNivel.innerHTML = '<option value="">--Seleccione curso--</option>';
      selCursoNivel.disabled = true;
      selSeccion.innerHTML = '<option value="">--Seleccione sección--</option>';
      selSeccion.disabled = true;
    }

    function populateCursoNivel() {
      const esp = selEspecialidad.value;
      selCursoNivel.innerHTML = '';
      selCursoNivel.appendChild(new Option('--Seleccione curso--', ''));
      if (!esp) {
        selCursoNivel.disabled = true;
        selSeccion.innerHTML = '<option value="">--Seleccione sección--</option>';
        selSeccion.disabled = true;
        return;
      }
      const niveles = [...new Set(CURSOS.filter(c => c.especialidad === esp).map(c => c.nivel))].sort((a, b) => a - b);
      niveles.forEach(n => selCursoNivel.appendChild(new Option(formatCursoNivel(n), n)));
      selCursoNivel.disabled = false;
      selSeccion.innerHTML = '<option value="">--Seleccione sección--</option>';
      selSeccion.disabled = true;
    }

    function populateSeccion() {
      const esp = selEspecialidad.value;
      const nivel = parseInt(selCursoNivel.value);
      selSeccion.innerHTML = '';
      selSeccion.appendChild(new Option('--Seleccione sección--', ''));
      if (!esp || !nivel) {
        selSeccion.disabled = true;
        return;
      }
      const secciones = [...new Set(CURSOS.filter(c => c.especialidad === esp && c.nivel === nivel).map(c => c.seccion))];
      secciones.forEach(s => selSeccion.appendChild(new Option(s, s)));
      selSeccion.disabled = false;
    }

    function updateCursoId(submit) {
      const esp = selEspecialidad.value;
      const nivel = parseInt(selCursoNivel.value);
      const seccion = selSeccion.value;
      cursoIdHidden.value = '';
      if (!esp || !nivel || !seccion) {
        if (config.onCursoChanged) {
          config.onCursoChanged('');
        }
        return;
      }
      const found = CURSOS.find(c => c.especialidad === esp && c.nivel === nivel && c.seccion === seccion);
      if (found) {
        cursoIdHidden.value = found.id;
        if (config.onCursoChanged) {
          config.onCursoChanged(String(found.id));
        }
        if (submit) {
          form.submit();
        }
      }
    }

    function preselectCurso() {
      if (!config.selectedCursoId) {
        return;
      }
      const found = CURSOS.find(c => c.id === config.selectedCursoId);
      if (!found) {
        return;
      }
      selEspecialidad.value = found.especialidad;
      populateCursoNivel();
      selCursoNivel.value = found.nivel;
      populateSeccion();
      selSeccion.value = found.seccion;
      updateCursoId(false);
    }

    populateEspecialidad();
    preselectCurso();
    selEspecialidad.addEventListener('change', function () {
      populateCursoNivel();
      updateCursoId(false);
    });
    selCursoNivel.addEventListener('change', function () {
      populateSeccion();
      updateCursoId(false);
    });
    selSeccion.addEventListener('change', function () {
      updateCursoId(true);
    });
  }

  const selectedCursoId = ${empty selCurso ? 0 : selCurso.id};

  setupCursoSelector({
    especialidadId: 'selEspecialidad',
    cursoId: 'selCursoNivel',
    seccionId: 'selSeccion',
    hiddenId: 'cursoIdHidden',
    formId: 'cursoSelectionForm',
    selectedCursoId: selectedCursoId
  });

  setupCursoSelector({
    especialidadId: 'classEspecialidad',
    cursoId: 'classCursoNivel',
    seccionId: 'classSeccion',
    hiddenId: 'classCursoIdHidden',
    formId: 'classSelectionForm',
    selectedCursoId: selectedCursoId,
    onCursoChanged: function (cursoId) {
      const hiddenPostCursoId = document.getElementById('formCursoId');
      if (hiddenPostCursoId) {
        hiddenPostCursoId.value = cursoId;
      }
    }
  });

  const classTurno = document.getElementById('classTurno');
  const classSelectionForm = document.getElementById('classSelectionForm');
  const formTurno = document.getElementById('formTurno');
  if (classTurno && formTurno) {
    formTurno.value = classTurno.value;
    classTurno.addEventListener('change', function () {
      formTurno.value = classTurno.value;
      if (classSelectionForm) {
        classSelectionForm.submit();
      }
    });
  }
})();
</script>
  <script src="${pageContext.request.contextPath}/vendor/flat-ui/js/vendor/jquery.min.js"></script>
  <script src="${pageContext.request.contextPath}/vendor/flat-ui/js/flat-ui.js"></script>
  <script src="${pageContext.request.contextPath}/scripts/sca-theme.js?v=164"></script>
  <script>
    if ('serviceWorker' in navigator) {
      window.addEventListener('load', () => {
        navigator.serviceWorker.register('${pageContext.request.contextPath}/sw.js');
      });
    }
  </script>
</body>

</html>
