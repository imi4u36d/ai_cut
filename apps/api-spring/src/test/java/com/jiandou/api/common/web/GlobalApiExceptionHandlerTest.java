package com.jiandou.api.common.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jiandou.api.generation.exception.GenerationProviderException;
import com.jiandou.api.task.exception.TaskNotFoundException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalApiExceptionHandlerTest {

    private final GlobalApiExceptionHandler handler = new GlobalApiExceptionHandler();

    @Test
    void handleApiExceptionUsesBusinessStatusAndCode() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v2/tasks/task_1");

        var response = handler.handleApiException(new TaskNotFoundException("task_1"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("task_not_found", response.getBody().code());
        assertEquals(404, response.getBody().status());
        assertEquals("Not Found", response.getBody().error());
        assertEquals("task not found: task_1", response.getBody().message());
        assertEquals("/api/v2/tasks/task_1", response.getBody().path());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    void handleGenerationProviderExceptionReturnsExplicitProviderFailure() {
        MockHttpServletRequest request = new MockHttpServletRequest(
            "POST",
            "/api/v2/workflows/wf_1/clips/1002/keyframes/generate"
        );

        var response = handler.handleGenerationProviderException(new GenerationProviderException(
            "provider request failed: http 502 {\"error\":{\"message\":\"Upstream returned an invalid image payload\",\"type\":\"upstream_error\"}}",
            Map.of("body", Map.of("model", "gpt-5.4")),
            "{\"error\":{\"message\":\"Upstream returned an invalid image payload\",\"type\":\"upstream_error\"}}",
            502
        ), request);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("provider_request_failed", response.getBody().code());
        assertEquals(502, response.getBody().status());
        assertTrue(response.getBody().message().contains("invalid image payload"));
        assertEquals(502, response.getBody().details().get("providerHttpStatus"));
        assertTrue(String.valueOf(response.getBody().details().get("providerResponse")).contains("upstream_error"));
    }

    @Test
    void handleIllegalArgumentMapsToBadRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v2/uploads/texts");

        var response = handler.handleIllegalArgument(new IllegalArgumentException("bad input"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("bad_request", response.getBody().code());
        assertEquals(400, response.getBody().status());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals("bad input", response.getBody().message());
        assertEquals("/api/v2/uploads/texts", response.getBody().path());
        assertNotNull(response.getBody().timestamp());
    }
}
