/**
 * 内部回收价目详情：轮播 / 属性 / 库存 / 分仓；底栏加入报价单（无单品导出海报）。
 */
(function () {
  var LIST_URL_KEY = "chai.innerRecycle.listUrl";
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
    recyclePrice: document.getElementById("recyclePrice"),
    officialPrice: document.getElementById("officialPrice"),
    discount: document.getElementById("discount"),
    recycleExtra: document.getElementById("recycleExtra"),
    stock: document.getElementById("stock"),
    cardAttrs: document.getElementById("cardAttrs"),
    cardWhStock: document.getElementById("cardWhStock"),
    bar: document.getElementById("bar"),
    sameBtn: document.getElementById("sameBtn"),
    quoteBtn: document.getElementById("quoteBtn"),
    gallery: document.getElementById("gallery"),
    galleryImg: document.getElementById("galleryImg"),
    galleryIndex: document.getElementById("galleryIndex"),
    galleryPrev: document.getElementById("galleryPrev"),
    galleryNext: document.getElementById("galleryNext"),
    backLink: document.getElementById("backLink"),
  };

  var currentItem = null;
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
    if (saved && saved.indexOf("/h5/chai/inner-recycle.html") === 0) {
      els.backLink.href = saved;
    } else {
      els.backLink.href = defaultHref || "/h5/chai/inner-recycle.html";
    }
  }

  function escapeHtml(s) {
    return String(s == null ? "" : s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function displayImageUrls(item) {
    var urls = item.imageUrls || item.image_urls || [];
    if (!urls.length && (item.coverImage || item.cover_image)) {
      return [item.coverImage || item.cover_image];
    }
    return urls.slice();
  }

  function formatRecyclePlain(item) {
    if (item.recyclePrice != null && item.recyclePrice !== "") {
      var n = Number(item.recyclePrice);
      if (!isNaN(n)) {
        return "¥ " + String(n);
      }
    }
    if (item.recyclePriceShow) {
      return String(item.recyclePriceShow).replace(/\(.*$/, "").trim();
    }
    return "-";
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

  function formatRecycleExtra(item) {
    var parts = [];
    if (item.recycleDamagePriceShow) {
      var dmg = String(item.recycleDamagePriceShow);
      if (
        item.recyclePriceReducePer != null &&
        item.recyclePriceReducePer !== ""
      ) {
        dmg += "(" + item.recyclePriceReducePer + "%)";
      }
      parts.push("破损 " + dmg);
    }
    var noBag = item.recyclePriceReduceNoBagShow;
    if (noBag != null && String(noBag).trim() !== "") {
      parts.push("无提袋扣 ¥" + String(noBag).trim());
    }
    return parts.join(" · ");
  }

  function stockHtml(item) {
    var qty = item.totalQty != null ? Number(item.totalQty) : 0;
    if (isNaN(qty) || qty < 0) {
      qty = 0;
    }
    var scarce = qty <= 3;
    var label = "库存 " + qty + " 件";
    var dmg = "";
    if (item.damageQty != null && item.damageQty > 0) {
      dmg =
        '<span class="dmg">破损 ' + escapeHtml(item.damageQty) + "</span>";
    }
    return (
      '<span class="stock ' +
      (scarce ? "scarce" : "normal") +
      '">' +
      escapeHtml(label) +
      dmg +
      "</span>"
    );
  }

  function showUnavailable(msg) {
    els.detail.hidden = true;
    els.bar.hidden = true;
    els.status.hidden = false;
    var back =
      (els.backLink && els.backLink.getAttribute("href")) ||
      "/h5/chai/inner-recycle.html";
    els.status.innerHTML =
      escapeHtml(msg || "暂不可查") +
      '<br/><br/><a href="' +
      escapeHtml(back) +
      '">返回回收价目</a>';
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
        var dmg = "";
        if (row.damageQty != null && Number(row.damageQty) > 0) {
          dmg =
            '<span class="wh-stock-dmg">破损 ' +
            escapeHtml(row.damageQty) +
            "</span>";
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

  function bindSameBtn(item) {
    if (!els.sameBtn) {
      return;
    }
    var sameCount =
      item.sameSpuCount != null ? item.sameSpuCount : item.sameSpuSaleCount;
    if (item.spuId && sameCount != null && sameCount > 1) {
      els.sameBtn.hidden = false;
      els.sameBtn.href =
        "/h5/chai/inner-recycle.html?spuId=" +
        encodeURIComponent(item.spuId);
    } else {
      els.sameBtn.hidden = true;
      els.sameBtn.removeAttribute("href");
    }
  }

  function syncQuoteBtn() {
    if (!els.quoteBtn) {
      return;
    }
    var inQuote =
      currentItem &&
      window.ChaiRecycleQuote &&
      window.ChaiRecycleQuote.has &&
      window.ChaiRecycleQuote.has(currentItem.id);
    if (inQuote) {
      els.quoteBtn.textContent = "已加入报价单";
      els.quoteBtn.disabled = true;
      els.quoteBtn.classList.add("is-in-quote");
    } else {
      els.quoteBtn.textContent = "加入报价单";
      els.quoteBtn.disabled = false;
      els.quoteBtn.classList.remove("is-in-quote");
    }
  }

  function render(item) {
    currentItem = item;
    var urls = displayImageUrls(item);
    document.title =
      (item.name || item.title || "回收详情") + " · 内部";

    renderCarousel(urls);

    if (item.brandName) {
      els.brand.hidden = false;
      els.brand.textContent = item.brandName;
    } else {
      els.brand.hidden = true;
      els.brand.textContent = "";
    }
    els.skuName.textContent = item.name || item.title || "";
    els.recyclePrice.textContent = formatRecyclePlain(item);

    var official = formatOfficialPlain(item);
    if (official) {
      els.officialPrice.hidden = false;
      els.officialPrice.textContent = official;
    } else {
      els.officialPrice.hidden = true;
      els.officialPrice.textContent = "";
    }

    var discount = item.recycleDiscountShow || item.discountShow || "";
    if (discount) {
      els.discount.hidden = false;
      els.discount.textContent = discount;
    } else {
      els.discount.hidden = true;
      els.discount.textContent = "";
    }

    var extra = formatRecycleExtra(item);
    if (els.recycleExtra) {
      if (extra) {
        els.recycleExtra.hidden = false;
        els.recycleExtra.textContent = extra;
      } else {
        els.recycleExtra.hidden = true;
        els.recycleExtra.textContent = "";
      }
    }

    if (els.stock) {
      els.stock.innerHTML = stockHtml(item);
    }

    renderAttrs(item);
    renderWhStock(item);
    bindSameBtn(item);
    syncQuoteBtn();

    els.status.hidden = true;
    els.detail.hidden = false;
    els.bar.hidden = false;
  }

  function load() {
    if (!id) {
      showUnavailable("缺少商品编号");
      return;
    }
    fetch("/chai/sku/recycle/detail?id=" + encodeURIComponent(id))
      .then(function (res) {
        return res.json();
      })
      .then(function (body) {
        if (!body || body.code !== 0 || !body.data) {
          showUnavailable(
            (body && body.message) || "商品不存在或暂不可查"
          );
          return;
        }
        render(body.data);
      })
      .catch(function () {
        showUnavailable("网络异常，请稍后重试");
      });
  }

  if (els.quoteBtn) {
    els.quoteBtn.addEventListener("click", function () {
      if (!currentItem || !window.ChaiRecycleQuote) {
        return;
      }
      if (window.ChaiRecycleQuote.has(currentItem.id)) {
        window.ChaiRecycleQuote.toast("已在报价单中");
        syncQuoteBtn();
        return;
      }
      var ok = window.ChaiRecycleQuote.add(currentItem);
      if (window.ChaiInnerUtil) {
        window.ChaiInnerUtil.toast(ok ? "已加入报价单" : "加入失败");
      }
      syncQuoteBtn();
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

  bindListBack("/h5/chai/inner-recycle.html");
  if (window.ChaiRecycleQuote && window.ChaiRecycleQuote.mountFab) {
    window.ChaiRecycleQuote.mountFab();
    window.ChaiRecycleQuote.onChange(syncQuoteBtn);
  }
  load();
})();
