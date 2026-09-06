package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.Corretora;
import com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus;
import com.trabalho.gestao_acoes.repositories.CorretoraRepository;
import com.trabalho.gestao_acoes.services.ports.RegulatoryRegistryPort;
import java.time.Clock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RegulatoryEvidenceService {
    private final CorretoraRepository brokers;
    private final RegulatoryRegistryPort registry;
    private final Clock clock;

    public RegulatoryEvidenceService(CorretoraRepository brokers, RegulatoryRegistryPort registry, Clock clock) {
        this.brokers = brokers;
        this.registry = registry;
        this.clock = clock;
    }

    public void refreshAll() {
        var entities = brokers.findAll();
        com.trabalho.gestao_acoes.services.ports.RegulatoryRegistrySnapshot snapshot;
        try {
            snapshot = registry.load();
        } catch (RuntimeException unavailable) {
            for (Corretora broker : entities) {
                boolean hadEvidence = broker.getRegulatoryStatus() == RegulatoryStatus.VERIFIED
                        || broker.getRegulatoryStatus() == RegulatoryStatus.STALE;
                broker.setRegulatoryStatus(hadEvidence ? RegulatoryStatus.STALE : RegulatoryStatus.UNAVAILABLE);
                broker.setRegulatoryCheckedAt(clock.instant());
                broker.setRegulatoryReason("CVM_REGISTRY_UNAVAILABLE");
            }
            brokers.saveAll(entities);
            return;
        }
        for (Corretora broker : entities) {
            var entry = snapshot.activeByCnpj().get(broker.getCnpj());
            broker.setRegulatoryStatus(entry == null ? RegulatoryStatus.NOT_FOUND : RegulatoryStatus.VERIFIED);
            broker.setRegulatoryCategory(entry == null ? null : entry.category());
            broker.setRegulatoryEvidenceId(entry == null ? null : entry.evidenceId());
            broker.setRegulatorySource(snapshot.source());
            broker.setRegulatoryReferenceAt(snapshot.referenceAt());
            broker.setRegulatoryCheckedAt(snapshot.fetchedAt());
            broker.setRegulatoryReason(entry == null ? "CNPJ_NOT_FOUND_IN_ACTIVE_CVM_INTERMEDIARIES" : null);
        }
        brokers.saveAll(entities);
    }

    @Scheduled(cron = "${app.regulatory.cvm.refresh-cron:0 15 3 * * *}", zone = "UTC")
    public void scheduledRefresh() { refreshAll(); }
}
