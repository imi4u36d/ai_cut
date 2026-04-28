package com.jiandou.api.generation.image;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiandou.api.generation.RemoteImageGenerationResult;
import com.jiandou.api.generation.exception.GenerationProviderException;
import com.jiandou.api.generation.runtime.MediaProviderCapabilities;
import com.jiandou.api.generation.runtime.MediaProviderConfig;
import com.jiandou.api.generation.runtime.MediaProviderProfile;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleImageModelProviderTest {

    @Test
    void generateUsesResponsesImageGenerationWithoutReferenceImages() {
        CapturingTransport transport = new CapturingTransport();
        transport.jsonResponse = "{\"output\":[{\"type\":\"image_generation_call\",\"result\":\"AQID\"}]}";
        OpenAiCompatibleImageModelProvider provider = new OpenAiCompatibleImageModelProvider(transport);

        RemoteImageGenerationResult result = provider.generate(
            profile(),
            new ImageGenerationRequest("gpt-image-2", "a prompt", 1824, 1024, "", List.of(), null)
        );

        assertEquals("https://api.deeps.example/v1/responses", transport.jsonEndpoint);
        assertEquals("gpt-5.4", transport.jsonBody.get("model"));
        assertEquals("a prompt", transport.jsonBody.get("input"));
        assertEquals("required", transport.jsonBody.get("tool_choice"));
        Map<String, Object> tool = firstTool(transport.jsonBody);
        assertEquals("image_generation", tool.get("type"));
        assertEquals("generate", tool.get("action"));
        assertEquals("1824x1024", tool.get("size"));
        assertEquals("png", tool.get("output_format"));
        assertEquals("high", tool.get("quality"));
        assertArrayEquals(new byte[] {1, 2, 3}, result.data());
        assertEquals("", result.remoteSourceUrl());
        assertEquals("deeps_api", result.provider());
        assertEquals("gpt-image-2", result.providerModel());
    }

    @Test
    void generateUsesResponsesImageEditWithReferenceImages() {
        CapturingTransport transport = new CapturingTransport();
        transport.jsonResponse = "{\"output\":[{\"type\":\"image_generation_call\",\"result\":\"AQID\"}]}";
        OpenAiCompatibleImageModelProvider provider = new OpenAiCompatibleImageModelProvider(transport);

        RemoteImageGenerationResult result = provider.generate(
            profile(),
            new ImageGenerationRequest("gpt-image-2", "edit prompt", 1024, 1824, "", List.of("https://cdn.example.com/ref.png"), null)
        );

        assertEquals("https://api.deeps.example/v1/responses", transport.jsonEndpoint);
        Map<String, Object> tool = firstTool(transport.jsonBody);
        assertEquals("edit", tool.get("action"));
        assertEquals("1024x1824", tool.get("size"));
        List<?> input = (List<?>) transport.jsonBody.get("input");
        Map<?, ?> message = (Map<?, ?>) input.get(0);
        List<?> content = (List<?>) message.get("content");
        assertEquals("input_text", ((Map<?, ?>) content.get(0)).get("type"));
        assertEquals("input_image", ((Map<?, ?>) content.get(1)).get("type"));
        assertEquals("https://cdn.example.com/ref.png", ((Map<?, ?>) content.get(1)).get("image_url"));
        assertArrayEquals(new byte[] {1, 2, 3}, result.data());
    }

    @Test
    void generateMapsRequestedDimensionsToConfiguredSupportedSize() {
        CapturingTransport transport = new CapturingTransport();
        transport.jsonResponse = "{\"output\":[{\"type\":\"image_generation_call\",\"result\":\"AQID\"}]}";
        OpenAiCompatibleImageModelProvider provider = new OpenAiCompatibleImageModelProvider(transport);

        RemoteImageGenerationResult result = provider.generate(
            profileWithSupportedSizes(),
            new ImageGenerationRequest("gpt-image-2", "portrait prompt", 720, 1280, "", List.of(), null)
        );

        assertEquals("1024x1536", firstTool(transport.jsonBody).get("size"));
        assertEquals("1024x1536", result.requestedSize());
    }

    @Test
    void generateDecodesBase64ImageData() {
        CapturingTransport transport = new CapturingTransport();
        transport.jsonResponse = "{\"output\":[{\"type\":\"image_generation_call\",\"result\":\"AQID\"}]}";
        OpenAiCompatibleImageModelProvider provider = new OpenAiCompatibleImageModelProvider(transport);

        RemoteImageGenerationResult result = provider.generate(
            profile(),
            new ImageGenerationRequest("gpt-image-2", "a prompt", 1248, 1248, "", List.of(), null)
        );

        assertArrayEquals(new byte[] {1, 2, 3}, result.data());
        assertEquals("", result.remoteSourceUrl());
    }

    @Test
    void generateFailsWhenProviderReturnsNoImageData() {
        CapturingTransport transport = new CapturingTransport();
        transport.jsonResponse = "{\"output\":[{\"type\":\"message\",\"content\":[]}]}";
        OpenAiCompatibleImageModelProvider provider = new OpenAiCompatibleImageModelProvider(transport);

        GenerationProviderException exception = assertThrows(
            GenerationProviderException.class,
            () -> provider.generate(profile(), new ImageGenerationRequest("gpt-image-2", "a prompt", 1248, 1248, "", List.of(), null))
        );

        assertEquals("deeps image response did not include usable image data", exception.getMessage());
    }

    @Test
    void generateFailsWhenReferenceImageUrlIsNotAbsoluteHttpUrl() {
        OpenAiCompatibleImageModelProvider provider = new OpenAiCompatibleImageModelProvider(new CapturingTransport());

        GenerationProviderException exception = assertThrows(
            GenerationProviderException.class,
            () -> provider.generate(profile(), new ImageGenerationRequest("gpt-image-2", "a prompt", 1248, 1248, "/storage/ref.png", List.of(), null))
        );

        assertTrue(exception.getMessage().contains("absolute http(s) URL"));
    }

    private MediaProviderProfile profile() {
        return new MediaProviderProfile(
            new MediaProviderConfig(
                "image",
                "gpt-image-2",
                "deeps_api",
                "gpt-image-2",
                "k",
                "https://api.deeps.example/v1",
                "",
                60,
                "test"
            ),
            new MediaProviderCapabilities(false, false, false, false, 5, 120, "", List.of(), List.of())
        );
    }

    private MediaProviderProfile profileWithSupportedSizes() {
        return new MediaProviderProfile(
            new MediaProviderConfig(
                "image",
                "gpt-image-2",
                "deeps_api",
                "gpt-image-2",
                "k",
                "https://api.deeps.example/v1",
                "",
                60,
                "test"
            ),
            new MediaProviderCapabilities(
                false,
                false,
                false,
                false,
                5,
                120,
                "",
                List.of("1024x1024", "1024x1536", "1536x1024"),
                List.of()
            )
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstTool(Map<String, Object> body) {
        return (Map<String, Object>) ((List<?>) body.get("tools")).get(0);
    }

    private static final class CapturingTransport extends ImageProviderTransport {
        private String jsonEndpoint;
        private Map<String, Object> jsonBody = Map.of();
        private String jsonResponse = "{}";
        private String multipartEndpoint;
        private Map<String, String> multipartFields = Map.of();
        private List<ImageProviderTransport.MultipartFilePart> multipartFiles = List.of();
        private String multipartResponse = "{}";
        private final java.util.Map<String, DownloadedBinary> downloads = new java.util.LinkedHashMap<>();

        private CapturingTransport() {
            super(new ObjectMapper());
        }

        @Override
        public HttpResponse<String> sendJson(
            String endpoint,
            String apiKey,
            Map<String, Object> body,
            int timeoutSeconds,
            Map<String, String> extraHeaders
        ) {
            this.jsonEndpoint = endpoint;
            this.jsonBody = body;
            return new StubStringHttpResponse(jsonResponse);
        }

        @Override
        public HttpResponse<String> sendMultipart(
            String endpoint,
            String apiKey,
            Map<String, String> fields,
            List<ImageProviderTransport.MultipartFilePart> files,
            int timeoutSeconds,
            Map<String, Object> requestPayload
        ) {
            this.multipartEndpoint = endpoint;
            this.multipartFields = fields;
            this.multipartFiles = files;
            return new StubStringHttpResponse(multipartResponse);
        }

        @Override
        public DownloadedBinary downloadBinary(String url, int timeoutSeconds) {
            DownloadedBinary binary = downloads.get(url);
            if (binary == null) {
                throw new GenerationProviderException("remote media download failed: missing stub");
            }
            return binary;
        }
    }

    private static final class StubStringHttpResponse implements HttpResponse<String> {

        private final String body;

        private StubStringHttpResponse(String body) {
            this.body = body;
        }

        @Override
        public int statusCode() {
            return 200;
        }

        @Override
        public String body() {
            return body;
        }

        @Override
        public java.net.http.HttpRequest request() {
            return null;
        }

        @Override
        public java.util.Optional<HttpResponse<String>> previousResponse() {
            return java.util.Optional.empty();
        }

        @Override
        public java.net.http.HttpHeaders headers() {
            return java.net.http.HttpHeaders.of(Map.of(), (a, b) -> true);
        }

        @Override
        public java.net.URI uri() {
            return java.net.URI.create("https://example.com");
        }

        @Override
        public java.net.http.HttpClient.Version version() {
            return java.net.http.HttpClient.Version.HTTP_1_1;
        }

        @Override
        public java.util.Optional<javax.net.ssl.SSLSession> sslSession() {
            return java.util.Optional.empty();
        }
    }
}
