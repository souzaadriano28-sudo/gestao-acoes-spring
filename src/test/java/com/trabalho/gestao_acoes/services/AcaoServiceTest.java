package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.Acao;
import com.trabalho.gestao_acoes.domains.UserAccount;
import com.trabalho.gestao_acoes.domains.dtos.AcaoDTO;
import com.trabalho.gestao_acoes.repositories.AcaoRepository;
import com.trabalho.gestao_acoes.services.ports.CotacaoBolsa;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AcaoServiceTest {
    @Test
    void refreshesALegacyAssetWithItsCompanyNameAndFullQuoteProvenance() {
        AcaoRepository repository = mock(AcaoRepository.class);
        CotacaoService quotes = mock(CotacaoService.class);
        SecurityUtils security = mock(SecurityUtils.class);
        UserAccount owner = new UserAccount();
        Acao legacy = new Acao();
        legacy.setId(7L); legacy.setTicker("PETR4"); legacy.setMercado("BRASIL"); legacy.setMoeda("BRL");
        legacy.setCotacaoAtual(new BigDecimal("20")); legacy.setOwner(owner);
        Instant referenceAt = Instant.parse("2026-09-11T12:00:00Z");
        Instant fetchedAt = Instant.parse("2026-09-11T12:01:00Z");
        CotacaoBolsa quote = new CotacaoBolsa(new BigDecimal("21"), "BRL", "MARKET_DATA_PROVIDER", "BRAPI", referenceAt, fetchedAt, "PROVIDER_TIMESTAMP");
        quote.setNomeEmpresa("Petróleo Brasileiro S.A. - Petrobras");
        when(security.currentOwnerId()).thenReturn(1L);
        when(repository.findByIdAndOwnerId(7L, 1L)).thenReturn(Optional.of(legacy));
        when(quotes.buscar("PETR4", "BRASIL")).thenReturn(quote);
        when(repository.save(legacy)).thenReturn(legacy);

        AcaoDTO result = new AcaoService(repository, quotes, security).atualizarCotacao(7L);

        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getNomeEmpresa()).isEqualTo("Petróleo Brasileiro S.A. - Petrobras");
        assertThat(result.getQuoteProvider()).isEqualTo("BRAPI");
        assertThat(result.getQuoteSourceType()).isEqualTo("MARKET_DATA_PROVIDER");
        assertThat(result.getQuoteReferenceAt()).isEqualTo(referenceAt);
        assertThat(result.getQuoteFetchedAt()).isEqualTo(fetchedAt);
        assertThat(result.getQuoteReferenceKind()).isEqualTo("PROVIDER_TIMESTAMP");
        assertThat(legacy.getOwner()).isSameAs(owner);
    }
}
