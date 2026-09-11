/**
 * 销售侧库存展示：列表 / 详情共用。
 * ≤3 紧缺「仅剩 n 件」；>3 「现货 n 件」；破损跟在后面。
 */
(function (global) {
  function escapeHtml(s) {
    return String(s == null ? "" : s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function renderHtml(item) {
    item = item || {};
    var qty = item.totalQty != null ? Number(item.totalQty) : 0;
    if (isNaN(qty) || qty < 0) {
      qty = 0;
    }
    var scarce = qty <= 3;
    var label = scarce ? "仅剩 " + qty + " 件" : "现货 " + qty + " 件";
    var dmg = "";
    if (item.damageQty != null && item.damageQty > 0) {
      dmg =
        '<span class="dmg">破损 ' + escapeHtml(item.damageQty) + "</span>";
    }
    return (
      '<span class="stock ' +
      (scarce ? "scarce" : "normal") +
      '">' +
      escapeHtml(label) +
      dmg +
      "</span>"
    );
  }

  global.ChaiSaleStock = {
    SCARCE_MAX: 3,
    renderHtml: renderHtml,
  };
})(window);
