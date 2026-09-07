package com.trabalho.gestao_acoes.services.ports;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record RegulatoryRegistrySnapshot(String source, Instant referenceAt, Instant fetchedAt,
        Map<String, List<RegulatoryEntry>> entriesByCnpj) {
    public RegulatoryRegistrySnapshot {
        if (source == null || source.isBlank() || referenceAt == null || fetchedAt == null
                || referenceAt.isAfter(fetchedAt) || entriesByCnpj == null) {
            throw new IllegalArgumentException("Invalid regulatory registry provenance");
        }
        entriesByCnpj = entriesByCnpj.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }

    public record RegulatoryEntry(String category, String status, String evidenceId) {
        public RegulatoryEntry {
            if (category == null || category.isBlank() || status == null || status.isBlank()
                    || evidenceId == null || evidenceId.isBlank()) {
                throw new IllegalArgumentException("Invalid regulatory evidence");
            }
        }
    }
}
