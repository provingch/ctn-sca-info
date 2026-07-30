<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
  if (session == null || session.getAttribute("user") == null) {
    response.sendRedirect(request.getContextPath() + "/index.jsp");
    return;
  }
%>
<!doctype html>
<html data-theme="light">
<head>
  <meta charset="UTF-8" />
  <title>Styleguide — Foundation</title>
  <link rel="manifest" href="${pageContext.request.contextPath}/manifest.jsp">
  <meta name="theme-color" content="#1f2d3d">
  <meta name="apple-mobile-web-app-capable" content="yes">
  <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
  <meta name="apple-mobile-web-app-title" content="SCA">
  <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/icons/pwa/apple-touch-icon.png">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/vendor/flat-ui/css/flat-ui.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/ctn-theme.css?v=250">
  <style>body{padding:24px}</style>
</head>
<body data-user-level="${sessionScope.user.level}" data-specialty="${empty sessionScope.scaSpecialty ? 'informatica' : sessionScope.scaSpecialty}" data-specialty-source="session">
  <h1>Design System — Tokens & Components</h1>
  <section>
    <h2>Specialty Accent Preview</h2>
    <p>Change specialty to preview institutional accents.</p>
    <select id="specialtySelect">
      <option value="">(default)</option>
      <option value="informatica">informatica</option>
      <option value="construcciones">construcciones</option>
      <option value="quimica">quimica</option>
      <option value="electronica">electronica</option>
      <option value="mecanica-automotriz">mecanica-automotriz</option>
      <option value="mecanica-general">mecanica-general</option>
      <option value="electromecanica">electromecanica</option>
      <option value="electricidad">electricidad</option>
    </select>
  </section>

  <section>
    <h2>Buttons</h2>
    <div style="display:flex;gap:12px;align-items:center;flex-wrap:wrap;margin-bottom:16px;">
      <button class="btn-primary">Primary</button>
      <button class="btn-secondary">Secondary</button>
      <button class="btn-danger">Danger</button>
      <button class="btn-ghost">Ghost</button>
      <button class="btn-primary" disabled>Disabled</button>
    </div>
  </section>

  <section>
    <h2>Form controls</h2>
    <label>Text input<br><input type="text" placeholder="Placeholder" /></label>
    <label style="margin-left:12px">Select<br>
      <select>
        <option>Option A</option>
        <option>Option B</option>
      </select>
    </label>
    <label style="margin-left:12px">Checkbox<br><input type="checkbox" checked /></label>
  </section>

  <section>
    <h2>Cards & badges</h2>
    <div class="card-surface" style="padding:16px;max-width:480px;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;">
        <strong>Card title</strong>
        <span class="badge">Badge</span>
      </div>
      <p style="margin:0;color:var(--color-text-muted)">Card body text demonstrating type scale and muted color.</p>
    </div>
  </section>

  <footer class="footer">
    <hr>
        <p>Colegio Técnico Nacional</p>
    <p><a href="${pageContext.request.contextPath}/privacidad">Política de privacidad</a> | <a href="${pageContext.request.contextPath}/terminos">Términos de servicio</a></p>
  </footer>

  <script src="${pageContext.request.contextPath}/vendor/flat-ui/js/vendor/jquery.min.js"></script>
  <script src="${pageContext.request.contextPath}/vendor/flat-ui/js/flat-ui.js"></script>
  <script src="${pageContext.request.contextPath}/scripts/sca-theme.js?v=170"></script>
  <script>
    if ('serviceWorker' in navigator) {
      window.addEventListener('load', () => {
        navigator.serviceWorker.register('${pageContext.request.contextPath}/sw.js');
      });
    }
  </script>
  <script>
    document.getElementById('specialtySelect').addEventListener('change', function(e){
      var val = e.target.value;
      if(!val) { document.body.removeAttribute('data-specialty'); return; }
      document.body.setAttribute('data-specialty', val);
    });
  </script>
</body>
</html>
