package com.trabalho.gestao_acoes.mappers;

import com.trabalho.gestao_acoes.domains.Acao;
import com.trabalho.gestao_acoes.domains.dtos.AcaoDTO;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AcaoMapperTest {
    @Test
    void transportsEveryQuoteProvenanceField() {
        Instant referenceAt = Instant.parse("2026-09-11T12:00:00Z");
        Instant fetchedAt = Instant.parse("2026-09-11T12:01:00Z");
        Acao entity = new Acao();
        entity.setQuoteSourceType("MARKET_DATA_PROVIDER");
        entity.setQuoteProvider("TWELVE_DATA");
        entity.setQuoteReferenceAt(referenceAt);
        entity.setQuoteFetchedAt(fetchedAt);
        entity.setQuoteReferenceKind("PROVIDER_TIMESTAMP");

        AcaoDTO dto = AcaoMapper.toDTO(entity);

        assertThat(dto.getQuoteSourceType()).isEqualTo("MARKET_DATA_PROVIDER");
        assertThat(dto.getQuoteProvider()).isEqualTo("TWELVE_DATA");
        assertThat(dto.getQuoteReferenceAt()).isEqualTo(referenceAt);
        assertThat(dto.getQuoteFetchedAt()).isEqualTo(fetchedAt);
        assertThat(dto.getQuoteReferenceKind()).isEqualTo("PROVIDER_TIMESTAMP");
        assertThat(AcaoMapper.toEntity(dto).getQuoteFetchedAt()).isEqualTo(fetchedAt);
    }
}
