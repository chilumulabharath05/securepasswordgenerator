package com.securepwgen.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "password_history")
public class PasswordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 2048)
    private String encryptedPassword;

    @Column(length = 128)
    private String label;

    private Integer passwordLength;
    private Double  entropyBits;

    @Enumerated(EnumType.STRING)
    private PasswordResponse.StrengthLevel strengthLevel;

    private boolean passphrase = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // ── Constructors ──────────────────────────────────────────────────

    public PasswordHistory() {}

    // ── Getters & Setters ─────────────────────────────────────────────

    public String                          getId()                          { return id; }
    public void                            setId(String id)                 { this.id = id; }

    public String                          getEncryptedPassword()           { return encryptedPassword; }
    public void                            setEncryptedPassword(String v)   { this.encryptedPassword = v; }

    public String                          getLabel()                       { return label; }
    public void                            setLabel(String v)               { this.label = v; }

    public Integer                         getPasswordLength()              { return passwordLength; }
    public void                            setPasswordLength(Integer v)     { this.passwordLength = v; }

    public Double                          getEntropyBits()                 { return entropyBits; }
    public void                            setEntropyBits(Double v)         { this.entropyBits = v; }

    public PasswordResponse.StrengthLevel  getStrengthLevel()               { return strengthLevel; }
    public void                            setStrengthLevel(PasswordResponse.StrengthLevel v) { this.strengthLevel = v; }

    public boolean                         isPassphrase()                   { return passphrase; }
    public void                            setPassphrase(boolean v)         { this.passphrase = v; }

    public Instant                         getCreatedAt()                   { return createdAt; }
    public void                            setCreatedAt(Instant v)          { this.createdAt = v; }
}
