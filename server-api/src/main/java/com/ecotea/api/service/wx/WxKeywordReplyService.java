package com.ecotea.api.service.wx;

import com.ecotea.api.common.constant.WxConstant;
import com.ecotea.api.config.EcoTeaSiteProperties;
import com.ecotea.api.service.chai.ChaiSkuSaleQueryService;
import com.ecotea.api.vo.chai.ChaiSkuSaleItemVO;
import com.ecotea.api.vo.chai.ChaiSkuSalePageVO;
import com.ecotea.api.vo.wx.WxCustomerServiceConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 微信文本关键词：客服引导、品牌/品名查价。
 * 文内跳转使用微信文本 a 标签（仅显示链文案，不露完整 URL）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxKeywordReplyService {

    private static final Set<String> CS_KEYWORDS = new HashSet<>(Arrays.asList(
            "客服", "联系我们", "人工", "联系客服", "人工客服"
    ));

    private final ChaiSkuSaleQueryService chaiSkuSaleQueryService;
    private final WxGlobalConfigService wxGlobalConfigService;
    private final EcoTeaSiteProperties ecoTeaSiteProperties;

    public String reply(String rawContent) {
        String text = rawContent == null ? "" : rawContent.trim();
        if (!StringUtils.hasText(text)) {
            return "请发送品牌全称或品名查询有货价目；发送「客服」获取联系方式。";
        }
        if (CS_KEYWORDS.contains(text)) {
            return buildCustomerServiceReply();
        }
        return buildSaleKeywordReply(text);
    }

    private String buildCustomerServiceReply() {
        String aboutUrl = absUrl(WxConstant.H5_ABOUT_PATH);
        WxCustomerServiceConfig cs = wxGlobalConfigService.getCustomerService();
        StringBuilder sb = new StringBuilder();
        sb.append("如需对接购买或回收，请").append(wxHref(aboutUrl, "打开「联系我们」")).append("。");
        if (cs != null && cs.getItems() != null && !cs.getItems().isEmpty()) {
            sb.append("\n\n客服微信：");
            for (int i = 0; i < cs.getItems().size(); i++) {
                String id = cs.getItems().get(i).getWechatId();
                if (!StringUtils.hasText(id)) {
                    continue;
                }
                if (i > 0) {
                    sb.append(" / ");
                }
                sb.append(id.trim());
            }
        } else {
            sb.append("\n\n也可回复品牌/品名查询现货价目。");
        }
        return sb.toString();
    }

    private String buildSaleKeywordReply(String keyword) {
        ChaiSkuSalePageVO page = chaiSkuSaleQueryService.pageSaleList(
                keyword, null, 1, WxConstant.SALE_KEYWORD_MAX_ITEMS);
        String h5Url = saleListUrl(keyword);
        long total = page.getTotal();
        List<ChaiSkuSaleItemVO> list = page.getList();

        if (total <= 0 || list == null || list.isEmpty()) {
            return "未找到与「" + keyword + "」匹配的有货商品。\n"
                    + "可换品牌全称/品名再试，或"
                    + wxHref(h5Url, "打开价目页浏览")
                    + "。";
        }

        String matchHint;
        if ("brand_exact".equals(page.getMatchType())) {
            matchHint = "品牌精确";
        } else if ("name_like".equals(page.getMatchType())) {
            matchHint = "品名匹配";
        } else {
            matchHint = "匹配";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("「").append(keyword).append("」有货 ").append(total).append(" 件（")
                .append(matchHint).append("）\n\n");

        int shown = 0;
        for (ChaiSkuSaleItemVO item : list) {
            String block = formatSaleItem(item);
            // 多行条目更长，预留页脚与空行
            if (sb.length() + block.length() + 100 > WxConstant.TEXT_SOFT_MAX_CHARS) {
                break;
            }
            sb.append(block).append("\n\n");
            shown++;
        }

        boolean truncated = total > shown;
        if (truncated || total > WxConstant.SALE_KEYWORD_MAX_ITEMS) {
            sb.append("更多现货请").append(wxHref(h5Url, "打开价目页")).append("。");
        } else {
            sb.append("详情与图片请").append(wxHref(h5Url, "打开价目页")).append("。");
        }
        return sb.toString().trim();
    }

    /**
     * 单条商品多行展示（条目间由调用方追加空行；无序号）。
     * <pre>
     * &lt;a href="详情"&gt;品牌 · 品名&lt;/a&gt;
     * 等级：…
     * 规格：…
     * 批次：…
     * 保质期：…
     * 官方：…
     * 售价：…
     * 库存数量：n[，破损：m]
     * </pre>
     */
    private String formatSaleItem(ChaiSkuSaleItemVO item) {
        String title;
        if (StringUtils.hasText(item.getTitle())) {
            title = item.getTitle().trim();
        } else if (StringUtils.hasText(item.getName())) {
            title = item.getName().trim();
        } else {
            title = "商品";
        }

        StringBuilder block = new StringBuilder();
        if (item.getId() != null) {
            block.append(wxHref(saleDetailUrl(item.getId()), title));
        } else {
            block.append(title);
        }

        appendLabeledLine(block, "等级", item.getGradeName());
        appendLabeledLine(block, "规格", item.getSpecShow());
        appendLabeledLine(block, "批次", item.getProdBatchShow());
        appendLabeledLine(block, "保质期", item.getExpirationName());
        appendLabeledLine(block, "官方", formatPriceLine(item.getOfficialPriceShow()));
        appendLabeledLine(block, "售价", formatPriceLine(item.getSalePriceShow()));

        if (item.getTotalQty() != null) {
            block.append('\n').append("库存数量：").append(item.getTotalQty());
            if (item.getDamageQty() != null && item.getDamageQty() > 0) {
                block.append("，破损：").append(item.getDamageQty());
            }
        }
        return block.toString();
    }

    private static void appendLabeledLine(StringBuilder block, String label, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        block.append('\n').append(label).append('：').append(value.trim());
    }

    /** 半角括号改为全角，贴近展示文案。 */
    private static String formatPriceLine(String show) {
        if (!StringUtils.hasText(show)) {
            return null;
        }
        return show.trim().replace('(', '（').replace(')', '）');
    }

    /**
     * 微信文本消息可点击链：仅 a 标签；标签内勿换行。
     */
    private static String wxHref(String url, String label) {
        return "<a href=\"" + url + "\">" + label + "</a>";
    }

    private String saleDetailUrl(Long id) {
        return absUrl(WxConstant.H5_SALE_DETAIL_PATH) + "?id=" + id;
    }

    private String saleListUrl(String keyword) {
        String base = absUrl(WxConstant.H5_SALE_PATH);
        try {
            return base + "?keyword=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException e) {
            return base + "?keyword=" + keyword;
        }
    }

    private String absUrl(String path) {
        String root = ecoTeaSiteProperties.getPublicBaseUrl();
        if (!StringUtils.hasText(root)) {
            root = "https://api.ecotea.cn";
        }
        root = root.trim();
        if (root.endsWith("/")) {
            root = root.substring(0, root.length() - 1);
        }
        if (!path.startsWith("/")) {
            return root + "/" + path;
        }
        return root + path;
    }
}
