<%-- 
    Document   : Tareas
    Created on : Sep 3, 2025, 8:45:48 PM
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
  <title>Tareas</title>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="manifest" href="${pageContext.request.contextPath}/manifest.jsp">
  <meta name="theme-color" content="#1f2d3d">
  <meta name="apple-mobile-web-app-capable" content="yes">
  <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
  <meta name="apple-mobile-web-app-title" content="SCA">
  <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/icons/pwa/apple-touch-icon.png">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/vendor/flat-ui/css/flat-ui.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/ctn-theme.css?v=244">
  <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/images/ctn-logo.svg">
</head>

<body data-user-level="${sessionScope.user.level}" data-specialty="${not empty cursoSpecialty ? cursoSpecialty : (empty sessionScope.scaSpecialty ? 'informatica' : sessionScope.scaSpecialty)}" data-specialty-source="session">
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
        <span>
          <c:out value="${nowFormatted}" />
        </span>
      </div>
      <div class="top-section planilla-hero hero-shell">
        <div class="planilla-hero__header">
          <div class="planilla-hero__info">
            <span class="badge"><span class="dot"></span>Tareas</span>
            <c:choose>
              <c:when test="${not empty editingTarea}"><h1>Modificar tarea</h1></c:when>
              <c:otherwise><h1>Agregar tarea</h1></c:otherwise>
            </c:choose>
            <p class="planilla-subtitle">Carga instrumentos, fechas y puntajes para la planilla seleccionada.</p>
          </div>
        </div>
      </div>
      
      <c:if test="${not empty errors}">
        <c:forEach var="err" items="${errors}">
            <div class="flash-errors" data-timeout="4000">
              <c:out value="${err}" />
              <button type="button" class="flash-close" aria-label="Cerrar mensajes de error">&times;</button>
            </div>
        </c:forEach>
        <c:remove var="flashErrors" scope="session"/>
      </c:if>

      
      <form id="tareaForm" action="${pageContext.request.contextPath}/TareaServlet" method="post">
        <input type="hidden" name="etapa" value="${etapa}" />
        <c:if test="${not empty editingTarea}">
            <input type="hidden" name="planillaId" value="${planillaId}" />
            <input type="hidden" name="tareaId" value="${editingTarea.id}" />
            <input type="hidden" name="_action" value="save" id="_action_input" />
            <input type="hidden" id="originalTotal" name="originalTotal"
                   value="${editingTarea != null ? editingTarea.total : ''}" />
            <input type="hidden" id="clearGrades" name="clearGrades" value="false" />
        </c:if>

        <div class="table-card card tareas-grid">
          <div class="table-header">Etapa</div>
          <div class="cell">
            ${etapaFormated} Etapa - Desde: 30/06/2025 - Hasta: 26/11/2025
          </div>

          <div class="table-header">Materia</div>
          <div class="cell">
            <select name="planillaId" required
                    <c:if test="${not empty editingTarea}">disabled</c:if>>
              <option value="" disabled>Seleccione una materia</option>
              <c:forEach var="p" items="${planillas}">
                  <option value="${p.id}"
                          <c:if test="${p.id == selPlanilla.id}">selected</c:if>>
                    ${p.toString()}
                  </option>
              </c:forEach>
            </select>
          </div>

          <div class="table-header">Instrumento</div>
          <div class="cell">
            <select name="instrumentoId" required>
              <option value="" selected disabled>Seleccione un instrumento</option><!-- TODO -->
              <c:forEach var="ins" items="${instrumentos}">
                  <option value="${ins.id}"
                          <c:if test="${ins.id == instrumentoId}">selected</c:if>>
                    <c:out value="${ins.nombre}" />
                  </option>
              </c:forEach>
            </select>
          </div>

          <div class="table-header">Fecha de tarea</div>
          <div class="cell">
            <input
              type="date"
              name="fecha"
              value="${fecha}"
              required />
          </div>

          <div class="table-header">Total de Puntos</div>
          <div class="cell">
            <input
              class="no-spinner"
              name="total" 
              type="number"
              placeholder="Ingrese el Total de Puntos"
              min="0"
              value="${total}"
              required />
          </div>

          <div class="table-header">Título</div>
          <div class="cell">
            <input
              type="text"
              name="titulo"
              placeholder="Ingrese el Título de la Tarea"
              value="${titulo}"
              required />
          </div>

          <div class="buttons-row table-header">

            <c:url var="backUrl" value="/PlanillaServlet">
                <c:param name="planillaId" value="${planillaId}" />
                <c:param name="etapa" value="${etapa}" />
            </c:url>

            <div class="button-group">
              <!-- Back link (anchor) — not a form submit -->
              <a id="backBtn" class="btn-secondary" href="${backUrl}">
                <img class="back-icon" src="${pageContext.request.contextPath}/icons/back-arrow.svg" alt="Atrás">
                Atrás
              </a>

              <button class="btn-primary save-button" id="saveBtn" type="submit" onclick="document.getElementById('_action_input').value='save'">
                <img class="save-icon" src="${pageContext.request.contextPath}/icons/add.svg">
                <c:choose>
                    <c:when test="${not empty editingTarea}">Guardar</c:when>
                    <c:otherwise>Grabar</c:otherwise>
                </c:choose>
              </button>
    
            </div>
            <c:if test="${not empty editingTarea}">
                <button class="btn-danger" id="deleteBtn" type="submit" onclick="return confirmDelete();" >
                  <img class="delete-icon" src="${pageContext.request.contextPath}/icons/delete-icon.svg">
                  Eliminar
                </button>
            </c:if>

          </div>
        </div>
      </form>


    </section>

    <footer class="footer">
      <hr>
      <p>Colegio Técnico Nacional</p>
    </footer>
  </main>

