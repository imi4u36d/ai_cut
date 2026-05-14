package com.jiandou.api.generation.image;

import com.jiandou.api.generation.exception.GenerationConfigurationException;
import com.jiandou.api.generation.exception.GenerationProviderException;
import com.jiandou.api.generation.runtime.MediaProviderProfile;
import com.jiandou.api.generation.RemoteImageGenerationResult;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Seedream 图片 provider。
 */
@Component
public class SeedreamImageModelProvider implements ImageModelProvider {

    private final ImageProviderTransport transport;

    public SeedreamImageModelProvider(ImageProviderTransport transport) {
        this.transport = transport;
    }

    @Override
    public boolean supports(MediaProviderProfile profile) {
        String provider = profile == null ? "" : profile.provider();
        return provider.toLowerCase(Locale.ROOT).contains("seedream");
    }

    @Override
    public RemoteImageGenerationResult generate(MediaProviderProfile profile, ImageGenerationRequest request) {
        if (!profile.ready()) {
            throw new GenerationConfigurationException("image provider config missing api key or base url");
        }
        String providerModel = blankTo(profile.modelName(), request.requestedModel());
        String size = seedreamSize(providerModel, request.width(), request.height());
        List<String> referenceImageUrls = normalizeReferenceImageUrls(request.referenceImageUrls(), request.referenceImageUrl());
        Map<String, Object> requestBody = buildSeedreamImageRequestBody(providerModel, request.prompt(), size, referenceImageUrls, request.seed());
        HttpResponse<String> response = transport.sendJson(
            profile.baseUrl(),
            profile.apiKey(),
            requestBody,
            profile.timeoutSeconds(),
            Map.of("X-Api-Key", profile.apiKey())
        );
        Map<String, Object> payload = transport.decode(response.body());
        Map<String, Object> providerRequest = new LinkedHashMap<>();
        providerRequest.put("method", "POST");
        providerRequest.put("endpoint", profile.baseUrl());
        providerRequest.put("body", requestBody);
        String sourceUrl = transport.extractFirstString(payload, "url", "image_url", "imageUrl", "file_url", "fileUrl");
        byte[] data;
        String mimeType = "image/png";
        if (!sourceUrl.isBlank()) {
            ImageProviderTransport.DownloadedBinary binary = transport.downloadBinary(sourceUrl, profile.timeoutSeconds());
            data = binary.data();
            mimeType = binary.mimeType().isBlank() ? mimeType : binary.mimeType();
        } else {
            String b64 = transport.extractFirstString(payload, "b64_json", "base64_data", "base64", "imageBase64");
            if (b64.isBlank()) {
                throw new GenerationProviderException(
                    "seedream response did not include usable image data",
                    providerRequest,
                    payload,
                    response.statusCode()
                );
            }
            try {
                data = Base64.getDecoder().decode(b64);
            } catch (IllegalArgumentException ex) {
                throw new GenerationProviderException(
                    "seedream response returned invalid base64 image data",
                    providerRequest,
                    payload,
                    response.statusCode()
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
            0,
            providerRequest,
            payload,
            response.statusCode()
        );
    }

    Map<String, Object> buildSeedreamImageRequestBody(String providerModel, String prompt, String size, List<String> referenceImageUrls, Integer seed) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", providerModel);
        body.put("prompt", prompt);
        body.put("sequential_image_generation", referenceImageUrls.isEmpty() ? "disabled" : "auto");
        body.put("response_format", "url");
        body.put("size", size);
        body.put("stream", false);
        body.put("watermark", false);
        if (!referenceImageUrls.isEmpty()) {
            body.put("image", referenceImageUrls.size() == 1 ? referenceImageUrls.get(0) : referenceImageUrls);
        }
        if (seed != null) {
            body.put("seed", seed);
        }
        return body;
    }

    String seedreamSize(String modelName, int width, int height) {
        String normalizedModel = modelName == null ? "" : modelName.trim().toLowerCase(Locale.ROOT);
        if ("doubao-seedream-4-5-251128".equals(normalizedModel)) {
            return "2K";
        }
        return "1K";
    }

    private String blankTo(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
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
}
