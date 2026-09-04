(function () {
  var els = {
    pageTitle: document.getElementById("pageTitle"),
    loading: document.getElementById("loading"),
    bodyBlock: document.getElementById("bodyBlock"),
    bodyText: document.getElementById("bodyText"),
    ctaBtn: document.getElementById("ctaBtn"),
    empty: document.getElementById("empty")
  };

  function text(v) {
    return v == null ? "" : String(v).trim();
  }

  function apply(data) {
    els.loading.hidden = true;
    if (!data) {
      els.empty.hidden = false;
      els.ctaBtn.hidden = false;
      els.ctaBtn.href = "/h5/about.html";
      els.ctaBtn.textContent = "联系客服";
      els.bodyBlock.hidden = false;
      els.bodyText.hidden = true;
      return;
    }

    var title = text(data.title);
    var body = text(data.body);
    var ctaText = text(data.ctaText) || text(data.cta_text) || "联系客服";
    var ctaUrl = text(data.ctaUrl) || text(data.cta_url) || "/h5/about.html";

    if (title) {
      els.pageTitle.textContent = title;
      document.title = title;
    }

    if (!body) {
      els.empty.hidden = false;
      els.bodyBlock.hidden = true;
      return;
    }

    els.bodyText.textContent = body;
    els.bodyText.hidden = false;
    els.empty.hidden = true;
    els.bodyBlock.hidden = false;

    els.ctaBtn.hidden = false;
    els.ctaBtn.textContent = ctaText;
    els.ctaBtn.href = ctaUrl;
  }

  fetch("/wx/globalConfig/recycleDesc")
    .then(function (res) {
      return res.json();
    })
    .then(function (body) {
      if (!body || body.code !== 0) {
        apply(null);
        return;
      }
      apply(body.data);
    })
    .catch(function () {
      apply(null);
    });
})();
