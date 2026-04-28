package com.jiandou.api.generation.image;

import com.jiandou.api.generation.RemoteImageGenerationResult;
import com.jiandou.api.generation.exception.GenerationConfigurationException;
import com.jiandou.api.generation.exception.GenerationProviderException;
import com.jiandou.api.generation.runtime.MediaProviderProfile;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * OpenAI Images 兼容协议的图片 provider。
 */
@Component
public class OpenAiCompatibleImageModelProvider implements ImageModelProvider {

    private static final String PROVIDER_DEEPS_API = "deeps_api";
    private static final String RESPONSES_PATH = "/responses";
    private static final String RESPONSES_IMAGE_MAIN_MODEL = "gpt-5.4";
    private static final String DEFAULT_GPT_IMAGE_QUALITY = "high";
    private static final String DEFAULT_OUTPUT_FORMAT = "png";

    private final ImageProviderTransport transport;

    public OpenAiCompatibleImageModelProvider(ImageProviderTransport transport) {
        this.transport = transport;
    }

    @Override
    public boolean supports(MediaProviderProfile profile) {
        return profile != null && PROVIDER_DEEPS_API.equalsIgnoreCase(profile.provider());
    }

    @Override
    public RemoteImageGenerationResult generate(MediaProviderProfile profile, ImageGenerationRequest request) {
        if (!profile.ready()) {
            throw new GenerationConfigurationException("image provider config missing api key or base url");
        }
        String providerModel = blankTo(profile.modelName(), request.requestedModel());
        String size = resolveImageSize(profile, request.width(), request.height());
        List<String> referenceImageUrls = normalizeReferenceImageUrls(request.referenceImageUrls(), request.referenceImageUrl());
        return generateWithResponses(profile, request, providerModel, size, referenceImageUrls);
    }

    private RemoteImageGenerationResult generateWithResponses(
        MediaProviderProfile profile,
        ImageGenerationRequest request,
        String providerModel,
        String size,
        List<String> referenceImageUrls
    ) {
        String endpoint = resolveResponsesEndpoint(profile.baseUrl());
        String action = referenceImageUrls.isEmpty() ? "generate" : "edit";
        Map<String, Object> body = buildResponsesRequestBody(providerModel, request.prompt(), size, action, referenceImageUrls);
        Map<String, Object> providerRequest = new LinkedHashMap<>();
        providerRequest.put("method", "POST");
        providerRequest.put("endpoint", endpoint);
        providerRequest.put("body", body);
        long startedAt = System.nanoTime();
        HttpResponse<String> response = transport.sendJson(endpoint, profile.apiKey(), body, profile.timeoutSeconds(), Map.of());
        int latencyMs = (int) ((System.nanoTime() - startedAt) / 1_000_000L);
        Map<String, Object> payload = transport.decode(response.body());
        return toResult(profile, request, providerModel, size, latencyMs, providerRequest, payload, response.statusCode());
    }

    Map<String, Object> buildResponsesRequestBody(
        String providerModel,
        String prompt,
        String size,
        String action,
        List<String> referenceImageUrls
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", RESPONSES_IMAGE_MAIN_MODEL);
        body.put("input", buildResponsesInput(prompt, referenceImageUrls));
        body.put("tools", List.of(Map.of(
            "type", "image_generation",
            "action", action,
            "size", size,
            "output_format", DEFAULT_OUTPUT_FORMAT,
            "quality", DEFAULT_GPT_IMAGE_QUALITY
        )));
        body.put("tool_choice", "required");
        return body;
    }

