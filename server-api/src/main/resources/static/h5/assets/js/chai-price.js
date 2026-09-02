(function () {
  var pageSize = 20;
  var state = { page: 1, keyword: "", total: 0 };
  var itemImages = {};
  var gallery = { urls: [], index: 0, open: false };
  var touch = { x: 0, y: 0, active: false };

  var keywordInput = document.getElementById("keyword");
  var listEl = document.getElementById("list");
  var emptyEl = document.getElementById("empty");
  var metaEl = document.getElementById("meta");
  var pagerEl = document.getElementById("pager");
  var pageInfoEl = document.getElementById("pageInfo");
  var prevBtn = document.getElementById("prevBtn");
  var nextBtn = document.getElementById("nextBtn");
  var lightbox = document.getElementById("lightbox");
  var lbImg = document.getElementById("lbImg");
  var lbIndex = document.getElementById("lbIndex");
  var lbPrev = document.getElementById("lbPrev");
  var lbNext = document.getElementById("lbNext");
  var lbClose = document.getElementById("lbClose");
  var lbStage = document.getElementById("lbStage");
  var noticeMask = document.getElementById("noticeMask");
  var noticeOk = document.getElementById("noticeOk");

  var NOTICE_ACK_KEY = "ecotea_h5_resale_notice_ack";

  function hasNoticeAck() {
    try {
      return window.localStorage.getItem(NOTICE_ACK_KEY) === "1";
    } catch (e) {
      return false;
    }
  }

  function saveNoticeAck() {
    try {
      window.localStorage.setItem(NOTICE_ACK_KEY, "1");
    } catch (e) {
      /* ignore quota / private mode */
    }
  }

  function openNotice() {
    noticeMask.hidden = false;
    document.body.classList.add("notice-open");
  }

  function closeNotice() {
    noticeMask.hidden = true;
    document.body.classList.remove("notice-open");
  }

  function qs(name) {
    var m = new RegExp("[?&]" + name + "=([^&]*)").exec(window.location.search);
    return m ? decodeURIComponent(m[1].replace(/\+/g, " ")) : "";
  }

  function matchLabel(type) {
    if (type === "brand_exact") return "品牌精确匹配";
    if (type === "name_like") return "品名模糊匹配";
    return "全部上架";
  }

  function escapeHtml(s) {
    return String(s == null ? "" : s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function line(label, value, extraClass) {
    if (value == null || value === "") return "";
    var cls = extraClass ? ' class="row ' + extraClass + '"' : ' class="row"';
    return "<p" + cls + '><span class="lab">' + escapeHtml(label) + "</span>" + escapeHtml(value) + "</p>";
  }

  function normalizeUrls(item) {
    var urls = item.imageUrls;
    if (!urls || !urls.length) {
      if (item.coverImage) {
        return [item.coverImage];
      }
      return [];
    }
    return urls.filter(function (u) {
      return !!u;
    });
  }

  function renderItem(item) {
    var id = String(item.id);
    var urls = normalizeUrls(item);
    itemImages[id] = urls;
    var title = escapeHtml(item.title || item.name || "未命名");
    var more = urls.length > 1 ? '<span class="more">+' + (urls.length - 1) + "</span>" : "";
    var thumbInner;
    if (urls.length) {
      thumbInner =
        '<img src="' +
        escapeHtml(urls[0]) +
        '" alt="" onerror="this.style.display=\'none\';var d=document.createElement(\'div\');d.className=\'ph\';this.parentNode.appendChild(d);"/>' +
        more;
    } else {
      thumbInner = '<div class="ph" aria-hidden="true"></div>';
    }
    var thumb =
      '<button type="button" class="thumb" data-id="' +
      escapeHtml(id) +
      '"' +
      (urls.length ? "" : " disabled") +
      ">" +
      thumbInner +
      "</button>";
    var body =
      "<h2>" +
      title +
      "</h2>" +
      line("等级：", item.gradeName) +
      line("规格：", item.specShow) +
      line("生产批次：", item.prodBatchShow) +
      line("保质期：", item.expirationName) +
      line("官方：", item.officialPriceShow) +
      line("特价：", item.salePriceShow, "sale");
    return '<li class="card">' + thumb + '<div class="body">' + body + "</div></li>";
  }

  function syncGalleryUi() {
    var n = gallery.urls.length;
    if (!n) {
      return;
    }
    lbImg.src = gallery.urls[gallery.index];
    lbIndex.textContent = gallery.index + 1 + " / " + n;
    var multi = n > 1;
    lbPrev.hidden = !multi;
    lbNext.hidden = !multi;
  }

  function openGallery(id, startIndex) {
    var urls = itemImages[String(id)] || [];
    if (!urls.length) {
      return;
    }
    gallery.urls = urls;
    gallery.index = Math.max(0, Math.min(startIndex || 0, urls.length - 1));
    gallery.open = true;
    lightbox.hidden = false;
    document.body.classList.add("lb-open");
    syncGalleryUi();
  }

  function closeGallery() {
    gallery.open = false;
    lightbox.hidden = true;
    document.body.classList.remove("lb-open");
    lbImg.removeAttribute("src");
  }

  function stepGallery(delta) {
    var n = gallery.urls.length;
    if (n <= 1) {
      return;
    }
    gallery.index = (gallery.index + delta + n) % n;
    syncGalleryUi();
  }

  function load() {
    var url =
      "/chai/sku/sale/list?page=" +
      encodeURIComponent(state.page) +
      "&size=" +
      encodeURIComponent(pageSize);
    if (state.keyword) {
      url += "&keyword=" + encodeURIComponent(state.keyword);
    }
    metaEl.textContent = "加载中…";
    listEl.innerHTML = "";
    emptyEl.hidden = true;
    itemImages = {};

    fetch(url)
      .then(function (res) {
        return res.json();
      })
      .then(function (body) {
        if (!body || body.code !== 0 || !body.data) {
          metaEl.textContent = "加载失败：" + (body && body.message ? body.message : "未知错误");
          return;
        }
        var data = body.data;
        state.total = data.total || 0;
        var pages = Math.max(1, Math.ceil(state.total / pageSize));
        metaEl.textContent =
          matchLabel(data.matchType) +
          (data.keyword ? "「" + data.keyword + "」" : "") +
          " · 共 " +
          state.total +
          " 条";

        var items = data.list || [];
        if (!items.length) {
          emptyEl.hidden = false;
          pagerEl.hidden = true;
          return;
        }
        listEl.innerHTML = items.map(renderItem).join("");
        pagerEl.hidden = pages <= 1;
        pageInfoEl.textContent = state.page + " / " + pages;
        prevBtn.disabled = state.page <= 1;
        nextBtn.disabled = state.page >= pages;
      })
      .catch(function (err) {
        metaEl.textContent = "请求异常";
        console.error(err);
      });
  }

  listEl.addEventListener("click", function (e) {
    var btn = e.target.closest(".thumb");
    if (!btn || btn.disabled) {
      return;
    }
    openGallery(btn.getAttribute("data-id"), 0);
  });

  lbClose.addEventListener("click", closeGallery);
  lbPrev.addEventListener("click", function () {
    stepGallery(-1);
  });
  lbNext.addEventListener("click", function () {
    stepGallery(1);
  });
  lightbox.addEventListener("click", function (e) {
    if (e.target === lightbox) {
      closeGallery();
    }
  });

  document.addEventListener("keydown", function (e) {
    if (!gallery.open) {
      return;
    }
    if (e.key === "Escape") {
      closeGallery();
    } else if (e.key === "ArrowLeft") {
      stepGallery(-1);
    } else if (e.key === "ArrowRight") {
      stepGallery(1);
    }
  });

  lbStage.addEventListener(
    "touchstart",
    function (e) {
      if (!gallery.open || !e.touches.length) {
        return;
      }
      touch.active = true;
      touch.x = e.touches[0].clientX;
      touch.y = e.touches[0].clientY;
    },
    { passive: true }
  );

  lbStage.addEventListener(
    "touchend",
    function (e) {
      if (!touch.active || !e.changedTouches.length) {
        return;
      }
      touch.active = false;
      var dx = e.changedTouches[0].clientX - touch.x;
      var dy = e.changedTouches[0].clientY - touch.y;
      if (Math.abs(dx) < 40 || Math.abs(dx) < Math.abs(dy)) {
        return;
      }
      if (dx < 0) {
        stepGallery(1);
      } else {
        stepGallery(-1);
      }
    },
    { passive: true }
  );

  document.getElementById("searchForm").addEventListener("submit", function (e) {
    e.preventDefault();
    state.keyword = (keywordInput.value || "").trim();
    state.page = 1;
    var next = window.location.pathname;
    if (state.keyword) {
      next += "?keyword=" + encodeURIComponent(state.keyword);
    }
    window.history.replaceState(null, "", next);
    load();
  });

  prevBtn.addEventListener("click", function () {
    if (state.page > 1) {
      state.page -= 1;
      load();
    }
  });
  nextBtn.addEventListener("click", function () {
    state.page += 1;
    load();
  });

  state.keyword = qs("keyword");
  keywordInput.value = state.keyword;

  noticeOk.addEventListener("click", function () {
    saveNoticeAck();
    closeNotice();
  });

  if (!hasNoticeAck()) {
    openNotice();
  }

  load();
})();
