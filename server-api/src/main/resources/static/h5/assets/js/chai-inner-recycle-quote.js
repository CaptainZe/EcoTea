/**
 * 内部回收报价单页：改价改数 + 复制文案 + 生成长图。
 */
(function () {
  var els = {
    meta: document.getElementById("meta"),
    list: document.getElementById("list"),
    empty: document.getElementById("empty"),
    quoteBar: document.getElementById("quoteBar"),
    totalAmount: document.getElementById("totalAmount"),
    clearBtn: document.getElementById("clearBtn"),
    cartToolbar: document.getElementById("cartToolbar"),
    clearModal: document.getElementById("clearModal"),
    clearOk: document.getElementById("clearOk"),
    copyBtn: document.getElementById("copyBtn"),
    imgBtn: document.getElementById("imgBtn"),
    capturePreview: document.getElementById("capturePreview"),
    captureImg: document.getElementById("captureImg"),
    captureHint: document.getElementById("captureHint"),
    captureSave: document.getElementById("captureSave"),
    captureLoading: document.getElementById("captureLoading"),
  };

  var lastCaptureDataUrl = null;

  function escapeHtml(s) {
    return String(s == null ? "" : s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function tagHtml(text) {
    if (!text) {
      return "";
    }
    return '<span class="tag">' + escapeHtml(text) + "</span>";
  }

  function displayTitle(item) {
    if (window.ChaiRecycleQuote && window.ChaiRecycleQuote.displayTitle) {
      return window.ChaiRecycleQuote.displayTitle(item);
    }
    return (
      item.title ||
      [item.brandName, item.name].filter(Boolean).join(" · ") ||
      "商品"
    );
  }

  function formatRecycleRef(item) {
    if (item.recyclePrice != null && item.recyclePrice !== "") {
      var n = Number(item.recyclePrice);
      if (!isNaN(n)) {
        return "¥" + String(n);
      }
    }
    if (item.recyclePriceShow) {
      return String(item.recyclePriceShow).replace(/\(.*$/, "").trim();
    }
    return "-";
  }

  function quotePriceValue(item) {
    if (item.quotePrice != null && item.quotePrice !== "") {
      var n = Number(item.quotePrice);
      if (!isNaN(n)) {
        return String(n);
      }
    }
    if (item.recyclePrice != null && item.recyclePrice !== "") {
      var r = Number(item.recyclePrice);
      if (!isNaN(r)) {
        return String(r);
      }
    }
    return "";
  }

  function formatTotal(n) {
    if (n == null || isNaN(n)) {
      return "¥0";
    }
    var s = String(Math.round(n * 100) / 100);
    if (s.indexOf(".") >= 0) {
      s = s.replace(/\.?0+$/, "");
    }
    return "¥" + s;
  }

  function renderItem(item) {
    var cover = item.coverImage
      ? '<div class="cover-wrap"><img class="cover" src="' +
        escapeHtml(item.coverImage) +
        '" alt="" loading="lazy"/></div>'
      : '<div class="cover-wrap placeholder">暂无图</div>';

    var tags =
      tagHtml(item.gradeName) +
      tagHtml(item.specShow) +
      tagHtml(item.prodBatchShow) +
      tagHtml(item.expirationName);

    var detailHref =
      item.id != null
        ? "/h5/chai/inner-recycle-detail.html?id=" +
          encodeURIComponent(item.id)
        : "#";

    var extraHtml = "";
    if (
      window.ChaiRecycleQuote &&
      window.ChaiRecycleQuote.formatRecycleExtra
    ) {
      var extra = window.ChaiRecycleQuote.formatRecycleExtra(item);
      if (extra) {
        extraHtml =
          '<p class="quote-ref-extra">' + escapeHtml(extra) + "</p>";
      }
    }

    return (
      '<li class="card" data-id="' +
      escapeHtml(item.id) +
      '">' +
      cover +
      '<div class="body">' +
      '<a class="title-link" href="' +
      escapeHtml(detailHref) +
      '"><p class="title">' +
      escapeHtml(displayTitle(item)) +
      "</p></a>" +
      '<div class="tags">' +
      tags +
      "</div>" +
      '<p class="quote-ref">参考回收价 ' +
      escapeHtml(formatRecycleRef(item)) +
      "</p>" +
      extraHtml +
      '<div class="quote-price-row">' +
      '<span class="quote-price-label">报价</span>' +
      '<input type="number" class="quote-price-input" data-act="price" min="0" step="0.01" inputmode="decimal" value="' +
      escapeHtml(quotePriceValue(item)) +
      '"/>' +
      '<span class="quote-price-unit">元</span>' +
      "</div>" +
      '<div class="qty-row">' +
      '<button type="button" class="qty-btn" data-act="dec" aria-label="减少">−</button>' +
      '<input type="number" class="qty-input" data-act="qty" min="1" step="1" inputmode="numeric" aria-label="数量" value="' +
      escapeHtml(item.qty || 1) +
      '"/>' +
      '<button type="button" class="qty-btn" data-act="inc" aria-label="增加">+</button>' +
      '<button type="button" class="qty-remove" data-act="remove">移除</button>' +
      "</div>" +
      "</div></li>"
    );
  }

  function render() {
    var quote = window.ChaiRecycleQuote;
    if (!quote) {
      return;
    }
    var items = quote.getItems();
    var kinds = items.length;
    var pcs = 0;
    items.forEach(function (it) {
      pcs += it.qty || 1;
    });

    if (!kinds) {
      els.list.innerHTML = "";
      els.empty.hidden = false;
      els.quoteBar.hidden = true;
      if (els.cartToolbar) {
        els.cartToolbar.hidden = true;
      }
      els.meta.textContent = "";
      return;
    }

    els.empty.hidden = true;
    els.quoteBar.hidden = false;
    if (els.cartToolbar) {
      els.cartToolbar.hidden = false;
    }
    els.meta.textContent =
      "共 " + kinds + " 种，合计 " + pcs + " 件 · 仅本机保存";
    els.list.innerHTML = items.map(renderItem).join("");
    if (els.totalAmount) {
      els.totalAmount.textContent = formatTotal(quote.totalAmount());
    }
  }

  function findRow(id) {
    var items = window.ChaiRecycleQuote.getItems();
    for (var i = 0; i < items.length; i++) {
      if (String(items[i].id) === String(id)) {
        return items[i];
      }
    }
    return null;
  }

  els.list.addEventListener("click", function (e) {
    var btn = e.target.closest("[data-act]");
    if (!btn || !window.ChaiRecycleQuote) {
      return;
    }
    if (btn.tagName === "INPUT") {
      return;
    }
    var card = btn.closest(".card");
    if (!card) {
      return;
    }
    var id = card.getAttribute("data-id");
    var act = btn.getAttribute("data-act");
    var row = findRow(id);
    if (!row) {
      return;
    }
    if (act === "inc") {
      window.ChaiRecycleQuote.setQty(id, (row.qty || 1) + 1);
    } else if (act === "dec") {
      window.ChaiRecycleQuote.setQty(id, (row.qty || 1) - 1);
    } else if (act === "remove") {
      window.ChaiRecycleQuote.remove(id);
    }
  });

  els.list.addEventListener("change", function (e) {
    var input = e.target.closest("input[data-act]");
    if (!input || !window.ChaiRecycleQuote) {
      return;
    }
    var card = input.closest(".card");
    if (!card) {
      return;
    }
    var id = card.getAttribute("data-id");
    var act = input.getAttribute("data-act");
    if (act === "price") {
      window.ChaiRecycleQuote.setQuotePrice(id, input.value);
      return;
    }
    if (act === "qty") {
      var row = findRow(id);
      var prev = row && row.qty != null ? Number(row.qty) : 1;
      if (isNaN(prev) || prev < 1) {
        prev = 1;
      }
      var raw = String(input.value == null ? "" : input.value).trim();
      if (raw === "") {
        input.value = String(prev);
        return;
      }
      var n = Number(raw);
      if (isNaN(n) || n < 0) {
        input.value = String(prev);
        return;
      }
      if (n < 1) {
        window.ChaiRecycleQuote.setQty(id, 0);
        return;
      }
      window.ChaiRecycleQuote.setQty(id, Math.floor(n));
    }
  });

  els.list.addEventListener("keydown", function (e) {
    var input = e.target.closest(
      'input[data-act="price"], input[data-act="qty"]'
    );
    if (!input || e.key !== "Enter") {
      return;
    }
    e.preventDefault();
    input.blur();
  });

  function openClearModal() {
    if (els.clearModal) {
      els.clearModal.hidden = false;
    }
  }

  function closeClearModal() {
    if (els.clearModal) {
      els.clearModal.hidden = true;
    }
  }

  if (els.clearBtn) {
    els.clearBtn.addEventListener("click", function () {
      var quote = window.ChaiRecycleQuote;
      if (!quote || !quote.kindCount()) {
        return;
      }
      openClearModal();
    });
  }

  if (els.clearModal) {
    els.clearModal.addEventListener("click", function (e) {
      if (e.target && e.target.getAttribute("data-clear-close") === "1") {
        closeClearModal();
      }
    });
  }

  if (els.clearOk) {
    els.clearOk.addEventListener("click", function () {
      var quote = window.ChaiRecycleQuote;
      closeClearModal();
      if (!quote) {
        return;
      }
      quote.clear();
      quote.toast("回收报价单已清空");
    });
  }

  /* —— R5 复制 —— */
  if (els.copyBtn) {
    els.copyBtn.addEventListener("click", function () {
      var quote = window.ChaiRecycleQuote;
      if (!quote) {
        return;
      }
      var text = quote.buildQuoteText();
      if (!text) {
        quote.toast("回收报价单为空");
        return;
      }
      var copyFn =
        window.ChaiInnerUtil && window.ChaiInnerUtil.copyText
          ? window.ChaiInnerUtil.copyText
          : null;
      if (!copyFn) {
        quote.toast("复制不可用");
        return;
      }
      copyFn(text).then(
        function () {
          quote.toast("回收报价单已复制");
        },
        function () {
          quote.toast("复制失败");
        }
      );
    });
  }

  /* —— R6 生成图片 —— */
  function qexRow(label, value) {
    if (!value) {
      return "";
    }
    return (
      '<li class="qex-row"><span class="qex-k">' +
      escapeHtml(label) +
      '</span><span class="qex-v">' +
      escapeHtml(value) +
      "</span></li>"
    );
  }

  function buildQuoteMarkup(items, total) {
    var quote = window.ChaiRecycleQuote;
    var now = new Date();
    function pad2(n) {
      return n < 10 ? "0" + n : String(n);
    }
    var dateStr =
      now.getFullYear() +
      "-" +
      pad2(now.getMonth() + 1) +
      "-" +
      pad2(now.getDate());

    var pcs = 0;
    items.forEach(function (it) {
      pcs += it.qty || 1;
    });
    var kinds = items.length;

    var cards = items
      .map(function (it, idx) {
        var cover = it.coverImage
          ? '<img class="qex-cover" src="' +
            escapeHtml(it.coverImage) +
            '" alt="" loading="eager"/>'
          : '<div class="qex-cover qex-cover-empty">茶</div>';
        var official = quote.formatOfficialLine(it);
        var price = quote.formatQuotePriceLine(it);
        var rows =
          qexRow("规格", it.specShow || it.spec_show) +
          qexRow("批次", it.prodBatchShow || it.prod_batch_show) +
          qexRow("官方", official) +
          qexRow("回收价", price) +
          qexRow("数量", String(it.qty || 1));
        return (
          '<article class="qex-card">' +
          '<div class="qex-card-idx">' +
          (idx + 1) +
          "</div>" +
          '<div class="qex-card-media">' +
          cover +
          "</div>" +
          '<div class="qex-card-body">' +
          '<h3 class="qex-name">' +
          escapeHtml(quote.displayTitle(it)) +
          "</h3>" +
          '<ul class="qex-rows">' +
          rows +
          "</ul></div></article>"
        );
      })
      .join("");

    return (
      '<div class="qex-sheet">' +
      '<header class="qex-head">' +
      '<div class="qex-head-title">' +
      '<span class="qex-head-en" aria-hidden="true">QUOTATION</span>' +
      '<span class="qex-head-zh">回收报价单</span>' +
      "</div>" +
      '<p class="qex-date">' +
      escapeHtml(dateStr) +
      "</p>" +
      "</header>" +
      '<div class="qex-body">' +
      '<div class="qex-list">' +
      cards +
      "</div>" +
      '<footer class="qex-foot">' +
      '<p class="qex-meta">共 ' +
      escapeHtml(kinds) +
      " 种，合计 " +
      escapeHtml(pcs) +
      " 件</p>" +
      '<div class="qex-total"><span>合计</span><strong>¥' +
      escapeHtml(quote.plainAmount(total)) +
      "</strong></div>" +
      '<p class="qex-note">内部回收参考报价 · 以沟通确认为准</p>' +
      "</footer></div></div>"
    );
  }

  function isSameOriginUrl(url) {
    try {
      var u = new URL(url, window.location.href);
      return u.origin === window.location.origin;
    } catch (e) {
      return true;
    }
  }

  function fetchUrlAsDataUrl(url, timeoutMs) {
    var ctrl =
      typeof AbortController !== "undefined" ? new AbortController() : null;
    var timer = null;
    var fetchOpts = {
      mode: "cors",
      credentials: "omit",
      cache: "reload",
    };
    if (ctrl) {
      fetchOpts.signal = ctrl.signal;
      timer = setTimeout(function () {
        try {
          ctrl.abort();
        } catch (e) {
          /* ignore */
        }
      }, timeoutMs || 4000);
    }
    return fetch(url, fetchOpts)
      .then(function (res) {
        if (!res.ok) {
          throw new Error("图片拉取失败");
        }
        return res.blob();
      })
      .then(function (blob) {
        return new Promise(function (resolve, reject) {
          var fr = new FileReader();
          fr.onload = function () {
            resolve(fr.result);
          };
          fr.onerror = function () {
            reject(new Error("图片读取失败"));
          };
          fr.readAsDataURL(blob);
        });
      })
      .then(
        function (dataUrl) {
          if (timer) {
            clearTimeout(timer);
          }
          return dataUrl;
        },
        function (err) {
          if (timer) {
            clearTimeout(timer);
          }
          throw err;
        }
      );
  }

  function embedExportImages(root) {
    var imgs = Array.prototype.slice.call(root.querySelectorAll("img"));
    var corsFail = 0;
    var tasks = imgs.map(function (img) {
      var src = img.getAttribute("src") || "";
      if (!src || src.indexOf("data:") === 0 || src.indexOf("blob:") === 0) {
        return Promise.resolve();
      }
      if (isSameOriginUrl(src)) {
        return Promise.resolve();
      }
      return fetchUrlAsDataUrl(src, 4000)
        .then(function (dataUrl) {
          img.src = dataUrl;
        })
        .catch(function () {
          corsFail += 1;
          img.replaceWith(
            (function () {
              var d = document.createElement("div");
              d.className = "qex-cover qex-cover-empty";
              d.textContent = "茶";
              return d;
            })()
          );
        });
    });
    return Promise.all(tasks).then(function () {
      return corsFail;
    });
  }

  function clearCaptureRoot(root) {
    if (!root) {
      return;
    }
    root.classList.remove("is-active");
    root.innerHTML = "";
  }

  function nextFrame() {
    return new Promise(function (resolve) {
      if (typeof requestAnimationFrame === "function") {
        requestAnimationFrame(function () {
          requestAnimationFrame(resolve);
        });
      } else {
        setTimeout(resolve, 32);
      }
    });
  }

  function showCaptureLoading() {
    if (els.captureLoading) {
      els.captureLoading.hidden = false;
    }
  }

  function hideCaptureLoading() {
    if (els.captureLoading) {
      els.captureLoading.hidden = true;
    }
  }

  function closeCapturePreview() {
    if (els.capturePreview) {
      els.capturePreview.hidden = true;
    }
  }

  function openCapturePreview(dataUrl) {
    lastCaptureDataUrl = dataUrl;
    els.captureImg.src = dataUrl;
    var wechat =
      window.DomToImage && window.DomToImage.isWeChatBrowser
        ? window.DomToImage.isWeChatBrowser()
        : /MicroMessenger/i.test(navigator.userAgent || "");
    if (els.captureHint) {
      els.captureHint.textContent = wechat
        ? "长按下方图片可保存到相册"
        : "可长按图片保存，或点击「保存文件」下载";
    }
    if (els.captureSave) {
      els.captureSave.hidden = !!wechat;
    }
    els.capturePreview.hidden = false;
  }

  function runCapture() {
    var quote = window.ChaiRecycleQuote;
    var root = document.getElementById("quoteCaptureRoot");
    if (!root) {
      return Promise.reject(new Error("缺少导出节点"));
    }
    if (!quote || !quote.kindCount()) {
      return Promise.reject(new Error("回收报价单为空"));
    }
    var items = quote.getItems();
    root.innerHTML = buildQuoteMarkup(items, quote.totalAmount());
    root.classList.add("is-active");

    return embedExportImages(root)
      .then(function (corsFail) {
        return nextFrame().then(function () {
          return corsFail;
        });
      })
      .then(function (corsFail) {
        var sheet = root.querySelector(".qex-sheet") || root;
        var h = Math.max(
          sheet.scrollHeight,
          sheet.offsetHeight,
          root.scrollHeight,
          200
        );
        return window.DomToImage.captureElement(root, {
          scale: 2,
          backgroundColor: "#2d523e",
          timeout: 20000,
          imagesTimeoutContinue: true,
          width: 375,
          height: h,
        }).then(function (dataUrl) {
          return { dataUrl: dataUrl, corsFail: corsFail };
        });
      })
      .then(
        function (result) {
          clearCaptureRoot(root);
          return result;
        },
        function (err) {
          clearCaptureRoot(root);
          throw err;
        }
      );
  }

  if (els.imgBtn) {
    els.imgBtn.addEventListener("click", function () {
      var quote = window.ChaiRecycleQuote;
      if (!quote || !quote.kindCount()) {
        if (quote) {
          quote.toast("回收报价单为空");
        }
        return;
      }
      if (!window.DomToImage) {
        quote.toast("截图组件未加载");
        return;
      }
      var btn = els.imgBtn;
      if (btn.disabled) {
        return;
      }
      btn.disabled = true;
      var oldText = btn.textContent;
      btn.textContent = "生成中…";
      showCaptureLoading();

      runCapture()
        .then(function (result) {
          hideCaptureLoading();
          openCapturePreview(result.dataUrl);
          if (result.corsFail > 0) {
            quote.toast("已生成（部分封面图未嵌入）");
          } else {
            quote.toast("已生成，请长按保存");
          }
        })
        .catch(function (err) {
          hideCaptureLoading();
          quote.toast("生成失败，请重试");
          if (typeof console !== "undefined" && console.warn) {
            console.warn("quote capture failed", err);
          }
        })
        .then(function () {
          hideCaptureLoading();
          btn.disabled = false;
          btn.textContent = oldText;
        });
    });
  }

  if (els.capturePreview) {
    els.capturePreview.addEventListener("click", function (e) {
      if (e.target && e.target.getAttribute("data-cap-close") === "1") {
        closeCapturePreview();
      }
    });
  }

  if (els.captureSave) {
    els.captureSave.addEventListener("click", function () {
      if (!lastCaptureDataUrl || !window.DomToImage) {
        return;
      }
      window.DomToImage.downloadDataUrl(lastCaptureDataUrl, "recycle-quote.png");
    });
  }

  if (window.ChaiRecycleQuote) {
    window.ChaiRecycleQuote.onChange(render);
  }
  render();
})();
