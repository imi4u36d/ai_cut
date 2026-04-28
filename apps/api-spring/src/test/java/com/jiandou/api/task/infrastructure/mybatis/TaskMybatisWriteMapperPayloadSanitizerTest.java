package com.jiandou.api.task.infrastructure.mybatis;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TaskMybatisWriteMapperPayloadSanitizerTest {

    @Test
    void toModelCallEntitySanitizesBase64Payloads() {
        TaskMybatisWriteMapper mapper = new TaskMybatisWriteMapper();

        TaskModelCallEntity entity = mapper.toModelCallEntity("task_1", Map.of(
            "modelCallId", "mdl_1",
            "requestPayload", Map.of("providerRequest", Map.of("base64_data", "AQID")),
            "responsePayload", Map.of("providerResponse", Map.of("data", List.of(Map.of("b64_json", "AQID")))),
            "success", true
        ));

        assertFalse(entity.getRequestPayloadJson().contains("AQID"));
        assertFalse(entity.getResponsePayloadJson().contains("AQID"));
        Map<String, Object> responsePayload = MybatisJsonSupport.readMap(entity.getResponsePayloadJson());
        @SuppressWarnings("unchecked")
        Map<String, Object> providerResponse = (Map<String, Object>) responsePayload.get("providerResponse");
        @SuppressWarnings("unchecked")
        Map<String, Object> image = (Map<String, Object>) ((List<?>) providerResponse.get("data")).get(0);
        assertInstanceOf(Map.class, image.get("b64_json"));
    }
}
