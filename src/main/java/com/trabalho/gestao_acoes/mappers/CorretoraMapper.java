package com.trabalho.gestao_acoes.mappers;

import com.trabalho.gestao_acoes.domains.Corretora;
import com.trabalho.gestao_acoes.domains.dtos.CorretoraDTO;

public class CorretoraMapper {

    public static CorretoraDTO toDTO(Corretora entity) {
        if (entity == null) {
            return null;
        }
        CorretoraDTO dto = new CorretoraDTO();
        dto.setId(entity.getId());
        dto.setCnpj(entity.getCnpj());
        dto.setRazaoSocial(entity.getRazaoSocial());
        dto.setNomeFantasia(entity.getNomeFantasia());
        dto.setEmail(entity.getEmail());
        dto.setTelefone(entity.getTelefone());
        dto.setCep(entity.getCep());
        dto.setLogradouro(entity.getLogradouro());
        dto.setNumero(entity.getNumero());
        dto.setComplemento(entity.getComplemento());
        dto.setBairro(entity.getBairro());
        dto.setCidade(entity.getCidade());
        dto.setUf(entity.getUf());
        dto.setSituacaoCadastral(entity.getSituacaoCadastral());
        dto.setValidadaNaCvm(entity.getValidadaNaCvm());
        dto.setDataCadastro(entity.getDataCadastro());
        return dto;
    }

    public static Corretora toEntity(CorretoraDTO dto) {
        if (dto == null) {
            return null;
        }
        Corretora entity = new Corretora();
        entity.setId(dto.getId());
        entity.setCnpj(dto.getCnpj());
        entity.setRazaoSocial(dto.getRazaoSocial());
        entity.setNomeFantasia(dto.getNomeFantasia());
        entity.setEmail(dto.getEmail());
        entity.setTelefone(dto.getTelefone());
        entity.setCep(dto.getCep());
        entity.setLogradouro(dto.getLogradouro());
        entity.setNumero(dto.getNumero());
        entity.setComplemento(dto.getComplemento());
        entity.setBairro(dto.getBairro());
        entity.setCidade(dto.getCidade());
        entity.setUf(dto.getUf());
        entity.setSituacaoCadastral(dto.getSituacaoCadastral());
        entity.setValidadaNaCvm(dto.getValidadaNaCvm());
        entity.setDataCadastro(dto.getDataCadastro());
        return entity;
    }
}