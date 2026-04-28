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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * OpenAI Images 兼容协议的图片 provider。
 */
@Component
public class OpenAiCompatibleImageModelProvider implements ImageModelProvider {

    private static final String PROVIDER_DEEPS_API = "deeps_api";
    private static final String IMAGE_GENERATIONS_PATH = "/images/generations";
    private static final String IMAGE_EDITS_PATH = "/images/edits";
    private static final String DEFAULT_OUTPUT_FORMAT = "png";
    private static final String DEFAULT_MODERATION = "auto";
    private static final Pattern DATA_URI_PATTERN = Pattern.compile("^data:([^;,]+);base64,(.*)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

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
        return referenceImageUrls.isEmpty()
            ? generateWithImagesApi(profile, request, providerModel, size)
            : editWithImagesApi(profile, request, providerModel, size, referenceImageUrls);
    }

    private RemoteImageGenerationResult generateWithImagesApi(
        MediaProviderProfile profile,
        ImageGenerationRequest request,
        String providerModel,
        String size
    ) {
        String endpoint = resolveImagesEndpoint(profile.baseUrl(), IMAGE_GENERATIONS_PATH);
        Map<String, Object> body = buildImageGenerationRequestBody(providerModel, request.prompt(), size);
        Map<String, Object> providerRequest = new LinkedHashMap<>();
        providerRequest.put("method", "POST");
        providerRequest.put("endpoint", endpoint);
        providerRequest.put("url", endpoint);
        providerRequest.put("body", body);
        long startedAt = System.nanoTime();
        HttpResponse<String> response = transport.sendJson(endpoint, profile.apiKey(), body, profile.timeoutSeconds(), Map.of());
        int latencyMs = (int) ((System.nanoTime() - startedAt) / 1_000_000L);
        Map<String, Object> payload = transport.decode(response.body());
        return toResult(profile, request, providerModel, size, latencyMs, providerRequest, payload, response.statusCode());
    }

    private RemoteImageGenerationResult editWithImagesApi(
        MediaProviderProfile profile,
        ImageGenerationRequest request,
        String providerModel,
        String size,
        List<String> referenceImageUrls
    ) {
        String endpoint = resolveImagesEndpoint(profile.baseUrl(), IMAGE_EDITS_PATH);
        Map<String, String> fields = buildImageEditFields(providerModel, request.prompt(), size);
        List<ImageProviderTransport.MultipartFilePart> files = buildReferenceImageParts(referenceImageUrls, profile.timeoutSeconds());
        Map<String, Object> providerRequest = new LinkedHashMap<>();
        providerRequest.put("method", "POST");
        providerRequest.put("endpoint", endpoint);
        providerRequest.put("url", endpoint);
        providerRequest.put("fields", fields);
        providerRequest.put("files", files.stream().map(this::filePartSummary).toList());
        long startedAt = System.nanoTime();
        HttpResponse<String> response = transport.sendMultipart(endpoint, profile.apiKey(), fields, files, profile.timeoutSeconds(), providerRequest);
        int latencyMs = (int) ((System.nanoTime() - startedAt) / 1_000_000L);
        Map<String, Object> payload = transport.decode(response.body());
        return toResult(profile, request, providerModel, size, latencyMs, providerRequest, payload, response.statusCode());
    }

