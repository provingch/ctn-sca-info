<%-- 
    Document   : index
    Created on : Aug 5, 2025, 5:25:54 PM
    Author     : jonat
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.Optional"%>
<%@page import="jakarta.servlet.http.Cookie"%>
<%@page import="ctn.informatica.sca.dao.UserDao"%>
<%@page import="ctn.informatica.sca.dao.ProfesorDao"%>
<%@page import="ctn.informatica.sca.dao.PadreDao"%>
<%@page import="ctn.informatica.sca.dao.EspecialidadDao"%>
<%@page import="ctn.informatica.sca.model.User"%>
<%@page import="ctn.informatica.sca.model.Profesor"%>
<%@page import="ctn.informatica.sca.model.Padre"%>
<%@page import="ctn.informatica.sca.model.Especialidad"%>
<%@page import="ctn.informatica.sca.util.RememberMeTokenStore"%>
<%@page import="ctn.informatica.sca.util.ScaUiContext"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
    HttpSession existingSession = request.getSession(false);
    User currentUser = existingSession == null ? null : (User) existingSession.getAttribute("user");
    if (currentUser == null) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("SCA_REMEMBER".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    String token = cookie.getValue().trim();
                    Integer userId = RememberMeTokenStore.resolveUserId(token).orElse(null);
                    if (userId != null) {
                        User restoredUser = new UserDao().findById(userId);
                        if (restoredUser != null) {
                            HttpSession restoredSession = request.getSession(true);
                            restoredSession.setMaxInactiveInterval(60 * 60 * 24 * 7);
                            restoredSession.setAttribute("user", restoredUser);
                            try {
                                Profesor profesor = new ProfesorDao().findById(restoredUser.getId());
                                restoredSession.setAttribute("profesor", profesor);
                                String specialty = "informatica";
                                if (profesor != null && profesor.getEspecialidadId() != null) {
                                    Especialidad especialidad = new EspecialidadDao().findById(profesor.getEspecialidadId());
                                    if (especialidad != null && especialidad.getNombre() != null && !especialidad.getNombre().isBlank()) {
                                        specialty = ScaUiContext.normalizeSpecialty(especialidad.getNombre());
                                    }
                                }
                                restoredSession.setAttribute("scaSpecialty", specialty);
                            } catch (Exception ignored) {
                                // no-op
                            }
                            try {
                                Padre padre = new PadreDao().findById(restoredUser.getId());
                                if (padre != null) {
                                    restoredSession.setAttribute("padre", padre);
                                }
                            } catch (Exception ignored) {
                                // no-op
                            }
                            String redirectTarget;
                            switch (restoredUser.getLevel()) {
                                case 1:
                                    redirectTarget = "/inicio";
                                    break;
                                case 2:
                                    redirectTarget = "/evaluacion";
                                    break;
                                case 3:
                                    redirectTarget = "/admin";
                                    break;
                                case 4:
                                    redirectTarget = "/padre";
                                    break;
                                default:
                                    redirectTarget = "/index.jsp";
                            }
                            response.sendRedirect(request.getContextPath() + redirectTarget);
                            return;
                        }
                    }
                    Cookie expiredCookie = new Cookie("SCA_REMEMBER", "");
                    expiredCookie.setMaxAge(0);
                    expiredCookie.setPath(request.getContextPath().isBlank() ? "/" : request.getContextPath());
                    expiredCookie.setHttpOnly(true);
                    expiredCookie.setSecure(request.isSecure());
                    response.addCookie(expiredCookie);
                    break;
                }
            }
        }
    } else {
        String redirectTarget;
        switch (currentUser.getLevel()) {
            case 1:
                redirectTarget = "/inicio";
                break;
            case 2:
                redirectTarget = "/evaluacion";
                break;
            case 3:
                redirectTarget = "/admin";
                break;
            case 4:
                redirectTarget = "/padre";
                break;
            default:
                redirectTarget = "/index.jsp";
        }
        response.sendRedirect(request.getContextPath() + redirectTarget);
        return;
    }
%>

