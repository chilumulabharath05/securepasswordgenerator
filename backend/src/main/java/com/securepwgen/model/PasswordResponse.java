package com.securepwgen.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PasswordResponse {

    private List<PasswordEntry> passwords;
    private Instant generatedAt = Instant.now();

    public PasswordResponse() {}
    public PasswordResponse(List<PasswordEntry> passwords) {
        this.passwords    = passwords;
        this.generatedAt  = Instant.now();
    }

    public List<PasswordEntry> getPasswords()           { return passwords; }
    public void setPasswords(List<PasswordEntry> p)     { this.passwords = p; }
    public Instant getGeneratedAt()                     { return generatedAt; }

    // ── StrengthLevel Enum ────────────────────────────────────────────

    public enum StrengthLevel { VERY_WEAK, WEAK, FAIR, STRONG, VERY_STRONG }

    // ── Inner PasswordEntry ───────────────────────────────────────────

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PasswordEntry {

        private String        password;
        private double        entropyBits;
        private StrengthLevel strengthLevel;
        private String        crackTimeEstimate;
        private int           charsetSize;
        private String        bcryptHash;
        private String        argon2Hash;
        private Boolean       breached;
        private Integer       breachCount;

        public PasswordEntry() {}

        // ── Getters & Setters ─────────────────────────────────────────

        public String        getPassword()                      { return password; }
        public void          setPassword(String v)              { this.password = v; }

        public double        getEntropyBits()                   { return entropyBits; }
        public void          setEntropyBits(double v)           { this.entropyBits = v; }

        public StrengthLevel getStrengthLevel()                 { return strengthLevel; }
        public void          setStrengthLevel(StrengthLevel v)  { this.strengthLevel = v; }

        public String        getCrackTimeEstimate()             { return crackTimeEstimate; }
        public void          setCrackTimeEstimate(String v)     { this.crackTimeEstimate = v; }

        public int           getCharsetSize()                   { return charsetSize; }
        public void          setCharsetSize(int v)              { this.charsetSize = v; }

        public String        getBcryptHash()                    { return bcryptHash; }
        public void          setBcryptHash(String v)            { this.bcryptHash = v; }

        public String        getArgon2Hash()                    { return argon2Hash; }
        public void          setArgon2Hash(String v)            { this.argon2Hash = v; }

        public Boolean       getBreached()                      { return breached; }
        public void          setBreached(Boolean v)             { this.breached = v; }

        public Integer       getBreachCount()                   { return breachCount; }
        public void          setBreachCount(Integer v)          { this.breachCount = v; }
    }
}
