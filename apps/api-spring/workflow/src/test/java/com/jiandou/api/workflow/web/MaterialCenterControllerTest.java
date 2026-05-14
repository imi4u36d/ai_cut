package com.jiandou.api.workflow.web;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jiandou.api.auth.security.CurrentUserPrincipal;
import com.jiandou.api.task.infrastructure.mybatis.MaterialAssetEntity;
import com.jiandou.api.task.application.TaskApplicationService;
import com.jiandou.api.workflow.application.WorkflowRepository;
import com.jiandou.api.workflow.web.dto.CreateMaterialGenerationRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class MaterialCenterControllerTest {

    private WorkflowRepository workflowRepository;

    @BeforeEach
    void setUp() {
        workflowRepository = mock(WorkflowRepository.class);
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                new CurrentUserPrincipal(88L, "tester", "Tester", "USER", "ACTIVE"),
                null,
                List.of()
            )
        );
        when(workflowRepository.findMaterialAssetsByIds(Set.of("asset_1"), 88L)).thenReturn(Map.of());
        when(workflowRepository.findMaterialAssetsByIds(Set.of(), 88L)).thenReturn(Map.of());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createMaterialGenerationDelegatesToTaskGeneration() {
        TaskApplicationService service = mock(TaskApplicationService.class);
        MaterialCenterController controller = new MaterialCenterController(service, workflowRepository);
        Map<String, Object> task = Map.of("id", "task_1", "taskType", "image_to_image");
        when(service.createGenerationTask(argThat(request ->
            "image_to_image".equals(request.taskType())
                && "free".equals(request.assetType())
                && "Ref image".equals(request.title())
                && "description".equals(request.creativePrompt())
                && "1:1".equals(request.aspectRatio())
                && "1024x1024".equals(request.imageSize())
                && "txt".equals(request.textAnalysisModel())
                && "img".equals(request.imageModel())
                && request.videoModel() == null
                && Integer.valueOf(42).equals(request.seed())
                && Integer.valueOf(1).equals(request.outputCount())
                && request.referenceImageUrls().equals(List.of("https://example.com/ref.png"))
                && request.referenceAssetIds().equals(List.of("asset_1"))
        ))).thenReturn(task);

        Map<String, Object> result = controller.createMaterialGeneration(new CreateMaterialGenerationRequest(
            "free",
            " Ref image ",
            " description ",
            List.of("cinematic"),
            List.of("https://example.com/ref.png"),
            List.of("asset_1"),
            "1:1",
            "1024x1024",
            "txt",
            "img",
            42
        ));

        assertSame(task, result);
        verify(service).createGenerationTask(argThat(request -> "image_to_image".equals(request.taskType())));
    }

    @Test
    void createCharacterSheetGenerationUsesCharacterSheetTaskType() {
        TaskApplicationService service = mock(TaskApplicationService.class);
        MaterialCenterController controller = new MaterialCenterController(service, workflowRepository);
        Map<String, Object> task = Map.of("id", "task_2", "taskType", "character_sheet");
        when(service.createGenerationTask(argThat(request ->
            "character_sheet".equals(request.taskType())
                && "character_sheet".equals(request.assetType())
                && "角色三视图".equals(request.title())
        ))).thenReturn(task);

        Map<String, Object> result = controller.createMaterialGeneration(new CreateMaterialGenerationRequest(
            "character_sheet",
            "",
            "角色描述",
            List.of(),
            List.of(),
            List.of(),
            null,
            null,
            "txt",
            "img",
            null
        ));

        assertSame(task, result);
        verify(service).createGenerationTask(argThat(request -> "character_sheet".equals(request.taskType())));
    }

    @Test
    void createMaterialGenerationResolvesReferenceAssetUrlsForTaskRequest() {
        TaskApplicationService service = mock(TaskApplicationService.class);
        MaterialCenterController controller = new MaterialCenterController(service, workflowRepository);
        MaterialAssetEntity asset = new MaterialAssetEntity();
        asset.setMaterialAssetId("asset_1");
        asset.setRemoteUrl("https://cdn.example.com/remote.png");
        asset.setPublicUrl("/storage/local.png");
        when(workflowRepository.findMaterialAssetsByIds(Set.of("asset_1"), 88L)).thenReturn(Map.of("asset_1", asset));
        Map<String, Object> task = Map.of("id", "task_3", "taskType", "image_to_image");
        when(service.createGenerationTask(argThat(request ->
            "image_to_image".equals(request.taskType())
                && request.referenceImageUrls().equals(List.of("https://example.com/ref.png", "https://cdn.example.com/remote.png"))
                && request.referenceAssetIds().equals(List.of("asset_1"))
        ))).thenReturn(task);

        Map<String, Object> result = controller.createMaterialGeneration(new CreateMaterialGenerationRequest(
            "free",
            "Ref image",
            "description",
            List.of(),
            List.of("https://example.com/ref.png"),
            List.of("asset_1"),
            "1:1",
            "1024x1024",
            "txt",
            "img",
            null
        ));

        assertSame(task, result);
        verify(workflowRepository).findMaterialAssetsByIds(Set.of("asset_1"), 88L);
    }
}
