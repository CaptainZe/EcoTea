/**
 * 内部价目共用：对外详情链接、复制、toast。
 */
(function (global) {
  function publicSaleDetailUrl(id) {
    return (
      global.location.origin +
      "/h5/chai/sale-detail.html?id=" +
      encodeURIComponent(id)
    );
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

  function copyText(text) {
    if (!text) {
      return Promise.reject(new Error("empty"));
    }
    if (
      global.navigator &&
      global.navigator.clipboard &&
      global.navigator.clipboard.writeText
    ) {
      return global.navigator.clipboard.writeText(text).catch(function () {
        return fallbackCopy(text);
      });
    }
    return fallbackCopy(text);
  }

  function toast(msg) {
    var el = document.querySelector(".inner-toast");
    if (!el) {
      el = document.createElement("div");
      el.className = "inner-toast";
      document.body.appendChild(el);
    }
    el.textContent = msg;
    el.classList.add("show");
    clearTimeout(el._timer);
    el._timer = setTimeout(function () {
      el.classList.remove("show");
    }, 1800);
  }

  function copyPublicDetail(id) {
    var url = publicSaleDetailUrl(id);
    return copyText(url).then(
      function () {
        toast("已复制对外详情链接");
      },
      function () {
        toast("复制失败");
      }
    );
  }

  global.ChaiInnerUtil = {
    publicSaleDetailUrl: publicSaleDetailUrl,
    copyText: copyText,
    toast: toast,
    copyPublicDetail: copyPublicDetail,
  };
})(window);
