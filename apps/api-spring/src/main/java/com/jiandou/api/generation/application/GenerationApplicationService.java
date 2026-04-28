package com.jiandou.api.generation.application;

import java.util.List;
import java.util.Map;

/**
 * 生成应用服务。
 */
public interface GenerationApplicationService {

    /**
     * 处理目录。
     * @return 处理结果
     */
    Map<String, Object> catalog();

    /**
     * 创建运行。
     * @param request 请求体
     * @return 处理结果
     */
    Map<String, Object> createRun(Map<String, Object> request);

    /**
     * 创建异步运行。
     * @param request 请求体
     * @return 处理结果
     */
    Map<String, Object> createAsyncRun(Map<String, Object> request);

    /**
     * 列出Runs。
     * @param limit 返回的最大条目数
     * @return 处理结果
     */
    List<Map<String, Object>> listRuns(int limit);

    /**
     * 返回运行。
     * @param runId 运行标识值
     * @return 处理结果
     */
    Map<String, Object> getRun(String runId);

    /**
     * 处理usage。
     * @return 处理结果
     */
    Map<String, Object> usage();
}
