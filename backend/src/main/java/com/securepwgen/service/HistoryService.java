package com.securepwgen.service;

import com.securepwgen.model.PasswordHistory;
import com.securepwgen.model.PasswordResponse.PasswordEntry;
import com.securepwgen.repository.PasswordHistoryRepository;
import com.securepwgen.security.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class HistoryService {

    private static final Logger log = LoggerFactory.getLogger(HistoryService.class);

    private final PasswordHistoryRepository repo;
    private final EncryptionService         encryption;

    public HistoryService(PasswordHistoryRepository repo, EncryptionService encryption) {
        this.repo       = repo;
        this.encryption = encryption;
    }

    /** Save a password entry encrypted. */
    public PasswordHistory save(PasswordEntry entry, String label, boolean passphrase) {
        PasswordHistory h = new PasswordHistory();
        h.setEncryptedPassword(encryption.encrypt(entry.getPassword()));
        h.setLabel(label != null ? label : "");
        h.setPasswordLength(entry.getPassword().length());
        h.setEntropyBits(entry.getEntropyBits());
        h.setStrengthLevel(entry.getStrengthLevel());
        h.setPassphrase(passphrase);
        h.setBreachChecked(entry.isBreached());

        PasswordHistory saved = repo.save(h);
        log.info("Saved encrypted history entry [id={}]", saved.getId());
        return saved;
    }

    /** List history metadata (no decryption). */
    @Transactional(readOnly = true)
    public Page<PasswordHistory> list(int page, int size) {
        return repo.findAll(
            PageRequest.of(page, Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    /** Decrypt a single entry by ID. */
    @Transactional(readOnly = true)
    public String decrypt(String id) {
        PasswordHistory h = repo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Entry not found: " + id));
        return encryption.decrypt(h.getEncryptedPassword());
    }

    /** Delete one entry. */
    public void delete(String id) {
        repo.deleteById(id);
    }

    /** Clear all history. */
    public void clearAll() {
        repo.deleteAll();
    }

    /** Export all passwords as encrypted blob. */
    public String exportEncrypted() {
        List<PasswordHistory> all = repo.findAll(
            Sort.by(Sort.Direction.DESC, "createdAt"));
        StringBuilder sb = new StringBuilder();
        for (PasswordHistory h : all) {
            String pwd = encryption.decrypt(h.getEncryptedPassword());
            sb.append(h.getCreatedAt())
              .append(" | ")
              .append(h.getLabel().isBlank() ? "unlabeled" : h.getLabel())
              .append(" | ")
              .append(pwd)
              .append("\n");
        }
        return encryption.encrypt(sb.toString());
    }
}
