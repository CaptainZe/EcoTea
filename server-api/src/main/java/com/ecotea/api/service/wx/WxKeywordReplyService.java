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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 微信文本关键词：客服引导、品牌/品名查价。
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
        sb.append("如需对接购买或回收，请打开「联系我们」：\n").append(aboutUrl);
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
                    + "可换品牌全称/品名再试，或打开价目页浏览：\n"
                    + h5Url;
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
            String line = formatSaleLine(shown + 1, item);
            if (sb.length() + line.length() + 80 > WxConstant.TEXT_SOFT_MAX_CHARS) {
                break;
            }
            sb.append(line).append('\n');
            shown++;
        }

        boolean truncated = total > shown;
        if (truncated || total > WxConstant.SALE_KEYWORD_MAX_ITEMS) {
            sb.append("\n更多现货请打开价目页：\n").append(h5Url);
        } else {
            sb.append("\n详情与图片：\n").append(h5Url);
        }
        return sb.toString().trim();
    }

    private String formatSaleLine(int index, ChaiSkuSaleItemVO item) {
        StringBuilder line = new StringBuilder();
        line.append(index).append(". ");
        if (StringUtils.hasText(item.getTitle())) {
            line.append(item.getTitle().trim());
        } else if (StringUtils.hasText(item.getName())) {
            line.append(item.getName().trim());
        } else {
            line.append("商品");
        }
        if (StringUtils.hasText(item.getSpecShow())) {
            line.append(" | ").append(item.getSpecShow().trim());
        }
        if (StringUtils.hasText(item.getProdBatchShow())) {
            line.append(" | ").append(item.getProdBatchShow().trim());
        }
        if (StringUtils.hasText(item.getSalePriceShow())) {
            line.append(" | ").append(item.getSalePriceShow().trim());
        }
        if (item.getTotalQty() != null) {
            line.append(" | 库存").append(item.getTotalQty());
        }
        if (item.getDamageQty() != null && item.getDamageQty() > 0) {
            line.append(" 破损").append(item.getDamageQty());
        }
        return line.toString();
    }

    private String saleListUrl(String keyword) {
        String base = absUrl(WxConstant.H5_SALE_PATH);
        try {
            return base + "?keyword=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
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
