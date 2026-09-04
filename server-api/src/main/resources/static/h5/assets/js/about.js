(function () {
  var els = {
    loading: document.getElementById("loading"),
    empty: document.getElementById("empty"),
    list: document.getElementById("list"),
    hint: document.getElementById("hint")
  };

  function escapeHtml(s) {
    return String(s == null ? "" : s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function render(items) {
    els.loading.hidden = true;
    if (!items || !items.length) {
      els.empty.hidden = false;
      els.hint.hidden = true;
      return;
    }
    els.empty.hidden = true;
    els.hint.hidden = false;
    els.list.innerHTML = items.map(function (it) {
      var id = it.wechatId || it.wechat_id || "";
      var qr = it.qrImageUrl || it.qr_image_url || "";
      return (
        '<li class="card">' +
        '<div class="wechat">' + escapeHtml(id) + "</div>" +
        (qr
          ? '<img class="qr" src="' + escapeHtml(qr) + '" alt="客服二维码" loading="lazy"/>'
          : "") +
        "</li>"
      );
    }).join("");
  }

  fetch("/wx/globalConfig/customerService")
    .then(function (res) {
      return res.json();
    })
    .then(function (body) {
      if (!body || body.code !== 0 || !body.data) {
        render([]);
        return;
      }
      render(body.data.items || []);
    })
    .catch(function () {
      render([]);
    });
})();
