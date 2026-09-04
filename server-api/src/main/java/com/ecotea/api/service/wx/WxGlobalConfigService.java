package com.ecotea.api.service.wx;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecotea.api.common.enums.wx.WxGlobalConfigType;
import com.ecotea.api.common.utils.JsonUtils;
import com.ecotea.api.domain.wx.WxGlobalConfig;
import com.ecotea.api.mapper.wx.WxGlobalConfigMapper;
import com.ecotea.api.vo.wx.WxCustomerServiceConfig;
import com.ecotea.api.vo.wx.WxRecycleDescConfig;
import com.ecotea.api.vo.wx.WxSaleH5CopyConfig;
import com.ecotea.api.vo.wx.WxSubscribeWelcomeConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 微信通用配置（只读）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxGlobalConfigService {

    private final WxGlobalConfigMapper wxGlobalConfigMapper;

    public WxGlobalConfig getByType(Integer type) {
        if (type == null) {
            return null;
        }
        return wxGlobalConfigMapper.selectOne(new LambdaQueryWrapper<WxGlobalConfig>()
                .eq(WxGlobalConfig::getType, type)
                .last("LIMIT 1"));
    }

    /**
     * 关注欢迎语正文；无配置或解析失败返回 null。
     */
    public String getSubscribeWelcomeText() {
        WxGlobalConfig row = getByType(WxGlobalConfigType.SUBSCRIBE_WELCOME.getCode());
        if (row == null || !StringUtils.hasText(row.getConfig())) {
            return null;
        }
        WxSubscribeWelcomeConfig content = JsonUtils.readValue(row.getConfig(), WxSubscribeWelcomeConfig.class);
        if (content == null || !StringUtils.hasText(content.getText())) {
            log.warn("wx subscribe welcome config invalid, id={}", row.getId());
            return null;
        }
        return content.getText().trim();
    }

    /**
     * 销售 H5 弹窗/顶栏文案；无配置返回 null。
     */
    public WxSaleH5CopyConfig getSaleH5Copy() {
        WxGlobalConfig row = getByType(WxGlobalConfigType.SALE_H5_COPY.getCode());
        if (row == null || !StringUtils.hasText(row.getConfig())) {
            return null;
        }
        WxSaleH5CopyConfig content = JsonUtils.readValue(row.getConfig(), WxSaleH5CopyConfig.class);
        if (content == null) {
            log.warn("wx sale h5 copy config invalid, id={}", row.getId());
            return null;
        }
        boolean empty = !StringUtils.hasText(content.getNoticeTitle())
                && !StringUtils.hasText(content.getNoticeBody())
                && !StringUtils.hasText(content.getBannerText());
        if (empty) {
            return null;
        }
        return content;
    }

    /**
     * 回收说明；正文为空视为无效。
     */
    public WxRecycleDescConfig getRecycleDesc() {
        WxGlobalConfig row = getByType(WxGlobalConfigType.RECYCLE_DESC.getCode());
        if (row == null || !StringUtils.hasText(row.getConfig())) {
            return null;
        }
        WxRecycleDescConfig content = JsonUtils.readValue(row.getConfig(), WxRecycleDescConfig.class);
        if (content == null || !StringUtils.hasText(content.getBody())) {
            log.warn("wx recycle desc config invalid, id={}", row == null ? null : row.getId());
            return null;
        }
        return content;
    }

    /**
     * 客服配置；无有效 items 返回 null。
     */
    public WxCustomerServiceConfig getCustomerService() {
        WxGlobalConfig row = getByType(WxGlobalConfigType.CUSTOMER_SERVICE.getCode());
        if (row == null || !StringUtils.hasText(row.getConfig())) {
            return null;
        }
        WxCustomerServiceConfig content = JsonUtils.readValue(row.getConfig(), WxCustomerServiceConfig.class);
        if (content == null || content.getItems() == null || content.getItems().isEmpty()) {
            log.warn("wx customer service config empty, id={}", row.getId());
            return null;
        }
        return content;
    }
}
