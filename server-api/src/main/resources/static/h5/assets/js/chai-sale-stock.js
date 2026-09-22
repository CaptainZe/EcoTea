/**
 * 销售侧库存展示：列表 / 详情共用。
 * ≤3 紧缺「仅剩 n 件」；>3 「现货 n 件」；例外品相仅 >0 时追加。
 */
(function (global) {
  function escapeHtml(s) {
    return String(s == null ? "" : s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function appendException(parts, label, value) {
    var n = value != null ? Number(value) : 0;
    if (!isNaN(n) && n > 0) {
      parts.push('<span class="dmg">' + escapeHtml(label + " " + n) + "</span>");
    }
  }

  function renderExceptionsHtml(item) {
    item = item || {};
    var parts = [];
    appendException(parts, "无袋", item.qtyNoBag);
    appendException(parts, "破损", item.qtyDamaged);
    appendException(parts, "破损无袋", item.qtyDamagedNoBag);
    return parts.join("");
  }

  function renderHtml(item) {
    item = item || {};
    var qty = item.totalQty != null ? Number(item.totalQty) : 0;
    if (isNaN(qty) || qty < 0) {
      qty = 0;
    }
    var scarce = qty <= 3;
    var label = scarce ? "仅剩 " + qty + " 件" : "现货 " + qty + " 件";
    return (
      '<span class="stock ' +
      (scarce ? "scarce" : "normal") +
      '">' +
      escapeHtml(label) +
      renderExceptionsHtml(item) +
      "</span>"
    );
  }

  global.ChaiSaleStock = {
    SCARCE_MAX: 3,
    renderHtml: renderHtml,
    renderExceptionsHtml: renderExceptionsHtml,
  };
})(typeof window !== "undefined" ? window : this);
