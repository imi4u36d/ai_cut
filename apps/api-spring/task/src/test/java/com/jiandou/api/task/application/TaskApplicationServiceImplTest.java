package com.jiandou.api.task.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jiandou.api.auth.domain.UserRole;
import com.jiandou.api.auth.domain.UserStatus;
import com.jiandou.api.auth.infrastructure.mybatis.SysUserEntity;
import com.jiandou.api.auth.security.CurrentUserPrincipal;
import com.jiandou.api.common.exception.ApiException;
import com.jiandou.api.config.JiandouTaskDefaultsProperties;
import com.jiandou.api.generation.GenerationModelKinds;
import com.jiandou.api.generation.RemoteTaskQueryResult;
import com.jiandou.api.generation.runtime.MediaProviderProfile;
import com.jiandou.api.generation.runtime.ModelRuntimePropertiesResolver;
import com.jiandou.api.generation.video.VideoModelProvider;
import com.jiandou.api.generation.video.VideoModelProviderRegistry;
import com.jiandou.api.task.TaskRecord;
import com.jiandou.api.task.web.dto.CreateGenerationTaskRequest;
import com.jiandou.api.task.web.dto.GenerateCreativePromptRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class TaskApplicationServiceImplTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createGenerationTaskReturnsQueriedTaskView() {
        TaskQueryService queryService = mock(TaskQueryService.class);
        TaskCommandService commandService = mock(TaskCommandService.class);
        ModelRuntimePropertiesResolver modelResolver = mock(ModelRuntimePropertiesResolver.class);
        VideoModelProviderRegistry videoModelProviderRegistry = mock(VideoModelProviderRegistry.class);
        TaskApplicationServiceImpl service = new TaskApplicationServiceImpl(
            queryService,
            commandService,
            modelResolver,
            videoModelProviderRegistry,
            defaultsProperties()
        );
        CreateGenerationTaskRequest request = new CreateGenerationTaskRequest(
            "title",
            "prompt",
            "9:16",
            "text-model",
            "image-model",
            "video-model",
            "720p",
            7,
            5,
            1,
            5,
            5,
            "transcript",
            false
        );
        TaskRecord task = new TaskRecord();
        task.setId("task_123");
        Map<String, Object> detail = Map.of("id", "task_123", "status", "PENDING");

        when(commandService.createGenerationTask(request)).thenReturn(task);
        when(queryService.getTask("task_123")).thenReturn(detail);

        assertEquals(detail, service.createGenerationTask(request));
        verify(commandService).createGenerationTask(request);
        verify(queryService).getTask("task_123");
    }

    @Test
    void generateCreativePromptTrimsTitleAndUsesFallback() {
        authenticateUser(7L);
        TaskApplicationServiceImpl service = new TaskApplicationServiceImpl(
            mock(TaskQueryService.class),
            mock(TaskCommandService.class),
            mock(ModelRuntimePropertiesResolver.class),
            mock(VideoModelProviderRegistry.class),
            defaultsProperties()
        );

        Map<String, Object> trimmed = service.generateCreativePrompt(
            new GenerateCreativePromptRequest("  城市场景  ", null, null, null, null, null, null)
        );
        Map<String, Object> fallback = service.generateCreativePrompt(
            new GenerateCreativePromptRequest("   ", null, null, null, null, null, null)
        );

        assertTrue(String.valueOf(trimmed.get("prompt")).endsWith("城市场景"));
        assertTrue(String.valueOf(fallback.get("prompt")).endsWith("未命名任务"));
        assertEquals("catalog", trimmed.get("source"));
    }

    @Test
    void generateCreativePromptRequiresAuthenticatedUser() {
        TaskApplicationServiceImpl service = new TaskApplicationServiceImpl(
            mock(TaskQueryService.class),
            mock(TaskCommandService.class),
            mock(ModelRuntimePropertiesResolver.class),
            mock(VideoModelProviderRegistry.class),
            defaultsProperties()
        );

        ApiException ex = assertThrows(ApiException.class, () -> service.generateCreativePrompt(
            new GenerateCreativePromptRequest("城市", null, null, null, null, null, null)
        ));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.status());
        assertEquals("unauthorized", ex.code());
    }

    @Test
    void getSeedanceTaskResultNormalizesBlankValues() {
        authenticateUser(7L);
        TaskQueryService queryService = mock(TaskQueryService.class);
        TaskCommandService commandService = mock(TaskCommandService.class);
        ModelRuntimePropertiesResolver modelResolver = mock(ModelRuntimePropertiesResolver.class);
        VideoModelProviderRegistry videoModelProviderRegistry = mock(VideoModelProviderRegistry.class);
        TaskApplicationServiceImpl service = new TaskApplicationServiceImpl(
            queryService,
            commandService,
            modelResolver,
            videoModelProviderRegistry,
            defaultsProperties()
        );
        MediaProviderProfile profile = new MediaProviderProfile(
            "seedance",
            "seedance-query-v1",
            "secret",
            "https://video.example.com",
            "https://task.example.com",
            120,
            5,
            300,
            false,
            false,
            false,
            "yaml"
        );
        VideoModelProvider provider = mock(VideoModelProvider.class);

        when(modelResolver.resolveVideoProfile("seedance-query-v1", 7L)).thenReturn(profile);
        when(videoModelProviderRegistry.resolve(profile)).thenReturn(provider);
        when(provider.query(profile, "remote-task-1")).thenReturn(
            new RemoteTaskQueryResult("remote-task-1", "SUCCEEDED", "   ", " ", Map.of("providerStatus", "done"))
        );

        Map<String, Object> result = service.getSeedanceTaskResult("remote-task-1");

        assertEquals("remote-task-1", result.get("taskId"));
        assertEquals("SUCCEEDED", result.get("status"));
        assertNull(result.get("videoUrl"));
        assertNull(result.get("message"));
        assertEquals(Map.of("providerStatus", "done"), result.get("payload"));
    }

    @Test
    void getSeedanceTaskResultRequiresAuthenticatedUser() {
        TaskApplicationServiceImpl service = new TaskApplicationServiceImpl(
            mock(TaskQueryService.class),
            mock(TaskCommandService.class),
            mock(ModelRuntimePropertiesResolver.class),
            mock(VideoModelProviderRegistry.class),
            defaultsProperties()
        );

        ApiException ex = assertThrows(ApiException.class, () -> service.getSeedanceTaskResult("remote-task-1"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.status());
        assertEquals("unauthorized", ex.code());
    }

    @Test
    void adminOverviewReflectsModelReadiness() {
        TaskQueryService queryService = mock(TaskQueryService.class);
        TaskCommandService commandService = mock(TaskCommandService.class);
        ModelRuntimePropertiesResolver modelResolver = mock(ModelRuntimePropertiesResolver.class);
        VideoModelProviderRegistry videoModelProviderRegistry = mock(VideoModelProviderRegistry.class);
        TaskApplicationServiceImpl service = new TaskApplicationServiceImpl(
            queryService,
            commandService,
            modelResolver,
            videoModelProviderRegistry,
            defaultsProperties()
        );
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("counts", Map.of("totalTasks", 1));

        when(queryService.adminOverview()).thenReturn(overview);
        when(modelResolver.listModelsByKind(GenerationModelKinds.TEXT)).thenReturn(List.of(Map.of("name", "text")));
        when(modelResolver.listModelsByKind(GenerationModelKinds.IMAGE)).thenReturn(List.of(Map.of("name", "image")));
        when(modelResolver.listModelsByKind(GenerationModelKinds.VIDEO)).thenReturn(List.of());

        Map<String, Object> notReady = service.adminOverview();

        assertFalse((Boolean) notReady.get("modelReady"));
        assertNull(notReady.get("primaryModel"));
        assertNull(notReady.get("textModel"));

        when(modelResolver.listModelsByKind(GenerationModelKinds.VIDEO)).thenReturn(List.of(Map.of("name", "video")));

        Map<String, Object> ready = service.adminOverview();

        assertTrue((Boolean) ready.get("modelReady"));
    }

    @Test
    void adminTerminateUsesUnownedTaskAndReturnsAdminView() {
        TaskQueryService queryService = mock(TaskQueryService.class);
        TaskCommandService commandService = mock(TaskCommandService.class);
        TaskApplicationServiceImpl service = new TaskApplicationServiceImpl(
            queryService,
            commandService,
            mock(ModelRuntimePropertiesResolver.class),
            mock(VideoModelProviderRegistry.class),
            defaultsProperties()
        );
        TaskRecord task = new TaskRecord();
        task.setId("task_other_user");
        task.setOwnerUserId(99L);
        Map<String, Object> adminView = Map.of("id", "task_other_user", "ownerUserId", 99L);

        when(queryService.requireTask("task_other_user")).thenReturn(task);
        when(commandService.terminate(task)).thenReturn(task);
        when(queryService.adminGetTask("task_other_user")).thenReturn(adminView);

        assertEquals(adminView, service.adminTerminateTask("task_other_user"));
        verify(queryService).requireTask("task_other_user");
        verify(commandService).terminate(task);
        verify(queryService).adminGetTask("task_other_user");
    }

    private JiandouTaskDefaultsProperties defaultsProperties() {
        JiandouTaskDefaultsProperties properties = new JiandouTaskDefaultsProperties();
        properties.setPromptSource("catalog");
        properties.setSeedanceQueryModel("seedance-query-v1");
        return properties;
    }

    private void authenticateUser(Long userId) {
        SysUserEntity user = new SysUserEntity();
        user.setId(userId);
        user.setUsername("tester");
        user.setDisplayName("Tester");
        user.setRole(UserRole.USER.value());
        user.setStatus(UserStatus.ACTIVE.value());
        CurrentUserPrincipal principal = CurrentUserPrincipal.from(user);
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities())
        );
    }
}
