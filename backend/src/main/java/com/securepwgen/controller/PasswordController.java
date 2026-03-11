package com.securepwgen.controller;

import com.securepwgen.config.AppConfig;
import com.securepwgen.model.PasswordRequest;
import com.securepwgen.model.PasswordResponse;
import com.securepwgen.model.PasswordResponse.PasswordEntry;
import com.securepwgen.service.BreachCheckService;
import com.securepwgen.service.HistoryService;
import com.securepwgen.service.PasswordGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/password")
@Tag(name = "Password Generator", description = "Cryptographically secure password generation API")
public class PasswordController {

    private static final Logger log = LoggerFactory.getLogger(PasswordController.class);

    private final PasswordGeneratorService generator;
    private final BreachCheckService       breach;
    private final HistoryService           history;

    public PasswordController(PasswordGeneratorService generator,
                               BreachCheckService breach,
                               HistoryService history) {
        this.generator = generator;
        this.breach    = breach;
        this.history   = history;
    }

    // ── Generate ──────────────────────────────────────────────────────

    @PostMapping("/generate")
    @Operation(summary = "Generate secure password(s)")
    public ResponseEntity<PasswordResponse> generate(@Valid @RequestBody PasswordRequest req) {
        return ResponseEntity.ok(generator.generate(req));
    }

    @PostMapping("/generate-with-hashes")
    @Operation(summary = "Generate password with BCrypt and Argon2id hashes")
    public ResponseEntity<PasswordResponse> generateWithHashes(
            @Valid @RequestBody PasswordRequest req) {

        PasswordResponse resp = generator.generate(req);

        // Enrich each entry with Argon2id hash
        List<PasswordEntry> enriched = resp.getPasswords();
        for (PasswordEntry entry : enriched) {
            try {
                entry.setArgon2Hash(AppConfig.argon2idHash(entry.getPassword()));
            } catch (Exception e) {
                log.warn("Argon2 hash failed for entry: {}", e.getMessage());
            }
        }
        return ResponseEntity.ok(resp);
    }

    // ── Breach Check ──────────────────────────────────────────────────

    @PostMapping("/breach-check")
    @Operation(summary = "Check password against HaveIBeenPwned (k-anonymity)")
    public ResponseEntity<Map<String, Object>> breachCheck(
            @RequestBody Map<String, String> body) {

        String password = body.get("password");
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Password field is required"));
        }

        Optional<Integer> result = breach.checkBreach(password);
        boolean isBreached  = result.map(c -> c > 0).orElse(false);
        int     breachCount = result.orElse(0);

        return ResponseEntity.ok(Map.of(
            "breached",    isBreached,
            "breachCount", breachCount,
            "message", isBreached
                ? "⚠️ Found in " + breachCount + " breach records. Do not use this password."
                : "✅ Not found in known breach databases."
        ));
    }

    // ── Save History ──────────────────────────────────────────────────

    @PostMapping("/save-history")
    @Operation(summary = "Save password to encrypted history")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Map<String, String>> saveHistory(
            @RequestBody Map<String, Object> body) {

        String  password   = (String) body.get("password");
        String  label      = (String) body.getOrDefault("label", "");
        boolean passphrase = Boolean.parseBoolean(
            body.getOrDefault("passphrase", "false").toString());

        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Password is required"));
        }

        PasswordEntry entry = new PasswordEntry();
        entry.setPassword(password);

        var saved = history.save(entry, label, passphrase);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of(
                "id",      saved.getId(),
                "message", "Saved with AES-256-GCM encryption."
            ));
    }

    // ── Export ────────────────────────────────────────────────────────

    @GetMapping("/export")
    @Operation(summary = "Export password history as AES-encrypted file")
    public ResponseEntity<byte[]> export() {
        String blob = history.exportEncrypted();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "passwords_encrypted.dat");
        return ResponseEntity.ok().headers(headers)
            .body(blob.getBytes(StandardCharsets.UTF_8));
    }

    // ── Health ────────────────────────────────────────────────────────

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status",  "UP",
            "version", "1.0.0",
            "crypto",  "SecureRandom | AES-256-GCM | BCrypt(12) | Argon2id"
        ));
    }
}
