<%-- 
    Document   : Planillas
    Created on : Aug 10, 2025, 5:17:28 PM
    Author     : jonat
--%>

<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE html>
<html data-theme="light">

<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title><c:out value="${not empty pageTitle ? pageTitle : planilla.nombre}" /></title>
  <link rel="manifest" href="${pageContext.request.contextPath}/manifest.jsp?v=${assetVersion}">
  <meta name="theme-color" content="#1f2d3d">
  <meta name="apple-mobile-web-app-capable" content="yes">
  <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
  <meta name="apple-mobile-web-app-title" content="SCA">
  <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/icons/pwa/apple-touch-icon.png">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/vendor/flat-ui/css/flat-ui.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/ctn-theme.css?v=${assetVersion}">
  <script src="${pageContext.request.contextPath}/scripts/planilla.js?v=${assetVersion}"></script>
  <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/images/ctn-logo.svg">
</head>

<body data-user-level="${sessionScope.user.level}" data-specialty="${not empty cursoSpecialty ? cursoSpecialty : (empty sessionScope.scaSpecialty ? 'informatica' : sessionScope.scaSpecialty)}" data-specialty-source="session">
  <c:url var="profileUrl" value="/perfil" />
  <c:url var="logoutUrl" value="/logout" />
  <header class="navbar navbar-default navbar-fixed-top ctn-navbar" role="navigation">
    <div class="container-fluid">
      <div class="navbar-header">
        <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#ctnNavbarMenu" aria-expanded="false">
          <span class="sr-only">Abrir navegación</span>
          <span class="icon-bar"></span>
          <span class="icon-bar"></span>
          <span class="icon-bar"></span>
        </button>
        <a class="navbar-brand ctn-navbar-brand" href="${pageContext.request.contextPath}/inicio" aria-label="Ir a inicio">
          <img class="header-logo" src="${pageContext.request.contextPath}/images/ctn-logo.svg" alt="CTN">
          <span>Colegio T&eacute;cnico Nacional</span>
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
    <div class="wrap">
    <section class="container">
        <div class="info-bar">
        <span>Bienvenido/a ${sessionScope.user.fullName}</span>
        <span>
          <c:out value="${nowFormatted}" />
        </span>
      </div>

      <div class="top-section planilla-hero" data-specialty="${fn:escapeXml(curso.especialidad)}">
        <div class="planilla-hero__header">
          <div class="planilla-hero__info">
            <span class="badge"><span class="dot"></span><span id="badgeSpecialtyName">${curso.especialidad}</span></span>
            <h1><c:out value="${not empty pageTitle ? pageTitle : planilla.nombre}" /></h1>
            <p class="planilla-subtitle">${curso.especialidad} ${curso.getCurso()}.<sup>o</sup> "${curso.seccion}" - ${planilla.etapa} etapa</p>
          </div>
          <div class="planilla-hero__actions">
            <%-- create a URL back to HomeServlet preserving cursoId + etapa --%>
            <c:url var="backUrl" value="/inicio">
                <c:param name="cursoId" value="${cursoId}" />
                <c:param name="etapa" value="${etapa}" />
            </c:url>

            <div class="btn-row">
              <a id="backBtn" class="btn-secondary" href="${backUrl}">
                <img class="back-icon" src="${pageContext.request.contextPath}/icons/back-arrow.svg" alt="Atrás">
                Atrás
              </a>
              <c:url var="downloadUrl" value="/planilla/export">
                  <c:param name="planillaId" value="${planilla.id}" />
              </c:url>

              <a id="downloadBtn" class="btn-primary" href="${downloadUrl}">
                <img class="download-icon" src="${pageContext.request.contextPath}/icons/download-icon.svg" alt="Descargar">
                Descargar
              </a>
            </div>
          </div>
        </div>

        <div class="planilla-toolbar">
          <form action="" method="get">
            <input type="hidden" name="cursoId" value="${cursoId}" />
            <input type="hidden" name="materiaId" value="${materiaId}" />

            <label for="etapaSelect">Etapa</label>
            <select class="selEtapa" id="etapaSelect" name="etapa" onchange="this.form.submit()">
              <option value="">Seleccione una etapa</option>
              <option value="1" ${ etapa == 1? "selected" : ""}>primera etapa</option>
              <option value="2" ${ etapa == 2? "selected" : ""}>segunda etapa</option>
            </select>
          </form>

          <span id="date-range"><i><b>Desde:</b> <c:out value="${planillaDesde}"/> <b>Hasta:</b> <c:out value="${planillaHasta}"/></i></span>
        </div>
      </div>

      <form method="post" action="${pageContext.request.contextPath}/planilla">
        <input type="hidden" name="cursoId" value="${cursoId}" />
        <input type="hidden" name="materiaId" value="${materiaId}" />
        <input type="hidden" name="etapa" value="${etapa}" />


        <div class="planilla-info-bar">
          <div class="grade-ranges-container">
            <span class="scale-summary">TP <strong>${totalPossiblePoints}</strong></span>
            <span class="scale-summary">Exigencia <strong>${exigencia}&percnt;</strong></span>
            <span class="grade-chip grade-chip--five" title="Desde ${gradeRanges['5'][0]} hasta ${gradeRanges['5'][1]}"><strong>5</strong>${gradeRanges['5'][0]}-${gradeRanges['5'][1]}</span>
            <span class="grade-chip grade-chip--four" title="Desde ${gradeRanges['4'][0]} hasta ${gradeRanges['4'][1]}"><strong>4</strong>${gradeRanges['4'][0]}-${gradeRanges['4'][1]}</span>
            <span class="grade-chip grade-chip--three" title="Desde ${gradeRanges['3'][0]} hasta ${gradeRanges['3'][1]}"><strong>3</strong>${gradeRanges['3'][0]}-${gradeRanges['3'][1]}</span>
            <span class="grade-chip grade-chip--two" title="Desde ${gradeRanges['2'][0]} hasta ${gradeRanges['2'][1]}"><strong>2</strong>${gradeRanges['2'][0]}-${gradeRanges['2'][1]}</span>
            <span class="grade-chip grade-chip--one" title="${gradeRanges['2'][0] - 1} puntos o menos"><strong>1</strong>${gradeRanges['2'][0] - 1} o menos</span>

            <label class="freeze-toggle" title="Fijar columnas # y Alumno">
              <input type="checkbox" id="freezeCheckbox" data-ignore-dirty/>
              Inmovilizar alumnos
            </label>
          </div>
          <button class="btn-primary save-button" type="button" disabled>
            <img class="save-icon" src="${pageContext.request.contextPath}/icons/save.svg">
            Sin edición en planilla
          </button>
        </div>

        <c:set var="taskColumns" value="" />
        <c:forEach var="t" items="${tareas}">
          <c:set var="taskColumns" value="${taskColumns} 88px" />
        </c:forEach>

        <div class="table-container">
          <div class="table-responsive">
            <div class="table planilla-table" style="grid-template-columns: 44px 220px 112px${taskColumns} minmax(0, 1fr);">
              <div class="table-row">
                <div class="table-heading">
                  Tareas - ${planilla.nombre}
                </div>
              </div>
              <div class="table-row">
                <div class="cell col-corner">
                  <img src="${pageContext.request.contextPath}/images/ctn-logo.svg" alt="CTN">
                </div>
                <div class="cell col-alumno table-column-head">Alumno</div>
                <div class="cell table-column-head">Total (${totalPossiblePoints})</div>
                <c:forEach var="t" items="${tareas}" varStatus="ts">
                  <div class="cell table-column-head task-column-head">
                    <span class="task-identifier">T${ts.index + 1}</span>
                    <c:choose>
                      <c:when test="${not empty t.googleCourseworkUrl}">
                        <a class="tarea-edit-link"
                           href="${t.googleCourseworkUrl}"
                           target="_blank"
                           rel="noopener noreferrer"
                           title="${t.tooltipText}">
                          <c:out value="${t.titulo}" />
                        </a>
                      </c:when>
                      <c:otherwise>
                        <a class="tarea-edit-link"
                           href="${pageContext.request.contextPath}/tarea?planillaId=${planilla.id}&amp;tareaId=${t.id}&amp;etapa=${etapa}"
                           title="${t.tooltipText}">
                          <c:out value="${t.titulo}" />
                        </a>
                      </c:otherwise>
                    </c:choose>
                    <div class="task-meta">
                      TP: <c:out value="${t.total}" />
                      <c:if test="${not empty t.fechaInicio}">Inicio: <c:out value="${t.fechaInicio}" /></c:if>
                    </div>
                  </div>
                </c:forEach>
                <div class="cell table-column-head col-fill" aria-hidden="true"></div>
              </div>

              <c:forEach var="row" items="${rows}" varStatus="rs">
                <div class="table-row">
                  <div class="cell col-index">${rs.index + 1}</div>
                  <div class="cell col-alumno">${row.alumnoNombre}</div>
                  <div class="cell row-summary">
                    <div>Total: <span class="row-total">${row.total}</span> (<span class="row-porcentaje">${row.porcentaje}</span>%)</div>
                    <div>Nota: <span class="row-nota">${row.nota}</span></div>
                  </div>
                  <c:forEach var="t" items="${tareas}">
                    <div class="cell grade-cell">
                      <span class="grade-readonly">
                        <c:choose>
                          <c:when test="${row.grades[t.id] != null}">${row.grades[t.id]}</c:when>
                          <c:otherwise>—</c:otherwise>
                        </c:choose>
                      </span>
                    </div>
                  </c:forEach>
                  <div class="cell col-fill" aria-hidden="true"></div>
                </div>
              </c:forEach>

            </div>
          </div>
        </div>
      </form>

    </section>

    <footer class="footer">
      <hr>
          <p>Colegio T&eacute;cnico Nacional</p>
    <p><a href="${pageContext.request.contextPath}/privacidad">Pol&iacute;tica de privacidad</a> | <a href="${pageContext.request.contextPath}/terminos">T&eacute;rminos de servicio</a></p>
    </footer>

    </div>

  </main>

  <c:if test="${not empty gradeRanges}">
    <script>
      window.planillaGradeRanges = {
        <c:forEach var="entry" items="${gradeRanges}" varStatus="loop">
          "${fn:escapeXml(entry.key)}":[${entry.value[0]},${entry.value[1]}]<c:if test="${!loop.last}">,</c:if>
        </c:forEach>
      };
    </script>
  </c:if>
  <script src="${pageContext.request.contextPath}/vendor/flat-ui/js/vendor/jquery.min.js"></script>
  <script src="${pageContext.request.contextPath}/vendor/flat-ui/js/flat-ui.js"></script>
  <script src="${pageContext.request.contextPath}/scripts/sca-theme.js?v=${assetVersion}"></script>
  <script>
    if ('serviceWorker' in navigator) {
      window.addEventListener('load', () => {
        navigator.serviceWorker.register('${pageContext.request.contextPath}/sw.js?v=${assetVersion}');
      });
    }
  </script>
</body>

</html>
