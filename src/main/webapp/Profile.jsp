<%-- 
    Document   : Profile
    Created on : Sep 15, 2025, 6:08:05 PM
    Author     : jonat
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE html>
<html data-theme="light">

<head>
  <title>Mi Perfil</title>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="manifest" href="${pageContext.request.contextPath}/manifest.jsp">
  <meta name="theme-color" content="#1f2d3d">
  <meta name="apple-mobile-web-app-capable" content="yes">
  <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
  <meta name="apple-mobile-web-app-title" content="SCA">
  <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/icons/pwa/apple-touch-icon.png">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/vendor/flat-ui/css/flat-ui.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/ctn-theme.css?v=246">
  <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/images/ctn-logo.svg">
  <style>
  .inline-form {
    display: inline-block;
    margin: 0;
  }
  .security-status {
    margin: 0.6rem 0 1rem;
    padding: 0.9rem 1rem;
    border-radius: 0.7rem;
    background: #f8fbff;
    color: #22303f;
    border: 1px solid #dbeafe;
    box-shadow: inset 0 1px 0 rgba(255,255,255,0.6);
  }
  .security-status--enabled {
    border-left: 4px solid #3c8dbc;
  }
  .security-status--disabled {
    border-left: 4px solid #d9534f;
  }
  .security-actions {
    display: flex;
    gap: 0.75rem;
    flex-wrap: wrap;
    align-items: center;
  }
  .security-panel-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 1rem;
    align-items: start;
  }
  @media (max-width: 900px) {
    .security-panel-grid {
      grid-template-columns: 1fr;
    }
  }
  .table-header, .form-card-header {
    font-weight: 700;
    letter-spacing: 0.01em;
  }
  .totp-setup-box {
    padding: 1rem;
    border-radius: 0.4rem;
    background: inherit;
    border: none;
    box-shadow: none;
    margin: 0.5rem 0;
  }
  .totp-qr {
    display: block;
    max-width: 200px;
    width: 100%;
    margin: 1rem auto;
    padding: 0.75rem;
    background: #ffffff;
    border: 1px solid #dbeafe;
    border-radius: 0.4rem;
    box-shadow: none;
  }
  #totpQrCanvas {
    display: flex;
    justify-content: center;
    margin: 1rem 0;
  }
  #totpQrCanvas svg,
  #totpQrCanvas img {
    max-width: 200px;
    width: 100%;
    height: auto;
    padding: 0.75rem;
    background: #ffffff;
    border: 1px solid #dbeafe;
    border-radius: 0.4rem;
  }
  html[data-theme="dark"] .totp-qr,
  html[data-theme="dark"] #totpQrCanvas svg,
  html[data-theme="dark"] #totpQrCanvas img {
    background: #1e293b;
    border-color: #475569;
  }
  .activity-log ul {
    margin: 0;
    padding-left: 1.2rem;
    display: grid;
    gap: 0.6rem;
  }
  .pwa-setup-grid {
    display: grid;
    gap: 1rem;
  }
  .pwa-status-pill {
    display: inline-flex;
    align-items: center;
    width: fit-content;
    padding: 0.35rem 0.7rem;
    border-radius: 999px;
    font-size: 0.85rem;
    font-weight: 700;
    margin-bottom: 0.75rem;
  }
  .pwa-setup-grid > .form-card {
    padding-bottom: 0.75rem;
  }
  .pwa-setup-grid > .form-card > .pwa-status-pill {
    display: flex;
    margin: 0.75rem 0.75rem 0.6rem;
  }
  .pwa-setup-grid > .form-card > p {
    margin: 0.25rem 0.75rem 0.75rem;
    line-height: 1.45;
  }
  .pwa-setup-grid > .form-card > .pwa-install-actions,
  .pwa-setup-grid > .form-card > .security-actions {
    margin: 0.5rem 0.75rem 0;
  }
  .pwa-setup-grid > .form-card > .save-status {
    margin: 0.75rem 0.75rem 0;
  }
  .pwa-status-pill.is-success {
    background: #e7f7ee;
    color: #20663f;
  }
  .pwa-status-pill.is-error {
    background: #fde8e8;
    color: #a11c1c;
  }
  .pwa-status-pill.is-warning {
    background: #fff8e1;
    color: #8a6200;
  }
  .pwa-status-pill.is-info {
    background: #e8f2ff;
    color: #24518d;
  }
  .pwa-install-actions {
    display: flex;
    gap: 0.75rem;
    flex-wrap: wrap;
    margin-top: 0.75rem;
  }
  .pwa-install-instructions {
    margin-top: 0.8rem;
    padding: 0.9rem 1rem;
    border-radius: 0.7rem;
    background: #f8fbff;
    color: #22303f;
    border: 1px solid #dbeafe;
  }
  .pwa-install-instructions strong {
    display: block;
    margin-bottom: 0.35rem;
  }
  .pwa-share-icon {
    display: inline-block;
    width: 1.1rem;
    height: 1.1rem;
    margin-right: 0.35rem;
    vertical-align: text-bottom;
  }
  .security-password-field {
    display: grid;
    gap: 0.5rem;
    padding: 0.75rem 0.75rem 0.15rem;
  }
  .security-password-field label {
    color: color-mix(in srgb, var(--ink) 82%, var(--muted));
    font-size: 0.8rem;
    font-weight: 800;
    line-height: 1.25;
  }
  .security-password-field input {
    width: 100%;
    background: var(--color-bg-input);
    border-color: color-mix(in srgb, var(--line) 78%, var(--ink) 22%);
  }
  .security-password-control {
    position: relative;
  }
  .security-password-control input {
    padding-right: 3.25rem;
  }
  .security-password-toggle,
  .security-password-toggle:hover,
  .security-password-toggle:active {
    position: absolute;
    top: 50%;
    right: 0.35rem;
    width: 2.35rem;
    min-width: 2.35rem;
    height: 2.35rem;
    min-height: 2.35rem;
    padding: 0;
    border: 0;
    border-radius: 0.6rem;
    background: transparent;
    color: var(--muted);
    box-shadow: none;
    filter: none;
    transform: translateY(-50%);
  }
  .security-password-toggle:hover {
    background: var(--accent-soft);
    color: var(--accent-deep);
  }
  .security-password-toggle:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }
  html[data-theme="dark"] .security-password-field label {
    color: color-mix(in srgb, var(--ink) 90%, var(--muted));
  }
  .totp-secret {
    word-break: break-all;
    padding: 0.75rem;
    background: var(--paper, #ffffff);
    border: 1px solid #dbeafe;
    border-radius: 0.4rem;
    margin: 0.5rem 0 0;
    color: var(--ink, #1e293b);
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 0.85rem;
  }
  html[data-theme="dark"] .totp-secret {
    background: #1e293b;
    border-color: #475569;
    color: #e2e8f0;
  }
  </style>
</head>

<body data-page="profile" data-user-level="${sessionScope.user.level}" data-specialty="${empty sessionScope.scaSpecialty ? 'informatica' : sessionScope.scaSpecialty}" data-specialty-source="session">
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

      <c:if test="${not empty sessionScope.flashMessage}">
        <div class="flash" data-timeout="4000">
          ${sessionScope.flashMessage}
          <button type="button" class="flash-close" aria-label="Cerrar mensaje">&times;</button>
        </div>
        <c:remove var="flashMessage" scope="session"/>
      </c:if>

      <c:if test="${not empty errors}">
        <c:forEach var="err" items="${errors}">
            <div class="flash-errors" data-timeout="4000">
              <c:out value="${err}" />
              <button type="button" class="flash-close" aria-label="Cerrar mensajes de error">&times;</button>
            </div>
        </c:forEach>
        <c:remove var="errors" scope="session"/>
      </c:if>

      <div class="profile-layout">
        <c:url var="HomeUrl" value="/HomeServlet" />
        <div class="profile-shell">
          <aside class="profile-sidebar" aria-label="Navegación de perfil">
            <div class="profile-role-banner">
              <span class="profile-role-label">Rol actual</span>
              <strong><c:out value="${profileRoleLabel}" /></strong>
              <p><c:out value="${profileAccessDescription}" /></p>
            </div>
            <div class="profile-tabs" role="tablist" aria-label="Secciones del perfil">
              <button type="button" class="profile-tab active" data-target="perfil-panel" role="tab" aria-controls="perfil-panel" aria-selected="true">
                <span>Perfil</span>
                <small>Datos personales</small>
              </button>
              <button type="button" class="profile-tab" data-target="seguridad-panel" role="tab" aria-controls="seguridad-panel" aria-selected="false">
                <span>Seguridad</span>
                <small>Contraseña</small>
              </button>
              <c:if test="${showMateriasPanel}">
                <button type="button" class="profile-tab" data-target="materias-panel" role="tab" aria-controls="materias-panel" aria-selected="false">
                  <span>Materias</span>
                  <small>Asignaciones</small>
                </button>
              </c:if>
              <button type="button" class="profile-tab" data-target="pwa-panel" role="tab" aria-controls="pwa-panel" aria-selected="false">
                <span>App / Notif</span>
                <small>Instalación</small>
              </button>
              <button type="button" class="profile-tab" data-target="registros-panel" role="tab" aria-controls="registros-panel" aria-selected="false">
                <span>Registros</span>
                <small>Actividad</small>
              </button>
            </div>
            <a id="backBtn" class="profile-back-link" href="${HomeUrl}">
              <img class="back-icon" src="${pageContext.request.contextPath}/icons/back-arrow.svg" alt="Atrás">
              <span>Volver al inicio</span>
            </a>
          </aside>
          <div class="profile-content">
          <div class="profile-panels">
            <section id="perfil-panel" class="profile-panel active">
              <c:choose>
                <c:when test="${isStaffProfile}">
                  <form id="profileForm" action="${pageContext.request.contextPath}/ProfileServlet" method="post" data-status-target="profileSaveStatus">
                    <input type="hidden" name="action" value="saveProfile" />
                    <div class="profile-grid profile-grid-layout">
                      <div class="form-card card">
                        <div class="form-card-header">Información Personal</div>
                        <div class="form-field">
                          <label for="nombre">Nombre</label>
                          <c:choose>
                            <c:when test="${canEditAdminOnlyProfileFields}">
                              <input type="text" id="nombre" name="nombre" value="${profesor.nombre}" />
                            </c:when>
                            <c:otherwise>
                              <input type="text" id="nombre" value="${profesor.nombre}" disabled />
                            </c:otherwise>
                          </c:choose>
                        </div>
                        <div class="form-field">
                          <label for="apellido">Apellido</label>
                          <c:choose>
                            <c:when test="${canEditAdminOnlyProfileFields}">
                              <input type="text" id="apellido" name="apellido" value="${profesor.apellido}" />
                            </c:when>
                            <c:otherwise>
                              <input type="text" id="apellido" value="${profesor.apellido}" disabled />
                            </c:otherwise>
                          </c:choose>
                        </div>
                        <div class="form-field">
                          <label for="ci">Cédula</label>
                          <c:choose>
                            <c:when test="${canEditAdminOnlyProfileFields}">
                              <input type="text" id="ci" name="ci" value="${profesor.ci}" />
                            </c:when>
                            <c:otherwise>
                              <input type="text" id="ci" value="${profesor.ci}" disabled />
                            </c:otherwise>
                          </c:choose>
                        </div>
                      </div>
                      <div class="form-card card">
                        <div class="form-card-header">Contacto</div>
                        <div class="form-field">
                          <label for="correo">Correo electrónico</label>
                          <input type="email" id="correo" name="correo" value="${profesor.correo}" />
                        </div>
                        <div class="form-field">
                          <label for="telefono">Teléfono</label>
                          <input class="no-spinner" type="number" id="telefono" name="telefono" value="${profesor.telefono}" />
                        </div>
                        <div class="form-field">
                          <label for="celular">Celular</label>
                          <input class="no-spinner" type="number" id="celular" name="celular" value="${profesor.celular}" />
                        </div>
                      </div>
                      <div class="form-card card">
                        <div class="form-card-header">Cuenta</div>
                        <div class="form-field">
                          <label for="usuario">Usuario</label>
                          <input type="text" id="usuario" name="usuario" value="${profesor.usuario}" />
                        </div>
                      </div>
                      <c:if test="${showGoogleClassroomPanel}">
                        <div class="form-card card">
                          <div class="form-card-header">Google Classroom</div>
                          <c:url var="googleConnectUrl" value="/GoogleLoginServlet" />
                          <c:url var="googleDisconnectUrl" value="/GoogleDisconnectServlet" />
                          <c:choose>
                            <c:when test="${not empty profesor.googleEmail or not empty profesor.gcAccessToken}">
                              <div class="classroom-status classroom-status--connected">Conectado como <strong><c:out value="${profesor.googleEmail}"/></strong></div>
                              <div class="classroom-actions">
                                <a class="btn-primary" href="${pageContext.request.contextPath}${googleConnectUrl}">Reconectar</a>
                                <button class="btn-danger" type="submit" form="googleDisconnectForm">Desconectar</button>
                              </div>
                            </c:when>
                            <c:otherwise>
                              <div class="classroom-status classroom-status--disconnected">No conectado a Google Classroom</div>
                              <a class="btn-primary" href="${pageContext.request.contextPath}${googleConnectUrl}">Conectar ahora</a>
                            </c:otherwise>
                          </c:choose>
                        </div>
                      </c:if>
                      <span id="profileSaveStatus" class="save-status profile-save-status" aria-live="polite">Guardado automático activo.</span>
                    </div>
                  </form>
                  <form id="googleDisconnectForm" action="${pageContext.request.contextPath}${googleDisconnectUrl}" method="post" style="display:none;"></form>
                </c:when>
                <c:when test="${isParentProfile}">
                  <form id="profileForm" action="${pageContext.request.contextPath}/ProfileServlet" method="post" data-status-target="profileSaveStatus">
                    <input type="hidden" name="action" value="saveProfile" />
                    <div class="profile-grid profile-grid-layout">
                      <div class="form-card card">
                        <div class="form-card-header">Información Personal</div>
                        <div class="form-field">
                          <label for="padreNombre">Nombre</label>
                          <c:choose>
                            <c:when test="${canEditAdminOnlyProfileFields}">
                              <input type="text" id="padreNombre" name="nombre" value="${empty profileOwner.nombre ? '' : profileOwner.nombre}" />
                            </c:when>
                            <c:otherwise>
                              <input type="text" id="padreNombre" value="${empty profileOwner.fullName ? sessionScope.user.fullName : profileOwner.fullName}" disabled />
                            </c:otherwise>
                          </c:choose>
                        </div>
                        <div class="form-field">
                          <label for="padreApellido">Apellido</label>
                          <c:choose>
                            <c:when test="${canEditAdminOnlyProfileFields}">
                              <input type="text" id="padreApellido" name="apellido" value="${empty profileOwner.apellido ? '' : profileOwner.apellido}" />
                            </c:when>
                            <c:otherwise>
                              <input type="text" id="padreApellido" value="${empty profileOwner.apellido ? '' : profileOwner.apellido}" disabled />
                            </c:otherwise>
                          </c:choose>
                        </div>
                        <div class="form-field">
                          <label for="padreCI">Cédula</label>
                          <c:choose>
                            <c:when test="${canEditAdminOnlyProfileFields}">
                              <input type="text" id="padreCI" name="ci" value="${empty profileOwner.ci ? '' : profileOwner.ci}" />
                            </c:when>
                            <c:otherwise>
                              <input type="text" id="padreCI" value="${empty profileOwner.ci ? 'No registrada' : profileOwner.ci}" disabled />
                            </c:otherwise>
                          </c:choose>
                        </div>
                      </div>
                      <div class="form-card card">
                        <div class="form-card-header">Contacto</div>
                        <div class="form-field">
                          <label for="correo">Correo electrónico</label>
                          <input type="email" id="correo" name="correo" value="${empty profileOwner.correo ? '' : profileOwner.correo}" />
                        </div>
                        <div class="form-field">
                          <label for="telefono">Teléfono</label>
                          <input type="text" id="telefono" name="telefono" value="${empty profileOwner.telefono ? '' : profileOwner.telefono}" />
                        </div>
                      </div>
                      <div class="form-card card">
                        <div class="form-card-header">Cuenta</div>
                        <div class="form-field">
                          <label for="usuario">Usuario</label>
                          <input type="text" id="usuario" name="usuario" value="${empty profileOwner.usuario ? sessionScope.user.username : profileOwner.usuario}" />
                        </div>
                      </div>
                      <span id="profileSaveStatus" class="save-status profile-save-status" aria-live="polite">Guardado automático activo.</span>
                    </div>
                  </form>
                </c:when>
                <c:otherwise>
                  <div class="profile-grid profile-grid-layout">
                    <div class="form-card card">
                      <div class="form-card-header">Información Personal</div>
                      <div class="profile-role-pill"><c:out value="${profileRoleLabel}" /></div>
                      <div class="form-field">
                        <label>Vista</label>
                        <input type="text" value="Consulta de perfil" disabled />
                      </div>
                      <div class="form-field">
                        <label>Nombre</label>
                        <input type="text" value="${empty profileOwner.fullName ? sessionScope.user.fullName : profileOwner.fullName}" disabled />
                      </div>
                      <div class="form-field">
                        <label>Cédula</label>
                        <input type="text" value="${empty profileOwner.ci ? 'No registrada' : profileOwner.ci}" disabled />
                      </div>
                    </div>
                    <div class="form-card card">
                      <div class="form-card-header">Cuenta</div>
                      <div class="form-field">
                        <label>Usuario</label>
                        <input type="text" value="${empty profileOwner.usuario ? sessionScope.user.username : profileOwner.usuario}" disabled />
                      </div>
                      <div class="form-field">
                        <label>Correo electrónico</label>
                        <input type="email" value="${empty profileOwner.correo ? 'No registrado' : profileOwner.correo}" disabled />
                      </div>
                      <div class="form-field">
                        <label>Teléfono</label>
                        <input type="text" value="${empty profileOwner.telefono ? 'No registrado' : profileOwner.telefono}" disabled />
                      </div>
                    </div>
                    <div class="form-card card">
                      <div class="form-card-header">Acceso</div>
                      <div class="profile-access-card">
                        <strong><c:out value="${profileRoleLabel}" /></strong>
                        <p><c:out value="${profileAccessDescription}" /></p>
                      </div>
                    </div>
                    <div class="form-card card">
                      <div class="form-card-header">Permisos</div>
                      <div class="profile-permissions-list">
                        <c:choose>
                          <c:when test="${isProfessorProfile}">
                            <span>Edición de datos personales</span>
                            <span>Gestión de Google Classroom</span>
                            <span>Visualización de materias y asignaciones</span>
                          </c:when>
                          <c:when test="${isParentProfile}">
                            <span>Consulta de información familiar</span>
                            <span>Vista académica asociada a estudiantes</span>
                          </c:when>
                          <c:otherwise>
                            <span>Consulta de perfil</span>
                            <span>Acceso a seguridad y registros</span>
                          </c:otherwise>
                        </c:choose>
                      </div>
                    </div>
                  </div>
                </c:otherwise>
              </c:choose>
            </section>

            <section id="seguridad-panel" class="profile-panel" hidden>
              <div class="security-panel-grid">
                <form id="securityForm" class="security-password-form" action="${pageContext.request.contextPath}/ProfileServlet" method="post" data-status-target="securitySaveStatus">
                  <input type="hidden" name="action" value="changePassword" />
                  <div class="table-card card">
                    <div class="table-header">Cambiar Contraseña</div>
                    <div class="security-password-field">
                      <label for="currentPassword">Contraseña Actual</label>
                      <div class="security-password-control">
                        <input type="password" name="currentPassword" id="currentPassword" autocomplete="current-password" />
                        <button class="password-toggle security-password-toggle" type="button"
                                data-password-target="currentPassword" aria-controls="currentPassword"
                                aria-pressed="false" aria-label="Mostrar contraseña" title="Mostrar contraseña">
                          <svg class="password-toggle__icon password-toggle__icon--show" viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M2.25 12s3.5-6 9.75-6 9.75 6 9.75 6-3.5 6-9.75 6-9.75-6-9.75-6Z"></path>
                            <circle cx="12" cy="12" r="2.75"></circle>
                          </svg>
                          <svg class="password-toggle__icon password-toggle__icon--hide" viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M3 3l18 18"></path>
                            <path d="M10.6 6.2A10.7 10.7 0 0 1 12 6c6.25 0 9.75 6 9.75 6a16.7 16.7 0 0 1-2.2 2.8M6.3 7.1C3.7 9 2.25 12 2.25 12s3.5 6 9.75 6a10.8 10.8 0 0 0 4.1-.8M9.8 9.8a3.1 3.1 0 0 0 4.4 4.4"></path>
                          </svg>
                        </button>
                      </div>
                    </div>
                    <div class="security-password-field">
                      <label for="newPassword">Nueva Contraseña</label>
                      <div class="security-password-control">
                        <input type="password" name="newPassword" id="newPassword" autocomplete="new-password" />
                        <button class="password-toggle security-password-toggle" type="button"
                                data-password-target="newPassword" aria-controls="newPassword"
                                aria-pressed="false" aria-label="Mostrar contraseña" title="Mostrar contraseña">
                          <svg class="password-toggle__icon password-toggle__icon--show" viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M2.25 12s3.5-6 9.75-6 9.75 6 9.75 6-3.5 6-9.75 6-9.75-6-9.75-6Z"></path>
                            <circle cx="12" cy="12" r="2.75"></circle>
                          </svg>
                          <svg class="password-toggle__icon password-toggle__icon--hide" viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M3 3l18 18"></path>
                            <path d="M10.6 6.2A10.7 10.7 0 0 1 12 6c6.25 0 9.75 6 9.75 6a16.7 16.7 0 0 1-2.2 2.8M6.3 7.1C3.7 9 2.25 12 2.25 12s3.5 6 9.75 6a10.8 10.8 0 0 0 4.1-.8M9.8 9.8a3.1 3.1 0 0 0 4.4 4.4"></path>
                          </svg>
                        </button>
                      </div>
                    </div>
                    <div class="security-password-field">
                      <label for="confirmPassword">Confirmar Contraseña</label>
                      <div class="security-password-control">
                        <input type="password" name="confirmPassword" id="confirmPassword" autocomplete="new-password" />
                        <button class="password-toggle security-password-toggle" type="button"
                                data-password-target="confirmPassword" aria-controls="confirmPassword"
                                aria-pressed="false" aria-label="Mostrar contraseña" title="Mostrar contraseña">
                          <svg class="password-toggle__icon password-toggle__icon--show" viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M2.25 12s3.5-6 9.75-6 9.75 6 9.75 6-3.5 6-9.75 6-9.75-6-9.75-6Z"></path>
                            <circle cx="12" cy="12" r="2.75"></circle>
                          </svg>
                          <svg class="password-toggle__icon password-toggle__icon--hide" viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M3 3l18 18"></path>
                            <path d="M10.6 6.2A10.7 10.7 0 0 1 12 6c6.25 0 9.75 6 9.75 6a16.7 16.7 0 0 1-2.2 2.8M6.3 7.1C3.7 9 2.25 12 2.25 12s3.5 6 9.75 6a10.8 10.8 0 0 0 4.1-.8M9.8 9.8a3.1 3.1 0 0 0 4.4 4.4"></path>
                          </svg>
                        </button>
                      </div>
                    </div>
                    <div class="cell selection-hint" style="grid-column: 1 / -1;">
                      Completa solo si quieres cambiar la contraseña. Debe tener al menos 6 caracteres.
                    </div>
                    <div class="cell" style="grid-column: 1 / -1;">
                      <button class="btn-primary save-button" type="submit">Cambiar Contraseña</button>
                      <span id="securitySaveStatus" class="save-status" aria-live="polite">Listo.</span>
                    </div>
                  </div>
                </form>

                <div class="form-card card">
                  <div class="form-card-header">Seguridad adicional</div>
                  <div class="form-field">
                    <strong>Autenticación de dos factores (2FA)</strong>
                    <c:choose>
                      <c:when test="${totpEnabled}">
                        <div class="security-status security-status--enabled">2FA activado en esta cuenta.</div>
                        <p>Usa tu app de autenticación para generar el código de inicio de sesión.</p>
                      </c:when>
                      <c:otherwise>
                        <div class="security-status security-status--disabled">2FA no está activo.</div>
                        <p>Activa 2FA para proteger tu cuenta con un código extra al iniciar sesión.</p>
                      </c:otherwise>
                    </c:choose>
                  </div>
                  <div class="form-field security-actions">
                    <c:choose>
                      <c:when test="${totpEnabled}">
                        <button class="btn-danger" type="submit" form="disableTotpForm">Desactivar 2FA</button>
                      </c:when>
                      <c:otherwise>
                        <button class="btn-secondary" type="submit" form="prepareTotpForm">Configurar 2FA</button>
                      </c:otherwise>
                    </c:choose>
                  </div>
                  <c:if test="${not empty pendingTotpSecret}">
                    <div class="form-field">
                      <div class="totp-setup-box">
                        <p>Escanea el código QR con tu app de autenticación o copia el secreto manualmente:</p>
                        <div id="totpQrCanvas" aria-label="Código QR de 2FA"></div>
                        <div class="totp-secret"><c:out value="${pendingTotpSecret}" /></div>
                      </div>
                    </div>
                    <form id="confirmTotpForm" action="${pageContext.request.contextPath}/ProfileServlet" method="post" style="margin:0;">
                      <input type="hidden" name="action" value="confirmTotp" />
                      <div class="form-field">
                        <label for="totpSetupCode">Código de la app</label>
                        <input type="text" id="totpSetupCode" name="totpSetupCode" maxlength="6" inputmode="numeric" autocomplete="one-time-code" required />
                      </div>
                      <div class="form-field security-actions">
                        <button class="btn-primary" id="confirmTotpButton" type="submit">Confirmar activación</button>
                      </div>
                    </form>
                  </c:if>
                </div>
              </div>

              <form id="prepareTotpForm" action="${pageContext.request.contextPath}/ProfileServlet" method="post" style="margin:0; display:none;">
                <input type="hidden" name="action" value="prepareTotp" />
              </form>
              <form id="disableTotpForm" action="${pageContext.request.contextPath}/ProfileServlet" method="post" style="margin:0; display:none;">
                <input type="hidden" name="action" value="disableTotp" />
              </form>
            </section>

        <c:if test="${showMateriasPanel}">
          <section id="materias-panel" class="profile-panel" hidden>
            <div class="table-card table-card--wide card">
              <div class="table-header">Asignaciones de materias</div>
              <div class="subject-list-grid">
                <c:choose>
                  <c:when test="${empty misAsignaciones}">
                    <p class="empty-state">No hay asignaciones de materias registradas.</p>
                  </c:when>
                  <c:otherwise>
                    <div class="subject-list-header">
                      <span>Materia</span>
                      <span>Curso</span>
                    </div>
                    <c:forEach var="asignacion" items="${misAsignaciones}">
                      <div class="subject-item">
                        <span class="subject-item__name"><c:out value="${asignacion.materiaNombre}" /></span>
                        <span class="subject-item__course"><c:out value="${asignacion.cursoDescripcion}" /></span>
                      </div>
                    </c:forEach>
                  </c:otherwise>
                </c:choose>
              </div>
            </div>
          </section>
        </c:if>

        <section id="pwa-panel" class="profile-panel" hidden>
          <div class="pwa-setup-grid">
            <div class="form-card card">
              <div class="form-card-header">Instalar la app</div>
              <div id="installAppState" class="pwa-status-pill is-info" aria-live="polite">Comprobando compatibilidad…</div>
              <p id="installAppMessage">Revisamos si tu navegador permite instalar la PWA desde este perfil.</p>
              <div id="installAppActions" class="pwa-install-actions">
                <button class="btn-primary" id="installAppButton" type="button" style="display:none;">Instalar app</button>
              </div>
              <div id="installAppHint" class="pwa-install-instructions" style="display:none;"></div>
            </div>

            <div class="form-card card">
              <div class="form-card-header">Activar notificaciones</div>
              <p>Usa este bloque para gestionar las alertas del navegador sin depender de un flag guardado en sesión.</p>
              <div class="security-actions">
                <button class="btn-secondary" id="enablePushButton" type="button">Activar notificaciones</button>
                <button class="btn-secondary" id="testPushButton" type="button">Enviar prueba</button>
                <button class="btn-danger" id="disablePushButton" type="button">Desactivar</button>
              </div>
              <div id="pushStateBadge" class="pwa-status-pill is-info" aria-live="polite">Sin confirmar</div>
              <div id="pushStatus" class="save-status is-success" aria-live="polite">Listo para activar.</div>
            </div>
          </div>
        </section>

        <section id="registros-panel" class="profile-panel" hidden>
          <div class="activity-log">
            <c:choose>
              <c:when test="${empty activityLog}">
                <p class="empty-state">Aún no hay movimientos registrados.</p>
              </c:when>
              <c:otherwise>
                <ul>
                  <c:forEach var="entry" items="${activityLog}">
                    <li><c:out value="${entry}" /></li>
                  </c:forEach>
                </ul>
              </c:otherwise>
            </c:choose>
          </div>
        </section>
      </div>
    </div>
  </div>
    </section>

    <footer class="footer">
      <hr>
      <p>Colegio Técnico Nacional</p>
    </footer>
  </main>

<script>
(function () {
  const tabs = document.querySelectorAll('.profile-tab');
  const panels = document.querySelectorAll('.profile-panel');
  const storageKey = 'ctn-profile-active-tab';

  function activateTab(tab) {
    tabs.forEach(function (item) {
      item.classList.remove('active');
      item.setAttribute('aria-selected', 'false');
    });
    panels.forEach(function (panel) {
      panel.classList.remove('active');
      panel.hidden = true;
    });
    tab.classList.add('active');
    tab.setAttribute('aria-selected', 'true');
    const target = document.getElementById(tab.dataset.target);
    if (target) {
      target.classList.add('active');
      target.hidden = false;
    }
    try {
      localStorage.setItem(storageKey, tab.dataset.target);
    } catch (e) {
      // ignore localStorage failures
    }
  }

  const savedTarget = (() => {
    try {
      return localStorage.getItem(storageKey);
    } catch (e) {
      return null;
    }
  })();
  const initialTab = Array.from(tabs).find(function (t) {
    return t.dataset.target === savedTarget;
  }) || tabs[0];
  if (initialTab) {
    activateTab(initialTab);
  }

  tabs.forEach(function (tab) {
    tab.addEventListener('click', function () {
      activateTab(tab);
    });
  });
})();
</script>

<script>
(function () {
  const profileForm = document.getElementById('profileForm');
  const securityForm = document.getElementById('securityForm');
  const confirmTotpForm = document.getElementById('confirmTotpForm');
  const totpQrCanvas = document.getElementById('totpQrCanvas');
  const totpProvisioningUri = '<c:out value="${totpProvisioningUri}" />';
  const enablePushButton = document.getElementById('enablePushButton');
  const disablePushButton = document.getElementById('disablePushButton');
  const testPushButton = document.getElementById('testPushButton');
  const pushStatus = document.getElementById('pushStatus');
  const pushStateBadge = document.getElementById('pushStateBadge');
  const installAppState = document.getElementById('installAppState');
  const installAppButton = document.getElementById('installAppButton');
  const installAppMessage = document.getElementById('installAppMessage');
  const installAppHint = document.getElementById('installAppHint');
  const securityPasswordToggles = document.querySelectorAll('.security-password-toggle[data-password-target]');
  const vapidPublicKey = '${pushPublicKey}';
  window.ctnProfilePushEnabled = <c:out value="${pushEnabled}" />;
  let deferredPrompt = null;

  function setSecurityPasswordVisibility(button, visible) {
    const input = document.getElementById(button.dataset.passwordTarget);
    if (!input) return;
    input.type = visible ? 'text' : 'password';
    button.classList.toggle('is-visible', visible);
    button.setAttribute('aria-pressed', String(visible));
    button.setAttribute('aria-label', visible ? 'Ocultar contraseña' : 'Mostrar contraseña');
    button.setAttribute('title', visible ? 'Ocultar contraseña' : 'Mostrar contraseña');
  }

  securityPasswordToggles.forEach(function (button) {
    button.addEventListener('click', function () {
      setSecurityPasswordVisibility(button, button.getAttribute('aria-pressed') !== 'true');
    });
  });

  function setPushStatus(message, tone) {
    if (!pushStatus) return;
    pushStatus.textContent = message;
    pushStatus.className = 'save-status ' + tone;
  }

  function setPushBadge(message, tone) {
    if (!pushStateBadge) return;
    pushStateBadge.textContent = message;
    pushStateBadge.className = 'pwa-status-pill ' + tone;
  }

  function updatePushButtonsOnly() {
    const permission = typeof Notification !== 'undefined' ? Notification.permission : 'default';
    const serverSubscribed = Boolean(window.ctnProfilePushEnabled);

    if (!enablePushButton || !disablePushButton || !testPushButton) return;

    if (permission === 'denied') {
      enablePushButton.style.display = 'none';
      disablePushButton.style.display = 'none';
      testPushButton.style.display = 'none';
      return;
    }

    if (permission === 'granted' && serverSubscribed) {
      enablePushButton.style.display = 'none';
      disablePushButton.style.display = 'inline-flex';
      testPushButton.style.display = 'inline-flex';
      return;
    }

    if (permission === 'granted') {
      enablePushButton.style.display = 'inline-flex';
      enablePushButton.textContent = 'Reintentar';
      disablePushButton.style.display = 'none';
      testPushButton.style.display = 'none';
      return;
    }

    enablePushButton.style.display = 'inline-flex';
    enablePushButton.textContent = 'Activar notificaciones';
    disablePushButton.style.display = 'none';
    testPushButton.style.display = 'none';
  }

  function syncPushUi() {
    const permission = typeof Notification !== 'undefined' ? Notification.permission : 'default';
    const serverSubscribed = Boolean(window.ctnProfilePushEnabled);
    updatePushButtonsOnly();

    if (permission === 'denied') {
      setPushBadge('Bloqueado', 'is-error');
      setPushStatus('Las notificaciones están bloqueadas. Habilítalas desde la configuración del navegador.', 'is-error');
      return;
    }

    if (permission === 'granted' && serverSubscribed) {
      setPushBadge('Activado', 'is-success');
      setPushStatus('Notificaciones activadas para esta cuenta.', 'is-success');
      return;
    }

    if (permission === 'granted') {
      setPushBadge('Permiso concedido', 'is-warning');
      setPushStatus('El permiso está activo, pero la suscripción aún no quedó registrada.', 'is-warning');
      return;
    }

    setPushBadge('No activado', 'is-info');
    setPushStatus('Listo para activar.', 'is-success');
  }

  function updateInstallUi() {
    if (!installAppState || !installAppButton || !installAppMessage || !installAppHint) return;

    const ua = navigator.userAgent || '';
    const isStandalone = window.matchMedia('(display-mode: standalone)').matches || Boolean(window.navigator.standalone);
    const isIOS = /iPad|iPhone|iPod/.test(ua);
    const isSafari = /Safari/.test(ua) && !/Chrome|CriOS|Edg|Android/.test(ua);
    const isChromeLike = /Chrome|CriOS|Edg\//.test(ua) && !/iPhone|iPad|iPod/.test(ua);

    if (isStandalone) {
      installAppState.textContent = 'Instalada';
      installAppState.className = 'pwa-status-pill is-success';
      installAppMessage.textContent = 'La app ya se está ejecutando en modo instalada.';
      installAppButton.style.display = 'none';
      installAppHint.style.display = 'none';
      installAppHint.innerHTML = '';
      return;
    }

    if (isIOS && isSafari) {
      installAppState.textContent = 'Paso manual';
      installAppState.className = 'pwa-status-pill is-info';
      installAppMessage.textContent = 'Safari iOS no ofrece un prompt de instalación programable desde la web.';
      installAppButton.style.display = 'none';
      installAppHint.style.display = 'block';
      installAppHint.innerHTML = '<strong>Agregar a inicio</strong><span><span class="pwa-share-icon" aria-hidden="true">⤴</span>Tocá el ícono de compartir y elegí “Agregar a inicio”.</span>';
      return;
    }

    if (!isChromeLike) {
      installAppState.textContent = 'No disponible';
      installAppState.className = 'pwa-status-pill is-info';
      installAppMessage.textContent = 'Este navegador no ofrece instalación de PWA en este contexto.';
      installAppButton.style.display = 'none';
      installAppHint.style.display = 'none';
      installAppHint.innerHTML = '';
      return;
    }

    if (deferredPrompt) {
      installAppState.textContent = 'Listo para instalar';
      installAppState.className = 'pwa-status-pill is-info';
      installAppMessage.textContent = 'Tu navegador ya está preparado para instalar la app.';
      installAppButton.style.display = 'inline-flex';
      installAppHint.style.display = 'none';
      installAppHint.innerHTML = '';
      return;
    }

    installAppState.textContent = 'Disponible';
    installAppState.className = 'pwa-status-pill is-info';
    installAppMessage.textContent = 'Este navegador admite la instalación. Usa el botón cuando el prompt esté disponible.';
    installAppButton.style.display = 'none';
    installAppHint.style.display = 'none';
    installAppHint.innerHTML = '';
  }

  function urlBase64ToUint8Array(base64String) {
    const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
    const normalized = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
    const rawData = window.atob(normalized);
    const output = new Uint8Array(rawData.length);
    for (let i = 0; i < rawData.length; i += 1) {
      output[i] = rawData.charCodeAt(i);
    }
    return output;
  }

  async function subscribeToPush() {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
      setPushStatus('Tu navegador no soporta notificaciones push.', 'is-error');
      return;
    }
    if (!vapidPublicKey) {
      setPushStatus('La clave pública VAPID no está configurada.', 'is-error');
      return;
    }
    try {
      const registration = await navigator.serviceWorker.ready;
      const permission = await Notification.requestPermission();
      if (permission !== 'granted') {
        setPushStatus('Se necesita permiso para mostrar notificaciones.', 'is-error');
        return;
      }
      const existingSubscription = await registration.pushManager.getSubscription();
      if (existingSubscription) {
        await existingSubscription.unsubscribe();
      }
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(vapidPublicKey)
      });
      const p256dhKey = subscription.getKey('p256dh');
      const authKey = subscription.getKey('auth');
      const response = await fetch('${pageContext.request.contextPath}/PushSubscriptionServlet', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
        body: new URLSearchParams({
          action: 'save',
          endpoint: subscription.endpoint,
          p256dh: p256dhKey ? btoa(String.fromCharCode(...new Uint8Array(p256dhKey))) : '',
          auth: authKey ? btoa(String.fromCharCode(...new Uint8Array(authKey))) : ''
        })
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'No se pudo guardar la suscripción.');
      }
      window.ctnProfilePushEnabled = true;
      setPushStatus(payload.message || 'Suscripción guardada.', 'is-success');
      syncPushUi();
    } catch (error) {
      setPushStatus(error.message || 'No se pudo activar.', 'is-error');
      updatePushButtonsOnly();
    }
  }

  async function unsubscribeFromPush() {
    try {
      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.getSubscription();
      if (subscription) {
        await subscription.unsubscribe();
      }
      const response = await fetch('${pageContext.request.contextPath}/PushSubscriptionServlet', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
        body: new URLSearchParams({ action: 'unsubscribe' })
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'No se pudo eliminar la suscripción.');
      }
      window.ctnProfilePushEnabled = false;
      setPushStatus(payload.message || 'Suscripción eliminada.', 'is-success');
      syncPushUi();
    } catch (error) {
      setPushStatus(error.message || 'No se pudo desactivar.', 'is-error');
      updatePushButtonsOnly();
    }
  }

  async function sendPushTest() {
    try {
      const response = await fetch('${pageContext.request.contextPath}/PushSubscriptionServlet', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
        body: new URLSearchParams({ action: 'test' })
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'No se pudo enviar la prueba.');
      }
      setPushStatus(payload.message || 'Prueba enviada.', 'is-success');
    } catch (error) {
      setPushStatus(error.message || 'No se pudo enviar la prueba.', 'is-error');
    }
  }

  function resolveAjaxPayload(responseText, response) {
    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
      try {
        return JSON.parse(responseText);
      } catch (e) {
        return { success: response.ok, message: responseText || 'Operación completada.' };
      }
    }
    return { success: response.ok, message: responseText || 'Operación completada.' };
  }

  function setStatus(el, message, tone) {
    if (!el) return;
    el.textContent = message;
    el.className = 'save-status ' + tone;
  }

  function submitWithFetch(form, statusId, successMessage, isSecurityForm) {
    const statusEl = document.getElementById(statusId);

    const formData = new FormData(form);
    const body = new URLSearchParams();
    for (const [key, value] of formData.entries()) {
      body.append(key, value);
    }
    setStatus(statusEl, 'Guardando...', 'is-saving');

    fetch(form.getAttribute('action'), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
        'X-Requested-With': 'XMLHttpRequest'
      },
      body: body
    }).then(async function (response) {
      const responseText = await response.text();
      const payload = resolveAjaxPayload(responseText, response);
      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'No se pudo guardar la información.');
      }

      if (isSecurityForm) {
        ['currentPassword', 'newPassword', 'confirmPassword'].forEach(function (id) {
          const input = document.getElementById(id);
          if (input) input.value = '';
        });
        securityPasswordToggles.forEach(function (button) {
          setSecurityPasswordVisibility(button, false);
        });
      }

      setStatus(statusEl, payload.message || successMessage, 'is-success');
    }).catch(function (error) {
      setStatus(statusEl, error.message || 'No se pudo guardar.', 'is-error');
    });
  }

  if (profileForm) {
    let autoSaveTimer;
    const profileStatus = document.getElementById('profileSaveStatus');
    function scheduleProfileSave() {
      clearTimeout(autoSaveTimer);
      autoSaveTimer = setTimeout(function () {
        submitWithFetch(profileForm, 'profileSaveStatus', 'Datos guardados.', false);
      }, 800);
    }

    profileForm.addEventListener('input', function (event) {
      if (!['correo', 'telefono', 'celular', 'usuario', 'especialidadId', 'nombre', 'apellido', 'ci', 'nivel'].includes(event.target.name)) return;
      scheduleProfileSave();
    });

    profileForm.addEventListener('change', function (event) {
      if (!['correo', 'telefono', 'celular', 'usuario', 'especialidadId', 'nombre', 'apellido', 'ci', 'nivel'].includes(event.target.name)) return;
      scheduleProfileSave();
    });

    profileStatus && setStatus(profileStatus, 'Guardado automático activo.', 'is-success');
  }

  if (securityForm) {
    const securityStatus = document.getElementById('securitySaveStatus');
    securityForm.addEventListener('submit', function (event) {
      event.preventDefault();
      submitWithFetch(securityForm, 'securitySaveStatus', 'Contraseña actualizada.', true);
    });
    securityStatus && setStatus(securityStatus, 'Listo para guardar.', 'is-success');
  }

  if (confirmTotpForm) {
    confirmTotpForm.addEventListener('submit', function (event) {
      const codeInput = document.getElementById('totpSetupCode');
      if (codeInput && codeInput.value.trim().length < 6) {
        event.preventDefault();
        codeInput.focus();
        return;
      }
    });
  }

  if (totpQrCanvas && totpProvisioningUri) {
    try {
      if (typeof window.qrcode === 'function') {
        const qr = window.qrcode(0, 'M');
        qr.addData(totpProvisioningUri);
        qr.make();
        totpQrCanvas.innerHTML = qr.createSvgTag({ scalable: true });
      }
    } catch (error) {
      console.error('No se pudo renderizar el QR de 2FA', error);
    }
  }

  if (installAppButton) {
    installAppButton.addEventListener('click', async function () {
      if (!deferredPrompt) {
        updateInstallUi();
        return;
      }
      deferredPrompt.prompt();
      try {
        await deferredPrompt.userChoice;
      } catch (error) {
        // Ignore prompt errors and keep UI responsive.
      }
      deferredPrompt = null;
      updateInstallUi();
    });
  }

  window.addEventListener('beforeinstallprompt', function (event) {
    event.preventDefault();
    deferredPrompt = event;
    updateInstallUi();
  });

  window.addEventListener('appinstalled', function () {
    deferredPrompt = null;
    updateInstallUi();
  });

  if (enablePushButton) {
    enablePushButton.addEventListener('click', function () {
      subscribeToPush();
    });
  }
  if (disablePushButton) {
    disablePushButton.addEventListener('click', function () {
      unsubscribeFromPush();
    });
  }
  if (testPushButton) {
    testPushButton.addEventListener('click', function () {
      sendPushTest();
    });
  }

  updateInstallUi();
  syncPushUi();
})();
</script>

<script>
(function () {
  const nodes = document.querySelectorAll('.flash, .flash-errors');
  if (!nodes.length) return;

  nodes.forEach(function (el) {
    let timeoutMs = parseInt(el.dataset.timeout, 10);
    if (!Number.isFinite(timeoutMs)) timeoutMs = 4000;

    let timer = setTimeout(() => {
      if (el.classList.contains('flash')) el.classList.add('flash--hide');
      else el.classList.add('flash-errors--hide');
    }, timeoutMs);

    el.addEventListener('transitionend', function (ev) {
      if (ev.propertyName === 'opacity' || ev.propertyName === 'max-height') {
        try { el.remove(); } catch (e) {}
      }
    });

    el.addEventListener('mouseenter', () => clearTimeout(timer));

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
  <script src="${pageContext.request.contextPath}/scripts/qrcode.js"></script>
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
