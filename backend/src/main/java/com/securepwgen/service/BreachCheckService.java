package com.securepwgen.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Optional;

/**
 * HaveIBeenPwned breach check using k-anonymity.
 *
 * Protocol:
 * 1. SHA-1 hash the password locally.
 * 2. Send only the first 5 hex chars (prefix) to HIBP API.
 * 3. HIBP returns all hashes with that prefix (~500 results).
 * 4. Check if the remaining 35-char suffix matches any result.
 *
 * The full password and full hash are NEVER transmitted.
 */
@Service
public class BreachCheckService {

    private static final Logger log = LoggerFactory.getLogger(BreachCheckService.class);
    private static final String HIBP_URL = "https://api.pwnedpasswords.com/range/";

    private final WebClient    webClient;

    @Value("${app.breach-check.enabled:true}")
    private boolean enabled;

    public BreachCheckService(WebClient.Builder builder) {
        this.webClient = builder
            .baseUrl(HIBP_URL)
            .defaultHeader("Add-Padding", "true")
            .defaultHeader("User-Agent",  "SecurePasswordGenerator/1.0")
            .build();
    }

    /**
     * Returns the number of times the password appeared in breach databases,
     * or Optional.empty() if the check could not be completed.
     */
    public Optional<Integer> checkBreach(String password) {
        if (!enabled) {
            log.debug("Breach check disabled — skipping.");
            return Optional.empty();
        }
        try {
            String sha1   = sha1Hex(password).toUpperCase();
            String prefix = sha1.substring(0, 5);
            String suffix = sha1.substring(5);

            String body = webClient.get()
                .uri(prefix)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (body == null) return Optional.empty();

            return Arrays.stream(body.split("\r?\n"))
                .filter(line -> line.toUpperCase().startsWith(suffix))
                .findFirst()
                .map(line -> Integer.parseInt(line.split(":")[1].trim()));

        } catch (Exception e) {
            log.warn("HIBP breach check failed: {}", e.getMessage());
            return Optional.empty(); // Fail open — don't block generation
        }
    }

    public boolean isBreached(String password) {
        return checkBreach(password).map(c -> c > 0).orElse(false);
    }

    private String sha1Hex(String input) throws Exception {
        MessageDigest md   = MessageDigest.getInstance("SHA-1");
        byte[]        hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb   = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
