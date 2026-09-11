/**
 * 询价单（本地 localStorage，无服务端）。
 * 角标 = SKU 种类数；数量不限制库存。
 */
(function (global) {
  var KEY = "chai_inquiry_cart";
  var CART_URL = "/h5/chai/sale-cart.html";
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
    var code = item.skuCode || item.sku_code || "";
    return {
      id: item.id,
      skuCode: code,
      name: item.name || "",
      brandName: item.brandName || item.brand_name || "",
      title: item.title || "",
      salePrice: item.salePrice != null ? item.salePrice : item.sale_price != null ? item.sale_price : null,
      salePriceShow: item.salePriceShow || item.sale_price_show || "",
      officialPriceShow: item.officialPriceShow || item.official_price_show || "",
      discountShow: item.discountShow || item.discount_show || "",
      gradeName: item.gradeName || item.grade_name || "",
      specShow: item.specShow || item.spec_show || "",
      prodBatchShow: item.prodBatchShow || item.prod_batch_show || "",
      expirationName: item.expirationName || item.expiration_name || "",
      coverImage: cover,
      spuId: item.spuId != null ? item.spuId : item.spu_id != null ? item.spu_id : null,
      totalQty: item.totalQty != null ? item.totalQty : item.total_qty != null ? item.total_qty : null,
      damageQty: item.damageQty != null ? item.damageQty : item.damage_qty != null ? item.damage_qty : null,
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

  function add(item, delta) {
    if (!item || item.id == null) {
      return false;
    }
    var d = delta == null ? 1 : Number(delta);
    if (!d || isNaN(d)) {
      d = 1;
    }
    var data = read();
    var idx = findIndex(data.items, item.id);
    if (idx >= 0) {
      data.items[idx].qty = Math.max(1, (data.items[idx].qty || 1) + d);
      var snap = snapshotOf(item);
      Object.keys(snap).forEach(function (k) {
        if (k === "id") {
          return;
        }
        if (snap[k] != null && snap[k] !== "") {
          data.items[idx][k] = snap[k];
        }
      });
      data.items[idx].updatedAt = Date.now();
    } else {
      var row = snapshotOf(item);
      row.qty = Math.max(1, d);
      row.updatedAt = Date.now();
      data.items.push(row);
    }
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

  /**
   * 用最新商品信息补全快照（不改数量）；用于旧询价单补 skuCode 等。
   */
  function mergeSnapshot(id, item) {
    if (id == null || !item) {
      return;
    }
    var data = read();
    var idx = findIndex(data.items, id);
    if (idx < 0) {
      return;
    }
    var snap = snapshotOf(item);
    Object.keys(snap).forEach(function (k) {
      if (k === "id") {
        return;
      }
      if (snap[k] != null && snap[k] !== "") {
        data.items[idx][k] = snap[k];
      }
    });
    data.items[idx].updatedAt = Date.now();
    write(data);
  }

  /**
   * 对缺少 skuCode 的条目拉取详情并回填。callback(err) 可选。
   */
  function enrichMissingSkuCodes(callback) {
    var data = read();
    var need = data.items.filter(function (it) {
      return it.id != null && !(it.skuCode || it.sku_code);
    });
    if (!need.length) {
      if (callback) {
        callback(null);
      }
      return;
    }
    var left = need.length;
    var failed = false;
    var latest = read();
    need.forEach(function (it) {
      fetch("/chai/sku/sale/detail?id=" + encodeURIComponent(it.id))
        .then(function (res) {
          return res.json();
        })
        .then(function (body) {
          if (!(body && body.code === 0 && body.data)) {
            failed = true;
            return;
          }
          var idx = findIndex(latest.items, it.id);
          if (idx < 0) {
            return;
          }
          var snap = snapshotOf(body.data);
          Object.keys(snap).forEach(function (k) {
            if (k === "id") {
              return;
            }
            if (snap[k] != null && snap[k] !== "") {
              latest.items[idx][k] = snap[k];
            }
          });
          latest.items[idx].updatedAt = Date.now();
        })
        .catch(function () {
          failed = true;
        })
        .then(function () {
          left -= 1;
          if (left > 0) {
            return;
          }
          write(latest);
          if (callback) {
            callback(failed ? new Error("enrich failed") : null);
          }
        });
    });
  }

  function onChange(fn) {
    if (typeof fn === "function") {
      listeners.push(fn);
    }
  }

  function formatPriceShow(show) {
    if (!show) {
      return "";
    }
    return String(show)
      .trim()
      .replace(/\(/g, "（")
      .replace(/\)/g, "）");
  }

  function buildInquiryText() {
    var items = getItems();
    if (!items.length) {
      return "";
    }
    var blocks = ["【询价单】"];
    var totalPcs = 0;
    items.forEach(function (it) {
      var qty = it.qty || 1;
      totalPcs += qty;
      var name =
        it.title ||
        [it.brandName, it.name].filter(Boolean).join(" · ") ||
        "商品";
      var lines = [name];
      var grade = it.gradeName || it.grade_name;
      var spec = it.specShow || it.spec_show;
      var batch = it.prodBatchShow || it.prod_batch_show;
      var exp = it.expirationName || it.expiration_name;
      var official = formatPriceShow(
        it.officialPriceShow || it.official_price_show
      );
      var sale = formatPriceShow(it.salePriceShow || it.sale_price_show);
      if (!sale && it.salePrice != null) {
        sale = it.salePrice + "元";
      }
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
      if (official && official !== "-") {
        lines.push("官方：" + official);
      }
      if (sale && sale !== "-") {
        lines.push("售价：" + sale);
      }
      lines.push("购买数量：" + qty);
      blocks.push(lines.join("\n"));
    });
    blocks.push("共 " + items.length + " 种，合计 " + totalPcs + " 件");
    return blocks.join("\n\n");
  }

  function copyText(text) {
    if (!text) {
      return Promise.reject(new Error("empty"));
    }
    if (global.navigator && global.navigator.clipboard && global.navigator.clipboard.writeText) {
      return global.navigator.clipboard.writeText(text).catch(function () {
        return fallbackCopy(text);
      });
    }
    return fallbackCopy(text);
  }

  function fallbackCopy(text) {
    return new Promise(function (resolve, reject) {
      var ta = document.createElement("textarea");
      ta.value = text;
      ta.setAttribute("readonly", "");
      ta.style.position = "fixed";
      ta.style.left = "-9999px";
      document.body.appendChild(ta);
      ta.select();
      try {
        var ok = document.execCommand("copy");
        document.body.removeChild(ta);
        if (ok) {
          resolve();
        } else {
          reject(new Error("copy failed"));
        }
      } catch (e) {
        document.body.removeChild(ta);
        reject(e);
      }
    });
  }

  function toast(msg) {
    var el = document.querySelector(".inquiry-toast");
    if (!el) {
      el = document.createElement("div");
      el.className = "inquiry-toast";
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
    var badge = root.querySelector(".inquiry-fab-badge");
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

  /**
   * 在页面挂载右下角询价单入口。options.hiddenWhenEmpty 默认 false。
   */
  function mountFab(options) {
    options = options || {};
    if (document.getElementById("inquiryFab")) {
      paintBadge(document.getElementById("inquiryFab"));
      return document.getElementById("inquiryFab");
    }
    var a = document.createElement("a");
    a.id = "inquiryFab";
    a.className = "inquiry-fab";
    a.href = CART_URL;
    a.setAttribute("aria-label", "询价单");
    a.innerHTML =
      '<img class="inquiry-fab-icon" src="/h5/assets/image/cart.png" alt=""/>' +
      '<span class="inquiry-fab-badge" hidden></span>';
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

  global.ChaiInquiryCart = {
    KEY: KEY,
    CART_URL: CART_URL,
    getItems: getItems,
    kindCount: kindCount,
    add: add,
    setQty: setQty,
    remove: remove,
    clear: clear,
    onChange: onChange,
    buildInquiryText: buildInquiryText,
    copyText: copyText,
    toast: toast,
    mountFab: mountFab,
    paintBadge: paintBadge,
    mergeSnapshot: mergeSnapshot,
    enrichMissingSkuCodes: enrichMissingSkuCodes,
  };
})(window);
