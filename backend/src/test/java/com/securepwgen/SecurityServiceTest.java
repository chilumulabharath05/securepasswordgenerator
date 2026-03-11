package com.securepwgen;

import com.securepwgen.model.PasswordResponse.StrengthLevel;
import com.securepwgen.security.EncryptionService;
import com.securepwgen.service.EntropyCalculatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("EncryptionService + EntropyCalculatorService Tests")
class SecurityServiceTest {

    private EncryptionService      encryptionService;
    private EntropyCalculatorService entropyService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService("");
        entropyService    = new EntropyCalculatorService();
    }

    // ── Encryption ────────────────────────────────────────────────────

    @Test
    @DisplayName("Encrypt then decrypt returns original plaintext")
    void roundTrip() {
        String plain = "MyS3cur3P@ssword!";
        assertThat(encryptionService.decrypt(encryptionService.encrypt(plain))).isEqualTo(plain);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "a", "Hello World!", "Unicode: 密码", "Long: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    @DisplayName("Handles various inputs")
    void variousInputs(String plain) {
        String enc = encryptionService.encrypt(plain);
        assertThat(encryptionService.decrypt(enc)).isEqualTo(plain);
    }

    @Test
    @DisplayName("Same plaintext produces different ciphertext each time (unique IV)")
    void uniqueIv() {
        String enc1 = encryptionService.encrypt("password");
        String enc2 = encryptionService.encrypt("password");
        assertThat(enc1).isNotEqualTo(enc2);
    }

    @Test
    @DisplayName("Tampered ciphertext throws exception (GCM integrity check)")
    void tamperDetected() {
        String enc     = encryptionService.encrypt("sensitive");
        String tampered = enc.substring(0, enc.length() - 6) + "XXXXXX";
        assertThatThrownBy(() -> encryptionService.decrypt(tampered))
            .isInstanceOf(RuntimeException.class);
    }

    // ── Entropy ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Entropy increases with charset size")
    void entropyIncreasesWithCharset() {
        assertThat(entropyService.calculateEntropy(95, 16))
            .isGreaterThan(entropyService.calculateEntropy(26, 16));
    }

    @Test
    @DisplayName("Entropy increases with length")
    void entropyIncreasesWithLength() {
        assertThat(entropyService.calculateEntropy(72, 32))
            .isGreaterThan(entropyService.calculateEntropy(72, 16));
    }

    @Test
    @DisplayName("VERY_WEAK: < 28 bits")
    void veryWeak() {
        assertThat(entropyService.strengthLevel(20)).isEqualTo(StrengthLevel.VERY_WEAK);
    }

    @Test
    @DisplayName("VERY_STRONG: >= 128 bits")
    void veryStrong() {
        assertThat(entropyService.strengthLevel(128)).isEqualTo(StrengthLevel.VERY_STRONG);
        assertThat(entropyService.strengthLevel(256)).isEqualTo(StrengthLevel.VERY_STRONG);
    }

    @Test
    @DisplayName("Crack time is non-empty string")
    void crackTimeNonEmpty() {
        assertThat(entropyService.estimateCrackTime(0)).isNotBlank();
        assertThat(entropyService.estimateCrackTime(64)).isNotBlank();
        assertThat(entropyService.estimateCrackTime(256)).isNotBlank();
    }
}
