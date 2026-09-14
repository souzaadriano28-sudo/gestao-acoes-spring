package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.Corretora;
import com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus;
import com.trabalho.gestao_acoes.repositories.CorretoraRepository;
import com.trabalho.gestao_acoes.services.exceptions.UpstreamUnavailableException;
import com.trabalho.gestao_acoes.services.ports.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegulatoryEvidenceServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-07T15:00:00Z");

    @Test
    void refreshMarksVerifiedInactiveAndIncompatibleWithoutRemovingBrokers() {
        Corretora verified = broker("11222333000181", RegulatoryStatus.NOT_CHECKED);
        Corretora inactive = broker("11444777000161", RegulatoryStatus.VERIFIED);
        Corretora incompatible = broker("19131243000197", RegulatoryStatus.VERIFIED);
        CorretoraRepository repository = mock(CorretoraRepository.class); RegulatoryRegistryPort registry = mock(RegulatoryRegistryPort.class);
        SecurityUtils securityUtils = mock(SecurityUtils.class); when(securityUtils.currentOwnerId()).thenReturn(7L);
        when(repository.findAllByOwnerId(7L)).thenReturn(List.of(verified, inactive, incompatible));
        when(registry.load()).thenReturn(snapshot(Map.of(
                verified.getCnpj(), List.of(entry("CORRETORAS", "EM FUNCIONAMENTO NORMAL")),
                inactive.getCnpj(), List.of(entry("CORRETORAS", "CANCELADA")),
                incompatible.getCnpj(), List.of(entry("CUSTODIANTES DE VALORES MOBILIÁRIOS", "EM FUNCIONAMENTO NORMAL")))));
        RegulatoryVerificationService verification = verifier(registry, Duration.ofDays(7));
        new RegulatoryEvidenceService(repository, verification, Clock.fixed(NOW, ZoneOffset.UTC), securityUtils).refreshAll();
        assertThat(verified.getRegulatoryStatus()).isEqualTo(RegulatoryStatus.VERIFIED);
        assertThat(inactive.getRegulatoryStatus()).isEqualTo(RegulatoryStatus.INACTIVE);
        assertThat(incompatible.getRegulatoryStatus()).isEqualTo(RegulatoryStatus.INCOMPATIBLE);
        assertThat(repository.count()).isZero();
        verify(repository).saveAll(List.of(verified, inactive, incompatible)); verify(repository, never()).delete(any());
        verify(repository, never()).findAll();
    }

    @Test
    void usesRecentCachedSnapshotButBlocksWhenCacheIsMissingOrExpired() {
        RegulatoryRegistryPort registry = mock(RegulatoryRegistryPort.class);
        when(registry.load()).thenReturn(snapshot(Map.of())).thenThrow(new RuntimeException("503"));
        RegulatoryVerificationService verification = verifier(registry, Duration.ofDays(7));
        verification.loadSnapshot();
        assertThat(verification.loadSnapshot()).isNotNull();

        RegulatoryRegistryPort unavailable = mock(RegulatoryRegistryPort.class); when(unavailable.load()).thenThrow(new RuntimeException("timeout"));
        assertThatThrownBy(() -> verifier(unavailable, Duration.ofDays(7)).loadSnapshot()).isInstanceOf(UpstreamUnavailableException.class);

        RegulatoryRegistryPort stale = mock(RegulatoryRegistryPort.class);
        when(stale.load()).thenReturn(new RegulatoryRegistrySnapshot("CVM", NOW.minus(Duration.ofDays(8)), NOW, Map.of()));
        assertThatThrownBy(() -> verifier(stale, Duration.ofDays(7)).loadSnapshot()).isInstanceOf(UpstreamUnavailableException.class);
    }

    private static RegulatoryVerificationService verifier(RegulatoryRegistryPort registry, Duration freshness) {
        return new RegulatoryVerificationService(registry, Clock.fixed(NOW, ZoneOffset.UTC), freshness, Set.of("CORRETORAS"));
    }
    private static RegulatoryRegistrySnapshot snapshot(Map<String,List<RegulatoryRegistrySnapshot.RegulatoryEntry>> entries) {
        return new RegulatoryRegistrySnapshot("CVM", NOW.minus(Duration.ofHours(1)), NOW, entries);
    }
    private static RegulatoryRegistrySnapshot.RegulatoryEntry entry(String category, String status) { return new RegulatoryRegistrySnapshot.RegulatoryEntry(category, status, "123"); }
    private static Corretora broker(String cnpj, RegulatoryStatus status) { Corretora broker = new Corretora(); broker.setCnpj(cnpj); broker.setRegulatoryStatus(status); return broker; }
}
