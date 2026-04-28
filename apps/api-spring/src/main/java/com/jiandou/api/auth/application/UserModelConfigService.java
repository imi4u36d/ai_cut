package com.jiandou.api.auth.application;

import com.jiandou.api.admin.dto.AdminModelConfigKeyUpdateRequest;
import com.jiandou.api.admin.dto.AdminModelConfigResponse;
import com.jiandou.api.admin.dto.AdminModelConfigValidationResponse;
import com.jiandou.api.auth.infrastructure.mybatis.MybatisUserModelCredentialRepository;
import com.jiandou.api.generation.GenerationModelKinds;
import com.jiandou.api.generation.runtime.MediaProviderProfile;
import com.jiandou.api.generation.runtime.ModelRuntimeProfile;
import com.jiandou.api.generation.runtime.ModelRuntimePropertiesResolver;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 当前登录用户自己的模型配置服务。
 */
@Service
public class UserModelConfigService {

    private static final List<String> KIND_ORDER = List.of(
        GenerationModelKinds.TEXT,
        GenerationModelKinds.IMAGE,
        GenerationModelKinds.VIDEO
    );
    private static final String MISSING_API_KEY_ISSUE = "缺少 api_key";

    private final ModelRuntimePropertiesResolver modelResolver;
    private final MybatisUserModelCredentialRepository userModelCredentialRepository;

    public UserModelConfigService(
        ModelRuntimePropertiesResolver modelResolver,
        MybatisUserModelCredentialRepository userModelCredentialRepository
    ) {
        this.modelResolver = modelResolver;
        this.userModelCredentialRepository = userModelCredentialRepository;
    }

    /**
     * 返回当前用户模型配置快照。
     * @param userId 用户ID
     * @return 处理结果
     */
    public AdminModelConfigResponse read(Long userId) {
        Map<String, String> apiKeys = userModelCredentialRepository.findApiKeysByUserId(userId);
        List<AdminModelConfigResponse.ModelItem> models = new ArrayList<>();
        models.addAll(readTextModels(GenerationModelKinds.TEXT, userId));
        models.addAll(readMediaModels(GenerationModelKinds.IMAGE, userId));
        models.addAll(readMediaModels(GenerationModelKinds.VIDEO, userId));
        models.sort(Comparator
            .comparingInt((AdminModelConfigResponse.ModelItem item) -> kindIndex(item.kind()))
            .thenComparing(AdminModelConfigResponse.ModelItem::name, String.CASE_INSENSITIVE_ORDER));
        List<AdminModelConfigResponse.ProviderItem> providers = readProviders(models, apiKeys, userId);
        return new AdminModelConfigResponse(
            "user-db",
            buildSummary(models, providers),
            readDefaults(),
            providers,
            List.copyOf(models),
            modelResolver.configErrors()
        );
    }

    /**
     * 校验当前用户的 key 草稿。
     * @param userId 用户ID
     * @param request 请求体
     * @return 处理结果
     */
    public AdminModelConfigValidationResponse validateKeys(Long userId, AdminModelConfigKeyUpdateRequest request) {
        AdminModelConfigResponse current = read(userId);
        AdminModelConfigResponse snapshot = applyApiKeyOverrides(current, collectApiKeyUpdates(request, current.providers()));
        boolean valid = snapshot.configErrors().isEmpty() && snapshot.models().stream().allMatch(AdminModelConfigResponse.ModelItem::ready);
        return new AdminModelConfigValidationResponse(valid, snapshot);
    }

    /**
     * 保存当前用户的 key 配置。
     * @param userId 用户ID
     * @param request 请求体
     * @return 处理结果
     */
    public AdminModelConfigResponse saveKeys(Long userId, AdminModelConfigKeyUpdateRequest request) {
        AdminModelConfigResponse current = read(userId);
        ApiKeyUpdateBatch updates = collectApiKeyUpdates(request, current.providers());
        if (!updates.errors().isEmpty()) {
            throw new IllegalArgumentException(String.join(" / ", updates.errors()));
        }
        if (!updates.apiKeys().isEmpty()) {
            userModelCredentialRepository.saveApiKeys(userId, updates.apiKeys());
        }
        return read(userId);
    }