    Map<String, Object> buildImageGenerationRequestBody(String providerModel, String prompt, String size) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", providerModel);
        body.put("prompt", prompt);
        body.put("size", size);
        body.put("output_format", DEFAULT_OUTPUT_FORMAT);
        body.put("moderation", DEFAULT_MODERATION);
        return body;
    }

    private Map<String, String> buildImageEditFields(String providerModel, String prompt, String size) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("model", providerModel);
        fields.put("prompt", prompt);
        fields.put("size", size);
        fields.put("output_format", DEFAULT_OUTPUT_FORMAT);
        fields.put("moderation", DEFAULT_MODERATION);
        return fields;
    }

    String resolveImageSize(MediaProviderProfile profile, int width, int height) {
        List<String> supportedSizes = profile == null ? List.of() : profile.supportedSizes();
        if (supportedSizes.isEmpty()) {
            return width + "x" + height;
        }
        String requestedSize = Math.max(1, width) + "x" + Math.max(1, height);
        for (String supportedSize : supportedSizes) {
            int[] parsed = parseSize(supportedSize);
            if (parsed[0] > 0 && parsed[1] > 0 && requestedSize.equals(parsed[0] + "x" + parsed[1])) {
                return requestedSize;
            }
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

    String resolveImagesEndpoint(String baseUrl, String imagePath) {
        String normalized = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        if (normalized.isBlank()) {
            return "";
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.endsWith(imagePath)) {
            return normalized;
        }
        if (lower.endsWith("/images/generations")) {
            normalized = normalized.substring(0, normalized.length() - "/images/generations".length());
        } else if (lower.endsWith("/images/edits")) {
            normalized = normalized.substring(0, normalized.length() - "/images/edits".length());
        } else if (lower.endsWith("/responses")) {
            normalized = normalized.substring(0, normalized.length() - "/responses".length());
        } else if (lower.endsWith("/chat/completions")) {
            normalized = normalized.substring(0, normalized.length() - "/chat/completions".length());
        }
        return normalized + imagePath;
    }

    private List<ImageProviderTransport.MultipartFilePart> buildReferenceImageParts(List<String> referenceImageUrls, int timeoutSeconds) {
        List<ImageProviderTransport.MultipartFilePart> files = new ArrayList<>();
        int index = 1;
        for (String referenceImageUrl : referenceImageUrls) {
            String normalized = referenceImageUrl == null ? "" : referenceImageUrl.trim();
            if (isImageDataUri(normalized)) {
                files.add(dataUriToFilePart(normalized, index++));
                continue;
            }
            if (isAbsoluteHttpUrl(normalized)) {
                ImageProviderTransport.DownloadedBinary binary = transport.downloadBinary(normalized, timeoutSeconds);
                String mimeType = blankTo(binary.mimeType(), "image/png");
                files.add(new ImageProviderTransport.MultipartFilePart("image[]", "input-" + index++ + fileExt(mimeType), mimeType, binary.data()));
                continue;
            }
            throw new GenerationProviderException(
                "deeps image reference image must be an absolute http(s) URL or image data URI: "
                    + compactReferenceImageUrl(normalized)
            );
        }
        return files;
    }

    private ImageProviderTransport.MultipartFilePart dataUriToFilePart(String dataUri, int index) {
        Matcher matcher = DATA_URI_PATTERN.matcher(dataUri.trim());
        if (!matcher.matches()) {
            throw new GenerationProviderException("deeps image reference image data URI is invalid: " + compactReferenceImageUrl(dataUri));
        }
        String mimeType = blankTo(matcher.group(1), "image/png").toLowerCase(Locale.ROOT);
        try {
            byte[] data = Base64.getDecoder().decode(matcher.group(2).replaceAll("\\s+", ""));
            return new ImageProviderTransport.MultipartFilePart("image[]", "input-" + index + fileExt(mimeType), mimeType, data);
        } catch (IllegalArgumentException ex) {
            throw new GenerationProviderException("deeps image reference image data URI contains invalid base64");
        }
    }

    private Map<String, Object> filePartSummary(ImageProviderTransport.MultipartFilePart file) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("fieldName", file.fieldName());
        summary.put("fileName", file.fileName());
        summary.put("contentType", file.contentType());
        summary.put("sizeBytes", file.data() == null ? 0 : file.data().length);
        return summary;
    }

    private String fileExt(String mimeType) {
        String normalized = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        if (normalized.contains("jpeg") || normalized.contains("jpg")) {
            return ".jpg";
        }
        if (normalized.contains("webp")) {
            return ".webp";
        }
        if (normalized.contains("gif")) {
            return ".gif";
        }
        return ".png";
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

    private boolean isImageDataUri(String url) {
        String normalized = url == null ? "" : url.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("data:image/") && normalized.contains(";base64,");
    }

    private String compactReferenceImageUrl(String url) {
        String normalized = url == null ? "" : url.trim();
        if (isImageDataUri(normalized)) {
            return "data:image/...;base64,...";
        }
        if (normalized.length() <= 160) {
            return normalized;
        }
        return normalized.substring(0, 120) + "...";
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
