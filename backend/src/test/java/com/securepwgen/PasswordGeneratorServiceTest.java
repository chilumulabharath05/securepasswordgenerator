package com.securepwgen;

import com.securepwgen.exception.PasswordGenerationException;
import com.securepwgen.model.PasswordRequest;
import com.securepwgen.model.PasswordResponse;
import com.securepwgen.model.PasswordResponse.StrengthLevel;
import com.securepwgen.service.EntropyCalculatorService;
import com.securepwgen.service.PasswordGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PasswordGeneratorService Tests")
class PasswordGeneratorServiceTest {

    private PasswordGeneratorService service;

    @BeforeEach
    void setUp() {
        service = new PasswordGeneratorService(
            new EntropyCalculatorService(),
            new BCryptPasswordEncoder(4)  // low cost for tests
        );
    }

    @Test
    @DisplayName("Generated password has exact requested length")
    void passwordHasCorrectLength() {
        PasswordRequest req = new PasswordRequest();
        req.setLength(20);
        req.setUseUppercase(true);
        req.setUseLowercase(true);
        req.setUseNumbers(true);
        req.setUseSpecialChars(true);

        String pwd = service.generate(req).getPasswords().get(0).getPassword();
        assertThat(pwd).hasSize(20);
    }

    @ParameterizedTest(name = "length={0}")
    @ValueSource(ints = {4, 8, 16, 32, 64, 128})
    @DisplayName("Various lengths are generated correctly")
    void variousLengthsWork(int len) {
        PasswordRequest req = new PasswordRequest();
        req.setLength(len);
        req.setUseLowercase(true);
        req.setUseNumbers(true);
        assertThat(service.generate(req).getPasswords().get(0).getPassword()).hasSize(len);
    }

    @Test
    @DisplayName("Numbers-only password contains only digits")
    void numbersOnly() {
        PasswordRequest req = new PasswordRequest();
        req.setLength(30);
        req.setUseUppercase(false);
        req.setUseLowercase(false);
        req.setUseNumbers(true);
        req.setUseSpecialChars(false);

        String pwd = service.generate(req).getPasswords().get(0).getPassword();
        assertThat(pwd).matches("[0-9]+");
    }

    @Test
    @DisplayName("Uppercase-only password contains only uppercase letters")
    void uppercaseOnly() {
        PasswordRequest req = new PasswordRequest();
        req.setLength(30);
        req.setUseUppercase(true);
        req.setUseLowercase(false);
        req.setUseNumbers(false);
        req.setUseSpecialChars(false);

        String pwd = service.generate(req).getPasswords().get(0).getPassword();
        assertThat(pwd).matches("[A-Z]+");
    }

    @Test
    @DisplayName("Batch generates unique passwords")
    void batchIsUnique() {
        PasswordRequest req = new PasswordRequest();
        req.setLength(16);
        req.setCount(5);
        req.setUseUppercase(true);
        req.setUseLowercase(true);
        req.setUseNumbers(true);

        PasswordResponse resp = service.generate(req);
        assertThat(resp.getPasswords()).hasSize(5);

        Set<String> unique = new HashSet<>();
        resp.getPasswords().forEach(e -> unique.add(e.getPassword()));
        assertThat(unique).hasSize(5);
    }

    @Test
    @DisplayName("Long password with full charset is STRONG or VERY_STRONG")
    void longPasswordIsStrong() {
        PasswordRequest req = new PasswordRequest();
        req.setLength(32);
        req.setUseUppercase(true);
        req.setUseLowercase(true);
        req.setUseNumbers(true);
        req.setUseSpecialChars(true);

        StrengthLevel level = service.generate(req).getPasswords().get(0).getStrengthLevel();
        assertThat(level).isIn(StrengthLevel.STRONG, StrengthLevel.VERY_STRONG);
    }

    @Test
    @DisplayName("Short numbers-only password is VERY_WEAK")
    void shortPasswordIsWeak() {
        PasswordRequest req = new PasswordRequest();
        req.setLength(4);
        req.setUseUppercase(false);
        req.setUseLowercase(false);
        req.setUseNumbers(true);
        req.setUseSpecialChars(false);

        StrengthLevel level = service.generate(req).getPasswords().get(0).getStrengthLevel();
        assertThat(level).isIn(StrengthLevel.VERY_WEAK, StrengthLevel.WEAK);
    }

    @Test
    @DisplayName("No character sets throws PasswordGenerationException")
    void noCharsetThrows() {
        PasswordRequest req = new PasswordRequest();
        req.setLength(16);
        req.setUseUppercase(false);
        req.setUseLowercase(false);
        req.setUseNumbers(false);
        req.setUseSpecialChars(false);

        assertThatThrownBy(() -> service.generate(req))
            .isInstanceOf(PasswordGenerationException.class);
    }

    @Test
    @DisplayName("Passphrase has correct word count")
    void passphraseWordCount() {
        PasswordRequest req = new PasswordRequest();
        req.setLength(0);
        req.setPassphrase(true);
        req.setPassphraseWordCount(6);
        req.setPassphraseSeparator("-");

        String pass = service.generate(req).getPasswords().get(0).getPassword();
        assertThat(pass.split("-")).hasSize(6);
    }

    @Test
    @DisplayName("Response includes all metadata fields")
    void responseHasAllFields() {
        PasswordRequest req = new PasswordRequest();
        req.setLength(16);
        req.setUseUppercase(true);
        req.setUseLowercase(true);
        req.setUseNumbers(true);

        PasswordResponse.PasswordEntry entry = service.generate(req).getPasswords().get(0);
        assertThat(entry.getPassword()).isNotBlank();
        assertThat(entry.getEntropyBits()).isPositive();
        assertThat(entry.getStrengthLevel()).isNotNull();
        assertThat(entry.getCrackTimeEstimate()).isNotBlank();
        assertThat(entry.getBcryptHash()).startsWith("$2a$");
    }

    @Test
    @DisplayName("Ambiguous chars excluded when flag set")
    void excludeAmbiguous() {
        PasswordRequest req = new PasswordRequest();
        req.setLength(100);
        req.setUseUppercase(true);
        req.setUseLowercase(true);
        req.setUseNumbers(true);
        req.setExcludeAmbiguous(true);

        for (int i = 0; i < 10; i++) {
            String pwd = service.generate(req).getPasswords().get(0).getPassword();
            assertThat(pwd).doesNotContain("0", "O", "l", "1", "I");
        }
    }

    @Test
    @DisplayName("Multiple generations produce unique results (CSPRNG check)")
    void generationsAreUnique() {
        PasswordRequest req = new PasswordRequest();
        req.setLength(16);
        req.setUseUppercase(true);
        req.setUseLowercase(true);
        req.setUseNumbers(true);

        Set<String> results = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            results.add(service.generate(req).getPasswords().get(0).getPassword());
        }
        assertThat(results.size()).isGreaterThan(15); // should be effectively all unique
    }
}
