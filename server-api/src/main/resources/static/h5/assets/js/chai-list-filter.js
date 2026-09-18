/**
 * 列表筛选共用：品牌 tag 渲染（已选置顶、默认约 20、多余折叠）。
 */
(function (global) {
  var BRAND_PREVIEW = 20;

  function idInList(list, id) {
    var key = String(id);
    for (var i = 0; i < (list || []).length; i++) {
      if (String(list[i]) === key) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param {object} opts
   * @param {Array} opts.brands
   * @param {Array} opts.selectedIds
   * @param {string} opts.keyword
   * @param {boolean} opts.expanded
   * @param {function} opts.escapeHtml
   * @returns {string} html
   */
  function renderBrandTagsHtml(opts) {
    var brands = (opts && opts.brands) || [];
    var selectedIds = (opts && opts.selectedIds) || [];
    var kw = String((opts && opts.keyword) || "")
      .trim()
      .toLowerCase();
    var expanded = !!(opts && opts.expanded);
    var escapeHtml =
      opts && typeof opts.escapeHtml === "function"
        ? opts.escapeHtml
        : function (s) {
            return String(s == null ? "" : s);
          };

    var selected = [];
    var rest = [];
    for (var i = 0; i < brands.length; i++) {
      var b = brands[i];
      if (!b || b.id == null) {
        continue;
      }
      var name = String(b.name || "");
      var on = idInList(selectedIds, b.id);
      if (kw && !on && name.toLowerCase().indexOf(kw) < 0) {
        continue;
      }
      if (on) {
        selected.push(b);
      } else if (!kw || name.toLowerCase().indexOf(kw) >= 0) {
        rest.push(b);
      }
    }

    var ordered = selected.concat(rest);
    if (!ordered.length) {
      return (
        '<p class="filter-tags-empty">' +
        (brands.length ? "无匹配品牌" : "暂无品牌") +
        "</p>"
      );
    }

    // 搜索时展示全部匹配；未搜索时默认约 20，且保证已选全部可见
    var searching = !!kw;
    var limit = searching
      ? ordered.length
      : Math.max(BRAND_PREVIEW, selected.length);
    var showAll = searching || expanded || ordered.length <= limit;
    var visible = showAll ? ordered : ordered.slice(0, limit);
    var hiddenCount = ordered.length - visible.length;

    var html = [];
    for (var j = 0; j < visible.length; j++) {
      var item = visible[j];
      var isOn = idInList(selectedIds, item.id);
      html.push(
        '<button type="button" class="filter-tag' +
          (isOn ? " is-on" : "") +
          '" data-brand-id="' +
          escapeHtml(item.id) +
          '">' +
          escapeHtml(item.name || "未命名") +
          "</button>"
      );
    }

    if (!searching && hiddenCount > 0) {
      html.push(
        '<button type="button" class="filter-brand-toggle" data-brand-more="1">展开更多（' +
          escapeHtml(hiddenCount) +
          "）</button>"
      );
    } else if (!searching && expanded && ordered.length > BRAND_PREVIEW) {
      html.push(
        '<button type="button" class="filter-brand-toggle" data-brand-less="1">收起</button>'
      );
    }

    return html.join("");
  }

  global.ChaiListFilter = {
    BRAND_PREVIEW: BRAND_PREVIEW,
    idInList: idInList,
    renderBrandTagsHtml: renderBrandTagsHtml,
  };
})(window);