    private AdminModelConfigResponse.Defaults readDefaults() {
        return new AdminModelConfigResponse.Defaults(
            modelResolver.value("pipeline", "default_aspect_ratio", "9:16"),
            modelResolver.value("catalog.defaults", "style_preset", "cinematic"),
            modelResolver.value("catalog.defaults", "image_size", "1024x1024"),
            modelResolver.value("catalog.defaults", "video_size", "720*1280"),
            modelResolver.intValue("catalog.defaults", "video_duration_seconds", 8),
            modelResolver.intValue("model", "timeout_seconds", 120),
            doubleValue(modelResolver.value("model", "temperature", "0.15"), 0.15),
            modelResolver.intValue("model", "max_tokens", 2000)
        );
    }

    private List<AdminModelConfigResponse.ModelItem> readTextModels(String kind, Long userId) {
        List<AdminModelConfigResponse.ModelItem> items = new ArrayList<>();
        for (Map<String, Object> item : modelResolver.listModelsByKind(kind)) {
            String name = stringValue(item.get("value"));
            ModelRuntimeProfile profile = modelResolver.resolveTextProfile(name, userId);
            List<String> issues = new ArrayList<>();
            if (profile.apiKey().isBlank()) {
                issues.add(MISSING_API_KEY_ISSUE);
            }
            if (profile.baseUrl().isBlank()) {
                issues.add("缺少 base_url");
            }
            items.add(new AdminModelConfigResponse.ModelItem(
                name,
                firstNonBlank(stringValue(item.get("label")), name),
                kind,
                profile.provider(),
                stringValue(item.get("vendor")),
                stringValue(item.get("family")),
                stringValue(item.get("description")),
                booleanValue(item.get("supportsSeed")),
                booleanValue(item.get("supportsResponsesApi")),
                "",
                List.of(),
                List.of(),
                issues.isEmpty() && profile.ready(),
                profile.source(),
                profile.endpointHost(),
                "",
                List.copyOf(issues)
            ));
        }
        return items;
    }

    private List<AdminModelConfigResponse.ModelItem> readMediaModels(String kind, Long userId) {
        List<AdminModelConfigResponse.ModelItem> items = new ArrayList<>();
        for (Map<String, Object> item : modelResolver.listModelsByKind(kind)) {
            String name = stringValue(item.get("value"));
            MediaProviderProfile profile = modelResolver.resolveMediaProfile(name, kind, userId);
            List<String> issues = new ArrayList<>();
            if (profile.apiKey().isBlank()) {
                issues.add(MISSING_API_KEY_ISSUE);
            }
            if (profile.baseUrl().isBlank()) {
                issues.add("缺少 base_url");
            }
            if (GenerationModelKinds.VIDEO.equals(kind) && profile.taskBaseUrl().isBlank()) {
                issues.add("缺少 task_base_url");
            }
            items.add(new AdminModelConfigResponse.ModelItem(
                name,
                firstNonBlank(stringValue(item.get("label")), name),
                kind,
                profile.provider(),
                stringValue(item.get("vendor")),
                stringValue(item.get("family")),
                stringValue(item.get("description")),
                booleanValue(item.get("supportsSeed")),
                false,
                stringValue(item.get("generationMode")),
                stringList(item.get("supportedSizes")),
                integerList(item.get("supportedDurations")),
                issues.isEmpty() && profile.ready(),
                profile.source(),
                profile.endpointHost(),
                profile.taskEndpointHost(),
                List.copyOf(issues)
            ));
        }
        return items;
    }

