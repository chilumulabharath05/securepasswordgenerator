package com.securepwgen.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class PasswordRequest {

    @NotNull(message = "Length is required")
    @Min(value = 4,   message = "Minimum length is 4")
    @Max(value = 256, message = "Maximum length is 256")
    private Integer length = 16;

    private boolean useUppercase    = true;
    private boolean useLowercase    = true;
    private boolean useNumbers      = true;
    private boolean useSpecialChars = true;
    private boolean excludeAmbiguous = false;
    private boolean passphrase      = false;

    @Min(value = 3,  message = "Minimum 3 words")
    @Max(value = 12, message = "Maximum 12 words")
    private int passphraseWordCount = 6;

    private String passphraseSeparator = "-";

    @Min(value = 1,  message = "Minimum 1")
    @Max(value = 10, message = "Maximum 10")
    private int count = 1;

    // ── Getters & Setters ─────────────────────────────────────────────

    public Integer getLength()                 { return length; }
    public void setLength(Integer length)      { this.length = length; }

    public boolean isUseUppercase()            { return useUppercase; }
    public void setUseUppercase(boolean v)     { this.useUppercase = v; }

    public boolean isUseLowercase()            { return useLowercase; }
    public void setUseLowercase(boolean v)     { this.useLowercase = v; }

    public boolean isUseNumbers()              { return useNumbers; }
    public void setUseNumbers(boolean v)       { this.useNumbers = v; }

    public boolean isUseSpecialChars()         { return useSpecialChars; }
    public void setUseSpecialChars(boolean v)  { this.useSpecialChars = v; }

    public boolean isExcludeAmbiguous()        { return excludeAmbiguous; }
    public void setExcludeAmbiguous(boolean v) { this.excludeAmbiguous = v; }

    public boolean isPassphrase()              { return passphrase; }
    public void setPassphrase(boolean v)       { this.passphrase = v; }

    public int getPassphraseWordCount()               { return passphraseWordCount; }
    public void setPassphraseWordCount(int v)         { this.passphraseWordCount = v; }

    public String getPassphraseSeparator()            { return passphraseSeparator; }
    public void setPassphraseSeparator(String v)      { this.passphraseSeparator = v; }

    public int getCount()                      { return count; }
    public void setCount(int v)                { this.count = v; }

    public boolean hasAtLeastOneCharSet() {
        return passphrase || useUppercase || useLowercase || useNumbers || useSpecialChars;
    }
}
