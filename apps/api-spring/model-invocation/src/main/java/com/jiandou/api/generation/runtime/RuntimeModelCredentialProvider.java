package com.jiandou.api.generation.runtime;

import java.util.List;
import java.util.Map;

/**
 * Provides model API keys scoped to users without coupling runtime resolution to credential storage.
 */
public interface RuntimeModelCredentialProvider {

    default Map<String, String> findApiKeysByUserId(Long userId) {
        return Map.of();
    }

    default String findRuntimeApiKey(Long userId, List<String> providerKeys) {
        return "";
    }

    default void saveApiKeys(Long userId, Map<String, String> providerApiKeys) {
    }
}
