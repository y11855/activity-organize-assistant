package com.aiapp.activityassistant.common.exception;

import com.aiapp.activityassistant.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常：封装业务错误码与提示信息，由 GlobalExceptionHandler 统一转换为响应。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
