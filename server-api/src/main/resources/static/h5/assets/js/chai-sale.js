(function () {
  var LIST_URL_KEY = "chai.sale.listUrl";
  var state = {
    page: 1,
    size: 20,
    keyword: "",
    barcode: "",
    spuId: null,
    recycleRecent: false,
    brandIds: [],
    types: [],
    priceMin: null,
    priceMax: null,
    officialPriceMin: null,
    officialPriceMax: null,
    nonSale: null,
    total: 0
  };

  var filterMeta = {
    brands: [],
    types: [],
    loaded: false,
    loading: false
  };

  var filterDraft = {
    brandIds: [],
    types: [],
    priceMin: null,
    priceMax: null,
    officialPriceMin: null,
    officialPriceMax: null,
    nonSale: null,
    brandKw: "",
    brandExpanded: false
  };

  var FILTER_ICON_OFF = "/h5/assets/image/filter_off.png";
  var FILTER_ICON_ON = "/h5/assets/image/filter_on.png";

  var itemMap = {};

  var gallery = {
    urls: [],
    index: 0,
    touchX: null
  };
  var listAbort = null;
  var listLoadTimer = null;
  var listLoadToken = 0;
  var LIST_LOAD_DEBOUNCE_MS = 120;

  var els = {
    form: document.getElementById("searchForm"),
    keyword: document.getElementById("keyword"),
    recycleSelect: document.getElementById("recycleSelect"),
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
    banner: document.getElementById("banner"),
    noticeModal: document.getElementById("noticeModal"),
    noticeTitle: document.getElementById("noticeTitle"),
    noticeBody: document.getElementById("noticeBody"),
    noticeOk: document.getElementById("noticeOk"),
    gallery: document.getElementById("gallery"),
    galleryImg: document.getElementById("galleryImg"),
    galleryIndex: document.getElementById("galleryIndex"),
    galleryPrev: document.getElementById("galleryPrev"),
    galleryNext: document.getElementById("galleryNext"),
    filterEntryBtn: document.getElementById("filterEntryBtn"),
    filterEntryIcon: document.getElementById("filterEntryIcon"),
    filterDrawer: document.getElementById("filterDrawer"),
    brandFilterKw: document.getElementById("brandFilterKw"),
    brandTags: document.getElementById("brandTags"),
    typeTags: document.getElementById("typeTags"),
    priceMinInput: document.getElementById("priceMinInput"),
    priceMaxInput: document.getElementById("priceMaxInput"),
    officialPriceMinInput: document.getElementById("officialPriceMinInput"),
    officialPriceMaxInput: document.getElementById("officialPriceMaxInput"),
    officialPriceHint: document.getElementById("officialPriceHint"),
    nonSaleTags: document.getElementById("nonSaleTags"),
    filterResetBtn: document.getElementById("filterResetBtn"),
    filterConfirmBtn: document.getElementById("filterConfirmBtn"),
    barcodeScanBtn: document.getElementById("barcodeScanBtn")
  };

  /** localStorage：存当天日期，同一天关闭后不再弹 */
  var NOTICE_KEY = "chai_sale_notice_day";

  function todayStr() {
    var d = new Date();
    return d.getFullYear() + "-" + (d.getMonth() + 1) + "-" + d.getDate();
  }

  function qs(name) {
    var params = new URLSearchParams(window.location.search);
    var v = params.get(name);
    return v == null ? "" : String(v).trim();
  }

  function syncUrl() {
    var params = new URLSearchParams();
    if (state.barcode) {
      params.set("barcode", state.barcode);
    } else if (state.keyword) {
      params.set("keyword", state.keyword);
    }
    if (state.spuId) {
      params.set("spuId", String(state.spuId));
    }
    if (state.recycleRecent) {
      params.set("recycleRecent", "1");
    }
    if (state.brandIds && state.brandIds.length) {
      params.set("brandIds", state.brandIds.join(","));
    }
    if (state.types && state.types.length) {
      params.set("types", state.types.join(","));
    }
    if (state.priceMin != null && state.priceMin !== "") {
      params.set("priceMin", String(state.priceMin));
    }
    if (state.priceMax != null && state.priceMax !== "") {
      params.set("priceMax", String(state.priceMax));
    }
    if (state.nonSale === 0 || state.nonSale === 1) {
      params.set("nonSale", String(state.nonSale));
    }
    if (state.nonSale !== 1) {
      if (state.officialPriceMin != null && state.officialPriceMin !== "") {
        params.set("officialPriceMin", String(state.officialPriceMin));
      }
      if (state.officialPriceMax != null && state.officialPriceMax !== "") {
        params.set("officialPriceMax", String(state.officialPriceMax));
      }
    }
    if (state.page > 1) {
      params.set("page", String(state.page));
    }
    var q = params.toString();
    var next = window.location.pathname + (q ? "?" + q : "");
    window.history.replaceState(null, "", next);
    rememberListUrl();
  }

  function parseIdList(raw, asInt) {
    if (!raw) {
      return [];
    }
    var parts = String(raw).split(",");
    var out = [];
    var seen = {};
    for (var i = 0; i < parts.length; i++) {
      var s = String(parts[i] || "").trim();
      if (!s) {
        continue;
      }
      var n = asInt ? parseInt(s, 10) : Number(s);
      if (isNaN(n)) {
        continue;
      }
      var key = String(n);
      if (seen[key]) {
        continue;
      }
      seen[key] = true;
      out.push(n);
    }
    return out;
  }

  function parsePrice(raw) {
    if (raw == null || raw === "") {
      return null;
    }
    var n = Number(raw);
    if (isNaN(n) || n < 0) {
      return null;
    }
    return n;
  }

  function hasActiveDrawerFilter() {
    return (
      (state.brandIds && state.brandIds.length > 0) ||
      (state.types && state.types.length > 0) ||
      state.priceMin != null ||
      state.priceMax != null ||
      state.officialPriceMin != null ||
      state.officialPriceMax != null ||
      state.nonSale === 0 ||
      state.nonSale === 1
    );
  }

  function parseNonSale(raw) {
    if (raw == null || raw === "") {
      return null;
    }
    if (String(raw) === "1") {
      return 1;
    }
    if (String(raw) === "0") {
      return 0;
    }
    return null;
  }

  function syncOfficialPriceInputsDisabled() {
    var onlyNonSale = filterDraft.nonSale === 1;
    if (els.officialPriceMinInput) {
      els.officialPriceMinInput.disabled = onlyNonSale;
    }
    if (els.officialPriceMaxInput) {
      els.officialPriceMaxInput.disabled = onlyNonSale;
    }
    if (els.officialPriceHint) {
      els.officialPriceHint.hidden = !onlyNonSale;
    }
  }

  function renderNonSaleTags() {
    if (!els.nonSaleTags) {
      return;
    }
    var buttons = els.nonSaleTags.querySelectorAll("[data-non-sale]");
    for (var i = 0; i < buttons.length; i++) {
      var btn = buttons[i];
      var val = parseNonSale(btn.getAttribute("data-non-sale"));
      var on =
        (val == null && filterDraft.nonSale == null) ||
        (val != null && filterDraft.nonSale === val);
      if (on) {
        btn.classList.add("is-on");
      } else {
        btn.classList.remove("is-on");
      }
    }
    syncOfficialPriceInputsDisabled();
  }

  function updateFilterEntryIcon() {
    if (!els.filterEntryIcon) {
      return;
    }
    els.filterEntryIcon.src = hasActiveDrawerFilter()
      ? FILTER_ICON_ON
      : FILTER_ICON_OFF;
  }

  function cloneIdList(list) {
    return (list || []).slice();
  }

  function draftFromState() {
    filterDraft.brandIds = cloneIdList(state.brandIds);
    filterDraft.types = cloneIdList(state.types);
    filterDraft.priceMin = state.priceMin;
    filterDraft.priceMax = state.priceMax;
    filterDraft.officialPriceMin = state.officialPriceMin;
    filterDraft.officialPriceMax = state.officialPriceMax;
    filterDraft.nonSale = state.nonSale;
    filterDraft.brandKw = "";
    filterDraft.brandExpanded = false;
    if (els.brandFilterKw) {
      els.brandFilterKw.value = "";
    }
    if (els.priceMinInput) {
      els.priceMinInput.value =
        state.priceMin != null ? String(state.priceMin) : "";
    }
    if (els.priceMaxInput) {
      els.priceMaxInput.value =
        state.priceMax != null ? String(state.priceMax) : "";
    }
    if (els.officialPriceMinInput) {
      els.officialPriceMinInput.value =
        state.officialPriceMin != null ? String(state.officialPriceMin) : "";
    }
    if (els.officialPriceMaxInput) {
      els.officialPriceMaxInput.value =
        state.officialPriceMax != null ? String(state.officialPriceMax) : "";
    }
    syncOfficialPriceInputsDisabled();
  }

  function idInList(list, id) {
    var key = String(id);
    for (var i = 0; i < list.length; i++) {
      if (String(list[i]) === key) {
        return true;
      }
    }
    return false;
  }

  function toggleIdInList(list, id) {
    var key = String(id);
    for (var i = 0; i < list.length; i++) {
      if (String(list[i]) === key) {
        list.splice(i, 1);
        return;
      }
    }
    list.push(id);
  }

  function renderBrandTags() {
    if (!els.brandTags) {
      return;
    }
    if (!window.ChaiListFilter || !window.ChaiListFilter.renderBrandTagsHtml) {
      els.brandTags.innerHTML = '<p class="filter-tags-empty">加载中…</p>';
      return;
    }
    els.brandTags.innerHTML = window.ChaiListFilter.renderBrandTagsHtml({
      brands: filterMeta.brands || [],
      selectedIds: filterDraft.brandIds,
      keyword: filterDraft.brandKw,
      expanded: filterDraft.brandExpanded,
      escapeHtml: escapeHtml
    });
  }

  function renderTypeTags() {
    if (!els.typeTags) {
      return;
    }
    var html = [];
    var types = filterMeta.types || [];
    for (var i = 0; i < types.length; i++) {
      var t = types[i];
      if (!t || t.code == null || t.code === "") {
        continue;
      }
      var codeNum = parseInt(t.code, 10);
      var codeVal = isNaN(codeNum) ? t.code : codeNum;
      var on = idInList(filterDraft.types, codeVal);
      html.push(
        '<button type="button" class="filter-tag' +
          (on ? " is-on" : "") +
          '" data-type-code="' +
          escapeHtml(t.code) +
          '">' +
          escapeHtml(t.text || t.code) +
          "</button>"
      );
    }
    if (!html.length) {
      els.typeTags.innerHTML = '<p class="filter-tags-empty">暂无茶类</p>';
      return;
    }
    els.typeTags.innerHTML = html.join("");
  }

  function renderFilterDraft() {
    renderBrandTags();
    renderTypeTags();
    renderNonSaleTags();
  }

  function ensureFilterMeta() {
    if (filterMeta.loaded || filterMeta.loading) {
      return Promise.resolve();
    }
    filterMeta.loading = true;
    return Promise.all([
      fetch("/chai/brand/online").then(function (res) {
        return res.json();
      }),
      fetch("/chai/dict/options?label=CHAI_TYPE").then(function (res) {
        return res.json();
      })
    ])
      .then(function (parts) {
        filterMeta.loading = false;
        filterMeta.loaded = true;
        var brandBody = parts[0];
        var typeBody = parts[1];
        filterMeta.brands =
          brandBody && brandBody.code === 0 && brandBody.data
            ? brandBody.data
            : [];
        filterMeta.types =
          typeBody && typeBody.code === 0 && typeBody.data ? typeBody.data : [];
      })
      .catch(function () {
        filterMeta.loading = false;
        filterMeta.loaded = true;
        filterMeta.brands = [];
        filterMeta.types = [];
      });
  }

  function openFilterDrawer() {
    if (!els.filterDrawer) {
      return;
    }
    draftFromState();
    els.filterDrawer.hidden = false;
    document.body.style.overflow = "hidden";
    ensureFilterMeta().then(function () {
      renderFilterDraft();
    });
    renderFilterDraft();
  }

  function closeFilterDrawer() {
    if (!els.filterDrawer) {
      return;
    }
    els.filterDrawer.hidden = true;
    if (els.gallery && !els.gallery.hidden) {
      return;
    }
    document.body.style.overflow = "";
  }

  function applyFilterDraftAndReload() {
    var min = parsePrice(els.priceMinInput && els.priceMinInput.value);
    var max = parsePrice(els.priceMaxInput && els.priceMaxInput.value);
    if (min != null && max != null && min > max) {
      var tmp = min;
      min = max;
      max = tmp;
    }
    var nonSale = filterDraft.nonSale;
    var oMin = null;
    var oMax = null;
    if (nonSale !== 1) {
      oMin = parsePrice(
        els.officialPriceMinInput && els.officialPriceMinInput.value
      );
      oMax = parsePrice(
        els.officialPriceMaxInput && els.officialPriceMaxInput.value
      );
      if (oMin != null && oMax != null && oMin > oMax) {
        var oTmp = oMin;
        oMin = oMax;
        oMax = oTmp;
      }
    }
    state.brandIds = cloneIdList(filterDraft.brandIds);
    state.types = cloneIdList(filterDraft.types);
    state.priceMin = min;
    state.priceMax = max;
    state.nonSale = nonSale;
    state.officialPriceMin = oMin;
    state.officialPriceMax = oMax;
    state.barcode = "";
    state.page = 1;
    updateFilterEntryIcon();
    closeFilterDrawer();
    updateFilterBar();
    syncUrl();
    load();
  }

  function resetFilterDraft() {
    filterDraft.brandIds = [];
    filterDraft.types = [];
    filterDraft.priceMin = null;
    filterDraft.priceMax = null;
    filterDraft.officialPriceMin = null;
    filterDraft.officialPriceMax = null;
    filterDraft.nonSale = null;
    filterDraft.brandKw = "";
    filterDraft.brandExpanded = false;
    if (els.brandFilterKw) {
      els.brandFilterKw.value = "";
    }
    if (els.priceMinInput) {
      els.priceMinInput.value = "";
    }
    if (els.priceMaxInput) {
      els.priceMaxInput.value = "";
    }
    if (els.officialPriceMinInput) {
      els.officialPriceMinInput.value = "";
    }
    if (els.officialPriceMaxInput) {
      els.officialPriceMaxInput.value = "";
    }
    renderFilterDraft();
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
    var inBarcode = !!state.barcode;
    els.filterBar.hidden = !inSameSpu && !inBarcode;
    els.clearFilterBtn.hidden = !inSameSpu && !inBarcode;
    if (inBarcode) {
      els.filterText.textContent = "条码：" + state.barcode;
    } else if (inSameSpu) {
      els.filterText.textContent = "正在查看同款现货";
    } else {
      els.filterText.textContent = "";
    }
  }

  function clearBarcodeAndOthersForScan(code) {
    state.barcode = String(code || "").replace(/\D/g, "").trim();
    state.keyword = "";
    els.keyword.value = "";
    state.spuId = null;
    state.recycleRecent = false;
    state.brandIds = [];
    state.types = [];
    state.priceMin = null;
    state.priceMax = null;
    state.officialPriceMin = null;
    state.officialPriceMax = null;
    state.nonSale = null;
    if (els.recycleSelect) {
      els.recycleSelect.value = "";
    }
    updateFilterEntryIcon();
    state.page = 1;
    updateFilterBar();
    syncUrl();
    load();
    window.scrollTo({ top: 0, behavior: "smooth" });
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

  function isInCart(id) {
    if (!window.ChaiInquiryCart || id == null) {
      return false;
    }
    var items = window.ChaiInquiryCart.getItems();
    for (var i = 0; i < items.length; i++) {
      if (String(items[i].id) === String(id)) {
        return true;
      }
    }
    return false;
  }

  function stockHtml(item) {
    if (window.ChaiSaleStock && window.ChaiSaleStock.renderHtml) {
      return window.ChaiSaleStock.renderHtml(item);
    }
    return "";
  }

  function addCartBtnHtml(item) {
    if (item.id == null) {
      return "";
    }
    var inCart = isInCart(item.id);
    return (
      '<button type="button" class="add-cart-btn' +
      (inCart ? " is-in-cart" : "") +
      '" data-add-id="' +
      escapeHtml(item.id) +
      '">' +
      (inCart ? "已加入" : "加入询价") +
      "</button>"
    );
  }

  function syncAddButtons() {
    var buttons = els.list.querySelectorAll("[data-add-id]");
    Array.prototype.forEach.call(buttons, function (btn) {
      var id = btn.getAttribute("data-add-id");
      var inCart = isInCart(id);
      if (inCart) {
        btn.classList.add("is-in-cart");
        btn.textContent = "已加入";
      } else {
        btn.classList.remove("is-in-cart");
        btn.textContent = "加入询价";
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

    var same = "";
    if (item.spuId && item.sameSpuSaleCount != null && item.sameSpuSaleCount > 1) {
      same =
        '<button type="button" class="same-btn" data-spu="' +
        escapeHtml(item.spuId) +
        '">看同款</button>';
    }

    var detailHref =
      item.id != null
        ? "/h5/chai/sale-detail.html?id=" + encodeURIComponent(item.id)
        : "";

    var officialShort = formatOfficialShort(item);
    var discountHtml = item.discountShow
      ? '<span class="discount">' + escapeHtml(item.discountShow) + "</span>"
      : "";

    return (
      '<li class="card" data-images="' +
      escapeHtml(JSON.stringify(urls)) +
      '"' +
      (detailHref ? ' data-detail="' + escapeHtml(detailHref) + '"' : "") +
      ">" +
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
      '<div class="card-actions">' +
      same +
      addCartBtnHtml(item) +
      "</div></div></div></li>"
    );
  }

  function render(data) {
    state.total = data.total || 0;
    var list = data.list || [];
    itemMap = {};
    list.forEach(function (item) {
      if (item && item.id != null) {
        itemMap[String(item.id)] = item;
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
    } else if (data.matchType === "barcode") {
      matchHint = "（条码精确）";
    }
    var kwHint = "";
    if (state.barcode) {
      kwHint = " · " + escapeHtml(state.barcode);
    } else if (state.keyword) {
      kwHint = " · " + escapeHtml(state.keyword);
    }
    els.meta.innerHTML =
      "共 <span class=\"meta-num\">" +
      escapeHtml(state.total) +
      "</span> 款现货" +
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
    if (state.barcode) {
      params.set("barcode", state.barcode);
    } else if (state.keyword) {
      params.set("keyword", state.keyword);
    }
    if (state.spuId) {
      params.set("spuId", String(state.spuId));
    }
    if (state.recycleRecent) {
      params.set("recycleRecent", "1");
    }
    if (state.brandIds && state.brandIds.length) {
      params.set("brandIds", state.brandIds.join(","));
    }
    if (state.types && state.types.length) {
      params.set("types", state.types.join(","));
    }
    if (state.priceMin != null && state.priceMin !== "") {
      params.set("priceMin", String(state.priceMin));
    }
    if (state.priceMax != null && state.priceMax !== "") {
      params.set("priceMax", String(state.priceMax));
    }
    if (state.nonSale === 0 || state.nonSale === 1) {
      params.set("nonSale", String(state.nonSale));
    }
    if (state.nonSale !== 1) {
      if (state.officialPriceMin != null && state.officialPriceMin !== "") {
        params.set("officialPriceMin", String(state.officialPriceMin));
      }
      if (state.officialPriceMax != null && state.officialPriceMax !== "") {
        params.set("officialPriceMax", String(state.officialPriceMax));
      }
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

  function paintGallery() {
    if (!gallery.urls.length) {
      return;
    }
    els.galleryImg.src = gallery.urls[gallery.index];
    els.galleryIndex.textContent = gallery.index + 1 + " / " + gallery.urls.length;
    var multi = gallery.urls.length > 1;
    els.galleryPrev.hidden = !multi;
    els.galleryNext.hidden = !multi;
  }

  function openGallery(urls, startIndex) {
    if (!urls || !urls.length) {
      return;
    }
    gallery.urls = urls;
    gallery.index = Math.max(0, Math.min(startIndex || 0, urls.length - 1));
    paintGallery();
    els.gallery.hidden = false;
    document.body.style.overflow = "hidden";
  }

  function closeGallery() {
    els.gallery.hidden = true;
    gallery.urls = [];
    gallery.index = 0;
    if (els.filterDrawer && !els.filterDrawer.hidden) {
      return;
    }
    document.body.style.overflow = "";
  }

  function galleryStep(delta) {
    if (gallery.urls.length <= 1) {
      return;
    }
    gallery.index = (gallery.index + delta + gallery.urls.length) % gallery.urls.length;
    paintGallery();
  }

  els.form.addEventListener("submit", function (e) {
    e.preventDefault();
    state.keyword = (els.keyword.value || "").trim();
    state.barcode = "";
    state.recycleRecent = readRecycleRecentFromSelect();
    state.spuId = null;
    state.page = 1;
    updateFilterBar();
    syncUrl();
    load();
  });

  if (els.recycleSelect) {
    els.recycleSelect.addEventListener("change", function () {
      syncKeywordFromInput();
      state.barcode = "";
      state.recycleRecent = readRecycleRecentFromSelect();
      state.page = 1;
      updateFilterBar();
      syncUrl();
      load();
    });
  }

  if (els.filterEntryBtn) {
    els.filterEntryBtn.addEventListener("click", function () {
      openFilterDrawer();
    });
  }

  if (els.filterDrawer) {
    els.filterDrawer.addEventListener("click", function (e) {
      if (e.target && e.target.getAttribute("data-filter-close") === "1") {
        closeFilterDrawer();
        return;
      }
      var brandBtn = e.target.closest("[data-brand-id]");
      if (brandBtn && els.brandTags && els.brandTags.contains(brandBtn)) {
        var bid = Number(brandBtn.getAttribute("data-brand-id"));
        if (!isNaN(bid)) {
          toggleIdInList(filterDraft.brandIds, bid);
          renderBrandTags();
        }
        return;
      }
      var moreBtn = e.target.closest("[data-brand-more]");
      if (moreBtn && els.brandTags && els.brandTags.contains(moreBtn)) {
        filterDraft.brandExpanded = true;
        renderBrandTags();
        return;
      }
      var lessBtn = e.target.closest("[data-brand-less]");
      if (lessBtn && els.brandTags && els.brandTags.contains(lessBtn)) {
        filterDraft.brandExpanded = false;
        renderBrandTags();
        return;
      }
      var typeBtn = e.target.closest("[data-type-code]");
      if (typeBtn && els.typeTags && els.typeTags.contains(typeBtn)) {
        var codeRaw = typeBtn.getAttribute("data-type-code");
        var codeNum = parseInt(codeRaw, 10);
        var codeVal = isNaN(codeNum) ? codeRaw : codeNum;
        toggleIdInList(filterDraft.types, codeVal);
        renderTypeTags();
        return;
      }
      var nonSaleBtn = e.target.closest("[data-non-sale]");
      if (
        nonSaleBtn &&
        els.nonSaleTags &&
        els.nonSaleTags.contains(nonSaleBtn)
      ) {
        filterDraft.nonSale = parseNonSale(
          nonSaleBtn.getAttribute("data-non-sale")
        );
        if (filterDraft.nonSale === 1) {
          filterDraft.officialPriceMin = null;
          filterDraft.officialPriceMax = null;
          if (els.officialPriceMinInput) {
            els.officialPriceMinInput.value = "";
          }
          if (els.officialPriceMaxInput) {
            els.officialPriceMaxInput.value = "";
          }
        }
        renderNonSaleTags();
      }
    });
  }

  if (els.brandFilterKw) {
    els.brandFilterKw.addEventListener("input", function () {
      filterDraft.brandKw = els.brandFilterKw.value || "";
      if (!String(filterDraft.brandKw).trim()) {
        filterDraft.brandExpanded = false;
      }
      renderBrandTags();
    });
  }

  if (els.filterResetBtn) {
    els.filterResetBtn.addEventListener("click", function () {
      resetFilterDraft();
    });
  }

  if (els.filterConfirmBtn) {
    els.filterConfirmBtn.addEventListener("click", function () {
      applyFilterDraftAndReload();
    });
  }

  els.clearFilterBtn.addEventListener("click", function () {
    state.spuId = null;
    state.barcode = "";
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
    var addBtn = e.target.closest("[data-add-id]");
    if (addBtn) {
      e.preventDefault();
      e.stopPropagation();
      var addId = addBtn.getAttribute("data-add-id");
      var item = itemMap[String(addId)];
      if (!item || !window.ChaiInquiryCart) {
        return;
      }
      var already = isInCart(item.id);
      window.ChaiInquiryCart.add(item, 1);
      window.ChaiInquiryCart.toast(already ? "数量 +1" : "已加入询价单");
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
      state.barcode = "";
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

  els.galleryImg.addEventListener("touchstart", function (e) {
    if (!e.touches || !e.touches.length) {
      return;
    }
    gallery.touchX = e.touches[0].clientX;
  }, { passive: true });
  els.galleryImg.addEventListener("touchend", function (e) {
    if (gallery.touchX == null || !e.changedTouches || !e.changedTouches.length) {
      return;
    }
    var dx = e.changedTouches[0].clientX - gallery.touchX;
    gallery.touchX = null;
    if (Math.abs(dx) < 40) {
      return;
    }
    galleryStep(dx < 0 ? 1 : -1);
  }, { passive: true });

  document.addEventListener("keydown", function (e) {
    if (els.filterDrawer && !els.filterDrawer.hidden && e.key === "Escape") {
      closeFilterDrawer();
      return;
    }
    if (els.gallery.hidden) {
      return;
    }
    if (e.key === "Escape") {
      closeGallery();
    } else if (e.key === "ArrowLeft") {
      galleryStep(-1);
    } else if (e.key === "ArrowRight") {
      galleryStep(1);
    }
  });

  function closeNotice() {
    els.noticeModal.hidden = true;
    try {
      localStorage.setItem(NOTICE_KEY, todayStr());
    } catch (e) {
      /* ignore */
    }
  }

  function showNotice(copy) {
    var title = (copy.noticeTitle || "").trim();
    var body = (copy.noticeBody || "").trim();
    if (!title && !body) {
      return;
    }
    try {
      if (localStorage.getItem(NOTICE_KEY) === todayStr()) {
        return;
      }
    } catch (e) {
      /* ignore */
    }
    els.noticeTitle.textContent = title || "购买须知";
    els.noticeBody.textContent = body;
    els.noticeBody.hidden = !body;
    els.noticeModal.hidden = false;
  }

  function applyCopy(copy) {
    if (!copy) {
      return;
    }
    var normalized = {
      noticeTitle: copy.noticeTitle || copy.notice_title || "",
      noticeBody: copy.noticeBody || copy.notice_body || "",
      bannerText: copy.bannerText || copy.banner_text || ""
    };
    var banner = String(normalized.bannerText).trim();
    if (banner) {
      els.banner.textContent = banner;
      els.banner.hidden = false;
    }
    showNotice(normalized);
  }

  function loadCopy() {
    fetch("/wx/globalConfig/saleH5Copy")
      .then(function (res) {
        return res.json();
      })
      .then(function (body) {
        if (!body || body.code !== 0 || !body.data) {
          return;
        }
        applyCopy(body.data);
      })
      .catch(function () {
        /* 文案失败不影响列表 */
      });
  }

  els.noticeOk.addEventListener("click", closeNotice);
  els.noticeModal.addEventListener("click", function (e) {
    if (e.target && e.target.getAttribute("data-close") === "1") {
      closeNotice();
    }
  });

  state.keyword = qs("keyword");
  els.keyword.value = state.keyword;
  state.barcode = qs("barcode");
  var spuRaw = qs("spuId");
  state.spuId = spuRaw ? Number(spuRaw) : null;
  if (state.spuId && isNaN(state.spuId)) {
    state.spuId = null;
  }
  state.recycleRecent = qs("recycleRecent") === "1";
  if (els.recycleSelect) {
    els.recycleSelect.value = state.recycleRecent ? "1" : "";
  }
  state.brandIds = parseIdList(qs("brandIds"), false);
  state.types = parseIdList(qs("types"), true);
  state.priceMin = parsePrice(qs("priceMin"));
  state.priceMax = parsePrice(qs("priceMax"));
  if (
    state.priceMin != null &&
    state.priceMax != null &&
    state.priceMin > state.priceMax
  ) {
    var swap = state.priceMin;
    state.priceMin = state.priceMax;
    state.priceMax = swap;
  }
  state.nonSale = parseNonSale(qs("nonSale"));
  if (state.nonSale === 1) {
    state.officialPriceMin = null;
    state.officialPriceMax = null;
  } else {
    state.officialPriceMin = parsePrice(qs("officialPriceMin"));
    state.officialPriceMax = parsePrice(qs("officialPriceMax"));
    if (
      state.officialPriceMin != null &&
      state.officialPriceMax != null &&
      state.officialPriceMin > state.officialPriceMax
    ) {
      var oSwap = state.officialPriceMin;
      state.officialPriceMin = state.officialPriceMax;
      state.officialPriceMax = oSwap;
    }
  }
  if (state.barcode) {
    state.keyword = "";
    els.keyword.value = "";
    state.spuId = null;
    state.recycleRecent = false;
    state.brandIds = [];
    state.types = [];
    state.priceMin = null;
    state.priceMax = null;
    state.officialPriceMin = null;
    state.officialPriceMax = null;
    state.nonSale = null;
    if (els.recycleSelect) {
      els.recycleSelect.value = "";
    }
  }
  var pageRaw = qs("page");
  state.page = pageRaw ? Math.max(1, parseInt(pageRaw, 10) || 1) : 1;
  updateFilterBar();
  updateFilterEntryIcon();
  rememberListUrl();
  ensureFilterMeta();
  loadCopy();

  if (els.barcodeScanBtn && window.ChaiBarcodeScan) {
    els.barcodeScanBtn.innerHTML = window.ChaiBarcodeScan.iconSvg();
    els.barcodeScanBtn.addEventListener("click", function () {
      window.ChaiBarcodeScan.open({
        onResult: function (code) {
          clearBarcodeAndOthersForScan(code);
        }
      });
    });
  }

  load();
  if (window.ChaiInquiryCart) {
    window.ChaiInquiryCart.mountFab();
    window.ChaiInquiryCart.onChange(syncAddButtons);
  }
})();
