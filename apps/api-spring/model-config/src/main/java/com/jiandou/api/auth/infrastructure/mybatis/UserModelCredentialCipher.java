package com.jiandou.api.auth.infrastructure.mybatis;

import com.jiandou.api.common.exception.ApiException;
import com.jiandou.api.config.JiandouAuthProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 用户模型凭证加解密工具。
 */
@Component
public class UserModelCredentialCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final String VERSION_PREFIX = "v1:";

    private final SecretKeySpec secretKeySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserModelCredentialCipher(JiandouAuthProperties authProperties) {
        this.secretKeySpec = new SecretKeySpec(sha256(normalizeSecret(authProperties.getCredentialSecret())), "AES");
    }

    /**
     * 加密明文 key。
     * @param plainText 明文值
     * @return 处理结果
     */
    public String encrypt(String plainText) {
        String normalized = plainText == null ? "" : plainText.trim();
        if (normalized.isBlank()) {
            return "";
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(normalized.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return VERSION_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "credential_encrypt_failed", "用户模型密钥加密失败", ex);
        }
    }

    /**
     * 解密密文 key。
     * @param encryptedText 密文值
     * @return 处理结果
     */
    public String decrypt(String encryptedText) {
        String normalized = encryptedText == null ? "" : encryptedText.trim();
        if (normalized.isBlank()) {
            return "";
        }
        if (!normalized.startsWith(VERSION_PREFIX)) {
            return normalized;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(normalized.substring(VERSION_PREFIX.length()));
            if (payload.length <= IV_LENGTH) {
                return "";
            }
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[payload.length - IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            System.arraycopy(payload, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8).trim();
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "credential_decrypt_failed", "用户模型密钥解密失败", ex);
        }
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "credential_cipher_init_failed", "用户模型密钥加密器初始化失败", ex);
        }
    }

    private String normalizeSecret(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "credential_secret_missing", "缺少用户模型密钥加密主密钥配置");
        }
        return normalized;
    }
}