    private List<AdminModelConfigResponse.ProviderItem> readProviders(
        List<AdminModelConfigResponse.ModelItem> models,
        Map<String, String> apiKeys,
        Long userId
    ) {
        Map<String, List<AdminModelConfigResponse.ModelItem>> vendorModels = new LinkedHashMap<>();
        for (AdminModelConfigResponse.ModelItem model : models) {
            String vendorKey = providerGroupKey(model.vendor(), model.provider());
            vendorModels.computeIfAbsent(vendorKey, ignored -> new ArrayList<>()).add(model);
        }
        List<AdminModelConfigResponse.ProviderItem> items = new ArrayList<>();
        for (Map.Entry<String, List<AdminModelConfigResponse.ModelItem>> entry : vendorModels.entrySet()) {
            List<AdminModelConfigResponse.ModelItem> providerModels = entry.getValue();
            String vendorName = providerModels.stream()
                .map(AdminModelConfigResponse.ModelItem::vendor)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(entry.getKey());
            String baseUrl = resolveProviderBaseUrl(providerModels, userId);
            String taskBaseUrl = resolveProviderTaskBaseUrl(providerModels, userId);
            items.add(new AdminModelConfigResponse.ProviderItem(
                entry.getKey(),
                vendorName,
                vendorName,
                providerModels.stream().map(AdminModelConfigResponse.ModelItem::kind).distinct().sorted(this::compareKinds).toList(),
                baseUrl,
                taskBaseUrl,
                hostOf(baseUrl),
                hostOf(taskBaseUrl),
                providerModels.stream().anyMatch(model -> isApiKeyConfigured(model, apiKeys, userId)),
                !baseUrl.isBlank(),
                !taskBaseUrl.isBlank(),
                Map.of(),
                providerModels.stream().map(AdminModelConfigResponse.ModelItem::name).toList()
            ));
        }
        items.sort(Comparator.comparing(AdminModelConfigResponse.ProviderItem::key, String.CASE_INSENSITIVE_ORDER));
        return items;
    }

    private String resolveProviderBaseUrl(List<AdminModelConfigResponse.ModelItem> providerModels, Long userId) {
        for (AdminModelConfigResponse.ModelItem model : providerModels) {
            if (GenerationModelKinds.TEXT.equals(model.kind())) {
                String baseUrl = modelResolver.resolveTextProfile(model.name(), userId).baseUrl();
                if (!baseUrl.isBlank()) {
                    return baseUrl;
                }
                continue;
            }
            String baseUrl = modelResolver.resolveMediaProfile(model.name(), model.kind(), userId).baseUrl();
            if (!baseUrl.isBlank()) {
                return baseUrl;
            }
        }
        return "";
    }

    private String resolveProviderTaskBaseUrl(List<AdminModelConfigResponse.ModelItem> providerModels, Long userId) {
        for (AdminModelConfigResponse.ModelItem model : providerModels) {
            if (!GenerationModelKinds.VIDEO.equals(model.kind())) {
                continue;
            }
            String taskBaseUrl = modelResolver.resolveMediaProfile(model.name(), model.kind(), userId).taskBaseUrl();
            if (!taskBaseUrl.isBlank()) {
                return taskBaseUrl;
            }
            String configuredTaskBaseUrl = modelResolver.value("model.providers." + model.provider() + ".extras", "task_base_url", "");
            if (!configuredTaskBaseUrl.isBlank()) {
                return configuredTaskBaseUrl;
            }
        }
        return "";
    }

    private ApiKeyUpdateBatch collectApiKeyUpdates(
        AdminModelConfigKeyUpdateRequest request,
        List<AdminModelConfigResponse.ProviderItem> providers
    ) {
        Map<String, String> knownProviders = buildProviderKeyLookup(providers);
        List<String> errors = new ArrayList<>();
        Map<String, String> updates = new LinkedHashMap<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<AdminModelConfigKeyUpdateRequest.ProviderKeyInput> inputs = request == null || request.providers() == null
            ? List.of()
            : request.providers();
        for (AdminModelConfigKeyUpdateRequest.ProviderKeyInput input : inputs) {
            String requestedKey = trimToEmpty(input == null ? null : input.key());
            String apiKey = trimToEmpty(input == null ? null : input.apiKey());
            if (requestedKey.isBlank()) {
                if (!apiKey.isBlank()) {
                    errors.add("存在未命名模型接入");
                }
                continue;
            }
            String resolvedKey = knownProviders.get(normalize(requestedKey));
            if (resolvedKey == null) {
                if (!apiKey.isBlank()) {
                    errors.add("未知模型接入: " + requestedKey);
                }
                continue;
            }
            if (!seen.add(resolvedKey)) {
                errors.add("模型接入重复: " + resolvedKey);
                continue;
            }
            if (!apiKey.isBlank()) {
                updates.put(resolvedKey, apiKey);
            }
        }
        return new ApiKeyUpdateBatch(Map.copyOf(updates), List.copyOf(errors));
    }

