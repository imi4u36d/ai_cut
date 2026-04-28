package com.jiandou.api.generation.observability;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Provider观测Payload脱敏工具。
 */
public final class ProviderPayloadSanitizer {

    private static final int LARGE_BASE64_THRESHOLD = 512;

    private ProviderPayloadSanitizer() {
    }

    /**
     * 递归脱敏Provider payload中的图片base64内容。
     * @param value 待处理的payload
     * @return 脱敏后的payload副本
     */
    public static Object sanitize(Object value) {
        return sanitize("", value);
    }

    private static Object sanitize(String key, Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String childKey = entry.getKey() == null ? "" : String.valueOf(entry.getKey());
                sanitized.put(childKey, sanitize(childKey, entry.getValue()));
            }
            return sanitized;
        }
        if (value instanceof List<?> list) {
            List<Object> sanitized = new ArrayList<>(list.size());
            for (Object item : list) {
                sanitized.add(sanitize("", item));
            }
            return sanitized;
        }
        if (value instanceof String text && shouldRedact(key, text)) {
            return redactedSummary(text);
        }
        return value;
    }

    private static boolean shouldRedact(String key, String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            return false;
        }
        if (isBase64ImageKey(key)) {
            return true;
        }
        if (trimmed.regionMatches(true, 0, "data:", 0, 5) && trimmed.toLowerCase(Locale.ROOT).contains(";base64,")) {
            return true;
        }
        return trimmed.length() >= LARGE_BASE64_THRESHOLD && looksLikeBase64(trimmed);
    }

    private static boolean isBase64ImageKey(String key) {
        String normalized = key == null ? "" : key.trim().replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        return "b64json".equals(normalized)
            || "base64data".equals(normalized)
            || "base64".equals(normalized)
            || "imagebase64".equals(normalized);
    }

    private static boolean looksLikeBase64(String value) {
        String compact = value.replaceAll("\\s+", "");
        if (compact.length() < LARGE_BASE64_THRESHOLD) {
            return false;
        }
        int base64Chars = 0;
        for (int index = 0; index < compact.length(); index++) {
            char ch = compact.charAt(index);
            if ((ch >= 'A' && ch <= 'Z')
                || (ch >= 'a' && ch <= 'z')
                || (ch >= '0' && ch <= '9')
                || ch == '+'
                || ch == '/'
                || ch == '='
                || ch == '-'
                || ch == '_') {
                base64Chars++;
            }
        }
        return base64Chars >= compact.length() * 0.95;
    }

    private static Map<String, Object> redactedSummary(String value) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("redacted", true);
        summary.put("type", "base64_image");
        summary.put("length", value == null ? 0 : value.length());
        summary.put("sha256", sha256(value == null ? "" : value));
        summary.put("mimeType", inferMimeType(value));
        return summary;
    }

    private static String inferMimeType(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.regionMatches(true, 0, "data:", 0, 5)) {
            int semicolon = normalized.indexOf(';');
            if (semicolon > 5) {
                return normalized.substring(5, semicolon);
            }
        }
        return "image/*";
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
