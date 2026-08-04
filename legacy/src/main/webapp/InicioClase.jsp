<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<c:set var="pageTitle" value="Iniciar clase" scope="request" />
<c:set var="headExtraFragment" value="/WEB-INF/includes/head-style-inicioclase.jspf" scope="request" />
<%@ include file="/WEB-INF/includes/head.jspf" %>

<body data-user-level="${sessionScope.user.level}" data-specialty="${empty sessionScope.scaSpecialty ? 'general' : sessionScope.scaSpecialty}" data-specialty-source="session">
  <c:set var="manualHrefOverride" value="${pageContext.request.contextPath}/pdfs/manual-profesor.pdf" />
  <%@ include file="/WEB-INF/includes/navbar.jspf" %>

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
        <div class="menu-container">
          <form id="classSelectionForm" action="${pageContext.request.contextPath}/inicio" method="get" class="curso-selection-form">
            <label for="classEspecialidad">Especialidad</label>
            <select id="classEspecialidad" name="especialidad"></select>
            <label for="classCursoNivel">Curso</label>
            <select id="classCursoNivel" name="promocion" disabled></select>
            <label for="classSeccion">Sección</label>
            <select id="classSeccion" name="seccion" disabled></select>
            <input type="hidden" name="cursoId" id="classCursoIdHidden" value="${empty selCurso ? '' : selCurso.id}" />
            <input type="hidden" name="etapa" value="${selEtapa}" />
            <input type="hidden" name="view" value="clase" />
          </form>
        </div>
      </div>

      <c:set var="planillaCount" value="${fn:length(planillas)}" />
      <c:url var="planillasTabAction" value="/inicio" />
      <form class="home-view-tabs" action="${pageContext.request.contextPath}/inicio" method="get" role="tablist" aria-label="Vista principal del curso">
        <input type="hidden" name="cursoId" id="tabCursoId" value="${empty selCurso ? '' : selCurso.id}" />
        <input type="hidden" name="etapa" value="${selEtapa}" />
        <c:if test="${planillaCount == 1}">
          <input type="hidden" name="planillaId" value="${planillas[0].id}" />
          <input type="hidden" name="materiaId" value="${planillas[0].materiaId}" />
        </c:if>
        <button type="submit" name="view" value="clase" class="home-view-tab home-view-tab--primary is-active" role="tab" aria-selected="true" aria-controls="clase-panel">
          <span class="home-view-tab__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" focusable="false"><path d="M7 4.8v14.4L19 12 7 4.8Z"/></svg>
          </span>
          <span class="home-view-tab__copy">
            <strong>Iniciar clase</strong>
            <small id="classTabSubtitle">
              <c:choose>
                <c:when test="${not empty selCurso}">${selCurso.especialidad} · ${selCurso.curso}° ${selCurso.seccion} · hoy</c:when>
                <c:otherwise>Seleccioná un curso y una sección</c:otherwise>
              </c:choose>
            </small>
          </span>
        </button>
        <button type="submit" name="view" value="planillas" formaction="${planillasTabAction}" class="home-view-tab" role="tab" aria-selected="false" aria-controls="planillas-panel">
          <span class="home-view-tab__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" focusable="false"><path d="M9 5h2.1a3 3 0 0 1 5.8 0H19a2 2 0 0 1 2 2v13a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h2.1A3 3 0 0 1 9 5Zm3-1.5A1.5 1.5 0 1 0 12 6a1.5 1.5 0 0 0 0-3ZM7 10v2h2v-2H7Zm4 0v2h6v-2h-6Zm-4 5v2h2v-2H7Zm4 0v2h6v-2h-6Z"/></svg>
          </span>
          <span class="home-view-tab__copy">
            <strong>Planillas de puntaje</strong>
            <small id="planillasTabSubtitle">
              <c:choose>
                <c:when test="${planillaCount == 1}"><c:out value="${planillas[0].nombre}" /></c:when>
                <c:when test="${planillaCount > 1}">${planillaCount} planillas filtradas</c:when>
                <c:otherwise>Sin planillas para este filtro</c:otherwise>
              </c:choose>
            </small>
          </span>
        </button>
      </form>

      <div class="class-view-toolbar">
        <a class="btn btn-default back-button" href="${pageContext.request.contextPath}/inicio">← Volver al inicio</a>
      </div>

      <div id="filterPendingState" class="empty-state empty-state-card home-filter-pending" role="status" hidden>
        Completá Especialidad, Curso y Sección para iniciar la clase.
      </div>
      <div id="clase-panel" class="home-tab-panel" role="tabpanel">
        <div class="inicio-shell class-session-summary">
          <span class="subject-card__chip">Nueva sesión</span>
          <h2 class="inicio-hero-title">
            <c:choose>
              <c:when test="${not empty selCurso}">${selCurso.especialidad} ${selCurso.curso}° ${selCurso.seccion}</c:when>
              <c:otherwise>Seleccioná el contexto de la clase</c:otherwise>
            </c:choose>
          </h2>
          <p class="inicio-hero-subtitle"><c:out value="${nowFormatted}" /></p>
        </div>

      <c:if test="${param.rasgoError eq 'tema'}">
        <div class="empty-state empty-state-card">Debes seleccionar curso, instrumento y tema a desarrollar.</div>
      </c:if>
      <c:if test="${param.rasgoError eq 'sin-alumnos'}">
        <div class="empty-state empty-state-card">No hay alumnos con nombre y apellido válidos para el curso seleccionado.</div>
      </c:if>
      <c:if test="${param.rasgoOk eq 'created'}">
        <div class="empty-state empty-state-card">Clase registrada correctamente con asistencia inicial.</div>
      </c:if>
      <c:if test="${not empty rasgoErrorMessage}">
        <div class="empty-state empty-state-card"><c:out value="${rasgoErrorMessage}" /></div>
      </c:if>

      <form action="${pageContext.request.contextPath}/inicio" method="post" style="display:grid; gap:12px;">
        <input type="hidden" name="action" value="create-rasgo-planilla" />
        <input type="hidden" name="cursoId" value="${empty selCurso ? '' : selCurso.id}" id="formCursoId" />
        <input type="hidden" name="turno" value="${param.turno}" id="formTurno" />
        <input type="hidden" name="etapa" value="${selEtapa}" />

        <div class="class-card">
          <div class="class-card-head">
            <h3>Datos de clase</h3>
            <button type="button" class="btn btn-default btn-sm" id="clearButton">Limpiar formulario</button>
          </div>
          <div class="class-grid">
            <div>
              <label for="horarioClase" style="font-weight:600;">Horario</label>
              <input id="horarioClase" class="form-control" placeholder="Ej: 07:00-09:20" pattern="^([0-1]?\d|2[0-4]):[0-5]\d-([0-1]?\d|2[0-4]):[0-5]\d$" title="Formato HH:MM-HH:MM, entre 07:00 y 18:00" inputmode="numeric" maxlength="11" />
            </div>
            <div>
              <label for="cantidadHoras" style="font-weight:600;">Cant. horas cátedra</label>
              <input id="cantidadHoras" type="number" min="1" max="12" step="1" inputmode="numeric" class="form-control" readonly placeholder="Automático" value="" />
            </div>
            <div>
              <label for="modalidadClase" style="font-weight:600;">Modalidad</label>
              <select id="modalidadClase" class="form-control">
                <option>Presencial</option>
                <option>Virtual</option>
              </select>
            </div>
            <div>
              <label for="instrumentoId" style="font-weight:600;">Tipo de clase (Instrumento)</label>
              <select id="instrumentoId" name="instrumentoId" class="form-control" required>
                <option value="">Seleccione instrumento</option>
                <c:forEach var="instrumento" items="${instrumentos}">
                  <option value="${instrumento.id}"><c:out value="${instrumento.nombre}" /></option>
                </c:forEach>
              </select>
            </div>
            <div>
              <label for="temaRasgo" style="font-weight:600;">Contenido específico desarrollado</label>
              <input id="temaRasgo" name="tema" class="form-control" maxlength="150" value="${fn:escapeXml(param.tema)}" placeholder="Ej.: Integrales definidas y aplicaciones" required />
            </div>
            <div style="grid-column: 1 / -1;">
              <label for="observacionesGenerales" style="font-weight:600;">Observaciones generales</label>
              <textarea id="observacionesGenerales" class="form-control" rows="3" placeholder="Cualquier eventualidad general de la clase..."></textarea>
            </div>
          </div>
        </div>

        <div class="class-card">
          <h3>3. Asistencia general y justificativos</h3>
          <div style="margin-bottom:10px;">
            <span class="student-pill">Habilitados: <strong>${fn:length(rasgoAlumnosValidos)}</strong></span>
            <span class="student-pill">Incompletos: <strong>${fn:length(rasgoAlumnosInvalidos)}</strong></span>
          </div>
          <div class="empty-state empty-state-card" style="text-align:left; margin-bottom:8px;">
            Marca ausentes en la lista. Los no marcados se guardan como presentes.
          </div>
          <div class="table-responsive" style="margin-bottom:8px;">
            <table class="table table-striped" id="tablaAsistencia">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Apellido(s) y nombre(s)</th>
                  <th style="text-align:right; width:140px;">Estado (P/A)</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="alumno" items="${rasgoAlumnosValidos}" varStatus="loop">
                  <tr>
                    <td>${loop.index + 1}</td>
                    <td>
                      <c:out value="${alumno.apellido}" />, <c:out value="${alumno.nombre}" />
                    </td>
                    <td style="text-align:right;">
                      <label style="margin:0;display:flex;align-items:center;gap:6px;font-weight:500;justify-content:flex-end;">
                        <input type="checkbox" name="alumnosAusentes" value="${alumno.id}" class="ausente-checkbox" />
                        Ausente
                      </label>
                    </td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </div>

        <div class="class-card">
          <h3>4. Reportes de asistencia</h3>
          <div class="class-grid" style="grid-template-columns: 220px minmax(0,1fr);">
            <button type="button" class="btn btn-default" id="reportButton">Generar reporte de asistencia</button>
            <div id="reportBox" class="empty-state empty-state-card" style="text-align:left;">Aún no hay resumen de asistencia.</div>
          </div>
        </div>

        <div class="class-card" style="display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
          <button type="submit" class="btn btn-primary">Guardar inicio de clase</button>
          <button type="button" class="btn btn-default" id="exportButton">Ver datos JSON generados</button>
        </div>

        <pre id="resultOutput" class="result-output"></pre>
      </form>

      <c:if test="${not empty rasgoAlumnosInvalidos}">
        <div class="class-card" style="margin-top:12px;">
          <h3>Alumnos con datos incompletos</h3>
          <div class="table-responsive">
            <table class="table table-striped">
              <thead>
                <tr><th>Alumno</th></tr>
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
        </div>
      </c:if>

      <c:if test="${not empty rasgoPlanillas}">
        <div class="class-card" style="margin-top:12px;">
          <h3>Historial de clases registradas</h3>
          <div style="margin-bottom:10px;">
            <label for="rasgoPlanillaSel" style="font-weight:600;">Clase</label>
            <select id="rasgoPlanillaSel" class="form-control" style="max-width:520px;" onchange="location.href='${pageContext.request.contextPath}/inicio?view=clase&cursoId=${selCurso.id}&etapa=${selEtapa}&turno=${fn:escapeXml(param.turno)}&rasgoPlanillaId=' + this.value;">
              <c:forEach var="rp" items="${rasgoPlanillas}">
                <option value="${rp.id}" ${not empty rasgoPlanillaSeleccionada and rasgoPlanillaSeleccionada.id == rp.id ? 'selected' : ''}>${rp.tema} - ${rp.fechaClase}</option>
              </c:forEach>
            </select>
          </div>
          <div class="table-responsive">
            <table class="table table-striped">
              <thead>
                <tr>
                  <th>Alumno</th>
                  <th>Estado</th>
                  <th>Código</th>
                  <th>Observación</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="asistencia" items="${rasgoAsistencias}">
                  <tr>
                    <td>
                      <a href="${pageContext.request.contextPath}/inicio?view=rasgos-form&asistenciaId=${asistencia.id}" style="font-weight:600; color:var(--accent);">
                        <c:out value="${asistencia.alumnoNombreCompleto}" />
                      </a>
                    </td>
                    <td><c:out value="${asistencia.estado}" /></td>
                    <td><c:out value="${empty asistencia.faltaCodigo ? '—' : asistencia.faltaCodigo}" /></td>
                    <td><c:out value="${empty asistencia.faltaObservacion ? '—' : asistencia.faltaObservacion}" /></td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </div>
      </c:if>
      </div>
    </section>

    <footer class="footer">
      <hr>
          <p>Colegio T&eacute;cnico Nacional</p>
    <p><a href="${pageContext.request.contextPath}/privacidad">Pol&iacute;tica de privacidad</a> | <a href="${pageContext.request.contextPath}/terminos">T&eacute;rminos de servicio</a></p>
    </footer>
  </main>

  <script>
  const CURSOS = [
      <c:forEach var="cu" items="${cursos}" varStatus="s">
          {"id": ${cu.id}, "especialidad": "<c:out value='${cu.especialidad}'/>", "nivel": ${cu.curso}, "seccion": "<c:out value='${cu.seccion}'/>"}<c:if test="${!s.last}">,</c:if>
      </c:forEach>
  ];

  (function () {
    function normalizeSpecialty(value) {
      const normalized = String(value || '')
        .trim()
        .toLowerCase()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/[_\s]+/g, '-')
        .replace(/[^a-z0-9-]/g, '')
        .replace(/-+/g, '-')
        .replace(/^-|-$/g, '');

      const aliases = {
        'construcciones-civiles': 'construcciones',
        'construccion-civil': 'construcciones',
        'quimica-industrial': 'quimica',
        'mecanica-automotriz': 'mecanica-automotriz',
        'mecanica-general': 'mecanica-general'
      };

      return aliases[normalized] || normalized || 'informatica';
    }

    function applySpecialtyToPage(specialtyName) {
      const normalized = normalizeSpecialty(specialtyName);
      document.body.setAttribute('data-specialty', normalized);
    }

    function updateFilterPreview(especialidad, nivel, seccion, cursoId) {
      const tabCursoId = document.getElementById('tabCursoId');
      const postCursoId = document.getElementById('formCursoId');
      const classSubtitle = document.getElementById('classTabSubtitle');
      const planillasSubtitle = document.getElementById('planillasTabSubtitle');
      const classPanel = document.getElementById('clase-panel');
      const pendingState = document.getElementById('filterPendingState');
      const tabButtons = document.querySelectorAll('.home-view-tab');
      const hasCompleteFilter = Boolean(cursoId);

      if (tabCursoId) {
        tabCursoId.value = cursoId || '';
      }
      if (postCursoId) {
        postCursoId.value = cursoId || '';
      }
      tabButtons.forEach(button => {
        button.disabled = !hasCompleteFilter;
      });

      if (hasCompleteFilter) {
        if (classSubtitle) {
          classSubtitle.textContent = especialidad + ' · ' + nivel + '° ' + seccion + ' · hoy';
        }
        if (classPanel) {
          classPanel.hidden = false;
        }
        if (pendingState) {
          pendingState.hidden = true;
        }
        return;
      }

      if (classSubtitle) {
        classSubtitle.textContent = 'Seleccioná un curso y una sección';
      }
      if (planillasSubtitle) {
        planillasSubtitle.textContent = 'Sin planillas para este filtro';
      }
      if (classPanel) {
        classPanel.hidden = true;
      }
      if (pendingState) {
        pendingState.hidden = false;
      }
    }

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
        selEspecialidad.appendChild(new Option('Seleccione especialidad', ''));
        uniqueEspecialidades().forEach(e => selEspecialidad.appendChild(new Option(e, e)));
        selCursoNivel.innerHTML = '<option value="">Seleccione curso</option>';
        selCursoNivel.disabled = true;
        selSeccion.innerHTML = '<option value="">Seleccione sección</option>';
        selSeccion.disabled = true;
      }

      function populateCursoNivel() {
        const esp = selEspecialidad.value;
        selCursoNivel.innerHTML = '';
        selCursoNivel.appendChild(new Option('Seleccione curso', ''));
        if (!esp) {
          selCursoNivel.disabled = true;
          selSeccion.innerHTML = '<option value="">Seleccione sección</option>';
          selSeccion.disabled = true;
          return;
        }
        const niveles = [...new Set(CURSOS.filter(c => c.especialidad === esp).map(c => c.nivel))].sort((a, b) => a - b);
        niveles.forEach(n => selCursoNivel.appendChild(new Option(formatCursoNivel(n), n)));
        selCursoNivel.disabled = false;
        selSeccion.innerHTML = '<option value="">Seleccione sección</option>';
        selSeccion.disabled = true;
      }

      function populateSeccion() {
        const esp = selEspecialidad.value;
        const nivel = parseInt(selCursoNivel.value, 10);
        selSeccion.innerHTML = '';
        selSeccion.appendChild(new Option('Seleccione sección', ''));
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
        const nivel = parseInt(selCursoNivel.value, 10);
        const seccion = selSeccion.value;
        applySpecialtyToPage(esp || 'general');
        cursoIdHidden.value = '';
        if (!esp || !nivel || !seccion) {
          updateFilterPreview(esp, nivel || '', seccion, '');
          if (config.onCursoChanged) {
            config.onCursoChanged('');
          }
          return;
        }
        const found = CURSOS.find(c => c.especialidad === esp && c.nivel === nivel && c.seccion === seccion);
        if (found) {
          cursoIdHidden.value = found.id;
          updateFilterPreview(found.especialidad, found.nivel, found.seccion, String(found.id));
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
        applySpecialtyToPage(found.especialidad);
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
    if (!selectedCursoId) {
      applySpecialtyToPage(document.body.getAttribute('data-specialty'));
    }
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

    const reportButton = document.getElementById('reportButton');
    const reportBox = document.getElementById('reportBox');
    if (reportButton && reportBox) {
      reportButton.addEventListener('click', function () {
        const ausentes = Array.from(document.querySelectorAll('.ausente-checkbox')).filter(c => c.checked).length;
        const total = document.querySelectorAll('.ausente-checkbox').length;
        const presentes = Math.max(total - ausentes, 0);
        const porcentaje = total > 0 ? Math.round((presentes * 100) / total) : 0;
        reportBox.textContent = 'Total alumnos: ' + total
          + ' | Presentes: ' + presentes
          + ' | Ausentes: ' + ausentes
          + ' | Asistencia: ' + porcentaje + '%';
      });
    }

    const clearButton = document.getElementById('clearButton');
    if (clearButton) {
      clearButton.addEventListener('click', function () {
        const tema = document.getElementById('temaRasgo');
        const horario = document.getElementById('horarioClase');
        const cantidadHoras = document.getElementById('cantidadHoras');
        const modalidad = document.getElementById('modalidadClase');
        const observaciones = document.getElementById('observacionesGenerales');
        const checkboxes = document.querySelectorAll('.ausente-checkbox');
        if (tema) tema.value = '';
        if (horario) {
          horario.value = '';
          horario.setCustomValidity('');
        }
        if (cantidadHoras) cantidadHoras.value = '';
        if (modalidad) modalidad.value = 'Presencial';
        if (observaciones) observaciones.value = '';
        checkboxes.forEach(c => { c.checked = false; });
      });
    }

    const clampHorarioSegment = function (segment, max) {
      if (segment.length !== 2) return segment;
      const normalized = Number(segment);
      if (Number.isNaN(normalized)) return segment;
      return String(Math.min(max, normalized)).padStart(2, '0');
    };

    const formatHorarioInput = function (value) {
      const digits = value.replace(/\D/g, '').slice(0, 8);
      const hour1 = digits.slice(0, 2);
      const min1 = digits.slice(2, 4);
      const hour2 = digits.slice(4, 6);
      const min2 = digits.slice(6, 8);

      const formattedHour1 = clampHorarioSegment(hour1, 24);
      const formattedMin1 = clampHorarioSegment(min1, 59);
      const formattedHour2 = clampHorarioSegment(hour2, 24);
      const formattedMin2 = clampHorarioSegment(min2, 59);

      let result = '';
      if (formattedHour1) {
        result += formattedHour1;
      }
      if (digits.length > 2) {
        result += ':' + formattedMin1;
      }
      if (digits.length > 4) {
        result += '-' + formattedHour2;
      }
      if (digits.length > 6) {
        result += ':' + formattedMin2;
      }
      return result.slice(0, 11);
    };

    const updateCantidadHorasField = function (value) {
      const cantidadHoras = document.getElementById('cantidadHoras');
      if (!cantidadHoras) return;
      const match = value.match(/^(\d{1,2}):(\d{2})-(\d{1,2}):(\d{2})$/);
      if (!match) {
        cantidadHoras.value = '';
        return;
      }
      const startHour = Number(match[1]);
      const startMin = Number(match[2]);
      const endHour = Number(match[3]);
      const endMin = Number(match[4]);
      const start = startHour * 60 + startMin;
      const end = endHour * 60 + endMin;
      if (end <= start) {
        cantidadHoras.value = '';
        return;
      }
      const durationMins = end - start;
      cantidadHoras.value = String(Math.min(12, Math.max(1, Math.ceil(durationMins / 35))));
    };

    const validateHorarioField = function () {
      const horario = document.getElementById('horarioClase');
      if (!horario) return;
      const value = horario.value.trim();
      if (value === '') {
        horario.setCustomValidity('');
        updateCantidadHorasField('');
        return;
      }
      const match = value.match(/^(\d{1,2}):(\d{2})-(\d{1,2}):(\d{2})$/);
      if (!match) {
        horario.setCustomValidity('Formato inválido: use HH:MM-HH:MM');
        updateCantidadHorasField('');
        return;
      }
      const startHour = Number(match[1]);
      const startMin = Number(match[2]);
      const endHour = Number(match[3]);
      const endMin = Number(match[4]);
      if (startHour > 24 || endHour > 24 || startMin > 59 || endMin > 59) {
        horario.setCustomValidity('Formato inválido: hora hasta 24 y minutos hasta 59');
        updateCantidadHorasField('');
        return;
      }
      const start = startHour * 60 + startMin;
      const end = endHour * 60 + endMin;
      const minTime = 7 * 60;
      const maxTime = 18 * 60;
      if (start < minTime || start > maxTime || end < minTime || end > maxTime) {
        horario.setCustomValidity('Las horas deben estar entre 07:00 y 18:00');
        updateCantidadHorasField('');
        return;
      }
      if (end <= start) {
        horario.setCustomValidity('La hora de inicio debe ser anterior a la hora de fin');
        updateCantidadHorasField('');
        return;
      }
      horario.setCustomValidity('');
      updateCantidadHorasField(value);
    };

    const horarioField = document.getElementById('horarioClase');
    if (horarioField) {
      horarioField.addEventListener('blur', validateHorarioField);
      horarioField.addEventListener('input', function () {
        this.value = formatHorarioInput(this.value);
        if (this.value.length === 11) {
          validateHorarioField();
        }
      });
    }

    const exportButton = document.getElementById('exportButton');
    const resultOutput = document.getElementById('resultOutput');
    if (exportButton && resultOutput) {
      exportButton.addEventListener('click', function () {
        const ausentes = Array.from(document.querySelectorAll('.ausente-checkbox'))
          .filter(c => c.checked)
          .map(c => Number(c.value));
        const payload = {
          cursoId: Number(document.getElementById('formCursoId') ? document.getElementById('formCursoId').value : 0),
          instrumentoId: Number(document.getElementById('instrumentoId') ? document.getElementById('instrumentoId').value : 0),
          tema: document.getElementById('temaRasgo') ? document.getElementById('temaRasgo').value : '',
          horarioClase: document.getElementById('horarioClase') ? document.getElementById('horarioClase').value : '',
          cantidadHoras: document.getElementById('cantidadHoras') ? document.getElementById('cantidadHoras').value : '',
          modalidad: document.getElementById('modalidadClase') ? document.getElementById('modalidadClase').value : '',
          observaciones: document.getElementById('observacionesGenerales') ? document.getElementById('observacionesGenerales').value : '',
          alumnosAusentes: ausentes
        };
        resultOutput.textContent = JSON.stringify(payload, null, 2);
        resultOutput.style.display = 'block';
      });
    }

    const now = new Date();
  })();
  </script>

  <%@ include file="/WEB-INF/includes/footer-scripts.jspf" %>
</body>
</html>
