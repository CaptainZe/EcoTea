package com.appsinnova.admin.business.common.enums.chai;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SPU 列表「码类型」筛选（请求参数 barcodeKind）。
 * <ul>
 *   <li>{@link #ALL}(-1)：全部，不加条码类型条件</li>
 *   <li>{@link #EMPTY}(0)：无码（空或 null）</li>
 *   <li>{@link #NATIONAL}(1)：国标码（{@code 69…}）</li>
 *   <li>{@link #SYSTEM}(2)：系统码（严格 {@code 29…}）</li>
 * </ul>
 * 非持久化字段；与库中 barcode 值本身无关。
 */
@Getter
@AllArgsConstructor
public enum ChaiBarcodeKind {
    ALL(-1, "全部"),
    EMPTY(0, "无码"),
    NATIONAL(1, "国标码"),
    SYSTEM(2, "系统码"),
    ;

    private final Integer code;
    private final String message;

    /**
     * 解析请求/实体上的码类型；未传或无法识别时默认 {@link #ALL}。
     */
    public static ChaiBarcodeKind fromCode(Integer code) {
        if (code == null) {
            return ALL;
        }
        for (ChaiBarcodeKind item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return ALL;
    }

    /**
     * 解析请求参数字符串；空或无法识别 → {@link #ALL}。
     */
    public static ChaiBarcodeKind fromRequestParam(String param) {
        if (param == null || param.trim().isEmpty()) {
            return ALL;
        }
        try {
            return fromCode(Integer.valueOf(param.trim()));
        } catch (NumberFormatException e) {
            return ALL;
        }
    }
}
