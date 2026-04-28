package com.jiandou.api.generation.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiandou.api.generation.runtime.ModelRuntimeProfile;
import com.jiandou.api.generation.TextModelResponse;
import com.jiandou.api.generation.runtime.TextProviderCapabilities;
import com.jiandou.api.generation.runtime.TextProviderConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OpenAiCompatibleTextModelProviderTest {

    @Test
    void generateTextUsesResponsesApiWhenProfileSupportsIt() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = (HttpResponse<String>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"id\":\"resp_1\",\"output_text\":\"OK\"}");
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        when(httpClient.send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        OpenAiCompatibleTextModelProvider provider = new OpenAiCompatibleTextModelProvider(
            new TextProviderTransport(new ObjectMapper(), httpClient),
            List.of(new ResponsesApiInvocationStrategy(), new ChatCompletionsInvocationStrategy())
        );

        TextModelResponse result = provider.generate(
            responsesProfile(),
            new TextCompletionInvocation("system", "user", 0.2, 128)
        );

        HttpRequest request = requestCaptor.getValue();
        assertEquals(URI.create("https://api.example.com/v1/responses"), request.uri());
        assertEquals("POST", request.method());
        assertEquals("OK", result.text());
        assertEquals(true, result.responsesApi());
        assertEquals(200, result.httpStatus());
        assertEquals("https://api.example.com/v1/responses", result.providerRequest().get("endpoint"));
        assertEquals("resp_1", result.providerResponse().get("id"));
        assertTrue(String.valueOf(result.providerRequest().get("body")).contains("gpt-text"));
    }

    @Test
    void generateTextKeepsThinkingForDashscopeQwenResponsesApi() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = (HttpResponse<String>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"id\":\"resp_1\",\"output_text\":\"OK\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        OpenAiCompatibleTextModelProvider provider = new OpenAiCompatibleTextModelProvider(
            new TextProviderTransport(new ObjectMapper(), httpClient),
            List.of(new ResponsesApiInvocationStrategy(), new ChatCompletionsInvocationStrategy())
        );

        TextModelResponse result = provider.generate(
            qwenResponsesProfile(),
            new TextCompletionInvocation("system", "user", 0.2, 128)
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) result.providerRequest().get("body");
        @SuppressWarnings("unchecked")
        Map<String, Object> reasoning = (Map<String, Object>) body.get("reasoning");
        assertEquals("medium", reasoning.get("effort"));
        assertFalse(body.containsKey("enable_thinking"));
    }

    @Test
    void generateTextDoesNotAddQwenThinkingFlagsForOtherProviders() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = (HttpResponse<String>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"id\":\"resp_1\",\"output_text\":\"OK\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        OpenAiCompatibleTextModelProvider provider = new OpenAiCompatibleTextModelProvider(
            new TextProviderTransport(new ObjectMapper(), httpClient),
            List.of(new ResponsesApiInvocationStrategy(), new ChatCompletionsInvocationStrategy())
        );

        TextModelResponse result = provider.generate(
            responsesProfile(),
            new TextCompletionInvocation("system", "user", 0.2, 128)
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) result.providerRequest().get("body");
        assertFalse(body.containsKey("reasoning"));
        assertFalse(body.containsKey("enable_thinking"));
    }

    private ModelRuntimeProfile responsesProfile() {
        return new ModelRuntimeProfile(
            new TextProviderConfig(
                "text",
                "gpt-text",
                "openai",
                "gpt-text",
                "k",
                "https://api.example.com/v1",
                60,
                0.2,
                2048,
                "test"
            ),
            new TextProviderCapabilities(false, true)
        );
    }

    private ModelRuntimeProfile qwenResponsesProfile() {
        return new ModelRuntimeProfile(
            new TextProviderConfig(
                "text",
                "qwen3.6-flash",
                "qwen",
                "qwen3.6-flash-2026-04-16",
                "k",
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                60,
                0.2,
                2048,
                "test"
            ),
            new TextProviderCapabilities(false, true)
        );
    }

}
