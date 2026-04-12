package com.telas.monitoring.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class AesTextEncryptionService {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKey secretKey;

    public AesTextEncryptionService(
            @Value("${monitoring.kasa.encryption-key:}") String rawKey) {
        if (!StringUtils.hasText(rawKey)) {
            this.secretKey = null;
        } else {
            byte[] keyBytes = rawKey.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length != 32) {
                try {
                    keyBytes =
                            Arrays.copyOf(
                                    MessageDigest.getInstance("SHA-256").digest(keyBytes), 32);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            } else {
                keyBytes = Arrays.copyOf(keyBytes, 32);
            }
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        }
    }

    public boolean isConfigured() {
        return secretKey != null;
    }

    public String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return null;
        }
        if (secretKey == null) {
            throw new IllegalStateException("monitoring.kasa.encryption-key is not configured");
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + cipherText.length);
            buf.put(iv);
            buf.put(cipherText);
            return Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public String decrypt(String cipherBase64) {
        if (!StringUtils.hasText(cipherBase64)) {
            return null;
        }
        if (secretKey == null) {
            throw new IllegalStateException("monitoring.kasa.encryption-key is not configured");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherBase64);
            ByteBuffer buf = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buf.get(iv);
            byte[] cipherBytes = new byte[buf.remaining()];
            buf.get(cipherBytes);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] plain = cipher.doFinal(cipherBytes);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
