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
        return dto;
    }

    public static Acao toEntity(AcaoDTO dto) {
        if (dto == null) {
            return null;
        }
        Acao entity = new Acao();
        entity.setId(dto.getId());
        entity.setTicker(dto.getTicker());
        entity.setNomeEmpresa(dto.getNomeEmpresa());
        entity.setMercado(dto.getMercado());
        entity.setMoeda(dto.getMoeda());
        entity.setCotacaoAtual(dto.getCotacaoAtual());
        entity.setDataHoraCotacao(dto.getDataHoraCotacao());
        return entity;
    }
}