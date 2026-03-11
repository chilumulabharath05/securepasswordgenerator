package com.securepwgen.controller;

import com.securepwgen.model.PasswordHistory;
import com.securepwgen.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/history")
@Tag(name = "Password History", description = "Encrypted password history management")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    @Operation(summary = "List history entries (metadata only — no decryption)")
    public ResponseEntity<Page<PasswordHistory>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(historyService.list(page, size));
    }

    @GetMapping("/{id}/decrypt")
    @Operation(summary = "Decrypt a single history entry by ID")
    public ResponseEntity<Map<String, String>> decrypt(@PathVariable String id) {
        String password = historyService.decrypt(id);
        return ResponseEntity.ok(Map.of("password", password));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a history entry")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        historyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Clear all history")
    public ResponseEntity<Map<String, String>> clearAll() {
        historyService.clearAll();
        return ResponseEntity.ok(Map.of("message", "All history cleared."));
    }
}
