package com.controltower.app.shared.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption/decryption utility.
 * IV (12 bytes) is prepended to the ciphertext before Base64 encoding.
 */
@Slf4j
@Component
public class AesEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Value("${app.encryption.key:0123456789abcdef0123456789abcdef}")
    private String secretKey;

    /**
     * Encrypts plainText using AES/GCM/NoPadding.
     * Returns Base64-encoded string with IV prepended (IV:ciphertext).
     * Returns null if input is null.
     */
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, buildKey(), parameterSpec);

            byte[] ciphertext = cipher.doFinal(plainText.getBytes());

            // Prepend IV to ciphertext
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception ex) {
            log.error("Encryption failed: {}", ex.getMessage());
            throw new RuntimeException("Encryption failed", ex);
        }
    }

    /**
     * Decrypts a Base64-encoded string that was encrypted with {@link #encrypt(String)}.
     * Returns null if input is null.
     */
    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedBytes = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, buildKey(), parameterSpec);

            byte[] plaintext = cipher.doFinal(encryptedBytes);
            return new String(plaintext);
        } catch (Exception ex) {
            log.error("Decryption failed: {}", ex.getMessage());
            throw new RuntimeException("Decryption failed", ex);
        }
    }

    private SecretKeySpec buildKey() {
        // Use the first 32 bytes (256 bits) of the secret key
        byte[] keyBytes = secretKey.getBytes();
        byte[] key = new byte[32];
        System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 32));
        return new SecretKeySpec(key, "AES");
    }
}
