package com.ecotea.api.service.wx;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecotea.api.common.exception.BizException;
import com.ecotea.api.domain.wx.WxMpMenu;
import com.ecotea.api.mapper.wx.WxMpMenuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 微信自定义菜单：读库并发布到微信。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxMpMenuService {

    private final WxMpMenuMapper wxMpMenuMapper;
    private final WxMpService wxMpService;

    public WxMpMenu getByAppId(String appId) {
        if (!StringUtils.hasText(appId)) {
            return null;
        }
        return wxMpMenuMapper.selectOne(new LambdaQueryWrapper<WxMpMenu>()
                .eq(WxMpMenu::getAppId, appId.trim())
                .last("LIMIT 1"));
    }

    public WxMpMenu getById(Long id) {
        if (id == null) {
            return null;
        }
        return wxMpMenuMapper.selectById(id);
    }

    /**
     * 将库中菜单 JSON 发布到微信（menu/create）。
     */
    public void publish(String appId, Long id) {
        WxMpMenu row = null;
        if (StringUtils.hasText(appId)) {
            row = getByAppId(appId);
        } else if (id != null) {
            row = getById(id);
        }
        if (row == null) {
            throw new BizException("菜单配置不存在");
        }
        if (!StringUtils.hasText(row.getConfig())) {
            throw new BizException("菜单 config 为空，请先在后台保存");
        }
        String json = row.getConfig().trim();
        if (!json.startsWith("{") || !json.contains("button")) {
            throw new BizException("菜单 config 须为含 button 的 JSON 对象");
        }
        try {
            wxMpService.getMenuService().menuCreate(json);
            log.info("wx mp menu published, id={}, appId={}", row.getId(), row.getAppId());
        } catch (WxErrorException e) {
            log.error("wx mp menuCreate failed, id={}, appId={}, err={}",
                    row.getId(), row.getAppId(), e.getError(), e);
            String msg = e.getError() != null ? e.getError().getErrorMsg() : e.getMessage();
            throw new BizException("发布到微信失败：" + msg);
        }
    }
}
