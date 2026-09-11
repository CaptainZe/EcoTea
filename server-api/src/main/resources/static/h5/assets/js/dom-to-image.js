/**
 * DomToImage — 浏览器端通用「DOM → PNG」工具（无业务逻辑）。
 *
 * 依赖：global `modernScreenshot`（modern-screenshot，见 vendor/modern-screenshot.js）
 *
 * 用法约定：
 * - 只截传入节点，业务方自行准备 DOM（建议固定设计宽度）。
 * - 微信内保存：将 dataURL 赋给 <img>，引导用户长按保存（文案由业务提供）。
 * - a[download] 仅作非微信增强，勿当作主路径。
 * - 建议 scale ≤ 2，避免大 DOM 卡顿。
 * - 跨域图片需 CORS；污染 canvas 时会抛明确错误。
 *
 * @example
 * await DomToImage.waitForImages(el);
 * const dataUrl = await DomToImage.captureElement(el, { scale: 2, backgroundColor: '#fff' });
 * previewImg.src = dataUrl;
 */
(function (global) {
  var lock = Promise.resolve();
  var DEFAULT_TIMEOUT = 15000;

  function getLib() {
    var lib = global.modernScreenshot;
    if (!lib || typeof lib.domToPng !== "function") {
      throw new Error(
        "DomToImage: modernScreenshot 未加载，请先引入 vendor/modern-screenshot.js"
      );
    }
    return lib;
  }

  function resolveElement(target) {
    if (!target) {
      throw new Error("DomToImage: target 不能为空");
    }
    if (typeof target === "string") {
      var el = document.querySelector(target);
      if (!el) {
        throw new Error("DomToImage: 未找到节点 " + target);
      }
      return el;
    }
    if (target.nodeType === 1) {
      return target;
    }
    throw new Error("DomToImage: target 须为 Element 或选择器字符串");
  }

  function isWeChatBrowser() {
    return /MicroMessenger/i.test(
      (global.navigator && global.navigator.userAgent) || ""
    );
  }

  function dataUrlToBlob(dataUrl) {
    if (!dataUrl || dataUrl.indexOf("data:") !== 0) {
      throw new Error("DomToImage: 非法 dataURL");
    }
    var parts = dataUrl.split(",");
    var meta = parts[0] || "";
    var data = parts[1] || "";
    var isBase64 = /;base64/i.test(meta);
    var mimeMatch = meta.match(/data:([^;]+)/);
    var mime = (mimeMatch && mimeMatch[1]) || "image/png";
    var binary = isBase64 ? global.atob(data) : decodeURIComponent(data);
    var len = binary.length;
    var bytes = new Uint8Array(len);
    for (var i = 0; i < len; i++) {
      bytes[i] = binary.charCodeAt(i);
    }
    return new Blob([bytes], { type: mime });
  }

  function downloadDataUrl(dataUrl, filename) {
    filename = filename || "screenshot.png";
    var a = document.createElement("a");
    a.href = dataUrl;
    a.download = filename;
    a.rel = "noopener";
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  }

  /**
   * 等待根节点内 img 加载完成。
   * @param {Element} root
   * @param {number} [timeoutMs=15000]
   * @param {{ onTimeout?: 'reject'|'continue' }} [opts]
   */
  function waitForImages(root, timeoutMs, opts) {
    opts = opts || {};
    var onTimeout = opts.onTimeout || "reject";
    timeoutMs = timeoutMs == null ? DEFAULT_TIMEOUT : timeoutMs;
    root = resolveElement(root);

    var imgs = Array.prototype.slice.call(root.querySelectorAll("img"));
    if (!imgs.length) {
      return Promise.resolve();
    }

    var pending = imgs.map(function (img) {
      if (img.complete && img.naturalWidth > 0) {
        if (typeof img.decode === "function") {
          return img.decode().catch(function () {
            /* 已显示则继续 */
          });
        }
        return Promise.resolve();
      }
      return new Promise(function (resolve, reject) {
        var done = false;
        function finish(ok, err) {
          if (done) {
            return;
          }
          done = true;
          if (ok) {
            resolve();
          } else {
            reject(err || new Error("图片加载失败: " + (img.currentSrc || img.src)));
          }
        }
        img.addEventListener(
          "load",
          function () {
            if (typeof img.decode === "function") {
              img.decode().then(
                function () {
                  finish(true);
                },
                function () {
                  finish(true);
                }
              );
            } else {
              finish(true);
            }
          },
          { once: true }
        );
        img.addEventListener(
          "error",
          function () {
            finish(false);
          },
          { once: true }
        );
      });
    });

    var waitAll = Promise.all(pending);
    if (!timeoutMs || timeoutMs <= 0) {
      return waitAll;
    }

    return new Promise(function (resolve, reject) {
      var settled = false;
      var timer = setTimeout(function () {
        if (settled) {
          return;
        }
        settled = true;
        if (onTimeout === "continue") {
          if (typeof console !== "undefined" && console.warn) {
            console.warn("DomToImage.waitForImages: 超时，继续截图");
          }
          resolve();
        } else {
          reject(new Error("DomToImage.waitForImages: 超时 " + timeoutMs + "ms"));
        }
      }, timeoutMs);

      waitAll.then(
        function () {
          if (settled) {
            return;
          }
          settled = true;
          clearTimeout(timer);
          resolve();
        },
        function (err) {
          if (settled) {
            return;
          }
          settled = true;
          clearTimeout(timer);
          reject(err);
        }
      );
    });
  }

  function withLock(fn) {
    var run = lock.then(fn, fn);
    lock = run.then(
      function () {},
      function () {}
    );
    return run;
  }

  function buildLibOptions(options) {
    options = options || {};
    var dpr =
      (global.devicePixelRatio && Number(global.devicePixelRatio)) || 1;
    var scale =
      options.scale != null
        ? Number(options.scale)
        : Math.min(2, Math.max(1, dpr));
    if (isNaN(scale) || scale <= 0) {
      scale = 2;
    }
    if (scale > 2) {
      scale = 2;
    }

    var libOpts = {
      scale: scale,
      backgroundColor:
        options.backgroundColor != null ? options.backgroundColor : "#ffffff",
      timeout: options.timeout != null ? options.timeout : DEFAULT_TIMEOUT,
    };
    if (options.width != null) {
      libOpts.width = options.width;
    }
    if (options.height != null) {
      libOpts.height = options.height;
    }
    if (typeof options.filter === "function") {
      libOpts.filter = options.filter;
    }
    if (typeof options.ignoreElements === "function") {
      var ignoreFn = options.ignoreElements;
      libOpts.filter = function (node) {
        if (ignoreFn(node)) {
          return false;
        }
        if (typeof options.filter === "function") {
          return options.filter(node);
        }
        return true;
      };
    }
    return libOpts;
  }

  function assertNotTainted(dataUrl) {
    if (!dataUrl || typeof dataUrl !== "string") {
      throw new Error("DomToImage: 截图结果为空");
    }
    if (dataUrl.indexOf("data:image") !== 0) {
      throw new Error("DomToImage: 截图结果不是图片 dataURL");
    }
    // 极小占位图多为失败；PNG 头 + 过短视为异常
    if (dataUrl.length < 100) {
      throw new Error(
        "DomToImage: 截图结果异常偏短，可能因跨域图片污染 canvas，请确认图片 CORS"
      );
    }
    return dataUrl;
  }

  /**
   * @param {Element|string} target
   * @param {object} [options]
   * @returns {Promise<string>} PNG dataURL
   */
  function captureElement(target, options) {
    options = options || {};
    return withLock(function () {
      var el = resolveElement(target);
      var lib = getLib();
      var skipWait = options.skipWaitImages === true;
      var timeout =
        options.timeout != null ? options.timeout : DEFAULT_TIMEOUT;
      var waitOpts = {
        onTimeout: options.imagesTimeoutContinue ? "continue" : "reject",
      };

      var chain = skipWait
        ? Promise.resolve()
        : waitForImages(el, timeout, waitOpts);

      return chain
        .then(function () {
          return lib.domToPng(el, buildLibOptions(options));
        })
        .then(assertNotTainted)
        .catch(function (err) {
          var msg = (err && err.message) || String(err);
          if (/taint|cross-origin|SecurityError|CORS/i.test(msg)) {
            throw new Error(
              "DomToImage: 跨域图片导致 canvas 污染，无法导出。请为图片配置 CORS（Access-Control-Allow-Origin）并使用 crossOrigin=anonymous。原始错误: " +
                msg
            );
          }
          throw err instanceof Error ? err : new Error(msg);
        });
    });
  }

  function captureElementToBlob(target, options) {
    return captureElement(target, options).then(dataUrlToBlob);
  }

  global.DomToImage = {
    captureElement: captureElement,
    captureElementToBlob: captureElementToBlob,
    waitForImages: waitForImages,
    dataUrlToBlob: dataUrlToBlob,
    isWeChatBrowser: isWeChatBrowser,
    downloadDataUrl: downloadDataUrl,
  };
})(window);
