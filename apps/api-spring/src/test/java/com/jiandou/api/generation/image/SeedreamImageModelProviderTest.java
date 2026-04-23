package com.jiandou.api.generation.image;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import com.jiandou.api.generation.runtime.MediaProviderCapabilities;
import com.jiandou.api.generation.runtime.MediaProviderConfig;
import com.jiandou.api.generation.runtime.MediaProviderProfile;
import com.jiandou.api.generation.RemoteImageGenerationResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SeedreamImageModelProviderTest {

    @Test
    void buildRequestBodyIncludesSeedAndDisablesWatermark() {
        SeedreamImageModelProvider provider = new SeedreamImageModelProvider(new ImageProviderTransport(new com.fasterxml.jackson.databind.ObjectMapper()));

        Map<String, Object> body = provider.buildSeedreamImageRequestBody(
            "doubao-seedream-5-0-260128",
            "a prompt",
            "1K",
            java.util.List.of(),
            42
        );

        assertEquals(42, body.get("seed"));
        assertFalse((Boolean) body.get("watermark"));
    }

    @Test
    void generateDownloadsRemoteBinaryWhenProviderReturnsUrl() {
        ImageProviderTransport transport = new ImageProviderTransport(new com.fasterxml.jackson.databind.ObjectMapper()) {
            @Override
            public java.net.http.HttpResponse<String> sendJson(
                String endpoint,
                String apiKey,
                Map<String, Object> body,
                int timeoutSeconds,
                Map<String, String> extraHeaders
            ) {
                return new StubStringHttpResponse("{\"data\":[{\"url\":\"https://example.com/image.png\"}]}");
            }

            @Override
            public DownloadedBinary downloadBinary(String url, int timeoutSeconds) {
                return new DownloadedBinary(new byte[] {1, 2, 3}, "image/png");
            }
        };
        SeedreamImageModelProvider provider = new SeedreamImageModelProvider(transport);

        RemoteImageGenerationResult result = provider.generate(
            profile(),
            new ImageGenerationRequest("seedream-4.5", "a prompt", 720, 1280, "", java.util.List.of(), null)
        );

        assertArrayEquals(new byte[] {1, 2, 3}, result.data());
        assertEquals("image/png", result.mimeType());
        assertEquals("https://example.com/image.png", result.remoteSourceUrl());
        assertEquals("doubao-seedream-4-5-251128", result.providerModel());
        assertEquals("2K", result.requestedSize());
        assertEquals(200, result.httpStatus());
        assertEquals("https://api.example.com/v1/images", result.providerRequest().get("endpoint"));
        assertEquals("https://example.com/image.png", result.providerResponse().toString().contains("https://example.com/image.png") ? "https://example.com/image.png" : "");
    }

    @Test
    void buildRequestBodyIncludesReferenceImagesWhenReferenceImagePresent() {
        SeedreamImageModelProvider provider = new SeedreamImageModelProvider(new ImageProviderTransport(new com.fasterxml.jackson.databind.ObjectMapper()));

        Map<String, Object> body = provider.buildSeedreamImageRequestBody(
            "doubao-seedream-4-5-251128",
            "a prompt",
            "2K",
            java.util.List.of("https://example.com/clip1-first.png"),
            null
        );

        assertEquals("https://example.com/clip1-first.png", body.get("image"));
        assertEquals("auto", body.get("sequential_image_generation"));
    }

    @Test
    void buildRequestBodyDisablesSequentialGenerationWithoutReferenceImage() {
        SeedreamImageModelProvider provider = new SeedreamImageModelProvider(new ImageProviderTransport(new com.fasterxml.jackson.databind.ObjectMapper()));

        Map<String, Object> body = provider.buildSeedreamImageRequestBody(
            "doubao-seedream-4-5-251128",
            "a prompt",
            "2K",
            java.util.List.of(),
            null
        );

        assertEquals("disabled", body.get("sequential_image_generation"));
    }

    @Test
    void buildRequestBodyIncludesMultipleReferenceImages() {
        SeedreamImageModelProvider provider = new SeedreamImageModelProvider(new ImageProviderTransport(new com.fasterxml.jackson.databind.ObjectMapper()));

        Map<String, Object> body = provider.buildSeedreamImageRequestBody(
            "doubao-seedream-4-5-251128",
            "a prompt",
            "2K",
            java.util.List.of("https://example.com/character-1.png", "https://example.com/character-2.png"),
            null
        );

        @SuppressWarnings("unchecked")
        java.util.List<String> referenceImages = (java.util.List<String>) body.get("image");
        assertIterableEquals(java.util.List.of("https://example.com/character-1.png", "https://example.com/character-2.png"), referenceImages);
        assertEquals("auto", body.get("sequential_image_generation"));
    }

    private MediaProviderProfile profile() {
        return new MediaProviderProfile(
            new MediaProviderConfig(
                "image",
                "seedream-4.5",
                "seedream",
                "doubao-seedream-4-5-251128",
                "k",
                "https://api.example.com/v1/images",
                "",
                60,
                "test"
            ),
            new MediaProviderCapabilities(false, false, false, false, 5, 120, "", List.of(), List.of())
        );
    }

    private static final class StubStringHttpResponse implements java.net.http.HttpResponse<String> {

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
        public java.util.Optional<java.net.http.HttpResponse<String>> previousResponse() {
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