    private AdminModelConfigResponse applyApiKeyOverrides(
        AdminModelConfigResponse base,
        ApiKeyUpdateBatch updates
    ) {
        Map<String, AdminModelConfigResponse.ProviderItem> providersByLookup = new LinkedHashMap<>();
        List<AdminModelConfigResponse.ProviderItem> providers = new ArrayList<>();
        for (AdminModelConfigResponse.ProviderItem provider : base.providers()) {
            boolean apiKeyConfigured = provider.apiKeyConfigured() || updates.apiKeys().containsKey(provider.key());
            AdminModelConfigResponse.ProviderItem updated = new AdminModelConfigResponse.ProviderItem(
                provider.key(),
                provider.provider(),
                provider.vendor(),
                provider.kinds(),
                provider.baseUrl(),
                provider.taskBaseUrl(),
                provider.endpointHost(),
                provider.taskEndpointHost(),
                apiKeyConfigured,
                provider.baseUrlConfigured(),
                provider.taskBaseUrlConfigured(),
                provider.extras(),
                provider.modelNames()
            );
            providers.add(updated);
            providersByLookup.put(normalize(updated.key()), updated);
            if (!updated.vendor().isBlank()) {
                providersByLookup.putIfAbsent(normalize(updated.vendor()), updated);
            }
            if (!updated.provider().isBlank()) {
                providersByLookup.putIfAbsent(normalize(updated.provider()), updated);
            }
        }
        List<AdminModelConfigResponse.ModelItem> models = base.models().stream()
            .map(model -> applyModelApiKeyOverride(
                model,
                providersByLookup.get(firstNonBlank(normalize(model.vendor()), normalize(model.provider())))
            ))
            .toList();
        List<String> configErrors = new ArrayList<>(base.configErrors());
        for (String error : updates.errors()) {
            if (!configErrors.contains(error)) {
                configErrors.add(error);
            }
        }
        return new AdminModelConfigResponse(
            base.configSource(),
            buildSummary(models, providers),
            base.defaults(),
            List.copyOf(providers),
            List.copyOf(models),
            List.copyOf(configErrors)
        );
    }

    private AdminModelConfigResponse.ModelItem applyModelApiKeyOverride(
        AdminModelConfigResponse.ModelItem model,
        AdminModelConfigResponse.ProviderItem provider
    ) {
        List<String> issues = new ArrayList<>();
        boolean missingApiKey = provider == null
            ? model.issues().contains(MISSING_API_KEY_ISSUE)
            : !provider.apiKeyConfigured();
        if (missingApiKey) {
            issues.add(MISSING_API_KEY_ISSUE);
        }
        for (String issue : model.issues()) {
            if (MISSING_API_KEY_ISSUE.equals(issue) || issues.contains(issue)) {
                continue;
            }
            issues.add(issue);
        }
        return new AdminModelConfigResponse.ModelItem(
            model.name(),
            model.label(),
            model.kind(),
            model.provider(),
            model.vendor(),
            model.family(),
            model.description(),
            model.supportsSeed(),
            model.supportsResponsesApi(),
            model.generationMode(),
            model.supportedSizes(),
            model.supportedDurations(),
            issues.isEmpty(),
            model.configSource(),
            model.endpointHost(),
            model.taskEndpointHost(),
            List.copyOf(issues)
        );
    }

    private boolean isApiKeyConfigured(
        AdminModelConfigResponse.ModelItem model,
        Map<String, String> apiKeys,
        Long userId
    ) {
        if (model == null) {
            return false;
        }
        if (containsApiKey(apiKeys, model.vendor()) || containsApiKey(apiKeys, model.provider())) {
            return true;
        }
        if (GenerationModelKinds.TEXT.equals(model.kind())) {
            return !modelResolver.resolveTextProfile(model.name(), userId).apiKey().isBlank();
        }
        return !modelResolver.resolveMediaProfile(model.name(), model.kind(), userId).apiKey().isBlank();
    }

    private boolean containsApiKey(Map<String, String> apiKeys, String key) {
        String normalizedKey = normalize(key);
        if (normalizedKey.isBlank() || apiKeys == null || apiKeys.isEmpty()) {
            return false;
        }
        return apiKeys.keySet().stream().map(this::normalize).anyMatch(normalizedKey::equals);
    }

