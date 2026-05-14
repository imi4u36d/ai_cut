package com.jiandou.api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 统一业务异常基类，约束错误码和 HTTP 状态的表达方式。
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    /**
     * 创建新的API异常。
     * @param status 状态值
     * @param code code值
     * @param message 消息文本
     */
    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /**
     * 创建新的API异常。
     * @param status 状态值
     * @param code code值
     * @param message 消息文本
     * @param cause cause值
     */
    public ApiException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    /**
     * 处理状态。
     * @return 处理结果
     */
    public HttpStatus status() {
        return status;
    }

    /**
     * 处理code。
     * @return 处理结果
     */
    public String code() {
        return code;
    }
}
