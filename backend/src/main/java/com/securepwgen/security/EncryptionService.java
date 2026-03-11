package com.securepwgen.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption for at-rest data protection.
 * - 256-bit key
 * - 96-bit random IV per encryption (NIST recommended)
 * - 128-bit GCM authentication tag (detects tampering)
 */
@Service
public class EncryptionService {

    private static final Logger log = LoggerFactory.getLogger(EncryptionService.class);

    private static final String ALGORITHM      = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int    IV_LENGTH      = 12;   // 96 bits
    private static final int    TAG_LENGTH     = 128;  // bits

    private final SecretKey    secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptionService(@Value("${app.encryption.key:}") String base64Key) {
        if (base64Key != null && !base64Key.isBlank()) {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            this.secretKey  = new SecretKeySpec(keyBytes, ALGORITHM);
            log.info("EncryptionService: loaded key from config.");
        } else {
            this.secretKey = generateKey();
            log.warn("EncryptionService: no key configured, using ephemeral key (history won't survive restart).");
        }
    }

    /** Encrypt plaintext → Base64(IV + ciphertext+tag) */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

            byte[] combined = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv,         0, combined, 0,         IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, IV_LENGTH, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /** Decrypt Base64(IV + ciphertext+tag) → plaintext */
    public String decrypt(String encryptedBase64) {
        try {
            byte[] combined    = Base64.getDecoder().decode(encryptedBase64);
            byte[] iv          = new byte[IV_LENGTH];
            byte[] ciphertext  = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0,         iv,         0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed — data may be corrupted or tampered", e);
        }
    }

    private SecretKey generateKey() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance(ALGORITHM);
            kg.init(256, secureRandom);
            return kg.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Could not generate AES key", e);
        }
    }
}
