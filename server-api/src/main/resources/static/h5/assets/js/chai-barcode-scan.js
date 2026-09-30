/**
 * H5 条码扫码层：摄像头 + ZXing；失败可手输。
 * 依赖：先加载 @zxing/library UMD（全局 ZXing）。
 *
 * 用法：
 *   ChaiBarcodeScan.open({ onResult: function (code) { ... } });
 *   ChaiBarcodeScan.iconSvg()  // 扫码按钮图标 HTML（img）
 */
(function (global) {
  var ZXING_SRC =
    "https://cdn.jsdelivr.net/npm/@zxing/library@0.21.3/umd/index.min.js";
  var zxingLoading = null;
  var reader = null;
  var overlay = null;
  var videoEl = null;
  var active = false;
  var onResultCb = null;

  function iconSvg() {
    return (
      '<img class="bcs-icon" src="/h5/assets/image/scan1.png" alt="" width="22" height="22"/>'
    );
  }

  function ensureZXing() {
    if (global.ZXing && global.ZXing.BrowserMultiFormatReader) {
      return Promise.resolve(global.ZXing);
    }
    if (zxingLoading) {
      return zxingLoading;
    }
    zxingLoading = new Promise(function (resolve, reject) {
      var s = document.createElement("script");
      s.src = ZXING_SRC;
      s.async = true;
      s.onload = function () {
        if (global.ZXing && global.ZXing.BrowserMultiFormatReader) {
          resolve(global.ZXing);
        } else {
          reject(new Error("ZXing 加载失败"));
        }
      };
      s.onerror = function () {
        reject(new Error("无法加载扫码库，请检查网络"));
      };
      document.head.appendChild(s);
    });
    return zxingLoading;
  }

  function ensureOverlay() {
    if (overlay) {
      return overlay;
    }
    overlay = document.createElement("div");
    overlay.className = "bcs-overlay";
    overlay.hidden = true;
    overlay.innerHTML =
      '<div class="bcs-panel">' +
      '<div class="bcs-hd">' +
      '<span class="bcs-title">扫码查商品</span>' +
      '<button type="button" class="bcs-close" data-bcs-close="1" aria-label="关闭">×</button>' +
      "</div>" +
      '<div class="bcs-video-wrap">' +
      '<video class="bcs-video" playsinline muted></video>' +
      '<div class="bcs-frame" aria-hidden="true"></div>' +
      "</div>" +
      '<p class="bcs-hint" data-bcs-hint>对准条形码，自动识别</p>' +
      '<div class="bcs-manual">' +
      '<input type="text" class="bcs-input" inputmode="numeric" maxlength="32" placeholder="或手动输入 13 位条码" autocomplete="off"/>' +
      '<button type="button" class="bcs-submit">查询</button>' +
      "</div>" +
      "</div>";
    document.body.appendChild(overlay);

    overlay.addEventListener("click", function (e) {
      if (e.target && e.target.getAttribute("data-bcs-close") === "1") {
        close();
      }
    });
    var submitBtn = overlay.querySelector(".bcs-submit");
    var input = overlay.querySelector(".bcs-input");
    submitBtn.addEventListener("click", function () {
      submitManual();
    });
    input.addEventListener("keydown", function (e) {
      if (e.key === "Enter" || e.keyCode === 13) {
        e.preventDefault();
        submitManual();
      }
    });
    return overlay;
  }

  function setHint(text, isError) {
    var el = overlay && overlay.querySelector("[data-bcs-hint]");
    if (!el) {
      return;
    }
    el.textContent = text || "";
    el.classList.toggle("is-error", !!isError);
  }

  function normalizeCode(raw) {
    return String(raw || "").replace(/\D/g, "").trim();
  }

  function emitResult(code) {
    var c = normalizeCode(code);
    if (!c) {
      setHint("请输入有效条码", true);
      return;
    }
    var cb = onResultCb;
    close();
    if (typeof cb === "function") {
      cb(c);
    }
  }

  function submitManual() {
    var input = overlay && overlay.querySelector(".bcs-input");
    emitResult(input ? input.value : "");
  }

  function stopReader() {
    if (reader) {
      try {
        reader.reset();
      } catch (e) {
        /* ignore */
      }
      reader = null;
    }
    if (videoEl && videoEl.srcObject) {
      try {
        videoEl.srcObject.getTracks().forEach(function (t) {
          t.stop();
        });
      } catch (e2) {
        /* ignore */
      }
      videoEl.srcObject = null;
    }
  }

  function startCamera() {
    videoEl = overlay.querySelector(".bcs-video");
    setHint("正在打开摄像头…", false);
    return ensureZXing()
      .then(function (ZXing) {
        reader = new ZXing.BrowserMultiFormatReader();
        var hints = new Map();
        if (ZXing.DecodeHintType && ZXing.BarcodeFormat) {
          hints.set(ZXing.DecodeHintType.POSSIBLE_FORMATS, [
            ZXing.BarcodeFormat.EAN_13,
            ZXing.BarcodeFormat.EAN_8,
            ZXing.BarcodeFormat.CODE_128,
            ZXing.BarcodeFormat.UPC_A,
          ]);
        }
        try {
          reader.hints = hints;
        } catch (e) {
          /* older builds */
        }
        return reader.decodeFromVideoDevice(
          undefined,
          videoEl,
          function (result, err) {
            if (!active) {
              return;
            }
            if (result) {
              var text = result.getText ? result.getText() : String(result);
              emitResult(text);
              return;
            }
            if (err && err.name !== "NotFoundException") {
              /* continuous scan noise */
            }
          }
        );
      })
      .then(function () {
        setHint("对准条形码，自动识别", false);
      })
      .catch(function (err) {
        var msg =
          (err && err.message) ||
          "无法使用摄像头，请手动输入条码";
        if (/Permission|NotAllowed|Denied/i.test(String(err && err.name))) {
          msg = "未获得摄像头权限，请手动输入条码";
        } else if (!window.isSecureContext) {
          msg = "当前非 HTTPS，无法调摄像头，请手动输入";
        }
        setHint(msg, true);
        var input = overlay.querySelector(".bcs-input");
        if (input) {
          input.focus();
        }
      });
  }

  function open(opts) {
    opts = opts || {};
    onResultCb = opts.onResult || null;
    ensureOverlay();
    active = true;
    overlay.hidden = false;
    document.body.style.overflow = "hidden";
    var input = overlay.querySelector(".bcs-input");
    if (input) {
      input.value = "";
    }
    stopReader();
    startCamera();
  }

  function close() {
    active = false;
    stopReader();
    if (overlay) {
      overlay.hidden = true;
    }
    document.body.style.overflow = "";
    onResultCb = null;
  }

  global.ChaiBarcodeScan = {
    open: open,
    close: close,
    iconSvg: iconSvg,
  };
})(window);
