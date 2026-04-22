package com.jiandou.api.auth.infrastructure.mybatis;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Repository;

/**
 * 用户模型凭证仓储。
 */
@Repository
public class MybatisUserModelCredentialRepository {

    private final SqlSessionFactory sqlSessionFactory;
    private final UserModelCredentialCipher credentialCipher;

    public MybatisUserModelCredentialRepository(
        SqlSessionFactory sqlSessionFactory,
        UserModelCredentialCipher credentialCipher
    ) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.credentialCipher = credentialCipher;
    }

    /**
     * 返回指定用户的 provider key 映射。
     * @param userId 用户 ID
     * @return 处理结果
     */
    public Map<String, String> findApiKeysByUserId(Long userId) {
        if (userId == null) {
            return Map.of();
        }
        try (SqlSession session = sqlSessionFactory.openSession()) {
            List<SysUserModelCredentialEntity> rows = session.getMapper(SysUserModelCredentialMapper.class).selectList(
                Wrappers.<SysUserModelCredentialEntity>lambdaQuery()
                    .eq(SysUserModelCredentialEntity::getUserId, userId)
                    .orderByAsc(SysUserModelCredentialEntity::getProviderKey)
            );
            Map<String, String> result = new LinkedHashMap<>();
            for (SysUserModelCredentialEntity row : rows) {
                String providerKey = normalizeProviderKey(row.getProviderKey());
                String apiKey = credentialCipher.decrypt(row.getEncryptedApiKey());
                if (!providerKey.isBlank() && !apiKey.isBlank()) {
                    result.put(providerKey, apiKey);
                }
            }
            return Map.copyOf(result);
        }
    }

    /**
     * 返回指定用户、指定 provider 的 API Key。
     * @param userId 用户 ID
     * @param providerKey provider key
     * @return 处理结果
     */
    public String findApiKey(Long userId, String providerKey) {
        return findApiKey(userId, List.of(providerKey));
    }

    /**
     * 返回指定用户、指定 provider 候选列表的首个 API Key。
     * @param userId 用户 ID
     * @param providerKeys provider key 候选列表
     * @return 处理结果
     */
    public String findApiKey(Long userId, List<String> providerKeys) {
        List<String> normalizedProviderKeys = providerKeys == null
            ? List.of()
            : providerKeys.stream()
                .map(this::normalizeProviderKey)
                .filter(key -> !key.isBlank())
                .distinct()
                .toList();
        if (userId == null || normalizedProviderKeys.isEmpty()) {
            return "";
        }
        try (SqlSession session = sqlSessionFactory.openSession()) {
            List<SysUserModelCredentialEntity> rows = session.getMapper(SysUserModelCredentialMapper.class).selectList(
                Wrappers.<SysUserModelCredentialEntity>lambdaQuery()
                    .eq(SysUserModelCredentialEntity::getUserId, userId)
                    .in(SysUserModelCredentialEntity::getProviderKey, normalizedProviderKeys)
            );
            Map<String, String> apiKeysByProvider = new LinkedHashMap<>();
            for (SysUserModelCredentialEntity row : rows) {
                String providerKey = normalizeProviderKey(row.getProviderKey());
                String apiKey = credentialCipher.decrypt(row.getEncryptedApiKey());
                if (!providerKey.isBlank() && !apiKey.isBlank()) {
                    apiKeysByProvider.put(providerKey, apiKey);
                }
            }
            for (String providerKey : normalizedProviderKeys) {
                String apiKey = apiKeysByProvider.get(providerKey);
                if (apiKey != null && !apiKey.isBlank()) {
                    return apiKey;
                }
            }
            return "";
        }
    }

    /**
     * 保存指定用户的 API Key 更新。
     * @param userId 用户 ID
     * @param providerApiKeys provider -> key
     */
    public void saveApiKeys(Long userId, Map<String, String> providerApiKeys) {
        if (userId == null || providerApiKeys == null || providerApiKeys.isEmpty()) {
            return;
        }
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            SysUserModelCredentialMapper mapper = session.getMapper(SysUserModelCredentialMapper.class);
            for (Map.Entry<String, String> entry : providerApiKeys.entrySet()) {
                String providerKey = normalizeProviderKey(entry.getKey());
                String apiKey = entry.getValue() == null ? "" : entry.getValue().trim();
                if (providerKey.isBlank() || apiKey.isBlank()) {
                    continue;
                }
                SysUserModelCredentialEntity existing = mapper.selectOne(
                    Wrappers.<SysUserModelCredentialEntity>lambdaQuery()
                        .eq(SysUserModelCredentialEntity::getUserId, userId)
                        .eq(SysUserModelCredentialEntity::getProviderKey, providerKey)
                        .last("LIMIT 1")
                );
                String encryptedApiKey = credentialCipher.encrypt(apiKey);
                if (existing == null) {
                    SysUserModelCredentialEntity entity = new SysUserModelCredentialEntity();
                    entity.setUserId(userId);
                    entity.setProviderKey(providerKey);
                    entity.setEncryptedApiKey(encryptedApiKey);
                    mapper.insert(entity);
                    continue;
                }
                SysUserModelCredentialEntity update = new SysUserModelCredentialEntity();
                update.setEncryptedApiKey(encryptedApiKey);
                mapper.update(
                    update,
                    Wrappers.<SysUserModelCredentialEntity>lambdaUpdate()
                        .eq(SysUserModelCredentialEntity::getUserId, userId)
                        .eq(SysUserModelCredentialEntity::getProviderKey, providerKey)
                );
            }
            session.commit();
        }
    }

    private String normalizeProviderKey(String providerKey) {
        return providerKey == null ? "" : providerKey.trim().toLowerCase(Locale.ROOT);
    }
}
