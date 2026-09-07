package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus;
import com.trabalho.gestao_acoes.services.exceptions.UpstreamUnavailableException;
import com.trabalho.gestao_acoes.services.ports.RegulatoryRegistryPort;
import com.trabalho.gestao_acoes.services.ports.RegulatoryRegistrySnapshot;
import com.trabalho.gestao_acoes.services.ports.RegulatoryRegistrySnapshot.RegulatoryEntry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RegulatoryVerificationService {
    static final String ACTIVE = "EM FUNCIONAMENTO NORMAL";
    private final RegulatoryRegistryPort registry;
    private final Clock clock;
    private final Duration freshness;
    private final Set<String> compatibleCategories;
    private final AtomicReference<RegulatoryRegistrySnapshot> cached = new AtomicReference<>();

    @Autowired
    public RegulatoryVerificationService(RegulatoryRegistryPort registry, Clock clock,
            @Value("${app.regulatory.cvm.freshness:P7D}") Duration freshness,
            @Value("${app.regulatory.cvm.compatible-categories:CORRETORAS,DISTRIBUIDORAS,BANCOS DE INVESTIMENTOS,BANCOS MÚLTIPLOS COM CARTEIRA DE INVESTIMENTO}") List<String> categories) {
        this(registry, clock, freshness, Set.copyOf(categories));
    }

    public RegulatoryVerificationService(RegulatoryRegistryPort registry, Clock clock, Duration freshness,
            Set<String> categories) {
        if (freshness == null || freshness.isZero() || freshness.isNegative() || categories == null || categories.isEmpty()) {
            throw new IllegalArgumentException("Regulatory policy must define freshness and compatible categories");
        }
        this.registry = registry;
        this.clock = clock;
        this.freshness = freshness;
        this.compatibleCategories = categories.stream().map(RegulatoryVerificationService::canonical)
                .collect(Collectors.toUnmodifiableSet());
    }

    public RegulatoryRegistrySnapshot loadSnapshot() {
        try {
            RegulatoryRegistrySnapshot snapshot = registry.load();
            requireFresh(snapshot);
            cached.set(snapshot);
            return snapshot;
        } catch (RuntimeException failure) {
            RegulatoryRegistrySnapshot snapshot = cached.get();
            if (snapshot != null && isFresh(snapshot)) return snapshot;
            throw new UpstreamUnavailableException("A consulta ao cadastro oficial da CVM está temporariamente indisponível. Tente novamente mais tarde.");
        }
    }

    public Decision verify(String cnpj) {
        return verify(cnpj, loadSnapshot());
    }

    public Decision verify(String cnpj, RegulatoryRegistrySnapshot snapshot) {
        requireFresh(snapshot);
        List<RegulatoryEntry> entries = snapshot.entriesByCnpj().get(cnpj);
        Instant checkedAt = clock.instant();
        if (entries == null || entries.isEmpty()) {
            return new Decision(RegulatoryStatus.NOT_FOUND, null, snapshot.source(), null,
                    snapshot.referenceAt(), checkedAt, "CNPJ não localizado no cadastro oficial da CVM.");
        }
        List<RegulatoryEntry> active = entries.stream().filter(entry -> ACTIVE.equalsIgnoreCase(entry.status())).toList();
        List<RegulatoryEntry> compatible = active.stream()
                .filter(entry -> compatibleCategories.contains(canonical(entry.category()))).toList();
        if (!compatible.isEmpty()) {
            return decision(RegulatoryStatus.VERIFIED, compatible, snapshot, checkedAt, null);
        }
        if (!active.isEmpty()) {
            return decision(RegulatoryStatus.INCOMPATIBLE, active, snapshot, checkedAt,
                    "A categoria encontrada na CVM não é compatível com o cadastro de corretora.");
        }
        return decision(RegulatoryStatus.INACTIVE, entries, snapshot, checkedAt,
                "O registro da instituição na CVM não está em funcionamento normal.");
    }

    private Decision decision(RegulatoryStatus status, List<RegulatoryEntry> entries,
            RegulatoryRegistrySnapshot snapshot, Instant checkedAt, String reason) {
        String categories = entries.stream().map(RegulatoryEntry::category).distinct().sorted().collect(Collectors.joining(" | "));
        String evidenceIds = entries.stream().map(RegulatoryEntry::evidenceId).distinct().sorted().collect(Collectors.joining(","));
        return new Decision(status, categories, snapshot.source(), evidenceIds, snapshot.referenceAt(), checkedAt, reason);
    }

    private void requireFresh(RegulatoryRegistrySnapshot snapshot) {
        if (!isFresh(snapshot)) {
            throw new UpstreamUnavailableException("O snapshot disponível da CVM está vencido. O cadastro foi temporariamente bloqueado.");
        }
    }

    private boolean isFresh(RegulatoryRegistrySnapshot snapshot) {
        return snapshot != null && !snapshot.referenceAt().plus(freshness).isBefore(clock.instant());
    }

    private static String canonical(String value) { return value.trim().toUpperCase(Locale.ROOT); }

    public record Decision(RegulatoryStatus status, String category, String source, String evidenceId,
            Instant referenceAt, Instant checkedAt, String reason) {
        public boolean authorized() { return status == RegulatoryStatus.VERIFIED; }
    }
}
