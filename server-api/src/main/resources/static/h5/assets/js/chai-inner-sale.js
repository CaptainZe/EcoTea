(function () {
  var LIST_URL_KEY = "chai.innerSale.listUrl";
  var state = {
    page: 1,
    size: 20,
    keyword: "",
    spuId: null,
    whId: null,
    recycleRecent: false,
    total: 0,
  };

  var gallery = { urls: [], index: 0, touchX: null };
  var warehouses = [];
  var listAbort = null;
  var listLoadTimer = null;
  var listLoadToken = 0;
  var LIST_LOAD_DEBOUNCE_MS = 120;

  var els = {
    form: document.getElementById("searchForm"),
    keyword: document.getElementById("keyword"),
    recycleSelect: document.getElementById("recycleSelect"),
    whSelect: document.getElementById("whSelect"),
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
    if (state.whId) {
      params.set("whId", String(state.whId));
    }
    if (state.recycleRecent) {
      params.set("recycleRecent", "1");
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
    els.filterText.textContent = inSameSpu ? "正在查看同款现货" : "";
  }

  function readWhIdFromSelect() {
    if (!els.whSelect) {
      return null;
    }
    var v = (els.whSelect.value || "").trim();
    if (!v) {
      return null;
    }
    var n = Number(v);
    return isNaN(n) ? null : n;
  }

  function readRecycleRecentFromSelect() {
    if (!els.recycleSelect) {
      return false;
    }
    return (els.recycleSelect.value || "").trim() === "1";
  }

  function syncKeywordFromInput() {
    state.keyword = (els.keyword.value || "").trim();
  }

  function applyFiltersAndReload() {
    syncKeywordFromInput();
    state.whId = readWhIdFromSelect();
    state.recycleRecent = readRecycleRecentFromSelect();
    state.page = 1;
    syncUrl();
    load();
  }

  function whShortLabel(whId) {
    for (var i = 0; i < warehouses.length; i++) {
      if (String(warehouses[i].id) === String(whId)) {
        return warehouses[i].shortName || warehouses[i].name || "";
      }
    }
    return "";
  }

  function fillWhSelect() {
    if (!els.whSelect) {
      return;
    }
    var html = '<option value="">全部仓</option>';
    for (var i = 0; i < warehouses.length; i++) {
      var w = warehouses[i];
      var label = w.shortName || w.name || String(w.id);
      html +=
        '<option value="' +
        escapeHtml(w.id) +
        '" title="' +
        escapeHtml(w.name || "") +
        '">' +
        escapeHtml(label) +
        "</option>";
    }
    els.whSelect.innerHTML = html;
    if (state.whId) {
      els.whSelect.value = String(state.whId);
      if (els.whSelect.value !== String(state.whId)) {
        state.whId = null;
      }
    }
  }

  function loadWarehouses() {
    return fetch("/chai/warehouse/online")
      .then(function (res) {
        return res.json();
      })
      .then(function (body) {
        if (body && body.code === 0 && Array.isArray(body.data)) {
          warehouses = body.data;
        } else {
          warehouses = [];
        }
        fillWhSelect();
      })
      .catch(function () {
        warehouses = [];
        fillWhSelect();
      });
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

  function whChipsHtml(item) {
    var names = item.whShortNames || item.wh_short_names || [];
    if (!names.length) {
      return "";
    }
    var chips = names
      .map(function (name) {
        if (!name) {
          return "";
        }
        return '<span class="wh-chip">' + escapeHtml(name) + "</span>";
      })
      .join("");
    if (!chips) {
      return "";
    }
    return '<div class="wh-chips">' + chips + "</div>";
  }

  function imageUrlsOf(item) {
    var urls = item.imageUrls || item.image_urls || [];
    if (!urls.length && item.coverImage) {
      return [item.coverImage];
    }
    return urls;
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

  function stockHtml(item) {
    if (window.ChaiSaleStock && window.ChaiSaleStock.renderHtml) {
      return window.ChaiSaleStock.renderHtml(item);
    }
    return "";
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

    var same = "";
    if (item.spuId && item.sameSpuSaleCount != null && item.sameSpuSaleCount > 1) {
      same =
        '<button type="button" class="same-btn" data-spu="' +
        escapeHtml(item.spuId) +
        '">看同款</button>';
    }

    var detailHref =
      item.id != null
        ? "/h5/chai/inner-sale-detail.html?id=" + encodeURIComponent(item.id)
        : "";

    var officialShort = formatOfficialShort(item);
    var discountHtml = item.discountShow
      ? '<span class="discount">' + escapeHtml(item.discountShow) + "</span>"
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
      escapeHtml(formatSalePlain(item)) +
      "</span>" +
      (officialShort
        ? '<span class="official">' + escapeHtml(officialShort) + "</span>"
        : "") +
      discountHtml +
      "</div>" +
      "</a>" +
      '<div class="card-foot">' +
      stockHtml(item) +
      whChipsHtml(item) +
      '<div class="card-actions">' +
      same +
      "</div></div></div></li>"
    );
  }

  function render(data) {
    state.total = data.total || 0;
    var list = data.list || [];
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
    var whHint = "";
    if (state.whId) {
      var label = whShortLabel(state.whId);
      whHint = label
        ? " · 仓 " + escapeHtml(label)
        : " · 仓筛选";
    }
    els.meta.innerHTML =
      "共 <span class=\"meta-num\">" +
      escapeHtml(state.total) +
      "</span> 款现货" +
      escapeHtml(matchHint) +
      kwHint +
      whHint;

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
    params.set("includeWh", "1");
    if (state.keyword) {
      params.set("keyword", state.keyword);
    }
    if (state.spuId) {
      params.set("spuId", String(state.spuId));
    }
    if (state.whId) {
      params.set("whId", String(state.whId));
    }
    if (state.recycleRecent) {
      params.set("recycleRecent", "1");
    }

    els.meta.textContent = "加载中…";
    var fetchOpts = ctrl ? { signal: ctrl.signal } : {};
    fetch("/chai/sku/sale/list?" + params.toString(), fetchOpts)
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
    state.whId = readWhIdFromSelect();
    state.recycleRecent = readRecycleRecentFromSelect();
    state.spuId = null;
    state.page = 1;
    updateFilterBar();
    syncUrl();
    load();
  });

  if (els.recycleSelect) {
    els.recycleSelect.addEventListener("change", applyFiltersAndReload);
  }
  if (els.whSelect) {
    els.whSelect.addEventListener("change", applyFiltersAndReload);
  }

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
  var whRaw = qs("whId");
  state.whId = whRaw ? Number(whRaw) : null;
  if (state.whId && isNaN(state.whId)) {
    state.whId = null;
  }
  state.recycleRecent = qs("recycleRecent") === "1";
  if (els.recycleSelect) {
    els.recycleSelect.value = state.recycleRecent ? "1" : "";
  }
  var pageRaw = qs("page");
  state.page = pageRaw ? Math.max(1, parseInt(pageRaw, 10) || 1) : 1;
  updateFilterBar();
  rememberListUrl();
  loadWarehouses().then(function () {
    load();
  });
})();
