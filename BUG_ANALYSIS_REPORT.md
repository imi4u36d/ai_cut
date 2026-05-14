# JianDou-enterprise 项目 BUG 和问题分析报告

> 报告生成日期: 2026-05-04  
> 项目版本: v0.1.0

---

## 目录

- [一、后端 Java 问题](#一后端-java-问题)
- [二、前端 TypeScript/JavaScript 问题](#二前端-typescriptjavascript-问题)
- [三、数据库相关问题](#三数据库相关问题)
- [四、配置和部署问题](#四配置和部署问题)
- [五、代码质量建议](#五代码质量建议)
- [六、优先级建议](#六优先级建议)

---

## 一、后端 Java 问题

### 1. Thread.sleep 同步轮询机制（性能问题）

**严重程度**: 中  
**位置**: 
- [`TaskWorkerRenderStageService.java:1014`](apps/api-spring/task/src/main/java/com/jiandou/api/task/runtime/TaskWorkerRenderStageService.java#L1014-L1019)
- [`LocalMediaArtifactService.java:441`](apps/api-spring/media/src/main/java/com/jiandou/api/media/LocalMediaArtifactService.java#L441-L449)

**问题描述**:  
使用 `Thread.sleep()` 进行同步轮询会阻塞线程，在高并发场景下浪费线程资源，降低系统吞吐量。

**风险代码**:
```java
// TaskWorkerRenderStageService.java
private void sleepBeforeNextVideoPoll() {
    if (videoRunPollIntervalMillis <= 0L) {
        return;
    }
    try {
        Thread.sleep(videoRunPollIntervalMillis);  // 阻塞轮询
    } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("video run wait interrupted", ex);
    }
}
```

**影响**:
- 每个轮询线程在等待期间完全阻塞
- 当 worker-concurrency 设置较高时，线程数呈倍数增长
- 线程上下文切换开销增加

**修复建议**:  
使用异步非阻塞方式，推荐以下几种方案：
1. 使用 `CompletableFuture` + `thenAcceptAsync`
2. 使用反应式编程模型 (Project Reactor)
3. 使用调度器配合 `ScheduledExecutorService` 的延时任务

---

### 2. 文件上传路径遍历安全风险（严重安全问题）

**严重程度**: 高  
**位置**: [`DefaultUploadApplicationService.java:72`](apps/api-spring/media/src/main/java/com/jiandou/api/upload/application/DefaultUploadApplicationService.java#L72)

**问题描述**:  
虽然对文件名进行了正则过滤，但使用 `normalize()` 后未验证路径是否仍在允许的目录内，理论上仍存在路径遍历攻击风险。

**风险代码**:
```java
String storedName = assetId + "_" + originalName.replaceAll("[^A-Za-z0-9._-]+", "_");
Path target = uploadsDir.resolve(storedName).normalize();
file.transferTo(target);  // 未充分验证
```

**攻击场景示例**:
如果 `originalName` 包含特殊编码的路径遍历序列，可能绕过正则过滤。

**修复建议**:
```java
Path target = uploadsDir.resolve(storedName).normalize();
// 验证规范化后仍在允许的目录内
if (!target.startsWith(uploadsDir.toAbsolutePath().normalize())) {
    throw new SecurityException("Invalid file path: path traversal detected");
}
file.transferTo(target);
```

---

### 3. JoinOutputService 无限循环模式（潜在问题）

**严重程度**: 中  
**位置**: [`JoinOutputService.java:93-127`](apps/api-spring/task/src/main/java/com/jiandou/api/task/runtime/JoinOutputService.java#L93-L127)

**问题描述**:  
使用 `while(true)` 无限循环处理任务，虽然有退出条件但模式不够安全，且在单线程 Executor 上执行可能阻塞其他任务。

**风险代码**:
```java
private void processTask(String taskId) {
    try {
        while (true) {  // 无限循环
            Integer target = pendingTargets.remove(taskId);
            if (target == null || target < 1) {
                return;
            }
            try {
                buildJoinOutput(taskId, target);
            } catch (Exception ex) {
                log.warn("join output build failed: taskId={}, target={}", taskId, target, ex);
                // 异常处理后继续循环
            }
        }
    } finally {
        runningTasks.remove(taskId);
        if (pendingTargets.containsKey(taskId) && runningTasks.add(taskId)) {
            executor.submit(() -> processTask(taskId));  // 递归提交任务
        }
    }
}
```

**潜在问题**:
1. `while(true)` 模式在复杂业务逻辑下容易出现意外的死循环
2. finally 块中的递归提交可能导致栈溢出或任务堆积
3. 单线程 Executor 上执行可能阻塞其他 taskId 的处理

**修复建议**:  
使用基于队列的消费者模式：
```java
private final BlockingQueue<TaskItem> taskQueue = new LinkedBlockingQueue<>();

public void scheduleJoin(String taskId, int endClipIndex) {
    taskQueue.offer(new TaskItem(taskId, endClipIndex));
    executor.submit(this::processQueue);
}

private void processQueue() {
    TaskItem item;
    try {
        item = taskQueue.poll(10, TimeUnit.SECONDS);
        if (item != null) {
            processItem(item);
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

---

### 4. FFmpeg 调用阻塞问题（性能问题）

**严重程度**: 中  
**位置**: 
- [`LocalMediaArtifactService.java:198-203`](apps/api-spring/media/src/main/java/com/jiandou/api/media/LocalMediaArtifactService.java#L198-L203)
- [`LocalMediaArtifactService.java:440-449`](apps/api-spring/media/src/main/java/com/jiandou/api/media/LocalMediaArtifactService.java#L440-L449)
- [`LocalMediaArtifactService.java:752-757`](apps/api-spring/media/src/main/java/com/jiandou/api/media/LocalMediaArtifactService.java#L752-L757)

**问题描述**:  
`process.waitFor()` 会无限期阻塞线程，如果 FFmpeg 进程卡住（如视频文件损坏、参数错误等），将导致线程永久阻塞。

**风险代码**:
```java
Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
int exitCode = process.waitFor();  // 可能无限期阻塞
String processOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
```

**修复建议**:
```java
Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
if (!process.waitFor(5, TimeUnit.MINUTES)) {  // 添加超时
    process.destroyForcibly();
    throw new IOException("FFmpeg timeout after 5 minutes");
}
int exitCode = process.exitValue();
```

---

### 5. TaskRecord.executionContext null 检查逻辑问题（代码质量问题）

**严重程度**: 低  
**位置**: [`TaskRecord.java:344-349`](apps/api-spring/task/src/main/java/com/jiandou/api/task/TaskRecord.java#L344-L349)

**问题描述**:  
`executionContext` 字段已初始化为 `LinkedHashMap`，但 `mutableExecutionContext()` 中仍检查 `null`，此检查永不可能为 true，属于冗余代码。

**风险代码**:
```java
Map<String, Object> executionContext = new LinkedHashMap<>();  // 已初始化

public Map<String, Object> mutableExecutionContext() {
    if (executionContext == null) {  // 此条件永远不会为 true
        executionContext = new LinkedHashMap<>();
    }
    return executionContext;
}
```

**修复建议**:  
移除无用的 null 检查：
```java
public Map<String, Object> mutableExecutionContext() {
    return executionContext;  // 直接返回即可
}
```

---

### 6. 线程池命名不规范（调试问题）

**严重程度**: 低  
**位置**: 
- [`TaskWorkerRunner.java:82-86`](apps/api-spring/task/src/main/java/com/jiandou/api/task/runtime/TaskWorkerRunner.java#L82-L86)
- [`JoinOutputService.java:41-45`](apps/api-spring/task/src/main/java/com/jiandou/api/task/runtime/JoinOutputService.java#L41-L45)

**问题描述**:  
线程池使用相同的线程名前缀，在调试时难以区分不同的线程池。

**风险代码**:
```java
pollExecutor = Executors.newScheduledThreadPool(workerConcurrency, r -> {
    Thread thread = new Thread(r, "jiandou-spring-worker");  // 所有线程同名
    thread.setDaemon(true);
    return thread;
});
```

**修复建议**:
```java
pollExecutor = Executors.newScheduledThreadPool(workerConcurrency, r -> {
    Thread thread = new Thread(r, "jiandou-spring-worker-poll-" + UUID.randomUUID());
    thread.setDaemon(true);
    return thread;
});

maintenanceExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread thread = new Thread(r, "jiandou-spring-worker-maint");
    thread.setDaemon(true);
    return thread;
});
```

---

## 二、前端 TypeScript/JavaScript 问题

### 7. 轮询超时配置硬编码（可配置性问题）

**严重程度**: 低  
**位置**: [`generation.ts:25-26`](apps/web/src/api/generation.ts#L25-L26)

**问题描述**:  
轮询超时和间隔时间硬编码，无法根据网络环境或用户需求动态调整。

**风险代码**:
```typescript
const RUN_POLL_INTERVAL_MS = 1200;
const RUN_POLL_TIMEOUT_MS = 120000;  // 固定 2 分钟
```

**修复建议**:
```typescript
// 从环境变量读取配置
const RUN_POLL_INTERVAL_MS = parseInt(import.meta.env.VITE_POLL_INTERVAL || "1200");
const RUN_POLL_TIMEOUT_MS = parseInt(import.meta.env.VITE_POLL_TIMEOUT || "120000");

// 或者从运行时配置读取
const config = getRuntimeConfig();
const pollInterval = config.pollInterval || 1200;
```

---

### 8. 缺少指数退避机制（性能问题）

**严重程度**: 中  
**位置**: [`generation.ts:292-309`](apps/web/src/api/generation.ts#L292-L309)

**问题描述**:  
轮询使用固定间隔，当服务端压力大或网络不稳定时，固定间隔的轮询可能加剧拥塞。

**风险代码**:
```typescript
async function waitForRunResult(runId: string, initialRun?: unknown) {
    const startedAt = Date.now();
    let latestRun = initialRun;
    while (Date.now() - startedAt < RUN_POLL_TIMEOUT_MS) {
        if (latestRun && hasTerminalRunResult(latestRun)) {
            return latestRun;
        }
        // ... 状态检查
        await delay(RUN_POLL_INTERVAL_MS);  // 固定间隔
        latestRun = await getJson<unknown>(RUN_DETAILS_ENDPOINT(runId));
    }
    throw new Error("生成任务等待超时，请稍后在任务列表中查看结果");
}
```

**修复建议**:
```typescript
async function waitForRunResult(runId: string, initialRun?: unknown) {
    const startedAt = Date.now();
    let latestRun = initialRun;
    let retryCount = 0;
    const baseDelay = 500;  // 基础延迟 500ms
    
    while (Date.now() - startedAt < RUN_POLL_TIMEOUT_MS) {
        if (latestRun && hasTerminalRunResult(latestRun)) {
            return latestRun;
        }
        // ... 状态检查
        
        // 指数退避，最大延迟 5 秒
        const delayMs = Math.min(baseDelay * Math.pow(2, retryCount), 5000);
        await delay(delayMs);
        retryCount++;
        
        latestRun = await getJson<unknown>(RUN_DETAILS_ENDPOINT(runId));
    }
    throw new Error("生成任务等待超时，请稍后在任务列表中查看结果");
}
```

---

## 三、数据库相关问题

### 9. 数据库排序规则可能不一致

**严重程度**: 低  
**位置**: [`V1__init.sql`](apps/api-spring/api-boot/src/main/resources/db/migration/V1__init.sql)

**问题描述**:  
`biz_tasks` 表使用 `utf8mb4_general_ci` 排序规则，在某些场景（如需要区分大小写的查询）可能导致意外行为。

**风险代码**:
```sql
CREATE TABLE biz_tasks (
    ...
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

**影响**:
- `utf8mb4_general_ci` 是不区分大小写的排序规则
- 某些特殊字符的排序可能不符合预期
- 性能略低于 `utf8mb4_bin`

**修复建议**:
```sql
-- 根据业务需求选择
-- 如果需要区分大小写:
ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin

-- 如果需要正确的 Unicode 排序:
ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
```

---

### 10. 缺少外键约束

**严重程度**: 中  
**位置**: [`V1__init.sql`](apps/api-spring/api-boot/src/main/resources/db/migration/V1__init.sql)

**问题描述**:  
表之间缺少外键约束，可能导致数据不一致。

**风险代码**:
```sql
CREATE TABLE biz_task_attempts (
    id VARCHAR(64) PRIMARY KEY,
    task_id VARCHAR(64),  -- 没有外键约束
    ...
);
```

**修复建议**:
```sql
CREATE TABLE biz_task_attempts (
    id VARCHAR(64) PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL,
    ...
    CONSTRAINT fk_attempt_task FOREIGN KEY (task_id) REFERENCES biz_tasks(id) ON DELETE CASCADE
);

CREATE INDEX idx_attempt_task ON biz_task_attempts(task_id);
```

**需要添加外键的表**:
- `biz_task_attempts.task_id` → `biz_tasks.id`
- `biz_task_stage_runs.task_id` → `biz_tasks.id`
- `biz_task_model_calls.task_id` → `biz_tasks.id`
- `biz_task_materials.task_id` → `biz_tasks.id`
- `biz_task_outputs.task_id` → `biz_tasks.id`

---

## 四、配置和部署问题

### 11. Worker 并发配置较低

**严重程度**: 中  
**位置**: [`application.yml`](apps/api-spring/api-boot/src/main/resources/application.yml)

**问题描述**:  
默认 `worker-concurrency: 1` 在多核服务器上无法充分利用 CPU 资源。

**当前配置**:
```yaml
jiandou:
  task-ops:
    worker-concurrency: 1  # 默认值过低
    worker-poll-interval-millis: 1000
    worker-stale-timeout-seconds: 30
```

**修复建议**:
```yaml
# 提高默认值到 CPU 核心数
jiandou:
  task-ops:
    worker-concurrency: ${JIANDOU_WORKER_CONCURRENCY:4}  # 通过环境变量配置
    worker-poll-interval-millis: 1000
    worker-stale-timeout-seconds: 30
```

**建议**:
- 单机部署: 设置为 CPU 核心数
- Docker 容器部署: 设置为容器分配的 CPU 核心数
- 可通过 `JIANDOU_WORKER_CONCURRENCY` 环境变量动态配置

---

### 12. 开发脚本缺少 graceful shutdown

**严重程度**: 低  
**位置**: [`dev-spring.sh`](scripts/dev-spring.sh)

**问题描述**:  
开发脚本在退出时直接 kill 进程，可能导致数据库连接未正常关闭或文件未正确写入。

**当前代码**:
```bash
cleanup() {
  trap - EXIT INT TERM
  
  if [[ -n "${API_PID}" ]] && kill -0 "${API_PID}" 2>/dev/null; then
    kill "${API_PID}" 2>/dev/null || true  # 直接 kill
  fi
  # ...
}
```

**修复建议**:
```bash
cleanup() {
  trap - EXIT INT TERM
  
  if [[ -n "${API_PID}" ]] && kill -0 "${API_PID}" 2>/dev/null; then
    echo "正在优雅关闭后端进程 ${API_PID}..."
    kill -TERM "${API_PID}" 2>/dev/null || true
    wait "${API_PID}" 2>/dev/null || true
  fi
  
  if [[ -n "${WEB_PID}" ]] && kill -0 "${WEB_PID}" 2>/dev/null; then
    echo "正在优雅关闭前端进程 ${WEB_PID}..."
    kill -TERM "${WEB_PID}" 2>/dev/null || true
    wait "${WEB_PID}" 2>/dev/null || true
  fi
}
```

---

## 五、代码质量建议

### 13. 缺少单元测试

**严重程度**: 中  
**当前状态**: 未发现单元测试

**建议测试覆盖**:
1. **TaskWorkerRenderStageService**
   - 渲染阶段各方法的单元测试
   - 关键帧生成逻辑测试
   - 视频轮询超时测试

2. **JoinOutputService**
   - 拼接任务调度测试
   - 多片段拼接测试
   - 异常处理测试

3. **DefaultUploadApplicationService**
   - 文件上传测试
   - 文件名清洗测试
   - 路径安全测试

4. **LocalMediaArtifactService**
   - 视频帧提取测试
   - 图片缩略图生成测试
   - FFmpeg 命令构建测试

**测试框架推荐**:
- JUnit 5: 单元测试
- Mockito: Mock 依赖
- AssertJ: 更流畅的断言

---

### 14. 缺少集成测试

**严重程度**: 中  
**当前状态**: 未发现集成测试

**建议测试覆盖**:
1. **API 端点集成测试**
   - `/api/v3/generation/catalog` 端点测试
   - `/api/v3/generation/runs` 端点测试
   - 文件上传端点测试

2. **数据库集成测试**
   - 任务创建和查询测试
   - 事务回滚测试
   - 并发冲突测试

3. **端到端测试**
   - 完整的任务生命周期测试
   - 失败重试测试

**测试框架推荐**:
- Spring Boot Test: 集成测试
- Testcontainers: Dockerized MySQL 测试
- WireMock: Mock 外部服务

---

### 15. 错误处理不一致

**严重程度**: 低  
**位置**: 多处

**问题描述**:  
错误处理方式不一致，有些地方抛出 `IllegalStateException`，有些地方返回空字符串。

**示例**:
```java
// 方式1: 抛出异常
if (taskId == null || taskId.isBlank()) {
    throw new IllegalStateException("taskId is required");
}

// 方式2: 返回空
private String firstNonBlank(String... values) {
    for (String value : values) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
    }
    return "";  // 静默失败
}
```

**建议**:  
统一错误处理策略：
- 参数校验失败: 抛出 `IllegalArgumentException`
- 业务规则违反: 抛出自定义业务异常
- 外部调用失败: 抛出 `RuntimeException` 或自定义异常
- 可恢复的边界情况: 返回空值或默认值

---

## 六、优先级建议

| 优先级 | 问题 | 影响 | 修复复杂度 |
|--------|------|------|-----------|
| **P0** | 路径遍历安全风险 | 安全漏洞，可能导致服务器文件泄露 | 低 |
| **P1** | FFmpeg 调用阻塞 | 性能问题，可能导致服务不可用 | 低 |
| **P1** | 缺少外键约束 | 数据一致性风险 | 中 |
| **P2** | Thread.sleep 轮询 | 性能问题，高并发下影响明显 | 中 |
| **P2** | JoinOutputService 无限循环 | 潜在的死循环风险 | 中 |
| **P2** | 缺少指数退避机制 | 性能问题，可能加剧拥塞 | 低 |
| **P3** | Worker 并发配置较低 | 资源利用不充分 | 低 |
| **P3** | 线程池命名不规范 | 调试困难 | 低 |
| **P4** | 单元测试缺失 | 质量风险，难以维护 | 高 |
| **P4** | 集成测试缺失 | 质量风险 | 高 |
| **P5** | 开发脚本缺少 graceful shutdown | 优雅性问题 | 低 |
| **P5** | 轮询配置硬编码 | 可配置性问题 | 低 |
| **P5** | 代码冗余检查 | 代码质量问题 | 低 |

---

## 七、总结

项目整体结构清晰，采用分层架构设计，代码风格较为统一。主要问题集中在：

1. **安全性**: 文件上传路径遍历风险需要立即修复
2. **性能**: 同步轮询和 FFmpeg 阻塞是主要性能瓶颈
3. **质量**: 缺少测试覆盖，需要补充单元测试和集成测试
4. **健壮性**: 缺少外键约束和超时控制，可能导致数据不一致

建议按照优先级顺序逐步修复问题，优先处理 P0 和 P1 级别问题。

---

*本报告由代码静态分析生成，部分问题可能需要结合运行时行为进一步验证。*
