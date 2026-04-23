package com.jiandou.api.generation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 远程任务查询记录结构。
 * @param taskId 任务标识
 * @param status 状态值
 * @param videoUrl 视频URL值
 * @param message 消息文本
 * @param payload 附加负载数据
 * @param requestPayload 实际查询请求信息
 * @param httpStatus HTTP状态码
 */
public record RemoteTaskQueryResult(
    String taskId,
    String status,
    String videoUrl,
    String message,
    Map<String, Object> payload,
    Map<String, Object> requestPayload,
    int httpStatus
) {
    public RemoteTaskQueryResult {
        payload = payload == null ? Map.of() : new LinkedHashMap<>(payload);
        requestPayload = requestPayload == null ? Map.of() : new LinkedHashMap<>(requestPayload);
    }

    public RemoteTaskQueryResult(
        String taskId,
        String status,
        String videoUrl,
        String message,
        Map<String, Object> payload
    ) {
        this(taskId, status, videoUrl, message, payload, Map.of(), 0);
    }
}