    private Map<String, String> buildProviderKeyLookup(List<AdminModelConfigResponse.ProviderItem> providers) {
        Map<String, String> knownProviders = new LinkedHashMap<>();
        for (AdminModelConfigResponse.ProviderItem provider : providers) {
            knownProviders.put(normalize(provider.key()), provider.key());
            if (!provider.provider().isBlank()) {
                knownProviders.putIfAbsent(normalize(provider.provider()), provider.key());
            }
            if (!provider.vendor().isBlank()) {
                knownProviders.putIfAbsent(normalize(provider.vendor()), provider.key());
            }
        }
        for (ModelRuntimePropertiesResolver.ConfigSection section : modelResolver.listSections("model.providers")) {
            String vendorKey = providerGroupKey(section.values().get("vendor"), firstNonBlank(section.values().get("provider"), section.name()));
            String resolvedKey = knownProviders.get(normalize(vendorKey));
            if (resolvedKey == null) {
                continue;
            }
            knownProviders.putIfAbsent(normalize(section.name()), resolvedKey);
            if (!firstNonBlank(section.values().get("provider")).isBlank()) {
                knownProviders.putIfAbsent(normalize(section.values().get("provider")), resolvedKey);
            }
        }
        return knownProviders;
    }

    private AdminModelConfigResponse.Summary buildSummary(
        List<AdminModelConfigResponse.ModelItem> models,
        List<AdminModelConfigResponse.ProviderItem> providers
    ) {
        int providerCount = providers == null ? 0 : providers.size();
        int vendorCount = providers == null
            ? 0
            : (int) providers.stream()
                .map(AdminModelConfigResponse.ProviderItem::vendor)
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .distinct()
                .count();
        return new AdminModelConfigResponse.Summary(
            providerCount,
            vendorCount,
            models.size(),
            countReadyModels(models, null),
            countReadyModels(models, GenerationModelKinds.TEXT),
            countReadyModels(models, GenerationModelKinds.IMAGE),
            countReadyModels(models, GenerationModelKinds.VIDEO)
        );
    }

    private int countReadyModels(List<AdminModelConfigResponse.ModelItem> models, String kind) {
        return (int) models.stream()
            .filter(AdminModelConfigResponse.ModelItem::ready)
            .filter(item -> kind == null || kind.equals(item.kind()))
            .count();
    }

    private int compareKinds(String left, String right) {
        return Integer.compare(kindIndex(left), kindIndex(right));
    }

    private int kindIndex(String kind) {
        int index = KIND_ORDER.indexOf(normalize(kind));
        return index >= 0 ? index : KIND_ORDER.size();
    }

    private String hostOf(String raw) {
        try {
            return java.net.URI.create(raw).getHost();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String providerGroupKey(String vendor, String fallback) {
        return firstNonBlank(normalize(vendor), normalize(fallback));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        String normalized = stringValue(value).toLowerCase(Locale.ROOT);
        return "true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized) || "on".equals(normalized);
    }

    private List<String> stringList(Object value) {
        if (value instanceof List<?> items) {
            return items.stream().map(this::stringValue).filter(item -> !item.isBlank()).toList();
        }
        String normalized = stringValue(value);
        if (normalized.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(normalized.split(","))
            .map(String::trim)
            .filter(item -> !item.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new))
            .stream()
            .toList();
    }

    private List<Integer> integerList(Object value) {
        if (value instanceof List<?> items) {
            return items.stream()
                .map(this::stringValue)
                .map(this::integerValue)
                .filter(item -> item != null && item > 0)
                .distinct()
                .toList();
        }
        List<Integer> numbers = new ArrayList<>();
        for (String item : stringList(value)) {
            Integer parsed = integerValue(item);
            if (parsed != null && parsed > 0 && !numbers.contains(parsed)) {
                numbers.add(parsed);
            }
        }
        return List.copyOf(numbers);
    }

    private Integer integerValue(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private double doubleValue(String raw, double fallback) {
        try {
            return Double.parseDouble(String.valueOf(raw).trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private record ApiKeyUpdateBatch(
        Map<String, String> apiKeys,
        List<String> errors
    ) {
    }
}
