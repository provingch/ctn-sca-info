<%-- 
    Document   : Planillas
    Created on : Aug 10, 2025, 5:17:28 PM
    Author     : jonat
--%>

<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<c:set var="pageTitle" value="${not empty pageTitle ? pageTitle : planilla.nombre}" scope="request" />
<c:set var="headExtraFragment" value="/WEB-INF/includes/head-extra-planilla.jspf" scope="request" />
<%@ include file="/WEB-INF/includes/head.jspf" %>

<body data-user-level="${sessionScope.user.level}" data-specialty="${not empty cursoSpecialty ? cursoSpecialty : (empty sessionScope.scaSpecialty ? 'general' : sessionScope.scaSpecialty)}" data-specialty-source="session">
  <%@ include file="/WEB-INF/includes/navbar.jspf" %>


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
              <option value="1" ${ etapa == 1? "selected" : ""}>Primera etapa</option>
              <option value="2" ${ etapa == 2? "selected" : ""}>Segunda etapa</option>
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

        <div class="planilla-table-tools" role="search" aria-label="Buscar alumnos en la planilla">
          <label class="planilla-student-search" for="studentSearch">
            <span>Buscar alumno</span>
            <input id="studentSearch"
                   type="search"
                   placeholder="Nombre del alumno…"
                   autocomplete="off"
                   spellcheck="false"
                   data-ignore-dirty />
          </label>
          <span id="studentCount" class="planilla-student-count" aria-live="polite">
            ${fn:length(rows)} ${fn:length(rows) == 1 ? 'alumno' : 'alumnos'}
          </span>
        </div>

        <div class="table-container">
          <div class="table-responsive">
            <div id="planillaStudentTable" class="table planilla-table" style="grid-template-columns: 44px minmax(200px, 240px) 92px 82px 74px${taskColumns} minmax(0, 1fr);">
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
                <div class="cell table-column-head sortable-column-head" role="columnheader" data-sort-header="total" aria-sort="none">
                  <button class="table-sort-button" type="button" data-sort-key="total" aria-label="Ordenar por total">
                    <span>Total</span>
                    <span class="table-sort-indicator" aria-hidden="true"></span>
                    <small>${totalPossiblePoints} puntos</small>
                  </button>
                </div>
                <div class="cell table-column-head sortable-column-head" role="columnheader" data-sort-header="percentage" aria-sort="none">
                  <button class="table-sort-button" type="button" data-sort-key="percentage" aria-label="Ordenar por porcentaje">
                    <span>%</span>
                    <span class="table-sort-indicator" aria-hidden="true"></span>
                  </button>
                </div>
                <div class="cell table-column-head sortable-column-head" role="columnheader" data-sort-header="grade" aria-sort="none">
                  <button class="table-sort-button" type="button" data-sort-key="grade" aria-label="Ordenar por nota">
                    <span>Nota</span>
                    <span class="table-sort-indicator" aria-hidden="true"></span>
                  </button>
                </div>
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
                <c:set var="notaClass" value="grade-chip--one" />
                <c:choose>
                  <c:when test="${row.nota == 5}"><c:set var="notaClass" value="grade-chip--five" /></c:when>
                  <c:when test="${row.nota == 4}"><c:set var="notaClass" value="grade-chip--four" /></c:when>
                  <c:when test="${row.nota == 3}"><c:set var="notaClass" value="grade-chip--three" /></c:when>
                  <c:when test="${row.nota == 2}"><c:set var="notaClass" value="grade-chip--two" /></c:when>
                </c:choose>
                <div class="table-row student-row"
                     data-student-row
                     data-student-name="${fn:escapeXml(row.alumnoNombre)}"
                     data-total="${row.total}"
                     data-percentage="${row.porcentaje}"
                     data-grade="${row.nota}"
                     data-original-index="${rs.index}">
                  <div class="cell col-index">${rs.index + 1}</div>
                  <div class="cell col-alumno"><c:out value="${row.alumnoNombre}" /></div>
                  <div class="cell row-summary row-total-cell">
                    <span class="row-total">${row.total}</span>
                    <small>de ${totalPossiblePoints}</small>
                  </div>
                  <div class="cell row-percentage-cell">
                    <span><span class="row-porcentaje">${row.porcentaje}</span>&percnt;</span>
                  </div>
                  <div class="cell row-grade-cell">
                    <span class="grade-chip ${notaClass} student-grade-chip row-nota" aria-label="Nota ${row.nota}">${row.nota}</span>
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
            <div id="studentFilterEmpty" class="planilla-filter-empty" hidden>
              No se encontraron alumnos con ese nombre.
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
  <%@ include file="/WEB-INF/includes/footer-scripts.jspf" %>
</body>

</html>
