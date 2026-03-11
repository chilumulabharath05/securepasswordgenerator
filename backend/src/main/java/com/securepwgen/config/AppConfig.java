package com.securepwgen.config;

import com.securepwgen.security.RateLimitFilter;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.SecureRandom;
import java.util.Base64;

@Configuration
public class AppConfig {

    /**
     * BCrypt with work factor 12.
     * ~300ms per hash on modern hardware — OWASP minimum recommendation.
     */
    @Bean
    public BCryptPasswordEncoder bcryptPasswordEncoder() {
        return new BCryptPasswordEncoder(12, new SecureRandom());
    }

    /**
     * WebClient for reactive HTTP calls (HIBP breach check API).
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    /**
     * Register the rate-limit filter so Spring Boot picks it up correctly.
     */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
        FilterRegistrationBean<RateLimitFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new RateLimitFilter());
        reg.addUrlPatterns("/api/*");
        reg.setOrder(1);
        return reg;
    }

    /**
     * Argon2id hash — OWASP first-choice algorithm.
     * Parameters: memory=64 MB, iterations=3, parallelism=4, output=32 bytes.
     *
     * Format: $argon2id$v=19$m=65536,t=3,p=4$<base64-salt>$<base64-hash>
     */
    public static String argon2idHash(String password) {
        SecureRandom sr   = new SecureRandom();
        byte[]       salt = new byte[16];
        sr.nextBytes(salt);

        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withSalt(salt)
                .withMemoryAsKB(65_536)
                .withIterations(3)
                .withParallelism(4)
                .build();

        Argon2BytesGenerator gen = new Argon2BytesGenerator();
        gen.init(params);

        byte[] hash = new byte[32];
        gen.generateBytes(password.toCharArray(), hash);

        return "$argon2id$v=19$m=65536,t=3,p=4$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }
}
