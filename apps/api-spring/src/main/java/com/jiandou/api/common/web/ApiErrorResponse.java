package com.jiandou.api.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

/**
 * APIError响应体。
 * @param code code值
 * @param status 状态值
 * @param error 错误值
 * @param message 消息文本
 * @param path 路径值
 * @param timestamp 时间戳值
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
    String code,
    int status,
    String error,
    String message,
    String path,
    @JsonProperty("timestamp") OffsetDateTime timestamp
) {
}
