package com.jiandou.api.generation;

import java.util.Map;

/**
 * 远程任务查询记录结构。
 * @param taskId 任务标识
 * @param status 状态值
 * @param videoUrl 视频URL值
 * @param message 消息文本
 * @param payload 附加负载数据
 */
public record RemoteTaskQueryResult(
    String taskId,
    String status,
    String videoUrl,
    String message,
    Map<String, Object> payload
) {
}
