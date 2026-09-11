(function () {
  var state = {
    page: 1,
    size: 20,
    keyword: "",
    spuId: null,
    total: 0
  };

  var itemMap = {};

  var gallery = {
    urls: [],
    index: 0,
    touchX: null
  };

  var els = {
    form: document.getElementById("searchForm"),
    keyword: document.getElementById("keyword"),
    meta: document.getElementById("meta"),
    list: document.getElementById("list"),
    empty: document.getElementById("empty"),
    pager: document.getElementById("pager"),
    pageInfo: document.getElementById("pageInfo"),
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
    galleryNext: document.getElementById("galleryNext")
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
    var next = window.location.pathname + (q ? "?" + q : "");
    window.history.replaceState(null, "", next);
  }

  function updateFilterBar() {
    var inSameSpu = !!state.spuId;
    els.filterBar.hidden = !inSameSpu;
    els.clearFilterBtn.hidden = !inSameSpu;
    els.filterText.textContent = inSameSpu ? "正在查看同款现货" : "";
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
    }
    var kwHint = state.keyword ? " · " + escapeHtml(state.keyword) : "";
    els.meta.innerHTML =
      "共 <span class=\"meta-num\">" +
      escapeHtml(state.total) +
      "</span> 款现货" +
      escapeHtml(matchHint) +
      kwHint;

    els.pager.hidden = state.total <= state.size;
    els.pageInfo.textContent = state.page + " / " + pages;
    els.prevBtn.disabled = state.page <= 1;
    els.nextBtn.disabled = state.page >= pages;
  }

  function load() {
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
    fetch("/chai/sku/sale/list?" + params.toString())
      .then(function (res) {
        return res.json();
      })
      .then(function (body) {
        if (!body || body.code !== 0 || !body.data) {
          els.meta.textContent = "加载失败";
          els.list.innerHTML = "";
          els.empty.hidden = false;
          return;
        }
        render(body.data);
      })
      .catch(function () {
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
    document.body.style.overflow = "";
    gallery.urls = [];
    gallery.index = 0;
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
  var spuRaw = qs("spuId");
  state.spuId = spuRaw ? Number(spuRaw) : null;
  if (state.spuId && isNaN(state.spuId)) {
    state.spuId = null;
  }
  var pageRaw = qs("page");
  state.page = pageRaw ? Math.max(1, parseInt(pageRaw, 10) || 1) : 1;
  updateFilterBar();
  loadCopy();
  load();
  if (window.ChaiInquiryCart) {
    window.ChaiInquiryCart.mountFab();
    window.ChaiInquiryCart.onChange(syncAddButtons);
  }
})();
