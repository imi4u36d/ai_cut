package com.jiandou.api.generation.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProviderPayloadSanitizerTest {

    @Test
    void sanitizesNestedImageBase64Fields() {
        Map<String, Object> payload = Map.of(
            "providerResponse", Map.of(
                "id", "resp_1",
                "data", List.of(Map.of(
                    "url", "https://cdn.example.com/image.png",
                    "b64_json", "AQID"
                ))
            )
        );

        Object sanitized = ProviderPayloadSanitizer.sanitize(payload);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) ((Map<String, Object>) sanitized).get("providerResponse");
        @SuppressWarnings("unchecked")
        Map<String, Object> image = (Map<String, Object>) ((List<?>) response.get("data")).get(0);
        assertEquals("resp_1", response.get("id"));
        assertEquals("https://cdn.example.com/image.png", image.get("url"));
        assertInstanceOf(Map.class, image.get("b64_json"));
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) image.get("b64_json");
        assertEquals(true, summary.get("redacted"));
        assertEquals("base64_image", summary.get("type"));
        assertEquals(4, summary.get("length"));
        assertEquals(64, String.valueOf(summary.get("sha256")).length());
        assertFalse(String.valueOf(sanitized).contains("AQID"));
    }

    @Test
    void sanitizesLargeBase64LikeStringsAndKeepsOrdinaryText() {
        String largeBase64 = "A".repeat(600);
        Map<String, Object> payload = Map.of(
            "prompt", "普通提示词 https://cdn.example.com/image.png",
            "providerResponse", Map.of("encrypted_content", largeBase64)
        );

        Object sanitized = ProviderPayloadSanitizer.sanitize(payload);

        assertTrue(String.valueOf(sanitized).contains("普通提示词"));
        assertTrue(String.valueOf(sanitized).contains("https://cdn.example.com/image.png"));
        assertFalse(String.valueOf(sanitized).contains(largeBase64));
        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) ((Map<String, Object>) sanitized).get("providerResponse");
        assertInstanceOf(Map.class, response.get("encrypted_content"));
    }

    @Test
    void infersMimeTypeFromDataUri() {
        String dataUri = "data:image/webp;base64,AQID";

        Object sanitized = ProviderPayloadSanitizer.sanitize(Map.of("imageBase64", dataUri));

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) ((Map<String, Object>) sanitized).get("imageBase64");
        assertEquals("image/webp", summary.get("mimeType"));
        assertEquals(dataUri.length(), summary.get("length"));
    }
}
