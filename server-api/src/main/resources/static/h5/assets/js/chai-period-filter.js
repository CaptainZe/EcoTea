/**
 * 列表顶栏：年份自绘弹层 + 上下年选项（未选芯片显示「上下年」，选项首项「全部」）。
 */
(function (global) {
  var PROD_BATCH_FIRST = 100;
  var PROD_BATCH_SECOND = 200;

  var yearRoot = null;
  var yearInput = null;
  var batchRoot = null;
  var yearDone = null;
  var batchDone = null;

  function parseYear(raw) {
    var s = String(raw == null ? "" : raw).trim();
    if (!s) {
      return { ok: true, year: null };
    }
    if (!/^\d{4}$/.test(s)) {
      return { ok: false, message: "请输入4位年份" };
    }
    var y = parseInt(s, 10);
    var max = new Date().getFullYear() + 1;
    if (y < 1900 || y > max) {
      return { ok: false, message: "年份超出范围" };
    }
    return { ok: true, year: y };
  }

  function yearLabel(year) {
    return year != null ? year + "年" : "年份";
  }

  function prodBatchLabel(prodBatch) {
    if (prodBatch === PROD_BATCH_FIRST) {
      return "上半年";
    }
    if (prodBatch === PROD_BATCH_SECOND) {
      return "下半年";
    }
    return "上下年";
  }

  function syncChip(btn, text, active) {
    if (!btn) {
      return;
    }
    btn.textContent = text;
    if (active) {
      btn.classList.add("is-on");
    } else {
      btn.classList.remove("is-on");
    }
  }

  function ensureYearModal() {
    if (yearRoot) {
      return;
    }
    yearRoot = document.createElement("div");
    yearRoot.className = "period-modal";
    yearRoot.id = "periodYearModal";
    yearRoot.hidden = true;
    yearRoot.innerHTML =
      '<div class="period-modal-mask" data-period-year-close="1"></div>' +
      '<div class="period-modal-panel" role="dialog" aria-modal="true" aria-labelledby="periodYearTitle">' +
      '<h2 class="period-modal-title" id="periodYearTitle">按年份查询</h2>' +
      '<input type="text" class="period-modal-input" id="periodYearInput" inputmode="numeric" maxlength="4" placeholder="输入4位年份，留空清除" autocomplete="off"/>' +
      '<div class="period-modal-ft">' +
      '<button type="button" class="period-modal-btn" data-period-year-close="1">取消</button>' +
      '<button type="button" class="period-modal-btn is-ok" id="periodYearOk">确定</button>' +
      "</div></div>";
    document.body.appendChild(yearRoot);
    yearInput = yearRoot.querySelector("#periodYearInput");

    yearRoot.addEventListener("click", function (e) {
      if (e.target && e.target.getAttribute("data-period-year-close") === "1") {
        closeYearModal();
      }
    });
    var okBtn = yearRoot.querySelector("#periodYearOk");
    if (okBtn) {
      okBtn.addEventListener("click", function () {
        var parsed = parseYear(yearInput && yearInput.value);
        if (!parsed.ok) {
          window.alert(parsed.message);
          return;
        }
        var cb = yearDone;
        closeYearModal();
        if (typeof cb === "function") {
          cb(parsed.year);
        }
      });
    }
    if (yearInput) {
      yearInput.addEventListener("keydown", function (e) {
        if (e.key === "Enter") {
          e.preventDefault();
          if (okBtn) {
            okBtn.click();
          }
        }
      });
    }
  }

  function closeYearModal() {
    yearDone = null;
    if (yearRoot) {
      yearRoot.hidden = true;
    }
    document.body.style.overflow = "";
  }

  function openYearModal(opts) {
    opts = opts || {};
    ensureYearModal();
    yearDone = opts.onDone || null;
    if (yearInput) {
      yearInput.value = opts.year != null ? String(opts.year) : "";
    }
    yearRoot.hidden = false;
    document.body.style.overflow = "hidden";
    setTimeout(function () {
      if (yearInput) {
        yearInput.focus();
        yearInput.select();
      }
    }, 30);
  }

  function ensureBatchSheet() {
    if (batchRoot) {
      return;
    }
    batchRoot = document.createElement("div");
    batchRoot.className = "period-sheet";
    batchRoot.id = "periodBatchSheet";
    batchRoot.hidden = true;
    batchRoot.innerHTML =
      '<div class="period-sheet-mask" data-period-batch-close="1"></div>' +
      '<div class="period-sheet-panel" role="dialog" aria-modal="true">' +
      '<button type="button" class="period-sheet-item" data-period-batch="">全部</button>' +
      '<button type="button" class="period-sheet-item" data-period-batch="100">上半年</button>' +
      '<button type="button" class="period-sheet-item" data-period-batch="200">下半年</button>' +
      '<button type="button" class="period-sheet-cancel" data-period-batch-close="1">取消</button>' +
      "</div>";
    document.body.appendChild(batchRoot);

    batchRoot.addEventListener("click", function (e) {
      if (e.target && e.target.getAttribute("data-period-batch-close") === "1") {
        closeBatchSheet();
        return;
      }
      var item = e.target.closest("[data-period-batch]");
      if (!item || !batchRoot.contains(item)) {
        return;
      }
      var raw = item.getAttribute("data-period-batch");
      var val = null;
      if (raw === "100") {
        val = PROD_BATCH_FIRST;
      } else if (raw === "200") {
        val = PROD_BATCH_SECOND;
      }
      var cb = batchDone;
      closeBatchSheet();
      if (typeof cb === "function") {
        cb(val);
      }
    });
  }

  function closeBatchSheet() {
    batchDone = null;
    if (batchRoot) {
      batchRoot.hidden = true;
    }
    document.body.style.overflow = "";
  }

  function openProdBatchSheet(opts) {
    opts = opts || {};
    ensureBatchSheet();
    batchDone = opts.onDone || null;
    var items = batchRoot.querySelectorAll("[data-period-batch]");
    for (var i = 0; i < items.length; i++) {
      var el = items[i];
      var raw = el.getAttribute("data-period-batch") || "";
      var on =
        (opts.prodBatch == null && raw === "") ||
        (opts.prodBatch != null && String(opts.prodBatch) === raw);
      if (on) {
        el.classList.add("is-on");
      } else {
        el.classList.remove("is-on");
      }
    }
    batchRoot.hidden = false;
    document.body.style.overflow = "hidden";
  }

  global.ChaiPeriodFilter = {
    parseYear: parseYear,
    yearLabel: yearLabel,
    prodBatchLabel: prodBatchLabel,
    syncChip: syncChip,
    openYearModal: openYearModal,
    openProdBatchSheet: openProdBatchSheet,
    closeYearModal: closeYearModal,
    closeProdBatchSheet: closeBatchSheet,
  };
})(window);
