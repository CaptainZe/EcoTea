package com.appsinnova.admin.business.common.enums.chai;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 列表「是否删除」筛选（请求参数 deleted）。
 * <ul>
 *   <li>{@link #ALL}(-1)：全部，查询条件为 null（不限）</li>
 *   <li>{@link #NO}(0)：未删除（列表默认）</li>
 *   <li>{@link #YES}(1)：已删除</li>
 * </ul>
 * 与库字段 deleted（0/1）不同：本枚举多一个「全部」查询码。
 */
@Getter
@AllArgsConstructor
public enum ChaiDeletedFilter {
    ALL(-1, "全部"),
    NO(0, "否"),
    YES(1, "是"),
    ;

    private final Integer code;
    private final String message;

    /**
     * 解析请求参数；未传或无法识别时默认 {@link #NO}（只看未删除）。
     */
    public static ChaiDeletedFilter fromRequestParam(String param) {
        if (param == null || param.trim().isEmpty()) {
            return NO;
        }
        String raw = param.trim();
        for (ChaiDeletedFilter item : values()) {
            if (String.valueOf(item.code).equals(raw)) {
                return item;
            }
        }
        return NO;
    }

    /**
     * 转为实体查询字段：全部 → null；否/是 → 0/1。
     */
    public Integer toQueryDeleted() {
        return this == ALL ? null : code;
    }

    /**
     * 一步：请求参数 → 查询用 deleted（null 表示不限）。
     */
    public static Integer parseQueryDeleted(String param) {
        return fromRequestParam(param).toQueryDeleted();
    }
}
