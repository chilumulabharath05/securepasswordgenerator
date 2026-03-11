package com.securepwgen.service;

import com.securepwgen.model.PasswordResponse.StrengthLevel;
import org.springframework.stereotype.Service;

/**
 * Shannon entropy calculation and password strength estimation.
 *
 * Formula: H = L × log₂(N)
 *   H = entropy bits
 *   L = password length
 *   N = charset size
 */
@Service
public class EntropyCalculatorService {

    private static final double GUESSES_PER_SECOND = 1e10; // fast GPU offline attack

    public double calculateEntropy(int charsetSize, int length) {
        if (charsetSize <= 1 || length <= 0) return 0;
        return length * (Math.log(charsetSize) / Math.log(2));
    }

    public double calculatePassphraseEntropy(int wordlistSize, int wordCount) {
        if (wordlistSize <= 1 || wordCount <= 0) return 0;
        return wordCount * (Math.log(wordlistSize) / Math.log(2));
    }

    public StrengthLevel strengthLevel(double bits) {
        if (bits < 28)  return StrengthLevel.VERY_WEAK;
        if (bits < 36)  return StrengthLevel.WEAK;
        if (bits < 60)  return StrengthLevel.FAIR;
        if (bits < 128) return StrengthLevel.STRONG;
        return StrengthLevel.VERY_STRONG;
    }

    public String estimateCrackTime(double bits) {
        double guesses = Math.pow(2, bits) / 2.0;
        double secs    = guesses / GUESSES_PER_SECOND;

        if (secs < 1)                return "less than a second";
        if (secs < 60)               return String.format("%.0f seconds", secs);
        if (secs < 3_600)            return String.format("%.0f minutes", secs / 60);
        if (secs < 86_400)           return String.format("%.0f hours",   secs / 3_600);
        if (secs < 31_536_000)       return String.format("%.0f days",    secs / 86_400);
        if (secs < 3_153_600_000.0)  return String.format("%.0f years",   secs / 31_536_000);
        return "millions of years";
    }
}
