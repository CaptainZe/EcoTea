/**
 * 内部回收价目列表：搜索 + 分页 + 看同款；无「新回收/仓」筛选。
 */
(function () {
  var LIST_URL_KEY = "chai.innerRecycle.listUrl";
  var state = {
    page: 1,
    size: 20,
    keyword: "",
    spuId: null,
    total: 0,
  };

  var gallery = { urls: [], index: 0 };
  var listAbort = null;
  var listLoadTimer = null;
  var listLoadToken = 0;
  var LIST_LOAD_DEBOUNCE_MS = 120;
  var itemMap = {};

  var els = {
    form: document.getElementById("searchForm"),
    keyword: document.getElementById("keyword"),
    meta: document.getElementById("meta"),
    list: document.getElementById("list"),
    empty: document.getElementById("empty"),
    pager: document.getElementById("pager"),
    pageInput: document.getElementById("pageInput"),
    pageTotal: document.getElementById("pageTotal"),
    pageGoBtn: document.getElementById("pageGoBtn"),
    prevBtn: document.getElementById("prevBtn"),
    nextBtn: document.getElementById("nextBtn"),
    filterBar: document.getElementById("filterBar"),
    filterText: document.getElementById("filterText"),
    clearFilterBtn: document.getElementById("clearFilterBtn"),
    gallery: document.getElementById("gallery"),
    galleryImg: document.getElementById("galleryImg"),
    galleryIndex: document.getElementById("galleryIndex"),
    galleryPrev: document.getElementById("galleryPrev"),
    galleryNext: document.getElementById("galleryNext"),
  };

  function qs(name) {
    var params = new URLSearchParams(window.location.search);
    var v = params.get(name);
    return v == null ? "" : String(v).trim();
  }

  function syncUrl() {
    var params = new URLSearchParams();
    if (state.keyword) {
      params.set("keyword", state.keyword);
    }
    if (state.spuId) {
      params.set("spuId", String(state.spuId));
    }
    if (state.page > 1) {
      params.set("page", String(state.page));
    }
    var q = params.toString();
    window.history.replaceState(
      null,
      "",
      window.location.pathname + (q ? "?" + q : "")
    );
    rememberListUrl();
  }

  function rememberListUrl() {
    try {
      sessionStorage.setItem(
        LIST_URL_KEY,
        window.location.pathname + window.location.search
      );
    } catch (e) {
      /* ignore */
    }
  }

  function jumpToPage() {
    var pages = Math.max(1, Math.ceil(state.total / state.size));
    var n = parseInt(els.pageInput && els.pageInput.value, 10);
    if (isNaN(n) || n < 1) {
      n = 1;
    }
    if (n > pages) {
      n = pages;
    }
    if (els.pageInput) {
      els.pageInput.value = String(n);
    }
    if (n === state.page) {
      return;
    }
    state.page = n;
    syncUrl();
    load();
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function updateFilterBar() {
    var inSameSpu = !!state.spuId;
    els.filterBar.hidden = !inSameSpu;
    els.clearFilterBtn.hidden = !inSameSpu;
    els.filterText.textContent = inSameSpu ? "正在查看同款" : "";
  }

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

  function imageUrlsOf(item) {
    var urls = item.imageUrls || item.image_urls || [];
    if (!urls.length && item.coverImage) {
      return [item.coverImage];
    }
    return urls;
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

  function formatOfficialShort(item) {
    if (!item.officialPriceShow || item.officialPriceShow === "-") {
      return "";
    }
    if (item.officialPriceShow === "非卖品") {
      return "官方 非卖品";
    }
    return "官方 ¥" + String(item.officialPriceShow).replace(/元$/, "");
  }

  /**
   * 辅文：破损价（含压价%）· 无提袋扣 ¥yy
   */
  function recycleExtraHtml(item) {
    var parts = [];
    if (item.recycleDamagePriceShow) {
      var dmg = String(item.recycleDamagePriceShow);
      if (item.recyclePriceReducePer != null && item.recyclePriceReducePer !== "") {
        dmg += "(" + item.recyclePriceReducePer + "%)";
      }
      parts.push("破损 " + dmg);
    }
    var noBag = item.recyclePriceReduceNoBagShow;
    if (noBag != null && String(noBag).trim() !== "") {
      parts.push("无提袋扣 ¥" + String(noBag).trim());
    }
    if (!parts.length) {
      return "";
    }
    return (
      '<p class="recycle-extra">' + escapeHtml(parts.join(" · ")) + "</p>"
    );
  }

  /** 不要求有货：统一「库存 n 件」；0 件标稀缺色。 */
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

  function isInQuote(id) {
    return !!(
      window.ChaiRecycleQuote &&
      window.ChaiRecycleQuote.has &&
      window.ChaiRecycleQuote.has(id)
    );
  }

  function addQuoteBtnHtml(item) {
    if (item.id == null) {
      return "";
    }
    var inQuote = isInQuote(item.id);
    return (
      '<button type="button" class="add-cart-btn' +
      (inQuote ? " is-in-cart" : "") +
      '" data-add-quote="' +
      escapeHtml(item.id) +
      '">' +
      (inQuote ? "已加入" : "加入报价") +
      "</button>"
    );
  }

  function syncAddButtons() {
    var buttons = els.list.querySelectorAll("[data-add-quote]");
    Array.prototype.forEach.call(buttons, function (btn) {
      var id = btn.getAttribute("data-add-quote");
      var inQuote = isInQuote(id);
      if (inQuote) {
        btn.classList.add("is-in-cart");
        btn.textContent = "已加入";
      } else {
        btn.classList.remove("is-in-cart");
        btn.textContent = "加入报价";
      }
    });
  }

  function renderItem(item) {
    var urls = imageUrlsOf(item);
    var coverHtml;
    if (urls.length) {
      var more =
        urls.length > 1
          ? '<span class="cover-more">+' + (urls.length - 1) + "</span>"
          : "";
      coverHtml =
        '<div class="cover-wrap" data-gallery-open="1" role="button" tabindex="0">' +
        '<img class="cover" src="' +
        escapeHtml(urls[0]) +
        '" alt="" loading="lazy"/>' +
        more +
        "</div>";
    } else {
      coverHtml = '<div class="cover-wrap placeholder">暂无图</div>';
    }

    var tags =
      tagHtml(item.gradeName) +
      tagHtml(item.specShow) +
      tagHtml(item.prodBatchShow) +
      tagHtml(item.expirationName);

    var sameCount =
      item.sameSpuCount != null ? item.sameSpuCount : item.sameSpuSaleCount;
    var same = "";
    if (item.spuId && sameCount != null && sameCount > 1) {
      same =
        '<button type="button" class="same-btn" data-spu="' +
        escapeHtml(item.spuId) +
        '">看同款</button>';
    }

    var detailHref =
      item.id != null
        ? "/h5/chai/inner-recycle-detail.html?id=" +
          encodeURIComponent(item.id)
        : "";

    var officialShort = formatOfficialShort(item);
    var discount =
      item.recycleDiscountShow || item.discountShow || "";
    var discountHtml = discount
      ? '<span class="discount">' + escapeHtml(discount) + "</span>"
      : "";

    return (
      '<li class="card" data-images="' +
      escapeHtml(JSON.stringify(urls)) +
      '">' +
      '<div class="card-media">' +
      coverHtml +
      "</div>" +
      '<div class="card-main">' +
      '<a class="body" href="' +
      escapeHtml(detailHref || "#") +
      '">' +
      '<p class="title">' +
      escapeHtml(item.title || item.name || "") +
      "</p>" +
      '<div class="tags">' +
      tags +
      "</div>" +
      '<div class="row-price">' +
      '<span class="sale">' +
      escapeHtml(formatRecyclePlain(item)) +
      "</span>" +
      (officialShort
        ? '<span class="official">' + escapeHtml(officialShort) + "</span>"
        : "") +
      discountHtml +
      "</div>" +
      recycleExtraHtml(item) +
      "</a>" +
      '<div class="card-foot">' +
      stockHtml(item) +
      '<div class="card-actions">' +
      same +
      addQuoteBtnHtml(item) +
      "</div></div></div></li>"
    );
  }

  function render(data) {
    state.total = data.total || 0;
    var list = data.list || [];
    itemMap = {};
    list.forEach(function (it) {
      if (it && it.id != null) {
        itemMap[String(it.id)] = it;
      }
    });
    els.list.innerHTML = list.map(renderItem).join("");
    els.empty.hidden = list.length > 0;

    var pages = Math.max(1, Math.ceil(state.total / state.size));
    var matchHint = "";
    if (data.matchType === "brand_exact") {
      matchHint = "（品牌精确）";
    } else if (data.matchType === "name_like") {
      matchHint = "（品名模糊）";
    } else if (data.matchType === "spu") {
      matchHint = "（同款）";
    }
    var kwHint = state.keyword ? " · " + escapeHtml(state.keyword) : "";
    els.meta.innerHTML =
      "共 <span class=\"meta-num\">" +
      escapeHtml(state.total) +
      "</span> 款" +
      escapeHtml(matchHint) +
      kwHint;

    els.pager.hidden = state.total <= state.size;
    if (els.pageInput) {
      els.pageInput.value = String(state.page);
      els.pageInput.max = String(pages);
    }
    if (els.pageTotal) {
      els.pageTotal.textContent = String(pages);
    }
    els.prevBtn.disabled = state.page <= 1;
    els.nextBtn.disabled = state.page >= pages;
  }

  function isAbortError(err) {
    return !!(
      err &&
      (err.name === "AbortError" ||
        err.code === 20 ||
        (typeof DOMException !== "undefined" &&
          err instanceof DOMException &&
          err.name === "AbortError"))
    );
  }

  function load() {
    if (listLoadTimer) {
      clearTimeout(listLoadTimer);
      listLoadTimer = null;
    }
    listLoadTimer = setTimeout(function () {
      listLoadTimer = null;
      loadNow();
    }, LIST_LOAD_DEBOUNCE_MS);
  }

  function loadNow() {
    if (listAbort) {
      try {
        listAbort.abort();
      } catch (e) {
        /* ignore */
      }
      listAbort = null;
    }
    var ctrl =
      typeof AbortController !== "undefined" ? new AbortController() : null;
    listAbort = ctrl;
    var token = ++listLoadToken;

    var params = new URLSearchParams();
    params.set("page", String(state.page));
    params.set("size", String(state.size));
    if (state.keyword) {
      params.set("keyword", state.keyword);
    }
    if (state.spuId) {
      params.set("spuId", String(state.spuId));
    }

    els.meta.textContent = "加载中…";
    var fetchOpts = ctrl ? { signal: ctrl.signal } : {};
    fetch("/chai/sku/recycle/list?" + params.toString(), fetchOpts)
      .then(function (res) {
        return res.json();
      })
      .then(function (body) {
        if (token !== listLoadToken) {
          return;
        }
        if (!body || body.code !== 0 || !body.data) {
          els.meta.textContent = "加载失败";
          els.list.innerHTML = "";
          els.empty.hidden = false;
          return;
        }
        render(body.data);
      })
      .catch(function (err) {
        if (token !== listLoadToken || isAbortError(err)) {
          return;
        }
        els.meta.textContent = "网络异常";
        els.list.innerHTML = "";
        els.empty.hidden = false;
      });
  }

  function openGallery(urls, index) {
    if (!urls || !urls.length) {
      return;
    }
    gallery.urls = urls;
    gallery.index = index || 0;
    els.gallery.hidden = false;
    paintGallery();
  }

  function closeGallery() {
    els.gallery.hidden = true;
    gallery.urls = [];
    gallery.index = 0;
  }

  function paintGallery() {
    if (!gallery.urls.length) {
      return;
    }
    els.galleryImg.src = gallery.urls[gallery.index];
    els.galleryIndex.textContent =
      gallery.index + 1 + " / " + gallery.urls.length;
    var multi = gallery.urls.length > 1;
    els.galleryPrev.hidden = !multi;
    els.galleryNext.hidden = !multi;
  }

  function galleryStep(delta) {
    if (gallery.urls.length <= 1) {
      return;
    }
    gallery.index =
      (gallery.index + delta + gallery.urls.length) % gallery.urls.length;
    paintGallery();
  }

  els.form.addEventListener("submit", function (e) {
    e.preventDefault();
    state.keyword = (els.keyword.value || "").trim();
    state.spuId = null;
    state.page = 1;
    updateFilterBar();
    syncUrl();
    load();
  });

  els.clearFilterBtn.addEventListener("click", function () {
    state.spuId = null;
    state.page = 1;
    updateFilterBar();
    syncUrl();
    load();
  });

  els.prevBtn.addEventListener("click", function () {
    if (state.page <= 1) {
      return;
    }
    state.page -= 1;
    syncUrl();
    load();
  });

  els.nextBtn.addEventListener("click", function () {
    var pages = Math.max(1, Math.ceil(state.total / state.size));
    if (state.page >= pages) {
      return;
    }
    state.page += 1;
    syncUrl();
    load();
  });

  if (els.pageGoBtn) {
    els.pageGoBtn.addEventListener("click", jumpToPage);
  }
  if (els.pageInput) {
    els.pageInput.addEventListener("keydown", function (e) {
      if (e.key === "Enter") {
        e.preventDefault();
        jumpToPage();
      }
    });
  }

  els.list.addEventListener("click", function (e) {
    var addBtn = e.target.closest("[data-add-quote]");
    if (addBtn) {
      e.preventDefault();
      e.stopPropagation();
      var addId = addBtn.getAttribute("data-add-quote");
      var item = itemMap[String(addId)];
      if (!item || !window.ChaiRecycleQuote) {
        return;
      }
      if (window.ChaiRecycleQuote.has(item.id)) {
        window.ChaiRecycleQuote.toast("已在报价单中");
        syncAddButtons();
        return;
      }
      var ok = window.ChaiRecycleQuote.add(item);
      window.ChaiRecycleQuote.toast(ok ? "已加入报价单" : "加入失败");
      syncAddButtons();
      return;
    }

    var sameBtn = e.target.closest(".same-btn");
    if (sameBtn) {
      var spu = sameBtn.getAttribute("data-spu");
      if (!spu) {
        return;
      }
      state.spuId = Number(spu);
      state.keyword = "";
      els.keyword.value = "";
      state.page = 1;
      updateFilterBar();
      syncUrl();
      load();
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }

    var cover = e.target.closest("[data-gallery-open]");
    if (!cover) {
      return;
    }
    var card = cover.closest(".card");
    if (!card) {
      return;
    }
    var raw = card.getAttribute("data-images") || "[]";
    var urls;
    try {
      urls = JSON.parse(raw);
    } catch (err) {
      urls = [];
    }
    openGallery(urls, 0);
  });

  els.gallery.addEventListener("click", function (e) {
    if (e.target && e.target.getAttribute("data-gallery-close") === "1") {
      closeGallery();
    }
  });
  els.galleryPrev.addEventListener("click", function (e) {
    e.stopPropagation();
    galleryStep(-1);
  });
  els.galleryNext.addEventListener("click", function (e) {
    e.stopPropagation();
    galleryStep(1);
  });

  state.keyword = qs("keyword");
  els.keyword.value = state.keyword;
  var spuRaw = qs("spuId");
  state.spuId = spuRaw ? Number(spuRaw) : null;
  if (state.spuId && isNaN(state.spuId)) {
    state.spuId = null;
  }
  var pageRaw = qs("page");
  state.page = pageRaw ? Math.max(1, parseInt(pageRaw, 10) || 1) : 1;
  updateFilterBar();
  rememberListUrl();
  load();
  if (window.ChaiRecycleQuote && window.ChaiRecycleQuote.mountFab) {
    window.ChaiRecycleQuote.mountFab();
    window.ChaiRecycleQuote.onChange(syncAddButtons);
  }
})();
