package com.ecotea.api.common.result;

import com.ecotea.api.common.constant.ErrorCode;
import lombok.Data;

import java.io.Serializable;

@Data
public class ApiResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;

    public static <T> ApiResult<T> ok() {
        return ok(null);
    }

    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(ErrorCode.SUCCESS);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <T> ApiResult<T> fail(String message) {
        return fail(ErrorCode.BIZ_ERROR, message);
    }

    public static <T> ApiResult<T> fail(int code, String message) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
