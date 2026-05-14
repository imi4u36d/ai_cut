package com.jiandou.api.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * APIError响应体。
 * @param code code值
 * @param status 状态值
 * @param error 错误值
 * @param message 消息文本
 * @param details 详情值
 * @param path 路径值
 * @param timestamp 时间戳值
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
    String code,
    int status,
    String error,
    String message,
    Map<String, Object> details,
    String path,
    @JsonProperty("timestamp") OffsetDateTime timestamp
) {
    public ApiErrorResponse(String code, int status, String error, String message, String path, OffsetDateTime timestamp) {
        this(code, status, error, message, Map.of(), path, timestamp);
    }
}
