(function () {
  var params = new URLSearchParams(window.location.search);
  var id = params.get("id");

  var els = {
    status: document.getElementById("status"),
    detail: document.getElementById("detail"),
    brand: document.getElementById("brand"),
    skuName: document.getElementById("skuName"),
    salePrice: document.getElementById("salePrice"),
    officialPrice: document.getElementById("officialPrice"),
    discount: document.getElementById("discount"),
    stock: document.getElementById("stock"),
    cardAttrs: document.getElementById("cardAttrs"),
    grid9: document.getElementById("grid9"),
    gridEmpty: document.getElementById("gridEmpty"),
    bar: document.getElementById("bar"),
    sameBtn: document.getElementById("sameBtn"),
    copyBtn: document.getElementById("copyBtn"),
    dlBtn: document.getElementById("dlBtn"),
    lightbox: document.getElementById("lightbox"),
    lightboxImg: document.getElementById("lightboxImg"),
    capturePreview: document.getElementById("capturePreview"),
    captureImg: document.getElementById("captureImg"),
    captureHint: document.getElementById("captureHint"),
    captureSave: document.getElementById("captureSave"),
  };

  var currentItem = null;
  var lastCaptureDataUrl = null;

  function escapeHtml(s) {
    return String(s == null ? "" : s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function imageUrlsOf(item) {
    var urls = item.imageUrls || item.image_urls || [];
    if (!urls.length && (item.coverImage || item.cover_image)) {
      return [item.coverImage || item.cover_image];
    }
    return urls.slice(0, 9);
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
    return (
      "官方价 ¥" + String(item.officialPriceShow).replace(/元$/, "")
    );
  }

  function showUnavailable(msg) {
    els.detail.hidden = true;
    els.bar.hidden = true;
    els.status.hidden = false;
    els.status.innerHTML =
      escapeHtml(msg || "暂不可售") +
      '<br/><br/><a href="/h5/chai/inner-sale.html">返回内部价目</a>';
  }

  function attrRow(label, value) {
    if (!value) {
      return "";
    }
    return (
      '<li class="attr-row">' +
      '<span class="attr-label">' +
      escapeHtml(label) +
      "</span>" +
      '<span class="attr-value">' +
      escapeHtml(value) +
      "</span></li>"
    );
  }

  function renderAttrs(item) {
    var html =
      attrRow("等级", item.gradeName) +
      attrRow("规格", item.specShow) +
      attrRow("批次", item.prodBatchShow) +
      attrRow("保质期", item.expirationName);
    if (!html) {
      els.cardAttrs.hidden = true;
      els.cardAttrs.innerHTML = "";
      return;
    }
    els.cardAttrs.hidden = false;
    els.cardAttrs.innerHTML = '<ul class="attr-list">' + html + "</ul>";
  }

  function renderGrid(urls) {
    if (!urls.length) {
      els.grid9.innerHTML = "";
      els.gridEmpty.hidden = false;
      return;
    }
    els.gridEmpty.hidden = true;
    els.grid9.innerHTML = urls
      .map(function (url, i) {
        return (
          '<button type="button" class="grid9-item" data-i="' +
          i +
          '"><img src="' +
          escapeHtml(url) +
          '" alt="" loading="' +
          (i < 3 ? "eager" : "lazy") +
          '"/></button>'
        );
      })
      .join("");
  }

  function mountMp() {
    var mount = document.getElementById("detailMp");
    if (!mount || mount.querySelector("img")) {
      return;
    }
    var img = document.createElement("img");
    img.src = "/h5/assets/image/rcch_mp_x.png";
    img.alt = "关注榕城茶话";
    img.className = "site-mp-img";
    mount.appendChild(img);
  }

  function render(item) {
    currentItem = item;
    var urls = imageUrlsOf(item);
    document.title =
      (item.name || item.title || "商品详情") + " · 内部";

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

    if (els.stock && window.ChaiSaleStock) {
      els.stock.innerHTML = window.ChaiSaleStock.renderHtml(item);
    }

    renderAttrs(item);
    renderGrid(urls);
    mountMp();

    if (item.spuId && item.sameSpuSaleCount != null && item.sameSpuSaleCount > 1) {
      els.sameBtn.hidden = false;
      els.sameBtn.href =
        "/h5/chai/inner-sale.html?spuId=" + encodeURIComponent(item.spuId);
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

  els.grid9.addEventListener("click", function (e) {
    var btn = e.target.closest(".grid9-item");
    if (!btn || !currentItem) {
      return;
    }
    var urls = imageUrlsOf(currentItem);
    var i = Number(btn.getAttribute("data-i")) || 0;
    if (!urls[i]) {
      return;
    }
    els.lightboxImg.src = urls[i];
    els.lightbox.hidden = false;
  });

  els.lightbox.addEventListener("click", function (e) {
    if (e.target && e.target.getAttribute("data-lb-close") === "1") {
      els.lightbox.hidden = true;
    }
  });

  els.copyBtn.addEventListener("click", function () {
    if (!currentItem || currentItem.id == null || !window.ChaiInnerUtil) {
      return;
    }
    window.ChaiInnerUtil.copyPublicDetail(currentItem.id);
  });

  function closeCapturePreview() {
    if (els.capturePreview) {
      els.capturePreview.hidden = true;
    }
  }

  function openCapturePreview(dataUrl) {
    lastCaptureDataUrl = dataUrl;
    els.captureImg.src = dataUrl;
    var wechat =
      window.DomToImage && window.DomToImage.isWeChatBrowser
        ? window.DomToImage.isWeChatBrowser()
        : /MicroMessenger/i.test(navigator.userAgent || "");
    if (els.captureHint) {
      els.captureHint.textContent = wechat
        ? "长按下方图片可保存到相册"
        : "可长按图片保存，或点击「保存文件」下载";
    }
    if (els.captureSave) {
      els.captureSave.hidden = !!wechat;
    }
    els.capturePreview.hidden = false;
  }

  function isSameOriginUrl(url) {
    try {
      var u = new URL(url, window.location.href);
      return u.origin === window.location.origin;
    } catch (e) {
      return true;
    }
  }

  function attrExportRow(label, valueHtml) {
    if (!valueHtml) {
      return "";
    }
    return (
      '<li class="ex-attr"><span class="ex-attr-k">' +
      escapeHtml(label) +
      '</span><span class="ex-attr-v">' +
      valueHtml +
      "</span></li>"
    );
  }

  function exportYenAmount(item, which) {
    if (which === "sale") {
      if (item.salePrice != null && item.salePrice !== "") {
        var n = Number(item.salePrice);
        if (!isNaN(n)) {
          return String(n);
        }
      }
      if (item.salePriceShow) {
        var raw = String(item.salePriceShow).replace(/\(.*$/, "").trim();
        return raw.replace(/^¥\s*/, "").replace(/^￥\s*/, "") || "";
      }
      return "";
    }
    if (!item.officialPriceShow || item.officialPriceShow === "-") {
      return "";
    }
    if (item.officialPriceShow === "非卖品") {
      return "非卖品";
    }
    return String(item.officialPriceShow).replace(/元$/, "");
  }

  function buildExportTitle(item) {
    var name = item.name || item.title || "";
    var brand = item.brandName ? String(item.brandName).trim() : "";
    if (brand && name) {
      return brand + " · " + name;
    }
    return brand || name || "";
  }

  function buildExportPriceCard(item) {
    var title = buildExportTitle(item);
    var officialAmt = exportYenAmount(item, "official");
    var saleAmt = exportYenAmount(item, "sale");
    var qty = item.totalQty != null ? Number(item.totalQty) : 0;
    if (isNaN(qty) || qty < 0) {
      qty = 0;
    }

    var officialRow = "";
    if (officialAmt === "非卖品") {
      officialRow = attrExportRow("官方价", escapeHtml("非卖品"));
    } else if (officialAmt) {
      officialRow = attrExportRow("官方价", escapeHtml("¥" + officialAmt));
    }

    var saleRow = "";
    if (saleAmt) {
      var saleHtml =
        '<span class="ex-meta-sale">' +
        escapeHtml("¥" + saleAmt) +
        "</span>";
      if (item.discountShow) {
        saleHtml +=
          '<span class="ex-meta-disc">' +
          escapeHtml("（" + item.discountShow + "）") +
          "</span>";
      }
      saleRow = attrExportRow("销售价", saleHtml);
    } else {
      saleRow = attrExportRow(
        "销售价",
        '<span class="ex-meta-sale">' + escapeHtml("询价") + "</span>"
      );
    }

    var qtyText = String(qty);
    if (item.damageQty != null && Number(item.damageQty) > 0) {
      qtyText += "（破损 " + Number(item.damageQty) + "）";
    }
    var qtyRow = attrExportRow("数量", escapeHtml(qtyText));

    return (
      '<section class="ex-card">' +
      (title
        ? '<h2 class="ex-title">' + escapeHtml(title) + "</h2>"
        : "") +
      '<ul class="ex-attrs">' +
      officialRow +
      saleRow +
      qtyRow +
      "</ul></section>"
    );
  }

  /** 用业务数据拼离屏导出 DOM，不碰页面 #detail */
  function buildExportMarkup(item) {
    var urls = imageUrlsOf(item);
    var attrs =
      attrExportRow("等级", item.gradeName ? escapeHtml(item.gradeName) : "") +
      attrExportRow("规格", item.specShow ? escapeHtml(item.specShow) : "") +
      attrExportRow(
        "批次",
        item.prodBatchShow ? escapeHtml(item.prodBatchShow) : ""
      ) +
      attrExportRow(
        "保质期",
        item.expirationName ? escapeHtml(item.expirationName) : ""
      );

    var gallery;
    if (!urls.length) {
      gallery = '<p class="ex-grid-empty">暂无展示图</p>';
    } else {
      gallery =
        '<div class="ex-grid">' +
        urls
          .map(function (url) {
            return (
              '<div class="ex-grid-cell"><img src="' +
              escapeHtml(url) +
              '" alt="" loading="eager"/></div>'
            );
          })
          .join("") +
        "</div>";
    }

    return (
      buildExportPriceCard(item) +
      (attrs
        ? '<section class="ex-card"><ul class="ex-attrs">' + attrs + "</ul></section>"
        : "") +
      '<section class="ex-card"><h3 class="ex-h">商品图片</h3>' +
      gallery +
      "</section>" +
      '<section class="ex-card ex-mp"><img src="/h5/assets/image/rcch_mp_x.png" alt="关注榕城茶话" loading="eager"/></section>'
    );
  }

  function fetchUrlAsDataUrl(url, timeoutMs) {
    var ctrl =
      typeof AbortController !== "undefined" ? new AbortController() : null;
    var timer = null;
    var fetchOpts = {
      mode: "cors",
      credentials: "omit",
      // 避免沿用「无 CORS」缓存导致仍失败
      cache: "reload",
    };
    if (ctrl) {
      fetchOpts.signal = ctrl.signal;
      timer = setTimeout(function () {
        try {
          ctrl.abort();
        } catch (e) {
          /* ignore */
        }
      }, timeoutMs || 4000);
    }
    return fetch(url, fetchOpts)
      .then(function (res) {
        if (!res.ok) {
          throw new Error("图片拉取失败");
        }
        return res.blob();
      })
      .then(function (blob) {
        return new Promise(function (resolve, reject) {
          var fr = new FileReader();
          fr.onload = function () {
            resolve(fr.result);
          };
          fr.onerror = function () {
            reject(new Error("图片读取失败"));
          };
          fr.readAsDataURL(blob);
        });
      })
      .then(
        function (dataUrl) {
          if (timer) {
            clearTimeout(timer);
          }
          return dataUrl;
        },
        function (err) {
          if (timer) {
            clearTimeout(timer);
          }
          throw err;
        }
      );
  }

  /**
   * 仅处理导出节点内图片：同域跳过；跨域短超时转 dataURL。
   * 失败保留原 URL，绝不改页面 #detail 上的 img。
   */
  function embedExportImages(root) {
    var imgs = Array.prototype.slice.call(root.querySelectorAll("img"));
    var corsFail = 0;
    var tasks = imgs.map(function (img) {
      var src = img.getAttribute("src") || "";
      if (!src || src.indexOf("data:") === 0 || src.indexOf("blob:") === 0) {
        return Promise.resolve();
      }
      if (isSameOriginUrl(src)) {
        return Promise.resolve();
      }
      return fetchUrlAsDataUrl(src, 4000)
        .then(function (dataUrl) {
          img.src = dataUrl;
        })
        .catch(function () {
          corsFail += 1;
        });
    });
    return Promise.all(tasks).then(function () {
      return corsFail;
    });
  }

  function clearExportRoot(root) {
    if (!root) {
      return;
    }
    root.classList.remove("is-active");
    root.innerHTML = "";
  }

  function nextFrame() {
    return new Promise(function (resolve) {
      if (typeof requestAnimationFrame === "function") {
        requestAnimationFrame(function () {
          requestAnimationFrame(resolve);
        });
      } else {
        setTimeout(resolve, 32);
      }
    });
  }

  function runCapture() {
    var root = document.getElementById("exportCaptureRoot");
    if (!root) {
      return Promise.reject(new Error("缺少导出节点"));
    }
    if (!currentItem) {
      return Promise.reject(new Error("详情未就绪"));
    }
    root.innerHTML = buildExportMarkup(currentItem);
    root.classList.add("is-active");

    return embedExportImages(root)
      .then(function (corsFail) {
        return nextFrame().then(function () {
          return corsFail;
        });
      })
      .then(function (corsFail) {
        return window.DomToImage.captureElement(root, {
          scale: 2,
          backgroundColor: "#f5f6f4",
          timeout: 12000,
          imagesTimeoutContinue: true,
          width: 375,
        }).then(function (dataUrl) {
          return { dataUrl: dataUrl, corsFail: corsFail };
        });
      })
      .then(
        function (result) {
          clearExportRoot(root);
          return result;
        },
        function (err) {
          clearExportRoot(root);
          throw err;
        }
      );
  }

  els.dlBtn.addEventListener("click", function () {
    if (!window.DomToImage) {
      if (window.ChaiInnerUtil) {
        window.ChaiInnerUtil.toast("截图组件未加载");
      }
      return;
    }
    if (!els.detail || els.detail.hidden || !currentItem) {
      if (window.ChaiInnerUtil) {
        window.ChaiInnerUtil.toast("请等待详情加载完成");
      }
      return;
    }
    var btn = els.dlBtn;
    if (btn.disabled) {
      return;
    }
    btn.disabled = true;
    var oldText = btn.textContent;
    btn.textContent = "生成中…";
    if (window.ChaiInnerUtil) {
      window.ChaiInnerUtil.toast("正在生成图片…");
    }

    runCapture()
      .then(function (result) {
        openCapturePreview(result.dataUrl);
        if (window.ChaiInnerUtil) {
          if (result.corsFail > 0) {
            window.ChaiInnerUtil.toast(
              "已生成（部分商品图因跨域未嵌入，需图床开 CORS）"
            );
          } else {
            window.ChaiInnerUtil.toast("已生成，请长按保存");
          }
        }
      })
      .catch(function (err) {
        var msg = (err && err.message) || "生成失败";
        if (window.ChaiInnerUtil) {
          window.ChaiInnerUtil.toast(
            msg.indexOf("跨域") >= 0 || msg.indexOf("CORS") >= 0
              ? "图片跨域，无法导出（请为图床配置 CORS）"
              : "生成失败，请重试"
          );
        }
        if (typeof console !== "undefined" && console.warn) {
          console.warn("capture failed", err);
        }
      })
      .then(function () {
        btn.disabled = false;
        btn.textContent = oldText;
      });
  });

  if (els.capturePreview) {
    els.capturePreview.addEventListener("click", function (e) {
      if (e.target && e.target.getAttribute("data-cap-close") === "1") {
        closeCapturePreview();
      }
    });
  }

  if (els.captureSave) {
    els.captureSave.addEventListener("click", function () {
      if (!lastCaptureDataUrl || !window.DomToImage) {
        return;
      }
      var name =
        "sku-" +
        (currentItem && currentItem.id != null ? currentItem.id : "detail") +
        ".png";
      window.DomToImage.downloadDataUrl(lastCaptureDataUrl, name);
    });
  }

  load();
})();
