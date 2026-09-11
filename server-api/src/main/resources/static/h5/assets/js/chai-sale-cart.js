(function () {
  var els = {
    meta: document.getElementById("meta"),
    list: document.getElementById("list"),
    empty: document.getElementById("empty"),
    cartBar: document.getElementById("cartBar"),
    copyBtn: document.getElementById("copyBtn"),
    clearBtn: document.getElementById("clearBtn"),
    cartToolbar: document.getElementById("cartToolbar"),
    clearModal: document.getElementById("clearModal"),
    clearOk: document.getElementById("clearOk"),
  };

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
      '<span class="qty-val">' +
      escapeHtml(item.qty || 1) +
      "</span>" +
      '<button type="button" class="qty-btn" data-act="inc" aria-label="增加">+</button>' +
      '<button type="button" class="qty-remove" data-act="remove">移除</button>' +
      "</div>" +
      "</div></li>"
    );
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
    els.list.innerHTML = items.map(renderItem).join("");
  }

  els.list.addEventListener("click", function (e) {
    var btn = e.target.closest("[data-act]");
    if (!btn || !window.ChaiInquiryCart) {
      return;
    }
    var card = btn.closest(".card");
    if (!card) {
      return;
    }
    var id = card.getAttribute("data-id");
    var act = btn.getAttribute("data-act");
    var items = window.ChaiInquiryCart.getItems();
    var row = null;
    for (var i = 0; i < items.length; i++) {
      if (String(items[i].id) === String(id)) {
        row = items[i];
        break;
      }
    }
    if (!row) {
      return;
    }
    if (act === "inc") {
      window.ChaiInquiryCart.setQty(id, (row.qty || 1) + 1);
    } else if (act === "dec") {
      window.ChaiInquiryCart.setQty(id, (row.qty || 1) - 1);
    } else if (act === "remove") {
      window.ChaiInquiryCart.remove(id);
    }
  });

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
          cart.toast("询价单已复制");
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
