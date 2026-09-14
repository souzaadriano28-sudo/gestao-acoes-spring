package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.Corretora;
import com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus;
import com.trabalho.gestao_acoes.repositories.CorretoraRepository;
import com.trabalho.gestao_acoes.services.exceptions.UpstreamUnavailableException;
import java.time.Clock;
import org.springframework.stereotype.Service;

@Service
public class RegulatoryEvidenceService {
    private final CorretoraRepository brokers;
    private final RegulatoryVerificationService verification;
    private final Clock clock;
    private final SecurityUtils securityUtils;

    public RegulatoryEvidenceService(CorretoraRepository brokers, RegulatoryVerificationService verification, Clock clock,
            SecurityUtils securityUtils) {
        this.brokers = brokers; this.verification = verification; this.clock = clock; this.securityUtils = securityUtils;
    }

    public void refreshAll() {
        var entities = brokers.findAllByOwnerId(securityUtils.currentOwnerId());
        final com.trabalho.gestao_acoes.services.ports.RegulatoryRegistrySnapshot snapshot;
        try {
            snapshot = verification.loadSnapshot();
        } catch (UpstreamUnavailableException unavailable) {
            for (Corretora broker : entities) {
                boolean hadEvidence = broker.getRegulatoryStatus() == RegulatoryStatus.VERIFIED || broker.getRegulatoryStatus() == RegulatoryStatus.STALE;
                broker.setRegulatoryStatus(hadEvidence ? RegulatoryStatus.STALE : RegulatoryStatus.UNAVAILABLE);
                broker.setValidadaNaCvm(false);
                broker.setRegulatoryCheckedAt(clock.instant());
                broker.setRegulatoryReason("Cadastro oficial da CVM temporariamente indisponível.");
            }
            brokers.saveAll(entities);
            throw unavailable;
        }
        for (Corretora broker : entities) CorretoraService.applyDecision(broker, verification.verify(broker.getCnpj(), snapshot));
        brokers.saveAll(entities);
    }

}
