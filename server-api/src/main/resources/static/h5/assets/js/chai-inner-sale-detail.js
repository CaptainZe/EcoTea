(function () {
  var LIST_URL_KEY = "chai.innerSale.listUrl";
  var params = new URLSearchParams(window.location.search);
  var id = params.get("id");

  var els = {
    status: document.getElementById("status"),
    detail: document.getElementById("detail"),
    carousel: document.getElementById("carousel"),
    carouselTrack: document.getElementById("carouselTrack"),
    carouselIndex: document.getElementById("carouselIndex"),
    brand: document.getElementById("brand"),
    skuName: document.getElementById("skuName"),
    salePrice: document.getElementById("salePrice"),
    officialPrice: document.getElementById("officialPrice"),
    discount: document.getElementById("discount"),
    stock: document.getElementById("stock"),
    cardAttrs: document.getElementById("cardAttrs"),
    cardWhStock: document.getElementById("cardWhStock"),
    bar: document.getElementById("bar"),
    copyInfoBtn: document.getElementById("copyInfoBtn"),
    copyBtn: document.getElementById("copyBtn"),
    dlBtn: document.getElementById("dlBtn"),
    quoteBtn: document.getElementById("quoteBtn"),
    gallery: document.getElementById("gallery"),
    galleryImg: document.getElementById("galleryImg"),
    galleryIndex: document.getElementById("galleryIndex"),
    galleryPrev: document.getElementById("galleryPrev"),
    galleryNext: document.getElementById("galleryNext"),
    capturePreview: document.getElementById("capturePreview"),
    captureImg: document.getElementById("captureImg"),
    captureHint: document.getElementById("captureHint"),
    captureSave: document.getElementById("captureSave"),
    backLink: document.getElementById("backLink"),
  };

  var currentItem = null;
  var lastCaptureDataUrl = null;
  var viewState = { urls: [], index: 0, galleryOpen: false };
  var touchX = null;
  var suppressClick = false;

  var ICONS = {
    grade:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 3l2.4 4.9 5.4.8-3.9 3.8.9 5.4L12 15.9 7.2 18l.9-5.4L4.2 8.7l5.4-.8L12 3z"/></svg>',
    spec:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="4" y="4" width="16" height="16" rx="2"/><path d="M8 9h8M8 12h8M8 15h5"/></svg>',
    batch:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M8 3v4M16 3v4M3 10h18"/></svg>',
    expiration:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="8"/><path d="M12 8v4l2.5 2.5"/></svg>',
  };

  function bindListBack(defaultHref) {
    if (!els.backLink) {
      return;
    }
    var saved = null;
    try {
      saved = sessionStorage.getItem(LIST_URL_KEY);
    } catch (e) {
      saved = null;
    }
    if (saved && saved.indexOf("/h5/chai/inner-sale.html") === 0) {
      els.backLink.href = saved;
    } else {
      els.backLink.href = defaultHref || "/h5/chai/inner-sale.html";
    }
  }

  function escapeHtml(s) {
    return String(s == null ? "" : s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  /** 导出最多 9 张；页面轮播可用全部 */
  function imageUrlsOf(item) {
    var urls = item.imageUrls || item.image_urls || [];
    if (!urls.length && (item.coverImage || item.cover_image)) {
      return [item.coverImage || item.cover_image];
    }
    return urls.slice(0, 9);
  }

  function displayImageUrls(item) {
    var urls = item.imageUrls || item.image_urls || [];
    if (!urls.length && (item.coverImage || item.cover_image)) {
      return [item.coverImage || item.cover_image];
    }
    return urls.slice();
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

  function formatOfficialPlain(item) {
    if (!item.officialPriceShow || item.officialPriceShow === "-") {
      return "";
    }
    if (item.officialPriceShow === "非卖品") {
      return "官方价 非卖品";
    }
    return (
      "官方价 ¥" + String(item.officialPriceShow).replace(/元$/, "")
    );
  }

  function formatPriceLineForCopy(show) {
    if (!show) {
      return "";
    }
    return String(show).trim().replace(/\(/g, "（").replace(/\)/g, "）");
  }

  /** 对齐微信关键词单条文案（无链接） */
  function formatKeywordPlainText(item) {
    var title = "";
    if (item.title) {
      title = String(item.title).trim();
    } else if (item.name) {
      title = String(item.name).trim();
    } else {
      title = "商品";
    }
    var lines = [title];
    function push(label, value) {
      if (value) {
        lines.push(label + "：" + String(value).trim());
      }
    }
    push("等级", item.gradeName);
    push("规格", item.specShow);
    push("批次", item.prodBatchShow);
    push("保质期", item.expirationName);
    push("官方", formatPriceLineForCopy(item.officialPriceShow));
    push("售价", formatPriceLineForCopy(item.salePriceShow));
    if (item.totalQty != null) {
      var stock = "库存数量：" + item.totalQty;
      var extras = [];
      if (item.qtyNoBag != null && Number(item.qtyNoBag) > 0) {
        extras.push("无袋：" + item.qtyNoBag);
      }
      if (item.qtyDamaged != null && Number(item.qtyDamaged) > 0) {
        extras.push("破损：" + item.qtyDamaged);
      }
      if (item.qtyDamagedNoBag != null && Number(item.qtyDamagedNoBag) > 0) {
        extras.push("破损无袋：" + item.qtyDamagedNoBag);
      }
      if (extras.length) {
        stock += "，" + extras.join("，");
      }
      lines.push(stock);
    }
    return lines.join("\n");
  }

  function showUnavailable(msg) {
    els.detail.hidden = true;
    els.bar.hidden = true;
    els.status.hidden = false;
    var back =
      (els.backLink && els.backLink.getAttribute("href")) ||
      "/h5/chai/inner-sale.html";
    els.status.innerHTML =
      escapeHtml(msg || "暂不可售") +
      '<br/><br/><a href="' +
      escapeHtml(back) +
      '">返回内部价目</a>';
  }

  function paintCarouselIndex() {
    if (!els.carouselIndex) {
      return;
    }
    if (viewState.urls.length <= 1) {
      els.carouselIndex.hidden = true;
      return;
    }
    els.carouselIndex.hidden = false;
    els.carouselIndex.textContent =
      viewState.index + 1 + " / " + viewState.urls.length;
  }

  function syncCarouselScroll() {
    if (!els.carouselTrack) {
      return;
    }
    var slides = els.carouselTrack.querySelectorAll(".carousel-slide");
    if (!slides.length || !slides[viewState.index]) {
      return;
    }
    els.carouselTrack.scrollTo({
      left: slides[viewState.index].offsetLeft,
      behavior: "auto",
    });
  }

  function setViewIndex(next, scroll) {
    if (!viewState.urls.length) {
      return;
    }
    viewState.index =
      ((next % viewState.urls.length) + viewState.urls.length) %
      viewState.urls.length;
    paintCarouselIndex();
    if (scroll) {
      syncCarouselScroll();
    }
    if (viewState.galleryOpen) {
      paintGallery();
    }
  }

  function paintGallery() {
    if (!viewState.urls.length || !els.galleryImg) {
      return;
    }
    els.galleryImg.src = viewState.urls[viewState.index];
    els.galleryIndex.textContent =
      viewState.index + 1 + " / " + viewState.urls.length;
    var multi = viewState.urls.length > 1;
    els.galleryPrev.hidden = !multi;
    els.galleryNext.hidden = !multi;
  }

  function openGallery() {
    if (!viewState.urls.length) {
      return;
    }
    viewState.galleryOpen = true;
    els.gallery.hidden = false;
    paintGallery();
  }

  function closeGallery() {
    viewState.galleryOpen = false;
    els.gallery.hidden = true;
    syncCarouselScroll();
    paintCarouselIndex();
  }

  function galleryStep(delta) {
    setViewIndex(viewState.index + delta, true);
  }

  function onCarouselScroll() {
    var w = els.carouselTrack.clientWidth || 1;
    var idx = Math.round(els.carouselTrack.scrollLeft / w);
    if (idx < 0) {
      idx = 0;
    }
    if (idx >= viewState.urls.length) {
      idx = viewState.urls.length - 1;
    }
    if (idx !== viewState.index) {
      viewState.index = idx;
      paintCarouselIndex();
    }
  }

  function renderCarousel(urls) {
    viewState.urls = urls;
    viewState.index = 0;
    if (!urls.length) {
      els.carouselTrack.innerHTML =
        '<div class="carousel-placeholder">暂无图</div>';
      els.carouselIndex.hidden = true;
      return;
    }
    els.carouselTrack.innerHTML = urls
      .map(function (url, i) {
        return (
          '<div class="carousel-slide" data-i="' +
          i +
          '"><img src="' +
          escapeHtml(url) +
          '" alt="" loading="' +
          (i === 0 ? "eager" : "lazy") +
          '"/></div>'
        );
      })
      .join("");
    paintCarouselIndex();
    Array.prototype.forEach.call(
      els.carouselTrack.querySelectorAll("img"),
      function (img) {
        img.addEventListener("click", function () {
          if (suppressClick) {
            return;
          }
          var slide = img.closest(".carousel-slide");
          var i = slide ? Number(slide.getAttribute("data-i")) : 0;
          if (!isNaN(i)) {
            viewState.index = i;
          }
          openGallery();
        });
      }
    );
  }

  function attrRow(iconKey, label, value) {
    if (!value) {
      return "";
    }
    return (
      '<li class="attr-row">' +
      '<span class="attr-icon">' +
      (ICONS[iconKey] || "") +
      "</span>" +
      '<div class="attr-body">' +
      '<span class="attr-label">' +
      escapeHtml(label) +
      "</span>" +
      '<span class="attr-value">' +
      escapeHtml(value) +
      "</span></div></li>"
    );
  }

  function renderAttrs(item) {
    var html =
      attrRow("grade", "等级", item.gradeName) +
      attrRow("spec", "规格", item.specShow) +
      attrRow("batch", "批次", item.prodBatchShow) +
      attrRow("expiration", "保质期", item.expirationName);
    if (!html) {
      els.cardAttrs.hidden = true;
      els.cardAttrs.innerHTML = "";
      return;
    }
    els.cardAttrs.hidden = false;
    els.cardAttrs.innerHTML = '<ul class="attr-list">' + html + "</ul>";
  }

  function renderWhStock(item) {
    if (!els.cardWhStock) {
      return;
    }
    var rows = item.warehouseStocks || item.warehouse_stocks || [];
    if (!rows.length) {
      els.cardWhStock.hidden = true;
      els.cardWhStock.innerHTML = "";
      return;
    }
    var listHtml = rows
      .map(function (row) {
        if (!row || row.qty == null || Number(row.qty) <= 0) {
          return "";
        }
        var name = row.shortName || row.short_name || "";
        if (!name) {
          return "";
        }
        var dmg =
          window.ChaiSaleStock && window.ChaiSaleStock.renderExceptionsHtml
            ? window.ChaiSaleStock.renderExceptionsHtml(row).replace(
                /class="dmg"/g,
                'class="wh-stock-dmg"'
              )
            : "";
        if (!dmg) {
          var parts = [];
          function push(label, value) {
            var n = value != null ? Number(value) : 0;
            if (!isNaN(n) && n > 0) {
              parts.push(
                '<span class="wh-stock-dmg">' +
                  escapeHtml(label + " " + n) +
                  "</span>"
              );
            }
          }
          push("无袋", row.qtyNoBag);
          push("破损", row.qtyDamaged);
          push("破损无袋", row.qtyDamagedNoBag);
          dmg = parts.join("");
        }
        return (
          '<li class="wh-stock-row">' +
          '<span class="wh-stock-name">' +
          escapeHtml(name) +
          "</span>" +
          '<span class="wh-stock-qty">' +
          escapeHtml(row.qty) +
          " 件" +
          dmg +
          "</span></li>"
        );
      })
      .join("");
    if (!listHtml) {
      els.cardWhStock.hidden = true;
      els.cardWhStock.innerHTML = "";
      return;
    }
    els.cardWhStock.hidden = false;
    els.cardWhStock.innerHTML =
      '<ul class="wh-stock-list">' + listHtml + "</ul>";
  }

  function render(item) {
    currentItem = item;
    var urls = displayImageUrls(item);
    document.title =
      (item.name || item.title || "商品详情") + " · 内部";

    renderCarousel(urls);

    if (item.brandName) {
      els.brand.hidden = false;
      els.brand.textContent = item.brandName;
    } else {
      els.brand.hidden = true;
      els.brand.textContent = "";
    }
    els.skuName.textContent = item.name || item.title || "";
    els.salePrice.textContent = formatSalePlain(item);

    var official = formatOfficialPlain(item);
    if (official) {
      els.officialPrice.hidden = false;
      els.officialPrice.textContent = official;
    } else {
      els.officialPrice.hidden = true;
      els.officialPrice.textContent = "";
    }

    if (item.discountShow) {
      els.discount.hidden = false;
      els.discount.textContent = item.discountShow;
    } else {
      els.discount.hidden = true;
      els.discount.textContent = "";
    }

    if (els.stock && window.ChaiSaleStock) {
      els.stock.innerHTML = window.ChaiSaleStock.renderHtml(item);
    }

    renderAttrs(item);
    renderWhStock(item);

    els.status.hidden = true;
    els.detail.hidden = false;
    els.bar.hidden = false;
    syncQuoteBtn();
  }

  function syncQuoteBtn() {
    if (!els.quoteBtn) {
      return;
    }
    var inQuote =
      currentItem &&
      window.ChaiSaleQuote &&
      window.ChaiSaleQuote.has(currentItem.id);
    if (inQuote) {
      els.quoteBtn.textContent = "已加入";
      els.quoteBtn.disabled = true;
      els.quoteBtn.classList.add("is-in-quote");
    } else {
      els.quoteBtn.textContent = "加入报价";
      els.quoteBtn.disabled = false;
      els.quoteBtn.classList.remove("is-in-quote");
    }
  }

  function load() {
    if (!id) {
      showUnavailable("缺少商品编号");
      return;
    }
    fetch("/chai/sku/sale/detail?id=" + encodeURIComponent(id))
      .then(function (res) {
        return res.json();
      })
      .then(function (body) {
        if (!body || body.code !== 0 || !body.data) {
          showUnavailable(
            (body && body.message) || "商品不存在或暂不可售"
          );
          return;
        }
        render(body.data);
      })
      .catch(function () {
        showUnavailable("网络异常，请稍后重试");
      });
  }

  if (els.copyBtn) {
    els.copyBtn.addEventListener("click", function () {
      if (!currentItem || currentItem.id == null || !window.ChaiInnerUtil) {
        return;
      }
      window.ChaiInnerUtil.copyPublicDetail(currentItem.id);
    });
  }

  if (els.copyInfoBtn) {
    els.copyInfoBtn.addEventListener("click", function () {
      if (!currentItem || !window.ChaiInnerUtil) {
        return;
      }
      var text = formatKeywordPlainText(currentItem);
      window.ChaiInnerUtil.copyText(text).then(
        function () {
          window.ChaiInnerUtil.toast("已复制商品信息");
        },
        function () {
          window.ChaiInnerUtil.toast("复制失败");
        }
      );
    });
  }

  if (els.gallery) {
    els.gallery.addEventListener("click", function (e) {
      if (e.target && e.target.getAttribute("data-gallery-close") === "1") {
        closeGallery();
      }
    });
  }
  if (els.galleryPrev) {
    els.galleryPrev.addEventListener("click", function (e) {
      e.stopPropagation();
      galleryStep(-1);
    });
  }
  if (els.galleryNext) {
    els.galleryNext.addEventListener("click", function (e) {
      e.stopPropagation();
      galleryStep(1);
    });
  }
  if (els.carouselTrack) {
    els.carouselTrack.addEventListener("scroll", onCarouselScroll, {
      passive: true,
    });
  }
  if (els.galleryImg) {
    els.galleryImg.addEventListener("touchstart", function (e) {
      if (!e.touches || !e.touches.length) {
        return;
      }
      touchX = e.touches[0].clientX;
    });
    els.galleryImg.addEventListener("touchend", function (e) {
      if (touchX == null || !e.changedTouches || !e.changedTouches.length) {
        return;
      }
      var dx = e.changedTouches[0].clientX - touchX;
      touchX = null;
      if (Math.abs(dx) < 40) {
        return;
      }
      suppressClick = true;
      setTimeout(function () {
        suppressClick = false;
      }, 300);
      galleryStep(dx < 0 ? 1 : -1);
    });
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

  function isSameOriginUrl(url) {
    try {
      var u = new URL(url, window.location.href);
      return u.origin === window.location.origin;
    } catch (e) {
      return true;
    }
  }

  function attrExportRow(label, valueHtml) {
    if (!valueHtml) {
      return "";
    }
    return (
      '<li class="ex-attr"><span class="ex-attr-k">' +
      escapeHtml(label) +
      '</span><span class="ex-attr-v">' +
      valueHtml +
      "</span></li>"
    );
  }

  function exportYenAmount(item, which) {
    if (which === "sale") {
      if (item.salePrice != null && item.salePrice !== "") {
        var n = Number(item.salePrice);
        if (!isNaN(n)) {
          return String(n);
        }
      }
      if (item.salePriceShow) {
        var raw = String(item.salePriceShow).replace(/\(.*$/, "").trim();
        return raw.replace(/^¥\s*/, "").replace(/^￥\s*/, "") || "";
      }
      return "";
    }
    if (!item.officialPriceShow || item.officialPriceShow === "-") {
      return "";
    }
    if (item.officialPriceShow === "非卖品") {
      return "非卖品";
    }
    return String(item.officialPriceShow).replace(/元$/, "");
  }

  function buildExportTitle(item) {
    var name = item.name || item.title || "";
    var brand = item.brandName ? String(item.brandName).trim() : "";
    if (brand && name) {
      return brand + " · " + name;
    }
    return brand || name || "";
  }

  function buildExportInfoRows(item) {
    var officialAmt = exportYenAmount(item, "official");
    var saleAmt = exportYenAmount(item, "sale");
    var qty = item.totalQty != null ? Number(item.totalQty) : 0;
    if (isNaN(qty) || qty < 0) {
      qty = 0;
    }

    var rows = "";
    if (officialAmt === "非卖品") {
      rows += attrExportRow("官方价", escapeHtml("非卖品"));
    } else if (officialAmt) {
      rows += attrExportRow("官方价", escapeHtml("¥" + officialAmt));
    }

    if (saleAmt) {
      var saleHtml =
        '<span class="ex-meta-sale">' +
        escapeHtml("¥" + saleAmt) +
        "</span>";
      if (item.discountShow) {
        saleHtml +=
          '<span class="ex-meta-disc">' +
          escapeHtml(item.discountShow) +
          "</span>";
      }
      rows += attrExportRow("销售价", saleHtml);
    } else {
      rows += attrExportRow(
        "销售价",
        '<span class="ex-meta-sale">' + escapeHtml("询价") + "</span>"
      );
    }

    var qtyText = String(qty);
    var qtyExtras = [];
    if (item.qtyNoBag != null && Number(item.qtyNoBag) > 0) {
      qtyExtras.push("无袋 " + Number(item.qtyNoBag));
    }
    if (item.qtyDamaged != null && Number(item.qtyDamaged) > 0) {
      qtyExtras.push("破损 " + Number(item.qtyDamaged));
    }
    if (item.qtyDamagedNoBag != null && Number(item.qtyDamagedNoBag) > 0) {
      qtyExtras.push("破损无袋 " + Number(item.qtyDamagedNoBag));
    }
    if (qtyExtras.length) {
      qtyText += "（" + qtyExtras.join(" · ") + "）";
    }
    rows += attrExportRow("数量", escapeHtml(qtyText));
    rows += attrExportRow(
      "等级",
      item.gradeName ? escapeHtml(item.gradeName) : ""
    );
    rows += attrExportRow(
      "规格",
      item.specShow ? escapeHtml(item.specShow) : ""
    );
    rows += attrExportRow(
      "批次",
      item.prodBatchShow ? escapeHtml(item.prodBatchShow) : ""
    );
    rows += attrExportRow(
      "保质期",
      item.expirationName ? escapeHtml(item.expirationName) : ""
    );
    return rows;
  }

  function buildExportGallery(urls) {
    var n = urls.length;
    if (!n) {
      return '<div class="ex-gallery ex-gallery-empty"><p class="ex-grid-empty">暂无展示图</p></div>';
    }
    var cells = urls
      .map(function (url) {
        return (
          '<div class="ex-grid-cell"><img src="' +
          escapeHtml(url) +
          '" alt="" loading="eager"/></div>'
        );
      })
      .join("");
    return (
      '<div class="ex-gallery n-' +
      n +
      '">' +
      cells +
      "</div>"
    );
  }

  /** 固定高海报：z3 底 + z2 票券（不拉伸）+ DOM 叠内容 */
  function buildExportMarkup(item) {
    var urls = imageUrlsOf(item);
    var title = buildExportTitle(item);
    var infoRows = buildExportInfoRows(item);

    var titleHtml = title
      ? '<h2 class="ex-title">' +
        '<img class="ex-leaf" src="/h5/assets/image/sku_sale_z1.png" alt="" width="18" height="18"/>' +
        '<span class="ex-title-text">' +
        escapeHtml(title) +
        "</span></h2>"
      : "";

    return (
      '<div class="ex-poster">' +
      '<img class="ex-bg" src="/h5/assets/image/sku_sale_z3.png" alt="" loading="eager"/>' +
      '<div class="ex-stage">' +
      '<div class="ex-ticket">' +
      '<img class="ex-ticket-img" src="/h5/assets/image/sku_sale_z2.png" alt="" loading="eager"/>' +
      '<div class="ex-ticket-ui">' +
      '<div class="ex-info">' +
      titleHtml +
      (infoRows ? '<ul class="ex-attrs">' + infoRows + "</ul>" : "") +
      '<p class="ex-disclaimer">回收转售 · 非官方 · 以咨询为准</p>' +
      "</div>" +
      buildExportGallery(urls) +
      "</div></div></div></div>"
    );
  }

  function fetchUrlAsDataUrl(url, timeoutMs) {
    var ctrl =
      typeof AbortController !== "undefined" ? new AbortController() : null;
    var timer = null;
    var fetchOpts = {
      mode: "cors",
      credentials: "omit",
      // 避免沿用「无 CORS」缓存导致仍失败
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

  /**
   * 仅处理导出节点内图片：同域跳过；跨域短超时转 dataURL。
   * 失败保留原 URL，绝不改页面 #detail 上的 img。
   */
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
        });
    });
    return Promise.all(tasks).then(function () {
      return corsFail;
    });
  }

  function clearExportRoot(root) {
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

  function runCapture() {
    var root = document.getElementById("exportCaptureRoot");
    if (!root) {
      return Promise.reject(new Error("缺少导出节点"));
    }
    if (!currentItem) {
      return Promise.reject(new Error("详情未就绪"));
    }
    root.innerHTML = buildExportMarkup(currentItem);
    root.classList.add("is-active");

    return embedExportImages(root)
      .then(function (corsFail) {
        return nextFrame().then(function () {
          return corsFail;
        });
      })
      .then(function (corsFail) {
        return window.DomToImage.captureElement(root, {
          scale: 2,
          backgroundColor: "#315E46",
          timeout: 12000,
          imagesTimeoutContinue: true,
          width: 375,
          height: 812,
        }).then(function (dataUrl) {
          return { dataUrl: dataUrl, corsFail: corsFail };
        });
      })
      .then(
        function (result) {
          clearExportRoot(root);
          return result;
        },
        function (err) {
          clearExportRoot(root);
          throw err;
        }
      );
  }

  els.dlBtn.addEventListener("click", function () {
    if (!window.DomToImage) {
      if (window.ChaiInnerUtil) {
        window.ChaiInnerUtil.toast("截图组件未加载");
      }
      return;
    }
    if (!els.detail || els.detail.hidden || !currentItem) {
      if (window.ChaiInnerUtil) {
        window.ChaiInnerUtil.toast("请等待详情加载完成");
      }
      return;
    }
    var btn = els.dlBtn;
    if (btn.disabled) {
      return;
    }
    btn.disabled = true;
    var oldText = btn.textContent;
    btn.textContent = "生成中…";
    if (window.ChaiInnerUtil) {
      window.ChaiInnerUtil.toast("正在生成图片…");
    }

    runCapture()
      .then(function (result) {
        openCapturePreview(result.dataUrl);
        if (window.ChaiInnerUtil) {
          if (result.corsFail > 0) {
            window.ChaiInnerUtil.toast(
              "已生成（部分商品图因跨域未嵌入，需图床开 CORS）"
            );
          } else {
            window.ChaiInnerUtil.toast("已生成，请长按保存");
          }
        }
      })
      .catch(function (err) {
        var msg = (err && err.message) || "生成失败";
        if (window.ChaiInnerUtil) {
          window.ChaiInnerUtil.toast(
            msg.indexOf("跨域") >= 0 || msg.indexOf("CORS") >= 0
              ? "图片跨域，无法导出（请为图床配置 CORS）"
              : "生成失败，请重试"
          );
        }
        if (typeof console !== "undefined" && console.warn) {
          console.warn("capture failed", err);
        }
      })
      .then(function () {
        btn.disabled = false;
        btn.textContent = oldText;
      });
  });

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
      var name =
        "sku-" +
        (currentItem && currentItem.id != null ? currentItem.id : "detail") +
        ".png";
      window.DomToImage.downloadDataUrl(lastCaptureDataUrl, name);
    });
  }

  bindListBack("/h5/chai/inner-sale.html");
  load();
  if (els.quoteBtn) {
    els.quoteBtn.addEventListener("click", function () {
      if (!currentItem || !window.ChaiSaleQuote) {
        return;
      }
      if (window.ChaiSaleQuote.has(currentItem.id)) {
        window.ChaiSaleQuote.toast("已在销售报价单中");
        syncQuoteBtn();
        return;
      }
      var ok = window.ChaiSaleQuote.add(currentItem);
      if (window.ChaiInnerUtil) {
        window.ChaiInnerUtil.toast(ok ? "已加入销售报价单" : "加入失败");
      } else if (window.ChaiSaleQuote.toast) {
        window.ChaiSaleQuote.toast(ok ? "已加入销售报价单" : "加入失败");
      }
      syncQuoteBtn();
    });
  }
  if (window.ChaiSaleQuote && window.ChaiSaleQuote.mountFab) {
    window.ChaiSaleQuote.mountFab();
    window.ChaiSaleQuote.onChange(syncQuoteBtn);
  }
})();
