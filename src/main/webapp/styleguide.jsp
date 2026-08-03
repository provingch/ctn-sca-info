<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
  if (session == null || session.getAttribute("user") == null) {
    response.sendRedirect(request.getContextPath() + "/index.jsp");
    return;
  }
%>
<c:set var="pageTitle" value="Styleguide — Foundation" scope="request" />
<c:set var="headExtraFragment" value="/WEB-INF/includes/head-style-styleguide.jspf" scope="request" />
<%@ include file="/WEB-INF/includes/head.jspf" %>
<body data-user-level="${sessionScope.user.level}" data-specialty="${empty sessionScope.scaSpecialty ? 'general' : sessionScope.scaSpecialty}" data-specialty-source="session">
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
        <p>Colegio T&eacute;cnico Nacional</p>
    <p><a href="${pageContext.request.contextPath}/privacidad">Pol&iacute;tica de privacidad</a> | <a href="${pageContext.request.contextPath}/terminos">T&eacute;rminos de servicio</a></p>
  </footer>

  <%@ include file="/WEB-INF/includes/footer-scripts.jspf" %>
  <script>
    document.getElementById('specialtySelect').addEventListener('change', function(e){
      var val = e.target.value;
      if(!val) { document.body.removeAttribute('data-specialty'); return; }
      document.body.setAttribute('data-specialty', val);
    });
  </script>
</body>
</html>
