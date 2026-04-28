package com.jiandou.api.task.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jiandou.api.generation.runtime.ModelRuntimePropertiesResolver;
import com.jiandou.api.task.TaskRecord;
import com.jiandou.api.task.domain.GenerationRequestSnapshot;
import com.jiandou.api.task.persistence.TaskRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 任务执行运行时相关测试。
 */
class TaskExecutionRuntimeSupportTest {

    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final ModelRuntimePropertiesResolver modelResolver = mock(ModelRuntimePropertiesResolver.class);
    private final TaskExecutionRuntimeSupport runtimeSupport = new TaskExecutionRuntimeSupport(taskRepository, modelResolver);

    /**
     * 处理解析时长SecondsDefaults转为TenSeconds。
     */
    @Test
    void resolveDurationSecondsDefaultsToTenSeconds() {
        TaskRecord task = new TaskRecord();
        when(modelResolver.intValue("catalog.defaults", "video_duration_seconds", 10)).thenReturn(10);
        assertEquals(10, runtimeSupport.resolveDurationSeconds(task));
    }

    /**
     * 构建视频运行请求EnablesGenerateAudioBy默认。
     */
    @Test
    void buildVideoRunRequestEnablesGenerateAudioByDefault() {
        TaskRecord task = new TaskRecord();
        task.setRequestSnapshot(GenerationRequestSnapshot.fromMap(Map.of(
            "textAnalysisModel", "gpt-text",
            "videoModel", "video-1"
        )));
        when(modelResolver.value("catalog.defaults", "video_generate_audio", "true")).thenReturn("true");

        Map<String, Object> request = runtimeSupport.buildVideoRunRequest(
            task,
            1,
            "prompt",
            "720*1280",
            10,
            8,
            12,
            "https://example.com/first.png",
            "https://example.com/last.png"
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> input = (Map<String, Object>) request.get("input");
        assertEquals(10, input.get("durationSeconds"));
        assertEquals(8, input.get("minDurationSeconds"));
        assertEquals(12, input.get("maxDurationSeconds"));
        assertEquals("https://example.com/first.png", input.get("firstFrameUrl"));
        assertEquals("https://example.com/last.png", input.get("lastFrameUrl"));
        assertTrue((Boolean) input.get("generateAudio"));
    }

    @Test
    void buildImageRunRequestDerivesStableSeedWhenTaskSeedMissing() {
        TaskRecord task = new TaskRecord();
        task.setId("task_seedless");
        task.setTitle("seedless-demo");
        task.setRequestSnapshot(GenerationRequestSnapshot.fromMap(Map.of(
            "textAnalysisModel", "gpt-text",
            "imageModel", "image-1"
        )));

        Map<String, Object> firstRequest = runtimeSupport.buildImageRunRequest(
            task,
            1,
            "prompt-1",
            720,
            1280,
            ""
        );
        Map<String, Object> secondRequest = runtimeSupport.buildImageRunRequest(
            task,
            1,
            "prompt-2",
            720,
            1280,
            "https://example.com/reference.png"
        );
        Map<String, Object> thirdRequest = runtimeSupport.buildImageRunRequest(
            task,
            2,
            "prompt-3",
            720,
            1280,
            ""
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> firstInput = (Map<String, Object>) firstRequest.get("input");
        @SuppressWarnings("unchecked")
        Map<String, Object> secondInput = (Map<String, Object>) secondRequest.get("input");
        @SuppressWarnings("unchecked")
        Map<String, Object> thirdInput = (Map<String, Object>) thirdRequest.get("input");
        assertTrue(((Integer) firstInput.get("seed")) > 0);
        assertEquals(firstInput.get("seed"), secondInput.get("seed"));
        assertNotEquals(firstInput.get("seed"), thirdInput.get("seed"));
    }
}
