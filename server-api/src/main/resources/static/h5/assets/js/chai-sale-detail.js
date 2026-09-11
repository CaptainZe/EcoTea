(function () {
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
    bar: document.getElementById("bar"),
    sameBtn: document.getElementById("sameBtn"),
    addCartBtn: document.getElementById("addCartBtn"),
    gallery: document.getElementById("gallery"),
    galleryImg: document.getElementById("galleryImg"),
    galleryIndex: document.getElementById("galleryIndex"),
    galleryPrev: document.getElementById("galleryPrev"),
    galleryNext: document.getElementById("galleryNext"),
  };

  var state = { urls: [], index: 0, galleryOpen: false };
  var currentItem = null;
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

  function escapeHtml(s) {
    return String(s == null ? "" : s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function imageUrlsOf(item) {
    if (item.imageUrls && item.imageUrls.length) {
      return item.imageUrls.slice();
    }
    if (item.coverImage) {
      return [item.coverImage];
    }
    return [];
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
    return "官方价 ¥" + String(item.officialPriceShow).replace(/元$/, "");
  }

  function showUnavailable(msg) {
    els.detail.hidden = true;
    els.bar.hidden = true;
    els.status.hidden = false;
    els.status.innerHTML =
      escapeHtml(msg || "暂不可售") +
      '<br/><br/><a href="/h5/chai/sale.html">返回价格目录</a>';
  }

  function paintCarouselIndex() {
    if (state.urls.length <= 1) {
      els.carouselIndex.hidden = true;
      return;
    }
    els.carouselIndex.hidden = false;
    els.carouselIndex.textContent =
      state.index + 1 + " / " + state.urls.length;
  }

  function syncCarouselScroll() {
    var slides = els.carouselTrack.querySelectorAll(".carousel-slide");
    if (!slides.length || !slides[state.index]) {
      return;
    }
    els.carouselTrack.scrollTo({
      left: slides[state.index].offsetLeft,
      behavior: "auto",
    });
  }

  function setIndex(next, scroll) {
    if (!state.urls.length) {
      return;
    }
    state.index =
      ((next % state.urls.length) + state.urls.length) % state.urls.length;
    paintCarouselIndex();
    if (scroll) {
      syncCarouselScroll();
    }
    if (state.galleryOpen) {
      paintGallery();
    }
  }

  function paintGallery() {
    if (!state.urls.length) {
      return;
    }
    els.galleryImg.src = state.urls[state.index];
    els.galleryIndex.textContent =
      state.index + 1 + " / " + state.urls.length;
    var multi = state.urls.length > 1;
    els.galleryPrev.hidden = !multi;
    els.galleryNext.hidden = !multi;
  }

  function openGallery() {
    if (!state.urls.length) {
      return;
    }
    state.galleryOpen = true;
    els.gallery.hidden = false;
    paintGallery();
  }

  function closeGallery() {
    state.galleryOpen = false;
    els.gallery.hidden = true;
    syncCarouselScroll();
    paintCarouselIndex();
  }

  function galleryStep(delta) {
    setIndex(state.index + delta, true);
  }

  function onCarouselScroll() {
    var w = els.carouselTrack.clientWidth || 1;
    var idx = Math.round(els.carouselTrack.scrollLeft / w);
    if (idx < 0) {
      idx = 0;
    }
    if (idx >= state.urls.length) {
      idx = state.urls.length - 1;
    }
    if (idx !== state.index) {
      state.index = idx;
      paintCarouselIndex();
    }
  }

  function renderCarousel(urls) {
    state.urls = urls;
    state.index = 0;
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
            state.index = i;
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

  function render(item) {
    currentItem = item;
    var urls = imageUrlsOf(item);
    document.title = (item.name || item.title || "商品详情") + " · 在售";

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

    var dmg = "";
    if (item.damageQty != null && item.damageQty > 0) {
      dmg = '<span class="dmg">破损 ' + escapeHtml(item.damageQty) + "</span>";
    }
    els.stock.innerHTML =
      "现货库存 " +
      escapeHtml(item.totalQty != null ? item.totalQty : 0) +
      dmg;

    renderAttrs(item);

    if (item.spuId && item.sameSpuSaleCount != null && item.sameSpuSaleCount > 1) {
      els.sameBtn.hidden = false;
      els.sameBtn.href =
        "/h5/chai/sale.html?spuId=" + encodeURIComponent(item.spuId);
      els.bar.classList.remove("same-hidden");
    } else {
      els.sameBtn.hidden = true;
      els.sameBtn.removeAttribute("href");
      els.bar.classList.add("same-hidden");
    }

    els.status.hidden = true;
    els.detail.hidden = false;
    els.bar.hidden = false;
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

  function mountDetailMp() {
    var mount = document.getElementById("detailMp");
    if (!mount || mount.querySelector("img")) {
      return;
    }
    var img = document.createElement("img");
    img.src = "/h5/assets/image/rcch_mp_x2.png";
    img.alt = "关注榕城茶话";
    img.className = "site-mp-img";
    mount.appendChild(img);
  }

  els.carouselTrack.addEventListener("scroll", function () {
    window.requestAnimationFrame(onCarouselScroll);
  });

  els.carouselTrack.addEventListener(
    "touchstart",
    function (e) {
      if (!e.touches || !e.touches.length) {
        return;
      }
      touchX = e.touches[0].clientX;
      suppressClick = false;
    },
    { passive: true }
  );
  els.carouselTrack.addEventListener(
    "touchmove",
    function (e) {
      if (touchX == null || !e.touches || !e.touches.length) {
        return;
      }
      if (Math.abs(e.touches[0].clientX - touchX) > 12) {
        suppressClick = true;
      }
    },
    { passive: true }
  );
  els.carouselTrack.addEventListener("touchend", function () {
    touchX = null;
    window.setTimeout(function () {
      suppressClick = false;
    }, 50);
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
    galleryStep(dx < 0 ? 1 : -1);
  });

  load();
  mountDetailMp();

  if (els.addCartBtn) {
    els.addCartBtn.addEventListener("click", function () {
      if (!currentItem || !window.ChaiInquiryCart) {
        return;
      }
      window.ChaiInquiryCart.add(currentItem, 1);
      window.ChaiInquiryCart.toast("已加入询价单");
    });
  }

  if (window.ChaiInquiryCart) {
    window.ChaiInquiryCart.mountFab();
  }
})();
