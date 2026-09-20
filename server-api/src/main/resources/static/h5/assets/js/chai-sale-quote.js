/**
 * 内部销售报价单（本地 localStorage，与询价单/回收报价隔离）。
 * 已加入不可再加；可改 quotePrice / qty；数量不受库存限制。
 */
(function (global) {
  var KEY = "chai_sale_quote";
  var QUOTE_URL = "/h5/chai/inner-sale-quote.html";
  var FAB_ID = "saleQuoteFab";
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

  function toNum(v) {
    if (v == null || v === "") {
      return null;
    }
    var n = Number(v);
    return isNaN(n) ? null : n;
  }

  function snapshotOf(item) {
    var cover = item.coverImage || item.cover_image || null;
    var urls = item.imageUrls || item.image_urls;
    if (!cover && urls && urls.length) {
      cover = urls[0];
    }
    var salePrice =
      item.salePrice != null
        ? item.salePrice
        : item.sale_price != null
          ? item.sale_price
          : null;
    var officialPrice =
      item.officialPrice != null
        ? item.officialPrice
        : item.official_price != null
          ? item.official_price
          : null;
    return {
      id: item.id,
      skuCode: item.skuCode || item.sku_code || "",
      name: item.name || "",
      brandName: item.brandName || item.brand_name || "",
      title: item.title || "",
      salePrice: salePrice,
      salePriceShow: item.salePriceShow || item.sale_price_show || "",
      officialPrice: officialPrice,
      officialPriceShow:
        item.officialPriceShow || item.official_price_show || "",
      discountShow: item.discountShow || item.discount_show || "",
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
    row.quotePrice = toNum(snap.salePrice);
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
      n = toNum(data.items[idx].salePrice);
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
      price = toNum(it.salePrice);
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

  function displayTitle(it) {
    return (
      it.title ||
      [it.brandName, it.name].filter(Boolean).join(" · ") ||
      "商品"
    );
  }

  function formatOfficialLine(it) {
    var show = it.officialPriceShow || it.official_price_show || "";
    if (!show || show === "-") {
      return "";
    }
    return String(show).trim();
  }

  function discountParen(quotePrice, officialPrice) {
    var q = toNum(quotePrice);
    var o = toNum(officialPrice);
    if (q == null || o == null || o <= 0) {
      return "";
    }
    var d = (q * 10) / o;
    var s = (Math.round(d * 100) / 100).toFixed(2).replace(/\.?0+$/, "");
    return "（" + s + "折）";
  }

  /** 特价：2988元（3.4折） */
  function formatQuotePriceLine(it) {
    var price = toNum(it.quotePrice);
    if (price == null) {
      price = toNum(it.salePrice);
    }
    if (price == null) {
      return "";
    }
    var line = plainAmount(price) + "元";
    var disc = discountParen(price, it.officialPrice);
    if (!disc && it.discountShow && price === toNum(it.salePrice)) {
      var ds = String(it.discountShow).trim();
      if (ds) {
        if (ds.indexOf("折") >= 0) {
          disc = "（" + ds.replace(/[（）()]/g, "") + "）";
        } else {
          disc = "（" + ds + "）";
        }
      }
    }
    return line + disc;
  }

  function buildQuoteText() {
    var items = getItems();
    if (!items.length) {
      return "";
    }
    var blocks = ["【销售报价单】"];
    var totalPcs = 0;
    items.forEach(function (it) {
      var lines = [displayTitle(it)];
      var grade = it.gradeName || it.grade_name;
      var spec = it.specShow || it.spec_show;
      var batch = it.prodBatchShow || it.prod_batch_show;
      var exp = it.expirationName || it.expiration_name;
      var official = formatOfficialLine(it);
      var sale = formatQuotePriceLine(it);
      var qty = it.qty || 1;
      totalPcs += qty;
      if (grade) {
        lines.push("等级：" + grade);
      }
      if (spec) {
        lines.push("规格：" + spec);
      }
      if (batch) {
        lines.push("批次：" + batch);
      }
      if (exp) {
        lines.push("保质期：" + exp);
      }
      if (official) {
        lines.push("官方：" + official);
      }
      if (sale) {
        lines.push("特价：" + sale);
      }
      lines.push("数量：" + qty);
      blocks.push(lines.join("\n"));
    });
    blocks.push(
      "------------------------------\n" +
        "共 " +
        items.length +
        " 种，合计 " +
        totalPcs +
        " 件\n" +
        "金额合计：¥" +
        plainAmount(totalAmount())
    );
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
    var el = document.querySelector(".sale-quote-toast");
    if (!el) {
      el = document.createElement("div");
      el.className = "sale-quote-toast";
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
    var badge = root.querySelector(".sale-quote-fab-badge");
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
    a.className = "sale-quote-fab";
    a.href = QUOTE_URL;
    a.setAttribute("aria-label", "销售报价单");
    a.innerHTML =
      '<img class="sale-quote-fab-icon" src="/h5/assets/image/quotation_sale.png" alt=""/>' +
      '<span class="sale-quote-fab-badge" hidden></span>';
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

  global.ChaiSaleQuote = {
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
    plainAmount: plainAmount,
    onChange: onChange,
    toast: toast,
    mountFab: mountFab,
    paintBadge: paintBadge,
    read: read,
    write: write,
  };
})(window);
