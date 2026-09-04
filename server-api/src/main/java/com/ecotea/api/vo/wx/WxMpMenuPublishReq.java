package com.ecotea.api.vo.wx;

import lombok.Data;

/**
 * 发布菜单请求。
 */
@Data
public class WxMpMenuPublishReq {

    /** 优先；与 wx_mp_menu.app_id 对应 */
    private String appId;

    /** 可选；未传 appId 时按主键查找 */
    private Long id;
}
