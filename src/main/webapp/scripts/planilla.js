// planilla.js — robust, DOM-ready, defensive
(function () {

  function ready(fn) {
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', fn);
    else fn();
  }

  ready(function init() {
    // global flag
    window.planillaDirty = false;

    // helper: check ignore marker
    function isIgnoreTarget(target) {
      if (!target) return false;
      if (target.dataset && target.dataset.ignoreDirty !== undefined) return true;
      if (target.closest && target.closest('[data-ignore-dirty]')) return true;
      return false;
    }

    // --------------- form dirty tracking ---------------
    const form = document.querySelector('form[action$="/PlanillaServlet"]') || document.querySelector('form');
    if (form) {
      form.addEventListener('input', function (e) {
        if (isIgnoreTarget(e.target)) return;
        window.planillaDirty = true;
      });

      form.addEventListener('change', function (e) {
        if (isIgnoreTarget(e.target)) return;
        window.planillaDirty = true;
      });

      form.addEventListener('submit', function () { window.planillaDirty = false; });
    }

    // back button guard
    const backBtn = document.getElementById('backBtn');
    if (backBtn) {
      backBtn.addEventListener('click', function (e) {
        if (window.planillaDirty) {
          const leave = confirm('Hay cambios sin guardar. ¿Deseas salir sin guardar?');
          if (!leave) e.preventDefault();
        }
      });
    }

    // beforeunload
    window.addEventListener('beforeunload', function (e) {
      if (window.planillaDirty) {
        e.preventDefault();
        e.returnValue = '';
      }
    });

    // --------------- specialty color mapping ---------------
    (function specialtyTheme() {
      const hero = document.querySelector('.planilla-hero');
      if (!hero) return;

      const specialty = (hero.dataset.specialty || '').toString().trim().toLowerCase();
      const match = [
        ['inform', 'specialty-informatica'],
        ['conta', 'specialty-contabilidad'],
        ['elect', 'specialty-electronica'],
        ['mec', 'specialty-mecanica'],
        ['admin', 'specialty-administracion'],
        ['comerc', 'specialty-comercio'],
        ['quim', 'specialty-quimica'],
        ['agro', 'specialty-agro'],
        ['arte', 'specialty-artes']
      ].find(function (entry) {
        return specialty.includes(entry[0]);
      });

      hero.classList.remove('specialty-default', 'specialty-informatica', 'specialty-contabilidad', 'specialty-electronica', 'specialty-mecanica', 'specialty-administracion', 'specialty-comercio', 'specialty-quimica', 'specialty-agro', 'specialty-artes');
      hero.classList.add(match ? match[1] : 'specialty-default');
    })();

    // --------------- flash messages ---------------
    (function flashHandler() {
      const nodes = document.querySelectorAll('.flash, .flash-errors');
      if (!nodes || !nodes.length) return;

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

    // --------------- download button guard (may not exist) ---------------
    const downloadBtn = document.getElementById('downloadBtn');
    if (downloadBtn) {
      downloadBtn.addEventListener('click', function (e) {
        if (window.planillaDirty) {
          const cont = confirm('Hay cambios sin guardar. Es recomendable guardar antes de descargar. ¿Desea descargar de todas formas?');
          if (!cont) e.preventDefault();
        }
      });
    }

    // --------------- freeze checkbox / localStorage ---------------
    (function freezeToggle() {
      const checkbox = document.getElementById('freezeCheckbox');
      const tableResp = document.querySelector('.table-responsive');

      function setFreeze(enabled) {
        if (!tableResp) return;
        tableResp.classList.toggle('freeze-alumnos', !!enabled);
      }

      try {
        const saved = localStorage.getItem('ctn.freezeAlumnos');
        if (saved === '1' && checkbox) {
          checkbox.checked = true;
          setFreeze(true);
        }
      } catch (e) {}

      if (checkbox) {
        checkbox.addEventListener('change', function () {
          setFreeze(this.checked);
          try {
            localStorage.setItem('ctn.freezeAlumnos', this.checked ? '1' : '0');
          } catch (e) {}
        });
      }
    })();

    // --------------- live nota recalculation ---------------
    (function liveNotaUpdate() {
      if (!window.planillaGradeRanges || typeof window.planillaGradeRanges !== 'object') return;

      function getNotaForSum(sum) {
        const ranges = window.planillaGradeRanges;
        if (!ranges) return 1;
        const minForTwo = ranges['2'] ? Number(ranges['2'][0]) : 0;
        if (sum < minForTwo) {
          return 1;
        }
        const grades = ['2', '3', '4', '5'];
        for (const grade of grades) {
          const range = ranges[grade];
          if (!range || range.length < 2) continue;
          const start = Number(range[0]);
          const end = Number(range[1]);
          if (start <= end && sum >= start && sum <= end) {
            return Number(grade);
          }
        }
        return 5;
      }

      function updateRowSummary(input) {
        const row = input.closest('.table-row');
        if (!row) return;
        const summaryCell = row.querySelector('.row-summary');
        if (!summaryCell) return;

        let total = 0;
        let maxTotal = 0;
        const gradeInputs = row.querySelectorAll('input[type="number"][name^="grade_"]');
        gradeInputs.forEach(function (el) {
          const raw = el.value != null ? el.value.trim() : '';
          const max = Number(el.dataset.max || 0);
          let value = raw === '' ? 0 : Number(raw);
          if (!Number.isFinite(value)) value = 0;
          value = Math.max(0, value);
          if (max > 0) value = Math.min(value, max);
          total += value;
          maxTotal += max;
        });

        const porcentaje = maxTotal > 0 ? Math.round((total * 100) / maxTotal) : 0;
        const nota = getNotaForSum(total);

        const totalSpan = summaryCell.querySelector('.row-total');
        const porcentajeSpan = summaryCell.querySelector('.row-porcentaje');
        const notaSpan = summaryCell.querySelector('.row-nota');

        if (totalSpan) totalSpan.textContent = total;
        if (porcentajeSpan) porcentajeSpan.textContent = porcentaje;
        if (notaSpan) notaSpan.textContent = nota;

        // apply grade color class according to nota (estado)
        if (notaSpan) {
          notaSpan.classList.remove('grade-ok', 'grade-warn', 'grade-bad');
          if (nota >= 4) notaSpan.classList.add('grade-ok');
          else if (nota >= 3) notaSpan.classList.add('grade-warn');
          else notaSpan.classList.add('grade-bad');
        }
      }

      const gradeInputs = document.querySelectorAll('.table input[type="number"][name^="grade_"]');
      gradeInputs.forEach(function (input) {
        ['input', 'change'].forEach(function (eventName) {
          input.addEventListener(eventName, function () {
            updateRowSummary(input);
          });
        });
      });
    })();

    // --------------- horizontal drag for table-responsive ---------------
    (function horizontalDrag() {
      const wrap = document.querySelector('.table-responsive');
      if (!wrap) return;

      let isDown = false;
      let startX = 0;
      let scrollLeft = 0;
      let dragStarted = false;

      function isInteractiveTarget(target) {
        return !!(target && target.closest && target.closest('input, textarea, select, button, a, label'));
      }

      wrap.addEventListener('pointerdown', function (e) {
        if (isInteractiveTarget(e.target)) return;

        isDown = true;
        dragStarted = false;
        startX = e.clientX;
        scrollLeft = wrap.scrollLeft;
        wrap.classList.add('dragging');

        if (e.pointerType !== 'mouse') {
          try { e.target.setPointerCapture(e.pointerId); } catch (err) {}
        }
      });

      wrap.addEventListener('pointermove', function (e) {
        if (!isDown) return;
        const dx = e.clientX - startX;
        wrap.scrollLeft = scrollLeft - dx;
      });

      function stopDrag(e) {
        if (!isDown) return;
        isDown = false;
        wrap.classList.remove('dragging');
        try { e.target && e.target.releasePointerCapture && e.target.releasePointerCapture(e.pointerId); } catch (err) {}
      }

      wrap.addEventListener('pointerup', stopDrag);
      wrap.addEventListener('pointercancel', stopDrag);
      wrap.addEventListener('pointerleave', function (e) {
        if (isDown && e.pointerType === 'mouse') stopDrag(e);
      });

      window.addEventListener('blur', function () { isDown = false; wrap.classList.remove('dragging'); });
    })();

  }); // ready/init
})();
