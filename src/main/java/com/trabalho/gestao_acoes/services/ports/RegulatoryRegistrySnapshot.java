package com.trabalho.gestao_acoes.services.ports;

import java.time.Instant;
import java.util.Map;

public record RegulatoryRegistrySnapshot(String source, Instant referenceAt, Instant fetchedAt,
        Map<String, RegulatoryEntry> activeByCnpj) {
    public RegulatoryRegistrySnapshot {
        if (source == null || source.isBlank() || referenceAt == null || fetchedAt == null
                || referenceAt.isAfter(fetchedAt) || activeByCnpj == null) {
            throw new IllegalArgumentException("Invalid regulatory registry provenance");
        }
        activeByCnpj = Map.copyOf(activeByCnpj);
    }

    public record RegulatoryEntry(String category, String evidenceId) {
        public RegulatoryEntry {
            if (category == null || category.isBlank() || evidenceId == null || evidenceId.isBlank()) {
                throw new IllegalArgumentException("Invalid regulatory evidence");
            }
        }
    }
}
