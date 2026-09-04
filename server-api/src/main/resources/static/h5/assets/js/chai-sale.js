(function () {
  var state = {
    page: 1,
    size: 20,
    keyword: "",
    spuId: null,
    total: 0
  };

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

  var NOTICE_KEY = "chai_sale_notice_dismissed";

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

    var dmg = "";
    if (item.damageQty != null && item.damageQty > 0) {
      dmg = '<span class="dmg">破损 ' + escapeHtml(item.damageQty) + "</span>";
    }

    var same = "";
    if (item.spuId && item.sameSpuSaleCount != null && item.sameSpuSaleCount > 1) {
      same =
        '<button type="button" class="same-btn" data-spu="' +
        escapeHtml(item.spuId) +
        '">看同款</button>';
    }

    return (
      '<li class="card" data-images="' +
      escapeHtml(JSON.stringify(urls)) +
      '">' +
      same +
      coverHtml +
      '<div class="body">' +
      '<p class="title">' +
      escapeHtml(item.title || item.name || "") +
      "</p>" +
      '<div class="tags">' +
      tags +
      "</div>" +
      '<div class="row-price">' +
      '<span class="sale">' +
      escapeHtml(item.salePriceShow || "询价") +
      "</span>" +
      (item.officialPriceShow
        ? '<span class="official">官方 ' + escapeHtml(item.officialPriceShow) + "</span>"
        : "") +
      "</div>" +
      '<div class="stock">库存 ' +
      escapeHtml(item.totalQty != null ? item.totalQty : 0) +
      dmg +
      "</div>" +
      "</div></li>"
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
    els.meta.textContent =
      "共 " + state.total + " 件有货" + matchHint + (state.keyword ? " · " + state.keyword : "");

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
      sessionStorage.setItem(NOTICE_KEY, "1");
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
    var dismissed = false;
    try {
      dismissed = sessionStorage.getItem(NOTICE_KEY) === "1";
    } catch (e) {
      dismissed = false;
    }
    if (dismissed) {
      return;
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
})();
