package com.ecotea.api.common.exception;

import com.ecotea.api.common.constant.ErrorCode;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(String message) {
        this(ErrorCode.BIZ_ERROR, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
