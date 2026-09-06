package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.Corretora;
import com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus;
import com.trabalho.gestao_acoes.repositories.CorretoraRepository;
import com.trabalho.gestao_acoes.services.ports.*;
import com.trabalho.gestao_acoes.mappers.CorretoraMapper;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RegulatoryEvidenceServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-06T15:00:00Z");

    @Test
    void refreshSeparatesVerifiedEvidenceFromNotFoundBusinessRegistration() {
        Corretora verified = broker("12345678000199", RegulatoryStatus.NOT_CHECKED);
        Corretora absent = broker("98765432000110", RegulatoryStatus.NOT_CHECKED);
        CorretoraRepository repository = mock(CorretoraRepository.class);
        RegulatoryRegistryPort registry = mock(RegulatoryRegistryPort.class);
        when(repository.findAll()).thenReturn(List.of(verified, absent));
        when(registry.load()).thenReturn(new RegulatoryRegistrySnapshot("CVM", NOW.minusSeconds(3600), NOW,
                Map.of(verified.getCnpj(), new RegulatoryRegistrySnapshot.RegulatoryEntry("CORRETORA", "123"))));

        new RegulatoryEvidenceService(repository, registry, Clock.fixed(NOW, ZoneOffset.UTC)).refreshAll();

        assertThat(verified.getRegulatoryStatus()).isEqualTo(RegulatoryStatus.VERIFIED);
        assertThat(verified.getRegulatoryEvidenceId()).isEqualTo("123");
        assertThat(absent.getRegulatoryStatus()).isEqualTo(RegulatoryStatus.NOT_FOUND);
        assertThat(absent.getRegulatoryReason()).isEqualTo("CNPJ_NOT_FOUND_IN_ACTIVE_CVM_INTERMEDIARIES");
        verify(repository).saveAll(List.of(verified, absent));
    }

    @Test
    void providerFailureMakesPriorEvidenceStaleAndUnknownEvidenceUnavailableWithoutChangingReference() {
        Corretora previous = broker("12345678000199", RegulatoryStatus.VERIFIED);
        Instant originalReference = NOW.minus(Duration.ofDays(3));
        previous.setRegulatoryReferenceAt(originalReference);
        Corretora unknown = broker("98765432000110", RegulatoryStatus.NOT_CHECKED);
        CorretoraRepository repository = mock(CorretoraRepository.class);
        RegulatoryRegistryPort registry = mock(RegulatoryRegistryPort.class);
        when(repository.findAll()).thenReturn(List.of(previous, unknown));
        when(registry.load()).thenThrow(new RuntimeException("timeout or HTTP 429"));

        new RegulatoryEvidenceService(repository, registry, Clock.fixed(NOW, ZoneOffset.UTC)).refreshAll();

        assertThat(previous.getRegulatoryStatus()).isEqualTo(RegulatoryStatus.STALE);
        assertThat(previous.getRegulatoryReferenceAt()).isEqualTo(originalReference);
        assertThat(unknown.getRegulatoryStatus()).isEqualTo(RegulatoryStatus.UNAVAILABLE);
        assertThat(previous.getRegulatoryCheckedAt()).isEqualTo(NOW);
        assertThat(unknown.getRegulatoryCheckedAt()).isEqualTo(NOW);
    }

    @Test
    void mapperMarksOldVerifiedEvidenceStaleAtTheConfiguredBoundary() {
        Corretora broker = broker("12345678000199", RegulatoryStatus.VERIFIED);
        broker.setRegulatoryReferenceAt(NOW.minus(Duration.ofDays(2)).minusSeconds(1));
        broker.setRegulatoryCheckedAt(NOW.minus(Duration.ofDays(2)));
        broker.setRegulatoryCategory("CORRETORA"); broker.setRegulatorySource("CVM"); broker.setRegulatoryEvidenceId("123");

        var dto = CorretoraMapper.toDTO(broker, NOW, Duration.ofDays(2));

        assertThat(dto.getRegulatoryEvidence().status()).isEqualTo(RegulatoryStatus.STALE);
        assertThat(dto.getRegulatoryEvidence().reason()).isEqualTo("CVM_REGISTRY_FRESHNESS_EXCEEDED");
        assertThat(dto.getRegulatoryEvidence().referenceAt()).isEqualTo(broker.getRegulatoryReferenceAt());
    }

    private static Corretora broker(String cnpj, RegulatoryStatus status) {
        Corretora broker = new Corretora(); broker.setCnpj(cnpj); broker.setRegulatoryStatus(status); return broker;
    }
}
