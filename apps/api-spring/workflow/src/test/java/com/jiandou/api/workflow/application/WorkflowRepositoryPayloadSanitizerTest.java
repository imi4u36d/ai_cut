package com.jiandou.api.workflow.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jiandou.api.task.observability.StructuredApplicationLogger;
import com.jiandou.api.task.infrastructure.mybatis.RequestLogEntity;
import com.jiandou.api.task.infrastructure.mybatis.RequestLogMapper;
import com.jiandou.api.task.infrastructure.mybatis.TaskModelCallEntity;
import com.jiandou.api.task.infrastructure.mybatis.TaskModelCallMapper;
import com.jiandou.api.workflow.infrastructure.WorkflowJsonSupport;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WorkflowRepositoryPayloadSanitizerTest {

    @Test
    void saveModelCallSanitizesBase64InRequestAndResponsePayloads() {
        SqlSessionFactory sqlSessionFactory = mock(SqlSessionFactory.class);
        SqlSession session = mock(SqlSession.class);
        TaskModelCallMapper mapper = mock(TaskModelCallMapper.class);
        RequestLogMapper requestLogMapper = mock(RequestLogMapper.class);
        when(sqlSessionFactory.openSession(true)).thenReturn(session);
        when(session.getMapper(TaskModelCallMapper.class)).thenReturn(mapper);
        when(session.getMapper(RequestLogMapper.class)).thenReturn(requestLogMapper);
        WorkflowRepository repository = new WorkflowRepository(sqlSessionFactory);

        repository.saveModelCall("wf_1", Map.of(
            "modelCallId", "mdl_1",
            "requestPayload", Map.of(
                "providerRequest", Map.of("body", Map.of("prompt", "生成图片", "base64", "AQID"))
            ),
            "responsePayload", Map.of(
                "providerResponse", Map.of("data", List.of(Map.of("b64_json", "AQID")))
            ),
            "success", true
        ));

        ArgumentCaptor<TaskModelCallEntity> captor = ArgumentCaptor.forClass(TaskModelCallEntity.class);
        verify(mapper).insert(captor.capture());
        TaskModelCallEntity entity = captor.getValue();
        assertFalse(entity.getRequestPayloadJson().contains("AQID"));
        assertFalse(entity.getResponsePayloadJson().contains("AQID"));
        Map<String, Object> responsePayload = WorkflowJsonSupport.readMap(entity.getResponsePayloadJson());
        @SuppressWarnings("unchecked")
        Map<String, Object> providerResponse = (Map<String, Object>) responsePayload.get("providerResponse");
        @SuppressWarnings("unchecked")
        Map<String, Object> image = (Map<String, Object>) ((List<?>) providerResponse.get("data")).get(0);
        assertInstanceOf(Map.class, image.get("b64_json"));
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) image.get("b64_json");
        assertEquals(true, summary.get("redacted"));
        assertEquals(64, String.valueOf(summary.get("sha256")).length());
        verify(requestLogMapper).insert(any(RequestLogEntity.class));
    }

    @Test
    void saveModelCallUpdatesExistingRowsByStableId() {
        SqlSessionFactory sqlSessionFactory = mock(SqlSessionFactory.class);
        SqlSession session = mock(SqlSession.class);
        TaskModelCallMapper mapper = mock(TaskModelCallMapper.class);
        RequestLogMapper requestLogMapper = mock(RequestLogMapper.class);
        when(sqlSessionFactory.openSession(true)).thenReturn(session);
        when(session.getMapper(TaskModelCallMapper.class)).thenReturn(mapper);
        when(session.getMapper(RequestLogMapper.class)).thenReturn(requestLogMapper);
        when(mapper.selectOne(any())).thenReturn(new TaskModelCallEntity());
        when(requestLogMapper.selectOne(any())).thenReturn(new RequestLogEntity());
        WorkflowRepository repository = new WorkflowRepository(sqlSessionFactory);

        repository.saveModelCall("wf_1", Map.of(
            "modelCallId", "mdl_1",
            "requestLogId", "reqlog_mdl_1",
            "requestPayload", Map.of(),
            "responsePayload", Map.of(),
            "success", true
        ));

        verify(mapper, never()).insert(org.mockito.ArgumentMatchers.<TaskModelCallEntity>any());
        verify(mapper).update(org.mockito.ArgumentMatchers.<TaskModelCallEntity>any(), any());
        verify(requestLogMapper, never()).insert(org.mockito.ArgumentMatchers.<RequestLogEntity>any());
        verify(requestLogMapper).update(org.mockito.ArgumentMatchers.<RequestLogEntity>any(), any());
    }

    @Test
    void workflowApplicationLogEventSanitizesBase64Payload() {
        Map<String, Object> event = StructuredApplicationLogger.workflowEvent(
            "wf_1",
            "workflow",
            "keyframe",
            "workflow.keyframe.response",
            "INFO",
            "done",
            Map.of("providerResponse", Map.of("data", List.of(Map.of("b64_json", "AQID"))))
        );

        String eventJson = WorkflowJsonSupport.write(event);
        assertFalse(eventJson.contains("AQID"));
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) event.get("payload");
        @SuppressWarnings("unchecked")
        Map<String, Object> providerResponse = (Map<String, Object>) payload.get("providerResponse");
        @SuppressWarnings("unchecked")
        Map<String, Object> image = (Map<String, Object>) ((List<?>) providerResponse.get("data")).get(0);
        assertInstanceOf(Map.class, image.get("b64_json"));
    }
}