    private Object buildResponsesInput(String prompt, List<String> referenceImageUrls) {
        if (referenceImageUrls == null || referenceImageUrls.isEmpty()) {
            return prompt;
        }
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "input_text", "text", prompt));
        for (String referenceImageUrl : referenceImageUrls) {
            if (!isAbsoluteHttpUrl(referenceImageUrl)) {
                throw new GenerationProviderException("deeps image reference image must be an absolute http(s) URL: " + referenceImageUrl);
            }
            content.add(Map.of("type", "input_image", "image_url", referenceImageUrl, "detail", "high"));
        }
        return List.of(Map.of("role", "user", "content", content));
    }

    String resolveImageSize(MediaProviderProfile profile, int width, int height) {
        List<String> supportedSizes = profile == null ? List.of() : profile.supportedSizes();
        if (supportedSizes.isEmpty()) {
            return width + "x" + height;
        }
        double requestedAspect = height <= 0 ? 1.0 : Math.max(1, width) / (double) height;
        String best = "";
        double bestDistance = Double.MAX_VALUE;
        int bestArea = 0;
        for (String supportedSize : supportedSizes) {
            int[] parsed = parseSize(supportedSize);
            if (parsed[0] <= 0 || parsed[1] <= 0) {
                continue;
            }
            double aspect = parsed[0] / (double) parsed[1];
            double distance = Math.abs(aspect - requestedAspect);
            int area = parsed[0] * parsed[1];
            if (best.isBlank() || distance < bestDistance || (distance == bestDistance && area > bestArea)) {
                best = parsed[0] + "x" + parsed[1];
                bestDistance = distance;
                bestArea = area;
            }
        }
        return best.isBlank() ? width + "x" + height : best;
    }

    private int[] parseSize(String size) {
        String normalized = size == null ? "" : size.trim().toLowerCase(Locale.ROOT).replace('*', 'x');
        String[] parts = normalized.split("x");
        if (parts.length != 2) {
            return new int[] {0, 0};
        }
        try {
            return new int[] {Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
        } catch (NumberFormatException ex) {
            return new int[] {0, 0};
        }
    }

    String resolveResponsesEndpoint(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        if (normalized.isBlank()) {
            return "";
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.endsWith(RESPONSES_PATH)) {
            return normalized;
        }
        if (lower.endsWith("/images/generations")) {
            normalized = normalized.substring(0, normalized.length() - "/images/generations".length());
        } else if (lower.endsWith("/images/edits")) {
            normalized = normalized.substring(0, normalized.length() - "/images/edits".length());
        } else if (lower.endsWith(RESPONSES_PATH)) {
            normalized = normalized.substring(0, normalized.length() - RESPONSES_PATH.length());
        } else if (lower.endsWith("/chat/completions")) {
            normalized = normalized.substring(0, normalized.length() - "/chat/completions".length());
        }
        return normalized + RESPONSES_PATH;
    }

    private RemoteImageGenerationResult toResult(
        MediaProviderProfile profile,
        ImageGenerationRequest request,
        String providerModel,
        String size,
        int latencyMs,
        Map<String, Object> providerRequest,
        Map<String, Object> payload,
        int httpStatus
    ) {
        String sourceUrl = transport.extractFirstString(payload, "url", "image_url", "imageUrl", "file_url", "fileUrl");
        byte[] data;
        String mimeType = "image/png";
        if (!sourceUrl.isBlank()) {
            ImageProviderTransport.DownloadedBinary binary = transport.downloadBinary(sourceUrl, profile.timeoutSeconds());
            data = binary.data();
            mimeType = blankTo(binary.mimeType(), mimeType);
        } else {
            String b64 = transport.extractFirstString(payload, "result", "b64_json", "base64_data", "base64", "imageBase64");
            if (b64.isBlank()) {
                throw new GenerationProviderException(
                    "deeps image response did not include usable image data",
                    providerRequest,
                    payload,
                    httpStatus
                );
            }
            try {
                data = Base64.getDecoder().decode(stripDataUriPrefix(b64));
            } catch (IllegalArgumentException ex) {
                throw new GenerationProviderException(
                    "deeps image response returned invalid base64 image data",
                    providerRequest,
                    payload,
                    httpStatus
                );
            }
        }
        return new RemoteImageGenerationResult(
            data,
            mimeType,
            sourceUrl,
            profile.provider(),
            providerModel,
            profile.endpointHost(),
            request.width(),
            request.height(),
            size,
            latencyMs,
            providerRequest,
            payload,
            httpStatus
        );
    }

    private boolean isAbsoluteHttpUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) && uri.getHost() != null;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private List<String> normalizeReferenceImageUrls(List<String> referenceImageUrls, String referenceImageUrl) {
        List<String> normalized = new ArrayList<>();
        if (referenceImageUrls != null) {
            for (String value : referenceImageUrls) {
                if (value != null && !value.isBlank()) {
                    normalized.add(value.trim());
                }
            }
        }
        if (normalized.isEmpty() && referenceImageUrl != null && !referenceImageUrl.isBlank()) {
            normalized.add(referenceImageUrl.trim());
        }
        return normalized;
    }

    private String stripDataUriPrefix(String value) {
        int commaIndex = value == null ? -1 : value.indexOf(',');
        if (commaIndex > 0 && value.substring(0, commaIndex).contains(";base64")) {
            return value.substring(commaIndex + 1);
        }
        return value;
    }

    private String blankTo(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }
}
