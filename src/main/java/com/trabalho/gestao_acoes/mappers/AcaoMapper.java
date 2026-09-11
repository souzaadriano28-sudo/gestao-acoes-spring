package com.trabalho.gestao_acoes.mappers;

import com.trabalho.gestao_acoes.domains.Acao;
import com.trabalho.gestao_acoes.domains.dtos.AcaoDTO;

public class AcaoMapper {

    public static AcaoDTO toDTO(Acao entity) {
        if (entity == null) {
            return null;
        }
        AcaoDTO dto = new AcaoDTO();
        dto.setId(entity.getId());
        dto.setTicker(entity.getTicker());
        dto.setNomeEmpresa(entity.getNomeEmpresa());
        dto.setMercado(entity.getMercado());
        dto.setMoeda(entity.getMoeda());
        dto.setCotacaoAtual(entity.getCotacaoAtual());
        dto.setDataHoraCotacao(entity.getDataHoraCotacao());
        dto.setQuoteSourceType(entity.getQuoteSourceType());
        dto.setQuoteProvider(entity.getQuoteProvider());
        dto.setQuoteReferenceAt(entity.getQuoteReferenceAt());
        dto.setQuoteFetchedAt(entity.getQuoteFetchedAt());
        dto.setQuoteReferenceKind(entity.getQuoteReferenceKind());
        return dto;
    }

    public static Acao toEntity(AcaoDTO dto) {
        if (dto == null) {
            return null;
        }
        Acao entity = new Acao();
        entity.setTicker(dto.getTicker());
        entity.setNomeEmpresa(dto.getNomeEmpresa());
        entity.setMercado(dto.getMercado());
        entity.setMoeda(dto.getMoeda());
        entity.setCotacaoAtual(dto.getCotacaoAtual());
        entity.setDataHoraCotacao(dto.getDataHoraCotacao());
        entity.setQuoteSourceType(dto.getQuoteSourceType());
        entity.setQuoteProvider(dto.getQuoteProvider());
        entity.setQuoteReferenceAt(dto.getQuoteReferenceAt());
        entity.setQuoteFetchedAt(dto.getQuoteFetchedAt());
        entity.setQuoteReferenceKind(dto.getQuoteReferenceKind());
        return entity;
    }
}