<script>
  (function () {
    // Find the main planilla form. Adjust selector if necessary.
    const form = document.querySelector('form[action$="/TareaServlet"]') || document.querySelector('form');
    let dirty = false;

    if (form) {
      // Mark dirty when inputs change
      form.addEventListener('input', () => { dirty = true; });
      form.addEventListener('change', () => { dirty = true; });

      // When the user submits (saves), clear dirty
      form.addEventListener('submit', () => { dirty = false; });
    }

    const backBtn = document.getElementById('backBtn');
    if (backBtn) {
      backBtn.addEventListener('click', function (e) {
        if (dirty) {
          // warn and optionally block navigation
          const leave = confirm('Hay cambios sin guardar. ¿Deseas salir sin guardar?');
          if (!leave) {
            e.preventDefault();
          }
        }
      });
    }

    // Prevent accidental tab/window close if dirty
    window.addEventListener('beforeunload', function (e) {
      if (dirty) {
        e.preventDefault();
        // modern browsers ignore the custom message, returning non-empty value is enough
        e.returnValue = '';
      }
    });
  })();
</script>

<script>
(function () {
  const nodes = document.querySelectorAll('.flash, .flash-errors');
  if (!nodes.length) return;

  nodes.forEach(function (el) {
    let timeoutMs = parseInt(el.dataset.timeout, 10);
    if (!Number.isFinite(timeoutMs)) timeoutMs = 4000;

    // start timer
    let timer = setTimeout(() => {
      if (el.classList.contains('flash')) el.classList.add('flash--hide');
      else el.classList.add('flash-errors--hide');
    }, timeoutMs);

    // cleanup after transition
    el.addEventListener('transitionend', function (ev) {
      if (ev.propertyName === 'opacity' || ev.propertyName === 'max-height') {
        try { el.remove(); } catch (e) {}
      }
    });

    // pause on hover (optional nicety)
    el.addEventListener('mouseenter', () => clearTimeout(timer));

    // close button
    const closeBtn = el.querySelector('.flash-close');
    if (closeBtn) {
      closeBtn.addEventListener('click', function (e) {
        e.preventDefault();
        clearTimeout(timer);
        if (el.classList.contains('flash')) el.classList.add('flash--hide');
        else el.classList.add('flash-errors--hide');
      });
    }
  });
})();
</script>
  <script src="${pageContext.request.contextPath}/vendor/flat-ui/js/vendor/jquery.min.js"></script>
  <script src="${pageContext.request.contextPath}/vendor/flat-ui/js/flat-ui.js"></script>
  <script src="${pageContext.request.contextPath}/scripts/sca-theme.js?v=168"></script>
  <script>
    if ('serviceWorker' in navigator) {
      window.addEventListener('load', () => {
        navigator.serviceWorker.register('${pageContext.request.contextPath}/sw.js');
      });
    }
  </script>
<script>
document.getElementById("saveBtn").addEventListener("click", function(){
    document.getElementById("tareaForm").className="submitted";
});
</script>

<script>
function confirmDelete() {
  if (!confirm('¿Desea eliminar esta tarea? Esta acción no se puede deshacer.')) {
    return false;
  }
  // set hidden action value to 'delete' so servlet knows to delete
  var act = document.getElementById('_action_input');
  if (act) act.value = 'delete';
  return true;
}
</script>

<script>
(function () {
  const form = document.getElementById('tareaForm');
  const saveBtn = document.getElementById('saveBtn');
  if (!form || !saveBtn) return;

  function parseIntSafe(v) {
    const n = parseInt(v, 10);
    return Number.isFinite(n) ? n : null;
  }

  saveBtn.addEventListener('click', function (ev) {
    // only run when saving (not when delete)
    // _action_input will be set by onclick attribute already, so check its value:
    const actionInput = document.getElementById('_action_input');
    const action = actionInput ? actionInput.value : 'save';

    if (action !== 'save') return;

    const orig = parseIntSafe(document.getElementById('originalTotal').value);
    const current = parseIntSafe(form.querySelector('input[name="total"]').value);

    // If we are editing an existing tarea (originalTotal non-empty) AND the value changed:
    if (orig !== null && current !== null && orig !== current) {
      const confirmed = confirm(
        'Ha cambiado el total de puntos de ' + orig + ' a ' + current + '.\n' +
        'Se borrarán todas las calificaciones existentes para esta tarea. ¿Desea continuar?'
      );
      if (!confirmed) {
        // prevent submit
        ev.preventDefault();
        return;
      }
      // user accepted -> set hidden flag so server knows (defensive)
      document.getElementById('clearGrades').value = 'true';
    } else {
      // ensure flag is false
      document.getElementById('clearGrades').value = 'false';
    }
    // allow the form to submit normally (the form's onclick for button sets _action_input too)
  });
})();
</script>

</body>

</html>
