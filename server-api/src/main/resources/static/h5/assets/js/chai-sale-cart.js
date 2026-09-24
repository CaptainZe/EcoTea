(function () {
  var els = {
    meta: document.getElementById("meta"),
    list: document.getElementById("list"),
    empty: document.getElementById("empty"),
    cartBar: document.getElementById("cartBar"),
    copyBtn: document.getElementById("copyBtn"),
    imgBtn: document.getElementById("imgBtn"),
    clearBtn: document.getElementById("clearBtn"),
    cartToolbar: document.getElementById("cartToolbar"),
    clearModal: document.getElementById("clearModal"),
    clearOk: document.getElementById("clearOk"),
    totalAmount: document.getElementById("totalAmount"),
    capturePreview: document.getElementById("capturePreview"),
    captureImg: document.getElementById("captureImg"),
    captureHint: document.getElementById("captureHint"),
    captureSave: document.getElementById("captureSave"),
    captureLoading: document.getElementById("captureLoading"),
    swapSheet: document.getElementById("swapSheet"),
    swapHint: document.getElementById("swapHint"),
    swapList: document.getElementById("swapList"),
    swapEmpty: document.getElementById("swapEmpty"),
  };

  var lastCaptureDataUrl = "";
  var swapFromId = null;
  var swapItemMap = {};
  var swapAbort = null;
  var swapToken = 0;

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

  function formatSalePlain(item) {
    if (item.salePrice != null && item.salePrice !== "") {
      var n = Number(item.salePrice);
      if (!isNaN(n)) {
        return "¥ " + String(n);
      }
    }
    if (item.salePriceShow) {
      return String(item.salePriceShow).replace(/\(.*$/, "").trim();
    }
    return "询价";
  }

  function formatOfficialShort(item) {
    if (!item.officialPriceShow || item.officialPriceShow === "-") {
      return "";
    }
    if (item.officialPriceShow === "非卖品") {
      return "官方 非卖品";
    }
    return "官方 ¥" + String(item.officialPriceShow).replace(/元$/, "");
  }

  function formatPriceShow(show) {
    if (!show) {
      return "";
    }
    return String(show)
      .trim()
      .replace(/\(/g, "（")
      .replace(/\)/g, "）");
  }

  function formatOfficialLine(item) {
    var official = formatPriceShow(
      item.officialPriceShow || item.official_price_show
    );
    if (!official || official === "-") {
      return "";
    }
    return official;
  }

  function formatSaleLine(item) {
    var sale = formatPriceShow(item.salePriceShow || item.sale_price_show);
    if (!sale && item.salePrice != null) {
      sale = item.salePrice + "元";
    }
    if (!sale || sale === "-") {
      return "";
    }
    return sale;
  }

  function displayTitle(item) {
    return (
      item.title ||
      [item.brandName, item.name].filter(Boolean).join(" · ") ||
      "商品"
    );
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

    var officialShort = formatOfficialShort(item);
    var discountHtml = item.discountShow
      ? '<span class="discount">' + escapeHtml(item.discountShow) + "</span>"
      : "";

    var detailHref =
      item.id != null
        ? "/h5/chai/sale-detail.html?id=" + encodeURIComponent(item.id)
        : "#";

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
      '<div class="row-price">' +
      '<span class="sale">' +
      escapeHtml(formatSalePlain(item)) +
      "</span>" +
      (officialShort
        ? '<span class="official">' + escapeHtml(officialShort) + "</span>"
        : "") +
      discountHtml +
      "</div>" +
      '<div class="qty-row">' +
      '<button type="button" class="qty-btn" data-act="dec" aria-label="减少">−</button>' +
      '<input type="number" class="qty-input" data-act="qty" min="1" step="1" inputmode="numeric" aria-label="数量" value="' +
      escapeHtml(item.qty || 1) +
      '"/>' +
      '<button type="button" class="qty-btn" data-act="inc" aria-label="增加">+</button>' +
      '<div class="qty-row-end">' +
      (item.spuId
        ? '<button type="button" class="qty-swap" data-act="swap">换同款</button>'
        : "") +
      '<button type="button" class="qty-remove" data-act="remove">移除</button>' +
      "</div></div>" +
      "</div></li>"
    );
  }

  function formatTotal(n) {
    var cart = window.ChaiInquiryCart;
    var plain =
      cart && cart.plainAmount
        ? cart.plainAmount(n)
        : String(n == null ? 0 : n);
    return "¥" + plain;
  }

  function render() {
    var cart = window.ChaiInquiryCart;
    if (!cart) {
      return;
    }
    var items = cart.getItems();
    var kinds = items.length;
    var pcs = 0;
    items.forEach(function (it) {
      pcs += it.qty || 1;
    });

    if (!kinds) {
      els.list.innerHTML = "";
      els.empty.hidden = false;
      els.cartBar.hidden = true;
      if (els.cartToolbar) {
        els.cartToolbar.hidden = true;
      }
      els.meta.textContent = "";
      return;
    }

    els.empty.hidden = true;
    els.cartBar.hidden = false;
    if (els.cartToolbar) {
      els.cartToolbar.hidden = false;
    }
    els.meta.textContent = "共 " + kinds + " 种，合计 " + pcs + " 件 · 仅本机保存";
    if (els.totalAmount) {
      els.totalAmount.textContent = formatTotal(
        cart.totalAmount ? cart.totalAmount() : 0
      );
    }
    els.list.innerHTML = items.map(renderItem).join("");
  }

  function findRow(id) {
    var items = window.ChaiInquiryCart.getItems();
    for (var i = 0; i < items.length; i++) {
      if (String(items[i].id) === String(id)) {
        return items[i];
      }
    }
    return null;
  }

  els.list.addEventListener("click", function (e) {
    var btn = e.target.closest("[data-act]");
    if (!btn || !window.ChaiInquiryCart) {
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
      window.ChaiInquiryCart.setQty(id, (row.qty || 1) + 1);
    } else if (act === "dec") {
      window.ChaiInquiryCart.setQty(id, (row.qty || 1) - 1);
    } else if (act === "remove") {
      window.ChaiInquiryCart.remove(id);
    } else if (act === "swap") {
      openSwapSheet(row);
    }
  });

  els.list.addEventListener("change", function (e) {
    var input = e.target.closest('input[data-act="qty"]');
    if (!input || !window.ChaiInquiryCart) {
      return;
    }
    var card = input.closest(".card");
    if (!card) {
      return;
    }
    var id = card.getAttribute("data-id");
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
      window.ChaiInquiryCart.setQty(id, 0);
      return;
    }
    window.ChaiInquiryCart.setQty(id, Math.floor(n));
  });

  els.list.addEventListener("keydown", function (e) {
    var input = e.target.closest('input[data-act="qty"]');
    if (!input || e.key !== "Enter") {
      return;
    }
    e.preventDefault();
    input.blur();
  });

  /* —— 换同款 —— */
  function imageUrlsOf(item) {
    var urls = item.imageUrls || item.image_urls || [];
    if ((!urls || !urls.length) && item.coverImage) {
      return [item.coverImage];
    }
    return urls || [];
  }

  function stockHtml(item) {
    if (window.ChaiSaleStock && window.ChaiSaleStock.renderHtml) {
      return window.ChaiSaleStock.renderHtml(item);
    }
    return "";
  }

  function swapActionBtnHtml(item) {
    var id = item.id;
    if (id == null) {
      return "";
    }
    if (String(id) === String(swapFromId)) {
      return (
        '<button type="button" class="add-cart-btn is-in-cart" data-swap-act="current" data-id="' +
        escapeHtml(id) +
        '">当前</button>'
      );
    }
    if (window.ChaiInquiryCart && window.ChaiInquiryCart.has(id)) {
      return (
        '<button type="button" class="add-cart-btn is-in-cart" data-swap-act="joined" data-id="' +
        escapeHtml(id) +
        '">已加入</button>'
      );
    }
    return (
      '<button type="button" class="add-cart-btn" data-swap-act="pick" data-id="' +
      escapeHtml(id) +
      '">替换</button>'
    );
  }

  function renderSwapItem(item) {
    var urls = imageUrlsOf(item);
    var coverHtml = urls.length
      ? '<div class="cover-wrap"><img class="cover" src="' +
        escapeHtml(urls[0]) +
        '" alt="" loading="lazy"/></div>'
      : '<div class="cover-wrap placeholder">暂无图</div>';
    var tags =
      tagHtml(item.gradeName) +
      tagHtml(item.specShow) +
      tagHtml(item.prodBatchShow) +
      tagHtml(item.expirationName);
    var officialShort = formatOfficialShort(item);
    var discountHtml = item.discountShow
      ? '<span class="discount">' + escapeHtml(item.discountShow) + "</span>"
      : "";
    var isCurrent = String(item.id) === String(swapFromId);

    return (
      '<li class="card swap-card' +
      (isCurrent ? " is-current" : "") +
      '" data-id="' +
      escapeHtml(item.id) +
      '">' +
      '<div class="card-media">' +
      coverHtml +
      "</div>" +
      '<div class="card-main">' +
      '<div class="body">' +
      '<p class="title">' +
      escapeHtml(item.title || item.name || "") +
      "</p>" +
      '<div class="tags">' +
      tags +
      "</div>" +
      '<div class="row-price">' +
      '<span class="sale">' +
      escapeHtml(formatSalePlain(item)) +
      "</span>" +
      (officialShort
        ? '<span class="official">' + escapeHtml(officialShort) + "</span>"
        : "") +
      discountHtml +
      "</div></div>" +
      '<div class="card-foot">' +
      stockHtml(item) +
      '<div class="card-actions">' +
      swapActionBtnHtml(item) +
      "</div></div></div></li>"
    );
  }

  function closeSwapSheet() {
    if (swapAbort) {
      try {
        swapAbort.abort();
      } catch (e) {
        /* ignore */
      }
      swapAbort = null;
    }
    swapFromId = null;
    swapItemMap = {};
    if (els.swapSheet) {
      els.swapSheet.hidden = true;
    }
    if (els.swapList) {
      els.swapList.innerHTML = "";
    }
    if (els.swapEmpty) {
      els.swapEmpty.hidden = true;
    }
    if (els.swapHint) {
      els.swapHint.textContent = "";
    }
  }

  function paintSwapList(list) {
    var rows = (list || []).slice();
    var current = null;
    var others = [];
    rows.forEach(function (it) {
      if (!it || it.id == null) {
        return;
      }
      if (String(it.id) === String(swapFromId)) {
        current = it;
      } else {
        others.push(it);
      }
    });
    var ordered = current ? [current].concat(others) : others;
    swapItemMap = {};
    ordered.forEach(function (it) {
      swapItemMap[String(it.id)] = it;
    });
    if (els.swapList) {
      els.swapList.innerHTML = ordered.map(renderSwapItem).join("");
    }
    if (els.swapEmpty) {
      els.swapEmpty.hidden = others.length > 0 || ordered.length > 0;
    }
    if (els.swapHint) {
      els.swapHint.textContent = others.length
        ? "共 " + ordered.length + " 款同款 · 点「替换」换入当前行"
        : ordered.length
          ? "仅当前款，暂无其它同款"
          : "";
    }
  }

  function openSwapSheet(row) {
    var cart = window.ChaiInquiryCart;
    if (!cart || !row || !row.spuId) {
      if (cart) {
        cart.toast("暂无同款可换");
      }
      return;
    }
    swapFromId = row.id;
    swapItemMap = {};
    if (els.swapList) {
      els.swapList.innerHTML = "";
    }
    if (els.swapEmpty) {
      els.swapEmpty.hidden = true;
    }
    if (els.swapHint) {
      els.swapHint.textContent = "加载中…";
    }
    if (els.swapSheet) {
      els.swapSheet.hidden = false;
    }
    if (swapAbort) {
      try {
        swapAbort.abort();
      } catch (e) {
        /* ignore */
      }
    }
    var ctrl =
      typeof AbortController !== "undefined" ? new AbortController() : null;
    swapAbort = ctrl;
    var token = ++swapToken;
    var params = new URLSearchParams();
    params.set("page", "1");
    params.set("size", "50");
    params.set("spuId", String(row.spuId));
    var fetchOpts = ctrl ? { signal: ctrl.signal } : {};

    fetch("/chai/sku/sale/list?" + params.toString(), fetchOpts)
      .then(function (res) {
        return res.json();
      })
      .then(function (body) {
        if (token !== swapToken) {
          return;
        }
        if (!body || body.code !== 0 || !body.data) {
          if (els.swapHint) {
            els.swapHint.textContent = "加载失败";
          }
          cart.toast("加载同款失败");
          return;
        }
        var list = body.data.list || [];
        var others = list.filter(function (it) {
          return String(it.id) !== String(swapFromId);
        });
        paintSwapList(list);
        if (!others.length) {
          cart.toast("暂无其它同款");
        }
      })
      .catch(function (err) {
        if (token !== swapToken) {
          return;
        }
        if (
          err &&
          (err.name === "AbortError" ||
            err.code === 20 ||
            (typeof DOMException !== "undefined" &&
              err instanceof DOMException &&
              err.name === "AbortError"))
        ) {
          return;
        }
        if (els.swapHint) {
          els.swapHint.textContent = "网络异常";
        }
        cart.toast("网络异常");
      });
  }

  if (els.swapSheet) {
    els.swapSheet.addEventListener("click", function (e) {
      if (e.target && e.target.getAttribute("data-swap-close") === "1") {
        closeSwapSheet();
        return;
      }
      var btn = e.target.closest("[data-swap-act]");
      if (!btn || !window.ChaiInquiryCart) {
        return;
      }
      var act = btn.getAttribute("data-swap-act");
      var id = btn.getAttribute("data-id");
      if (act === "current") {
        window.ChaiInquiryCart.toast("已是当前款");
        return;
      }
      if (act === "joined") {
        window.ChaiInquiryCart.toast("已在询价单中");
        return;
      }
      if (act === "pick") {
        var item = swapItemMap[String(id)];
        if (!item) {
          return;
        }
        if (window.ChaiInquiryCart.has(item.id)) {
          window.ChaiInquiryCart.toast("已在询价单中");
          return;
        }
        var ok = window.ChaiInquiryCart.replace(swapFromId, item);
        if (ok) {
          window.ChaiInquiryCart.toast("已换同款");
          closeSwapSheet();
        } else {
          window.ChaiInquiryCart.toast("替换失败");
        }
      }
    });
  }

  if (els.copyBtn) {
    els.copyBtn.addEventListener("click", function () {
      var cart = window.ChaiInquiryCart;
      if (!cart) {
        return;
      }
      function doCopy() {
        var text = cart.buildInquiryText();
        if (!text) {
          cart.toast("询价单为空");
          return;
        }
        cart.copyText(text).then(
          function () {
            cart.toast("询价文字已复制");
          },
          function () {
            cart.toast("复制失败，请长按手动复制");
          }
        );
      }
      if (cart.enrichMissingSkuCodes) {
        cart.enrichMissingSkuCodes(function () {
          doCopy();
        });
      } else {
        doCopy();
      }
    });
  }

  function openClearModal() {
    if (!els.clearModal) {
      return;
    }
    els.clearModal.hidden = false;
  }

  function closeClearModal() {
    if (!els.clearModal) {
      return;
    }
    els.clearModal.hidden = true;
  }

  if (els.clearBtn) {
    els.clearBtn.addEventListener("click", function () {
      var cart = window.ChaiInquiryCart;
      if (!cart || !cart.kindCount()) {
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
      var cart = window.ChaiInquiryCart;
      closeClearModal();
      if (!cart) {
        return;
      }
      cart.clear();
      cart.toast("询价单已清空");
    });
  }

  /* —— 生成询价图片 —— */
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
    var cart = window.ChaiInquiryCart;
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
        var rows =
          qexRow("等级", it.gradeName || it.grade_name) +
          qexRow("规格", it.specShow || it.spec_show) +
          qexRow("批次", it.prodBatchShow || it.prod_batch_show) +
          qexRow("保质期", it.expirationName || it.expiration_name) +
          qexRow("官方", formatOfficialLine(it)) +
          qexRow("售价", formatSaleLine(it)) +
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
          escapeHtml(displayTitle(it)) +
          "</h3>" +
          '<ul class="qex-rows">' +
          rows +
          "</ul></div></article>"
        );
      })
      .join("");

    var amount =
      cart && cart.plainAmount ? cart.plainAmount(total) : String(total || 0);

    return (
      '<div class="qex-sheet">' +
      '<header class="qex-head">' +
      '<div class="qex-head-title">' +
      '<span class="qex-head-en" aria-hidden="true">ENQUIRY</span>' +
      '<span class="qex-head-zh">询价单</span>' +
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
      escapeHtml(amount) +
      "</strong></div>" +
      '<p class="qex-note">询价参考 · 以沟通确认为准</p>' +
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
    var cart = window.ChaiInquiryCart;
    var root = document.getElementById("quoteCaptureRoot");
    if (!root) {
      return Promise.reject(new Error("缺少导出节点"));
    }
    if (!cart || !cart.kindCount()) {
      return Promise.reject(new Error("询价单为空"));
    }
    var items = cart.getItems();
    root.innerHTML = buildQuoteMarkup(items, cart.totalAmount());
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
          backgroundColor: "#1e3a5f",
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
      var cart = window.ChaiInquiryCart;
      if (!cart || !cart.kindCount()) {
        if (cart) {
          cart.toast("询价单为空");
        }
        return;
      }
      if (!window.DomToImage) {
        cart.toast("截图组件未加载");
        return;
      }
      var btn = els.imgBtn;
      if (btn.disabled) {
        return;
      }

      function start() {
        btn.disabled = true;
        var oldText = btn.textContent;
        btn.textContent = "生成中…";
        showCaptureLoading();

        runCapture()
          .then(function (result) {
            hideCaptureLoading();
            openCapturePreview(result.dataUrl);
            if (result.corsFail > 0) {
              cart.toast("已生成（部分封面图未嵌入）");
            } else {
              cart.toast("询价图片已生成");
            }
          })
          .catch(function (err) {
            hideCaptureLoading();
            cart.toast("生成失败，请重试");
            if (typeof console !== "undefined" && console.warn) {
              console.warn("inquiry capture failed", err);
            }
          })
          .then(function () {
            hideCaptureLoading();
            btn.disabled = false;
            btn.textContent = oldText;
          });
      }

      if (cart.enrichMissingSkuCodes) {
        cart.enrichMissingSkuCodes(function () {
          start();
        });
      } else {
        start();
      }
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
      window.DomToImage.downloadDataUrl(lastCaptureDataUrl, "inquiry.png");
    });
  }

  if (window.ChaiInquiryCart) {
    window.ChaiInquiryCart.onChange(render);
    if (window.ChaiInquiryCart.enrichMissingSkuCodes) {
      window.ChaiInquiryCart.enrichMissingSkuCodes(function () {
        render();
      });
    }
  }
  render();
})();
