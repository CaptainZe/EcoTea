package com.ecotea.api.common.constant;

/**
 * API 统一错误码。业务侧新增码段时在此登记，避免魔法数字。
 */
public interface ErrorCode {

    /** 成功 */
    int SUCCESS = 0;

    /** 通用业务失败 */
    int BIZ_ERROR = 1;

    /** 参数校验失败 */
    int BAD_REQUEST = 400;

    /** 未授权（如 Api-Key 无效） */
    int UNAUTHORIZED = 401;

    /** 系统异常（对外统一文案，细节只写日志） */
    int SYSTEM_ERROR = 500;
}
