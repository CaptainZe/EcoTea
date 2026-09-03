/**
 * 通用备案页脚：挂载到 #siteFooter，拉取 GET /site/beian。
 * 各 H5 页引入本脚本 + site-footer.css 即可。
 */
(function () {
  var mount = document.getElementById("siteFooter");
  if (!mount) {
    return;
  }

  function hasText(s) {
    return !!(s && String(s).trim());
  }

  function appendLink(parent, text, url) {
    var a = document.createElement("a");
    a.textContent = text;
    a.href = url || "#";
    a.target = "_blank";
    a.rel = "noreferrer noopener";
    parent.appendChild(a);
  }

  function render(data) {
    var footer = document.createElement("footer");
    footer.className = "site-beian";

    var mpsOk = hasText(data.mpsText) && hasText(data.mpsUrl);
    var icpOk = hasText(data.icpText) && hasText(data.icpUrl);

    if (!mpsOk && !icpOk) {
      footer.hidden = true;
      mount.appendChild(footer);
      return;
    }

    if (mpsOk) {
      appendLink(footer, data.mpsText.trim(), data.mpsUrl.trim());
    }
    if (icpOk) {
      appendLink(footer, data.icpText.trim(), data.icpUrl.trim());
    }
    mount.appendChild(footer);
  }

  fetch("/site/beian")
    .then(function (res) {
      return res.json();
    })
    .then(function (body) {
      if (!body || body.code !== 0 || !body.data) {
        return;
      }
      render(body.data);
    })
    .catch(function () {
      /* 页脚失败不影响业务页 */
    });
})();
