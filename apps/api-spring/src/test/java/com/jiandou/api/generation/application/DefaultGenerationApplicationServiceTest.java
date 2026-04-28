package com.jiandou.api.generation.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiandou.api.config.JiandouStorageProperties;
import com.jiandou.api.generation.GenerationRunKinds;
import com.jiandou.api.generation.GenerationRunStatuses;
import com.jiandou.api.generation.exception.GenerationRunNotFoundException;
import com.jiandou.api.generation.exception.UnsupportedGenerationKindException;
import com.jiandou.api.generation.orchestration.GenerationRunFactory;
import com.jiandou.api.generation.orchestration.GenerationRunSupport;
import com.jiandou.api.generation.orchestration.LocalGenerationRunStore;
import com.jiandou.api.generation.runtime.ModelRuntimePropertiesResolver;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class DefaultGenerationApplicationServiceTest {

    @TempDir
    Path tempDir;

    private LocalGenerationRunStore runStore;
    private ModelRuntimePropertiesResolver modelResolver;
    private GenerationCatalogService catalogService;
    private GenerationRunFactory runFactory;
    private GenerationRunSupport support;
    private DefaultGenerationApplicationService service;

    @BeforeEach
    void setUp() {
        JiandouStorageProperties storageProperties = new JiandouStorageProperties();
        storageProperties.setRootDir(tempDir.toString());
        runStore = new LocalGenerationRunStore(storageProperties, new ObjectMapper());
        modelResolver = mock(ModelRuntimePropertiesResolver.class);
        catalogService = mock(GenerationCatalogService.class);
        runFactory = mock(GenerationRunFactory.class);
        support = mock(GenerationRunSupport.class);
        service = new DefaultGenerationApplicationService(runStore, modelResolver, catalogService, runFactory, support);
    }

    @Test
    void catalogDelegatesToCatalogService() {
        Map<String, Object> payload = Map.of("catalog", true);
        when(catalogService.catalog()).thenReturn(payload);

        assertSame(payload, service.catalog());
    }

    @Test
    void createRunRoutesByKindAndPersistsResult() {
        Map<String, Object> request = Map.of("kind", GenerationRunKinds.SCRIPT, "prompt", "hello");
        Map<String, Object> run = new LinkedHashMap<>(Map.of("status", "queued"));
        when(runFactory.createScriptRun(any(), eq(request))).thenReturn(run);

        Map<String, Object> result = service.createRun(request);

        assertSame(run, result);
        ArgumentCaptor<String> runIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(runFactory).createScriptRun(runIdCaptor.capture(), eq(request));
        assertTrue(runIdCaptor.getValue().startsWith("run_"));
        assertEquals(run, runStore.loadRun(runIdCaptor.getValue()));
    }

    @Test
    void createRunRoutesStoryboardAdjustmentKind() {
        Map<String, Object> request = Map.of("kind", GenerationRunKinds.SCRIPT_ADJUST, "prompt", "hello");
        Map<String, Object> run = new LinkedHashMap<>(Map.of("status", "queued"));
        when(runFactory.createScriptAdjustRun(any(), eq(request))).thenReturn(run);

        Map<String, Object> result = service.createRun(request);

        assertSame(run, result);
        verify(runFactory).createScriptAdjustRun(any(), eq(request));
    }

    @Test
    void createRunRejectsUnsupportedKind() {
        UnsupportedGenerationKindException ex = assertThrows(
            UnsupportedGenerationKindException.class,
            () -> service.createRun(Map.of("kind", "unknown"))
        );

        assertTrue(ex.getMessage().contains("unknown"));
    }

    @Test
    void createAsyncRunReturnsAcceptedBeforeBackgroundExecutionPersistsResult() throws Exception {
        Map<String, Object> request = Map.of("kind", GenerationRunKinds.SCRIPT, "prompt", "hello");
        Map<String, Object> completed = new LinkedHashMap<>(Map.of("status", GenerationRunStatuses.SUCCEEDED));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(support.callLog(any(), any(), any(), any(), any())).thenReturn(Map.of("event", "run.accepted"));
        when(support.runEnvelope(any(), eq(GenerationRunKinds.SCRIPT), eq(request), any(), eq("resultScript"), eq(GenerationRunStatuses.ACCEPTED)))
            .thenAnswer(invocation -> {
                Map<String, Object> run = new LinkedHashMap<>();
                run.put("id", invocation.getArgument(0));
                run.put("kind", GenerationRunKinds.SCRIPT);
                run.put("status", GenerationRunStatuses.ACCEPTED);
                run.put("result", invocation.getArgument(3));
                return run;
            });
        when(runFactory.createScriptRun(any(), eq(request))).thenAnswer(invocation -> {
            started.countDown();
            if (!release.await(1, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test did not release async run");
            }
            return completed;
        });

        Map<String, Object> accepted = service.createAsyncRun(request);

        assertEquals(GenerationRunStatuses.ACCEPTED, accepted.get("status"));
        String runId = String.valueOf(accepted.get("id"));
        assertTrue(runId.startsWith("run_"));
        assertTrue(started.await(1, TimeUnit.SECONDS));
        assertEquals(accepted, runStore.loadRun(runId));
        release.countDown();
        verify(runFactory, timeout(1000)).createScriptRun(eq(runId), eq(request));
        assertEquals(completed, runStore.loadRun(runId));
    }

    @Test
    void listRunsDelegatesToStore() {
        Map<String, Object> run = new LinkedHashMap<>(Map.of(
            "id", "run_1",
            "status", "completed",
            "updatedAt", "2026-04-16T00:00:00Z"
        ));
        runStore.persistRun("run_1", run);

        assertEquals(List.of(run), service.listRuns(20));
    }

    @Test
    void getRunLoadsFromStoreRefreshesAndPersists() {
        Map<String, Object> stored = new LinkedHashMap<>(Map.of("id", "run_2", "status", "running"));
        Map<String, Object> refreshed = new LinkedHashMap<>(Map.of("id", "run_2", "status", "completed"));
        runStore.persistRun("run_2", stored);
        when(runFactory.refreshVideoRun(any())).thenReturn(refreshed);

        Map<String, Object> result = service.getRun("run_2");

        assertSame(refreshed, result);
        verify(runFactory).refreshVideoRun(any());
        assertEquals(refreshed, runStore.loadRun("run_2"));
    }

    @Test
    void getRunThrowsWhenMissing() {
        assertThrows(GenerationRunNotFoundException.class, () -> service.getRun("missing"));
    }

    @Test
    void usageBuildsUsageRowsFromTextModels() {
        when(modelResolver.listModelsByKind("text")).thenReturn(List.of(
            Map.of("value", "gpt-4.1", "label", "GPT 4.1", "provider", "openai"),
            Map.of("value", "glm", "provider", "zhipu")
        ));
        when(modelResolver.configSource()).thenReturn("dir:/workspace/config");
        when(support.nowIso()).thenReturn("2026-04-16T00:00:00Z");

        Map<String, Object> usage = service.usage();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) usage.get("items");
        assertEquals(2, items.size());
        assertEquals("gpt-4.1", items.get(0).get("model"));
        assertEquals("GPT 4.1", items.get(0).get("label"));
        assertEquals("glm", items.get(1).get("model"));
        assertEquals("glm", items.get(1).get("label"));
        assertEquals("dir:/workspace/config", items.get(0).get("source"));
        assertEquals("2026-04-16T00:00:00Z", usage.get("generatedAt"));
        assertEquals("2026-04-16T00:00:00Z", usage.get("updatedAt"));
    }
}