<!DOCTYPE html>
<html data-theme="light">

  <c:set var="pageTitle" value="Sistema de Carpeta Academica" scope="request" />
  <c:set var="headExtraFragment" value="/WEB-INF/includes/head-extra-index.jspf" scope="request" />
  <%@ include file="/WEB-INF/includes/head.jspf" %>

  <!-- as convention the class names must be in english -->

  <body class="login-page" data-user-level="${sessionScope.user.level}" data-specialty="${empty sessionScope.scaSpecialty ? 'general' : sessionScope.scaSpecialty}" data-specialty-source="session">
    <c:set var="navbarHomeUrl" value="${pageContext.request.contextPath}/index.jsp" />
    <c:set var="navbarShowSessionMenu" value="false" />
    <%@ include file="/WEB-INF/includes/navbar.jspf" %>

    <main class="login-main">
      <div class="login-wrapper">
        <div class="login-card">
          <div class="login-logo-container">
            <img class="login-logo" src="${pageContext.request.contextPath}/images/ctn-logo-2.svg">
          </div>
          <div class="login-heading">
            <h1>Iniciar sesi&oacute;n</h1>
            <p>Sistema de informes acad&eacute;micos</p>
          </div>
          <details class="login-about">
            <summary>
              <span class="login-about__icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" focusable="false">
                  <circle cx="12" cy="12" r="9"></circle>
                  <path d="M12 10.5v6"></path>
                  <circle cx="12" cy="7.5" r="1"></circle>
                </svg>
              </span>
              <span>Acerca de este sistema</span>
              <span class="login-about__chevron" aria-hidden="true"></span>
            </summary>
            <%@ include file="/WEB-INF/includes/app-transparency.jspf" %>
          </details>
          <c:if test="${loginError}">
              <div class="login-error">Nombre de usuario o contrase&ntilde;a incorrectos.</div>
          </c:if>
          <c:if test="${param.notice == 'login-required'}">
              <div class="login-info">Inicia sesi&oacute;n para ver tus planillas y cursos. Si est&aacute;s corrigiendo la vinculaci&oacute;n de alumnos, entra con tu usuario de integraci&oacute;n tras iniciar sesi&oacute;n.</div>
          </c:if>
          <form class="login-form" action="${pageContext.request.contextPath}/login" method="post">
            <input class="form-username" placeholder="Usuario" type="text" name="username" autocomplete="username">
            <div class="password-field">
              <input class="form-password" id="loginPassword" placeholder="Contrase&ntilde;a" type="password" name="password" autocomplete="current-password">
              <button class="password-toggle" id="passwordToggle" type="button"
                      aria-controls="loginPassword" aria-pressed="false"
                      aria-label="Mostrar contrase&ntilde;a" title="Mostrar contrase&ntilde;a">
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
            <div class="login-remember">
              <label>
                <input type="checkbox" name="rememberMe" value="true">
                <span>Mantener sesi&oacute;n</span>
              </label>
            </div>
            <input class="form-submit" type="submit" value="Iniciar Sesi&oacute;n">
          </form>
        </div>
      </div>
    </main>

    <footer class="footer">
      <hr>
          <p>Colegio T&eacute;cnico Nacional</p>
    <p><a href="${pageContext.request.contextPath}/privacidad">Pol&iacute;tica de privacidad</a> | <a href="${pageContext.request.contextPath}/terminos">T&eacute;rminos de servicio</a></p>
    </footer>

    <!-- Cookie Consent Banner -->
    <div id="cookieConsent" class="cookie-consent-banner" role="banner">
      <div class="cookie-consent-content">
        <div class="cookie-consent-text">
          <strong>Cookies funcionales</strong>
          <p>Usamos cookies necesarias para iniciar sesi&oacute;n, mantener la sesi&oacute;n activa y recordar tu preferencia de tema. Si marc&aacute;s &ldquo;Mantener sesi&oacute;n&rdquo;, estas cookies son obligatorias.</p>
        </div>
        <div class="cookie-consent-actions">
          <button id="acceptCookies" class="cookie-consent-btn cookie-consent-btn-primary">Entendido</button>
        </div>
      </div>
    </div>
  <%@ include file="/WEB-INF/includes/footer-scripts.jspf" %>
    <script src="${pageContext.request.contextPath}/scripts/cookie-consent.js?v=164"></script>
  <script>
    (function () {
      const passwordInput = document.getElementById('loginPassword');
      const passwordToggle = document.getElementById('passwordToggle');
      if (!passwordInput || !passwordToggle) return;

      passwordToggle.addEventListener('click', function () {
        const showPassword = passwordInput.type === 'password';
        passwordInput.type = showPassword ? 'text' : 'password';
        passwordToggle.classList.toggle('is-visible', showPassword);
        passwordToggle.setAttribute('aria-pressed', String(showPassword));
        passwordToggle.setAttribute('aria-label', showPassword ? 'Ocultar contrase\u00f1a' : 'Mostrar contrase\u00f1a');
        passwordToggle.title = showPassword ? 'Ocultar contrase\u00f1a' : 'Mostrar contrase\u00f1a';
        passwordInput.focus({ preventScroll: true });
      });
    })();
  </script>
  </body>

</html>
