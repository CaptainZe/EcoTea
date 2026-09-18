/**
 * 内部回收报价单（本地 localStorage，与询价单隔离）。
 * 已加入不可再加；可改 quotePrice / qty；参考 recyclePrice 只读。
 */
(function (global) {
  var KEY = "chai_recycle_quote";
  var QUOTE_URL = "/h5/chai/inner-recycle-quote.html";
  var FAB_ID = "recycleQuoteFab";
  var listeners = [];

  function emptyState() {
    return { items: [] };
  }

  function read() {
    try {
      var raw = global.localStorage.getItem(KEY);
      if (!raw) {
        return emptyState();
      }
      var data = JSON.parse(raw);
      if (!data || !Array.isArray(data.items)) {
        return emptyState();
      }
      return data;
    } catch (e) {
      return emptyState();
    }
  }

  function write(data) {
    global.localStorage.setItem(KEY, JSON.stringify(data));
    notify();
  }

  function notify() {
    var n = kindCount();
    listeners.forEach(function (fn) {
      try {
        fn(n);
      } catch (e) {
        /* ignore */
      }
    });
  }

  function snapshotOf(item) {
    var cover = item.coverImage || item.cover_image || null;
    var urls = item.imageUrls || item.image_urls;
    if (!cover && urls && urls.length) {
      cover = urls[0];
    }
    var recyclePrice =
      item.recyclePrice != null
        ? item.recyclePrice
        : item.recycle_price != null
          ? item.recycle_price
          : null;
    return {
      id: item.id,
      skuCode: item.skuCode || item.sku_code || "",
      name: item.name || "",
      brandName: item.brandName || item.brand_name || "",
      title: item.title || "",
      recyclePrice: recyclePrice,
      recyclePriceShow:
        item.recyclePriceShow || item.recycle_price_show || "",
      officialPriceShow:
        item.officialPriceShow || item.official_price_show || "",
      recycleDiscountShow:
        item.recycleDiscountShow || item.recycle_discount_show || "",
      recyclePriceReducePer:
        item.recyclePriceReducePer != null
          ? item.recyclePriceReducePer
          : item.recycle_price_reduce_per != null
            ? item.recycle_price_reduce_per
            : null,
      recycleDamagePrice:
        item.recycleDamagePrice != null
          ? item.recycleDamagePrice
          : item.recycle_damage_price != null
            ? item.recycle_damage_price
            : null,
      recycleDamagePriceShow:
        item.recycleDamagePriceShow ||
        item.recycle_damage_price_show ||
        "",
      recyclePriceReduceNoBag:
        item.recyclePriceReduceNoBag != null
          ? item.recyclePriceReduceNoBag
          : item.recycle_price_reduce_no_bag != null
            ? item.recycle_price_reduce_no_bag
            : null,
      recyclePriceReduceNoBagShow:
        item.recyclePriceReduceNoBagShow ||
        item.recycle_price_reduce_no_bag_show ||
        "",
      gradeName: item.gradeName || item.grade_name || "",
      specShow: item.specShow || item.spec_show || "",
      prodBatchShow: item.prodBatchShow || item.prod_batch_show || "",
      expirationName: item.expirationName || item.expiration_name || "",
      coverImage: cover,
      spuId:
        item.spuId != null
          ? item.spuId
          : item.spu_id != null
            ? item.spu_id
            : null,
      totalQty:
        item.totalQty != null
          ? item.totalQty
          : item.total_qty != null
            ? item.total_qty
            : null,
    };
  }

  function kindCount() {
    return read().items.length;
  }

  function getItems() {
    return read().items.slice();
  }

  function findIndex(items, id) {
    for (var i = 0; i < items.length; i++) {
      if (String(items[i].id) === String(id)) {
        return i;
      }
    }
    return -1;
  }

  function has(id) {
    if (id == null) {
      return false;
    }
    return findIndex(read().items, id) >= 0;
  }

  function toNum(v) {
    if (v == null || v === "") {
      return null;
    }
    var n = Number(v);
    return isNaN(n) ? null : n;
  }

  /**
   * 加入报价单。已存在则不重复加入（不改数量），返回 false。
   */
  function add(item) {
    if (!item || item.id == null) {
      return false;
    }
    var data = read();
    if (findIndex(data.items, item.id) >= 0) {
      return false;
    }
    var snap = snapshotOf(item);
    var row = snap;
    row.qty = 1;
    row.quotePrice = toNum(snap.recyclePrice);
    row.updatedAt = Date.now();
    data.items.push(row);
    write(data);
    return true;
  }

  function setQty(id, qty) {
    var q = Number(qty);
    if (!q || isNaN(q) || q < 1) {
      remove(id);
      return;
    }
    var data = read();
    var idx = findIndex(data.items, id);
    if (idx < 0) {
      return;
    }
    data.items[idx].qty = Math.floor(q);
    data.items[idx].updatedAt = Date.now();
    write(data);
  }

  function setQuotePrice(id, price) {
    var data = read();
    var idx = findIndex(data.items, id);
    if (idx < 0) {
      return;
    }
    var n = toNum(price);
    if (n == null || n < 0) {
      n = toNum(data.items[idx].recyclePrice);
    }
    data.items[idx].quotePrice = n;
    data.items[idx].updatedAt = Date.now();
    write(data);
  }

  function remove(id) {
    var data = read();
    data.items = data.items.filter(function (it) {
      return String(it.id) !== String(id);
    });
    write(data);
  }

  function clear() {
    write(emptyState());
  }

  function lineAmount(it) {
    var price = toNum(it.quotePrice);
    if (price == null) {
      price = toNum(it.recyclePrice);
    }
    if (price == null) {
      return 0;
    }
    var qty = Number(it.qty) || 1;
    return price * qty;
  }

  function totalAmount() {
    var sum = 0;
    getItems().forEach(function (it) {
      sum += lineAmount(it);
    });
    return Math.round(sum * 100) / 100;
  }

  function plainAmount(n) {
    if (n == null || n === "" || isNaN(Number(n))) {
      return "";
    }
    var s = String(Number(n));
    if (s.indexOf(".") >= 0) {
      s = s.replace(/\.?0+$/, "");
    }
    return s;
  }

  function formatOfficialLine(it) {
    var show = it.officialPriceShow || it.official_price_show || "";
    if (!show || show === "-") {
      return "";
    }
    return String(show).trim();
  }

  function formatQuotePriceLine(it) {
    var price = toNum(it.quotePrice);
    if (price == null) {
      price = toNum(it.recyclePrice);
    }
    if (price == null) {
      return "";
    }
    return plainAmount(price) + "元";
  }

  /** 破损价(压价%) · 无提袋扣 ¥yy */
  function formatRecycleExtra(it) {
    var parts = [];
    var dmgShow =
      it.recycleDamagePriceShow || it.recycle_damage_price_show || "";
    if (dmgShow) {
      var dmg = String(dmgShow);
      var per =
        it.recyclePriceReducePer != null
          ? it.recyclePriceReducePer
          : it.recycle_price_reduce_per;
      if (per != null && per !== "") {
        dmg += "(" + per + "%)";
      }
      parts.push("破损 " + dmg);
    }
    var noBag =
      it.recyclePriceReduceNoBagShow ||
      it.recycle_price_reduce_no_bag_show ||
      "";
    if (noBag != null && String(noBag).trim() !== "") {
      parts.push("无提袋扣 ¥" + String(noBag).trim());
    }
    return parts.join(" · ");
  }

  function displayTitle(it) {
    return (
      it.title ||
      [it.brandName, it.name].filter(Boolean).join(" · ") ||
      "商品"
    );
  }

  function buildQuoteText() {
    var items = getItems();
    if (!items.length) {
      return "";
    }
    var blocks = ["【报价单】"];
    items.forEach(function (it) {
      var lines = [displayTitle(it)];
      var spec = it.specShow || it.spec_show;
      var batch = it.prodBatchShow || it.prod_batch_show;
      var official = formatOfficialLine(it);
      var recycle = formatQuotePriceLine(it);
      var qty = it.qty || 1;
      if (spec) {
        lines.push("规格：" + spec);
      }
      if (batch) {
        lines.push("批次：" + batch);
      }
      if (official) {
        lines.push("官方：" + official);
      }
      if (recycle) {
        lines.push("回收价：" + recycle);
      }
      lines.push("数量：" + qty);
      blocks.push(lines.join("\n"));
    });
    blocks.push("合计：" + "¥" + plainAmount(totalAmount()));
    return blocks.join("\n\n");
  }

  function onChange(fn) {
    if (typeof fn === "function") {
      listeners.push(fn);
    }
  }

  function toast(msg) {
    if (global.ChaiInnerUtil && global.ChaiInnerUtil.toast) {
      global.ChaiInnerUtil.toast(msg);
      return;
    }
    var el = document.querySelector(".recycle-quote-toast");
    if (!el) {
      el = document.createElement("div");
      el.className = "recycle-quote-toast";
      document.body.appendChild(el);
    }
    el.textContent = msg;
    el.classList.add("show");
    clearTimeout(el._timer);
    el._timer = setTimeout(function () {
      el.classList.remove("show");
    }, 1800);
  }

  function paintBadge(root) {
    if (!root) {
      return;
    }
    var badge = root.querySelector(".recycle-quote-fab-badge");
    if (!badge) {
      return;
    }
    var n = kindCount();
    if (n > 0) {
      badge.hidden = false;
      badge.textContent = n > 99 ? "99+" : String(n);
    } else {
      badge.hidden = true;
      badge.textContent = "";
    }
  }

  function mountFab(options) {
    options = options || {};
    var existing = document.getElementById(FAB_ID);
    if (existing) {
      paintBadge(existing);
      return existing;
    }
    var a = document.createElement("a");
    a.id = FAB_ID;
    a.className = "recycle-quote-fab";
    a.href = QUOTE_URL;
    a.setAttribute("aria-label", "报价单");
    a.innerHTML =
      '<img class="recycle-quote-fab-icon" src="/h5/assets/image/quotation.png" alt=""/>' +
      '<span class="recycle-quote-fab-badge" hidden></span>';
    document.body.appendChild(a);
    paintBadge(a);
    onChange(function () {
      paintBadge(a);
      if (options.hiddenWhenEmpty) {
        a.hidden = kindCount() === 0;
      }
    });
    if (options.hiddenWhenEmpty) {
      a.hidden = kindCount() === 0;
    }
    return a;
  }

  global.ChaiRecycleQuote = {
    KEY: KEY,
    QUOTE_URL: QUOTE_URL,
    kindCount: kindCount,
    getItems: getItems,
    has: has,
    add: add,
    setQty: setQty,
    setQuotePrice: setQuotePrice,
    remove: remove,
    clear: clear,
    lineAmount: lineAmount,
    totalAmount: totalAmount,
    buildQuoteText: buildQuoteText,
    displayTitle: displayTitle,
    formatQuotePriceLine: formatQuotePriceLine,
    formatOfficialLine: formatOfficialLine,
    formatRecycleExtra: formatRecycleExtra,
    plainAmount: plainAmount,
    onChange: onChange,
    toast: toast,
    mountFab: mountFab,
    paintBadge: paintBadge,
    read: read,
    write: write,
  };
})(window);
