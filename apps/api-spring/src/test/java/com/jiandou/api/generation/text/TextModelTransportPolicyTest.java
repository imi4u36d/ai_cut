package com.jiandou.api.generation.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jiandou.api.generation.runtime.ModelRuntimeProfile;
import com.jiandou.api.generation.runtime.TextProviderCapabilities;
import com.jiandou.api.generation.runtime.TextProviderConfig;
import org.junit.jupiter.api.Test;

class TextModelTransportPolicyTest {

    @Test
    void capabilityChecksReflectProfileFlags() {
        ModelRuntimeProfile profile = new ModelRuntimeProfile(
            new TextProviderConfig("text", "gpt-4.1", "openai", "gpt-4.1", "key", "https://api.example.com/v1", 30, 0.2, 1000, "cfg"),
            new TextProviderCapabilities(true, true, true)
        );

        assertTrue(TextModelTransportPolicy.supportsResponsesApi(profile));
        assertTrue(TextModelTransportPolicy.prefersChatCompletionsForVision(profile));
        assertFalse(TextModelTransportPolicy.supportsResponsesApi(null));
        assertFalse(TextModelTransportPolicy.prefersChatCompletionsForVision(null));
    }

    @Test
    void resolveEndpointAppendsOrPreservesExpectedSuffix() {
        assertEquals(
            "https://api.example.com/v1/responses",
            TextModelTransportPolicy.resolveEndpoint("https://api.example.com/v1", true)
        );
        assertEquals(
            "https://api.example.com/v1/responses",
            TextModelTransportPolicy.resolveEndpoint("https://api.example.com/v1/responses/", true)
        );
        assertEquals(
            "https://api.example.com/v1/chat/completions",
            TextModelTransportPolicy.resolveEndpoint("https://api.example.com/v1", false)
        );
        assertEquals(
            "https://api.example.com/v1/chat/completions",
            TextModelTransportPolicy.resolveEndpoint("https://api.example.com/v1/chat/completions/", false)
        );
        assertEquals(
            "https://dashscope.aliyuncs.com/compatible-mode/v1/responses",
            TextModelTransportPolicy.resolveEndpoint("https://dashscope.aliyuncs.com/compatible-mode/v1", true)
        );
        assertEquals(
            "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
            TextModelTransportPolicy.resolveEndpoint("https://dashscope.aliyuncs.com/api/v2/apps/protocols/compatible-mode/v1", false)
        );
        assertEquals(
            "https://dashscope.aliyuncs.com/compatible-mode/v1/responses",
            TextModelTransportPolicy.resolveEndpoint("https://dashscope.aliyuncs.com/api/v2/apps/protocols/compatible-mode/v1", true)
        );
    }
}
